package com.codeistari.probe.dto.request;

import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProbeForgeJobRequest(
		@NotBlank String artifactName,
		@NotNull ArtifactType artifactType,
		@NotNull ForgeMaterial material,
		@NotBlank String requestedBy,
		@Min(1) @Max(10) int powerLevel,
		@NotBlank String requesterReference,
		String originalRequestReference) {}
