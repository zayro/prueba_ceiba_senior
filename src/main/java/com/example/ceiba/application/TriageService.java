package com.example.ceiba.application;

import com.example.ceiba.domain.PatientData;
import com.example.ceiba.domain.PatientState;
import com.example.ceiba.domain.SlaPolicy;
import com.example.ceiba.domain.TriageClassifier;
import com.example.ceiba.domain.TriageLevel;
import com.example.ceiba.infrastructure.persistence.AuditEntity;
import com.example.ceiba.infrastructure.persistence.AuditRepository;
import com.example.ceiba.infrastructure.persistence.OutboxEntity;
import com.example.ceiba.infrastructure.persistence.OutboxRepository;
import com.example.ceiba.infrastructure.persistence.PatientEntity;
import com.example.ceiba.infrastructure.persistence.PatientRepository;
import com.example.ceiba.infrastructure.web.OperationReportRow;
import com.example.ceiba.infrastructure.web.PatientResponse;
import com.example.ceiba.infrastructure.web.RegisterPatientRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TriageService {
	private final PatientRepository patients;
	private final AuditRepository audits;
	private final OutboxRepository outbox;
	private final TriageClassifier classifier;
	private final SlaPolicy slaPolicy;
	private final DatabaseClient database;
	private final ObjectMapper objectMapper;
	private final MeterRegistry metrics;

	public TriageService(
		PatientRepository patients,
		AuditRepository audits,
		OutboxRepository outbox,
		TriageClassifier classifier,
		SlaPolicy slaPolicy,
		DatabaseClient database,
		ObjectMapper objectMapper,
		MeterRegistry metrics
	) {
		this.patients = patients;
		this.audits = audits;
		this.outbox = outbox;
		this.classifier = classifier;
		this.slaPolicy = slaPolicy;
		this.database = database;
		this.objectMapper = objectMapper;
		this.metrics = metrics;
	}

	@Transactional
	public Mono<PatientResponse> register(RegisterPatientRequest request) {
		if (request.diastolicPressure() >= request.systolicPressure()) {
			return Mono.error(new IllegalArgumentException(
				"diastolicPressure debe ser menor que systolicPressure"));
		}
		Instant now = Instant.now();
		PatientData data = new PatientData(
			request.fullName().strip(), request.age(), request.heartRate(),
			request.systolicPressure(), request.diastolicPressure(), request.oxygenSaturation(),
			request.temperature(), request.pain(), request.gcs(), request.chiefComplaint().strip()
		);
		TriageLevel level = classifier.classify(data);
		int slaMinutes = slaPolicy.minutesFor(level);
		PatientEntity patient = new PatientEntity(
			UUID.randomUUID(), data.fullName(), data.age(), data.heartRate(), data.systolicPressure(),
			data.diastolicPressure(), data.oxygenSaturation(), data.temperature(), data.pain(),
			data.gcs(), data.chiefComplaint(), level.name(), slaMinutes,
			PatientState.REGISTRADO.name(), false, now, now, null
		);
		return patients.save(patient)
			.flatMap(saved -> persistEvent(saved, null, PatientState.REGISTRADO.name(), "paciente_registrado"))
			.doOnSuccess(saved -> metrics.counter("triage.classifications", "level", level.name()).increment())
			.map(saved -> PatientResponse.from(saved, java.util.List.of(), now));
	}

	@Transactional
	public Mono<PatientResponse> changeState(UUID id, String requestedState) {
		PatientState target = PatientState.valueOf(requestedState);
		Instant now = Instant.now();
		return patients.findById(id)
			.switchIfEmpty(Mono.error(new PatientNotFoundException("No existe el paciente " + id)))
			.flatMap(current -> {
				PatientState previous = PatientState.valueOf(current.state());
				if (!isAllowed(previous, target)) {
					return Mono.error(new InvalidTransitionException(
						"Transición no permitida: " + previous + " -> " + target));
				}
				return patients.transition(id, previous.name(), target.name(), now)
					.flatMap(updated -> updated == 1
						? patients.findById(id)
							.flatMap(patient -> persistEvent(patient, previous.name(), target.name(),
								target == PatientState.ALTA ? "paciente_alta" : "paciente_en_consulta"))
						: Mono.error(new InvalidTransitionException(
							"El estado del paciente cambió concurrentemente; vuelva a consultar")));
			})
			.flatMap(patient -> audits.findByPatientIdOrderByTimestampAsc(id).collectList()
				.map(history -> PatientResponse.from(patient, history, now)));
	}

	public Mono<PatientResponse> findPatient(UUID id) {
		return patients.findById(id)
			.switchIfEmpty(Mono.error(new PatientNotFoundException("No existe el paciente " + id)))
			.flatMap(patient -> audits.findByPatientIdOrderByTimestampAsc(id).collectList()
				.map(history -> PatientResponse.from(patient, history, Instant.now())));
	}

	@Transactional
	public Mono<Void> escalateIfDue(PatientEntity candidate) {
		Instant now = Instant.now();
		return patients.markEscalatedIfDue(candidate.id(), now)
			.flatMap(updated -> {
				if (updated == 0) {
					return Mono.empty();
				}
				return patients.findById(candidate.id())
					.flatMap(patient -> persistEvent(patient, patient.state(), patient.state(), "paciente_escalado"))
					.doOnSuccess(ignored -> metrics.counter("triage.escalations").increment())
					.then();
			});
	}

	public Flux<OperationReportRow> operationReport(Instant from, Instant until) {
		String sql = """
			SELECT date_trunc('hour', created_at) AS hour,
			       COUNT(*) AS total,
			       COUNT(*) FILTER (WHERE level = 'CRITICO') AS critico,
			       COUNT(*) FILTER (WHERE level = 'URGENTE') AS urgente,
			       COUNT(*) FILTER (WHERE level = 'URGENTE_2') AS urgente2,
			       COUNT(*) FILTER (WHERE level = 'NO_URGENTE') AS no_urgente,
			       COUNT(*) FILTER (WHERE level = 'NO_URGENTE_2') AS no_urgente2,
			       AVG((EXTRACT(EPOCH FROM (first_consultation_at - created_at)) / 60.0)::double precision)
			           FILTER (WHERE first_consultation_at IS NOT NULL) AS average_wait,
			       percentile_cont(0.95) WITHIN GROUP (
			           ORDER BY (EXTRACT(EPOCH FROM (first_consultation_at - created_at)) / 60.0)::double precision
			       ) FILTER (WHERE first_consultation_at IS NOT NULL) AS p95_wait
			FROM patients
			WHERE created_at >= :from AND created_at < :until
			GROUP BY date_trunc('hour', created_at)
			ORDER BY hour
			""";
		return database.sql(sql)
			.bind("from", from)
			.bind("until", until)
			.map((row, metadata) -> new OperationReportRow(
				row.get("hour", Instant.class),
				row.get("total", Long.class),
				row.get("critico", Long.class),
				row.get("urgente", Long.class),
				row.get("urgente2", Long.class),
				row.get("no_urgente", Long.class),
				row.get("no_urgente2", Long.class),
				row.get("average_wait", Double.class),
				row.get("p95_wait", Double.class)
			))
			.all();
	}

	private boolean isAllowed(PatientState current, PatientState target) {
		return current == PatientState.REGISTRADO
				&& (target == PatientState.EN_CONSULTA || target == PatientState.ALTA)
			|| current == PatientState.EN_CONSULTA && target == PatientState.ALTA;
	}

	private Mono<PatientEntity> persistEvent(
		PatientEntity patient, String previousState, String newState, String eventType
	) {
		Instant now = Instant.now();
		TriageEvent event = new TriageEvent(
			UUID.randomUUID(), patient.id(), eventType, patient.level(), newState, now
		);
		String payload;
		try {
			payload = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException error) {
			return Mono.error(new IllegalStateException("No fue posible serializar el evento clínico", error));
		}
		boolean newEscalated = patient.escalated();
		boolean previousEscalated = eventType.equals("paciente_escalado") ? false : newEscalated;
		return Mono.when(
			audits.save(new AuditEntity(
				UUID.randomUUID(), patient.id(), now, previousState, newState, eventType,
				previousEscalated, newEscalated)),
			outbox.save(new OutboxEntity(event.eventId(), patient.id().toString(), payload, now, null))
		).thenReturn(patient);
	}
}
