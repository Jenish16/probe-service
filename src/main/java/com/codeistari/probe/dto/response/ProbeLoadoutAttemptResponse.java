package com.codeistari.probe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProbeLoadoutAttemptResponse(
		int attemptNumber,
		String forgeJobId,
		String requesterReference,
		String status,
		String approvalStatus,
		String rejectionReason,
		String failureReason,
		Instant createdAt) {}
