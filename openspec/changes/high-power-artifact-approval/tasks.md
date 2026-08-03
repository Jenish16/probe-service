## 1. Domain and data model (design.md § State and data models)

- [x] 1.1 Add `domain.ApprovalStatus` enum (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `EXPIRED`, `CANCELLED`). Implemented with an additional `NOT_REQUIRED` value to mirror forge-service's actual wire value for power level 1-7 jobs exactly; `ProbeRequestMapper` translates `NOT_REQUIRED` to `null` on the public `ProbeForgeJobResponse`, preserving the originally-designed public contract.
- [x] 1.2 Extend `dto.request.CreateProbeForgeJobRequest` with `requesterReference` (`@NotBlank`, Decision 4) and `originalRequestReference` (optional, Decision 5).
- [x] 1.3 Extend `dto.response.ProbeForgeJobResponse` with `requesterReference`, `originalRequestReference`, `approvalStatus`, `rejectionReason`, `approvalExpiresAt`.
- [x] 1.4 Add new public DTOs: `ProbePendingApprovalSummary`, `ApprovalDecisionRequest`, `CancelRequestRequest`, `DecisionHistoryEntry`, `DecisionHistoryResponse`. Also added `RejectDecisionRequest` (separate from `ApprovalDecisionRequest`) and `domain.ApprovalDecisionType`, since forge-service's confirmed contract (`forge-service#5`) uses distinct `ApproveForgeJobRequest`/`RejectForgeJobRequest` types and a 3-value `ApprovalDecisionType` (`APPROVE`/`REJECT`/`CANCEL`) for decision history, not the single shared type/2-value enum originally sketched in design.md.
- [x] 1.5 Extend downstream REST DTOs `dto.client.forge.request.ForgeCreateJobRestRequest` / `dto.client.forge.response.ForgeJobRestResponse` with the same additive fields, plus new downstream DTOs for decision-history (REST shape). No separate pending-summary downstream DTO was needed — forge-service's confirmed `GET /pending-approval` reuses `ForgeJobResponse` directly (`forge-service#5`), so `listPendingApprovals()` returns `List<ForgeJobRestResponse>`, converted to `ProbePendingApprovalSummary` only at the mapper boundary.

## 2. Downstream client layer (design.md § Interfaces, cross-service dependencies D-1..D-4)

- [x] 2.1 Define `client.ApprovalForgeClient` interface: `listPendingApprovals()`, `approveRequest(forgeJobId, operatorId)`, `rejectRequest(forgeJobId, operatorId, reason)`, `cancelRequest(forgeJobId, requestedBy)`, `getDecisionHistory(requesterReference)` — kept separate from `ForgeJobClient` (Decision: Alternatives).
- [x] 2.2 Implement `client.rest.RestApprovalForgeClient` extending `BaseApiClient`, reusing `FORGE_REST` circuit-breaker/retry names, following `RestForgeJobClient`'s fallback-method pattern.
- [x] 2.3 Implement `client.grpc.GrpcApprovalForgeClient` reusing `FORGE_GRPC` circuit-breaker/retry names and `properties.grpc().deadlineMs()`, reusing `GrpcForgeJobClient`'s `mapGrpcException` status-mapping approach. Also added `FAILED_PRECONDITION`/`ALREADY_EXISTS` → 409 mapping, needed for the idempotency/competing-decision conflict responses this capability introduces (not present in the original create/get-only mapping).
- [x] 2.4 Added the gRPC request/response message types for the new operations by mirroring `forge-service#5`'s actual, already-implemented `proto/artifact_forge_service.proto` exactly (confirmed at Gate 2 — not a unilateral change).

## 3. Mapper extensions (design.md § Components)

- [x] 3.1 Extend `mapper.ProbeRequestMapper` create/get methods (both REST and gRPC directions) to carry the new fields end-to-end.
- [x] 3.2 Add mapper methods for `ProbePendingApprovalSummary`, `DecisionHistoryResponse`, and the approve/reject/cancel request/response shapes, for both REST and gRPC directions, following the existing per-transport overload pattern.

## 4. Service layer (design.md § Components, § Architecture)

