# Phase 0 Research: Quest Loadout Fulfilment (probe-service)

All items below were `NEEDS CLARIFICATION` in the initial Technical Context
because the functional spec deliberately leaves participating-service
responsibilities, orchestration ownership, APIs, and storage unresolved, and
because `forge-service`'s already-published Technical Plan for this same
initiative (`Jenish16/forge-service#4`) fixes its own contract without
addressing `probe-service`'s role. D1–D3 were resolved interactively with the
developer (Gate 1 / `/speckit-plan`); D4–D10 are low-risk technical decisions
that follow directly from D1–D3 and existing repository conventions.

## D1 — `probe-service`'s role (Gate 1, PD-001)

- **Decision**: `probe-service` exposes its own public `Probe*`-prefixed
  Loadout REST API and delegates every operation to `forge-service`'s
  already-fixed `/api/v1/loadouts/*` REST and `LoadoutService` gRPC contract.
  `probe-service` owns no Loadout aggregate, state, or business logic of its
  own — it is a stateless pass-through facade.
- **Rationale**: Developer-confirmed (Gate 1, `parallel-design-review.md`
  PD-001). `forge-service`'s published plan explicitly states it owns the
  Loadout aggregate and does not plan `probe-service`; mirroring the only
  existing precedent in this codebase (single-artifact REST/gRPC facade)
  avoids inventing a new integration style and cannot conflict with
  `forge-service`'s already-fixed, additive contract.
- **Alternatives considered**: (a) `probe-service` not involved at all,
  requesters call `forge-service` directly — rejected by the developer; (b)
  `probe-service` builds an independent Loadout aggregate on top of
  `forge-service`'s unchanged single-artifact contract, ignoring
  `forge-service`'s new Loadout capability — rejected as a certain Gate 2
  conflict (two independent implementations of the same initiative).

## D2 — Endpoint / transport-selection shape

- **Decision**: Every one of the five Loadout operations gets a `/rest` and
  `/grpc` path-suffix variant (10 endpoints total) under a new
  `/api/v1/probe-requests/loadouts` resource, exactly mirroring
  `/api/v1/probe-requests/forge-jobs/{rest,grpc}`.
- **Rationale**: Developer-confirmed. Directly satisfies FR-020 ("consistent
  behavior regardless of REST or gRPC") using the same mechanism already
  proven for single-artifact requests — the caller picks the downstream
  transport via the URL path.
- **Alternatives considered**: A single fixed-transport endpoint set — the
  developer confirmed the existing per-transport pattern instead.

## D3 — Status representation

- **Decision**: `probe-service` does not define its own status enums for
  Loadout/item/attempt status fields (`ACCEPTED`, `IN_PROGRESS`, `READY`,
  `PARTIALLY_READY`, `FAILED`, `CANCELLED`, or the underlying
  `QUEUED`/`FORGING`/`COMPLETED`/`FAILED`/`CANCELLED` item statuses, or the
  pass-through `approvalStatus` values). All status fields are opaque
  `String` pass-throughs from `forge-service`'s response.
- **Rationale**: Developer-confirmed. Matches the existing, deliberate
  decoupling documented in `docs/entities-enums.md` for `ForgeJobStatus`:
  "probe-service does not need to know forge-service's status values to
  relay them." Also means `probe-service` never needs updating if
  `forge-service` or the sibling High-Power-Approval initiative adds a new
  status value.
- **Alternatives considered**: A typed `ProbeLoadoutStatus` enum mirroring
  `forge-service`'s values — rejected by the developer; would require
  `probe-service` to track every value `forge-service` (and its own
  cross-initiative dependency) ever introduces.

## D4 — Resilience instance reuse

- **Decision**: The new Loadout REST/gRPC clients reuse the existing
  `FORGE_REST` / `FORGE_GRPC` Resilience4j circuit-breaker and retry
  instances; no new instance names are introduced.
- **Rationale**: `FORGE_REST`/`FORGE_GRPC` already exist as
  per-transport (not per-endpoint) policies guarding all calls to
  `forge-service` over that transport (`ResilienceConfig`,
  `application.yml`). The new Loadout endpoints are additional calls to the
  same downstream host over the same transports; reusing the existing
  instances avoids new configuration surface for a lightweight POC.
- **Alternatives considered**: New `LOADOUT_REST`/`LOADOUT_GRPC` instances
  for isolated blast radius — rejected as unnecessary extra configuration;
  nothing in the functional spec calls for isolating Loadout failures from
  single-artifact failures.

## D5 — gRPC contract mirroring

- **Decision**: `forge-service`'s new `proto/quest_loadout_service.proto`
  (from `Jenish16/forge-service#4`) is copied verbatim into
  `probe-service`'s own `proto/` directory, generating a
  `LoadoutServiceGrpc` stub via the existing `com.google.protobuf` Gradle
  plugin (already configured with `srcDir("proto")`).
- **Rationale**: Mirrors the existing `proto/artifact_forge_service.proto`
  convention exactly — `probe-service` already keeps its own copy of
  `forge-service`'s proto to generate a gRPC *client* stub (it has no
  server-side gRPC, per `docs/architecture.md`).
