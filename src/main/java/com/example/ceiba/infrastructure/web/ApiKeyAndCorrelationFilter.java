package com.example.ceiba.infrastructure.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyAndCorrelationFilter implements WebFilter {
	private static final Logger log = LoggerFactory.getLogger(ApiKeyAndCorrelationFilter.class);
	private final byte[] expectedApiKey;

	public ApiKeyAndCorrelationFilter(@Value("${security.api-key}") String expectedApiKey) {
		if (expectedApiKey.isBlank()) {
			throw new IllegalArgumentException("API_KEY must not be blank");
		}
		this.expectedApiKey = expectedApiKey.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (path.startsWith("/actuator/health")) {
			return chain.filter(exchange);
		}
		String supplied = exchange.getRequest().getHeaders().getFirst("X-API-Key");
		if (supplied == null || !MessageDigest.isEqual(
			expectedApiKey, supplied.getBytes(StandardCharsets.UTF_8))) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}
		String correlationId = exchange.getRequest().getHeaders()
			.getFirst("X-Correlation-Id");
		if (correlationId == null || !correlationId.matches("[A-Za-z0-9._-]{1,100}")) {
			correlationId = UUID.randomUUID().toString();
		}
		exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);
		String requestId = correlationId;
		long startedAt = System.nanoTime();
		return chain.filter(exchange)
			.doFinally(signal -> log.info("request completed correlationId={} method={} path={} status={} durationMs={}",
				requestId, exchange.getRequest().getMethod(), path,
				exchange.getResponse().getStatusCode(),
				Duration.ofNanos(System.nanoTime() - startedAt).toMillis()));
	}
}
