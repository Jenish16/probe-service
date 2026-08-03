# REST Contract: Quest Loadout Fulfilment (probe-service public API)

New resource, additive only, under `/api/v1/probe-requests/loadouts`. Does
not modify the existing `/api/v1/probe-requests/forge-jobs/{rest,grpc}`
endpoints (FR-019).

Every endpoint below is offered in two transport variants — `.../rest` and
`.../grpc` path suffixes — matching the existing single-artifact pattern
(research.md D2). Both variants accept and return the identical shape
described here; the suffix only selects whether `probe-service` calls
`forge-service` over REST or gRPC (FR-020).

`probe-service` performs no business logic of its own on this data
(research.md D1) — every field, status value, and error below is forwarded
verbatim to/from `forge-service`'s `/api/v1/loadouts/*` REST contract
(`Jenish16/forge-service#4`,
`specs/002-quest-loadout-fulfilment/contracts/quest-loadouts-rest.md`) or its
`LoadoutService` gRPC equivalent. `probe-service` additionally performs only
the structural pre-checks noted per-endpoint below (research.md D7).

## `POST /api/v1/probe-requests/loadouts/{rest,grpc}`

Create a loadout (User Story 1).

**Request body** (`CreateProbeLoadoutRequest`)

```json
{
  "requestReference": "string, required",
  "loadoutName": "string, required",
  "requestedBy": "string, required",
  "items": [
    {
      "artifactName": "string, required",
      "artifactType": "RING | BLADE | STAFF | AMULET | SHIELD | SCROLL",
      "material": "MITHRIL | ELVEN_STEEL | DWARVEN_IRON | OBSIDIAN | SILVERWOOD",
      "powerLevel": "int 1-10, required"
    }
  ]
}
```

`items` MUST contain 2–10 entries (`@Size(min = 2, max = 10)`, FR-001) —
`probe-service` rejects with `400 Bad Request` before calling `forge-service`
if this structural bound is violated. Per-item business-rule validation
(artifact-name/type/material/power-level, FR-002) is `forge-service`'s alone;
`probe-service` forwards every item regardless of its own opinion on
validity.

**Response — `201 Created`** (new loadout) or `200 OK` (identical
`requestReference` resubmission, FR-008) — `ProbeLoadoutResponse`

```json
{
  "loadoutId": "string",
  "loadoutName": "string",
  "requestedBy": "string",
  "status": "ACCEPTED",
  "items": [
    {
      "loadoutItemId": "string",
      "artifactName": "string",
      "artifactType": "string",
      "material": "string",
      "powerLevel": 0,
      "status": "string",
      "approvalStatus": "string, omitted until High-Power Approval ships"
    }
  ],
  "rejectedItems": [
    {
      "artifactName": "string",
      "artifactType": "string",
      "material": "string",
      "powerLevel": 0,
      "reason": "string"
    }
  ],
  "transport": "REST | GRPC"
}
```

`status` is always `ACCEPTED` here (FR-006). `rejectedItems` is omitted/empty
when every submitted item was valid. `transport` tags which downstream
transport served the request (matches the existing `ProbeForgeJobResponse`
convention).

**Error responses** — statuses forwarded verbatim from `forge-service`
(research.md D9) via the existing `ForgeRemoteCallException`/
`GlobalExceptionHandler` pipeline, plus `probe-service`'s own structural
pre-check:

| Condition | Status | Source |
|---|---|---|
| `items` outside 2–10 entries, or any required field blank/missing | `400 Bad Request` | `probe-service` (Bean Validation, fails before calling `forge-service`) |
| Zero valid items after per-item validation | `400 Bad Request` | `forge-service` (forwarded) |
| `requestReference` reused with different content | `409 Conflict` | `forge-service` (forwarded, FR-009) |

## `GET /api/v1/probe-requests/loadouts/{loadoutId}/{rest,grpc}`

Retrieve a loadout and its items' current status (User Story 2).

