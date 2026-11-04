package com.example.ceiba.infrastructure.web;

import java.time.Instant;

public record OperationReportRow(
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
