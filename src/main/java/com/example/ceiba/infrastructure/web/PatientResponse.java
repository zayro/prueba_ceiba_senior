package com.example.ceiba.infrastructure.web;

import com.example.ceiba.infrastructure.persistence.AuditEntity;
import com.example.ceiba.infrastructure.persistence.PatientEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientResponse(
	UUID id,
	String fullName,
	int age,
	int heartRate,
	int systolicPressure,
	int diastolicPressure,
	int oxygenSaturation,
	java.math.BigDecimal temperature,
	int pain,
	int gcs,
	String chiefComplaint,
	String level,
	int slaMinutes,
	String state,
	boolean escalated,
	Instant createdAt,
	Instant stateSince,
	long secondsInCurrentState,
	List<AuditEntity> audit
) {
	public static PatientResponse from(PatientEntity patient, List<AuditEntity> audit, Instant now) {
		return new PatientResponse(
			patient.id(), patient.fullName(), patient.age(), patient.heartRate(),
			patient.systolicPressure(), patient.diastolicPressure(), patient.oxygenSaturation(),
			patient.temperature(), patient.pain(), patient.gcs(), patient.chiefComplaint(),
			patient.level(), patient.slaMinutes(), patient.state(), patient.escalated(),
			patient.createdAt(), patient.stateSince(),
			Math.max(0, Duration.between(patient.stateSince(), now).toSeconds()), audit
		);
	}
}
