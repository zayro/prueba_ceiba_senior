package com.example.assistant.application;

import com.example.assistant.domain.Protocol;
import java.util.List;
import reactor.core.publisher.Mono;

public interface ClinicalAnswerGenerator {
	Mono<String> generate(String question, List<Protocol> evidence);
}
