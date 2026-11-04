package com.example.assistant.domain.port;

import com.example.assistant.domain.model.Protocol;
import java.util.List;

@FunctionalInterface
public interface ProtocolRetriever {
	List<Protocol> retrieve(String question);
}
