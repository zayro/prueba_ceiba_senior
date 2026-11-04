package com.example.assistant.infrastructure.ai;

import com.example.assistant.domain.Protocol;
import com.example.assistant.domain.ProtocolCatalog;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProtocolVectorIndexInitializer implements ApplicationRunner {
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
		vectorStore.add(documents);
	}

	private Document toDocument(Protocol protocol) {
		String text = protocol.title() + ". " + protocol.section() + ". " + protocol.content();
		return Document.builder()
			.id(protocol.id())
			.text(text)
			.build();
	}
}
