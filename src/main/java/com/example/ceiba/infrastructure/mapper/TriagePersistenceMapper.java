package com.example.ceiba.infrastructure.mapper;

import com.example.ceiba.domain.model.Patient;
import com.example.ceiba.domain.model.PatientAudit;
import com.example.ceiba.domain.model.PatientData;
import com.example.ceiba.domain.model.PatientState;
import com.example.ceiba.domain.model.TriageLevel;
import com.example.ceiba.infrastructure.persistence.AuditEntity;
import com.example.ceiba.infrastructure.persistence.PatientEntity;
import org.springframework.stereotype.Component;

@Component
public class TriagePersistenceMapper {
	public PatientEntity toEntity(Patient patient) {
		PatientData data = patient.data();
		return new PatientEntity(
			patient.id(), data.fullName(), data.age(), data.heartRate(), data.systolicPressure(),
			data.diastolicPressure(), data.oxygenSaturation(), data.temperature(), data.pain(),
			data.gcs(), data.chiefComplaint(), patient.level().name(), patient.slaMinutes(),
			patient.state().name(), patient.escalated(), patient.createdAt(), patient.stateSince(),
			patient.firstConsultationAt()
		);
	}

	public Patient toDomain(PatientEntity entity) {
		PatientData data = new PatientData(
			entity.fullName(), entity.age(), entity.heartRate(), entity.systolicPressure(),
			entity.diastolicPressure(), entity.oxygenSaturation(), entity.temperature(),
			entity.pain(), entity.gcs(), entity.chiefComplaint()
		);
		return new Patient(
			entity.id(), data, TriageLevel.valueOf(entity.level()), entity.slaMinutes(),
			PatientState.valueOf(entity.state()), entity.escalated(), entity.createdAt(),
			entity.stateSince(), entity.firstConsultationAt()
		);
	}

	public AuditEntity toEntity(PatientAudit audit) {
		return new AuditEntity(
			audit.id(), audit.patientId(), audit.timestamp(), audit.previousState(), audit.newState(),
			audit.eventType(), audit.previousEscalated(), audit.newEscalated()
		);
	}

	public PatientAudit toDomain(AuditEntity entity) {
		return new PatientAudit(
			entity.id(), entity.patientId(), entity.timestamp(), entity.previousState(),
			entity.newState(), entity.eventType(), entity.previousEscalated(), entity.newEscalated()
		);
	}
}
