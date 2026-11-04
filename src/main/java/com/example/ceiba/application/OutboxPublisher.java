package com.example.ceiba.application;

import com.example.ceiba.infrastructure.persistence.OutboxEntity;
import com.example.ceiba.infrastructure.persistence.OutboxRepository;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OutboxPublisher {
	private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
	private final OutboxRepository repository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final String topic;
	private final AtomicBoolean running = new AtomicBoolean();

	public OutboxPublisher(
		OutboxRepository repository,
		KafkaTemplate<String, String> kafkaTemplate,
		@Value("${triage.kafka.topic:triaje.eventos}") String topic
	) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	@Scheduled(fixedDelayString = "${triage.outbox.poll-interval-ms:500}")
	public void publishPending() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		repository.findPending()
			.concatMap(this::publish)
			.doOnError(error -> log.error("Failed to publish triage outbox events", error))
			.doFinally(signal -> running.set(false))
			.subscribe();
	}

	private Mono<Void> publish(OutboxEntity event) {
		return Mono.fromFuture(kafkaTemplate.send(topic, event.aggregateKey(), event.payload()))
			.then(repository.markPublished(event.id(), Instant.now()))
			.flatMap(updated -> updated == 1
				? Mono.empty()
				: Mono.error(new IllegalStateException(
					"Published event disappeared from outbox: " + event.id())));
	}
}
