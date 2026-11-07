package com.example.assistant.infrastructure.adapter.outbound.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.assistant.domain.exception.LlmUnavailableException;
import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.model.ProtocolCatalog;
import com.google.genai.errors.ClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RedisProtocolRetrieverTest {
	@Test
	void mapsVectorSearchResultsBackToProtocolEvidence() {
		VectorStore vectorStore = mock(VectorStore.class);
		Document document = Document.builder()
			.id("PROTO-001")
			.text("Crisis Hipertensiva. Manejo inicial. Reconfirmar la presión.")
			.build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
		Protocol expectedProtocol = new Protocol(
			"PROTO-001", "Crisis Hipertensiva", "Manejo inicial", "Reconfirmar la presión."
		);
		RedisProtocolRetriever retriever = new RedisProtocolRetriever(
			vectorStore, new ProtocolCatalog(List.of(expectedProtocol)), 0.55, "test-key",
			CircuitBreaker.ofDefaults("test-embeddings"));

		assertEquals(List.of(expectedProtocol), retriever.retrieve("¿Qué hacer ante una presión elevada?"));
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void rejectsSimilarityThresholdOutsideTheSupportedRange() {
		VectorStore vectorStore = mock(VectorStore.class);

		assertThrows(IllegalArgumentException.class,
			() -> new RedisProtocolRetriever(vectorStore, new ProtocolCatalog(List.of()),
				1.1, "test-key", CircuitBreaker.ofDefaults("test-embeddings")));
	}

	@Test
	void reportsUnavailableEmbeddingProviderWhenVectorSearchFails() {
		VectorStore vectorStore = mock(VectorStore.class);
		doThrow(new ClientException(400, "INVALID_ARGUMENT", "API key not valid"))
			.when(vectorStore).similaritySearch(any(SearchRequest.class));
		RedisProtocolRetriever retriever = new RedisProtocolRetriever(
			vectorStore, new ProtocolCatalog(List.of()), 0.55, "test-key",
			CircuitBreaker.ofDefaults("test-embeddings"));

		LlmUnavailableException error = assertThrows(LlmUnavailableException.class,
			() -> retriever.retrieve("¿Qué hacer ante una presión elevada?"));

		assertTrue(error.getMessage().contains("Proveedor de embeddings no disponible"));
		assertTrue(error.getCause() instanceof ClientException);
	}

	@Test
	void opensCircuitAfterEmbeddingFailuresAndReturnsExplicitUnavailableError() {
		VectorStore vectorStore = mock(VectorStore.class);
		doThrow(new ClientException(400, "INVALID_ARGUMENT", "API key not valid"))
			.when(vectorStore).similaritySearch(any(SearchRequest.class));
		CircuitBreaker circuitBreaker = CircuitBreaker.of("test-embeddings", CircuitBreakerConfig.custom()
			.slidingWindowSize(1)
			.minimumNumberOfCalls(1)
			.failureRateThreshold(50)
			.build());
		RedisProtocolRetriever retriever = new RedisProtocolRetriever(
			vectorStore, new ProtocolCatalog(List.of()), 0.55, "test-key", circuitBreaker);

		assertThrows(LlmUnavailableException.class,
			() -> retriever.retrieve("¿Qué hacer ante una presión elevada?"));
		assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

		LlmUnavailableException error = assertThrows(LlmUnavailableException.class,
			() -> retriever.retrieve("¿Qué hacer ante una presión elevada?"));

		assertTrue(error.getMessage().contains("circuit breaker abierto"));
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void returnsUnavailableWithoutCallingVectorStoreWhenEmbeddingKeyIsBlank() {
		VectorStore vectorStore = mock(VectorStore.class);
		RedisProtocolRetriever retriever = new RedisProtocolRetriever(
			vectorStore, new ProtocolCatalog(List.of()), 0.55, " ", CircuitBreaker.ofDefaults("test-embeddings"));

		LlmUnavailableException error = assertThrows(LlmUnavailableException.class,
			() -> retriever.retrieve("¿Qué hacer ante una presión elevada?"));

		assertTrue(error.getMessage().contains("defina LLM_API_KEY"));
		verify(vectorStore, org.mockito.Mockito.never()).similaritySearch(any(SearchRequest.class));
	}
}
