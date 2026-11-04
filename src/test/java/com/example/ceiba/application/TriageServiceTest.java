package com.example.ceiba.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ceiba.domain.TriageClassifier;
import com.example.ceiba.domain.SlaPolicy;
import com.example.ceiba.domain.TriageLevel;
import com.example.ceiba.infrastructure.persistence.AuditEntity;
import com.example.ceiba.infrastructure.persistence.AuditRepository;
import com.example.ceiba.infrastructure.persistence.OutboxEntity;
import com.example.ceiba.infrastructure.persistence.OutboxRepository;
import com.example.ceiba.infrastructure.persistence.PatientEntity;
import com.example.ceiba.infrastructure.persistence.PatientRepository;
import com.example.ceiba.infrastructure.web.RegisterPatientRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
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
	@Mock private PatientRepository patients;
	@Mock private AuditRepository audits;
	@Mock private OutboxRepository outbox;
	private TriageService service;

	@BeforeEach
	void setUp() {
		service = new TriageService(
			patients, audits, outbox, new TriageClassifier(),
			new SlaPolicy(Map.of(
				TriageLevel.CRITICO, 5,
				TriageLevel.URGENTE, 10,
				TriageLevel.URGENTE_2, 30,
				TriageLevel.NO_URGENTE, 60,
				TriageLevel.NO_URGENTE_2, 120
			)),
			null,
			new ObjectMapper().findAndRegisterModules(), new SimpleMeterRegistry()
		);
	}

	@Test
	void rejectsInvalidStateTransitionWithoutPersisting() {
		UUID id = UUID.randomUUID();
		when(patients.findById(id)).thenReturn(Mono.just(patient(id, "ALTA")));

		StepVerifier.create(service.changeState(id, "EN_CONSULTA"))
			.expectError(InvalidTransitionException.class)
			.verify();

		verify(patients, never()).transition(any(UUID.class), anyString(), anyString(), any(Instant.class));
		verify(audits, never()).save(any(AuditEntity.class));
	}

	@Test
	void registersAndPersistsTheConfiguredSlaAndAuditEvent() {
		RegisterPatientRequest request = new RegisterPatientRequest(
			"Paciente Prueba", 45, 130, 180, 110, 97,
			new BigDecimal("36.8"), 8, 15, "Dolor torácico"
		);
		when(patients.save(any(PatientEntity.class))).thenAnswer(call ->
			Mono.just(call.getArgument(0, PatientEntity.class)));
		when(audits.save(any(AuditEntity.class))).thenAnswer(call ->
			Mono.just(call.getArgument(0, AuditEntity.class)));
		when(outbox.save(any(OutboxEntity.class))).thenAnswer(call ->
			Mono.just(call.getArgument(0, OutboxEntity.class)));

		StepVerifier.create(service.register(request))
			.assertNext(response -> {
				assertEquals("CRITICO", response.level());
				assertEquals(5, response.slaMinutes());
				assertEquals("REGISTRADO", response.state());
			})
			.verifyComplete();

		verify(audits).save(any(AuditEntity.class));
		verify(outbox).save(any(OutboxEntity.class));
	}

	@Test
	void recordsAValidStateTransitionAndItsEvent() {
		UUID id = UUID.randomUUID();
		PatientEntity registered = patient(id, "REGISTRADO");
		PatientEntity inConsultation = patient(id, "EN_CONSULTA");
		when(patients.findById(id)).thenReturn(Mono.just(registered)).thenReturn(Mono.just(inConsultation));
		when(patients.transition(any(UUID.class), anyString(), anyString(), any(Instant.class)))
			.thenReturn(Mono.just(1));
		when(audits.save(any(AuditEntity.class))).thenAnswer(call ->
			Mono.just(call.getArgument(0, AuditEntity.class)));
		when(audits.findByPatientIdOrderByTimestampAsc(id)).thenReturn(Flux.empty());
		when(outbox.save(any(OutboxEntity.class))).thenAnswer(call ->
			Mono.just(call.getArgument(0, OutboxEntity.class)));

		StepVerifier.create(service.changeState(id, "EN_CONSULTA"))
			.assertNext(response -> assertEquals("EN_CONSULTA", response.state()))
			.verifyComplete();

		verify(patients).transition(eq(id), eq("REGISTRADO"), eq("EN_CONSULTA"), any());
		verify(audits).save(any(AuditEntity.class));
		verify(outbox).save(any(OutboxEntity.class));
	}

	@Test
	void doesNotDuplicateAuditOrEventWhenEscalationWasAlreadyHandled() {
		UUID id = UUID.randomUUID();
		when(patients.markEscalatedIfDue(eq(id), any(Instant.class))).thenReturn(Mono.just(0));

		StepVerifier.create(service.escalateIfDue(patient(id, "REGISTRADO")))
			.verifyComplete();

		verify(audits, never()).save(any(AuditEntity.class));
		verify(outbox, never()).save(any(OutboxEntity.class));
	}

	private PatientEntity patient(UUID id, String state) {
		Instant now = Instant.now();
		return new PatientEntity(id, "Paciente Prueba", 40, 80, 120, 80, 98,
			new BigDecimal("36.5"), 2, 15, "Control", "NO_URGENTE", 60,
			state, false, now, now, null);
	}
}
