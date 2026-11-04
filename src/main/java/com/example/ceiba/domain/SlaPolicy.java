package com.example.ceiba.domain;

import java.util.Map;

public class SlaPolicy {
	private final Map<TriageLevel, Integer> minutesByLevel;

	public SlaPolicy(Map<TriageLevel, Integer> minutesByLevel) {
		this.minutesByLevel = Map.copyOf(minutesByLevel);
		if (!this.minutesByLevel.keySet().containsAll(java.util.Set.of(TriageLevel.values()))) {
			throw new IllegalArgumentException("Debe configurarse un SLA para cada nivel de triaje");
		}
		if (this.minutesByLevel.values().stream().anyMatch(minutes -> minutes == null || minutes <= 0)) {
			throw new IllegalArgumentException("Los SLA deben ser mayores que cero");
		}
	}

	public int minutesFor(TriageLevel level) {
		return minutesByLevel.get(level);
	}
}
