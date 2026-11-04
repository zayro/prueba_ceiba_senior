package com.example.ceiba.domain;

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
}
