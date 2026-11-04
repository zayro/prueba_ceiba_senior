package com.example.assistant.domain;

import java.util.List;

public record ProtocolCatalog(List<Protocol> protocols) {
	public ProtocolCatalog {
		protocols = List.copyOf(protocols);
	}
}
