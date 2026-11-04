package com.example.ceiba.domain.exception;

public class PatientNotFoundException extends RuntimeException {
	public PatientNotFoundException(String message) {
		super(message);
	}
}
