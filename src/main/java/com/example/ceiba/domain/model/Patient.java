package com.example.ceiba.domain.model;

import com.example.ceiba.domain.valueobject.TriageLevel;
import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.valueobject.PatientData;

import java.time.Instant;
import java.util.UUID;

public record Patient(
	UUID id,
	PatientData data,
	TriageLevel level,
	int slaMinutes,
	PatientState state,
	boolean escalated,
	Instant createdAt,
	Instant stateSince,
	Instant firstConsultationAt
) {
	public Patient withState(PatientState newState, Instant changedAt) {
		Instant consultationAt = newState == PatientState.EN_CONSULTA && firstConsultationAt == null
			? changedAt
			: firstConsultationAt;
		return new Patient(id, data, level, slaMinutes, newState, escalated, createdAt,
			changedAt, consultationAt);
	}

	public Patient withEscalated(boolean newEscalated) {
		return new Patient(id, data, level, slaMinutes, state, newEscalated, createdAt,
			stateSince, firstConsultationAt);
	}
}
