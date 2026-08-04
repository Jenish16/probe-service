package com.codeistari.probe.controller;

import com.codeistari.probe.dto.request.CreateProbeLoadoutRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutRequest;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptHistoryResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.codeistari.probe.service.ProbeLoadoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/probe-requests/loadouts")
public class ProbeLoadoutController {

	private final ProbeLoadoutService probeLoadoutService;

	public ProbeLoadoutController(ProbeLoadoutService probeLoadoutService) {
		this.probeLoadoutService = probeLoadoutService;
	}

	@PostMapping("/rest")
	@ResponseStatus(HttpStatus.CREATED)
	public ProbeLoadoutResponse createViaRest(@Valid @RequestBody CreateProbeLoadoutRequest request) {
		return probeLoadoutService.createLoadoutViaRest(request);
	}

	@PostMapping("/grpc")
	@ResponseStatus(HttpStatus.CREATED)
	public ProbeLoadoutResponse createViaGrpc(@Valid @RequestBody CreateProbeLoadoutRequest request) {
		return probeLoadoutService.createLoadoutViaGrpc(request);
	}

	@GetMapping("/{loadoutId}/rest")
	public ProbeLoadoutResponse getViaRest(@PathVariable String loadoutId) {
		return probeLoadoutService.getLoadoutViaRest(loadoutId);
	}

	@GetMapping("/{loadoutId}/grpc")
	public ProbeLoadoutResponse getViaGrpc(@PathVariable String loadoutId) {
		return probeLoadoutService.getLoadoutViaGrpc(loadoutId);
	}

	@PostMapping("/{loadoutId}/cancel/rest")
	public ProbeLoadoutResponse cancelViaRest(@PathVariable String loadoutId) {
		return probeLoadoutService.cancelLoadoutViaRest(loadoutId);
	}

	@PostMapping("/{loadoutId}/cancel/grpc")
	public ProbeLoadoutResponse cancelViaGrpc(@PathVariable String loadoutId) {
		return probeLoadoutService.cancelLoadoutViaGrpc(loadoutId);
	}

	@PostMapping("/{loadoutId}/retry/rest")
	public ProbeLoadoutResponse retryViaRest(
			@PathVariable String loadoutId, @Valid @RequestBody RetryProbeLoadoutRequest request) {
		return probeLoadoutService.retryLoadoutViaRest(loadoutId, request);
	}

	@PostMapping("/{loadoutId}/retry/grpc")
	public ProbeLoadoutResponse retryViaGrpc(
			@PathVariable String loadoutId, @Valid @RequestBody RetryProbeLoadoutRequest request) {
		return probeLoadoutService.retryLoadoutViaGrpc(loadoutId, request);
	}

	@GetMapping("/{loadoutId}/items/{loadoutItemId}/attempts/rest")
	public ProbeLoadoutAttemptHistoryResponse getItemAttemptsViaRest(
			@PathVariable String loadoutId, @PathVariable String loadoutItemId) {
		return probeLoadoutService.getItemAttemptsViaRest(loadoutId, loadoutItemId);
	}

	@GetMapping("/{loadoutId}/items/{loadoutItemId}/attempts/grpc")
	public ProbeLoadoutAttemptHistoryResponse getItemAttemptsViaGrpc(
			@PathVariable String loadoutId, @PathVariable String loadoutItemId) {
		return probeLoadoutService.getItemAttemptsViaGrpc(loadoutId, loadoutItemId);
	}
}
