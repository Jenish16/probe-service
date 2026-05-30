package com.codeistari.probe.client.rest;

import com.codeistari.probe.client.BaseApiClient;
import com.codeistari.probe.client.ForgeJobClient;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
@Qualifier("restForgeJobClient")
public class RestForgeJobClient extends BaseApiClient implements ForgeJobClient {

	private static final Logger log = LoggerFactory.getLogger(RestForgeJobClient.class);

	public RestForgeJobClient(
			@Qualifier("forgeRestClient") RestClient restClient, ObjectMapper objectMapper) {
		super(restClient, "forge-service", objectMapper);
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackCreateForgeJob")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackCreateForgeJob")
	public ForgeJobRestResponse createForgeJob(ForgeCreateJobRestRequest request) {
		return handleResponse(
				post("/api/v1/forge-jobs", request, ForgeJobRestResponse.class),
				"createForgeJob");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackGetForgeJob")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackGetForgeJob")
	public ForgeJobRestResponse getForgeJob(String forgeJobId) {
		return handleResponse(
				get("/api/v1/forge-jobs/{forgeJobId}", ForgeJobRestResponse.class, forgeJobId),
				"getForgeJob(" + forgeJobId + ")");
	}

	public ForgeJobRestResponse fallbackCreateForgeJob(ForgeCreateJobRestRequest request, Throwable throwable) {
		return rethrowFallback("createForgeJob", throwable);
	}

	public ForgeJobRestResponse fallbackGetForgeJob(String forgeJobId, Throwable throwable) {
		return rethrowFallback("getForgeJob(" + forgeJobId + ")", throwable);
	}

	private ForgeJobRestResponse rethrowFallback(String operation, Throwable throwable) {
		log.error("Resilience fallback triggered for forge-service REST {}: {}", operation, throwable.getMessage());
		if (throwable instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new ForgeRemoteCallException(
				org.springframework.http.HttpStatus.BAD_GATEWAY,
				"Unexpected error calling forge-service REST: " + throwable.getMessage());
	}
}
