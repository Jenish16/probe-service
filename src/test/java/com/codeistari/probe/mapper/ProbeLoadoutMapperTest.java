package com.codeistari.probe.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeistari.forge.artifact.grpc.proto.CreateLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutAttemptGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemAttemptsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RejectedLoadoutItemGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutGrpcRequest;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
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
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbeLoadoutMapperTest {

	private final ProbeLoadoutMapper mapper = new ProbeLoadoutMapper();

	@Test
	void mapsCreateRequestToForgeRestRequest() {
		CreateProbeLoadoutRequest request = new CreateProbeLoadoutRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ProbeLoadoutItemRequest("ember-ring", "RING", "MITHRIL", 7)));

		ForgeCreateLoadoutRestRequest restRequest = mapper.toForgeCreateLoadoutRestRequest(request);

		assertThat(restRequest.requestReference()).isEqualTo("req-1");
		assertThat(restRequest.items()).hasSize(1);
		assertThat(restRequest.items().get(0).artifactType()).isEqualTo("RING");
	}

	@Test
	void mapsCreateRequestToGrpcRequest() {
		CreateProbeLoadoutRequest request = new CreateProbeLoadoutRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ProbeLoadoutItemRequest("ember-ring", "RING", "MITHRIL", 7)));

		CreateLoadoutGrpcRequest grpcRequest = mapper.toCreateLoadoutGrpcRequest(request);

		assertThat(grpcRequest.getRequestReference()).isEqualTo("req-1");
		assertThat(grpcRequest.getItemsList()).hasSize(1);
		assertThat(grpcRequest.getItems(0).getArtifactType()).isEqualTo("RING");
	}

	@Test
	void mapsRestResponseIncludingRejectedItems() {
		ForgeLoadoutRestResponse restResponse = new ForgeLoadoutRestResponse(
				"lo_123",
				"Dragon Raid Kit",
				"questmaster",
				"ACCEPTED",
				List.of(new ForgeLoadoutItemRestResponse(
						"li_1", "ember-ring", "RING", "MITHRIL", 7, "QUEUED", null, null, null, null)),
				List.of(new ForgeRejectedLoadoutItemRestResponse("bad-item", "RING", "MITHRIL", 999, "powerLevel must be between 1 and 10")));

		ProbeLoadoutResponse response = mapper.toProbeLoadoutResponse(restResponse, ForgeTransport.REST);

		assertThat(response.loadoutId()).isEqualTo("lo_123");
		assertThat(response.items()).hasSize(1);
		assertThat(response.rejectedItems()).hasSize(1);
		assertThat(response.rejectedItems().get(0).reason()).isEqualTo("powerLevel must be between 1 and 10");
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
	}

	@Test
	void mapsGrpcResponseIncludingRejectedItems() {
		LoadoutGrpcResponse grpcResponse = LoadoutGrpcResponse.newBuilder()
				.setLoadoutId("lo_123")
				.setLoadoutName("Dragon Raid Kit")
				.setRequestedBy("questmaster")
				.setStatus("ACCEPTED")
				.addItems(LoadoutItemGrpcResponse.newBuilder()
						.setLoadoutItemId("li_1")
						.setArtifactName("ember-ring")
						.setArtifactType("RING")
						.setMaterial("MITHRIL")
						.setPowerLevel(7)
						.setStatus("QUEUED"))
				.addRejectedItems(RejectedLoadoutItemGrpcResponse.newBuilder()
						.setArtifactName("bad-item")
						.setArtifactType("RING")
						.setMaterial("MITHRIL")
						.setPowerLevel(999)
						.setReason("powerLevel must be between 1 and 10"))
				.build();

		ProbeLoadoutResponse response = mapper.toProbeLoadoutResponse(grpcResponse, ForgeTransport.GRPC);

		assertThat(response.items()).hasSize(1);
		assertThat(response.rejectedItems()).hasSize(1);
		assertThat(response.items().get(0).approvalStatus()).isNull();
		assertThat(response.transport()).isEqualTo(ForgeTransport.GRPC);
	}

	@Test
	void mapsRetryRequestKeepingNullFieldsAsPriorValueOmissions() {
		RetryProbeLoadoutRequest request =
				new RetryProbeLoadoutRequest(List.of(new RetryProbeLoadoutItemRequest("li_1", null, "SHIELD", null, 9)));

		var forgeRequest = mapper.toForgeRetryLoadoutRestRequest(request);
		assertThat(forgeRequest.items().get(0).loadoutItemId()).isEqualTo("li_1");
		assertThat(forgeRequest.items().get(0).artifactName()).isNull();
		assertThat(forgeRequest.items().get(0).artifactType()).isEqualTo("SHIELD");

		RetryLoadoutGrpcRequest grpcRequest = mapper.toRetryLoadoutGrpcRequest("lo_123", request);
		assertThat(grpcRequest.getLoadoutId()).isEqualTo("lo_123");
		assertThat(grpcRequest.getItems(0).getLoadoutItemId()).isEqualTo("li_1");
		assertThat(grpcRequest.getItems(0).getArtifactType()).isEqualTo("SHIELD");
		assertThat(grpcRequest.getItems(0).getPowerLevel()).isEqualTo(9);
	}

	@Test
	void mapsAttemptHistoryOrderedOldestToNewestFromRest() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		ForgeLoadoutAttemptsRestResponse restResponse = new ForgeLoadoutAttemptsRestResponse(
				"li_1",
				List.of(
						new ForgeLoadoutAttemptRestResponse(1, "fj_1", null, "FAILED", null, null, "forge cracked", createdAt),
						new ForgeLoadoutAttemptRestResponse(2, "fj_2", null, "COMPLETED", null, null, null, createdAt)));

		ProbeLoadoutAttemptHistoryResponse response = mapper.toProbeLoadoutAttemptHistoryResponse(restResponse);

		assertThat(response.attempts()).hasSize(2);
		assertThat(response.attempts().get(0).status()).isEqualTo("FAILED");
		assertThat(response.attempts().get(1).status()).isEqualTo("COMPLETED");
	}

	@Test
	void mapsAttemptHistoryFromGrpcConvertingBlankOptionalFieldsToNull() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		Timestamp timestamp =
				Timestamp.newBuilder().setSeconds(createdAt.getEpochSecond()).setNanos(createdAt.getNano()).build();
		LoadoutItemAttemptsGrpcResponse grpcResponse = LoadoutItemAttemptsGrpcResponse.newBuilder()
				.setLoadoutItemId("li_1")
				.addAttempts(LoadoutAttemptGrpcResponse.newBuilder()
						.setAttemptNumber(1)
						.setForgeJobId("fj_1")
						.setStatus("FAILED")
						.setFailureReason("forge cracked")
						.setCreatedAt(timestamp))
				.build();

		ProbeLoadoutAttemptHistoryResponse response = mapper.toProbeLoadoutAttemptHistoryResponse(grpcResponse);

		assertThat(response.attempts().get(0).failureReason()).isEqualTo("forge cracked");
		assertThat(response.attempts().get(0).approvalStatus()).isNull();
		assertThat(response.attempts().get(0).createdAt()).isEqualTo(createdAt);
	}

	@Test
	void convertsProtobufTimestampToInstant() {
		Instant expected = Instant.parse("2026-05-27T10:00:00Z");
		Timestamp timestamp =
				Timestamp.newBuilder().setSeconds(expected.getEpochSecond()).setNanos(expected.getNano()).build();

		assertThat(mapper.toInstant(timestamp)).isEqualTo(expected);
	}
}
