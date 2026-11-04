package com.example.ceiba.infrastructure.adapter.inbound.rest;

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
	List<AuditResponse> audit
) {
}
