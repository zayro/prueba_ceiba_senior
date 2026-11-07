package com.example.assistant.infrastructure.adapter.outbound.ai;

import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.model.ProtocolCatalog;
import com.google.genai.errors.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProtocolVectorIndexInitializer implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(ProtocolVectorIndexInitializer.class);

	private final ProtocolCatalog protocolCatalog;
	private final VectorStore vectorStore;

	public ProtocolVectorIndexInitializer(ProtocolCatalog protocolCatalog, VectorStore vectorStore) {
		this.protocolCatalog = protocolCatalog;
		this.vectorStore = vectorStore;
	}

	@Override
	public void run(ApplicationArguments args) {
		List<Document> documents = protocolCatalog.protocols().stream()
			.map(this::toDocument)
			.toList();
		try {
			vectorStore.add(documents);
		} catch (ApiException error) {
			log.warn("Protocol vector index initialization failed ({}); Assistant will start in degraded mode. "
				+ "Protocol consultations will return HTTP 503 until embeddings are available.",
				error.getClass().getSimpleName());
		}
	}

	private Document toDocument(Protocol protocol) {
		String text = protocol.title() + ". " + protocol.section() + ". " + protocol.content();
		return Document.builder()
			.id(protocol.id())
			.text(text)
			.build();
	}
}
