package com.codeistari.probe.mapper;

import com.codeistari.forge.artifact.grpc.proto.CreateLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutAttemptGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemAttemptsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RejectedLoadoutItemGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutItemGrpcRequest;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeLoadoutItemRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutItemRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutItemRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeRejectedLoadoutItemRestResponse;
import com.codeistari.probe.dto.request.CreateProbeLoadoutRequest;
import com.codeistari.probe.dto.request.ProbeLoadoutItemRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutItemRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutRequest;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptHistoryResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutItemResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.codeistari.probe.dto.response.ProbeRejectedLoadoutItemResponse;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProbeLoadoutMapper {

	public ForgeCreateLoadoutRestRequest toForgeCreateLoadoutRestRequest(CreateProbeLoadoutRequest request) {
		return new ForgeCreateLoadoutRestRequest(
				request.requestReference(),
				request.loadoutName(),
				request.requestedBy(),
				request.items().stream().map(this::toForgeLoadoutItemRestRequest).toList());
	}

	private ForgeLoadoutItemRestRequest toForgeLoadoutItemRestRequest(ProbeLoadoutItemRequest item) {
		return new ForgeLoadoutItemRestRequest(
				item.artifactName(), item.artifactType(), item.material(), item.powerLevel());
	}

	public CreateLoadoutGrpcRequest toCreateLoadoutGrpcRequest(CreateProbeLoadoutRequest request) {
		CreateLoadoutGrpcRequest.Builder builder = CreateLoadoutGrpcRequest.newBuilder()
				.setRequestReference(request.requestReference())
				.setLoadoutName(request.loadoutName())
				.setRequestedBy(request.requestedBy());
		for (ProbeLoadoutItemRequest item : request.items()) {
			builder.addItems(LoadoutItemGrpcRequest.newBuilder()
					.setArtifactName(item.artifactName())
					.setArtifactType(item.artifactType())
					.setMaterial(item.material())
					.setPowerLevel(item.powerLevel())
					.build());
		}
		return builder.build();
	}

	public RetryLoadoutGrpcRequest toRetryLoadoutGrpcRequest(String loadoutId, RetryProbeLoadoutRequest request) {
		RetryLoadoutGrpcRequest.Builder builder = RetryLoadoutGrpcRequest.newBuilder().setLoadoutId(loadoutId);
		List<RetryProbeLoadoutItemRequest> items = request.items();
		if (items != null) {
			for (RetryProbeLoadoutItemRequest item : items) {
				RetryLoadoutItemGrpcRequest.Builder itemBuilder =
						RetryLoadoutItemGrpcRequest.newBuilder().setLoadoutItemId(item.loadoutItemId());
				if (item.artifactName() != null) {
					itemBuilder.setArtifactName(item.artifactName());
				}
				if (item.artifactType() != null) {
					itemBuilder.setArtifactType(item.artifactType());
				}
				if (item.material() != null) {
					itemBuilder.setMaterial(item.material());
				}
				if (item.powerLevel() != null) {
					itemBuilder.setPowerLevel(item.powerLevel());
				}
				builder.addItems(itemBuilder.build());
			}
		}
		return builder.build();
	}

	public ForgeRetryLoadoutRestRequest toForgeRetryLoadoutRestRequest(RetryProbeLoadoutRequest request) {
		List<RetryProbeLoadoutItemRequest> items = request.items();
		List<ForgeRetryLoadoutItemRestRequest> forgeItems =
				items == null
						? List.of()
						: items.stream()
								.map(item -> new ForgeRetryLoadoutItemRestRequest(
										item.loadoutItemId(),
										item.artifactName(),
										item.artifactType(),
										item.material(),
										item.powerLevel()))
								.toList();
		return new ForgeRetryLoadoutRestRequest(forgeItems);
	}

	public ProbeLoadoutResponse toProbeLoadoutResponse(ForgeLoadoutRestResponse response, ForgeTransport transport) {
		return new ProbeLoadoutResponse(
				response.loadoutId(),
				response.loadoutName(),
				response.requestedBy(),
				response.status(),
				response.items() == null
						? List.of()
						: response.items().stream().map(this::toProbeLoadoutItemResponse).toList(),
				response.rejectedItems() == null
						? List.of()
						: response.rejectedItems().stream().map(this::toProbeRejectedLoadoutItemResponse).toList(),
				transport);
	}

	private ProbeLoadoutItemResponse toProbeLoadoutItemResponse(ForgeLoadoutItemRestResponse item) {
		return new ProbeLoadoutItemResponse(
				item.loadoutItemId(),
				item.artifactName(),
				item.artifactType(),
				item.material(),
				item.powerLevel(),
				item.status(),
				item.approvalStatus(),
				item.approvalExpiresAt(),
				item.rejectionReason(),
				item.failureReason());
	}

	private ProbeRejectedLoadoutItemResponse toProbeRejectedLoadoutItemResponse(ForgeRejectedLoadoutItemRestResponse rejected) {
		return new ProbeRejectedLoadoutItemResponse(
				rejected.artifactName(), rejected.artifactType(), rejected.material(), rejected.powerLevel(), rejected.reason());
	}

	public ProbeLoadoutResponse toProbeLoadoutResponse(LoadoutGrpcResponse response, ForgeTransport transport) {
		return new ProbeLoadoutResponse(
				response.getLoadoutId(),
				response.getLoadoutName(),
				response.getRequestedBy(),
				response.getStatus(),
				response.getItemsList().stream().map(this::toProbeLoadoutItemResponse).toList(),
				response.getRejectedItemsList().stream().map(this::toProbeRejectedLoadoutItemResponse).toList(),
				transport);
	}

	private ProbeLoadoutItemResponse toProbeLoadoutItemResponse(LoadoutItemGrpcResponse item) {
		return new ProbeLoadoutItemResponse(
				item.getLoadoutItemId(),
				item.getArtifactName(),
				item.getArtifactType(),
				item.getMaterial(),
				item.getPowerLevel(),
				blankToNull(item.getStatus()),
				blankToNull(item.getApprovalStatus()),
				item.hasApprovalExpiresAt() ? toInstant(item.getApprovalExpiresAt()) : null,
				blankToNull(item.getRejectionReason()),
				blankToNull(item.getFailureReason()));
	}

	private ProbeRejectedLoadoutItemResponse toProbeRejectedLoadoutItemResponse(RejectedLoadoutItemGrpcResponse rejected) {
		return new ProbeRejectedLoadoutItemResponse(
				rejected.getArtifactName(),
				rejected.getArtifactType(),
				rejected.getMaterial(),
				rejected.getPowerLevel(),
				rejected.getReason());
	}

	public ProbeLoadoutAttemptHistoryResponse toProbeLoadoutAttemptHistoryResponse(
			ForgeLoadoutAttemptsRestResponse response) {
		return new ProbeLoadoutAttemptHistoryResponse(
				response.loadoutItemId(),
				response.attempts().stream().map(this::toProbeLoadoutAttemptResponse).toList());
	}

	private ProbeLoadoutAttemptResponse toProbeLoadoutAttemptResponse(ForgeLoadoutAttemptRestResponse attempt) {
		return new ProbeLoadoutAttemptResponse(
				attempt.attemptNumber(),
				attempt.forgeJobId(),
				attempt.requesterReference(),
				attempt.status(),
				attempt.approvalStatus(),
				attempt.rejectionReason(),
				attempt.failureReason(),
				attempt.createdAt());
	}

	public ProbeLoadoutAttemptHistoryResponse toProbeLoadoutAttemptHistoryResponse(
			LoadoutItemAttemptsGrpcResponse response) {
		return new ProbeLoadoutAttemptHistoryResponse(
				response.getLoadoutItemId(),
				response.getAttemptsList().stream().map(this::toProbeLoadoutAttemptResponse).toList());
	}

	private ProbeLoadoutAttemptResponse toProbeLoadoutAttemptResponse(LoadoutAttemptGrpcResponse attempt) {
		return new ProbeLoadoutAttemptResponse(
				attempt.getAttemptNumber(),
				attempt.getForgeJobId(),
				blankToNull(attempt.getRequesterReference()),
				blankToNull(attempt.getStatus()),
				blankToNull(attempt.getApprovalStatus()),
				blankToNull(attempt.getRejectionReason()),
				blankToNull(attempt.getFailureReason()),
				toInstant(attempt.getCreatedAt()));
	}

	public Instant toInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