- **Alternatives considered**: A shared proto library/submodule — rejected;
  out of scope for this lightweight POC and not how the existing
  single-artifact proto is managed.

## D6 — Public DTO shape

- **Decision**: New `dto/request`/`dto/response` DTOs (`Probe`-prefixed)
  mirror `forge-service`'s REST contract fields 1:1 (`CreateProbeLoadoutRequest`,
  `ProbeLoadoutItemRequest`, `RetryProbeLoadoutRequest`,
  `RetryProbeLoadoutItemRequest`, `ProbeLoadoutResponse`,
  `ProbeLoadoutItemResponse`, `ProbeRejectedLoadoutItemResponse`,
  `ProbeLoadoutAttemptResponse`). New downstream DTOs
  (`dto/client/forge/request`/`response`) mirror `forge-service`'s REST
  contract exactly for the client layer, kept separate from the public DTOs
  per `AGENTS.md`.
- **Rationale**: Direct consequence of D1 (pure pass-through) — there is no
  independent probe-side data model to design; the shape is dictated by
  `forge-service`'s already-fixed contract (`contracts/quest-loadouts-rest.md`
  in `Jenish16/forge-service#4`).
- **Alternatives considered**: None — D1 already forecloses inventing an
  independent shape.

## D7 — Validation split (revised by Gate 2, IC-001)

- **Original decision (superseded)**: `probe-service` performs structural
  Jakarta Bean Validation on its public request DTOs, including typed
  `ArtifactType`/`ForgeMaterial` enums with `@NotNull` and `@Min`/`@Max` on
  `powerLevel`, and `@Size(min = 2, max = 10)` on `items` — matching the
  existing `CreateProbeForgeJobRequest` pattern.
- **Gate 2 finding (IC-001)**: `forge-service`'s actual implementation
  (`Jenish16/forge-service#6`, `LoadoutItemRequest.java`,
  `RetryLoadoutItemRequest.java`, `LoadoutService.validationFailureReason`)
  deliberately uses **raw, unvalidated `String` fields** for
  `artifactType`/`material` and an unconstrained `int`/`Integer`
  `powerLevel` — no Bean Validation at all on individual item fields — so
  that an invalid value becomes an individual `RejectedItemResponse` entry
  (FR-002/FR-003) rather than failing Bean Validation for the whole request.
  The 2–10 item-count bound (FR-001) is likewise **not** Bean-Validation-
  enforced on `forge-service`'s side; it is a manual check in
  `LoadoutService.createLoadout` with a dedicated error message. If
  `probe-service` used typed enums or numeric bounds on these same fields,
  an invalid value would fail JSON deserialization or Bean Validation at
  `probe-service` and reject the **entire** request with a generic `400`,
  making it impossible for a `probe-service` caller to ever reach
  `forge-service`'s correct "some items accepted, one rejected with a
  reason" behavior (spec.md User Story 1, Acceptance Scenario 2).
- **Revised decision (developer-confirmed)**: `probe-service`'s
  `ProbeLoadoutItemRequest` and `RetryProbeLoadoutItemRequest` mirror
  `forge-service`'s `LoadoutItemRequest`/`RetryLoadoutItemRequest` exactly —
  raw `String artifactName`/`artifactType`/`material`, unconstrained
  `int`/`Integer powerLevel`, with **no** per-field Bean Validation
  annotations. `CreateProbeLoadoutRequest.items` uses only `@NotNull
  @NotEmpty @Valid` (matching `forge-service`'s own Bean Validation
  footprint on `CreateLoadoutRequest` 1:1) — **no** `@Size(min=2, max=10)`.
  All per-item business-rule validation and the 2–10 item-count bound
  remain entirely `forge-service`'s responsibility; `probe-service` forwards
  every item regardless of its own opinion on validity and passes
  `forge-service`'s validation outcome (`rejectedItems`, or a `400` with
  `forge-service`'s dedicated message) through unmodified. `probe-service`
  retains `@NotBlank` on `requestReference`/`loadoutName`/`requestedBy` and
  `@NotBlank` on `RetryProbeLoadoutItemRequest.loadoutItemId` — these match
  `forge-service`'s own Bean Validation exactly and have no
  behavior-narrowing effect (both sides reject blank container fields the
  same way, with the same category of generic message).
- **Rationale**: A caller-side structural pre-check is only equivalent, not
  duplicative, when it produces the exact same outcome as `forge-service`'s
  authoritative check for every input — which holds for blank
  container-level fields, but not for typed/bounded item fields (those
  change *which* items get rejected and by *which* mechanism). Using typed
  enums or numeric bounds here would silently duplicate — and narrow —
  `forge-service`'s business validation, violating `AGENTS.md` ("Do not
  duplicate forge-service business logic or validation rules beyond
  caller-side pre-checks") in a way that breaks FR-002/FR-003 for
  `probe-service` callers specifically.
