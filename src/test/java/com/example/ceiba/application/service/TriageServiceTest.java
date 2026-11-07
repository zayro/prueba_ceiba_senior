package com.example.ceiba.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ceiba.domain.exception.InvalidTransitionException;
import com.example.ceiba.domain.model.Patient;
import com.example.ceiba.domain.model.PatientAudit;
import com.example.ceiba.domain.valueobject.PatientData;
import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.valueobject.SlaPolicy;
import com.example.ceiba.domain.model.TriageClassifier;
import com.example.ceiba.domain.valueobject.TriageLevel;
import com.example.ceiba.domain.port.outbound.AuditStore;
import com.example.ceiba.domain.port.outbound.OperationReportStore;
import com.example.ceiba.domain.port.outbound.OutboxStore;
import com.example.ceiba.domain.port.outbound.PatientStore;
import com.example.ceiba.domain.port.outbound.TriageMetrics;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {
	@Mock private PatientStore patients;
	@Mock private AuditStore audits;
	@Mock private OutboxStore outbox;
	@Mock private OperationReportStore reports;
	@Mock private TriageMetrics metrics;
	private TriageService service;

	@BeforeEach
	void setUp() {
		service = new TriageService(
			patients, audits, outbox, reports, new TriageClassifier(),
			new SlaPolicy(Map.of(
				TriageLevel.CRITICO, 5,
				TriageLevel.URGENTE, 10,
				TriageLevel.URGENTE_2, 30,
				TriageLevel.NO_URGENTE, 60,
				TriageLevel.NO_URGENTE_2, 120
			)), metrics, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
		);
	}

	@Test
	void rejectsInvalidStateTransitionWithoutPersisting() {
		UUID id = UUID.randomUUID();
		when(patients.findById(id)).thenReturn(Mono.just(patient(id, PatientState.ALTA)));

		StepVerifier.create(service.changeState(id, PatientState.EN_CONSULTA))
			.expectError(InvalidTransitionException.class)
			.verify();

		verify(patients, never()).transition(any(UUID.class), any(), any(), any(Instant.class));
		verify(audits, never()).save(any(PatientAudit.class));
	}

	@Test
	void registersAndPersistsTheConfiguredSlaAndAuditEvent() {
		PatientData request = new PatientData(
			"Paciente Prueba", 45, 130, 180, 110, 97,
			new BigDecimal("36.8"), 8, 15, "Dolor torácico"
		);
		when(patients.save(any(Patient.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
		when(audits.save(any(PatientAudit.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
		when(outbox.append(any())).thenReturn(Mono.empty());

		StepVerifier.create(service.register(request))
			.assertNext(response -> {
				assertEquals("CRITICO", response.patient().level().name());
				assertEquals(5, response.patient().slaMinutes());
				assertEquals("REGISTRADO", response.patient().state().name());
			})
			.verifyComplete();

		verify(audits).save(any(PatientAudit.class));
		verify(outbox).append(any());
	}

	@Test
	void recordsAValidStateTransitionAndItsEvent() {
		UUID id = UUID.randomUUID();
		Patient registered = patient(id, PatientState.REGISTRADO);
		Patient inConsultation = patient(id, PatientState.EN_CONSULTA);
		when(patients.findById(id)).thenReturn(Mono.just(registered)).thenReturn(Mono.just(inConsultation));
		when(patients.transition(any(UUID.class), any(), any(), any(Instant.class)))
			.thenReturn(Mono.just(1));
		when(audits.save(any(PatientAudit.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
		when(audits.findByPatientId(id)).thenReturn(Flux.empty());
		when(outbox.append(any())).thenReturn(Mono.empty());

		StepVerifier.create(service.changeState(id, PatientState.EN_CONSULTA))
			.assertNext(response -> assertEquals("EN_CONSULTA", response.patient().state().name()))
			.verifyComplete();

		verify(patients).transition(eq(id), eq(PatientState.REGISTRADO),
			eq(PatientState.EN_CONSULTA), any());
		verify(audits).save(any(PatientAudit.class));
		verify(outbox).append(any());
	}

	@Test
	void doesNotDuplicateAuditOrEventWhenEscalationWasAlreadyHandled() {
		UUID id = UUID.randomUUID();
		when(patients.markEscalatedIfDue(eq(id), any(Instant.class))).thenReturn(Mono.just(0));

		StepVerifier.create(service.escalateIfDue(id))
			.verifyComplete();

		verify(audits, never()).save(any(PatientAudit.class));
		verify(outbox, never()).append(any());
	}

	private Patient patient(UUID id, PatientState state) {
		Instant now = Instant.now();
		PatientData data = new PatientData("Paciente Prueba", 40, 80, 120, 80, 98,
			new BigDecimal("36.5"), 2, 15, "Control");
		return new Patient(id, data, TriageLevel.NO_URGENTE, 60, state, false, now, now, null);
	}
}
