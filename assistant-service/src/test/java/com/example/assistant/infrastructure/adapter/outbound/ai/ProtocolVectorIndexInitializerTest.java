package com.example.assistant.infrastructure.adapter.outbound.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.model.ProtocolCatalog;
import com.google.genai.errors.ClientException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;

class ProtocolVectorIndexInitializerTest {
	@Test
	void startsInDegradedModeWhenProtocolIndexCannotBeCreated() {
		ProtocolCatalog catalog = new ProtocolCatalog(List.of(
			new Protocol("PROTO-001", "Crisis Hipertensiva", "Manejo inicial", "Reconfirmar la presión.")
		));
		VectorStore vectorStore = mock(VectorStore.class);
		doThrow(new ClientException(400, "INVALID_ARGUMENT", "API key not valid"))
			.when(vectorStore).add(anyList());
		ProtocolVectorIndexInitializer initializer = new ProtocolVectorIndexInitializer(catalog, vectorStore);

		assertDoesNotThrow(() -> initializer.run(mock(ApplicationArguments.class)));
		verify(vectorStore).add(anyList());
	}
}
