package com.codeistari.probe.dto.client.forge.response;

import java.time.Instant;

public record ForgeLoadoutAttemptRestResponse(
		int attemptNumber,
		String forgeJobId,
		String requesterReference,
		String status,
		String approvalStatus,
		String rejectionReason,
		String failureReason,
		Instant createdAt) {}
