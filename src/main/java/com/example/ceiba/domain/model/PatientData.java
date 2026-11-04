package com.example.ceiba.domain.model;

import java.math.BigDecimal;

public record PatientData(
	String fullName,
	int age,
	int heartRate,
	int systolicPressure,
	int diastolicPressure,
	int oxygenSaturation,
	BigDecimal temperature,
	int pain,
	int gcs,
	String chiefComplaint
) {
	public PatientData {
		if (diastolicPressure >= systolicPressure) {
			throw new IllegalArgumentException(
				"diastolicPressure debe ser menor que systolicPressure");
		}
	}
}
