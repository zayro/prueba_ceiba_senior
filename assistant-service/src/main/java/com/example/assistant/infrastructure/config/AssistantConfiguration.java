package com.example.assistant.infrastructure.config;

import com.example.assistant.application.service.ClinicalAssistant;
import com.example.assistant.application.service.UrgencyService;
import com.example.assistant.application.validation.AnswerGroundingValidator;
import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.model.ProtocolCatalog;
import com.example.assistant.domain.port.ActiveUrgencyProjection;
import com.example.assistant.domain.port.ClinicalAnswerGenerator;
import com.example.assistant.domain.port.ProtocolRetriever;
import com.example.assistant.infrastructure.adapter.ai.RedisProtocolRetriever;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class AssistantConfiguration {
	@Bean
	AnswerGroundingValidator answerGroundingValidator() {
		return new AnswerGroundingValidator();
	}

	@Bean
	ClinicalAssistant clinicalAssistant(
		ProtocolRetriever protocolRetriever,
		ClinicalAnswerGenerator answerGenerator,
		AnswerGroundingValidator answerGroundingValidator
	) {
		return new ClinicalAssistant(protocolRetriever, answerGenerator, answerGroundingValidator);
	}

	@Bean
	UrgencyService urgencyService(ActiveUrgencyProjection projection) {
		return new UrgencyService(projection);
	}

	@Bean
	ProtocolCatalog protocolCatalog(ObjectMapper objectMapper) throws IOException {
		List<Protocol> protocols = objectMapper.readValue(
			new ClassPathResource("clinical-protocols.json").getInputStream(),
			new TypeReference<>() { }
		);
		return new ProtocolCatalog(protocols);
	}

	@Bean
	ProtocolRetriever protocolRetriever(
		VectorStore vectorStore,
		ProtocolCatalog protocolCatalog,
		@Value("${assistant.rag.similarity-threshold:0.82}") double similarityThreshold
	) {
		return new RedisProtocolRetriever(vectorStore, protocolCatalog, similarityThreshold);
	}

	@Bean
	CircuitBreaker llmCircuitBreaker(MeterRegistry meterRegistry) {
		CircuitBreaker breaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
			.slidingWindowSize(10)
			.minimumNumberOfCalls(4)
			.failureRateThreshold(50)
			.waitDurationInOpenState(Duration.ofSeconds(30))
			.build()).circuitBreaker("clinical-llm");
		breaker.getEventPublisher().onStateTransition(event ->
			meterRegistry.counter("assistant.llm.circuit.transitions",
				"from", event.getStateTransition().getFromState().name(),
				"to", event.getStateTransition().getToState().name()).increment());
		return breaker;
	}
}
