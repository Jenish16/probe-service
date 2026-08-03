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

## D7 — Validation split

- **Decision**: `probe-service` performs only structural Jakarta Bean
  Validation on its public request DTOs (`@NotBlank`/`@NotNull`/`@Size(min=2,
  max=10)` on `items`/`@Min`/`@Max` on `powerLevel`), matching the existing
  `CreateProbeForgeJobRequest` pattern. Per-item business-rule validation
  (artifact-name/type/material/power-level rules, FR-002) and the resulting
  `rejectedItems` reporting are entirely `forge-service`'s responsibility;
  `probe-service` passes `forge-service`'s validation outcome through
  unmodified.
- **Rationale**: Matches the existing split exactly — `probe-service` never
  duplicates forge-service's business validation rules today, and D1 (pure
  pass-through) means it must not start doing so for loadouts either.
- **Alternatives considered**: Duplicating forge-service's per-item
  validation rules in `probe-service` — rejected; violates `AGENTS.md`
  ("Do not duplicate forge-service business logic or validation rules
  beyond caller-side pre-checks") and would risk drifting from
  `forge-service`'s rules over time.

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
