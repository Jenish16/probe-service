package com.codeistari.probe.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Cancel-request body. {@code requestedBy} identifies the requester (cancellation is a requester
 * action, not an operator action, per Functional Spec clarification 5/6); the identity is not
 * re-verified, consistent with the deferred authz scope.
 */
public record CancelRequestRequest(@NotBlank String requestedBy) {}
