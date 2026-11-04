package com.example.ceiba.domain.port;

import com.example.ceiba.domain.model.PatientAudit;
import java.util.UUID;
import org.reactivestreams.Publisher;

public interface AuditStore {
	Publisher<PatientAudit> save(PatientAudit audit);

	Publisher<PatientAudit> findByPatientId(UUID patientId);
}
