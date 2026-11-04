package com.example.ceiba.application;

import com.example.ceiba.domain.PatientState;
import com.example.ceiba.infrastructure.persistence.PatientRepository;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaEscalationScheduler {
	private static final Logger log = LoggerFactory.getLogger(SlaEscalationScheduler.class);
	private final PatientRepository patients;
	private final TriageService triageService;
	private final AtomicBoolean running = new AtomicBoolean();

	public SlaEscalationScheduler(PatientRepository patients, TriageService triageService) {
		this.patients = patients;
		this.triageService = triageService;
	}

	@Scheduled(fixedDelayString = "${triage.scheduler.fixed-delay-ms:10000}")
	public void checkSlas() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		patients.findByState(PatientState.REGISTRADO.name())
			.concatMap(triageService::escalateIfDue)
			.doOnError(error -> log.error("SLA evaluation failed", error))
			.doFinally(signal -> running.set(false))
			.subscribe();
	}
}
