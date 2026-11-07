package com.example.ceiba.infrastructure.adapter.inbound.rest;

import com.example.ceiba.domain.exception.InvalidTransitionException;
import com.example.ceiba.domain.exception.PatientNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler({WebExchangeBindException.class, ServerWebInputException.class,
		ConstraintViolationException.class, IllegalArgumentException.class})
	public ResponseEntity<ApiError> badRequest(Exception error) {
		String message = error instanceof WebExchangeBindException binding
			? binding.getFieldErrors().stream()
				.map(field -> field.getField() + ": " + field.getDefaultMessage())
				.findFirst().orElse("Solicitud inválida")
			: error.getMessage();
		return response(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(PatientNotFoundException.class)
	public ResponseEntity<ApiError> notFound(PatientNotFoundException error) {
		return response(HttpStatus.NOT_FOUND, error.getMessage());
	}

	@ExceptionHandler(InvalidTransitionException.class)
	public ResponseEntity<ApiError> conflict(InvalidTransitionException error) {
		return response(HttpStatus.CONFLICT, error.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> status(ResponseStatusException error) {
		return response(HttpStatus.valueOf(error.getStatusCode().value()), error.getReason());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> internalError(Exception error) {
		log.error("Unhandled triage request failure", error);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servicio");
	}

	private ResponseEntity<ApiError> response(HttpStatus status, String message) {
		return ResponseEntity.status(status)
			.body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
	}
}
