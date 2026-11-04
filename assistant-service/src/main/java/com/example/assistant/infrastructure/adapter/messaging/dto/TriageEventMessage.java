package com.example.assistant.infrastructure.adapter.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record TriageEventMessage(
	UUID eventId,
	UUID patientId,
	String type,
	String level,
	String state,
	Instant occurredAt
) {
}
