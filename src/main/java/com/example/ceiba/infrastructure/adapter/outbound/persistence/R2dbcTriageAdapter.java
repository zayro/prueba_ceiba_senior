package com.example.ceiba.infrastructure.adapter.persistence;

import com.example.ceiba.domain.model.PatientState;
import com.example.ceiba.domain.model.OperationReport;
import com.example.ceiba.domain.model.Patient;
import com.example.ceiba.domain.model.PatientAudit;
import com.example.ceiba.domain.port.AuditStore;
import com.example.ceiba.domain.port.OperationReportStore;
import com.example.ceiba.domain.port.OutboxStore;
import com.example.ceiba.domain.port.PatientStore;
import com.example.ceiba.domain.model.TriageEvent;
import com.example.ceiba.infrastructure.mapper.TriagePersistenceMapper;
import com.example.ceiba.infrastructure.persistence.AuditRepository;
import com.example.ceiba.infrastructure.persistence.OutboxEntity;
import com.example.ceiba.infrastructure.persistence.OutboxRepository;
import com.example.ceiba.infrastructure.persistence.PatientRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class R2dbcTriageAdapter implements PatientStore, AuditStore, OutboxStore, OperationReportStore {
	private static final String OPERATION_REPORT_SQL = """
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

	private final PatientRepository patients;
	private final AuditRepository audits;
	private final OutboxRepository outbox;
	private final DatabaseClient database;
	private final ObjectMapper objectMapper;
	private final TriagePersistenceMapper mapper;

	public R2dbcTriageAdapter(
		PatientRepository patients,
		AuditRepository audits,
		OutboxRepository outbox,
		DatabaseClient database,
		ObjectMapper objectMapper,
		TriagePersistenceMapper mapper
	) {
		this.patients = patients;
		this.audits = audits;
		this.outbox = outbox;
		this.database = database;
		this.objectMapper = objectMapper;
		this.mapper = mapper;
	}

	@Override
	public Mono<Patient> save(Patient patient) {
		return patients.save(mapper.toEntity(patient)).map(mapper::toDomain);
	}

	@Override
	public Mono<Patient> findById(UUID id) {
		return patients.findById(id).map(mapper::toDomain);
	}

	@Override
	public Flux<Patient> findByState(PatientState state) {
		return patients.findByState(state.name()).map(mapper::toDomain);
	}

	@Override
	public Mono<Integer> transition(UUID id, PatientState expectedState, PatientState newState, Instant changedAt) {
		return patients.transition(id, expectedState.name(), newState.name(), changedAt);
	}

	@Override
	public Mono<Integer> markEscalatedIfDue(UUID id, Instant now) {
		return patients.markEscalatedIfDue(id, now);
	}

	@Override
	public Mono<PatientAudit> save(PatientAudit audit) {
		return audits.save(mapper.toEntity(audit)).map(mapper::toDomain);
	}

	@Override
	public Flux<PatientAudit> findByPatientId(UUID patientId) {
		return audits.findByPatientIdOrderByTimestampAsc(patientId).map(mapper::toDomain);
	}

	@Override
	public Mono<Void> append(TriageEvent event) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException error) {
			return Mono.error(new IllegalStateException("No fue posible serializar el evento clínico", error));
		}
		OutboxEntity entity = new OutboxEntity(
			event.eventId(), event.patientId().toString(), payload, event.occurredAt(), null
		);
		return outbox.save(entity).then();
	}

	@Override
	public Flux<TriageEvent> findPending() {
		return outbox.findPending()
			.map(entity -> {
				try {
					return objectMapper.readValue(entity.payload(), TriageEvent.class);
				} catch (JsonProcessingException error) {
					throw new IllegalStateException(
						"Evento de outbox inválido: " + entity.id(), error);
				}
			});
	}

	@Override
	public Mono<Integer> markPublished(UUID eventId, Instant publishedAt) {
		return outbox.markPublished(eventId, publishedAt);
	}

	@Override
	public Flux<OperationReport> findBetween(Instant from, Instant until) {
		return database.sql(OPERATION_REPORT_SQL)
			.bind("from", from)
			.bind("until", until)
			.map((row, metadata) -> new OperationReport(
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
}
