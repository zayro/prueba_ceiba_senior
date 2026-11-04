package com.example.assistant.infrastructure.adapter.ai;

import com.example.assistant.application.exception.LlmUnavailableException;
import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.port.ClinicalAnswerGenerator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SpringAiAnswerGenerator implements ClinicalAnswerGenerator {
	private final ChatClient chatClient;
	private final CircuitBreaker circuitBreaker;
	private final MeterRegistry meterRegistry;
	private final String apiKey;

	public SpringAiAnswerGenerator(
		ChatClient.Builder chatClientBuilder,
		CircuitBreaker circuitBreaker,
		MeterRegistry meterRegistry,
		@Value("${spring.ai.google.genai.api-key:}") String apiKey
	) {
		this.chatClient = chatClientBuilder.build();
		this.circuitBreaker = circuitBreaker;
		this.meterRegistry = meterRegistry;
		this.apiKey = apiKey;
	}

	@Override
	public Mono<String> generate(String question, List<Protocol> evidence) {
		if (apiKey.isBlank()) {
			return Mono.error(new LlmUnavailableException(
				"Proveedor de IA no configurado; defina LLM_API_KEY"));
		}
		String context = evidence.stream()
			.map(protocol -> protocol.id() + " — " + protocol.title() + " / "
				+ protocol.section() + ": " + protocol.content())
			.collect(java.util.stream.Collectors.joining("\n\n"));
		String systemInstruction = """
			Eres un asistente de consulta de protocolos internos. Responde copiando literalmente
			una o mas frases completas de la evidencia. No reformules, infieras ni agregues
			recomendaciones, diagnósticos o datos externos. Cita el identificador de cada frase
			con el formato [PROTO-001]. Si la evidencia no contiene la respuesta, responde
			exactamente: No existe un protocolo disponible para la pregunta.
			Evidencia oficial:
			""" + context;
		Timer.Sample sample = Timer.start(meterRegistry);
		return Mono.defer(() -> chatClient.prompt()
				.system(systemInstruction)
				.user(question)
				.stream()
				.content()
				.collectList()
				.map(parts -> String.join("", parts).strip())
				.filter(answer -> !answer.isBlank())
				.switchIfEmpty(Mono.error(new LlmUnavailableException("El proveedor de IA devolvió una respuesta vacía"))))
			.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
			.onErrorMap(error -> error instanceof LlmUnavailableException
				? error
				: new LlmUnavailableException("Proveedor de IA no disponible", error))
			.doFinally(signal -> sample.stop(meterRegistry.timer("assistant.llm.latency")));
	}
}
