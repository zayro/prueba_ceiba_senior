package com.example.ceiba.domain.port.outbound;

import com.example.ceiba.domain.valueobject.TriageLevel;

public interface TriageMetrics {
	void classificationRecorded(TriageLevel level);

	void escalationRecorded();
}
