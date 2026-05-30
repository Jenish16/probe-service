package com.codeistari.probe.client;

import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;

public interface ForgeJobClient {

	ForgeJobRestResponse createForgeJob(ForgeCreateJobRestRequest request);

	ForgeJobRestResponse getForgeJob(String forgeJobId);
}
