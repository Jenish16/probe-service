package com.codeistari.probe.client.grpc;

import com.codeistari.forge.artifact.grpc.proto.ApproveForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ApprovalDecisionGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.ArtifactForgeServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.CancelForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobHistoryGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.GetApprovalHistoryGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ListPendingApprovalsGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ListPendingApprovalsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RejectForgeJobGrpcRequest;
import com.codeistari.probe.client.ApprovalForgeClient;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionEntryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * gRPC implementation of {@link ApprovalForgeClient}, reusing the same {@code FORGE_GRPC}
 * circuit-breaker/retry pair, deadline property, and {@code mapGrpcException} status mapping as
 * {@link GrpcForgeJobClient}.
 */
@Component
@Qualifier("grpcApprovalForgeClient")
public class GrpcApprovalForgeClient implements ApprovalForgeClient {

	private static final Logger log = LoggerFactory.getLogger(GrpcApprovalForgeClient.class);

	private final ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub;
	private final ForgeClientProperties properties;
	private final ProbeRequestMapper mapper;

	public GrpcApprovalForgeClient(
			ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub,
			ForgeClientProperties properties,
			ProbeRequestMapper mapper) {
		this.blockingStub = blockingStub;
		this.properties = properties;
		this.mapper = mapper;
	}

	@Override
	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackListPendingApprovals")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackListPendingApprovals")
	public List<ForgeJobRestResponse> listPendingApprovals() {
		try {
			ListPendingApprovalsGrpcResponse response =
					withDeadline().listPendingApprovals(ListPendingApprovalsGrpcRequest.newBuilder().build());
			return response.getPendingList().stream().map(this::toRestResponse).collect(Collectors.toList());
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackApproveRequest")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackApproveRequest")
	public ForgeJobRestResponse approveRequest(String forgeJobId, String operatorId) {
		try {
			ForgeJobGrpcResponse response =
					withDeadline()
							.approveForgeJob(
									ApproveForgeJobGrpcRequest.newBuilder()
											.setForgeJobId(forgeJobId)
											.setOperatorId(operatorId)
											.build());
			return toRestResponse(response);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackRejectRequest")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackRejectRequest")
	public ForgeJobRestResponse rejectRequest(String forgeJobId, String operatorId, String reason) {
		try {
			ForgeJobGrpcResponse response =
					withDeadline()
							.rejectForgeJob(
									RejectForgeJobGrpcRequest.newBuilder()
											.setForgeJobId(forgeJobId)
											.setOperatorId(operatorId)
											.setReason(reason)
											.build());
			return toRestResponse(response);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackCancelRequest")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackCancelRequest")
	public ForgeJobRestResponse cancelRequest(String forgeJobId, String requestedBy) {
		try {
			ForgeJobGrpcResponse response =
					withDeadline()
							.cancelForgeJob(
									CancelForgeJobGrpcRequest.newBuilder()
											.setForgeJobId(forgeJobId)
											.setRequestedBy(requestedBy)
											.build());
			return toRestResponse(response);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackGetDecisionHistory")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackGetDecisionHistory")
	public ForgeDecisionHistoryRestResponse getDecisionHistory(String requesterReference) {
		try {
			ForgeJobHistoryGrpcResponse response =
					withDeadline()
							.getApprovalHistory(
									GetApprovalHistoryGrpcRequest.newBuilder()
											.setRequesterReference(requesterReference)
											.build());
			return toRestResponse(response);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
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

	private ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub withDeadline() {
		return blockingStub.withDeadlineAfter(properties.grpc().deadlineMs(), TimeUnit.MILLISECONDS);
	}

	private <T> T rethrowFallback(String operation, Throwable throwable) {
		log.error("Resilience fallback triggered for forge-service gRPC {}: {}", operation, throwable.getMessage());
		if (throwable instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY, "Unexpected error calling forge-service gRPC: " + throwable.getMessage());
	}

	private ForgeRemoteCallException mapGrpcException(StatusRuntimeException ex) {
		Status status = ex.getStatus();
		String description =
				status.getDescription() != null && !status.getDescription().isBlank()
						? status.getDescription()
						: status.getCode().name();

		return switch (status.getCode()) {
			case INVALID_ARGUMENT -> new ForgeRemoteCallException(HttpStatus.BAD_REQUEST, description);
			case NOT_FOUND -> new ForgeRemoteCallException(HttpStatus.NOT_FOUND, description);
			case FAILED_PRECONDITION, ALREADY_EXISTS ->
					new ForgeRemoteCallException(HttpStatus.CONFLICT, description);
			case DEADLINE_EXCEEDED ->
					new ForgeRemoteCallException(
							HttpStatus.GATEWAY_TIMEOUT,
							"forge-service gRPC call timed out after " + properties.grpc().deadlineMs() + "ms");
			case INTERNAL, UNAVAILABLE, RESOURCE_EXHAUSTED ->
					new ForgeRemoteCallException(
							HttpStatus.BAD_GATEWAY, "forge-service internal error: " + description);
			default ->
					new ForgeRemoteCallException(HttpStatus.BAD_GATEWAY, "forge-service gRPC error: " + description);
		};
	}

	private ForgeJobRestResponse toRestResponse(ForgeJobGrpcResponse response) {
		return new ForgeJobRestResponse(
				response.getForgeJobId(),
				response.getArtifactName(),
				response.getArtifactType(),
				response.getMaterial(),
				response.getRequestedBy(),
				response.getPowerLevel(),
				response.getStatus(),
				mapper.toInstant(response.getCreatedAt()),
				response.getRequesterReference(),
				response.getOriginalRequestId().isEmpty() ? null : response.getOriginalRequestId(),
				response.getApprovalStatus().isEmpty() ? null : response.getApprovalStatus(),
				response.hasApprovalExpiresAt() ? mapper.toInstant(response.getApprovalExpiresAt()) : null,
				response.getRejectionReason().isEmpty() ? null : response.getRejectionReason());
	}

	private ForgeDecisionEntryRestResponse toRestResponse(ApprovalDecisionGrpcResponse response) {
		return new ForgeDecisionEntryRestResponse(
				response.getDecisionId(),
				response.getForgeJobId(),
				response.getDecisionType(),
				response.getOperatorId(),
				response.getReason().isEmpty() ? null : response.getReason(),
				mapper.toInstant(response.getDecidedAt()));
	}

	private ForgeDecisionHistoryRestResponse toRestResponse(ForgeJobHistoryGrpcResponse response) {
		return new ForgeDecisionHistoryRestResponse(
				response.getRequesterReference(),
				response.getAttemptsList().stream().map(this::toRestResponse).collect(Collectors.toList()),
				response.getDecisionsList().stream().map(this::toRestResponse).collect(Collectors.toList()));
	}
}
