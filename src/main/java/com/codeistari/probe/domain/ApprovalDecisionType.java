package com.codeistari.probe.domain;

/**
 * Type of decision recorded against a forge job attempt in its approval history, mirroring
 * forge-service's {@code ApprovalDecisionType}.
 */
public enum ApprovalDecisionType {
	APPROVE,
	REJECT,
	CANCEL
}
