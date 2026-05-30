package com.codeistari.probe.config;

import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.function.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j setup for forge-service downstream calls.
 *
 * <p>Architecture: CircuitBreaker(Retry(downstream call)). Only server/network failures count
 * toward retry and circuit breaker thresholds; 4xx-style client errors fail fast.
 */
@Configuration
public class ResilienceConfig {

	static boolean isRetryableFailure(Throwable throwable) {
		if (throwable instanceof ForgeRemoteCallException exception) {
			int status = exception.getStatus().value();
			return status >= 500 || status == 408;
		}

		if (throwable instanceof StatusRuntimeException grpcException) {
			return switch (grpcException.getStatus().getCode()) {
				case DEADLINE_EXCEEDED, INTERNAL, UNAVAILABLE, RESOURCE_EXHAUSTED, ABORTED -> true;
				default -> false;
			};
		}

		Throwable cause = throwable.getCause();
		return cause instanceof ConnectException
				|| cause instanceof HttpTimeoutException
				|| cause instanceof IOException;
	}

	@Bean
	public RetryConfigCustomizer forgeRestRetryConfigCustomizer() {
		Predicate<Throwable> retryPredicate = ResilienceConfig::isRetryableFailure;
		return RetryConfigCustomizer.of("FORGE_REST", builder -> builder.retryOnException(retryPredicate));
	}

	@Bean
	public CircuitBreakerConfigCustomizer forgeRestCircuitBreakerConfigCustomizer() {
		Predicate<Throwable> recordPredicate = ResilienceConfig::isRetryableFailure;
		return CircuitBreakerConfigCustomizer.of(
				"FORGE_REST", builder -> builder.recordException(recordPredicate));
	}

	@Bean
	public RetryConfigCustomizer forgeGrpcRetryConfigCustomizer() {
		Predicate<Throwable> retryPredicate = ResilienceConfig::isRetryableFailure;
		return RetryConfigCustomizer.of("FORGE_GRPC", builder -> builder.retryOnException(retryPredicate));
	}

	@Bean
	public CircuitBreakerConfigCustomizer forgeGrpcCircuitBreakerConfigCustomizer() {
		Predicate<Throwable> recordPredicate = ResilienceConfig::isRetryableFailure;
		return CircuitBreakerConfigCustomizer.of(
				"FORGE_GRPC", builder -> builder.recordException(recordPredicate));
	}
}
