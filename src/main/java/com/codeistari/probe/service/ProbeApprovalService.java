package com.codeistari.probe.service;

import com.codeistari.probe.client.grpc.GrpcApprovalForgeClient;
import com.codeistari.probe.client.rest.RestApprovalForgeClient;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.response.DecisionHistoryResponse;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.dto.response.ProbePendingApprovalSummary;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the five operator/audit operations across both transports, mirroring {@link
 * ProbeRequestService}'s per-transport method-pair convention.
 */
@Service
public class ProbeApprovalService {

	private final RestApprovalForgeClient restApprovalForgeClient;
	private final GrpcApprovalForgeClient grpcApprovalForgeClient;
	private final ProbeRequestMapper mapper;

	public ProbeApprovalService(
			RestApprovalForgeClient restApprovalForgeClient,
			GrpcApprovalForgeClient grpcApprovalForgeClient,
			ProbeRequestMapper mapper) {
		this.restApprovalForgeClient = restApprovalForgeClient;
		this.grpcApprovalForgeClient = grpcApprovalForgeClient;
		this.mapper = mapper;
	}

	public List<ProbePendingApprovalSummary> listPendingApprovalsViaRest() {
		return mapper.toPendingApprovalSummaries(restApprovalForgeClient.listPendingApprovals());
	}

	public List<ProbePendingApprovalSummary> listPendingApprovalsViaGrpc() {
		return mapper.toPendingApprovalSummaries(grpcApprovalForgeClient.listPendingApprovals());
	}

	public ProbeForgeJobResponse approveViaRest(String forgeJobId, String operatorId) {
		return mapper.toProbeForgeJobResponse(
				restApprovalForgeClient.approveRequest(forgeJobId, operatorId), ForgeTransport.REST);
	}

	public ProbeForgeJobResponse approveViaGrpc(String forgeJobId, String operatorId) {
		return mapper.toProbeForgeJobResponse(
				grpcApprovalForgeClient.approveRequest(forgeJobId, operatorId), ForgeTransport.GRPC);
	}

	public ProbeForgeJobResponse rejectViaRest(String forgeJobId, String operatorId, String reason) {
		return mapper.toProbeForgeJobResponse(
				restApprovalForgeClient.rejectRequest(forgeJobId, operatorId, reason), ForgeTransport.REST);
	}

	public ProbeForgeJobResponse rejectViaGrpc(String forgeJobId, String operatorId, String reason) {
		return mapper.toProbeForgeJobResponse(
				grpcApprovalForgeClient.rejectRequest(forgeJobId, operatorId, reason), ForgeTransport.GRPC);
	}

	public ProbeForgeJobResponse cancelViaRest(String forgeJobId, String requestedBy) {
		return mapper.toProbeForgeJobResponse(
				restApprovalForgeClient.cancelRequest(forgeJobId, requestedBy), ForgeTransport.REST);
	}

	public ProbeForgeJobResponse cancelViaGrpc(String forgeJobId, String requestedBy) {
		return mapper.toProbeForgeJobResponse(
				grpcApprovalForgeClient.cancelRequest(forgeJobId, requestedBy), ForgeTransport.GRPC);
	}

	public DecisionHistoryResponse getDecisionHistoryViaRest(String requesterReference) {
		return mapper.toDecisionHistoryResponse(restApprovalForgeClient.getDecisionHistory(requesterReference));
	}

	public DecisionHistoryResponse getDecisionHistoryViaGrpc(String requesterReference) {
		return mapper.toDecisionHistoryResponse(grpcApprovalForgeClient.getDecisionHistory(requesterReference));
	}
}
