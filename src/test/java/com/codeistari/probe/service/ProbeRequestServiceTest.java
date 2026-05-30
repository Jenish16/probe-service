package com.codeistari.probe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeistari.forge.artifact.grpc.proto.CreateForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.probe.client.grpc.GrpcForgeJobClient;
import com.codeistari.probe.client.rest.RestForgeJobClient;
import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeRequestServiceTest {

	@Mock private RestForgeJobClient restForgeJobClient;
	@Mock private GrpcForgeJobClient grpcForgeJobClient;

	private ProbeRequestService service;

	@BeforeEach
	void setUp() {
		service = new ProbeRequestService(restForgeJobClient, grpcForgeJobClient, new ProbeRequestMapper());
	}

	@Test
	void createForgeJobViaRestUsesRestClient() {
		CreateProbeForgeJobRequest request = sampleRequest();
		ForgeJobRestResponse forgeResponse = sampleRestResponse();

		when(restForgeJobClient.createForgeJob(any(ForgeCreateJobRestRequest.class)))
				.thenReturn(forgeResponse);

		ProbeForgeJobResponse response = service.createForgeJobViaRest(request);

		verify(restForgeJobClient).createForgeJob(any(ForgeCreateJobRestRequest.class));
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void createForgeJobViaGrpcUsesGrpcClient() {
		CreateProbeForgeJobRequest request = sampleRequest();
		ForgeJobGrpcResponse grpcResponse = sampleGrpcResponse();

		when(grpcForgeJobClient.createForgeJob(any(CreateForgeJobGrpcRequest.class)))
				.thenReturn(grpcResponse);

		ProbeForgeJobResponse response = service.createForgeJobViaGrpc(request);

		verify(grpcForgeJobClient).createForgeJob(any(CreateForgeJobGrpcRequest.class));
		assertThat(response.transport()).isEqualTo(ForgeTransport.GRPC);
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	private CreateProbeForgeJobRequest sampleRequest() {
		return new CreateProbeForgeJobRequest(
				"ember-ring", ArtifactType.RING, ForgeMaterial.MITHRIL, "ranger", 7);
	}

	private ForgeJobRestResponse sampleRestResponse() {
		return new ForgeJobRestResponse(
				"fj_123",
				"ember-ring",
				"RING",
				"MITHRIL",
				"ranger",
				7,
				"QUEUED",
				Instant.parse("2026-05-27T10:00:00Z"));
	}

	private ForgeJobGrpcResponse sampleGrpcResponse() {
		return ForgeJobGrpcResponse.newBuilder()
				.setForgeJobId("fj_123")
				.setArtifactName("ember-ring")
				.setArtifactType("RING")
				.setMaterial("MITHRIL")
				.setRequestedBy("ranger")
				.setPowerLevel(7)
				.setStatus("QUEUED")
				.setCreatedAt(
						Timestamp.newBuilder().setSeconds(Instant.parse("2026-05-27T10:00:00Z").getEpochSecond()))
				.build();
	}
}