- [x] 4.1 `service.ProbeRequestService`'s four existing methods already pass `requesterReference`/`originalRequestReference` through on submission and surface approval fields on get, for both REST and gRPC paths — no code change was needed since both flows were already fully delegated to `ProbeRequestMapper` (Task 3.1).
- [x] 4.2 Create `service.ProbeApprovalService` with per-transport method pairs (`...ViaRest`/`...ViaGrpc`, matching `ProbeRequestService`'s naming convention) for list-pending, approve, reject, cancel, and get-decision-history.

## 5. Controller layer (design.md § Interfaces)

- [x] 5.1 Update `controller.ProbeRequestController`'s Javadoc/validation for the extended request/response shapes (no new endpoints on this controller).
- [x] 5.2 Create `controller.ProbeApprovalController` with the 10 new REST endpoints from design.md's Interfaces table (`/pending/{rest,grpc}`, `/{forgeJobId}/approve/{rest,grpc}`, `/{forgeJobId}/reject/{rest,grpc}`, `/{forgeJobId}/cancel/{rest,grpc}`, `/history/{requesterReference}/{rest,grpc}`).

## 6. Validation and error handling (design.md § Error handling)

- [x] 6.1 Enforce `reason` as required only on the reject path — implemented as a dedicated `RejectDecisionRequest` subtype (rather than a validation group), matching forge-service's own confirmed `ApproveForgeJobRequest`/`RejectForgeJobRequest` split exactly.
- [x] 6.2 Verified (via `ProbeApprovalControllerTest`/`RestApprovalForgeClientTest`/`GrpcApprovalForgeClientTest`) that `ForgeRemoteCallException` + `GlobalExceptionHandler` + `ResilienceConfig`'s existing predicates require no changes to correctly map 400/404/409/503/504 for every new call path; the only addition was the `FAILED_PRECONDITION`/`ALREADY_EXISTS` → 409 gRPC mapping (Task 2.3), REST 409s already pass through unchanged via `BaseApiClient.handleResponse`'s generic 4xx branch.

## 7. Tests (design.md § Testing)

- [x] 7.1 Unit tests for `ProbeApprovalService` covering all five operations across both transports.
- [x] 7.2 Unit tests for the `ProbeRequestMapper` extensions (new fields, new DTOs, both transports, including the `NOT_REQUIRED` → `null` translation).
- [x] 7.3 Validation tests for the extended `CreateProbeForgeJobRequest`, `ApprovalDecisionRequest`/`RejectDecisionRequest`, and `CancelRequestRequest`.
- [x] 7.4 Controller tests for `ProbeApprovalController` and the extended `ProbeRequestController`, covering success, 400 (missing reject reason, blank operator/requester fields), 404, and 409 (conflicting/competing decisions) cases.
- [x] 7.5 Client tests for `RestApprovalForgeClient` (`MockRestServiceServer`) / `GrpcApprovalForgeClient` (Mockito-stubbed blocking stub, matching the existing `GrpcForgeJobClientTest` pattern), including 400/404/409 error-mapping cases.
- [x] 7.6 Scenario-coverage pass: all 33 `#### Scenario:` blocks across the six `specs/high-power-approval-*/spec.md` files describe behavior forge-service owns and enforces (idempotency dedupe, expiry timers, decision recording, lifecycle transitions); probe-service's slice of every scenario is limited to correctly proxying the request/response/error shape, which `ProbeApprovalServiceTest`, `ProbeApprovalControllerTest`, `RestApprovalForgeClientTest`, and `GrpcApprovalForgeClientTest` cover for all five operations across both transports (success, validation, 404, 409). No probe-service-owned gap found.

## 8. Documentation (per `.cursor/rules/docs-maintenance.mdc`)

- [x] 8.1 Update `docs/architecture.md`'s component diagram and request lifecycle for the new approval flow and `ProbeApprovalController`/`ProbeApprovalService`/`ApprovalForgeClient` components.
- [x] 8.2 Update `docs/entities-enums.md` with the new `ApprovalStatus`/`ApprovalDecisionType` enums, keeping the "no persisted entities / stateless" statement accurate.
- [x] 8.3 Update `docs/external-services.md` with the new forge-service operations (list/approve/reject/cancel/history) and additive create/get fields, confirmed against `forge-service#5`'s actual implementation.
- [x] 8.4 Add/update `docs/flows/` entries for the new approval-review, cancellation, and history flows, matching the existing flow-doc style used for create/get forge job.
- [x] 8.5 Update `docs/AI_CONTEXT.md`'s "Important flows" list and current-experiment description to mention the high-power approval capability.

## 9. Cross-service coordination (implementation-conflict-review.md, Gate 2 `dependency`)

- [x] 9.1 D-1 through D-5 confirmed against `forge-service#5`'s actual implementation; `design.md`'s Cross-service dependencies section updated in place.
- [x] 9.2 `IC-001` (mandatory joint rollout) and `IC-002` (mirror `originalRequestId` at the downstream DTO boundary) recorded and applied to `design.md`.
- [x] 9.3 Implement Tasks 1–3's downstream DTOs (`ForgeCreateJobRestRequest`, `ForgeJobRestResponse`, gRPC equivalents) using forge-service's exact field name `originalRequestId`, translating to/from the public `originalRequestReference` field in `ProbeRequestMapper` (`IC-002`).
- [x] 9.4 Implement `RestApprovalForgeClient.getDecisionHistory` calling `GET /api/v1/forge-jobs/history?requesterReference={ref}` as a query parameter, matching forge-service's confirmed REST contract.
- [ ] 9.5 **Do not merge probe-service's implementation PR before, or independently of, `forge-service#5` (or its merged successor)** — this is a hard joint-rollout dependency (`IC-001`), because forge-service's `requesterReference` field becoming required is a breaking change that probe-service's idempotency/retry logic depends on being real (not a server-generated fallback).
- [x] 9.6 Before opening probe-service's implementation PR, re-check `forge-service#5`'s state (merged, still open, or superseded) and re-run Gate 2 discovery if its contract has changed since this review. Re-checked at implementation time: `forge-service#5` is still `OPEN`, same commit (`f99f4b26`) reviewed at Gate 2 — no drift, no re-run needed.

## 10. Completion validation (`.sdd-parallel/WORKFLOW.md` § Implementation completion validation)

- [ ] 10.1 Refresh the Functional Code Context to reflect the implemented approval capability.
- [ ] 10.2 Refresh the Technical Code Context / `docs/` set (Tasks 8.1–8.5) to reflect the as-implemented approval workflow.
- [ ] 10.3 Open the `[SDD-IMPL][POC-HIGH-POWER-APPROVAL]` implementation PR(s) and record their URLs in `status.yaml`'s `pull_requests.implementation`.
- [ ] 10.4 Rerun completion validation (merge status, mandatory tasks, tests, integration validation, docs, both context refreshes) before setting `status: implemented`.
