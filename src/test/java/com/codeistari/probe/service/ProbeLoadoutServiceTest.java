package com.codeistari.probe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeistari.forge.artifact.grpc.proto.CreateLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemAttemptsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutGrpcRequest;
import com.codeistari.probe.client.grpc.GrpcLoadoutClient;
import com.codeistari.probe.client.rest.RestLoadoutClient;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;
import com.codeistari.probe.dto.request.CreateProbeLoadoutRequest;
import com.codeistari.probe.dto.request.ProbeLoadoutItemRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutItemRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutRequest;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptHistoryResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.codeistari.probe.mapper.ProbeLoadoutMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeLoadoutServiceTest {

	@Mock private RestLoadoutClient restLoadoutClient;
	@Mock private GrpcLoadoutClient grpcLoadoutClient;

	private ProbeLoadoutService service;

	@BeforeEach
	void setUp() {
		service = new ProbeLoadoutService(restLoadoutClient, grpcLoadoutClient, new ProbeLoadoutMapper());
	}

	@Test
	void createLoadoutViaRestUsesRestClient() {
		CreateProbeLoadoutRequest request = sampleCreateRequest();
		when(restLoadoutClient.createLoadout(any(ForgeCreateLoadoutRestRequest.class))).thenReturn(sampleRestResponse());

		ProbeLoadoutResponse response = service.createLoadoutViaRest(request);

		verify(restLoadoutClient).createLoadout(any(ForgeCreateLoadoutRestRequest.class));
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void createLoadoutViaGrpcUsesGrpcClient() {
		CreateProbeLoadoutRequest request = sampleCreateRequest();
		when(grpcLoadoutClient.createLoadout(any(CreateLoadoutGrpcRequest.class))).thenReturn(sampleGrpcResponse());

		ProbeLoadoutResponse response = service.createLoadoutViaGrpc(request);

		verify(grpcLoadoutClient).createLoadout(any(CreateLoadoutGrpcRequest.class));
		assertThat(response.transport()).isEqualTo(ForgeTransport.GRPC);
		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void getLoadoutViaRestUsesRestClient() {
		when(restLoadoutClient.getLoadout("lo_123")).thenReturn(sampleRestResponse());

		ProbeLoadoutResponse response = service.getLoadoutViaRest("lo_123");

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void getLoadoutViaGrpcUsesGrpcClient() {
		when(grpcLoadoutClient.getLoadoutGrpc("lo_123")).thenReturn(sampleGrpcResponse());

		ProbeLoadoutResponse response = service.getLoadoutViaGrpc("lo_123");

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void cancelLoadoutViaRestUsesRestClient() {
		when(restLoadoutClient.cancelLoadout("lo_123")).thenReturn(sampleRestResponse());

		ProbeLoadoutResponse response = service.cancelLoadoutViaRest("lo_123");

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void cancelLoadoutViaGrpcUsesGrpcClient() {
		when(grpcLoadoutClient.cancelLoadoutGrpc("lo_123")).thenReturn(sampleGrpcResponse());

		ProbeLoadoutResponse response = service.cancelLoadoutViaGrpc("lo_123");

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void retryLoadoutViaRestUsesRestClient() {
		RetryProbeLoadoutRequest request =
				new RetryProbeLoadoutRequest(List.of(new RetryProbeLoadoutItemRequest("li_1", null, null, null, null)));
		when(restLoadoutClient.retryLoadout(anyString(), any(ForgeRetryLoadoutRestRequest.class)))
				.thenReturn(sampleRestResponse());

		ProbeLoadoutResponse response = service.retryLoadoutViaRest("lo_123", request);

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void retryLoadoutViaGrpcUsesGrpcClient() {
		RetryProbeLoadoutRequest request =
				new RetryProbeLoadoutRequest(List.of(new RetryProbeLoadoutItemRequest("li_1", null, null, null, null)));
		when(grpcLoadoutClient.retryLoadout(any(RetryLoadoutGrpcRequest.class))).thenReturn(sampleGrpcResponse());

		ProbeLoadoutResponse response = service.retryLoadoutViaGrpc("lo_123", request);

		assertThat(response.loadoutId()).isEqualTo("lo_123");
	}

	@Test
	void getItemAttemptsViaRestUsesRestClient() {
		when(restLoadoutClient.getItemAttempts("lo_123", "li_1"))
				.thenReturn(new ForgeLoadoutAttemptsRestResponse("li_1", List.of()));

		ProbeLoadoutAttemptHistoryResponse response = service.getItemAttemptsViaRest("lo_123", "li_1");

		assertThat(response.loadoutItemId()).isEqualTo("li_1");
	}

	@Test
	void getItemAttemptsViaGrpcUsesGrpcClient() {
		when(grpcLoadoutClient.getLoadoutItemAttemptsGrpc("lo_123", "li_1"))
				.thenReturn(LoadoutItemAttemptsGrpcResponse.newBuilder().setLoadoutItemId("li_1").build());

		ProbeLoadoutAttemptHistoryResponse response = service.getItemAttemptsViaGrpc("lo_123", "li_1");

		assertThat(response.loadoutItemId()).isEqualTo("li_1");
	}

	private CreateProbeLoadoutRequest sampleCreateRequest() {
		return new CreateProbeLoadoutRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ProbeLoadoutItemRequest("ember-ring", "RING", "MITHRIL", 7)));
	}

	private ForgeLoadoutRestResponse sampleRestResponse() {
		return new ForgeLoadoutRestResponse("lo_123", "Dragon Raid Kit", "questmaster", "ACCEPTED", List.of(), List.of());
	}

	private LoadoutGrpcResponse sampleGrpcResponse() {
		return LoadoutGrpcResponse.newBuilder()
				.setLoadoutId("lo_123")
				.setLoadoutName("Dragon Raid Kit")
				.setRequestedBy("questmaster")
				.setStatus("ACCEPTED")
				.build();
	}
}
