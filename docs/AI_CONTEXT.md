# AI context

## Service role

`probe-service` is a normal Spring Boot caller-side service. It exposes its own REST APIs and may call external/downstream services when required.

Current experiment: **Artifact Forging** — probe-service accepts local forge-job requests and delegates creation/fetching to `forge-service` through REST or gRPC.

## What is not in scope yet

No database, Redis, Kafka, authentication, or distributed tracing in the current experiment.

## Package root

`com.codeistari.probe`

## Package layout

Keep generic backend packages:

- `controller` — public REST endpoints
- `dto` — request/response models (public and downstream client DTOs)
- `service` — orchestration
- `client` — downstream REST and gRPC clients (`BaseApiClient` for shared REST helpers)
- `mapper` — DTO and protobuf mapping
- `config` — Spring configuration and properties
- `exception` — error types and global handler
- `domain` — enums and small domain types

Do not create `com.codeistari.probe.forge` as a top-level module.

## DTO boundaries

- **Public probe-service DTOs**: `dto/request`, `dto/response`
- **Downstream forge REST DTOs**: `dto/client/forge/request`, `dto/client/forge/response`

These boundaries are intentional; contracts may diverge over time.

## Main flow

```text
External caller → probe-service REST API → forge-service (REST or gRPC)
```

## forge-service endpoints

| Transport | Target |
|-----------|--------|
| REST | `http://localhost:8081` (prefix `/api/v1`) |
| gRPC | `localhost:9091` |

## Protobuf

- Proto package: `codeistari.forge.artifact`
- Service: `ArtifactForgeService`
- Generated Java package: `com.codeistari.forge.artifact.grpc.proto`
- Proto file: `proto/artifact_forge_service.proto`
