package com.codeistari.probe.client.rest;

import com.codeistari.probe.client.ApprovalForgeClient;
import com.codeistari.probe.client.BaseApiClient;
import com.codeistari.probe.dto.client.forge.request.ForgeApproveJobRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeCancelJobRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRejectJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * REST implementation of {@link ApprovalForgeClient}, reusing the {@code FORGE_REST}
 * circuit-breaker/retry pair and {@link BaseApiClient}'s response-mapping pattern, exactly as
 * {@link RestForgeJobClient} does for the existing create/get operations.
 */
@Component
@Qualifier("restApprovalForgeClient")
public class RestApprovalForgeClient extends BaseApiClient implements ApprovalForgeClient {

	private static final Logger log = LoggerFactory.getLogger(RestApprovalForgeClient.class);

	public RestApprovalForgeClient(
			@Qualifier("forgeRestClient") RestClient restClient, ObjectMapper objectMapper) {
		super(restClient, "forge-service", objectMapper);
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackListPendingApprovals")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackListPendingApprovals")
	public List<ForgeJobRestResponse> listPendingApprovals() {
		ForgeJobRestResponse[] pending =
				handleResponse(
						get("/api/v1/forge-jobs/pending-approval", ForgeJobRestResponse[].class),
						"listPendingApprovals");
		return pending == null ? List.of() : Arrays.asList(pending);
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackApproveRequest")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackApproveRequest")
	public ForgeJobRestResponse approveRequest(String forgeJobId, String operatorId) {
		return handleResponse(
				post(
						"/api/v1/forge-jobs/{forgeJobId}/approve",
						new ForgeApproveJobRestRequest(operatorId),
						ForgeJobRestResponse.class,
						forgeJobId),
				"approveRequest(" + forgeJobId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackRejectRequest")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackRejectRequest")
	public ForgeJobRestResponse rejectRequest(String forgeJobId, String operatorId, String reason) {
		return handleResponse(
				post(
						"/api/v1/forge-jobs/{forgeJobId}/reject",
						new ForgeRejectJobRestRequest(operatorId, reason),
						ForgeJobRestResponse.class,
						forgeJobId),
				"rejectRequest(" + forgeJobId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackCancelRequest")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackCancelRequest")
	public ForgeJobRestResponse cancelRequest(String forgeJobId, String requestedBy) {
		return handleResponse(
				post(
						"/api/v1/forge-jobs/{forgeJobId}/cancel",
						new ForgeCancelJobRestRequest(requestedBy),
						ForgeJobRestResponse.class,
						forgeJobId),
				"cancelRequest(" + forgeJobId + ")");
	}

	@Override
	@CircuitBreaker(name = "FORGE_REST", fallbackMethod = "fallbackGetDecisionHistory")
	@Retry(name = "FORGE_REST", fallbackMethod = "fallbackGetDecisionHistory")
	public ForgeDecisionHistoryRestResponse getDecisionHistory(String requesterReference) {
		return handleResponse(
				get(
						"/api/v1/forge-jobs/history?requesterReference={requesterReference}",
						ForgeDecisionHistoryRestResponse.class,
						requesterReference),
				"getDecisionHistory(" + requesterReference + ")");
	}

	public List<ForgeJobRestResponse> fallbackListPendingApprovals(Throwable throwable) {
		return rethrowFallback("listPendingApprovals", throwable);
	}

	public ForgeJobRestResponse fallbackApproveRequest(
			String forgeJobId, String operatorId, Throwable throwable) {
		return rethrowFallback("approveRequest(" + forgeJobId + ")", throwable);
	}

	public ForgeJobRestResponse fallbackRejectRequest(
			String forgeJobId, String operatorId, String reason, Throwable throwable) {
		return rethrowFallback("rejectRequest(" + forgeJobId + ")", throwable);
	}

	public ForgeJobRestResponse fallbackCancelRequest(
			String forgeJobId, String requestedBy, Throwable throwable) {
		return rethrowFallback("cancelRequest(" + forgeJobId + ")", throwable);
	}

	public ForgeDecisionHistoryRestResponse fallbackGetDecisionHistory(
			String requesterReference, Throwable throwable) {
		return rethrowFallback("getDecisionHistory(" + requesterReference + ")", throwable);
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