**Response — `200 OK`** — `ProbeLoadoutResponse`

```json
{
  "loadoutId": "string",
  "loadoutName": "string",
  "requestedBy": "string",
  "status": "IN_PROGRESS | READY | PARTIALLY_READY | FAILED | CANCELLED",
  "items": [
    {
      "loadoutItemId": "string",
      "artifactName": "string",
      "artifactType": "string",
      "material": "string",
      "powerLevel": 0,
      "status": "string",
      "approvalStatus": "string, omitted until High-Power Approval ships",
      "approvalExpiresAt": "ISO-8601 timestamp, present only while approvalStatus == PENDING_APPROVAL",
      "rejectionReason": "string, present only when approvalStatus == REJECTED",
      "failureReason": "string, present only when status == FAILED"
    }
  ],
  "transport": "REST | GRPC"
}
```

`status` here is the derived overall status (FR-011); never `ACCEPTED` (only
the create response returns that).

**Error responses**: `404 Not Found` — unknown `loadoutId` (forwarded from
`forge-service`).

## `POST /api/v1/probe-requests/loadouts/{loadoutId}/cancel/{rest,grpc}`

Cancel all outstanding (non-terminal) items (User Story 4). No request body.

**Response — `200 OK`**: same shape as the `GET` endpoint above, reflecting
the loadout after cancellation.

Always succeeds as a safe no-op when there is nothing outstanding to cancel
(FR-014), including when the loadout is already terminal. Never returns an
error for "nothing to cancel."

**Error responses**: `404 Not Found` — unknown `loadoutId`.

## `POST /api/v1/probe-requests/loadouts/{loadoutId}/retry/{rest,grpc}`

Retry every currently-failed item, optionally with corrected content (User
Story 5).

**Request body** (`RetryProbeLoadoutRequest`)

```json
{
  "items": [
    {
      "loadoutItemId": "string, required",
      "artifactName": "string, optional — omit to keep prior value",
      "artifactType": "string, optional",
      "material": "string, optional",
      "powerLevel": "int, optional"
    }
  ]
}
```

`items` MUST include every currently-failed `loadoutItemId` (FR-016,
canonical clarification #4); `probe-service` performs no eligibility
pre-check of its own — `forge-service` is authoritative and rejects an
incomplete or ineligible body. If the loadout has no failed items, the
request is a safe no-op (canonical clarification #6). If a retried item's
resolved content is identical to its latest attempt, no new attempt is
created for that item (canonical clarification #7).

**Response — `200 OK`**: same shape as the `GET` endpoint above, reflecting
the loadout after the new attempts resolve.

**Error responses**

| Condition | Status | Source |
|---|---|---|
| Body omits a currently-failed `loadoutItemId`, or includes one that is not currently failed (e.g. cancelled, FR-015) | `400 Bad Request` | `forge-service` (forwarded) |
| Unknown `loadoutId` | `404 Not Found` | `forge-service` (forwarded) |

## `GET /api/v1/probe-requests/loadouts/{loadoutId}/items/{loadoutItemId}/attempts/{rest,grpc}`

Retrieve the full attempt history of a single item (support-operator use
case, User Story 3).

**Response — `200 OK`** — `ProbeLoadoutAttemptHistoryResponse`

```json
{
  "loadoutItemId": "string",
  "attempts": [
    {
      "attemptNumber": 1,
      "forgeJobId": "string",
      "requesterReference": "string, omitted until High-Power Approval ships",
      "status": "string",
      "approvalStatus": "string, omitted until High-Power Approval ships",
      "rejectionReason": "string, present only when approvalStatus == REJECTED",
      "failureReason": "string, present only when status == FAILED",
      "createdAt": "ISO-8601 timestamp"
    }
  ],
  "transport": "REST | GRPC"
}
```

`attempts` is ordered oldest → newest (FR-012/FR-017).

**Error responses**: `404 Not Found` — unknown `loadoutId` or
`loadoutItemId` (forwarded from `forge-service`).
