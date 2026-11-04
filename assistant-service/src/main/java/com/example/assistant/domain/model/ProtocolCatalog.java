package com.example.assistant.domain.model;

import java.util.List;

public record ProtocolCatalog(List<Protocol> protocols) {
	public ProtocolCatalog {
		protocols = List.copyOf(protocols);
	}
}
