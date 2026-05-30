package com.codeistari.probe.client.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.codeistari.forge.artifact.grpc.proto.ArtifactForgeServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.CreateForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.GetForgeJobGrpcRequest;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import com.google.protobuf.Timestamp;
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
class GrpcForgeJobClientTest {

	@Mock private ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub;
	@Mock private ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub deadlineStub;

	private GrpcForgeJobClient client;

	@BeforeEach
	void setUp() {
		ForgeClientProperties properties =
				new ForgeClientProperties(
						new ForgeClientProperties.Rest("http://localhost:8081", 2000),
						new ForgeClientProperties.Grpc("localhost", 9091, 2000L));
		client = new GrpcForgeJobClient(blockingStub, properties, new ProbeRequestMapper());
	}

	@Test
	void mapsSuccessfulCreateResponse() {
		CreateForgeJobGrpcRequest request =
				CreateForgeJobGrpcRequest.newBuilder()
						.setArtifactName("ember-ring")
						.setArtifactType("RING")
						.setMaterial("MITHRIL")
						.setRequestedBy("ranger")
						.setPowerLevel(7)
						.build();
		ForgeJobGrpcResponse grpcResponse = sampleResponse();

		when(blockingStub.withDeadlineAfter(2000L, TimeUnit.MILLISECONDS)).thenReturn(deadlineStub);
		when(deadlineStub.createForgeJob(request)).thenReturn(grpcResponse);

		ForgeJobGrpcResponse response = client.createForgeJob(request);

		assertThat(response.getForgeJobId()).isEqualTo("fj_123");
		assertThat(response.getStatus()).isEqualTo("QUEUED");
	}

	@Test
	void mapsInvalidArgument() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createForgeJob(any(CreateForgeJobGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("bad input")));

		assertThatThrownBy(
						() ->
								client.createForgeJob(
										CreateForgeJobGrpcRequest.newBuilder()
												.setArtifactName("")
												.build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("bad input")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void mapsNotFound() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.getForgeJob(any(GetForgeJobGrpcRequest.class)))
				.thenThrow(
						new StatusRuntimeException(
								Status.NOT_FOUND.withDescription("Forge job not found: fj_missing")));

		assertThatThrownBy(() -> client.getForgeJobGrpc("fj_missing"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("Forge job not found: fj_missing")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void mapsDeadlineExceeded() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createForgeJob(any(CreateForgeJobGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

		assertThatThrownBy(
						() ->
								client.createForgeJob(
										CreateForgeJobGrpcRequest.newBuilder()
												.setArtifactName("ember-ring")
												.build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("timed out after 2000ms")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
	}

	@Test
	void mapsInternal() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createForgeJob(any(CreateForgeJobGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.INTERNAL.withDescription("boom")));

		assertThatThrownBy(
						() ->
								client.createForgeJob(
										CreateForgeJobGrpcRequest.newBuilder()
												.setArtifactName("ember-ring")
												.build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("internal error: boom")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	private ForgeJobGrpcResponse sampleResponse() {
		Instant createdAt = Instant.parse("2026-05-27T10:00:00Z");
		return ForgeJobGrpcResponse.newBuilder()
				.setForgeJobId("fj_123")
				.setArtifactName("ember-ring")
				.setArtifactType("RING")
				.setMaterial("MITHRIL")
				.setRequestedBy("ranger")
				.setPowerLevel(7)
				.setStatus("QUEUED")
				.setCreatedAt(
						Timestamp.newBuilder()
								.setSeconds(createdAt.getEpochSecond())
								.setNanos(createdAt.getNano()))
				.build();
	}
}
