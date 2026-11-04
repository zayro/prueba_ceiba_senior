package com.example.ceiba.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("clinical_audit")
public record AuditEntity(
	@Id UUID id,
	@Column("patient_id") UUID patientId,
	@Column("occurred_at") Instant timestamp,
	@Column("previous_state") String previousState,
	@Column("new_state") String newState,
	@Column("event_type") String eventType,
	@Column("previous_escalated") boolean previousEscalated,
	@Column("new_escalated") boolean newEscalated
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
