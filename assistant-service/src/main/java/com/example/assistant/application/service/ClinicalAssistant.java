package com.example.assistant.application.service;

import com.example.assistant.domain.model.AssistantResponse;
import com.example.assistant.domain.model.ProtocolSource;
import com.example.assistant.application.validation.AnswerGroundingValidator;
import com.example.assistant.domain.model.Protocol;
import com.example.assistant.domain.port.outbound.ClinicalAnswerGenerator;
import com.example.assistant.domain.port.outbound.ProtocolRetriever;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ClinicalAssistant {
	private static final String NO_PROTOCOL =
		"No existe un protocolo disponible para la pregunta.";
	private final ProtocolRetriever retriever;
	private final ClinicalAnswerGenerator answerGenerator;
	private final AnswerGroundingValidator answerGroundingValidator;

	public ClinicalAssistant(
		ProtocolRetriever retriever,
		ClinicalAnswerGenerator answerGenerator,
		AnswerGroundingValidator answerGroundingValidator
	) {
		this.retriever = retriever;
		this.answerGenerator = answerGenerator;
		this.answerGroundingValidator = answerGroundingValidator;
	}

	public Mono<AssistantResponse> consult(String question) {
		return Mono.fromCallable(() -> retriever.retrieve(question))
			.subscribeOn(Schedulers.boundedElastic())
			.flatMap(evidence -> {
				if (evidence.isEmpty()) {
					return Mono.just(new AssistantResponse(NO_PROTOCOL, List.of()));
				}
				List<ProtocolSource> sources = evidence.stream()
					.map(protocol -> new ProtocolSource(protocol.id(), protocol.section()))
					.toList();
				return Mono.from(answerGenerator.generate(question, evidence))
					.map(answer -> new AssistantResponse(
						answerGroundingValidator.validate(answer, evidence), sources));
			});
	}
}
