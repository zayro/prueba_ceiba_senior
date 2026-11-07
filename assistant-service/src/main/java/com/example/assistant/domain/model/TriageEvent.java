package com.example.assistant.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TriageEvent(
	UUID eventId,
	UUID patientId,
	String type,
	String level,
	String state,
	Instant occurredAt
) {
}
