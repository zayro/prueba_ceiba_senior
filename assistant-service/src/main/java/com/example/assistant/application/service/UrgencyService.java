package com.example.assistant.application.service;

import com.example.assistant.domain.model.TriageEvent;
import com.example.assistant.domain.port.ActiveUrgencyProjection;
import java.util.Map;
import reactor.core.publisher.Mono;

public class UrgencyService {
	private final ActiveUrgencyProjection projection;

	public UrgencyService(ActiveUrgencyProjection projection) {
		this.projection = projection;
	}

	public Mono<Long> apply(TriageEvent event) {
		return Mono.from(projection.apply(event));
	}

	public Mono<Map<String, Long>> activeCounts() {
		return Mono.from(projection.activeCounts());
	}
}
