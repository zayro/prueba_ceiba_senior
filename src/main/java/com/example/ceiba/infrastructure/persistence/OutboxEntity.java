package com.example.ceiba.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("event_outbox")
public record OutboxEntity(
	@Id UUID id,
	@Column("aggregate_key") String aggregateKey,
	String payload,
	@Column("created_at") Instant createdAt,
	@Column("published_at") Instant publishedAt
) implements Persistable<UUID> {
	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return true;
	}
}
