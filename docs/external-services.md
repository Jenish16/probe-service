# External Services

## Downstream services (probe-service calls out to)

| Service | Protocol | Endpoints/methods used | Timeout/deadline | Resilience |
|---|---|---|---|---|
| `forge-service` | REST | `POST /api/v1/forge-jobs`, `GET /api/v1/forge-jobs/{forgeJobId}` (base URL `http://localhost:8081`, configurable via `forge.rest.base-url`) | `forge.rest.timeout-ms` (default 2000ms) | Resilience4j `FORGE_REST` circuit breaker + retry |
| `forge-service` | gRPC | `ArtifactForgeService.CreateForgeJob`, `ArtifactForgeService.GetForgeJob` (`localhost:9091`, configurable via `forge.grpc.host`/`forge.grpc.port`) | `forge.grpc.deadline-ms` (default 2000ms) | Resilience4j `FORGE_GRPC` circuit breaker + retry |

`forge-service` is the only downstream dependency in this codebase — confirmed by scanning `client/` for all outbound calls.

## Upstream callers (call into probe-service)

None confirmed. A workspace-wide scan found no other repository calling `probe-service`'s REST API (`:8082`, `/api/v1/probe-requests/**`). Per `docs/AI_CONTEXT.md`, it is designed to be hit by an external/manual caller (see `docs/LOCAL_DEVELOPMENT.md` for example `curl` calls); there is currently no service-to-service caller.

## Events / async topics

None. No Kafka, message queue, or event producer/consumer exists in this codebase.

## Third-party integrations

| Integration | Purpose |
|---|---|
| `io.grpc` (grpc-netty-shaded, grpc-stub, grpc-protobuf) | gRPC client runtime (`ManagedChannel` to forge-service) |
| `com.google.protobuf` | Protobuf message/timestamp types |
| `io.github.resilience4j` (resilience4j-spring-boot4) | Circuit breaker + retry around both forge-service clients |

These are libraries embedded in the same process, not external service dependencies.

## AI guidance

- `forge-service` is the sole downstream dependency; both REST and gRPC paths are actively exercised. Do not change `dto/client/forge/*` or the gRPC client without confirming forge-service's actual contract (`forge-service/proto/artifact_forge_service.proto`, `forge-service/specs/001-artifact-forging-rest-grpc/spec.md`).
- Do not remove or loosen the Resilience4j timeouts/retry predicates in `ResilienceConfig` without updating `docs/TECH_STACK.md` and the relevant spec — the retry-on-5xx/network-only, fail-fast-on-4xx design is deliberate.
- Because there is no confirmed upstream caller, changing probe-service's public REST contract (`dto/request`, `dto/response`) is lower-risk than changing the forge-service-facing contract, but should still go through `specs/001-artifact-forging-rest-grpc/spec.md`.
