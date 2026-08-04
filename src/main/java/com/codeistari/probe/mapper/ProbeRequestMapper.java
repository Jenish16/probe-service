package com.codeistari.probe.mapper;

import com.codeistari.forge.artifact.grpc.proto.CreateForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.probe.domain.ApprovalDecisionType;
import com.codeistari.probe.domain.ApprovalStatus;
import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionEntryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.DecisionHistoryEntry;
import com.codeistari.probe.dto.response.DecisionHistoryResponse;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.dto.response.ProbePendingApprovalSummary;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProbeRequestMapper {

	public ForgeCreateJobRestRequest toForgeCreateJobRestRequest(CreateProbeForgeJobRequest request) {
		return new ForgeCreateJobRestRequest(
				request.artifactName(),
				request.artifactType().name(),
				request.material().name(),
				request.requestedBy(),
				request.powerLevel(),
				request.requesterReference(),
				request.originalRequestReference());
	}

	public CreateForgeJobGrpcRequest toCreateForgeJobGrpcRequest(CreateProbeForgeJobRequest request) {
		CreateForgeJobGrpcRequest.Builder builder =
				CreateForgeJobGrpcRequest.newBuilder()
						.setArtifactName(request.artifactName())
						.setArtifactType(request.artifactType().name())
						.setMaterial(request.material().name())
						.setRequestedBy(request.requestedBy())
						.setPowerLevel(request.powerLevel())
						.setRequesterReference(request.requesterReference());
		if (request.originalRequestReference() != null) {
			builder.setOriginalRequestId(request.originalRequestReference());
		}
		return builder.build();
	}

	public ProbeForgeJobResponse toProbeForgeJobResponse(
			ForgeJobRestResponse response, ForgeTransport transport) {
		return new ProbeForgeJobResponse(
				response.forgeJobId(),
				response.artifactName(),
				ArtifactType.valueOf(response.artifactType()),
				ForgeMaterial.valueOf(response.material()),
				response.requestedBy(),
				response.powerLevel(),
				response.status(),
				response.createdAt(),
				transport,
				response.requesterReference(),
				// downstream field is named `originalRequestId` to match forge-service exactly
				// (Gate 2, IC-002); translated here to the public `originalRequestReference` name.
				response.originalRequestId(),
				toApprovalStatus(response.approvalStatus()),
				response.rejectionReason(),
				response.approvalExpiresAt());
	}

	public ProbeForgeJobResponse toProbeForgeJobResponse(
			ForgeJobGrpcResponse response, ForgeTransport transport) {
		return new ProbeForgeJobResponse(
				response.getForgeJobId(),
				response.getArtifactName(),
				ArtifactType.valueOf(response.getArtifactType()),
				ForgeMaterial.valueOf(response.getMaterial()),
				response.getRequestedBy(),
				response.getPowerLevel(),
				blankToNull(response.getStatus()),
				toInstant(response.getCreatedAt()),
				transport,
				response.getRequesterReference(),
				blankToNull(response.getOriginalRequestId()),
				toApprovalStatus(response.getApprovalStatus()),
				blankToNull(response.getRejectionReason()),
				response.hasApprovalExpiresAt() ? toInstant(response.getApprovalExpiresAt()) : null);
	}

	public ProbePendingApprovalSummary toPendingApprovalSummary(ForgeJobRestResponse response) {
		return new ProbePendingApprovalSummary(
				response.forgeJobId(),
				response.requesterReference(),
				response.requestedBy(),
				ArtifactType.valueOf(response.artifactType()),
				ForgeMaterial.valueOf(response.material()),
				response.powerLevel(),
				response.createdAt(),
				response.approvalExpiresAt());
	}

	public List<ProbePendingApprovalSummary> toPendingApprovalSummaries(
			List<ForgeJobRestResponse> responses) {
		return responses.stream().map(this::toPendingApprovalSummary).toList();
	}

	public DecisionHistoryResponse toDecisionHistoryResponse(ForgeDecisionHistoryRestResponse response) {
		return new DecisionHistoryResponse(
				response.requesterReference(),
				response.decisions().stream().map(this::toDecisionHistoryEntry).toList());
	}

	private DecisionHistoryEntry toDecisionHistoryEntry(ForgeDecisionEntryRestResponse entry) {
		return new DecisionHistoryEntry(
				entry.forgeJobId(),
				entry.operatorId(),
				ApprovalDecisionType.valueOf(entry.decisionType()),
				entry.reason(),
				entry.decidedAt());
	}

	/**
	 * Translates forge-service's {@code NOT_REQUIRED} wire value (power level 1-7 jobs) to {@code
	 * null}, matching the public API contract ("null/absent for power level 1-7"); every other
	 * value maps directly onto {@link ApprovalStatus}.
	 */
	private ApprovalStatus toApprovalStatus(String rawApprovalStatus) {
		if (rawApprovalStatus == null || rawApprovalStatus.isBlank()) {
			return null;
		}
		ApprovalStatus status = ApprovalStatus.valueOf(rawApprovalStatus);
		return status == ApprovalStatus.NOT_REQUIRED ? null : status;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	public Instant toInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}
}
