package com.codeistari.probe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * artifactType/material are kept as raw {@code String} (not the ArtifactType/ForgeMaterial
 * enums) since forge-service is authoritative and a future new value must not break
 * deserialization here (data-model.md). {@code @JsonInclude(NON_NULL)} mirrors forge-service's
 * own wire format exactly, including approval fields when a high-power attempt is gated.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProbeLoadoutItemResponse(
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
