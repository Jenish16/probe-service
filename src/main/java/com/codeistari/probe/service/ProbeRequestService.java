package com.codeistari.probe.service;

import com.codeistari.probe.client.grpc.GrpcForgeJobClient;
import com.codeistari.probe.client.rest.RestForgeJobClient;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import org.springframework.stereotype.Service;

@Service
public class ProbeRequestService {

	private final RestForgeJobClient restForgeJobClient;
	private final GrpcForgeJobClient grpcForgeJobClient;
	private final ProbeRequestMapper mapper;

	public ProbeRequestService(
			RestForgeJobClient restForgeJobClient,
			GrpcForgeJobClient grpcForgeJobClient,
			ProbeRequestMapper mapper) {
		this.restForgeJobClient = restForgeJobClient;
		this.grpcForgeJobClient = grpcForgeJobClient;
		this.mapper = mapper;
	}

	public ProbeForgeJobResponse createForgeJobViaRest(CreateProbeForgeJobRequest request) {
		var forgeRequest = mapper.toForgeCreateJobRestRequest(request);
		var forgeResponse = restForgeJobClient.createForgeJob(forgeRequest);
		return mapper.toProbeForgeJobResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeForgeJobResponse createForgeJobViaGrpc(CreateProbeForgeJobRequest request) {
		var grpcRequest = mapper.toCreateForgeJobGrpcRequest(request);
		var grpcResponse = grpcForgeJobClient.createForgeJob(grpcRequest);
		return mapper.toProbeForgeJobResponse(grpcResponse, ForgeTransport.GRPC);
	}

	public ProbeForgeJobResponse getForgeJobViaRest(String forgeJobId) {
		var forgeResponse = restForgeJobClient.getForgeJob(forgeJobId);
		return mapper.toProbeForgeJobResponse(forgeResponse, ForgeTransport.REST);
	}

	public ProbeForgeJobResponse getForgeJobViaGrpc(String forgeJobId) {
		var grpcResponse = grpcForgeJobClient.getForgeJobGrpc(forgeJobId);
		return mapper.toProbeForgeJobResponse(grpcResponse, ForgeTransport.GRPC);
	}
}
