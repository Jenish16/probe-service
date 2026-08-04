package com.codeistari.probe.client;

import com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import java.util.List;

/**
 * Downstream operations for the five new operator/audit capabilities, kept separate from {@link
 * ForgeJobClient} rather than added to it (design.md Alternatives) to avoid deepening the
 * existing unused-abstraction pattern onto more methods.
 */
public interface ApprovalForgeClient {

	List<ForgeJobRestResponse> listPendingApprovals();

	ForgeJobRestResponse approveRequest(String forgeJobId, String operatorId);

	ForgeJobRestResponse rejectRequest(String forgeJobId, String operatorId, String reason);

	ForgeJobRestResponse cancelRequest(String forgeJobId, String requestedBy);

	ForgeDecisionHistoryRestResponse getDecisionHistory(String requesterReference);
}
