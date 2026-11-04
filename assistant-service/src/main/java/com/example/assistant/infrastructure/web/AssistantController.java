package com.example.assistant.infrastructure.web;

import com.example.assistant.application.AssistantResponse;
import com.example.assistant.application.ClinicalAssistant;
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
