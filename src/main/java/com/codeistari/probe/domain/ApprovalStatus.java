package com.codeistari.probe.domain;

/**
 * Approval lifecycle state for a forge job, mirroring forge-service's {@code ApprovalStatus}
 * (the owning service, per PD-001). {@code NOT_REQUIRED} is forge-service's actual wire value for
 * power level 1-7 jobs; {@link com.codeistari.probe.dto.response.ProbeForgeJobResponse} surfaces
 * that case as a {@code null} {@code approvalStatus} instead, per the public API contract.
 */
public enum ApprovalStatus {
	NOT_REQUIRED,
	PENDING_APPROVAL,
	APPROVED,
	REJECTED,
	EXPIRED,
	CANCELLED
}
