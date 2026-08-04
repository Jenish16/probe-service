package com.codeistari.probe.client.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RestForgeJobClientTest {

	private MockRestServiceServer server;
	private RestForgeJobClient client;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://localhost:8081");
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		client = new RestForgeJobClient(restClientBuilder.build(), objectMapper);
	}

	@Test
	void mapsHttp201CreateResponse() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
								"""
								{
								  "forgeJobId": "fj_123",
								  "artifactName": "ember-ring",
								  "artifactType": "RING",
								  "material": "MITHRIL",
								  "requestedBy": "ranger",
								  "powerLevel": 7,
								  "status": "QUEUED",
								  "createdAt": "2026-05-27T10:00:00Z"
								}
								""",
								MediaType.APPLICATION_JSON));

		ForgeJobRestResponse response =
				client.createForgeJob(
						new ForgeCreateJobRestRequest(
								"ember-ring", "RING", "MITHRIL", "ranger", 7, "req-ref-1", null));

		assertThat(response.forgeJobId()).isEqualTo("fj_123");
		assertThat(response.status()).isEqualTo("QUEUED");
		server.verify();
	}

	@Test
	void mapsHttp400ErrorMessage() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withBadRequest()
								.body("{\"message\":\"artifactName: must not be blank\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(
						() ->
										client.createForgeJob(
										new ForgeCreateJobRestRequest(
												"", "RING", "MITHRIL", "ranger", 7, "req-ref-1", null)))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("artifactName: must not be blank")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);

		server.verify();
	}

	@Test
	void mapsHttp404ErrorMessage() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs/fj_missing"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withStatus(HttpStatus.NOT_FOUND)
								.body("{\"message\":\"Forge job not found: fj_missing\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getForgeJob("fj_missing"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("Forge job not found: fj_missing")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);

		server.verify();
	}

	@Test
	void mapsHttp500Response() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withRawStatus(500));

		assertThatThrownBy(
						() ->
								client.createForgeJob(
										new ForgeCreateJobRestRequest(
												"ember-ring", "RING", "MITHRIL", "ranger", 7, "req-ref-1", null)))
				.isInstanceOf(ForgeRemoteCallException.class)
				.hasMessageContaining("forge-service returned HTTP 500")
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_GATEWAY);

		server.verify();
	}
}
