# Spec 001: Artifact Forging (REST + gRPC)

## Goal

Implement caller-side APIs in `probe-service` that delegate forge job creation and retrieval to `forge-service` through REST or gRPC.

## probe-service endpoints

| Method | Path | Downstream |
|--------|------|------------|
| POST | `/api/v1/probe-requests/forge-jobs/rest` | forge REST |
| POST | `/api/v1/probe-requests/forge-jobs/grpc` | forge gRPC |
| GET | `/api/v1/probe-requests/forge-jobs/{forgeJobId}/rest` | forge REST |
| GET | `/api/v1/probe-requests/forge-jobs/{forgeJobId}/grpc` | forge gRPC |

## Structure

Keep normal service packages under `com.codeistari.probe`. Do not root the codebase on `forge-service`.

Public DTOs and downstream forge REST DTOs stay separate even when fields look similar.

## forge-service contract

| Transport | Target |
|-----------|--------|
| REST base URL | `http://localhost:8081` |
| REST prefix | `/api/v1` |
| gRPC target | `localhost:9091` |
| Proto package | `codeistari.forge.artifact` |
| Generated Java | `com.codeistari.forge.artifact.grpc.proto` |

Proto file: `proto/artifact_forge_service.proto`

Enum-like gRPC fields are strings (e.g. `"RING"`, `"MITHRIL"`, `"QUEUED"`). `created_at` is `google.protobuf.Timestamp`.

## Out of scope

No database, Redis, Kafka, auth, tracing, or service discovery in this experiment.

## Validation (probe-service)

Align with forge-service before downstream calls:

- `artifactName` — required, not blank
- `artifactType` — required
- `material` — required
- `requestedBy` — required, not blank
- `powerLevel` — integer 1–10

## Error mapping

- REST downstream: 400, 404, 500 → probe-service errors
- gRPC downstream: `INVALID_ARGUMENT`, `NOT_FOUND`, `DEADLINE_EXCEEDED`, `INTERNAL` → probe-service errors

## Response

Public responses include `transport`: `REST` or `GRPC`.
