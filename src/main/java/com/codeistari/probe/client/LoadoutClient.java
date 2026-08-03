package com.codeistari.probe.client;

import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;

public interface LoadoutClient {

	ForgeLoadoutRestResponse createLoadout(ForgeCreateLoadoutRestRequest request);

	ForgeLoadoutRestResponse getLoadout(String loadoutId);

	ForgeLoadoutRestResponse cancelLoadout(String loadoutId);

	ForgeLoadoutRestResponse retryLoadout(String loadoutId, ForgeRetryLoadoutRestRequest request);

	ForgeLoadoutAttemptsRestResponse getItemAttempts(String loadoutId, String loadoutItemId);
}
