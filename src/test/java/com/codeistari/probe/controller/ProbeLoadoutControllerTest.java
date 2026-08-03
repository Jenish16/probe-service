package com.codeistari.probe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.request.CreateProbeLoadoutRequest;
import com.codeistari.probe.dto.request.RetryProbeLoadoutRequest;
import com.codeistari.probe.dto.response.ProbeLoadoutAttemptHistoryResponse;
import com.codeistari.probe.dto.response.ProbeLoadoutResponse;
import com.codeistari.probe.dto.response.ProbeRejectedLoadoutItemResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.exception.GlobalExceptionHandler;
import com.codeistari.probe.service.ProbeLoadoutService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProbeLoadoutControllerTest {

	@Mock private ProbeLoadoutService probeLoadoutService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ProbeLoadoutController(probeLoadoutService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createViaRestReturnsCreatedResponse() throws Exception {
		when(probeLoadoutService.createLoadoutViaRest(any(CreateProbeLoadoutRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.REST, List.of()));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.loadoutId").value("lo_123"))
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.transport").value("REST"));

		verify(probeLoadoutService).createLoadoutViaRest(any(CreateProbeLoadoutRequest.class));
	}

	@Test
	void createViaGrpcReturnsCreatedResponse() throws Exception {
		when(probeLoadoutService.createLoadoutViaGrpc(any(CreateProbeLoadoutRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.GRPC, List.of()));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/grpc")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transport").value("GRPC"));

		verify(probeLoadoutService).createLoadoutViaGrpc(any(CreateProbeLoadoutRequest.class));
	}

	@Test
	void mixedValidInvalidItemRequestIsForwardedNotRejectedAtProbe() throws Exception {
		// research.md D7 revised, Gate 2 IC-001; spec.md User Story 1 Acceptance Scenario 2:
		// an out-of-range powerLevel must reach forge-service and come back as a rejected item,
		// not fail Bean Validation at probe-service with a whole-request 400.
		ProbeRejectedLoadoutItemResponse rejected =
				new ProbeRejectedLoadoutItemResponse("cursed-blade", "BLADE", "OBSIDIAN", 999, "powerLevel must be between 1 and 10");
		when(probeLoadoutService.createLoadoutViaRest(any(CreateProbeLoadoutRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.REST, List.of(rejected)));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"""
								{
								  "requestReference": "req-1",
								  "loadoutName": "Dragon Raid Kit",
								  "requestedBy": "questmaster",
								  "items": [
								    { "artifactName": "ember-ring", "artifactType": "RING", "material": "MITHRIL", "powerLevel": 7 },
								    { "artifactName": "cursed-blade", "artifactType": "BLADE", "material": "OBSIDIAN", "powerLevel": 999 }
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rejectedItems[0].reason").value("powerLevel must be between 1 and 10"));

		verify(probeLoadoutService).createLoadoutViaRest(any(CreateProbeLoadoutRequest.class));
	}

	@Test
	void rejectsBlankLoadoutName() throws Exception {
		mockMvc.perform(post("/api/v1/probe-requests/loadouts/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"""
								{
								  "requestReference": "req-1",
								  "loadoutName": "   ",
								  "requestedBy": "questmaster",
								  "items": [
								    { "artifactName": "ember-ring", "artifactType": "RING", "material": "MITHRIL", "powerLevel": 7 }
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void rejectsEmptyItemsList() throws Exception {
		mockMvc.perform(post("/api/v1/probe-requests/loadouts/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"""
								{
								  "requestReference": "req-1",
								  "loadoutName": "Dragon Raid Kit",
								  "requestedBy": "questmaster",
								  "items": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void conflictOnDuplicateRequestReferenceIsForwarded() throws Exception {
		when(probeLoadoutService.createLoadoutViaRest(any(CreateProbeLoadoutRequest.class)))
				.thenThrow(new ForgeRemoteCallException(
						HttpStatus.CONFLICT, "requestReference already used with different content"));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("requestReference already used with different content"));
	}

	@Test
	void getViaRestReturnsLoadout() throws Exception {
		when(probeLoadoutService.getLoadoutViaRest("lo_123")).thenReturn(sampleResponse(ForgeTransport.REST, List.of()));

		mockMvc.perform(get("/api/v1/probe-requests/loadouts/lo_123/rest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loadoutId").value("lo_123"));
	}

	@Test
	void getViaGrpcReturnsLoadout() throws Exception {
		when(probeLoadoutService.getLoadoutViaGrpc("lo_123")).thenReturn(sampleResponse(ForgeTransport.GRPC, List.of()));

		mockMvc.perform(get("/api/v1/probe-requests/loadouts/lo_123/grpc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transport").value("GRPC"));
	}

	@Test
	void getReturnsNotFoundForUnknownLoadout() throws Exception {
		when(probeLoadoutService.getLoadoutViaRest("lo_missing"))
				.thenThrow(new ForgeRemoteCallException(HttpStatus.NOT_FOUND, "Loadout not found: lo_missing"));

		mockMvc.perform(get("/api/v1/probe-requests/loadouts/lo_missing/rest")).andExpect(status().isNotFound());
	}

	@Test
	void cancelViaRestReturnsUpdatedLoadout() throws Exception {
		when(probeLoadoutService.cancelLoadoutViaRest("lo_123")).thenReturn(sampleResponse(ForgeTransport.REST, List.of()));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/lo_123/cancel/rest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loadoutId").value("lo_123"));
	}

	@Test
	void cancelIsSafeNoOpWhenNothingOutstanding() throws Exception {
		when(probeLoadoutService.cancelLoadoutViaRest("lo_123")).thenReturn(sampleResponse(ForgeTransport.REST, List.of()));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/lo_123/cancel/rest")).andExpect(status().isOk());

		verify(probeLoadoutService).cancelLoadoutViaRest("lo_123");
	}

	@Test
	void retryViaRestReturnsUpdatedLoadout() throws Exception {
		when(probeLoadoutService.retryLoadoutViaRest(anyString(), any(RetryProbeLoadoutRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.REST, List.of()));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/lo_123/retry/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"""
								{ "items": [ { "loadoutItemId": "li_1" } ] }
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void retryReturnsBadRequestForIncompleteEligibleItems() throws Exception {
		when(probeLoadoutService.retryLoadoutViaRest(anyString(), any(RetryProbeLoadoutRequest.class)))
				.thenThrow(new ForgeRemoteCallException(
						HttpStatus.BAD_REQUEST, "retry request must include exactly the currently retry-eligible items"));

		mockMvc.perform(post("/api/v1/probe-requests/loadouts/lo_123/retry/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"items\": [] }"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getItemAttemptsViaRestReturnsHistory() throws Exception {
		when(probeLoadoutService.getItemAttemptsViaRest("lo_123", "li_1"))
				.thenReturn(new ProbeLoadoutAttemptHistoryResponse("li_1", List.of()));

		mockMvc.perform(get("/api/v1/probe-requests/loadouts/lo_123/items/li_1/attempts/rest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.loadoutItemId").value("li_1"));
	}

	private String createRequestJson() {
		return """
				{
				  "requestReference": "req-1",
				  "loadoutName": "Dragon Raid Kit",
				  "requestedBy": "questmaster",
				  "items": [
				    { "artifactName": "ember-ring", "artifactType": "RING", "material": "MITHRIL", "powerLevel": 7 },
				    { "artifactName": "storm-shield", "artifactType": "SHIELD", "material": "DWARVEN_IRON", "powerLevel": 5 }
				  ]
				}
				""";
	}

	private ProbeLoadoutResponse sampleResponse(ForgeTransport transport, List<ProbeRejectedLoadoutItemResponse> rejectedItems) {
		return new ProbeLoadoutResponse("lo_123", "Dragon Raid Kit", "questmaster", "ACCEPTED", List.of(), rejectedItems, transport);
	}
}
