package com.example.ceiba.domain.model;

public enum PatientState {
	REGISTRADO,
	EN_CONSULTA,
	ALTA;

	public boolean canTransitionTo(PatientState target) {
		return this == REGISTRADO
				&& (target == EN_CONSULTA || target == ALTA)
			|| this == EN_CONSULTA && target == ALTA;
	}
}
