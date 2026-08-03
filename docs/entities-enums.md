# Entities & Enums

## Entities

None. `probe-service` holds no persisted domain entities — a scan of `domain/`, the (absent) `repository/` package, and `resources/` confirms there is no database, cache, or in-memory store of its own. All request/response types (`CreateProbeForgeJobRequest`, `ProbeForgeJobResponse`, `ForgeCreateJobRestRequest`, `ForgeJobRestResponse`, and the High-Power Artifact Approval DTOs below) are transient DTOs, not entities — the service is a stateless caller. All approval state (`PENDING_APPROVAL`/`APPROVED`/`REJECTED`/`EXPIRED`/`CANCELLED`, idempotency, decision history) is owned and persisted by `forge-service`, not probe-service.

## Enums

| Enum | Values | Source | Notes |
|---|---|---|---|
| `ArtifactType` | `RING`, `BLADE`, `STAFF`, `AMULET`, `SHIELD`, `SCROLL` | `domain/ArtifactType.java` | Duplicated from forge-service's enum of the same name (separate repos, no shared library) — must be kept in sync manually if forge-service's values change. |
| `ForgeMaterial` | `MITHRIL`, `ELVEN_STEEL`, `DWARVEN_IRON`, `OBSIDIAN`, `SILVERWOOD` | `domain/ForgeMaterial.java` | Same duplication note as `ArtifactType`. |
| `ForgeTransport` | `REST`, `GRPC` | `domain/ForgeTransport.java` | probe-service-specific; tags `ProbeForgeJobResponse` with which downstream transport served the request. |
| `ApprovalStatus` | `NOT_REQUIRED`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `EXPIRED`, `CANCELLED` | `domain/ApprovalStatus.java` | Mirrored from forge-service (the owning service). forge-service's wire value `NOT_REQUIRED` (power level 1-7 jobs) is translated to a `null` `ProbeForgeJobResponse.approvalStatus` by `ProbeRequestMapper`, per the public API contract. |
| `ApprovalDecisionType` | `APPROVE`, `REJECT`, `CANCEL` | `domain/ApprovalDecisionType.java` | Mirrored from forge-service; used only in `DecisionHistoryEntry.decision`. |

Note: unlike forge-service, `probe-service` does not define its own `ForgeJobStatus` enum. `ProbeForgeJobResponse.status` is a raw `String`, passed through opaquely from whatever forge-service returns (`ForgeJobRestResponse.status` / gRPC `status` field are also `String`). This is a deliberate (if implicit) decoupling — probe-service does not need to know forge-service's status values to relay them.

## State transitions

None owned by probe-service — the `PENDING_APPROVAL → APPROVED/REJECTED/EXPIRED/CANCELLED` approval lifecycle (power level 8-10 only) and retry/idempotency rules are owned and enforced entirely by forge-service (Functional Spec `product-specs/POC-HIGH-POWER-APPROVAL.md`); probe-service's `status`/`approvalStatus` fields are opaque pass-through values.

## Persistence-sensitive rules

Not applicable. probe-service persists nothing between requests; every call is independently mapped, forwarded, and translated.
