package com.codeistari.probe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.request.CreateProbeForgeJobRequest;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.exception.GlobalExceptionHandler;
import com.codeistari.probe.service.ProbeRequestService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProbeRequestControllerTest {

	@Mock private ProbeRequestService probeRequestService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc =
				MockMvcBuilders.standaloneSetup(new ProbeRequestController(probeRequestService))
						.setControllerAdvice(new GlobalExceptionHandler())
						.build();
	}

	@Test
	void createViaRestReturnsCreatedResponse() throws Exception {
		when(probeRequestService.createForgeJobViaRest(any(CreateProbeForgeJobRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.REST));

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "artifactName": "ember-ring",
										  "artifactType": "RING",
										  "material": "MITHRIL",
										  "requestedBy": "ranger",
										  "powerLevel": 7
										}
										"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.forgeJobId").value("fj_123"))
				.andExpect(jsonPath("$.transport").value("REST"));

		verify(probeRequestService).createForgeJobViaRest(any(CreateProbeForgeJobRequest.class));
	}

	@Test
	void createViaGrpcReturnsCreatedResponse() throws Exception {
		when(probeRequestService.createForgeJobViaGrpc(any(CreateProbeForgeJobRequest.class)))
				.thenReturn(sampleResponse(ForgeTransport.GRPC));

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/grpc")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "artifactName": "ember-ring",
										  "artifactType": "RING",
										  "material": "MITHRIL",
										  "requestedBy": "ranger",
										  "powerLevel": 7
										}
										"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.forgeJobId").value("fj_123"))
				.andExpect(jsonPath("$.transport").value("GRPC"));

		verify(probeRequestService).createForgeJobViaGrpc(any(CreateProbeForgeJobRequest.class));
	}

	@Test
	void rejectsBlankArtifactName() throws Exception {
		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "artifactName": "   ",
										  "artifactType": "RING",
										  "material": "MITHRIL",
										  "requestedBy": "ranger",
										  "powerLevel": 7
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void rejectsInvalidPowerLevel() throws Exception {
		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "artifactName": "ember-ring",
										  "artifactType": "RING",
										  "material": "MITHRIL",
										  "requestedBy": "ranger",
										  "powerLevel": 11
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	private ProbeForgeJobResponse sampleResponse(ForgeTransport transport) {
		return new ProbeForgeJobResponse(
				"fj_123",
				"ember-ring",
				ArtifactType.RING,
				ForgeMaterial.MITHRIL,
				"ranger",
				7,
				"QUEUED",
				Instant.parse("2026-05-27T10:00:00Z"),
				transport);
	}
}
