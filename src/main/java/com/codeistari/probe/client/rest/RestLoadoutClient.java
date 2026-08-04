package com.codeistari.probe.client.rest;

import com.codeistari.probe.client.BaseApiClient;
import com.codeistari.probe.client.LoadoutClient;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
@Qualifier("restLoadoutClient")
public class RestLoadoutClient extends BaseApiClient implements LoadoutClient {

	private static final Logger log = LoggerFactory.getLogger(RestLoadoutClient.class);

	public RestLoadoutClient(@Qualifier("forgeRestClient") RestClient restClient, ObjectMapper objectMapper) {
		super(restClient, "forge-service", objectMapper);
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackCreateLoadout")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackCreateLoadout")
	public ForgeLoadoutRestResponse createLoadout(ForgeCreateLoadoutRestRequest request) {
		return handleResponse(post("/api/v1/loadouts", request, ForgeLoadoutRestResponse.class), "createLoadout");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackGetLoadout")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackGetLoadout")
	public ForgeLoadoutRestResponse getLoadout(String loadoutId) {
		return handleResponse(
				get("/api/v1/loadouts/{loadoutId}", ForgeLoadoutRestResponse.class, loadoutId),
				"getLoadout(" + loadoutId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackCancelLoadout")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackCancelLoadout")
	public ForgeLoadoutRestResponse cancelLoadout(String loadoutId) {
		return handleResponse(
				postNoBody("/api/v1/loadouts/{loadoutId}/cancel", ForgeLoadoutRestResponse.class, loadoutId),
				"cancelLoadout(" + loadoutId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackRetryLoadout")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackRetryLoadout")
	public ForgeLoadoutRestResponse retryLoadout(String loadoutId, ForgeRetryLoadoutRestRequest request) {
		return handleResponse(
				post("/api/v1/loadouts/{loadoutId}/retry", request, ForgeLoadoutRestResponse.class, loadoutId),
				"retryLoadout(" + loadoutId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackGetItemAttempts")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackGetItemAttempts")
	public ForgeLoadoutAttemptsRestResponse getItemAttempts(String loadoutId, String loadoutItemId) {
		return handleResponse(
				get(
						"/api/v1/loadouts/{loadoutId}/items/{loadoutItemId}/attempts",
						ForgeLoadoutAttemptsRestResponse.class,
						loadoutId,
						loadoutItemId),
				"getItemAttempts(" + loadoutId + ", " + loadoutItemId + ")");
	}

	public ForgeLoadoutRestResponse fallbackCreateLoadout(ForgeCreateLoadoutRestRequest request, Throwable throwable) {
		return rethrowFallback("createLoadout", throwable);
	}

	public ForgeLoadoutRestResponse fallbackGetLoadout(String loadoutId, Throwable throwable) {
		return rethrowFallback("getLoadout(" + loadoutId + ")", throwable);
	}

	public ForgeLoadoutRestResponse fallbackCancelLoadout(String loadoutId, Throwable throwable) {
		return rethrowFallback("cancelLoadout(" + loadoutId + ")", throwable);
	}

	public ForgeLoadoutRestResponse fallbackRetryLoadout(
			String loadoutId, ForgeRetryLoadoutRestRequest request, Throwable throwable) {
		return rethrowFallback("retryLoadout(" + loadoutId + ")", throwable);
	}

	public ForgeLoadoutAttemptsRestResponse fallbackGetItemAttempts(
			String loadoutId, String loadoutItemId, Throwable throwable) {
		return rethrowFallback("getItemAttempts(" + loadoutId + ", " + loadoutItemId + ")", throwable);
	}

	private <T> T rethrowFallback(String operation, Throwable throwable) {
		log.error("Resilience fallback triggered for forge-service REST {}: {}", operation, throwable.getMessage());
		if (throwable instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY, "Unexpected error calling forge-service REST: " + throwable.getMessage());
	}
}
