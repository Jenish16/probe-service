# High-Power Artifact Approval — Operator Review & Audit (via probe-service)

## Purpose

Proxy the five operator/audit operations to `forge-service`, via the caller-selected transport (REST or gRPC), for power level 8-10 forge jobs that are gated behind an approval decision. forge-service owns all approval state; probe-service only translates requests/responses and applies the same resilience policy as the create/get flow.

## Entrypoints

| Endpoint | Downstream transport | Source |
|---|---|---|
| `GET /api/v1/probe-requests/forge-jobs/pending/{rest,grpc}` | list pending approvals | `ProbeApprovalController#listPendingViaRest/Grpc` |
| `POST /api/v1/probe-requests/forge-jobs/{forgeJobId}/approve/{rest,grpc}` | approve | `ProbeApprovalController#approveViaRest/Grpc` |
| `POST /api/v1/probe-requests/forge-jobs/{forgeJobId}/reject/{rest,grpc}` | reject (reason required) | `ProbeApprovalController#rejectViaRest/Grpc` |
| `POST /api/v1/probe-requests/forge-jobs/{forgeJobId}/cancel/{rest,grpc}` | cancel | `ProbeApprovalController#cancelViaRest/Grpc` |
| `GET /api/v1/probe-requests/forge-jobs/history/{requesterReference}/{rest,grpc}` | decision history | `ProbeApprovalController#historyViaRest/Grpc` |

## Steps

1. Caller sends a REST request to one of the `/rest` or `/grpc` endpoints above.
2. `ProbeApprovalController` validates the request body (`ApprovalDecisionRequest`, `RejectDecisionRequest` — reason required, `CancelRequestRequest`) via Jakarta Bean Validation.
3. `ProbeApprovalService` calls `RestApprovalForgeClient` or `GrpcApprovalForgeClient`, wrapped in the same `FORGE_REST`/`FORGE_GRPC` Resilience4j `@CircuitBreaker`/`@Retry` pair used by the create/get flow.
4. On success, `ProbeRequestMapper` converts the forge-service response (`ForgeJobRestResponse` for approve/reject/cancel; a list for pending; `ForgeDecisionHistoryRestResponse` for history) into the public DTO (`ProbeForgeJobResponse`, `List<ProbePendingApprovalSummary>`, or `DecisionHistoryResponse`).
5. On failure, the client maps the error to a `ForgeRemoteCallException` with the status forge-service returned — including `409 Conflict` for repeated/competing decisions and post-expiry actions, which forge-service enforces per the Functional Spec's idempotency/retry rules — rendered by `GlobalExceptionHandler`.

## Key source files

| File | Role |
|---|---|
| `controller/ProbeApprovalController.java` | Public entrypoints |
| `service/ProbeApprovalService.java` | Orchestration |
| `mapper/ProbeRequestMapper.java` | Public ↔ downstream mapping for approval DTOs |
| `client/rest/RestApprovalForgeClient.java`, `client/BaseApiClient.java` | REST downstream calls + error mapping |
| `client/grpc/GrpcApprovalForgeClient.java` | gRPC downstream calls + error mapping |

## Edge cases

- `reject` without a `reason` → 400, no downstream call made.
- Unknown `forgeJobId` → 404 (forge-service `NOT_FOUND`).
- Repeating the same decision type, a competing terminal decision, or acting on an already-terminal/expired request → 409 (forge-service `FAILED_PRECONDITION`/`ALREADY_EXISTS`), per the Functional Spec's clarified idempotency rules.
- forge-service 5xx / network failure / gRPC `UNAVAILABLE`/`INTERNAL` → retried, then 502/504.
- Circuit breaker open → 503, without attempting the call.
