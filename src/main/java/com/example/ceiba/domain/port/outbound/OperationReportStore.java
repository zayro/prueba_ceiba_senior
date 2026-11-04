package com.example.ceiba.domain.port;

import com.example.ceiba.domain.model.OperationReport;
import java.time.Instant;
import org.reactivestreams.Publisher;

public interface OperationReportStore {
	Publisher<OperationReport> findBetween(Instant from, Instant until);
}
