package com.example.ceiba.application.service;

import com.example.ceiba.domain.model.TriageEvent;
import com.example.ceiba.domain.port.OutboxPublishingUseCase;
import com.example.ceiba.domain.port.OutboxStore;
import com.example.ceiba.domain.port.TriageEventPublisher;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OutboxPublishingService implements OutboxPublishingUseCase {
	private final OutboxStore outbox;
	private final TriageEventPublisher eventPublisher;
	private final Clock clock;
	private final AtomicBoolean running = new AtomicBoolean();

	public OutboxPublishingService(
		OutboxStore outbox,
		TriageEventPublisher eventPublisher,
		Clock clock
	) {
		this.outbox = outbox;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	@Override
	public Mono<Void> publishPending() {
		return Mono.defer(() -> {
			if (!running.compareAndSet(false, true)) {
				return Mono.empty();
			}
			return Flux.from(outbox.findPending())
				.concatMap(this::publish)
				.then()
				.doFinally(signal -> running.set(false));
		});
	}

	private Mono<Void> publish(TriageEvent event) {
		UUID eventId = event.eventId();
		return Mono.from(eventPublisher.publish(event))
			.then(Mono.defer(() -> Mono.from(outbox.markPublished(eventId, clock.instant()))))
			.flatMap(updated -> updated == 1
				? Mono.empty()
				: Mono.error(new IllegalStateException(
					"Published event disappeared from outbox: " + eventId)));
	}
}
