package com.codeistari.probe.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Approve-request body. Kept as a dedicated type (rather than a shared approve/reject body with a
 * validation group) to mirror forge-service's own {@code ApproveForgeJobRequest}/{@code
 * RejectForgeJobRequest} split exactly (design.md Interfaces, confirmed against {@code
 * forge-service#5} at Gate 2).
 */
public record ApprovalDecisionRequest(@NotBlank String operatorId) {}
