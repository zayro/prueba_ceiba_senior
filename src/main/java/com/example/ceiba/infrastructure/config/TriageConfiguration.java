package com.example.ceiba.infrastructure.config;

import com.example.ceiba.domain.TriageClassifier;
import com.example.ceiba.domain.SlaPolicy;
import com.example.ceiba.domain.TriageLevel;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TriageConfiguration {
	@Bean
	TriageClassifier triageClassifier() {
		return new TriageClassifier();
	}

	@Bean
	SlaPolicy slaPolicy(
		@Value("${triage.sla.critico-minutes:5}") int critico,
		@Value("${triage.sla.urgente-minutes:10}") int urgente,
		@Value("${triage.sla.urgente-2-minutes:30}") int urgente2,
		@Value("${triage.sla.no-urgente-minutes:60}") int noUrgente,
		@Value("${triage.sla.no-urgente-2-minutes:120}") int noUrgente2
	) {
		return new SlaPolicy(Map.of(
			TriageLevel.CRITICO, critico,
			TriageLevel.URGENTE, urgente,
			TriageLevel.URGENTE_2, urgente2,
			TriageLevel.NO_URGENTE, noUrgente,
			TriageLevel.NO_URGENTE_2, noUrgente2
		));
	}
}
