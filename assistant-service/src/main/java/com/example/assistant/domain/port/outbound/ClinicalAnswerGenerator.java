package com.example.assistant.domain.port.outbound;

import com.example.assistant.domain.model.Protocol;
import java.util.List;
import org.reactivestreams.Publisher;

public interface ClinicalAnswerGenerator {
	Publisher<String> generate(String question, List<Protocol> evidence);
}
