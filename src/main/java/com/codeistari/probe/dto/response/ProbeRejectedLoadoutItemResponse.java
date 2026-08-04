package com.codeistari.probe.dto.response;

public record ProbeRejectedLoadoutItemResponse(
		String artifactName, String artifactType, String material, Integer powerLevel, String reason) {}
