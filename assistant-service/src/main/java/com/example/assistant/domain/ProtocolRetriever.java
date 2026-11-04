package com.example.assistant.domain;

import java.util.List;

@FunctionalInterface
public interface ProtocolRetriever {
	List<Protocol> retrieve(String question);
}
