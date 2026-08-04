package com.codeistari.probe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * No {@code @Size} bound on {@code items} — the 2-10 item-count bound (FR-001) is
 * forge-service's alone (research.md D7, revised by Gate 2 IC-001).
 */
public record CreateProbeLoadoutRequest(
		@NotBlank String requestReference,
		@NotBlank String loadoutName,
		@NotBlank String requestedBy,
		@NotNull @NotEmpty @Valid List<ProbeLoadoutItemRequest> items) {}
