package com.codeistari.probe.dto.client.forge.response;

import java.time.Instant;

public record ForgeJobRestResponse(
		String forgeJobId,
		String artifactName,
		String artifactType,
		String material,
		String requestedBy,
		int powerLevel,
		String status,
		Instant createdAt) {}
