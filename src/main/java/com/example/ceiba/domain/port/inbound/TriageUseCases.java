package com.example.ceiba.domain.port.inbound;

import com.example.ceiba.domain.model.OperationReport;
import com.example.ceiba.domain.valueobject.PatientData;
import com.example.ceiba.domain.model.PatientDetails;
import com.example.ceiba.domain.valueobject.PatientState;
import java.time.Instant;
import java.util.UUID;
import org.reactivestreams.Publisher;

public interface TriageUseCases {
	Publisher<PatientDetails> register(PatientData patientData);

	Publisher<PatientDetails> changeState(UUID id, PatientState state);

	Publisher<PatientDetails> findPatient(UUID id);

	Publisher<Void> escalateIfDue(UUID id);

	Publisher<OperationReport> operationReport(Instant from, Instant until);
}
