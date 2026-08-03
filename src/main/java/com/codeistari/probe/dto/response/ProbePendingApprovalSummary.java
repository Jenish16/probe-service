package com.codeistari.probe.dto.response;

import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import java.time.Instant;

public record ProbePendingApprovalSummary(
		String forgeJobId,
		String requesterReference,
		String requestedBy,
		ArtifactType artifactType,
		ForgeMaterial material,
		int powerLevel,
		Instant submittedAt,
		Instant approvalExpiresAt) {}
