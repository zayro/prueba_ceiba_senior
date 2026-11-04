package com.example.assistant.application.validation;

import com.example.assistant.domain.model.Protocol;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerGroundingValidator {
	private static final Pattern PROTOCOL_CITATION = Pattern.compile("PROTO-\\d{3}");

	public String validate(String answer, List<Protocol> evidence) {
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
			.map(AnswerGroundingValidator::normalizeWhitespace)
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

	private static String normalizeWhitespace(String text) {
		return text.replaceAll("\\s+", " ").strip();
	}
}
