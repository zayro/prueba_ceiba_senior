package com.example.ceiba.infrastructure.adapter.messaging;

import com.example.ceiba.domain.model.TriageEvent;
import com.example.ceiba.domain.port.TriageEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaTriageEventPublisher implements TriageEventPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;

	public KafkaTriageEventPublisher(
		KafkaTemplate<String, String> kafkaTemplate,
		ObjectMapper objectMapper,
		@Value("${triage.kafka.topic:triaje.eventos}") String topic
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	@Override
	public Mono<Void> publish(TriageEvent event) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException error) {
			return Mono.error(new IllegalStateException("No fue posible serializar el evento clínico", error));
		}
		return Mono.fromFuture(kafkaTemplate.send(topic, event.patientId().toString(), payload)).then();
	}
}
