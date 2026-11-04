package com.example.ceiba.domain.model;

import java.util.List;

public record PatientDetails(Patient patient, List<PatientAudit> audit, long secondsInCurrentState) {
	public PatientDetails {
		audit = List.copyOf(audit);
	}
}
