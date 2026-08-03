package com.codeistari.probe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeistari.probe.client.grpc.GrpcApprovalForgeClient;
import com.codeistari.probe.client.rest.RestApprovalForgeClient;
import com.codeistari.probe.domain.ForgeTransport;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionEntryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeDecisionHistoryRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.mapper.ProbeRequestMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeApprovalServiceTest {

	@Mock private RestApprovalForgeClient restApprovalForgeClient;
	@Mock private GrpcApprovalForgeClient grpcApprovalForgeClient;

	private ProbeApprovalService service;

	@BeforeEach
	void setUp() {
		service =
				new ProbeApprovalService(
						restApprovalForgeClient, grpcApprovalForgeClient, new ProbeRequestMapper());
	}

	@Test
	void listPendingApprovalsViaRestDelegatesToRestClient() {
		when(restApprovalForgeClient.listPendingApprovals()).thenReturn(List.of(sampleJob()));

		var summaries = service.listPendingApprovalsViaRest();

		verify(restApprovalForgeClient).listPendingApprovals();
		assertThat(summaries).hasSize(1);
		assertThat(summaries.get(0).forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void listPendingApprovalsViaGrpcDelegatesToGrpcClient() {
		when(grpcApprovalForgeClient.listPendingApprovals()).thenReturn(List.of(sampleJob()));

		var summaries = service.listPendingApprovalsViaGrpc();

		verify(grpcApprovalForgeClient).listPendingApprovals();
		assertThat(summaries).hasSize(1);
	}

	@Test
	void approveViaRestDelegatesToRestClient() {
		when(restApprovalForgeClient.approveRequest("fj_123", "op-1")).thenReturn(sampleJob());

		var response = service.approveViaRest("fj_123", "op-1");

		verify(restApprovalForgeClient).approveRequest("fj_123", "op-1");
		assertThat(response.transport()).isEqualTo(ForgeTransport.REST);
	}

	@Test
	void approveViaGrpcDelegatesToGrpcClient() {
		when(grpcApprovalForgeClient.approveRequest("fj_123", "op-1")).thenReturn(sampleJob());

		var response = service.approveViaGrpc("fj_123", "op-1");

		verify(grpcApprovalForgeClient).approveRequest("fj_123", "op-1");
		assertThat(response.transport()).isEqualTo(ForgeTransport.GRPC);
	}

	@Test
	void rejectViaRestDelegatesToRestClient() {
		when(restApprovalForgeClient.rejectRequest("fj_123", "op-1", "bad materials"))
				.thenReturn(sampleJob());

		var response = service.rejectViaRest("fj_123", "op-1", "bad materials");

		verify(restApprovalForgeClient).rejectRequest("fj_123", "op-1", "bad materials");
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void rejectViaGrpcDelegatesToGrpcClient() {
		when(grpcApprovalForgeClient.rejectRequest("fj_123", "op-1", "bad materials"))
				.thenReturn(sampleJob());

		var response = service.rejectViaGrpc("fj_123", "op-1", "bad materials");

		verify(grpcApprovalForgeClient).rejectRequest("fj_123", "op-1", "bad materials");
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void cancelViaRestDelegatesToRestClient() {
		when(restApprovalForgeClient.cancelRequest("fj_123", "ranger")).thenReturn(sampleJob());

		var response = service.cancelViaRest("fj_123", "ranger");

		verify(restApprovalForgeClient).cancelRequest("fj_123", "ranger");
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void cancelViaGrpcDelegatesToGrpcClient() {
		when(grpcApprovalForgeClient.cancelRequest("fj_123", "ranger")).thenReturn(sampleJob());

		var response = service.cancelViaGrpc("fj_123", "ranger");

		verify(grpcApprovalForgeClient).cancelRequest("fj_123", "ranger");
		assertThat(response.forgeJobId()).isEqualTo("fj_123");
	}

	@Test
	void getDecisionHistoryViaRestDelegatesToRestClient() {
		when(restApprovalForgeClient.getDecisionHistory("req-ref-1")).thenReturn(sampleHistory());

		var history = service.getDecisionHistoryViaRest("req-ref-1");

		verify(restApprovalForgeClient).getDecisionHistory("req-ref-1");
		assertThat(history.requesterReference()).isEqualTo("req-ref-1");
		assertThat(history.decisions()).hasSize(1);
	}

	@Test
	void getDecisionHistoryViaGrpcDelegatesToGrpcClient() {
		when(grpcApprovalForgeClient.getDecisionHistory("req-ref-1")).thenReturn(sampleHistory());

		var history = service.getDecisionHistoryViaGrpc("req-ref-1");

		verify(grpcApprovalForgeClient).getDecisionHistory("req-ref-1");
		assertThat(history.requesterReference()).isEqualTo("req-ref-1");
	}

	private ForgeJobRestResponse sampleJob() {
		return new ForgeJobRestResponse(
				"fj_123",
				"ember-ring",
				"RING",
				"MITHRIL",
				"ranger",
				9,
				"QUEUED",
				Instant.parse("2026-05-27T10:00:00Z"),
				"req-ref-1",
				null,
				"APPROVED",
				null,
				null);
	}

	private ForgeDecisionHistoryRestResponse sampleHistory() {
		return new ForgeDecisionHistoryRestResponse(
				"req-ref-1",
				List.of(),
				List.of(
						new ForgeDecisionEntryRestResponse(
								"decision-1",
								"fj_123",
								"APPROVE",
								"op-1",
								null,
								Instant.parse("2026-05-27T10:05:00Z"))));
	}
}
