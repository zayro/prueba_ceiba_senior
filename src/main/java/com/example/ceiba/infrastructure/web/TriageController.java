package com.example.ceiba.infrastructure.web;

import com.example.ceiba.domain.model.PatientState;
import com.example.ceiba.domain.port.TriageUseCases;
import com.example.ceiba.infrastructure.mapper.TriageWebMapper;
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
	private final TriageUseCases triageUseCases;
	private final TriageWebMapper mapper;

	public TriageController(TriageUseCases triageUseCases, TriageWebMapper mapper) {
		this.triageUseCases = triageUseCases;
		this.mapper = mapper;
	}

	@PostMapping("/pacientes")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<PatientResponse> register(@Valid @RequestBody RegisterPatientRequest request) {
		return Mono.from(triageUseCases.register(mapper.toDomain(request))).map(mapper::toResponse);
	}

	@PatchMapping("/pacientes/{id}/estado")
	public Mono<PatientResponse> changeState(
		@PathVariable UUID id, @Valid @RequestBody ChangeStateRequest request
	) {
		return Mono.from(triageUseCases.changeState(id, PatientState.valueOf(request.state())))
			.map(mapper::toResponse);
	}

	@GetMapping("/pacientes/{id}")
	public Mono<PatientResponse> findPatient(@PathVariable UUID id) {
		return Mono.from(triageUseCases.findPatient(id)).map(mapper::toResponse);
	}

	@GetMapping("/reportes/operacion")
	public Flux<OperationReportRow> operationReport(
		@RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
		@RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta
	) {
		if (!desde.isBefore(hasta)) {
			return Flux.error(new IllegalArgumentException("desde debe ser anterior a hasta"));
		}
		return Flux.from(triageUseCases.operationReport(desde, hasta)).map(mapper::toResponse);
	}
}
