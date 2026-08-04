package com.codeistari.probe.controller;

import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.service.ProbeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Submit and retrieve forge jobs. Power level 8-10 submissions now return {@code
 * approvalStatus: PENDING_APPROVAL} and no forging begins until an operator decision is made; see
 * {@link ProbeApprovalController} for the operator/audit operations.
 */
@RestController
@RequestMapping("/api/v1/probe-requests/forge-jobs")
public class ProbeRequestController {

	private final ProbeRequestService probeRequestService;

	public ProbeRequestController(ProbeRequestService probeRequestService) {
		this.probeRequestService = probeRequestService;
	}

	@PostMapping("/rest")
	@ResponseStatus(HttpStatus.CREATED)
	public ProbeForgeJobResponse createViaRest(@Valid @RequestBody CreateProbeForgeJobRequest request) {
		return probeRequestService.createForgeJobViaRest(request);
	}

	@PostMapping("/grpc")
	@ResponseStatus(HttpStatus.CREATED)
	public ProbeForgeJobResponse createViaGrpc(@Valid @RequestBody CreateProbeForgeJobRequest request) {
		return probeRequestService.createForgeJobViaGrpc(request);
	}

	@GetMapping("/{forgeJobId}/rest")
	public ProbeForgeJobResponse getViaRest(@PathVariable String forgeJobId) {
		return probeRequestService.getForgeJobViaRest(forgeJobId);
	}

	@GetMapping("/{forgeJobId}/grpc")
	public ProbeForgeJobResponse getViaGrpc(@PathVariable String forgeJobId) {
		return probeRequestService.getForgeJobViaGrpc(forgeJobId);
	}
}
