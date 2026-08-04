package com.codeistari.probe.dto.client.forge.response;

import java.util.List;

/**
 * Mirrors forge-service's {@code ForgeJobHistoryResponse} shape exactly, including {@code
 * attempts}, even though probe's public {@code DecisionHistoryResponse} only surfaces {@code
 * decisions} today — kept for forward compatibility and to avoid relying on Jackson's
 * unknown-property tolerance.
 */
public record ForgeDecisionHistoryRestResponse(
		String requesterReference,
		List<ForgeJobRestResponse> attempts,
		List<ForgeDecisionEntryRestResponse> decisions) {}
