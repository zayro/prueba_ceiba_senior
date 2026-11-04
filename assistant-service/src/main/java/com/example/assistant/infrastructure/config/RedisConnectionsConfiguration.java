package com.example.assistant.infrastructure.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConnectionsConfiguration {
	@Bean
	@Primary
	LettuceConnectionFactory reactiveRedisConnectionFactory(
		RedisProperties properties,
		@Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled
	) {
		LettuceClientConfiguration clientConfiguration = sslEnabled
			? LettuceClientConfiguration.builder().useSsl().build()
			: LettuceClientConfiguration.defaultConfiguration();
		return new LettuceConnectionFactory(standaloneConfiguration(properties), clientConfiguration);
	}

	@Bean
	JedisConnectionFactory vectorStoreRedisConnectionFactory(
		RedisProperties properties,
		@Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled
	) {
		JedisClientConfiguration clientConfiguration = sslEnabled
			? JedisClientConfiguration.builder().useSsl().build()
			: JedisClientConfiguration.defaultConfiguration();
		return new JedisConnectionFactory(standaloneConfiguration(properties), clientConfiguration);
	}

	private RedisStandaloneConfiguration standaloneConfiguration(RedisProperties properties) {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
			properties.getHost(), properties.getPort());
		if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
			configuration.setUsername(properties.getUsername());
		}
		if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
			configuration.setPassword(RedisPassword.of(properties.getPassword()));
		}
		return configuration;
	}
}
