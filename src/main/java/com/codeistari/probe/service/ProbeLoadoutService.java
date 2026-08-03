package com.codeistari.probe.service;

import com.codeistari.probe.client.grpc.GrpcLoadoutClient;
import com.codeistari.probe.client.rest.RestLoadoutClient;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.request.CreateProbeLoadoutRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutRequest;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptHistoryResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.codeistari.probe.mapper.ProbeLoadoutMapper;
import org.springframework.stereotype.Service;

@Service
public class ProbeLoadoutService {

	private final RestLoadoutClient restLoadoutClient;
	private final GrpcLoadoutClient grpcLoadoutClient;
	private final ProbeLoadoutMapper mapper;

	public ProbeLoadoutService(
			RestLoadoutClient restLoadoutClient, GrpcLoadoutClient grpcLoadoutClient, ProbeLoadoutMapper mapper) {
		this.restLoadoutClient = restLoadoutClient;
		this.grpcLoadoutClient = grpcLoadoutClient;
		this.mapper = mapper;
	}

	public ProbeLoadoutResponse createLoadoutViaRest(CreateProbeLoadoutRequest request) {
		var forgeRequest = mapper.toForgeCreateLoadoutRestRequest(request);
		var forgeResponse = restLoadoutClient.createLoadout(forgeRequest);
		return mapper.toProbeLoadoutResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeLoadoutResponse createLoadoutViaGrpc(CreateProbeLoadoutRequest request) {
		var grpcRequest = mapper.toCreateLoadoutGrpcRequest(request);
		var grpcResponse = grpcLoadoutClient.createLoadout(grpcRequest);
		return mapper.toProbeLoadoutResponse(grpcResponse, ForgeTransport.GRPC);
	}

	public ProbeLoadoutResponse getLoadoutViaRest(String loadoutId) {
		var forgeResponse = restLoadoutClient.getLoadout(loadoutId);
		return mapper.toProbeLoadoutResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeLoadoutResponse getLoadoutViaGrpc(String loadoutId) {
		var grpcResponse = grpcLoadoutClient.getLoadoutGrpc(loadoutId);
		return mapper.toProbeLoadoutResponse(grpcResponse, ForgeTransport.GRPC);
	}

	public ProbeLoadoutResponse cancelLoadoutViaRest(String loadoutId) {
		var forgeResponse = restLoadoutClient.cancelLoadout(loadoutId);
		return mapper.toProbeLoadoutResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeLoadoutResponse cancelLoadoutViaGrpc(String loadoutId) {
		var grpcResponse = grpcLoadoutClient.cancelLoadoutGrpc(loadoutId);
		return mapper.toProbeLoadoutResponse(grpcResponse, ForgeTransport.GRPC);
	}

	public ProbeLoadoutResponse retryLoadoutViaRest(String loadoutId, RetryProbeLoadoutRequest request) {
		var forgeRequest = mapper.toForgeRetryLoadoutRestRequest(request);
		var forgeResponse = restLoadoutClient.retryLoadout(loadoutId, forgeRequest);
		return mapper.toProbeLoadoutResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeLoadoutResponse retryLoadoutViaGrpc(String loadoutId, RetryProbeLoadoutRequest request) {
		var grpcRequest = mapper.toRetryLoadoutGrpcRequest(loadoutId, request);
		var grpcResponse = grpcLoadoutClient.retryLoadout(grpcRequest);
		return mapper.toProbeLoadoutResponse(grpcResponse, ForgeTransport.GRPC);
	}

	public ProbeLoadoutAttemptHistoryResponse getItemAttemptsViaRest(String loadoutId, String loadoutItemId) {
		var forgeResponse = restLoadoutClient.getItemAttempts(loadoutId, loadoutItemId);
		return mapper.toProbeLoadoutAttemptHistoryResponse(forgeResponse);
	}

	public ProbeLoadoutAttemptHistoryResponse getItemAttemptsViaGrpc(String loadoutId, String loadoutItemId) {
		var grpcResponse = grpcLoadoutClient.getLoadoutItemAttemptsGrpc(loadoutId, loadoutItemId);
		return mapper.toProbeLoadoutAttemptHistoryResponse(grpcResponse);
	}
}
