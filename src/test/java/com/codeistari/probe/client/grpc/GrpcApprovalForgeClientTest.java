package com.codeistari.probe.client.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.codeistari.forge.artifact.grpc.proto.ApproveForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ArtifactForgeServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.CancelForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobHistoryGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.GetApprovalHistoryGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ListPendingApprovalsGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ListPendingApprovalsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RejectForgeJobGrpcRequest;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GrpcApprovalForgeClientTest {

	@Mock private ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub;
	@Mock private ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub deadlineStub;

	private GrpcApprovalForgeClient client;

	@BeforeEach
	void setUp() {
		ForgeClientProperties properties =
				new ForgeClientProperties(
						new ForgeClientProperties.Rest("http://localhost:8081", 2000),
						new ForgeClientProperties.Grpc("localhost", 9091, 2000L));
		client = new GrpcApprovalForgeClient(blockingStub, properties, new ProbeRequestMapper());
	}

	@Test
	void listsPendingApprovals() {
		when(blockingStub.withDeadlineAfter(2000L, TimeUnit.MILLISECONDS)).thenReturn(deadlineStub);
		when(deadlineStub.listPendingApprovals(any(ListPendingApprovalsGrpcRequest.class)))
				.thenReturn(
						ListPendingApprovalsGrpcResponse.newBuilder().addPending(sampleResponse()).build());

		var pending = client.listPendingApprovals();

		assertThat(pending).hasSize(1);
		assertThat(pending.get(0).forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void approvesRequest() {
		when(blockingStub.withDeadlineAfter(2000L, TimeUnit.MILLISECONDS)).thenReturn(deadlineStub);
		when(deadlineStub.approveForgeJob(any(ApproveForgeJobGrpcRequest.class)))
				.thenReturn(sampleResponse());

		var response = client.approveRequest("fj_123", "op-1");

		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void rejectMapsFailedPreconditionToConflict() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.rejectForgeJob(any(RejectForgeJobGrpcRequest.class)))
				.thenThrow(
						new StatusRuntimeException(
								Status.FAILED_PRECONDITION.withDescription("already terminal")));

		assertThatThrownBy(() -> client.rejectRequest("fj_123", "op-1", "bad materials"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void cancelMapsNotFound() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.cancelForgeJob(any(CancelForgeJobGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.NOT_FOUND.withDescription("not found")));

		assertThatThrownBy(() -> client.cancelRequest("fj_missing", "ranger"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getsDecisionHistory() {
		when(blockingStub.withDeadlineAfter(2000L, TimeUnit.MILLISECONDS)).thenReturn(deadlineStub);
		when(deadlineStub.getApprovalHistory(any(GetApprovalHistoryGrpcRequest.class)))
				.thenReturn(
						ForgeJobHistoryGrpcResponse.newBuilder().setRequesterReference("req-ref-1").build());

		var history = client.getDecisionHistory("req-ref-1");

		assertThat(history.requesterReference()).isEqualTo("req-ref-1");
		assertThat(history.decisions()).isEmpty();
	}

	private ForgeJobGrpcResponse sampleResponse() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		return ForgeJobGrpcResponse.newBuilder()
				.setForgeJobId("fj_123")
				.setArtifactName("ember-ring")
				.setArtifactType("RING")
				.setMaterial("MITHRIL")
				.setRequestedBy("ranger")
				.setPowerLevel(9)
				.setStatus("QUEUED")
				.setApprovalStatus("APPROVED")
				.setRequesterReference("req-ref-1")
				.setCreatedAt(
						com.google.protobuf.Timestamp.newBuilder()
								.setSeconds(createdAt.getEpochSecond())
								.setNanos(createdAt.getNano()))
				.build();
	}
}
