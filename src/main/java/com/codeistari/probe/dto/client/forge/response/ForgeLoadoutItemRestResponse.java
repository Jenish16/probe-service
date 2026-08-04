package com.codeistari.probe.dto.client.forge.response;

import java.time.Instant;

public record ForgeLoadoutItemRestResponse(
		String loadoutItemId,
		String artifactName,
		String artifactType,
		String material,
		int powerLevel,
		String status,
		String approvalStatus,
		Instant approvalExpiresAt,
		String rejectionReason,
		String failureReason) {}
