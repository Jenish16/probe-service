# Feature Specification: Quest Loadout Fulfilment

**Feature Branch**: `quest-loadouts`

**Created**: 2026-08-03

**Status**: Draft

**Initiative ID**: `POC-QUEST-LOADOUTS`

**Input**: User description: "Read product-specs/POC-QUEST-LOADOUTS.md as the approved Functional Spec. Create a framework-native functional specification that preserves the complete end-to-end product behaviour. Keep participating-service responsibilities and orchestration ownership unresolved because they will be decided during Technical Design."

## Background

Quest leaders currently request artifacts one at a time. Preparing a quest
loadout — such as a shield, blade, ring, and scroll — requires multiple
separate requests, separate tracking, and manual recovery when only some
artifacts fail. This feature introduces a single loadout experience that
hides the operational complexity of individual artifact fulfilment from the
requester.

**Note on scope of this specification**: This specification describes
end-to-end product behavior only. It intentionally does **not** assign
responsibilities to any participating system, name an orchestration owner,
or make any technical design, API, or storage decisions — those are Technical
Spec concerns to be resolved during planning.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a quest loadout (Priority: P1)

A quest leader submits a single request describing a named loadout and a
list of 2 to 10 artifact items. Instead of tracking each artifact
individually, the leader receives one stable loadout reference that
represents the whole submission, plus visibility into which items were
accepted and which were rejected for invalid input.

**Why this priority**: This is the entry point for the entire feature. Without
the ability to create a loadout, no other capability (tracking, cancellation,
retry, history) has any value.

**Independent Test**: Can be fully tested by submitting a creation request
with a mix of valid and invalid items and verifying the response contains a
stable loadout reference, the list of accepted items, the list of rejected
items with reasons, and an initial loadout status — without needing any other
capability to be built.

**Acceptance Scenarios**:

1. **Given** a valid four-item loadout submission, **When** it is submitted,
   **Then** the requester receives a stable loadout reference, all four items
   are accepted, item order matches the submitted order, and the loadout
   status reflects that processing can begin.
2. **Given** a loadout submission with three valid items and one invalid
   item, **When** it is submitted, **Then** the three valid items are
   accepted and continue, and the invalid item is reported individually with
   a validation reason, without blocking the valid items.
3. **Given** a loadout submission where every item is invalid, **When** it is
   submitted, **Then** the loadout is not created and every item's rejection
   reason is reported.
4. **Given** a previously accepted loadout submission, **When** the same
   `requestReference` is resubmitted with identical content, **Then** the
   existing loadout is returned rather than creating a duplicate.
5. **Given** a previously used `requestReference`, **When** it is resubmitted
   with different content, **Then** the resubmission is rejected.

---

### User Story 2 - Track loadout and item status (Priority: P1)

A quest leader retrieves a loadout at any time to see its overall status and
the status of every individual item, without needing to know or supply any
internal service-specific identifiers.

**Why this priority**: Visibility into progress and outcome is the core value
proposition — it replaces the leader's need to manually track multiple
separate artifact requests.

**Independent Test**: Can be fully tested by creating a loadout, allowing
items to reach a mix of outcomes, and retrieving the loadout to verify the
overall status and every item's individual status are visible using only the
loadout reference and item references returned at creation time.

**Acceptance Scenarios**:

1. **Given** a loadout whose items are still being processed, **When** the
   requester retrieves it, **Then** the loadout status shows that processing
   is still under way.
2. **Given** a loadout where every accepted item has completed successfully,
   **When** the requester retrieves it, **Then** the loadout status shows
   full completion and all completed items are visible.
3. **Given** a loadout with a mix of completed and failed items, **When** the
   requester retrieves it, **Then** the loadout status reflects partial
   completion and each failed item is visible with a status distinguishing
   it from completed items.
4. **Given** a loadout where no accepted item completed successfully,
   **When** the requester retrieves it, **Then** the loadout status reflects
   that no item succeeded.

---

### User Story 3 - Investigate item failure history (Priority: P2)

A support operator investigating a failed or partially completed loadout
retrieves the attempt history of a specific item to see every attempt made
and the latest failure reason.

**Why this priority**: Support investigation is essential for operational
trust in the feature but depends on loadouts already existing and having
experienced at least one failure or retry, making it secondary to creation
and tracking.

**Independent Test**: Can be fully tested by creating a loadout, causing an
item to fail, retrieving that item's attempt history, and verifying every
attempt and the latest failure reason are visible.

**Acceptance Scenarios**:

1. **Given** an item that has failed once, **When** the attempt history is
   retrieved, **Then** one attempt is visible with its failure reason.
