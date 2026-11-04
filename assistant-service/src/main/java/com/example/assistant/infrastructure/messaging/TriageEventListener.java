package com.example.assistant.infrastructure.messaging;

import com.example.assistant.application.TriageEvent;
import com.example.assistant.application.UrgencyProjection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TriageEventListener {
	private static final Logger log = LoggerFactory.getLogger(TriageEventListener.class);
	private final ObjectMapper objectMapper;
	private final UrgencyProjection projection;

	public TriageEventListener(ObjectMapper objectMapper, UrgencyProjection projection) {
		this.objectMapper = objectMapper;
		this.projection = projection;
	}

	@KafkaListener(topics = "${assistant.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
	public void consume(String payload, Acknowledgment acknowledgment) {
		TriageEvent event;
		try {
			event = objectMapper.readValue(payload, TriageEvent.class);
		} catch (JsonProcessingException error) {
			throw new IllegalArgumentException("Evento de triaje inválido", error);
		}
		projection.apply(event)
			.doOnSuccess(ignored -> acknowledgment.acknowledge())
			.doOnError(error -> log.error("Failed to update urgency projection eventId={}",
				event.eventId(), error))
			.subscribe();
	}
}
