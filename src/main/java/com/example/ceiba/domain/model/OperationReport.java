package com.example.ceiba.domain.model;

import java.time.Instant;

public record OperationReport(
	Instant hour,
	long total,
	long critico,
	long urgente,
	long urgente2,
	long noUrgente,
	long noUrgente2,
	Double averageWaitMinutes,
	Double p95WaitMinutes
) {
}
