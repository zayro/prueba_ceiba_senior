package com.example.ceiba.domain.model;

import com.example.ceiba.domain.valueobject.TriageLevel;
import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.valueobject.PatientData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void rejectsDiastolicPressureThatIsNotBelowSystolicPressure() {
		assertThrows(IllegalArgumentException.class, () -> new PatientData(
			"Paciente de prueba", 40, 80, 120, 120, 98,
			new BigDecimal("36.5"), 2, 15, "Control"
		));
	}

	@Test
	void onlyAllowsTheSupportedPatientStateTransitions() {
		assertTrue(PatientState.REGISTRADO.canTransitionTo(PatientState.EN_CONSULTA));
		assertTrue(PatientState.REGISTRADO.canTransitionTo(PatientState.ALTA));
		assertFalse(PatientState.ALTA.canTransitionTo(PatientState.EN_CONSULTA));
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
