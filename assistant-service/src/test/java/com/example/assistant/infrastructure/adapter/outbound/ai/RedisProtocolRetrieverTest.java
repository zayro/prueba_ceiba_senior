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
			vectorStore, new ProtocolCatalog(List.of(expectedProtocol)), 0.55);

		assertEquals(List.of(expectedProtocol), retriever.retrieve("¿Qué hacer ante una presión elevada?"));
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void rejectsSimilarityThresholdOutsideTheSupportedRange() {
		VectorStore vectorStore = mock(VectorStore.class);

		assertThrows(IllegalArgumentException.class,
			() -> new RedisProtocolRetriever(vectorStore, new ProtocolCatalog(List.of()), 1.1));
	}

	@Test
	void reportsUnavailableEmbeddingProviderWhenVectorSearchFails() {
		VectorStore vectorStore = mock(VectorStore.class);
		doThrow(new ClientException(400, "INVALID_ARGUMENT", "API key not valid"))
			.when(vectorStore).similaritySearch(any(SearchRequest.class));
		RedisProtocolRetriever retriever = new RedisProtocolRetriever(
			vectorStore, new ProtocolCatalog(List.of()), 0.55);

		LlmUnavailableException error = assertThrows(LlmUnavailableException.class,
			() -> retriever.retrieve("¿Qué hacer ante una presión elevada?"));

		assertTrue(error.getMessage().contains("Proveedor de embeddings no disponible"));
		assertTrue(error.getCause() instanceof ClientException);
	}
}
