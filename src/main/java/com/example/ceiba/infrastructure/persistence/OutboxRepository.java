package com.example.ceiba.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxEntity, UUID> {
	@Query("SELECT * FROM event_outbox WHERE published_at IS NULL ORDER BY created_at LIMIT 100")
	Flux<OutboxEntity> findPending();

	@Modifying
	@Query("UPDATE event_outbox SET published_at = :publishedAt WHERE id = :id AND published_at IS NULL")
	Mono<Integer> markPublished(
		@Param("id") UUID id,
		@Param("publishedAt") Instant publishedAt
	);
}
