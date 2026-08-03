# Data Model: Quest Loadout Fulfilment (probe-service)

`probe-service` holds no persisted entities for this feature, consistent
with `docs/entities-enums.md` — every type below is a transient DTO, mapped
1:1 from a request into a downstream call, or from a downstream response
into a public response, on every request. Nothing is stored between calls
(research.md D1, D8).

## Public request DTOs (`dto/request`)

**Revised by Gate 2 (IC-001)**: the item-level DTOs below use raw,
unvalidated fields mirroring `forge-service`'s actual
`LoadoutItemRequest`/`RetryLoadoutItemRequest` implementation exactly
(research.md D7, revised) — no typed enums, no `@Min`/`@Max`/`@NotNull` on
individual item fields, and no `@Size` bound on `items`. This is
intentional: `forge-service` validates every item field itself and reports
invalid ones as individual `rejectedItems` (FR-002/FR-003); if
`probe-service` validated these fields too, an invalid value would reject
the whole request at `probe-service` before `forge-service` ever saw it.

### `CreateProbeLoadoutRequest`

| Field | Type | Validation | Notes |
|---|---|---|---|
| `requestReference` | `String` | `@NotBlank` | Forwarded verbatim; idempotency is entirely `forge-service`'s responsibility (research.md D8). |
| `loadoutName` | `String` | `@NotBlank` | |
| `requestedBy` | `String` | `@NotBlank` | |
| `items` | `List<ProbeLoadoutItemRequest>` | `@NotNull @NotEmpty @Valid` | No `@Size` bound (research.md D7 revised) — the 2–10 item-count check (FR-001) is `forge-service`'s alone, matching its own `CreateLoadoutRequest` Bean Validation footprint exactly. |

### `ProbeLoadoutItemRequest`

| Field | Type | Validation | Notes |
|---|---|---|---|
| `artifactName` | `String` | none | Raw, unvalidated (research.md D7 revised) — matches `forge-service`'s `LoadoutItemRequest.artifactName`. |
| `artifactType` | `String` | none | Raw `String`, not the `domain.ArtifactType` enum — an invalid value must reach `forge-service` to be reported as a rejected item, not fail deserialization at `probe-service`. |
| `material` | `String` | none | Raw `String`, not the `domain.ForgeMaterial` enum — same reasoning. |
| `powerLevel` | `int` | none | No `@Min`/`@Max` — an out-of-range value must reach `forge-service` to be reported as a rejected item. |

### `RetryProbeLoadoutRequest`

| Field | Type | Validation | Notes |
|---|---|---|---|
| `items` | `List<RetryProbeLoadoutItemRequest>` | `@Valid` | Forwarded verbatim; `forge-service` enforces "every currently-eligible item MUST be included" (canonical clarification #4). A `null` `items` list is forwarded as-is — `forge-service`'s own compact constructor defaults it to an empty list. |

### `RetryProbeLoadoutItemRequest`

| Field | Type | Validation | Notes |
|---|---|---|---|
| `loadoutItemId` | `String` | `@NotBlank` | Matches `forge-service`'s `RetryLoadoutItemRequest.loadoutItemId`. |
| `artifactName` | `String` (nullable) | none | Optional — omit to keep the item's prior value (mirrors `forge-service`'s contract). |
| `artifactType` | `String` (nullable) | none | Raw `String`, optional — same IC-001 reasoning as create; an invalid correction value must reach `forge-service`'s own error handling, not fail at `probe-service`. |
| `material` | `String` (nullable) | none | Raw `String`, optional. |
| `powerLevel` | `Integer` (nullable) | none | Optional, unconstrained. |

## Public response DTOs (`dto/response`)

### `ProbeLoadoutResponse`

| Field | Type | Notes |
|---|---|---|
| `loadoutId` | `String` | Verbatim from `forge-service`. |
| `loadoutName` | `String` | |
| `requestedBy` | `String` | |
| `status` | `String` | Opaque pass-through (research.md D3) — `ACCEPTED` on create, one of `IN_PROGRESS`/`READY`/`PARTIALLY_READY`/`FAILED`/`CANCELLED` on retrieval/cancel/retry. |
| `items` | `List<ProbeLoadoutItemResponse>` | Ordered exactly as `forge-service` returns them. |
| `rejectedItems` | `List<ProbeRejectedLoadoutItemResponse>` | Populated on create only; empty/omitted otherwise. |
| `transport` | `ForgeTransport` | Existing `domain.ForgeTransport` enum (`REST`/`GRPC`) — tags which downstream transport served the request, same as `ProbeForgeJobResponse` today. |

### `ProbeLoadoutItemResponse`

