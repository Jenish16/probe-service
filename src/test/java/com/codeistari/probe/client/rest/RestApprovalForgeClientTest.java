package com.codeistari.probe.client.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

class RestApprovalForgeClientTest {

	private MockRestServiceServer server;
	private RestApprovalForgeClient client;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://localhost:8081");
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		client = new RestApprovalForgeClient(restClientBuilder.build(), objectMapper);
	}

	@Test
	void listsPendingApprovals() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs/pending-approval"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withSuccess(
								"""
								[
								  {
								    "forgeJobId": "fj_123",
								    "artifactName": "ember-ring",
								    "artifactType": "RING",
								    "material": "MITHRIL",
								    "requestedBy": "ranger",
								    "powerLevel": 9,
								    "status": null,
								    "createdAt": "2026-05-27T10:00:00Z",
								    "requesterReference": "req-ref-1",
								    "approvalStatus": "PENDING_APPROVAL",
								    "approvalExpiresAt": "2026-05-27T10:30:00Z"
								  }
								]
								""",
								MediaType.APPLICATION_JSON));

		var pending = client.listPendingApprovals();

		assertThat(pending).hasSize(1);
		assertThat(pending.get(0).forgeJobId()).isEqualTo("fj_123");
		assertThat(pending.get(0).approvalStatus()).isEqualTo("PENDING_APPROVAL");
		server.verify();
	}

	@Test
	void approvesRequest() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs/fj_123/approve"))
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
								  "powerLevel": 9,
								  "status": "QUEUED",
								  "createdAt": "2026-05-27T10:00:00Z",
								  "requesterReference": "req-ref-1",
								  "approvalStatus": "APPROVED"
								}
								""",
								MediaType.APPLICATION_JSON));

		ForgeJobRestResponse response = client.approveRequest("fj_123", "op-1");

		assertThat(response.approvalStatus()).isEqualTo("APPROVED");
		server.verify();
	}

	@Test
	void rejectWithoutReasonMapsToBadRequest() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs/fj_123/reject"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.BAD_REQUEST)
								.body("{\"message\":\"reason: must not be blank\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.rejectRequest("fj_123", "op-1", null))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);

		server.verify();
	}

	@Test
	void cancelOnUnknownJobMapsToNotFound() {
		server.expect(requestTo("http://localhost:8081/api/v1/forge-jobs/fj_missing/cancel"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withStatus(HttpStatus.NOT_FOUND)
								.body("{\"message\":\"Forge job not found: fj_missing\"}")
								.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.cancelRequest("fj_missing", "ranger"))
				.isInstanceOf(ForgeRemoteCallException.class)
				.extracting(ex -> ((ForgeRemoteCallException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);

		server.verify();
	}

	@Test
	void getsDecisionHistory() {
		server.expect(
						requestTo(
								"http://localhost:8081/api/v1/forge-jobs/history?requesterReference=req-ref-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(
						withSuccess(
								"""
								{
								  "requesterReference": "req-ref-1",
								  "attempts": [],
								  "decisions": [
								    {
								      "decisionId": "decision-1",
								      "forgeJobId": "fj_123",
								      "decisionType": "APPROVE",
								      "operatorId": "op-1",
								      "reason": null,
								      "decidedAt": "2026-05-27T10:05:00Z"
								    }
								  ]
								}
								""",
								MediaType.APPLICATION_JSON));

		var history = client.getDecisionHistory("req-ref-1");

		assertThat(history.requesterReference()).isEqualTo("req-ref-1");
		assertThat(history.decisions()).hasSize(1);
		assertThat(history.decisions().get(0).decisionType()).isEqualTo("APPROVE");
		server.verify();
	}
}
