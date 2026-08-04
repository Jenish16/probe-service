package com.codeistari.probe.dto.client.forge.response;

public record ForgeRejectedLoadoutItemRestResponse(
		String artifactName, String artifactType, String material, Integer powerLevel, String reason) {}
