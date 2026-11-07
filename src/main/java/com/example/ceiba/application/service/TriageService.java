package com.example.ceiba.application.service;

import com.example.ceiba.domain.exception.InvalidTransitionException;
import com.example.ceiba.domain.exception.PatientNotFoundException;
import com.example.ceiba.domain.model.OperationReport;
import com.example.ceiba.domain.model.Patient;
import com.example.ceiba.domain.model.PatientAudit;
import com.example.ceiba.domain.valueobject.PatientData;
import com.example.ceiba.domain.model.PatientDetails;
import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.model.TriageEvent;
import com.example.ceiba.domain.valueobject.SlaPolicy;
import com.example.ceiba.domain.model.TriageClassifier;
import com.example.ceiba.domain.valueobject.TriageLevel;
import com.example.ceiba.domain.port.outbound.AuditStore;
import com.example.ceiba.domain.port.outbound.OperationReportStore;
import com.example.ceiba.domain.port.outbound.OutboxStore;
import com.example.ceiba.domain.port.outbound.PatientStore;
import com.example.ceiba.domain.port.outbound.TriageMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TriageService {
	private final PatientStore patients;
	private final AuditStore audits;
	private final OutboxStore outbox;
	private final OperationReportStore reports;
	private final TriageClassifier classifier;
	private final SlaPolicy slaPolicy;
	private final TriageMetrics metrics;
	private final Clock clock;

	public TriageService(
		PatientStore patients,
		AuditStore audits,
		OutboxStore outbox,
		OperationReportStore reports,
		TriageClassifier classifier,
		SlaPolicy slaPolicy,
		TriageMetrics metrics,
		Clock clock
	) {
		this.patients = patients;
		this.audits = audits;
		this.outbox = outbox;
		this.reports = reports;
		this.classifier = classifier;
		this.slaPolicy = slaPolicy;
		this.metrics = metrics;
		this.clock = clock;
	}

	public Mono<PatientDetails> register(PatientData request) {
		PatientData data = new PatientData(
			request.fullName().strip(), request.age(), request.heartRate(),
			request.systolicPressure(), request.diastolicPressure(), request.oxygenSaturation(),
			request.temperature(), request.pain(), request.gcs(), request.chiefComplaint().strip()
		);
		Instant now = clock.instant();
		TriageLevel level = classifier.classify(data);
		Patient patient = new Patient(UUID.randomUUID(), data, level, slaPolicy.minutesFor(level),
			PatientState.REGISTRADO, false, now, now, null);
		return Mono.from(patients.save(patient))
			.flatMap(saved -> persistEvent(saved, null, PatientState.REGISTRADO, "paciente_registrado"))
			.doOnSuccess(saved -> metrics.classificationRecorded(level))
			.map(saved -> toDetails(saved, List.of(), now));
	}

	public Mono<PatientDetails> changeState(UUID id, PatientState target) {
		Instant now = clock.instant();
		return findPatientRecord(id)
			.flatMap(current -> {
				if (!current.state().canTransitionTo(target)) {
					return Mono.error(new InvalidTransitionException(
						"Transición no permitida: " + current.state() + " -> " + target));
				}
				return Mono.from(patients.transition(id, current.state(), target, now))
					.flatMap(updated -> updated == 1
						? findPatientRecord(id).flatMap(patient -> persistEvent(
							patient, current.state(), target,
							target == PatientState.ALTA ? "paciente_alta" : "paciente_en_consulta"))
						: Mono.error(new InvalidTransitionException(
							"El estado del paciente cambió concurrentemente; vuelva a consultar")));
			})
			.flatMap(patient -> findAudits(id).map(history -> toDetails(patient, history, now)));
	}

	public Mono<PatientDetails> findPatient(UUID id) {
		return findPatientRecord(id)
			.flatMap(patient -> findAudits(id)
				.map(history -> toDetails(patient, history, clock.instant())));
	}

	public Mono<Void> escalateIfDue(UUID id) {
		Instant now = clock.instant();
		return Mono.from(patients.markEscalatedIfDue(id, now))
			.flatMap(updated -> {
				if (updated == 0) {
					return Mono.empty();
				}
				return findPatientRecord(id)
					.flatMap(patient -> persistEvent(patient, patient.state(), patient.state(), "paciente_escalado"))
					.doOnSuccess(ignored -> metrics.escalationRecorded())
					.then();
			});
	}

	public Flux<OperationReport> operationReport(Instant from, Instant until) {
		return Flux.from(reports.findBetween(from, until));
	}

	private Mono<Patient> findPatientRecord(UUID id) {
		return Mono.from(patients.findById(id))
			.switchIfEmpty(Mono.error(new PatientNotFoundException("No existe el paciente " + id)));
	}

	private Mono<List<PatientAudit>> findAudits(UUID id) {
		return Flux.from(audits.findByPatientId(id)).collectList();
	}

	private Mono<Patient> persistEvent(
		Patient patient, PatientState previousState, PatientState newState, String eventType
	) {
		Instant now = clock.instant();
		boolean previousEscalated = eventType.equals("paciente_escalado") ? false : patient.escalated();
		PatientAudit audit = new PatientAudit(
			UUID.randomUUID(), patient.id(), now,
			previousState == null ? null : previousState.name(),
			newState == null ? null : newState.name(),
			eventType, previousEscalated, patient.escalated()
		);
		TriageEvent event = new TriageEvent(
			UUID.randomUUID(), patient.id(), eventType, patient.level().name(),
			newState == null ? null : newState.name(), now
		);
		return Mono.from(audits.save(audit))
			.then(Mono.from(outbox.append(event)))
			.thenReturn(patient);
	}

	private PatientDetails toDetails(Patient patient, List<PatientAudit> history, Instant now) {
		long seconds = Math.max(0, Duration.between(patient.stateSince(), now).toSeconds());
		return new PatientDetails(patient, history, seconds);
	}

}
