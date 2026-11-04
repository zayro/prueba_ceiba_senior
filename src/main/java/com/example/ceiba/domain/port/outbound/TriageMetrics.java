package com.example.ceiba.domain.port;

import com.example.ceiba.domain.model.TriageLevel;

public interface TriageMetrics {
	void classificationRecorded(TriageLevel level);

	void escalationRecorded();
}
