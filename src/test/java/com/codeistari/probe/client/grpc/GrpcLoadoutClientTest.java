package com.codeistari.probe.client.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.codeistari.forge.artifact.grpc.proto.CancelLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.CreateLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.GetLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.GetLoadoutItemAttemptsGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemAttemptsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutItemGrpcRequest;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GrpcLoadoutClientTest {

	@Mock private LoadoutServiceGrpc.LoadoutServiceBlockingStub blockingStub;
	@Mock private LoadoutServiceGrpc.LoadoutServiceBlockingStub deadlineStub;

	private GrpcLoadoutClient client;

	@BeforeEach
	void setUp() {
		ForgeClientProperties properties = new ForgeClientProperties(
				new ForgeClientProperties.Rest("http://localhost:8081", 2000),
				new ForgeClientProperties.Grpc("localhost", 9091, 2000L));
		client = new GrpcLoadoutClient(blockingStub, properties);
	}

	@Test
	void mapsSuccessfulCreateResponse() {
		CreateLoadoutGrpcRequest request = CreateLoadoutGrpcRequest.newBuilder()
				.setRequestReference("req-1")
				.setLoadoutName("Dragon Raid Kit")
				.setRequestedBy("questmaster")
				.addItems(LoadoutItemGrpcRequest.newBuilder()
						.setArtifactName("ember-ring")
						.setArtifactType("RING")
						.setMaterial("MITHRIL")
						.setPowerLevel(7))
				.build();
		LoadoutGrpcResponse grpcResponse = LoadoutGrpcResponse.newBuilder()
				.setLoadoutId("lo_123")
				.setLoadoutName("Dragon Raid Kit")
				.setRequestedBy("questmaster")
				.setStatus("ACCEPTED")
				.build();

		when(blockingStub.withDeadlineAfter(2000L, TimeUnit.MILLISECONDS)).thenReturn(deadlineStub);
		when(deadlineStub.createLoadout(request)).thenReturn(grpcResponse);

		LoadoutGrpcResponse response = client.createLoadout(request);

		assertThat(response.getLoadoutId()).isEqualTo("lo_123");
		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
	}

	@Test
	void mapsAlreadyExistsToConflict() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createLoadout(any(CreateLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(
						Status.ALREADY_EXISTS.withDescription("requestReference already used with different content")));

		assertThatThrownBy(() -> client.createLoadout(CreateLoadoutGrpcRequest.newBuilder().build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("requestReference already used with different content")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void mapsInvalidArgument() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createLoadout(any(CreateLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("bad input")));

		assertThatThrownBy(() -> client.createLoadout(CreateLoadoutGrpcRequest.newBuilder().build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("bad input")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void mapsNotFoundOnGet() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.getLoadout(any(GetLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.NOT_FOUND.withDescription("Loadout not found: lo_missing")));

		assertThatThrownBy(() -> client.getLoadoutGrpc("lo_missing"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("Loadout not found: lo_missing")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void mapsCancelSuccess() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.cancelLoadout(any(CancelLoadoutGrpcRequest.class)))
				.thenReturn(LoadoutGrpcResponse.newBuilder()
						.setLoadoutId("lo_123")
						.setStatus("CANCELLED")
						.build());

		LoadoutGrpcResponse response = client.cancelLoadoutGrpc("lo_123");

		assertThat(response.getStatus()).isEqualTo("CANCELLED");
	}

	@Test
	void mapsRetrySuccess() {
		RetryLoadoutGrpcRequest request = RetryLoadoutGrpcRequest.newBuilder()
				.setLoadoutId("lo_123")
				.addItems(RetryLoadoutItemGrpcRequest.newBuilder().setLoadoutItemId("li_1"))
				.build();
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.retryLoadout(request))
				.thenReturn(LoadoutGrpcResponse.newBuilder()
						.setLoadoutId("lo_123")
						.setStatus("IN_PROGRESS")
						.build());

		LoadoutGrpcResponse response = client.retryLoadout(request);

		assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
	}

	@Test
	void mapsRetryInvalidArgument() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.retryLoadout(any(RetryLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(
						Status.INVALID_ARGUMENT.withDescription("retry request must include exactly the currently retry-eligible items")));

		assertThatThrownBy(() -> client.retryLoadout(RetryLoadoutGrpcRequest.newBuilder().build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void mapsItemAttemptsOrderedOldestToNewest() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.getLoadoutItemAttempts(any(GetLoadoutItemAttemptsGrpcRequest.class)))
				.thenReturn(LoadoutItemAttemptsGrpcResponse.newBuilder()
						.setLoadoutItemId("li_1")
						.build());

		LoadoutItemAttemptsGrpcResponse response = client.getLoadoutItemAttemptsGrpc("lo_123", "li_1");

		assertThat(response.getLoadoutItemId()).isEqualTo("li_1");
	}

	@Test
	void mapsDeadlineExceeded() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createLoadout(any(CreateLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

		assertThatThrownBy(() -> client.createLoadout(CreateLoadoutGrpcRequest.newBuilder().build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("timed out after 2000ms")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
	}

	@Test
	void mapsInternal() {
		when(blockingStub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
		when(deadlineStub.createLoadout(any(CreateLoadoutGrpcRequest.class)))
				.thenThrow(new StatusRuntimeException(Status.INTERNAL.withDescription("boom")));

		assertThatThrownBy(() -> client.createLoadout(CreateLoadoutGrpcRequest.newBuilder().build()))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("internal error: boom")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_GATEWAY);
	}
}
