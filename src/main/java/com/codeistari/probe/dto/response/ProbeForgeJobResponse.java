package com.codeistari.probe.dto.response;

import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import java.time.Instant;

public record ProbeForgeJobResponse(
		String forgeJobId,
		String artifactName,
		ArtifactType artifactType,
		ForgeMaterial material,
		String requestedBy,
		int powerLevel,
		String status,
		Instant createdAt,
		ForgeTransport transport) {}
