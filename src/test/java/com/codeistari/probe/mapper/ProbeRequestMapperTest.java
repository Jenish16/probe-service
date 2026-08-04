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
						"moon-blade",
						ArtifactType.BLADE,
						ForgeMaterial.ELVEN_STEEL,
						"ranger",
						5,
						"req-ref-1",
						null);

		ForgeCreateJobRestRequest restRequest = mapper.toForgeCreateJobRestRequest(request);

		assertThat(restRequest.artifactName()).isEqualTo("moon-blade");
		assertThat(restRequest.artifactType()).isEqualTo("BLADE");
		assertThat(restRequest.material()).isEqualTo("ELVEN_STEEL");
		assertThat(restRequest.requestedBy()).isEqualTo("ranger");
		assertThat(restRequest.powerLevel()).isEqualTo(5);
		assertThat(restRequest.requesterReference()).isEqualTo("req-ref-1");
		assertThat(restRequest.originalRequestId()).isNull();
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
						createdAt,
						"req-ref-1",
						null,
						"PENDING_APPROVAL",
						null,
						null);

		ProbeForgeJobResponse response =
				mapper.toProbeForgeJobResponse(restResponse, ForgeTransport.REST);

		assertThat(response.forgeJobId()).isEqualTo("fj_abc");
		assertThat(response.artifactType()).isEqualTo(ArtifactType.STAFF);
		assertThat(response.material()).isEqualTo(ForgeMaterial.SILVERWOOD);
		assertThat(response.createdAt()).isEqualTo(createdAt);
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
		assertThat(response.requesterReference()).isEqualTo("req-ref-1");
		assertThat(response.approvalStatus()).isEqualTo(com.codeistari.probe.domain.ApprovalStatus.PENDING_APPROVAL);
	}

	@Test
	void mapsNotRequiredApprovalStatusToNull() {
		ForgeJobRestResponse restResponse =
				new ForgeJobRestResponse(
						"fj_abc",
						"white-staff",
						"STAFF",
						"SILVERWOOD",
						"ranger",
						3,
						"QUEUED",
						Instant.parse("2026-05-27T10:00:00Z"),
						"req-ref-1",
						null,
						"NOT_REQUIRED",
						null,
						null);

		ProbeForgeJobResponse response =
				mapper.toProbeForgeJobResponse(restResponse, ForgeTransport.REST);

		assertThat(response.approvalStatus()).isNull();
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
	void mapsPendingApprovalSummaries() {
		Instant submittedAt = Instant.parse("2026-05-27T10:00:00Z");
		Instant expiresAt = Instant.parse("2026-05-27T10:30:00Z");
		ForgeJobRestResponse restResponse =
				new ForgeJobRestResponse(
						"fj_pending",
						"moon-blade",
						"BLADE",
						"ELVEN_STEEL",
						"ranger",
						9,
						null,
						submittedAt,
						"req-ref-9",
						null,
						"PENDING_APPROVAL",
						expiresAt,
						null);

		var summaries = mapper.toPendingApprovalSummaries(java.util.List.of(restResponse));

		assertThat(summaries).hasSize(1);
		var summary = summaries.get(0);
		assertThat(summary.forgeJobId()).isEqualTo("fj_pending");
		assertThat(summary.requesterReference()).isEqualTo("req-ref-9");
		assertThat(summary.powerLevel()).isEqualTo(9);
		assertThat(summary.submittedAt()).isEqualTo(submittedAt);
		assertThat(summary.approvalExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void mapsDecisionHistoryResponse() {
		Instant decidedAt = Instant.parse("2026-05-27T10:15:00Z");
		var forgeHistory =
				new com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse(
						"req-ref-9",
						java.util.List.of(),
						java.util.List.of(
								new com.codeistari.probe.dto.client.forge.response.ForgeDecisionEntryRestResponse(
										"decision-1", "fj_pending", "REJECT", "op-1", "insufficient materials", decidedAt)));

		var history = mapper.toDecisionHistoryResponse(forgeHistory);

		assertThat(history.requesterReference()).isEqualTo("req-ref-9");
		assertThat(history.decisions()).hasSize(1);
		var entry = history.decisions().get(0);
		assertThat(entry.forgeJobId()).isEqualTo("fj_pending");
		assertThat(entry.decision())
				.isEqualTo(com.codeistari.probe.domain.ApprovalDecisionType.REJECT);
		assertThat(entry.reason()).isEqualTo("insufficient materials");
		assertThat(entry.decidedAt()).isEqualTo(decidedAt);
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
