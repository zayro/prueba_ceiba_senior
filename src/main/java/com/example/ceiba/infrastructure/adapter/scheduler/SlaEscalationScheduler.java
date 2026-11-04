package com.example.ceiba.infrastructure.adapter.scheduler;

import com.example.ceiba.domain.model.PatientState;
import com.example.ceiba.domain.port.PatientStore;
import com.example.ceiba.domain.port.TriageUseCases;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SlaEscalationScheduler {
	private static final Logger log = LoggerFactory.getLogger(SlaEscalationScheduler.class);
	private final PatientStore patients;
	private final TriageUseCases triageUseCases;
	private final AtomicBoolean running = new AtomicBoolean();

	public SlaEscalationScheduler(PatientStore patients, TriageUseCases triageUseCases) {
		this.patients = patients;
		this.triageUseCases = triageUseCases;
	}

	@Scheduled(fixedDelayString = "${triage.scheduler.fixed-delay-ms:10000}")
	public void checkSlas() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		Flux.from(patients.findByState(PatientState.REGISTRADO))
			.concatMap(patient -> Mono.from(triageUseCases.escalateIfDue(patient.id())))
			.doOnError(error -> log.error("SLA evaluation failed", error))
			.doFinally(signal -> running.set(false))
			.subscribe();
	}
}
