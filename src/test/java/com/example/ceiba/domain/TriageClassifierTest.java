package com.example.ceiba.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TriageClassifierTest {
	private final TriageClassifier classifier = new TriageClassifier();

	@Test
	void classifiesReferencePatientsE1ToE6() {
		assertEquals(TriageLevel.CRITICO, classify(130, 180, 110, 97, "36.8", 8, 15, "Dolor torácico"));
		assertEquals(TriageLevel.URGENTE, classify(95, 150, 95, 91, "37.0", 4, 15, "Dificultad para respirar"));
		assertEquals(TriageLevel.URGENTE_2, classify(88, 130, 85, 98, "38.2", 6, 15, "Fiebre"));
		assertEquals(TriageLevel.NO_URGENTE, classify(72, 120, 80, 99, "36.5", 2, 15, "Herida por cortar"));
		assertEquals(TriageLevel.NO_URGENTE_2, classify(70, 115, 75, 99, "36.4", 0, 15, "Renovación de recetario"));
		assertEquals(TriageLevel.NO_URGENTE_2, classify(70, 115, 75, 99, "36.4", 0, 15, "VACUNACION"));
		assertEquals(TriageLevel.CRITICO, classify(35, 80, 50, 85, "35.9", 0, 6, "Inconsciente"));
	}

	private TriageLevel classify(
		int heartRate, int systolic, int diastolic, int oxygen, String temperature,
		int pain, int gcs, String complaint
	) {
		return classifier.classify(new PatientData(
			"Paciente de prueba", 40, heartRate, systolic, diastolic, oxygen,
			new BigDecimal(temperature), pain, gcs, complaint
		));
	}
}
