package com.codeistari.probe.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Optional fields left {@code null} keep the item's prior value. Raw, unvalidated
 * artifactType/material/powerLevel (research.md D7, revised by Gate 2 IC-001) — an invalid
 * correction value must reach forge-service's own error handling, not fail at probe-service.
 */
public record RetryProbeLoadoutItemRequest(
		@NotBlank String loadoutItemId, String artifactName, String artifactType, String material, Integer powerLevel) {}