2. **Given** an item that failed and was later retried successfully, **When**
   the attempt history is retrieved, **Then** both the failed attempt and the
   successful attempt are visible in order, and the earlier failure reason is
   retained.

---

### User Story 4 - Cancel outstanding loadout work (Priority: P2)

A quest leader cancels a loadout to stop all outstanding item work while
keeping the results of items that already completed or failed.

**Why this priority**: Cancellation protects requesters from waiting on
loadouts they no longer need, but it is only meaningful once loadouts can be
created and tracked.

**Independent Test**: Can be fully tested by creating a loadout with a mix of
completed and still-active items, cancelling it, and verifying completed
results remain visible while active items stop and do not later complete.

**Acceptance Scenarios**:

1. **Given** a loadout containing completed and still-active items, **When**
   the requester cancels it, **Then** the completed items retain their
   results and the active items' outstanding work is cancelled.
2. **Given** a loadout that has already been cancelled, **When** the
   requester cancels it again, **Then** the repeated cancellation has no
   additional effect and does not create new work.
3. **Given** a cancelled item, **When** no further action is taken, **Then**
   the item does not restart on its own.

---

### User Story 5 - Retry failed loadout items (Priority: P2)

A quest leader retries the failed items of a loadout — for example after
correcting invalid input — without affecting items that already succeeded.

**Why this priority**: Retry closes the loop on partial failure, letting a
requester recover a loadout to full completion, but it depends on loadouts
existing and having failed items.

**Independent Test**: Can be fully tested by creating a loadout with a mix of
successful and failed items, retrying it, and verifying only the failed items
receive new attempts while successful items are unchanged.

**Acceptance Scenarios**:

1. **Given** a loadout with successful and failed items, **When** the
   requester retries it, **Then** each failed item receives exactly one new
   attempt under its existing item reference, and successful items are left
   unchanged.
2. **Given** a loadout retry that has already been requested and is still
   being processed, **When** the same retry is requested again, **Then** no
   duplicate attempt is created.
3. **Given** an item with a prior failed attempt that is retried
   successfully, **When** the item's attempt history is later retrieved,
   **Then** the previous failed attempt is still visible alongside the new
   successful attempt.

---

### Edge Cases

- What happens when a creation request contains fewer than 2 items or more
  than 10 items? The request must be rejected rather than partially
  accepted.
- What happens when a creation request contains zero valid items (all
  invalid)? No loadout is created.
- What happens when a `requestReference` is reused with content that differs
  only slightly (e.g., item order, whitespace, or a changed field value) from
  the original request? The resubmission must be treated as different content
  and rejected, not silently accepted as identical.
- What happens when a requester retries a loadout that has no failed items?
  The retry has no effect since there is nothing eligible to retry.
- What happens when a requester retries an item that was cancelled rather
  than failed? Retry is only permitted for items in a state where retry is
  explicitly allowed; cancelled items follow the same restriction described
  in User Story 4.
- What happens when a requester cancels a loadout that is already fully
  `READY`, `PARTIALLY_READY`, or `FAILED` (i.e., no outstanding work
  remains)? The cancellation is safe and has no effect since there is nothing
  outstanding to cancel.
- What happens when two identical creation requests (same `requestReference`
  and same content) are submitted concurrently? Only one loadout is created;
  both callers see the same loadout reference.
- What happens when a support operator requests the attempt history of an
  item that has never failed? The history shows only successful attempt(s)
  and no failure reason.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The product MUST allow a requester to submit a single loadout
  creation request containing a `loadoutName`, a `requestedBy` value, a
  `requestReference`, and between 2 and 10 artifact items, where each item
  specifies an `artifactName`, `artifactType`, `material`, and `powerLevel`.
- **FR-002**: The product MUST validate every item in a creation request
  against the existing artifact-name, type, material, and power-level rules,
  and MUST report invalid items individually rather than rejecting the whole
  request outright.
- **FR-003**: The product MUST continue processing valid items in a creation
  request even when one or more other items in the same request are invalid.
- **FR-004**: The product MUST reject a creation request outright when it
  contains zero valid items.
- **FR-005**: The product MUST preserve the original item order from the
  request when returning accepted items.
- **FR-006**: The product MUST return, in response to a creation request, a
  stable `loadoutId`, the accepted items, any items rejected during
  validation with their rejection reasons, and the current loadout status.
- **FR-007**: The product MUST assign a stable `loadoutItemId` to every
  accepted item, usable by the requester without needing any internal
  service-specific identifiers.
