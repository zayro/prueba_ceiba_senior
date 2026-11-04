package com.example.ceiba.domain.port.outbound;

import com.example.ceiba.domain.model.TriageEvent;
import java.time.Instant;
import java.util.UUID;
import org.reactivestreams.Publisher;

public interface OutboxStore {
	Publisher<Void> append(TriageEvent event);

	Publisher<TriageEvent> findPending();

	Publisher<Integer> markPublished(UUID eventId, Instant publishedAt);
}
