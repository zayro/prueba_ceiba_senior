package com.example.ceiba.infrastructure.config;

import com.example.ceiba.application.service.TriageService;
import com.example.ceiba.application.service.OutboxPublishingService;
import com.example.ceiba.domain.model.SlaPolicy;
import com.example.ceiba.domain.model.TriageClassifier;
import com.example.ceiba.domain.model.TriageLevel;
import com.example.ceiba.domain.port.AuditStore;
import com.example.ceiba.domain.port.OperationReportStore;
import com.example.ceiba.domain.port.OutboxStore;
import com.example.ceiba.domain.port.OutboxPublishingUseCase;
import com.example.ceiba.domain.port.PatientStore;
import com.example.ceiba.domain.port.TriageMetrics;
import com.example.ceiba.domain.port.TriageEventPublisher;
import com.example.ceiba.domain.port.TriageUseCases;
import com.example.ceiba.infrastructure.adapter.transaction.TransactionalTriageUseCases;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableScheduling
public class TriageConfiguration {
	@Bean
	TriageClassifier triageClassifier() {
		return new TriageClassifier();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	TriageService triageService(
		PatientStore patients,
		AuditStore audits,
		OutboxStore outbox,
		OperationReportStore reports,
		TriageClassifier classifier,
		SlaPolicy slaPolicy,
		TriageMetrics metrics,
		Clock clock
	) {
		return new TriageService(patients, audits, outbox, reports, classifier, slaPolicy, metrics, clock);
	}

	@Bean
	OutboxPublishingUseCase outboxPublishingService(
		OutboxStore outbox,
		TriageEventPublisher eventPublisher,
		Clock clock
	) {
		return new OutboxPublishingService(outbox, eventPublisher, clock);
	}

	@Bean
	TriageUseCases triageUseCases(
		TriageService triageService,
		ReactiveTransactionManager transactionManager
	) {
		return new TransactionalTriageUseCases(
			triageService, TransactionalOperator.create(transactionManager));
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