- **Alternatives considered**: Keep the original typed/bounded design and
  accept the behavior difference as documented — rejected by the developer;
  it would make `probe-service`'s create/retry endpoints non-conformant
  with FR-002/FR-003/FR-016 for a whole class of otherwise-valid requests.

## D11 — gRPC `ALREADY_EXISTS` error mapping (Gate 2, IC-002)

- **Gate 2 finding**: `forge-service`'s Loadout gRPC service
  (`LoadoutGrpcService.createLoadout`) returns
  `Status.ALREADY_EXISTS.withDescription(...)` for the duplicate-
  `requestReference`-with-different-content case (REST's `409 Conflict`,
  FR-009). The existing `GrpcForgeJobClient.mapGrpcException` switch
  (reused by the single-artifact flow) has no `ALREADY_EXISTS` case; it
  would fall through to the generic `default → 502 Bad Gateway` branch,
  silently misclassifying a legitimate `409` as a downstream failure.
- **Decision**: `GrpcLoadoutClient`'s own gRPC-exception-mapping method
  adds an explicit case: `ALREADY_EXISTS → HttpStatus.CONFLICT` (409),
  alongside the existing `INVALID_ARGUMENT → 400`, `NOT_FOUND → 404`,
  `DEADLINE_EXCEEDED → 504`, `INTERNAL/UNAVAILABLE/RESOURCE_EXHAUSTED →
  502` cases (mirrors `GrpcForgeJobClient.mapGrpcException`'s structure,
  extended with the one additional code the Loadout API introduces).
- **Rationale**: `forge-service`'s gRPC and REST Loadout APIs must produce
  equivalent `probe-service`-facing outcomes (FR-020, "consistent behavior
  regardless of whether the underlying integration uses REST or gRPC") —
  the REST path already forwards `409` generically via
  `BaseApiClient.handleResponse`; the gRPC path needs this one explicit
  addition since its error mapping is a hardcoded switch, not generic.
- **Alternatives considered**: Reusing `GrpcForgeJobClient.mapGrpcException`
  as-is for `GrpcLoadoutClient` — rejected; it has no `ALREADY_EXISTS` case
  and editing it to add one would also change the single-artifact flow's
  gRPC error mapping, which has no idempotency-conflict case in its own
  contract today (FR-019, must stay unmodified).

## D8 — Idempotency handling

- **Decision**: `probe-service` has no idempotency store of its own for
  `requestReference`. It forwards every create request to `forge-service`
  unconditionally; `forge-service`'s own idempotency logic (its `research.md`
  D12) determines whether to return an existing loadout (200) or a new one
  (201) or reject with 409.
- **Rationale**: Direct consequence of D1 — `probe-service` holds no
  Loadout state (`docs/entities-enums.md`: "probe-service holds no persisted
  domain entities"), so it cannot itself detect duplicates.
- **Alternatives considered**: None — precluded by D1.

## D9 — Error mapping

- **Decision**: No new exception-handling code is needed. The existing
  `BaseApiClient.handleResponse` already forwards any 4xx status
  (`ForgeRemoteCallException(status, message)`, including a `409` from
  `forge-service`'s idempotency-conflict case) and any 5xx as `502 Bad
  Gateway`; `GlobalExceptionHandler` already maps `ForgeRemoteCallException`
  generically by its carried status. This applies unchanged to the new
  `RestLoadoutClient`/`GrpcLoadoutClient`.
- **Rationale**: The existing error-mapping pipeline is already
  status-code-generic (not hardcoded to specific ForgeJob-only statuses), so
  no forge-service Loadout-specific status code (400/404/409) requires new
  handling.
- **Alternatives considered**: None — existing pipeline already covers this.

## D10 — Cross-initiative pass-through fields

- **Decision**: `forge-service`'s response fields owned by the sibling
  `POC-HIGH-POWER-APPROVAL` initiative (`approvalStatus`, `approvalExpiresAt`,
  `rejectionReason`) are included as optional/nullable fields in
  `probe-service`'s public response DTOs, passed through verbatim like every
  other field (per D3, as opaque strings/timestamps). `probe-service` never
  derives, interprets, or requires these fields.
- **Rationale**: Direct consequence of D1 (pure pass-through) and D3 (opaque
  status representation) — no separate decision is needed since
  `probe-service` treats every `forge-service` response field the same way,
  regardless of which initiative owns it.
- **Alternatives considered**: Omitting these fields from `probe-service`'s
  contract until `POC-HIGH-POWER-APPROVAL` ships — rejected, since
  `forge-service`'s contract already defines them as present-but-absent
  (never a placeholder value) today, so pass-through requires no conditional
  logic to add later.
