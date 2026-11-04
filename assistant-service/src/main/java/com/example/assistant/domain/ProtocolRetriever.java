package com.example.assistant.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtocolRetriever {
	private static final Set<String> IGNORED = Set.of(
		"ante", "para", "cuando", "cuales", "cual", "que", "como", "con", "del", "las",
		"los", "una", "uno", "sobre", "protocolo", "tratamiento", "manejo", "inicial",
		"primera", "linea"
	);
	private final List<Protocol> protocols;

	public ProtocolRetriever(List<Protocol> protocols) {
		this.protocols = List.copyOf(protocols);
	}

	public List<ProtocolMatch> retrieve(String question) {
		Set<String> terms = tokenize(question);
		return protocols.stream()
			.map(protocol -> new ProtocolMatch(protocol, score(terms, tokenize(
				protocol.title() + " " + protocol.section() + " " + protocol.content()))))
			.filter(match -> match.score() > 0)
			.sorted((first, second) -> Integer.compare(second.score(), first.score()))
			.limit(3)
			.toList();
	}

	private int score(Set<String> question, Set<String> document) {
		return (int) question.stream().filter(document::contains).count();
	}

	private Set<String> tokenize(String text) {
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
		return Arrays.stream(normalized.split("[^a-z0-9]+"))
			.filter(token -> token.length() > 2 && !IGNORED.contains(token))
			.collect(Collectors.toSet());
	}
}
