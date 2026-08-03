package com.codeistari.probe.dto.client.forge.response;

import java.util.List;

public record ForgeLoadoutAttemptsRestResponse(String loadoutItemId, List<ForgeLoadoutAttemptRestResponse> attempts) {}
