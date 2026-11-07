package com.example.assistant.infrastructure.adapter.outbound.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.schemafields.SchemaField;

class RedisProtocolIndexSchemaInitializerTest {
	@Test
	void createsRedisIndexSchemaWithoutCallingEmbeddingProvider() {
		VectorStore vectorStore = mock(VectorStore.class);
		JedisPooled jedis = mock(JedisPooled.class);
		when(vectorStore.<JedisPooled>getNativeClient()).thenReturn(Optional.of(jedis));
		when(jedis.ftList()).thenReturn(Set.of());
		RedisProtocolIndexSchemaInitializer initializer = new RedisProtocolIndexSchemaInitializer(
			vectorStore, "clinical-protocols", "clinical-protocol:", 768);

		assertDoesNotThrow(() -> initializer.run(mock(ApplicationArguments.class)));

		verify(jedis).ftCreate(eq("clinical-protocols"), any(FTCreateParams.class),
			ArgumentMatchers.<Iterable<SchemaField>>any());
	}

	@Test
	void doesNotRecreateAnExistingRedisIndex() {
		VectorStore vectorStore = mock(VectorStore.class);
		JedisPooled jedis = mock(JedisPooled.class);
		when(vectorStore.<JedisPooled>getNativeClient()).thenReturn(Optional.of(jedis));
		when(jedis.ftList()).thenReturn(Set.of("clinical-protocols"));
		RedisProtocolIndexSchemaInitializer initializer = new RedisProtocolIndexSchemaInitializer(
			vectorStore, "clinical-protocols", "clinical-protocol:", 768);

		initializer.run(mock(ApplicationArguments.class));

		verify(jedis, never()).ftCreate(any(String.class), any(FTCreateParams.class),
			ArgumentMatchers.<Iterable<SchemaField>>any());
	}
}