| Field | Type | Notes |
|---|---|---|
| `loadoutItemId` | `String` | |
| `artifactName` / `artifactType` / `material` / `powerLevel` | `String`/`String`/`String`/`int` | Current values (post any retry corrections). `artifactType`/`material` kept as raw `String` here (not parsed back to the probe enum) since `forge-service` is authoritative and a future new value must not break deserialization — mirrors `ProbeForgeJobResponse`'s existing enum-parsing risk being deliberately avoided for new fields. |
| `status` | `String` | Opaque pass-through (research.md D3). |
| `approvalStatus` | `String` (nullable) | Opaque pass-through; absent until `POC-HIGH-POWER-APPROVAL` ships (research.md D10). |
| `approvalExpiresAt` | `Instant` (nullable) | Present only while `approvalStatus == "PENDING_APPROVAL"`. |
| `rejectionReason` | `String` (nullable) | Present only when `approvalStatus == "REJECTED"`. |
| `failureReason` | `String` (nullable) | Present only when `status == "FAILED"`. |

### `ProbeRejectedLoadoutItemResponse`

| Field | Type | Notes |
|---|---|---|
| `artifactName` / `artifactType` / `material` / `powerLevel` | as submitted | |
| `reason` | `String` | Validation failure reason, verbatim from `forge-service`. |

### `ProbeLoadoutAttemptHistoryResponse` (attempt-history endpoint)

| Field | Type | Notes |
|---|---|---|
| `loadoutItemId` | `String` | |
| `attempts` | `List<ProbeLoadoutAttemptResponse>` | Ordered oldest → newest, verbatim from `forge-service` (FR-012/FR-017). |

### `ProbeLoadoutAttemptResponse`

| Field | Type | Notes |
|---|---|---|
| `attemptNumber` | `int` | |
| `forgeJobId` | `String` | Verbatim — exposed only on this support-operator attempt-history view (User Story 3), not on the requester-facing create/get responses, consistent with FR-007's "usable by the requester without needing any internal service-specific identifiers." |
| `requesterReference` | `String` (nullable) | Opaque pass-through; absent until `POC-HIGH-POWER-APPROVAL` ships. |
| `status` | `String` | Opaque pass-through. |
| `approvalStatus` | `String` (nullable) | Opaque pass-through. |
| `rejectionReason` | `String` (nullable) | |
| `failureReason` | `String` (nullable) | |
| `createdAt` | `Instant` | |

## Downstream DTOs (`dto/client/forge/request` / `dto/client/forge/response`)

Mirror `forge-service`'s REST contract
(`contracts/quest-loadouts-rest.md` in `Jenish16/forge-service#4`) field for
field, kept structurally identical to the public DTOs above but in
`dto/client/forge/*` per the DTO-boundary rule (`AGENTS.md`,
`docs/AI_CONTEXT.md`): `ForgeCreateLoadoutRestRequest`,
`ForgeLoadoutItemRestRequest`, `ForgeRetryLoadoutRestRequest`,
`ForgeRetryLoadoutItemRestRequest`, `ForgeLoadoutRestResponse`,
`ForgeLoadoutItemRestResponse`, `ForgeRejectedLoadoutItemRestResponse`,
`ForgeLoadoutAttemptRestResponse`.

## gRPC mapping

`ProbeRequestMapper` (or a new `ProbeLoadoutMapper`, per research.md D6) maps
directly between the public DTOs above and the generated
`com.codeistari.forge.artifact.grpc.proto.*` Loadout message types produced
from the mirrored `proto/quest_loadout_service.proto` (research.md D5) — no
intermediate downstream-DTO layer exists for gRPC, matching the existing
`toCreateForgeJobGrpcRequest`/`toProbeForgeJobResponse(ForgeJobGrpcResponse,
...)` pattern.

## No entities, no state transitions

`probe-service` defines no domain entity and no state machine for this
feature (consistent with `docs/entities-enums.md`). Every field above is
computed fresh from `forge-service`'s response on every call; nothing is
cached, stored, or reconciled across requests.

## Validation rules (from Functional Requirements, as applied at probe-service)

| Rule | Requirement | Enforced by |
|---|---|---|
| 2–10 items count bound | FR-001 | `forge-service` only (research.md D7, revised by IC-001) — `probe-service` has no `@Size` bound and forwards any non-empty `items` list |
| Per-item artifact-name/type/material/power-level business rules | FR-002 | `forge-service` only (research.md D7, revised by IC-001) |
| `requestReference` idempotency | FR-008/FR-009 | `forge-service` only (research.md D8) |
| Retry request must include every currently-eligible item | Canonical clarification #4 | `forge-service` only — `probe-service` forwards the request body unmodified |
