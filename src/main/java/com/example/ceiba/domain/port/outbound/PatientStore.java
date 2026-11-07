package com.example.ceiba.domain.port.outbound;

import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.model.Patient;
import java.time.Instant;
import java.util.UUID;
import org.reactivestreams.Publisher;

public interface PatientStore {
	Publisher<Patient> save(Patient patient);

	Publisher<Patient> findById(UUID id);

	Publisher<Patient> findByState(PatientState state);

	Publisher<Integer> transition(
		UUID id, PatientState expectedState, PatientState newState, Instant changedAt
	);

	Publisher<Integer> markEscalatedIfDue(UUID id, Instant now);
}
