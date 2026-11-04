package com.example.assistant.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.assistant.domain.Protocol;
import com.example.assistant.domain.ProtocolRetriever;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ClinicalAssistantTest {
	private static final Protocol HYPERTENSION = new Protocol(
		"PROTO-001", "Crisis Hipertensiva", "Manejo inicial",
		"Reconfirmar la presión tras 5 minutos de reposo."
	);

	@Test
	void returnsRetrievedSourceCitationWithTheAnswer() {
		ClinicalAssistant assistant = new ClinicalAssistant(
			new ProtocolRetriever(List.of(HYPERTENSION)),
			(question, evidence) -> Mono.just("Reconfirme la presión tras 5 minutos. [PROTO-001]")
		);

		StepVerifier.create(assistant.consult("¿Qué hacer ante una crisis hipertensiva?"))
			.assertNext(response -> {
				assertTrue(response.respuesta().contains("[PROTO-001]"));
				assertEquals("PROTO-001", response.fuentes().getFirst().protocolo());
			})
			.verifyComplete();
	}

	@Test
	void explicitlyDeclinesWhenTheCorpusHasNoMatch() {
		ClinicalAssistant assistant = new ClinicalAssistant(
			new ProtocolRetriever(List.of(HYPERTENSION)),
			(question, evidence) -> Mono.error(new AssertionError("LLM must not be called"))
		);

		StepVerifier.create(assistant.consult("Tratamiento para diabetes tipo 2"))
			.assertNext(response -> {
				assertEquals("No existe un protocolo disponible para la pregunta.", response.respuesta());
				assertTrue(response.fuentes().isEmpty());
			})
			.verifyComplete();
	}

	@Test
	void doesNotMatchAnUnrelatedProtocolOnGenericFirstLineTerms() {
		ClinicalAssistant assistant = new ClinicalAssistant(
			new ProtocolRetriever(List.of(HYPERTENSION,
				new Protocol("PROTO-002", "Sepsis", "Acciones iniciales",
					"Antibiótico de amplio espectro en la primera hora."))),
			(question, evidence) -> Mono.error(new AssertionError("LLM must not be called"))
		);

		StepVerifier.create(assistant.consult("Tratamiento de primera línea para diabetes tipo 2"))
			.assertNext(response -> {
				assertEquals("No existe un protocolo disponible para la pregunta.", response.respuesta());
				assertTrue(response.fuentes().isEmpty());
			})
			.verifyComplete();
	}

	@Test
	void replacesAnUnverifiedModelCitationWithExtractiveEvidence() {
		ClinicalAssistant assistant = new ClinicalAssistant(
			new ProtocolRetriever(List.of(HYPERTENSION)),
			(question, evidence) -> Mono.just("Use la insulina [PROTO-001]")
		);

		StepVerifier.create(assistant.consult("Crisis hipertensiva"))
			.assertNext(response -> {
				assertEquals(HYPERTENSION.content() + " [PROTO-001]", response.respuesta());
				assertEquals("PROTO-001", response.fuentes().getFirst().protocolo());
			})
			.verifyComplete();
	}
}
