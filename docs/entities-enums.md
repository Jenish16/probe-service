# Entities & Enums

## Entities

None. `probe-service` holds no persisted domain entities — a scan of `domain/`, the (absent) `repository/` package, and `resources/` confirms there is no database, cache, or in-memory store of its own. All request/response types (`CreateProbeForgeJobRequest`, `ProbeForgeJobResponse`, `ForgeCreateJobRestRequest`, `ForgeJobRestResponse`) are transient DTOs, not entities — the service is a stateless caller.

## Enums

| Enum | Values | Source | Notes |
|---|---|---|---|
| `ArtifactType` | `RING`, `BLADE`, `STAFF`, `AMULET`, `SHIELD`, `SCROLL` | `domain/ArtifactType.java` | Duplicated from forge-service's enum of the same name (separate repos, no shared library) — must be kept in sync manually if forge-service's values change. |
| `ForgeMaterial` | `MITHRIL`, `ELVEN_STEEL`, `DWARVEN_IRON`, `OBSIDIAN`, `SILVERWOOD` | `domain/ForgeMaterial.java` | Same duplication note as `ArtifactType`. |
| `ForgeTransport` | `REST`, `GRPC` | `domain/ForgeTransport.java` | probe-service-specific; tags `ProbeForgeJobResponse` with which downstream transport served the request. |

Note: unlike forge-service, `probe-service` does not define its own `ForgeJobStatus` enum. `ProbeForgeJobResponse.status` is a raw `String`, passed through opaquely from whatever forge-service returns (`ForgeJobRestResponse.status` / gRPC `status` field are also `String`). This is a deliberate (if implicit) decoupling — probe-service does not need to know forge-service's status values to relay them.

## State transitions

None — probe-service does not own or mutate any state machine; `status` is an opaque pass-through value from forge-service.

## Persistence-sensitive rules

Not applicable. probe-service persists nothing between requests; every call is independently mapped, forwarded, and translated.
