package com.example.assistant.infrastructure.adapter.outbound.ai;

import com.example.assistant.domain.exception.LlmUnavailableException;
import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.model.ProtocolCatalog;
import com.example.assistant.domain.port.outbound.ProtocolRetriever;
import com.google.genai.errors.ApiException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class RedisProtocolRetriever implements ProtocolRetriever {
	private static final int MAX_RESULTS = 1;

	private final VectorStore vectorStore;
	private final Map<String, Protocol> protocolsById;
	private final double similarityThreshold;

	public RedisProtocolRetriever(
		VectorStore vectorStore,
		ProtocolCatalog protocolCatalog,
		double similarityThreshold
	) {
		if (!Double.isFinite(similarityThreshold) || similarityThreshold < 0 || similarityThreshold > 1) {
			throw new IllegalArgumentException("RAG_SIMILARITY_THRESHOLD must be between 0 and 1");
		}
		this.vectorStore = vectorStore;
		this.protocolsById = protocolCatalog.protocols().stream()
			.collect(Collectors.toUnmodifiableMap(Protocol::id, Function.identity()));
		this.similarityThreshold = similarityThreshold;
	}

	@Override
	public List<Protocol> retrieve(String question) {
		SearchRequest searchRequest = SearchRequest.builder()
			.query(question)
			.topK(MAX_RESULTS)
			.similarityThreshold(similarityThreshold)
			.build();
		List<Document> results;
		try {
			results = vectorStore.similaritySearch(searchRequest);
		} catch (ApiException error) {
			throw new LlmUnavailableException(
				"Proveedor de embeddings no disponible; no se pueden consultar protocolos", error);
		}
		return results.stream()
			.map(this::protocolFor)
			.toList();
	}

	private Protocol protocolFor(Document document) {
		Protocol protocol = protocolsById.get(document.getId());
		if (protocol == null) {
			throw new IllegalStateException("Redis vector result references unknown protocol: "
				+ document.getId());
		}
		return protocol;
	}
}
