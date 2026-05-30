package com.codeistari.probe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forge")
public record ForgeClientProperties(Rest rest, Grpc grpc) {

	public record Rest(String baseUrl, int timeoutMs) {}

	public record Grpc(String host, int port, long deadlineMs) {}
}
