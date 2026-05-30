package com.codeistari.probe.mapper;

import com.codeistari.forge.artifact.grpc.proto.CreateForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ProbeRequestMapper {

	public ForgeCreateJobRestRequest toForgeCreateJobRestRequest(CreateProbeForgeJobRequest request) {
		return new ForgeCreateJobRestRequest(
				request.artifactName(),
				request.artifactType().name(),
				request.material().name(),
				request.requestedBy(),
				request.powerLevel());
	}

	public CreateForgeJobGrpcRequest toCreateForgeJobGrpcRequest(CreateProbeForgeJobRequest request) {
		return CreateForgeJobGrpcRequest.newBuilder()
				.setArtifactName(request.artifactName())
				.setArtifactType(request.artifactType().name())
				.setMaterial(request.material().name())
				.setRequestedBy(request.requestedBy())
				.setPowerLevel(request.powerLevel())
				.build();
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
				transport);
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
				response.getStatus(),
				toInstant(response.getCreatedAt()),
				transport);
	}

	public Instant toInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}
}
