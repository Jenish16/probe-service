package com.codeistari.probe.dto.client.forge.request;

import java.util.List;

public record ForgeCreateLoadoutRestRequest(
		String requestReference, String loadoutName, String requestedBy, List<ForgeLoadoutItemRestRequest> items) {}
