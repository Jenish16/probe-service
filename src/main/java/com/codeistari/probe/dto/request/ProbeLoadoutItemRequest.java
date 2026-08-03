package com.codeistari.probe.dto.request;

/**
 * Deliberately avoids Bean Validation annotations and uses raw {@code String} fields for
 * artifactType/material (research.md D7, revised by Gate 2 IC-001): per-item content is
 * validated entirely by forge-service so an invalid value is reported per-item as a rejected
 * item (FR-002/FR-003) instead of failing the whole request at probe-service.
 */
public record ProbeLoadoutItemRequest(String artifactName, String artifactType, String material, int powerLevel) {}
