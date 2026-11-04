package com.example.ceiba.application.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ceiba.domain.model.TriageEvent;
import com.example.ceiba.domain.port.outbound.OutboxStore;
import com.example.ceiba.domain.port.outbound.TriageEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OutboxPublishingServiceTest {
	@Mock private OutboxStore outbox;
	@Mock private TriageEventPublisher eventPublisher;

	@Test
	void marksAnEventPublishedOnlyAfterTheBrokerAcknowledgesIt() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		TriageEvent event = event(now);
		when(outbox.findPending()).thenReturn(Flux.just(event));
		when(eventPublisher.publish(event)).thenReturn(Mono.empty());
		when(outbox.markPublished(event.eventId(), now)).thenReturn(Mono.just(1));
		OutboxPublishingService service = new OutboxPublishingService(
			outbox, eventPublisher, Clock.fixed(now, ZoneOffset.UTC));

		StepVerifier.create(service.publishPending()).verifyComplete();

		InOrder order = inOrder(eventPublisher, outbox);
		order.verify(eventPublisher).publish(event);
		order.verify(outbox).markPublished(event.eventId(), now);
	}

	@Test
	void leavesAnEventPendingWhenTheBrokerPublishFails() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		TriageEvent event = event(now);
		when(outbox.findPending()).thenReturn(Flux.just(event));
		when(eventPublisher.publish(event)).thenReturn(Mono.error(new IllegalStateException("Kafka unavailable")));
		OutboxPublishingService service = new OutboxPublishingService(
			outbox, eventPublisher, Clock.fixed(now, ZoneOffset.UTC));

		StepVerifier.create(service.publishPending())
			.expectErrorMessage("Kafka unavailable")
			.verify();

		verify(outbox, never()).markPublished(event.eventId(), now);
	}

	private TriageEvent event(Instant occurredAt) {
		return new TriageEvent(
			UUID.randomUUID(), UUID.randomUUID(), "paciente_registrado", "URGENTE", "REGISTRADO",
			occurredAt
		);
	}
}
