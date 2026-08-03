package com.codeistari.probe.controller;

import com.codeistari.probe.dto.request.ApprovalDecisionRequest;
import com.codeistari.probe.dto.request.CancelRequestRequest;
import com.codeistari.probe.dto.request.RejectDecisionRequest;
import com.codeistari.probe.dto.response.DecisionHistoryResponse;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.dto.response.ProbePendingApprovalSummary;
import com.codeistari.probe.service.ProbeApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator/audit endpoints for the High-Power Artifact Approval capability: list pending
 * approvals, approve, reject, cancel, and retrieve linked decision history — each with a REST and
 * a gRPC-downstream variant, mirroring {@link ProbeRequestController}'s existing dual-transport
 * convention.
 */
@RestController
@RequestMapping("/api/v1/probe-requests/forge-jobs")
public class ProbeApprovalController {

	private final ProbeApprovalService probeApprovalService;

	public ProbeApprovalController(ProbeApprovalService probeApprovalService) {
		this.probeApprovalService = probeApprovalService;
	}

	@GetMapping("/pending/rest")
	public List<ProbePendingApprovalSummary> listPendingViaRest() {
		return probeApprovalService.listPendingApprovalsViaRest();
	}

	@GetMapping("/pending/grpc")
	public List<ProbePendingApprovalSummary> listPendingViaGrpc() {
		return probeApprovalService.listPendingApprovalsViaGrpc();
	}

	@PostMapping("/{forgeJobId}/approve/rest")
	public ProbeForgeJobResponse approveViaRest(
			@PathVariable String forgeJobId, @Valid @RequestBody ApprovalDecisionRequest request) {
		return probeApprovalService.approveViaRest(forgeJobId, request.operatorId());
	}

	@PostMapping("/{forgeJobId}/approve/grpc")
	public ProbeForgeJobResponse approveViaGrpc(
			@PathVariable String forgeJobId, @Valid @RequestBody ApprovalDecisionRequest request) {
		return probeApprovalService.approveViaGrpc(forgeJobId, request.operatorId());
	}

	@PostMapping("/{forgeJobId}/reject/rest")
	public ProbeForgeJobResponse rejectViaRest(
			@PathVariable String forgeJobId, @Valid @RequestBody RejectDecisionRequest request) {
		return probeApprovalService.rejectViaRest(forgeJobId, request.operatorId(), request.reason());
	}

	@PostMapping("/{forgeJobId}/reject/grpc")
	public ProbeForgeJobResponse rejectViaGrpc(
			@PathVariable String forgeJobId, @Valid @RequestBody RejectDecisionRequest request) {
		return probeApprovalService.rejectViaGrpc(forgeJobId, request.operatorId(), request.reason());
	}

	@PostMapping("/{forgeJobId}/cancel/rest")
	public ProbeForgeJobResponse cancelViaRest(
			@PathVariable String forgeJobId, @Valid @RequestBody CancelRequestRequest request) {
		return probeApprovalService.cancelViaRest(forgeJobId, request.requestedBy());
	}

	@PostMapping("/{forgeJobId}/cancel/grpc")
	public ProbeForgeJobResponse cancelViaGrpc(
			@PathVariable String forgeJobId, @Valid @RequestBody CancelRequestRequest request) {
		return probeApprovalService.cancelViaGrpc(forgeJobId, request.requestedBy());
	}

	@GetMapping("/history/{requesterReference}/rest")
	public DecisionHistoryResponse historyViaRest(@PathVariable String requesterReference) {
		return probeApprovalService.getDecisionHistoryViaRest(requesterReference);
	}

	@GetMapping("/history/{requesterReference}/grpc")
	public DecisionHistoryResponse historyViaGrpc(@PathVariable String requesterReference) {
		return probeApprovalService.getDecisionHistoryViaGrpc(requesterReference);
	}
}
