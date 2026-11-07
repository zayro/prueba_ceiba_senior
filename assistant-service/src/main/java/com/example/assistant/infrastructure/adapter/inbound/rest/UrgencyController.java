package com.example.assistant.infrastructure.adapter.inbound.rest;

import com.example.assistant.application.service.UrgencyService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UrgencyController {
	private final UrgencyService urgencyService;

	public UrgencyController(UrgencyService urgencyService) {
		this.urgencyService = urgencyService;
	}

	@GetMapping("/asistente/estado-urgencias")
	public Mono<Map<String, Long>> activeUrgencies() {
		return urgencyService.activeCounts();
	}
}
