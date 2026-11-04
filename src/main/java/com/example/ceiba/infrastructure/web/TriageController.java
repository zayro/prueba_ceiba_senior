package com.example.ceiba.infrastructure.web;

import com.example.ceiba.application.TriageService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/triage")
public class TriageController {
	private final TriageService triageService;

	public TriageController(TriageService triageService) {
		this.triageService = triageService;
	}

	@PostMapping("/pacientes")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<PatientResponse> register(@Valid @RequestBody RegisterPatientRequest request) {
		return triageService.register(request);
	}

	@PatchMapping("/pacientes/{id}/estado")
	public Mono<PatientResponse> changeState(
		@PathVariable UUID id, @Valid @RequestBody ChangeStateRequest request
	) {
		return triageService.changeState(id, request.state());
	}

	@GetMapping("/pacientes/{id}")
	public Mono<PatientResponse> findPatient(@PathVariable UUID id) {
		return triageService.findPatient(id);
	}

	@GetMapping("/reportes/operacion")
	public Flux<OperationReportRow> operationReport(
		@RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
		@RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta
	) {
		if (!desde.isBefore(hasta)) {
			return Flux.error(new IllegalArgumentException("desde debe ser anterior a hasta"));
		}
		return triageService.operationReport(desde, hasta);
	}
}
