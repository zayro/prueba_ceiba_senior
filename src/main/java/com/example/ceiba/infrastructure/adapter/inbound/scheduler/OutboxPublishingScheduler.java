package com.example.ceiba.infrastructure.adapter.scheduler;

import com.example.ceiba.domain.port.OutboxPublishingUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OutboxPublishingScheduler {
	private static final Logger log = LoggerFactory.getLogger(OutboxPublishingScheduler.class);
	private final OutboxPublishingUseCase publisher;

	public OutboxPublishingScheduler(OutboxPublishingUseCase publisher) {
		this.publisher = publisher;
	}

	@Scheduled(fixedDelayString = "${triage.outbox.poll-interval-ms:500}")
	public void publishPending() {
		Mono.from(publisher.publishPending())
			.doOnError(error -> log.error("Failed to publish triage outbox events", error))
			.subscribe();
	}
}
