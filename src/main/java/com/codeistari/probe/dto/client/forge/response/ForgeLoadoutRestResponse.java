package com.codeistari.probe.dto.client.forge.response;

import java.util.List;

public record ForgeLoadoutRestResponse(
		String loadoutId,
		String loadoutName,
		String requestedBy,
		String status,
		List<ForgeLoadoutItemRestResponse> items,
		List<ForgeRejectedLoadoutItemRestResponse> rejectedItems) {}
