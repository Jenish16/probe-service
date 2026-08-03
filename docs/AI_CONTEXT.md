# AI context

## Read first

- [`docs/README.md`](README.md) — index of all docs
- [`../specs/README.md`](../specs/README.md) — how specs/experiments and tasks work
- The relevant `specs/<feature>/spec.md` + `tasks.md` for the feature you are working on

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

## Architecture summary

Layering: `ProbeRequestController` → `ProbeRequestService` → `ProbeRequestMapper` + (`RestForgeJobClient` or `GrpcForgeJobClient`) → `forge-service`. There is no repository/data layer — probe-service is stateless. See `docs/architecture.md` for the full component diagram and request lifecycle, and the "Known anti-patterns / deviations" below for where this layering is not followed strictly (`ForgeJobClient` interface unused for dispatch).

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

## Critical rules

- Keep it lightweight: no frameworks/infrastructure without a clear reason (see `AGENTS.md`).
- Do not structure the codebase around `forge-service` — it is a downstream dependency, not the root module.
- Keep public DTOs (`dto/request`, `dto/response`) separate from downstream forge DTOs (`dto/client/forge/`); never reuse one as the other.
- Do not remove the Resilience4j timeouts/circuit-breaker/retry config without updating `docs/TECH_STACK.md` and the relevant spec.

## Known anti-patterns / deviations

- **Unused abstraction**: `ForgeJobClient` is implemented by both `RestForgeJobClient` and `GrpcForgeJobClient`, but `ProbeRequestService` holds them as concrete-typed fields and calls each directly rather than dispatching through the interface — the interface currently adds no polymorphism.
- **Latent self-invocation trap in `GrpcForgeJobClient`**: its `createForgeJob(ForgeCreateJobRestRequest)`/`getForgeJob(String)` interface-implementing methods internally call the `@CircuitBreaker`/`@Retry`-annotated overloads (`createForgeJob(CreateForgeJobGrpcRequest)`, `getForgeJobGrpc(String)`) via plain `this.method(...)` calls. Spring AOP proxies do not intercept same-class self-invocation, so if any caller is ever routed through the `ForgeJobClient` interface methods on `GrpcForgeJobClient` instead of the current direct calls from `ProbeRequestService`, resilience (circuit breaker + retry) would silently stop applying. Today this is harmless because `ProbeRequestService` calls the annotated methods directly (external bean-to-bean calls, so the proxy is engaged) and never goes through the interface-shaped methods — but do not refactor `ProbeRequestService` to depend on the `ForgeJobClient` interface for the gRPC client without first fixing this self-invocation gap (e.g. by moving the annotations onto the interface-shaped methods, or by self-injecting a proxy reference).
- **Duplicated enums across repos**: `ArtifactType` and `ForgeMaterial` are copy-defined here and in forge-service (no shared library); if forge-service adds/renames a value, this repo must be updated manually and will otherwise fail enum parsing (`ArtifactType.valueOf(...)` in `ProbeRequestMapper`).

## Important flows

- [Create forge job](flows/create-forge-job/README.md) — REST + gRPC create path, including resilience/error mapping
- [Get forge job](flows/get-forge-job/README.md) — REST + gRPC retrieval path, including not-found and downstream-failure handling

## Commands

```bash
./gradlew bootRun   # run the service (REST :8082)
./gradlew test       # run unit, controller, mapper, and client tests
```
