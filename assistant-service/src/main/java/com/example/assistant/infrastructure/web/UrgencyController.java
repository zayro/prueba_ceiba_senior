package com.example.assistant.infrastructure.web;

import com.example.assistant.application.UrgencyProjection;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UrgencyController {
	private final UrgencyProjection projection;

	public UrgencyController(UrgencyProjection projection) {
		this.projection = projection;
	}

	@GetMapping("/asistente/estado-urgencias")
	public Mono<Map<String, Long>> activeUrgencies() {
		return projection.activeCounts();
	}
}
