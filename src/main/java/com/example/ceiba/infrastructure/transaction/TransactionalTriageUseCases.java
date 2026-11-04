package com.example.ceiba.infrastructure.transaction;

import com.example.ceiba.domain.model.OperationReport;
import com.example.ceiba.domain.valueobject.PatientData;
import com.example.ceiba.domain.model.PatientDetails;
import com.example.ceiba.domain.valueobject.PatientState;
import com.example.ceiba.domain.port.inbound.TriageUseCases;
import com.example.ceiba.application.service.TriageService;
import java.time.Instant;
import java.util.UUID;
import org.reactivestreams.Publisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionalTriageUseCases implements TriageUseCases {
	private final TriageService delegate;
	private final TransactionalOperator transactions;

	public TransactionalTriageUseCases(TriageService delegate, TransactionalOperator transactions) {
		this.delegate = delegate;
		this.transactions = transactions;
	}

	@Override
	public Publisher<PatientDetails> register(PatientData patientData) {
		return Mono.from(delegate.register(patientData)).as(transactions::transactional);
	}

	@Override
	public Publisher<PatientDetails> changeState(UUID id, PatientState state) {
		return Mono.from(delegate.changeState(id, state)).as(transactions::transactional);
	}

	@Override
	public Publisher<PatientDetails> findPatient(UUID id) {
		return delegate.findPatient(id);
	}

	@Override
	public Publisher<Void> escalateIfDue(UUID id) {
		return Mono.from(delegate.escalateIfDue(id)).as(transactions::transactional);
	}

	@Override
	public Publisher<OperationReport> operationReport(Instant from, Instant until) {
		return Flux.from(delegate.operationReport(from, until));
	}
}
