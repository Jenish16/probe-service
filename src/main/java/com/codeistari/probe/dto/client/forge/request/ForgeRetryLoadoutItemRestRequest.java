package com.codeistari.probe.dto.client.forge.request;

public record ForgeRetryLoadoutItemRestRequest(
		String loadoutItemId, String artifactName, String artifactType, String material, Integer powerLevel) {}
