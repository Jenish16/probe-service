package com.codeistari.probe.dto.response;

import com.codeistari.probe.domain.ForgeTransport;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProbeLoadoutResponse(
		String loadoutId,
		String loadoutName,
		String requestedBy,
		String status,
		List<ProbeLoadoutItemResponse> items,
		List<ProbeRejectedLoadoutItemResponse> rejectedItems,
		ForgeTransport transport) {}
