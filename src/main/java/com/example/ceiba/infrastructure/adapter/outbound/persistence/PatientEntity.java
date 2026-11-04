package com.example.ceiba.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("patients")
public record PatientEntity(
	@Id UUID id,
	@Column("full_name") String fullName,
	int age,
	@Column("heart_rate") int heartRate,
	@Column("systolic_pressure") int systolicPressure,
	@Column("diastolic_pressure") int diastolicPressure,
	@Column("oxygen_saturation") int oxygenSaturation,
	BigDecimal temperature,
	int pain,
	int gcs,
	@Column("chief_complaint") String chiefComplaint,
	String level,
	@Column("sla_minutes") int slaMinutes,
	String state,
	boolean escalated,
	@Column("created_at") Instant createdAt,
	@Column("state_since") Instant stateSince,
	@Column("first_consultation_at") Instant firstConsultationAt
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
