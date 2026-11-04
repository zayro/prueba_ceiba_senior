package com.example.ceiba.application.exception;

public class PatientNotFoundException extends RuntimeException {
	public PatientNotFoundException(String message) {
		super(message);
	}
}
