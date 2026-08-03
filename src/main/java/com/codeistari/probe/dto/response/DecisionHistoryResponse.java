package com.codeistari.probe.dto.response;

import java.util.List;

public record DecisionHistoryResponse(
		String requesterReference, List<DecisionHistoryEntry> decisions) {}
