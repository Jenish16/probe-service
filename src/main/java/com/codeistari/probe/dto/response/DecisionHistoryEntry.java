package com.codeistari.probe.dto.response;

import com.codeistari.probe.domain.ApprovalDecisionType;
import java.time.Instant;

public record DecisionHistoryEntry(
		String forgeJobId,
		String operatorId,
		ApprovalDecisionType decision,
		String reason,
		Instant decidedAt) {}