- **FR-008**: The product MUST return the existing loadout, rather than
  creating a new one, when a creation request repeats a previously used
  `requestReference` with identical content.
- **FR-009**: The product MUST reject a creation request when it reuses a
  previously used `requestReference` with content that differs from the
  original request.
- **FR-010**: The product MUST allow a requester to retrieve a loadout by its
  `loadoutId`, including the current status of the loadout and of every one
  of its items.
- **FR-011**: The product MUST derive and expose the overall loadout status
  (`ACCEPTED`, `IN_PROGRESS`, `READY`, `PARTIALLY_READY`, `FAILED`, or
  `CANCELLED`) from the latest visible states of its accepted items.
- **FR-012**: The product MUST allow a support operator to retrieve the full
  attempt history and latest failure reason for any loadout item.
- **FR-013**: The product MUST allow a requester to cancel all outstanding
  (non-terminal) work for a loadout, while leaving already-completed and
  already-failed items' results unchanged.
- **FR-014**: The product MUST make repeated cancellation requests for the
  same loadout safe, such that they have no additional effect and create no
  additional work.
- **FR-015**: The product MUST prevent a cancelled item from restarting on
  its own; it MUST only resume if the requester explicitly retries it and
  retry is permitted for that item.
- **FR-016**: The product MUST allow a requester to retry the failed items of
  a loadout, creating exactly one new attempt per failed item under its
  existing `loadoutItemId`, without repeating items that already succeeded.
- **FR-017**: The product MUST retain the full attempt history for a retried
  item, including all previous outcomes, when a new attempt is created.
- **FR-018**: The product MUST prevent a repeated retry request for the same
  loadout or item from creating duplicate attempts.
- **FR-019**: The product MUST continue to support existing single-artifact
  request and retrieval behavior unchanged for callers that do not use
  loadouts.
- **FR-020**: The product MUST provide the loadout experience (creation,
  retrieval, cancellation, retry, attempt history) with consistent behavior
  regardless of whether the underlying integration uses REST or gRPC.

*Deferred to Technical Design (explicitly unresolved by this specification)*:

- Which participating system(s) own creation, validation, status
  aggregation, cancellation, and retry responsibilities for a loadout and its
  items.
- Which system, if any, acts as the orchestration owner coordinating loadout
  item work across participating systems.
- API shapes, request/response payloads, storage/persistence design, and any
  other technical implementation detail.

### Key Entities *(include if feature involves data)*

- **Loadout**: Represents a single requester submission bundling multiple
  artifact items under one reference. Key attributes: stable `loadoutId`,
  `loadoutName`, `requestedBy`, `requestReference`, and an overall status
  derived from its items' states.
- **Loadout Item**: Represents one artifact within a loadout. Key attributes:
  stable `loadoutItemId`, `artifactName`, `artifactType`, `material`,
  `powerLevel`, current item status, and a reference to its attempt history.
- **Attempt**: Represents one processing attempt made for a loadout item.
  Key attributes: an outcome (success or failure), a failure reason when
  applicable, and its position in the item's attempt history. Multiple
  attempts may exist for the same loadout item over time (e.g., due to
  retry).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A requester can create a loadout of up to 10 artifacts through
  a single operation, rather than 10 separate individual requests.
- **SC-002**: Duplicate submissions using the same `requestReference` and
  content never result in more than one loadout being created.
- **SC-003**: A requester or support operator can determine a loadout's
  overall outcome — full success, partial success, full failure, or
  cancellation — and each item's individual outcome, using only loadout- and
  item-level references, without correlating internal service-specific job
  identifiers.
- **SC-004**: Every failed item retains a visible, retrievable failure reason
  and full attempt history after cancellation, retry, or repeated retrieval.
- **SC-005**: Existing single-artifact request and retrieval behavior for
  callers that do not use loadouts is unaffected by this feature.

## Assumptions

- The artifact validation rules (name, type, material, power-level) referenced
  by loadout item validation are the same rules already governing
  single-artifact requests today; this specification does not redefine them.
- "Terminal state" for a loadout item means any state from which the item
  will not change without an explicit requester action (e.g., completed,
  failed-and-not-yet-retried, or cancelled).
- Retry is only meaningful for items that failed; items that completed
  successfully are never retried, and cancelled items follow whatever retry
  eligibility rule applies to their state at cancellation time.
- Capacity scheduling, material inventory management, pricing/quota
  enforcement, notifications, and persistent storage design are out of scope
  for this specification, consistent with the approved Functional Spec.
- Which participating system(s) implement each responsibility, and who owns
  orchestration across them, are intentionally left undecided here and are
  Technical Design decisions.
