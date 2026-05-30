package com.codeistari.probe.dto.client.forge.request;

public record ForgeCreateJobRestRequest(
		String artifactName,
		String artifactType,
		String material,
		String requestedBy,
		int powerLevel) {}
