package com.example.assistant.application;

import com.example.assistant.domain.Protocol;
import com.example.assistant.domain.ProtocolRetriever;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ClinicalAssistant {
	private static final Pattern PROTOCOL_CITATION = Pattern.compile("PROTO-\\d{3}");
	private static final String NO_PROTOCOL =
		"No existe un protocolo disponible para la pregunta.";
	private final ProtocolRetriever retriever;
	private final ClinicalAnswerGenerator answerGenerator;

	public ClinicalAssistant(ProtocolRetriever retriever, ClinicalAnswerGenerator answerGenerator) {
		this.retriever = retriever;
		this.answerGenerator = answerGenerator;
	}

	public Mono<AssistantResponse> consult(String question) {
		return Mono.fromCallable(() -> retriever.retrieve(question))
			.subscribeOn(Schedulers.boundedElastic())
			.flatMap(evidence -> {
				if (evidence.isEmpty()) {
					return Mono.just(new AssistantResponse(NO_PROTOCOL, List.of()));
				}
				List<ProtocolSource> sources = evidence.stream()
					.map(protocol -> new ProtocolSource(protocol.id(), protocol.section()))
					.toList();
				return answerGenerator.generate(question, evidence)
					.map(answer -> new AssistantResponse(validateCitation(answer, evidence), sources));
			});
	}

	private String validateCitation(String answer, List<Protocol> evidence) {
		Matcher matcher = PROTOCOL_CITATION.matcher(answer);
		Set<String> allowedIds = evidence.stream().map(Protocol::id)
			.collect(java.util.stream.Collectors.toSet());
		boolean citationFound = false;
		while (matcher.find()) {
			citationFound = true;
			if (!allowedIds.contains(matcher.group())) {
				return extractiveFallback(evidence);
			}
		}
		String answerWithoutCitations = PROTOCOL_CITATION.matcher(answer).replaceAll("")
			.strip();
		boolean textIsGrounded = evidence.stream()
			.map(Protocol::content)
			.map(this::normalizeWhitespace)
			.anyMatch(content -> content.contains(normalizeWhitespace(answerWithoutCitations)));
		return citationFound && !answerWithoutCitations.isBlank() && textIsGrounded
			? answer
			: extractiveFallback(evidence);
	}

	private String extractiveFallback(List<Protocol> evidence) {
		return evidence.stream()
			.map(protocol -> protocol.content() + " [" + protocol.id() + "]")
			.collect(java.util.stream.Collectors.joining("\n\n"));
	}

	private String normalizeWhitespace(String text) {
		return text.replaceAll("\\s+", " ").strip();
	}
}
