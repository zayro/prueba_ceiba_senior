package com.example.ceiba.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PatientRepository extends ReactiveCrudRepository<PatientEntity, UUID> {
	Flux<PatientEntity> findByState(String state);

	@Modifying
	@Query("""
		UPDATE patients
		SET state = :newState,
		    state_since = :now,
		    first_consultation_at = CASE
		        WHEN :newState = 'EN_CONSULTA' THEN COALESCE(first_consultation_at, :now)
		        ELSE first_consultation_at
		    END
		WHERE id = :id AND state = :expectedState
		""")
	Mono<Integer> transition(
		@Param("id") UUID id,
		@Param("expectedState") String expectedState,
		@Param("newState") String newState,
		@Param("now") Instant now
	);

	@Modifying
	@Query("""
		UPDATE patients SET escalated = TRUE
		WHERE id = :id AND state = 'REGISTRADO' AND escalated = FALSE
		  AND created_at + (sla_minutes * INTERVAL '1 minute') <= :now
		""")
	Mono<Integer> markEscalatedIfDue(@Param("id") UUID id, @Param("now") Instant now);
}
