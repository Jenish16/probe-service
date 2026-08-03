# Functional Spec: Quest Loadout Fulfilment

## Metadata

| Field | Value |
|---|---|
| Initiative ID | `POC-QUEST-LOADOUTS` |
| Product area | Artifact request experience |
| Known participating systems | Probe and Forge |
| Status | Functional approved |

## Background

Quest leaders currently request artifacts one at a time. Preparing a quest
loadout—such as a shield, blade, ring, and scroll—requires multiple requests,
separate tracking, and manual recovery when only some artifacts fail.

The product needs a single loadout experience without exposing the operational
complexity of individual forge jobs to the requester.

## Product outcome

A requester can submit, monitor, cancel, and recover a quest loadout containing
multiple artifacts. The requester receives one loadout reference while still
being able to inspect every item.

## Users

- **Quest leader:** requests and tracks a complete loadout.
- **Support operator:** investigates failed or partially completed loadouts.

## Required user operations

The product must support operations to:

1. Create a loadout.
2. Retrieve a loadout and its items.
3. Cancel outstanding work for a loadout.
4. Retry failed loadout items.
5. Retrieve the attempt history of an item.

The Technical Spec will define the APIs and participating-service contracts
needed to provide these operations.

## Create loadout

A creation request contains:

- A requester-provided `requestReference` used to prevent accidental duplicate
  submissions.
- A `loadoutName`.
- `requestedBy`.
- Between 2 and 10 artifact items.

Each item contains:

- `artifactName`
- `artifactType`
- `material`
- `powerLevel`

The response provides:

- A stable `loadoutId`.
- The accepted items.
- Any items rejected during validation.
- The current loadout status.

## Loadout lifecycle

| Status | Meaning |
|---|---|
| `ACCEPTED` | The loadout was accepted and item processing can begin |
| `IN_PROGRESS` | At least one accepted item is still being processed |
| `READY` | Every accepted item completed successfully |
| `PARTIALLY_READY` | Some accepted items completed and some failed |
| `FAILED` | No accepted item completed successfully |
| `CANCELLED` | All outstanding item work was cancelled |

## Functional requirements

### Creation and validation

1. A loadout shall contain between 2 and 10 items.
2. Every item shall follow the existing artifact-name, type, material, and
   power-level rules.
3. Invalid items shall be reported individually.
4. Valid items shall continue even when another item is invalid.
5. At least one valid item is required to create a loadout.
6. Repeating the same `requestReference` with the same request shall return the
   existing loadout.
7. Reusing a `requestReference` with different content shall be rejected.
8. Item ordering shall match the original request.

### Tracking

9. Each accepted item shall have a stable `loadoutItemId`.
10. The requester shall see the current status of every item.
11. The requester shall not need to know internal service-specific identifiers
    to manage the loadout.
12. Loadout status shall reflect the latest visible item states.
13. A support operator shall be able to view the attempt history and latest
    failure reason for each item.

### Cancellation

14. A requester may cancel all items that have not reached a terminal state.
15. Completed and failed items shall retain their results.
16. Repeated cancellation shall be safe and shall not create additional work.
17. A cancelled item shall not restart unless the requester explicitly retries
    it where retry is permitted.

### Retry

18. A requester may retry failed items after correcting any invalid input.
19. Successful items shall not be repeated.
20. Each retry shall create a new attempt under the same `loadoutItemId`.
21. Attempt history shall retain previous outcomes.
22. Repeated retry requests shall not create duplicate attempts.

## Acceptance scenarios

### Complete loadout

- **Given** a valid four-item loadout
- **When** every item completes
- **Then** the loadout becomes `READY`
- **And** the requester can see all four completed items

### Partially valid submission

- **Given** a loadout with three valid items and one invalid item
- **When** it is submitted
- **Then** the three valid items continue
- **And** the invalid item is reported separately

### Partial processing failure

- **Given** a loadout with completed and failed items
- **When** the requester retrieves it
- **Then** its status is `PARTIALLY_READY`
- **And** each failed item includes a failure reason

### Retry failed item

- **Given** a partially ready loadout
- **When** the requester retries its failed items
- **Then** successful items are unchanged
- **And** each retried item receives one new attempt

### Cancel outstanding work

- **Given** a loadout containing completed and active items
- **When** the requester cancels it
- **Then** completed results remain available
- **And** outstanding work is cancelled

## Compatibility expectations

- Existing single-artifact request and retrieval behavior must continue.
- The loadout experience must work consistently regardless of whether the
  underlying integration uses REST or gRPC.
- Existing callers that do not use loadouts are unaffected.

## Success measures

- One requester operation can create a loadout of up to 10 artifacts.
- Duplicate requester submissions do not create duplicate loadouts.
- Partial success, cancellation, and retry outcomes are visible without
  support teams correlating internal job IDs manually.

## Out of scope

- Capacity scheduling
- Material inventory management
- Pricing or quota enforcement
- Notifications
- Persistent storage requirements
- Choice of orchestration owner or technical implementation
