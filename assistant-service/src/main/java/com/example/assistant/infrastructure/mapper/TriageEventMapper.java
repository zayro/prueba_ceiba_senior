package com.example.assistant.infrastructure.mapper;

import com.example.assistant.domain.model.TriageEvent;
import com.example.assistant.infrastructure.adapter.messaging.dto.TriageEventMessage;
import org.springframework.stereotype.Component;

@Component
public class TriageEventMapper {
	public TriageEvent toDomain(TriageEventMessage message) {
		return new TriageEvent(
			message.eventId(),
			message.patientId(),
			message.type(),
			message.level(),
			message.state(),
			message.occurredAt()
		);
	}
}
