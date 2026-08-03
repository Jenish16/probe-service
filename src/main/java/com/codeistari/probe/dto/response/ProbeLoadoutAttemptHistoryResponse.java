package com.codeistari.probe.dto.response;

import java.util.List;

public record ProbeLoadoutAttemptHistoryResponse(String loadoutItemId, List<ProbeLoadoutAttemptResponse> attempts) {}
