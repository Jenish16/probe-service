package com.codeistari.probe.client.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeLoadoutItemRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutItemRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RestLoadoutClientTest {

	private MockRestServiceServer server;
	private RestLoadoutClient client;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://localhost:8081");
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		client = new RestLoadoutClient(restClientBuilder.build(), objectMapper);
	}

	@Test
	void mapsHttp201CreateResponse() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.CREATED)
								.body(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "ACCEPTED",
										  "items": [],
										  "rejectedItems": []
										}
										""")
								.contentType(MediaType.APPLICATION_JSON));

		ForgeCreateLoadoutRestRequest request = new ForgeCreateLoadoutRestRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ForgeLoadoutItemRestRequest("ember-ring", "RING", "MITHRIL", 7)));
		ForgeLoadoutRestResponse response = client.createLoadout(request);

		assertThat(response.loadoutId()).isEqualTo("lo_123");
		assertThat(response.status()).isEqualTo("ACCEPTED");
		server.verify();
	}

	@Test
	void mapsHttp200IdempotentReplayResponse() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "ACCEPTED",
										  "items": [],
										  "rejectedItems": []
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeCreateLoadoutRestRequest request = new ForgeCreateLoadoutRestRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ForgeLoadoutItemRestRequest("ember-ring", "RING", "MITHRIL", 7)));
		ForgeLoadoutRestResponse response = client.createLoadout(request);

		assertThat(response.loadoutId()).isEqualTo("lo_123");
		server.verify();
	}

	@Test
	void mapsHttp400ErrorMessage() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.BAD_REQUEST)
								.body("{\"message\":\"items must contain between 2 and 10 entries, got 1\"}")
								.contentType(MediaType.APPLICATION_JSON));

		ForgeCreateLoadoutRestRequest request = new ForgeCreateLoadoutRestRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ForgeLoadoutItemRestRequest("ember-ring", "RING", "MITHRIL", 7)));

		assertThatThrownBy(() -> client.createLoadout(request))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("items must contain between 2 and 10 entries")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);

		server.verify();
	}

	@Test
	void mapsHttp409ConflictMessage() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.CONFLICT)
								.body("{\"message\":\"requestReference already used with different content\"}")
								.contentType(MediaType.APPLICATION_JSON));

		ForgeCreateLoadoutRestRequest request = new ForgeCreateLoadoutRestRequest(
				"req-1",
				"Dragon Raid Kit",
				"questmaster",
				List.of(new ForgeLoadoutItemRestRequest("ember-ring", "RING", "MITHRIL", 7)));

		assertThatThrownBy(() -> client.createLoadout(request))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.CONFLICT);

		server.verify();
	}

	@Test
	void mapsGetLoadout200() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "IN_PROGRESS",
										  "items": []
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeLoadoutRestResponse response = client.getLoadout("lo_123");

		assertThat(response.status()).isEqualTo("IN_PROGRESS");
		server.verify();
	}

	@Test
	void mapsGetLoadout404() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_missing"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withStatus(HttpStatus.NOT_FOUND)
								.body("{\"message\":\"Loadout not found: lo_missing\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getLoadout("lo_missing"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);

		server.verify();
	}

	@Test
	void mapsCancelLoadout200() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/cancel"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "CANCELLED",
										  "items": []
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeLoadoutRestResponse response = client.cancelLoadout("lo_123");

		assertThat(response.status()).isEqualTo("CANCELLED");
		server.verify();
	}

	@Test
	void mapsCancelLoadoutNoOpAsPlain200() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/cancel"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "READY",
										  "items": []
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeLoadoutRestResponse response = client.cancelLoadout("lo_123");

		assertThat(response.status()).isEqualTo("READY");
		server.verify();
	}

	@Test
	void mapsRetryLoadout200() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/retry"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutId": "lo_123",
										  "loadoutName": "Dragon Raid Kit",
										  "requestedBy": "questmaster",
										  "status": "IN_PROGRESS",
										  "items": []
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeRetryLoadoutRestRequest request =
				new ForgeRetryLoadoutRestRequest(List.of(new ForgeRetryLoadoutItemRestRequest("li_1", null, null, null, null)));
		ForgeLoadoutRestResponse response = client.retryLoadout("lo_123", request);

		assertThat(response.status()).isEqualTo("IN_PROGRESS");
		server.verify();
	}

	@Test
	void mapsRetryLoadout400ForIncompleteItems() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/retry"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.BAD_REQUEST)
								.body("{\"message\":\"retry request must include exactly the currently retry-eligible items\"}")
								.contentType(MediaType.APPLICATION_JSON));

		ForgeRetryLoadoutRestRequest request = new ForgeRetryLoadoutRestRequest(List.of());

		assertThatThrownBy(() -> client.retryLoadout("lo_123", request))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);

		server.verify();
	}

	@Test
	void mapsRetryLoadout404() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_missing/retry"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.NOT_FOUND)
								.body("{\"message\":\"Loadout not found: lo_missing\"}")
								.contentType(MediaType.APPLICATION_JSON));

		ForgeRetryLoadoutRestRequest request = new ForgeRetryLoadoutRestRequest(List.of());

		assertThatThrownBy(() -> client.retryLoadout("lo_missing", request))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);

		server.verify();
	}

	@Test
	void mapsGetItemAttemptsOrderedOldestToNewest() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/items/li_1/attempts"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withSuccess(
										"""
										{
										  "loadoutItemId": "li_1",
										  "attempts": [
										    {
										      "attemptNumber": 1,
										      "forgeJobId": "fj_1",
										      "status": "FAILED",
										      "failureReason": "forge cracked",
										      "createdAt": "2026-05-27T10:00:00Z"
										    },
										    {
										      "attemptNumber": 2,
										      "forgeJobId": "fj_2",
										      "status": "COMPLETED",
										      "createdAt": "2026-05-27T10:05:00Z"
										    }
										  ]
										}
										""",
										MediaType.APPLICATION_JSON));

		ForgeLoadoutAttemptsRestResponse response = client.getItemAttempts("lo_123", "li_1");

		assertThat(response.attempts()).hasSize(2);
		assertThat(response.attempts().get(0).attemptNumber()).isEqualTo(1);
		assertThat(response.attempts().get(0).status()).isEqualTo("FAILED");
		assertThat(response.attempts().get(1).attemptNumber()).isEqualTo(2);
		assertThat(response.attempts().get(1).status()).isEqualTo("COMPLETED");
		server.verify();
	}

	@Test
	void mapsGetItemAttempts404() {
		server.expect(requestTo("http://localhost:8081/api/v1/loadouts/lo_123/items/li_missing/attempts"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withStatus(HttpStatus.NOT_FOUND)
								.body("{\"message\":\"Loadout not found: lo_123/li_missing\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getItemAttempts("lo_123", "li_missing"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);

		server.verify();
	}
}
