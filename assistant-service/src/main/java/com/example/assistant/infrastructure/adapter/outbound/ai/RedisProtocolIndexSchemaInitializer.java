package com.example.assistant.infrastructure.adapter.outbound.ai;

import java.util.List;
import java.util.Map;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.IndexDataType;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.search.schemafields.VectorField;
import redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm;

@Component
@Order(0)
public class RedisProtocolIndexSchemaInitializer implements ApplicationRunner {
	private final VectorStore vectorStore;
	private final String indexName;
	private final String prefix;
	private final int dimensions;

	public RedisProtocolIndexSchemaInitializer(
		VectorStore vectorStore,
		@Value("${spring.ai.vectorstore.redis.index-name}") String indexName,
		@Value("${spring.ai.vectorstore.redis.prefix}") String prefix,
		@Value("${spring.ai.google.genai.embedding.text.options.dimensions:768}") int dimensions
	) {
		if (dimensions <= 0) {
			throw new IllegalArgumentException("Embedding dimensions must be positive");
		}
		this.vectorStore = vectorStore;
		this.indexName = indexName;
		this.prefix = prefix;
		this.dimensions = dimensions;
	}

	@Override
	public void run(ApplicationArguments args) {
		JedisPooled jedis = vectorStore.<JedisPooled>getNativeClient()
			.orElseThrow(() -> new IllegalStateException("Redis vector store does not expose a Jedis client"));
		if (jedis.ftList().contains(indexName)) {
			return;
		}

		Map<String, Object> vectorAttributes = Map.of(
			"DIM", dimensions,
			"DISTANCE_METRIC", "COSINE",
			"TYPE", "FLOAT32"
		);
		List<SchemaField> fields = List.of(
			TextField.of("$.content").as("content").weight(1.0),
			VectorField.builder()
				.fieldName("$.embedding")
				.algorithm(VectorAlgorithm.HNSW)
				.attributes(vectorAttributes)
				.as("embedding")
				.build()
		);
		jedis.ftCreate(indexName,
			FTCreateParams.createParams().on(IndexDataType.JSON).addPrefix(prefix), fields);
	}
}
