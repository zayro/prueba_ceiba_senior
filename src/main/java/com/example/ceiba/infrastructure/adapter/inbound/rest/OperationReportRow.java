package com.example.ceiba.infrastructure.adapter.inbound.rest;

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
