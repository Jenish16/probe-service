package com.codeistari.probe.dto.client.forge.response;

import java.time.Instant;

public record ForgeDecisionEntryRestResponse(
		String decisionId,
		String forgeJobId,
		String decisionType,
		String operatorId,
		String reason,
		Instant decidedAt) {}
