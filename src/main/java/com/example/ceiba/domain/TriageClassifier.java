package com.example.ceiba.domain;

import java.util.Set;
import java.text.Normalizer;

public class TriageClassifier {
	private static final Set<String> ADMINISTRATIVE_COMPLAINTS = Set.of(
		"documentos", "recoleccion de resultados", "renovacion de recetario",
		"vacunacion", "seguimiento"
	);

	public TriageLevel classify(PatientData patient) {
		if (patient.heartRate() < 40 || patient.heartRate() > 120
			|| patient.oxygenSaturation() < 90 || patient.systolicPressure() < 90
			|| patient.gcs() <= 8) {
			return TriageLevel.CRITICO;
		}
		if (between(patient.oxygenSaturation(), 90, 93)
			|| between(patient.systolicPressure(), 90, 100)
			|| patient.temperature().compareTo(new java.math.BigDecimal("38.5")) >= 0
			|| between(patient.gcs(), 9, 13)) {
			return TriageLevel.URGENTE;
		}
		if (between(patient.heartRate(), 90, 120)
			|| (patient.temperature().compareTo(new java.math.BigDecimal("38.0")) >= 0
				&& patient.temperature().compareTo(new java.math.BigDecimal("38.5")) < 0)
			|| patient.pain() >= 7) {
			return TriageLevel.URGENTE_2;
		}
		if (ADMINISTRATIVE_COMPLAINTS.contains(normalize(patient.chiefComplaint()))) {
			return TriageLevel.NO_URGENTE_2;
		}
		return TriageLevel.NO_URGENTE;
	}

	private boolean between(int value, int lower, int upper) {
		return value >= lower && value <= upper;
	}

	private String normalize(String value) {
		return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(java.util.Locale.ROOT);
	}
}
