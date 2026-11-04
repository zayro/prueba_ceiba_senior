package com.example.assistant.infrastructure.web;

import com.example.assistant.application.LlmUnavailableException;
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
public class AssistantExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(AssistantExceptionHandler.class);

	@ExceptionHandler(LlmUnavailableException.class)
	public ResponseEntity<ApiError> llmUnavailable(LlmUnavailableException error) {
		return response(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage());
	}

	@ExceptionHandler({WebExchangeBindException.class, ServerWebInputException.class})
	public ResponseEntity<ApiError> badRequest(Exception error) {
		String message = error instanceof WebExchangeBindException binding
			? binding.getFieldErrors().stream()
				.map(field -> field.getField() + ": " + field.getDefaultMessage())
				.findFirst().orElse("Solicitud inválida")
			: error.getMessage();
		return response(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> status(ResponseStatusException error) {
		return response(HttpStatus.valueOf(error.getStatusCode().value()), error.getReason());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> internalError(Exception error) {
		log.error("Unhandled assistant request failure", error);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servicio");
	}

	private ResponseEntity<ApiError> response(HttpStatus status, String message) {
		return ResponseEntity.status(status)
			.body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
	}
}
