package com.example.assistant.infrastructure.config;

import com.example.assistant.domain.Protocol;
import com.example.assistant.domain.ProtocolCatalog;
import com.example.assistant.domain.ProtocolRetriever;
import com.example.assistant.infrastructure.ai.RedisProtocolRetriever;
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
