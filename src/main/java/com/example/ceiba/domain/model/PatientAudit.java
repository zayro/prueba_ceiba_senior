package com.example.ceiba.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PatientAudit(
	UUID id,
	UUID patientId,
	Instant timestamp,
	String previousState,
	String newState,
	String eventType,
	boolean previousEscalated,
	boolean newEscalated
) {
}
