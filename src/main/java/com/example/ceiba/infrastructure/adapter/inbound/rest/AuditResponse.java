package com.example.ceiba.infrastructure.adapter.inbound.rest;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
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
