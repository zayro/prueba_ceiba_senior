package com.example.ceiba.infrastructure.adapter.metrics;

import com.example.ceiba.domain.model.TriageLevel;
import com.example.ceiba.domain.port.TriageMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerTriageMetrics implements TriageMetrics {
	private final MeterRegistry meterRegistry;

	public MicrometerTriageMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void classificationRecorded(TriageLevel level) {
		meterRegistry.counter("triage.classifications", "level", level.name()).increment();
	}

	@Override
	public void escalationRecorded() {
		meterRegistry.counter("triage.escalations").increment();
	}
}
