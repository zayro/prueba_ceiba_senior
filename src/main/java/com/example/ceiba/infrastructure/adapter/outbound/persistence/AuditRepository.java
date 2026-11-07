package com.example.ceiba.infrastructure.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AuditRepository extends ReactiveCrudRepository<AuditEntity, UUID> {
	Flux<AuditEntity> findByPatientIdOrderByTimestampAsc(UUID patientId);
}
