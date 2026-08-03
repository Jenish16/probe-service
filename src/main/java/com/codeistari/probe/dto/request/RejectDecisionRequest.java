package com.codeistari.probe.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Reject-request body; {@code reason} is required (unlike {@link ApprovalDecisionRequest}). */
public record RejectDecisionRequest(@NotBlank String operatorId, @NotBlank String reason) {}
