package com.example.ceiba.infrastructure.mapper;

import com.example.ceiba.domain.model.OperationReport;
import com.example.ceiba.domain.model.PatientAudit;
import com.example.ceiba.domain.valueobject.PatientData;
import com.example.ceiba.domain.model.PatientDetails;
import com.example.ceiba.infrastructure.adapter.inbound.rest.AuditResponse;
import com.example.ceiba.infrastructure.adapter.inbound.rest.OperationReportRow;
import com.example.ceiba.infrastructure.adapter.inbound.rest.PatientResponse;
import com.example.ceiba.infrastructure.adapter.inbound.rest.RegisterPatientRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TriageWebMapper {
	public PatientData toDomain(RegisterPatientRequest request) {
		return new PatientData(
			request.fullName(), request.age(), request.heartRate(), request.systolicPressure(),
			request.diastolicPressure(), request.oxygenSaturation(), request.temperature(),
			request.pain(), request.gcs(), request.chiefComplaint()
		);
	}

	public PatientResponse toResponse(PatientDetails details) {
		var patient = details.patient();
		var data = patient.data();
		List<AuditResponse> history = details.audit().stream().map(this::toResponse).toList();
		return new PatientResponse(
			patient.id(), data.fullName(), data.age(), data.heartRate(), data.systolicPressure(),
			data.diastolicPressure(), data.oxygenSaturation(), data.temperature(), data.pain(),
			data.gcs(), data.chiefComplaint(), patient.level().name(), patient.slaMinutes(),
			patient.state().name(), patient.escalated(), patient.createdAt(), patient.stateSince(),
			details.secondsInCurrentState(), history
		);
	}

	public OperationReportRow toResponse(OperationReport report) {
		return new OperationReportRow(
			report.hour(), report.total(), report.critico(), report.urgente(), report.urgente2(),
			report.noUrgente(), report.noUrgente2(), report.averageWaitMinutes(),
			report.p95WaitMinutes()
		);
	}

	private AuditResponse toResponse(PatientAudit audit) {
		return new AuditResponse(
			audit.id(), audit.patientId(), audit.timestamp(), audit.previousState(), audit.newState(),
			audit.eventType(), audit.previousEscalated(), audit.newEscalated()
		);
	}
}
