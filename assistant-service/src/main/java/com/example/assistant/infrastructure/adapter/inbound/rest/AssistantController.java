package com.example.assistant.infrastructure.adapter.inbound.rest;

import com.example.assistant.domain.model.AssistantResponse;
import com.example.assistant.application.service.ClinicalAssistant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/asistente")
public class AssistantController {
	private final ClinicalAssistant assistant;

	public AssistantController(ClinicalAssistant assistant) {
		this.assistant = assistant;
	}

	@PostMapping("/consulta")
	public Mono<AssistantResponse> consult(@Valid @RequestBody ConsultRequest request) {
		return assistant.consult(request.pregunta());
	}
}
