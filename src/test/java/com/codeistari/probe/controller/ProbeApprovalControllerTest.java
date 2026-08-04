package com.codeistari.probe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeistari.probe.domain.ApprovalDecisionType;
import com.codeistari.probe.domain.ApprovalStatus;
import com.codeistari.probe.domain.ArtifactType;
import com.codeistari.probe.domain.ForgeMaterial;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.response.DecisionHistoryEntry;
import com.codeistari.probe.dto.response.DecisionHistoryResponse;
import com.codeistari.probe.dto.response.ProbeForgeJobResponse;
import com.codeistari.probe.dto.response.ProbePendingApprovalSummary;
import com.codeistari.probe.exception.GlobalExceptionHandler;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.service.ProbeApprovalService;
import java.time.Instant;
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
class ProbeApprovalControllerTest {

	@Mock private ProbeApprovalService probeApprovalService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc =
				MockMvcBuilders.standaloneSetup(new ProbeApprovalController(probeApprovalService))
						.setControllerAdvice(new GlobalExceptionHandler())
						.build();
	}

	@Test
	void listPendingViaRestReturnsSummaries() throws Exception {
		when(probeApprovalService.listPendingApprovalsViaRest()).thenReturn(List.of(sampleSummary()));

		mockMvc.perform(get("/api/v1/probe-requests/forge-jobs/pending/rest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].forgeJobId").value("fj_123"));
	}

	@Test
	void approveViaRestReturnsUpdatedJob() throws Exception {
		when(probeApprovalService.approveViaRest("fj_123", "op-1")).thenReturn(sampleResponse());

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/approve/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"op-1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

		verify(probeApprovalService).approveViaRest("fj_123", "op-1");
	}

	@Test
	void approveRejectsBlankOperatorId() throws Exception {
		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/approve/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectViaRestRequiresReason() throws Exception {
		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/reject/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"op-1\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void rejectViaRestReturnsUpdatedJob() throws Exception {
		when(probeApprovalService.rejectViaRest(eq("fj_123"), eq("op-1"), eq("bad materials")))
				.thenReturn(sampleResponse());

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/reject/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"op-1\", \"reason\": \"bad materials\"}"))
				.andExpect(status().isOk());

		verify(probeApprovalService).rejectViaRest("fj_123", "op-1", "bad materials");
	}

	@Test
	void cancelRejectsBlankRequestedBy() throws Exception {
		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/cancel/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"requestedBy\": \"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void cancelViaRestReturnsUpdatedJob() throws Exception {
		when(probeApprovalService.cancelViaRest("fj_123", "ranger")).thenReturn(sampleResponse());

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/cancel/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"requestedBy\": \"ranger\"}"))
				.andExpect(status().isOk());

		verify(probeApprovalService).cancelViaRest("fj_123", "ranger");
	}

	@Test
	void historyViaRestReturnsDecisions() throws Exception {
		when(probeApprovalService.getDecisionHistoryViaRest("req-ref-1")).thenReturn(sampleHistory());

		mockMvc.perform(get("/api/v1/probe-requests/forge-jobs/history/req-ref-1/rest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requesterReference").value("req-ref-1"))
				.andExpect(jsonPath("$.decisions[0].decision").value("APPROVE"));
	}

	@Test
	void approveOnUnknownJobReturnsNotFound() throws Exception {
		when(probeApprovalService.approveViaRest("fj_missing", "op-1"))
				.thenThrow(new ForgeRemoteCallException(HttpStatus.NOT_FOUND, "Forge job not found: fj_missing"));

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_missing/approve/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"op-1\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void approveOnAlreadyDecidedJobReturnsConflict() throws Exception {
		when(probeApprovalService.approveViaRest(eq("fj_123"), any()))
				.thenThrow(new ForgeRemoteCallException(HttpStatus.CONFLICT, "competing decision"));

		mockMvc.perform(
						post("/api/v1/probe-requests/forge-jobs/fj_123/approve/rest")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"operatorId\": \"op-1\"}"))
				.andExpect(status().isConflict());
	}

	private ProbePendingApprovalSummary sampleSummary() {
		return new ProbePendingApprovalSummary(
				"fj_123",
				"req-ref-1",
				"ranger",
				ArtifactType.RING,
				ForgeMaterial.MITHRIL,
				9,
				Instant.parse("2026-05-27T10:00:00Z"),
				Instant.parse("2026-05-27T10:30:00Z"));
	}

	private ProbeForgeJobResponse sampleResponse() {
		return new ProbeForgeJobResponse(
				"fj_123",
				"ember-ring",
				ArtifactType.RING,
				ForgeMaterial.MITHRIL,
				"ranger",
				9,
				"QUEUED",
				Instant.parse("2026-05-27T10:00:00Z"),
				ForgeTransport.REST,
				"req-ref-1",
				null,
				ApprovalStatus.APPROVED,
				null,
				null);
	}

	private DecisionHistoryResponse sampleHistory() {
		return new DecisionHistoryResponse(
				"req-ref-1",
				List.of(
						new DecisionHistoryEntry(
								"fj_123",
								"op-1",
								ApprovalDecisionType.APPROVE,
								null,
								Instant.parse("2026-05-27T10:05:00Z"))));
	}
}
