package com.codeistari.probe.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record RetryProbeLoadoutRequest(@Valid List<RetryProbeLoadoutItemRequest> items) {}
