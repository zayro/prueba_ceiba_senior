package com.example.assistant.infrastructure.adapter.redis;

import com.example.assistant.domain.model.TriageEvent;
import com.example.assistant.domain.model.TriageLevel;
import com.example.assistant.domain.port.ActiveUrgencyProjection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RedisUrgencyProjectionAdapter implements ActiveUrgencyProjection {
	private static final String PATIENTS_KEY = "triage:{patients}:projection";
	private static final String ACTIVE_PREFIX = "triage:{patients}:active:";
	private static final DefaultRedisScript<Long> APPLY_EVENT = new DefaultRedisScript<>("""
		local old = redis.call('HGET', KEYS[1], ARGV[1])
		if old then
		  local oldLevel, oldState, oldTime = string.match(old, '([^|]+)|([^|]+)|([^|]+)')
		  if oldTime and tonumber(oldTime) > tonumber(ARGV[3]) then return 0 end
		  if oldState ~= 'ALTA' then redis.call('SREM', KEYS[2] .. oldLevel, ARGV[1]) end
		end
		redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
		if ARGV[4] ~= 'ALTA' then redis.call('SADD', KEYS[2] .. ARGV[5], ARGV[1]) end
		return 1
		""", Long.class);

	private final ReactiveStringRedisTemplate redis;

	public RedisUrgencyProjectionAdapter(ReactiveStringRedisTemplate redis) {
		this.redis = redis;
	}

	@Override
	public Mono<Long> apply(TriageEvent event) {
		long eventTimeMicros = event.occurredAt().getEpochSecond() * 1_000_000
			+ event.occurredAt().getNano() / 1_000;
		String timestamp = Long.toString(eventTimeMicros);
		String value = event.level() + "|" + event.state() + "|" + timestamp;
		return redis.execute(APPLY_EVENT, java.util.List.of(PATIENTS_KEY, ACTIVE_PREFIX),
			event.patientId().toString(), value, timestamp,
			event.state(), event.level()).next();
	}

	@Override
	public Mono<Map<String, Long>> activeCounts() {
		return Flux.fromArray(TriageLevel.values())
			.flatMap(level -> redis.opsForSet().size(ACTIVE_PREFIX + level.name())
				.map(count -> Map.entry(level.name(), count)))
			.collectMap(Map.Entry::getKey, Map.Entry::getValue)
			.map(counts -> java.util.Arrays.stream(TriageLevel.values())
				.collect(Collectors.toMap(
					Enum::name, level -> counts.getOrDefault(level.name(), 0L))));
	}
}
