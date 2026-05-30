package com.codeistari.probe.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;

class ProbeRequestMapperTest {

	private final ProbeRequestMapper mapper = new ProbeRequestMapper();

	@Test
	void mapsPublicRequestToDownstreamRestRequest() {
		CreateProbeForgeJobRequest request =
				new CreateProbeForgeJobRequest(
						"moon-blade", ArtifactType.BLADE, ForgeMaterial.ELVEN_STEEL, "ranger", 5);

		ForgeCreateJobRestRequest restRequest = mapper.toForgeCreateJobRestRequest(request);

		assertThat(restRequest.artifactName()).isEqualTo("moon-blade");
		assertThat(restRequest.artifactType()).isEqualTo("BLADE");
		assertThat(restRequest.material()).isEqualTo("ELVEN_STEEL");
		assertThat(restRequest.requestedBy()).isEqualTo("ranger");
		assertThat(restRequest.powerLevel()).isEqualTo(5);
	}

	@Test
	void mapsDownstreamRestResponseToPublicResponse() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		ForgeJobRestResponse restResponse =
				new ForgeJobRestResponse(
						"fj_abc",
						"white-staff",
						"STAFF",
						"SILVERWOOD",
						"ranger",
						8,
						"QUEUED",
						createdAt);

		ProbeForgeJobResponse response =
				mapper.toProbeForgeJobResponse(restResponse, ForgeTransport.REST);

		assertThat(response.forgeJobId()).isEqualTo("fj_abc");
		assertThat(response.artifactType()).isEqualTo(ArtifactType.STAFF);
		assertThat(response.material()).isEqualTo(ForgeMaterial.SILVERWOOD);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
	}

	@Test
	void mapsGrpcResponseToPublicResponse() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		ForgeJobGrpcResponse grpcResponse =
				ForgeJobGrpcResponse.newBuilder()
						.setForgeJobId("fj_grpc")
						.setArtifactName("ranger-amulet")
						.setArtifactType("AMULET")
						.setMaterial("OBSIDIAN")
						.setRequestedBy("ranger")
						.setPowerLevel(6)
						.setStatus("QUEUED")
						.setCreatedAt(
								Timestamp.newBuilder()
										.setSeconds(createdAt.getEpochSecond())
										.setNanos(createdAt.getNano()))
						.build();

		ProbeForgeJobResponse response =
				mapper.toProbeForgeJobResponse(grpcResponse, ForgeTransport.GRPC);

		assertThat(response.forgeJobId()).isEqualTo("fj_grpc");
		assertThat(response.artifactType()).isEqualTo(ArtifactType.AMULET);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.transport()).isEqualTo(ForgeTransport.GRPC);
	}

	@Test
	void convertsProtobufTimestampToInstant() {
		Instant expected = Instant.parse("2026-05-27T10:00:00Z");
		Timestamp timestamp =
				Timestamp.newBuilder()
						.setSeconds(expected.getEpochSecond())
						.setNanos(expected.getNano())
						.build();

		assertThat(mapper.toInstant(timestamp)).isEqualTo(expected);
	}
}
