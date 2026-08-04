# Tasks: Quest Loadout Fulfilment (probe-service)

**Input**: Design documents from `/specs/002-probe-quest-loadouts/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/quest-loadouts-probe-rest.md, quickstart.md

**Tests**: Included — `plan.md`'s Testing Strategy explicitly names one test
class per layer (`ProbeLoadoutControllerTest`, `ProbeLoadoutServiceTest`,
`RestLoadoutClientTest`, `GrpcLoadoutClientTest`, `ProbeLoadoutMapperTest`),
mirroring the existing single-artifact test suite. Each of these files grows
incrementally across user-story phases below (new test methods added per
story), matching how the corresponding production classes grow.

**Organization**: Tasks are grouped by user story (spec.md priorities:
US1=P1, US2=P1, US3=P2, US4=P2, US5=P2) to enable independent implementation
and testing of each story, per PD-001 (`probe-service` as a stateless
pass-through facade over `forge-service`'s already-fixed
`/api/v1/loadouts/*` REST and `LoadoutService` gRPC contract).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unmet dependencies)
- **[Story]**: Maps task to US1–US5 from spec.md
- File paths are relative to the repository root

## Path Conventions

Single Java Spring Boot project (existing `probe-service` layout,
`AGENTS.md` package structure): `src/main/java/com/codeistari/probe/`,
`src/test/java/com/codeistari/probe/`, `proto/`.

---

## Phase 1: Setup

**Purpose**: Bring in the downstream gRPC contract this feature depends on.

- [X] T001 Copy `forge-service`'s `quest_loadout_service.proto` (from
      `Jenish16/forge-service#4`,
      `specs/002-quest-loadout-fulfilment/contracts/quest_loadout_service.proto`)
      verbatim into `proto/quest_loadout_service.proto` (research.md D5)
- [X] T002 Run `./gradlew compileJava` and confirm the `com.google.protobuf`
      Gradle plugin (already configured with `srcDir("proto")` in
      `build.gradle.kts`) generates the `LoadoutServiceGrpc` client stub and
      message types with no build configuration changes needed

**Checkpoint**: `LoadoutServiceGrpc` and generated message types compile and
are available to the mapper/client layer.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared contracts and DTOs used by every user story (create,
get, cancel, and retry all return the same `ProbeLoadoutResponse` shape;
every client implementation goes through one shared interface).

**⚠️ CRITICAL**: Must complete before any user story phase begins.

- [X] T003 Create `client/LoadoutClient.java` interface declaring
      `createLoadout`, `getLoadout`, `cancelLoadout`, `retryLoadout`, and
      `getItemAttempts` signatures (mirrors `client/ForgeJobClient.java`,
      contracts/quest-loadouts-probe-rest.md)
- [X] T004 [P] Create `dto/response/ProbeLoadoutResponse.java` and
      `dto/response/ProbeLoadoutItemResponse.java` per data-model.md (opaque
      `String` status fields, research.md D3; `transport` field reuses
      `domain.ForgeTransport`)
- [X] T005 [P] Create
      `dto/client/forge/response/ForgeLoadoutRestResponse.java` and
      `dto/client/forge/response/ForgeLoadoutItemRestResponse.java` mirroring
      `forge-service`'s REST response shape (data-model.md, kept separate
      from the public DTOs per `AGENTS.md`)
- [X] T006 Create `mapper/ProbeLoadoutMapper.java` (empty class, `@Component`)
      to hold the per-story mapping methods added below

**Checkpoint**: Foundation ready — user story implementation can begin.

---

## Phase 3: User Story 1 - Create a quest loadout (Priority: P1) 🎯 MVP

**Goal**: A requester submits a loadout of 2–10 items and receives a stable
`loadoutId`, the accepted items, and any per-item rejection reasons, with
the response status always `ACCEPTED` (FR-001–FR-009).

**Independent Test**: `quickstart.md` steps 1–3 — submit a mixed
valid/invalid loadout, confirm accepted/rejected item reporting; resubmit
the same `requestReference`/content and confirm idempotent replay (`200`,
same `loadoutId`); resubmit with different content and confirm `409`.

### Implementation for User Story 1

- [X] T007 [P] [US1] Create `dto/request/CreateProbeLoadoutRequest.java`
      (`@NotBlank` on `requestReference`/`loadoutName`/`requestedBy`,
      `@NotNull @NotEmpty @Valid` on `items` — no `@Size` bound) and
      `dto/request/ProbeLoadoutItemRequest.java` (raw, unvalidated
      `String artifactType`/`material`, unconstrained `powerLevel` — no
      `@Min`/`@Max`/`@NotNull`) per data-model.md (research.md D7, revised
      by Gate 2 IC-001 — mirrors `forge-service`'s `LoadoutItemRequest`
      exactly so invalid items reach `forge-service`'s per-item rejection
      reporting instead of failing the whole request at `probe-service`)
- [X] T008 [P] [US1] Create
      `dto/client/forge/request/ForgeCreateLoadoutRestRequest.java` and
      `dto/client/forge/request/ForgeLoadoutItemRestRequest.java` mirroring
      `forge-service`'s `POST /api/v1/loadouts` request shape
- [X] T009 [P] [US1] Create `dto/response/ProbeRejectedLoadoutItemResponse.java`
      and `dto/client/forge/response/ForgeRejectedLoadoutItemRestResponse.java`
      per data-model.md
- [X] T010 [US1] Implement `createLoadout` in `client/rest/RestLoadoutClient.java`
      (extends `BaseApiClient`, implements `LoadoutClient`; `POST
      /api/v1/loadouts`; `@CircuitBreaker(name = "FORGE_REST")` /
      `@Retry(name = "FORGE_REST")`, reusing the existing instances per
      research.md D4) — depends on T003, T007, T008
- [X] T011 [US1] Implement `createLoadout` in `client/grpc/GrpcLoadoutClient.java`
      (`LoadoutServiceGrpc` stub; `@CircuitBreaker(name = "FORGE_GRPC")` /
      `@Retry(name = "FORGE_GRPC")`) and its own gRPC-status-mapping method
      (mirrors `GrpcForgeJobClient.mapGrpcException`'s cases, plus
      `ALREADY_EXISTS → HttpStatus.CONFLICT`, research.md D11/Gate 2 IC-002)
      — depends on T002, T003, T007
- [X] T012 [US1] Add `toForgeCreateLoadoutRestRequest`,
      `toCreateLoadoutGrpcRequest`, and `toProbeLoadoutResponse(...)` (REST
      and gRPC overloads, covering `rejectedItems` mapping) to
      `mapper/ProbeLoadoutMapper.java` — depends on T004, T006, T007, T008, T009
- [X] T013 [US1] Create `service/ProbeLoadoutService.java` with
      `createLoadoutViaRest`/`createLoadoutViaGrpc` — depends on T010, T011, T012
- [X] T014 [US1] Create `controller/ProbeLoadoutController.java` with
      `POST /api/v1/probe-requests/loadouts/{rest,grpc}`
      (`@Valid @RequestBody`, `@ResponseStatus(HttpStatus.CREATED)`) —
      depends on T013
- [X] T015 [P] [US1] Add create-flow test cases to
      `src/test/java/com/codeistari/probe/mapper/ProbeLoadoutMapperTest.java`
      (rest + gRPC request/response mapping, `rejectedItems`) — depends on T012
- [X] T016 [P] [US1] Add create-flow test cases to
      `src/test/java/com/codeistari/probe/client/rest/RestLoadoutClientTest.java`
      (`201`, idempotent `200`, `400`, `409` forwarding) — depends on T010
- [X] T017 [P] [US1] Add create-flow test cases to
      `src/test/java/com/codeistari/probe/client/grpc/GrpcLoadoutClientTest.java`,
      including a case asserting `ALREADY_EXISTS` maps to `409 Conflict`
      (research.md D11/Gate 2 IC-002) — depends on T011
- [X] T018 [P] [US1] Add create-flow test cases to
      `src/test/java/com/codeistari/probe/service/ProbeLoadoutServiceTest.java` — depends on T013
- [X] T019 [P] [US1] Add create-flow test cases to
      `src/test/java/com/codeistari/probe/controller/ProbeLoadoutControllerTest.java`,
      including a case asserting a mixed valid/invalid-item request (e.g. an
      out-of-range `powerLevel`) is forwarded to `forge-service` and comes
      back as an accepted item plus a `rejectedItems` entry — **not** a
      whole-request `400` at `probe-service` (research.md D7 revised, Gate 2
      IC-001; spec.md User Story 1 Acceptance Scenario 2) — depends on T014

**Checkpoint**: User Story 1 fully functional and independently testable
(quickstart.md steps 1–3).

---

## Phase 4: User Story 2 - Track loadout and item status (Priority: P1)

**Goal**: A requester retrieves a loadout by `loadoutId` to see its overall
status and every item's status (FR-010, FR-011).

**Independent Test**: `quickstart.md` step 4 — create a loadout, retrieve it
mid-processing and after completion, and confirm the derived status
precedence (FR-011).

### Implementation for User Story 2

- [X] T020 [US2] Implement `getLoadout` in `client/rest/RestLoadoutClient.java`
      (`GET /api/v1/loadouts/{loadoutId}`, same resilience instances as
      T010) — depends on T010
- [X] T021 [US2] Implement `getLoadout` in `client/grpc/GrpcLoadoutClient.java` — depends on T011
- [X] T022 [US2] Add `getLoadoutViaRest`/`getLoadoutViaGrpc` to
      `service/ProbeLoadoutService.java` (reuses
      `toProbeLoadoutResponse(...)` from T012 — no new mapper methods
      needed since the response shape is identical to create's) — depends on T013, T020, T021
- [X] T023 [US2] Add `GET /api/v1/probe-requests/loadouts/{loadoutId}/{rest,grpc}`
      to `controller/ProbeLoadoutController.java` — depends on T014, T022
- [X] T024 [P] [US2] Add retrieval test cases to `RestLoadoutClientTest.java`
      (`200`, `404`) — depends on T020
- [X] T025 [P] [US2] Add retrieval test cases to `GrpcLoadoutClientTest.java` — depends on T021
- [X] T026 [P] [US2] Add retrieval test cases to `ProbeLoadoutServiceTest.java` — depends on T022
- [X] T027 [P] [US2] Add retrieval test cases to `ProbeLoadoutControllerTest.java`
      (status precedence scenarios from spec.md User Story 2) — depends on T023

**Checkpoint**: User Stories 1 and 2 both independently functional — this
is the feature's true MVP (both P1).

---

## Phase 5: User Story 3 - Investigate item failure history (Priority: P2)

**Goal**: A support operator retrieves an item's full attempt history and
latest failure reason (FR-012).

**Independent Test**: `quickstart.md` step 5 — cause an item to fail,
retrieve its attempt history, confirm the failure reason is visible.

### Implementation for User Story 3

- [X] T028 [P] [US3] Create
      `dto/response/ProbeLoadoutAttemptHistoryResponse.java` and
      `dto/response/ProbeLoadoutAttemptResponse.java` per data-model.md
- [X] T029 [P] [US3] Create
      `dto/client/forge/response/ForgeLoadoutAttemptRestResponse.java`
      mirroring `forge-service`'s attempt-history response shape
- [X] T030 [US3] Implement `getItemAttempts` in
      `client/rest/RestLoadoutClient.java` (`GET
      /api/v1/loadouts/{loadoutId}/items/{loadoutItemId}/attempts`) —
      depends on T003, T028, T029
- [X] T031 [US3] Implement `getItemAttempts` in
      `client/grpc/GrpcLoadoutClient.java` — depends on T003, T028
- [X] T032 [US3] Add attempt-history mapping methods to
      `mapper/ProbeLoadoutMapper.java` — depends on T006, T028, T029
- [X] T033 [US3] Add `getItemAttemptsViaRest`/`getItemAttemptsViaGrpc` to
      `service/ProbeLoadoutService.java` — depends on T030, T031, T032
- [X] T034 [US3] Add `GET /api/v1/probe-requests/loadouts/{loadoutId}/items/{loadoutItemId}/attempts/{rest,grpc}`
      to `controller/ProbeLoadoutController.java` — depends on T033
- [X] T035 [P] [US3] Add attempt-history test cases to
      `ProbeLoadoutMapperTest.java` — depends on T032
- [X] T036 [P] [US3] Add attempt-history test cases to
      `RestLoadoutClientTest.java` (ordering oldest → newest, `404`) —
      depends on T030
- [X] T037 [P] [US3] Add attempt-history test cases to
      `GrpcLoadoutClientTest.java` — depends on T031
- [X] T038 [P] [US3] Add attempt-history test cases to
      `ProbeLoadoutServiceTest.java` and `ProbeLoadoutControllerTest.java`
      (both failed-then-retried-successfully scenarios from spec.md User
      Story 3) — depends on T033, T034

**Checkpoint**: User Story 3 independently functional (requires a loadout
from US1 to exist, but adds no dependency on US2/US4/US5 code).

---

## Phase 6: User Story 4 - Cancel outstanding loadout work (Priority: P2)

**Goal**: A requester cancels all outstanding (non-terminal) items in a
loadout while completed/failed results remain visible (FR-013–FR-015).

**Independent Test**: `quickstart.md` step 7 — cancel a loadout with a mix
of completed and active items, confirm active items stop and their status
becomes `CANCELLED`, confirm repeated cancellation is a no-op (FR-014).

### Implementation for User Story 4

- [X] T039 [US4] Implement `cancelLoadout` in
      `client/rest/RestLoadoutClient.java` (`POST
      /api/v1/loadouts/{loadoutId}/cancel`, no request body; reuses
      `ForgeLoadoutRestResponse` from T005) — depends on T003, T005
- [X] T040 [US4] Implement `cancelLoadout` in
      `client/grpc/GrpcLoadoutClient.java` — depends on T003
- [X] T041 [US4] Add `cancelLoadoutViaRest`/`cancelLoadoutViaGrpc` to
      `service/ProbeLoadoutService.java` (reuses `toProbeLoadoutResponse(...)`
      from T012 — response shape identical to get) — depends on T013, T039, T040
- [X] T042 [US4] Add `POST /api/v1/probe-requests/loadouts/{loadoutId}/cancel/{rest,grpc}`
      to `controller/ProbeLoadoutController.java` — depends on T014, T041
- [X] T043 [P] [US4] Add cancellation test cases to `RestLoadoutClientTest.java`
      (`200` including no-op, `404`) — depends on T039
- [X] T044 [P] [US4] Add cancellation test cases to `GrpcLoadoutClientTest.java` — depends on T040
- [X] T045 [P] [US4] Add cancellation test cases to `ProbeLoadoutServiceTest.java`
      and `ProbeLoadoutControllerTest.java` (all four spec.md User Story 4
      acceptance scenarios, including terminal-cancelled-item behavior
      FR-015) — depends on T041, T042

**Checkpoint**: User Story 4 independently functional.

---

## Phase 7: User Story 5 - Retry failed loadout items (Priority: P2)

**Goal**: A requester retries every currently-failed item in a loadout,
optionally with corrected content, without affecting succeeded items
(FR-016–FR-018).

**Independent Test**: `quickstart.md` step 6 — retry a loadout with a
failed item, confirm exactly one new attempt is created and the item's
history retains the prior failed attempt (via US3's attempt-history
endpoint).

### Implementation for User Story 5

- [X] T046 [P] [US5] Create `dto/request/RetryProbeLoadoutRequest.java` and
      `dto/request/RetryProbeLoadoutItemRequest.java` per data-model.md
      (`loadoutItemId` required via `@NotBlank`; `artifactName`/
      `artifactType`/`material`/`powerLevel` are raw, optional, unvalidated
      fields — no typed enums or bounds, research.md D7 revised/Gate 2
      IC-001 — mirrors `forge-service`'s `RetryLoadoutItemRequest` exactly
      so an invalid correction value reaches `forge-service`'s own error
      handling instead of failing at `probe-service`)
- [X] T047 [P] [US5] Create
      `dto/client/forge/request/ForgeRetryLoadoutRestRequest.java` and
      `dto/client/forge/request/ForgeRetryLoadoutItemRestRequest.java`
      mirroring `forge-service`'s retry request shape
- [X] T048 [US5] Implement `retryLoadout` in
      `client/rest/RestLoadoutClient.java` (`POST
      /api/v1/loadouts/{loadoutId}/retry`) — depends on T003, T046, T047
- [X] T049 [US5] Implement `retryLoadout` in
      `client/grpc/GrpcLoadoutClient.java` (reuses the gRPC-status-mapping
      method added in T011) — depends on T003, T011, T046
- [X] T050 [US5] Add `toForgeRetryLoadoutRestRequest`/
      `toRetryLoadoutGrpcRequest` to `mapper/ProbeLoadoutMapper.java` —
      depends on T006, T046, T047
- [X] T051 [US5] Add `retryLoadoutViaRest`/`retryLoadoutViaGrpc` to
      `service/ProbeLoadoutService.java` — depends on T013, T048, T049, T050
- [X] T052 [US5] Add `POST /api/v1/probe-requests/loadouts/{loadoutId}/retry/{rest,grpc}`
      to `controller/ProbeLoadoutController.java` (`@Valid @RequestBody`) —
      depends on T014, T051
- [X] T053 [P] [US5] Add retry test cases to `ProbeLoadoutMapperTest.java` — depends on T050
- [X] T054 [P] [US5] Add retry test cases to `RestLoadoutClientTest.java`
      (`200`, `400` for incomplete/ineligible items, `404`) — depends on T048
- [X] T055 [P] [US5] Add retry test cases to `GrpcLoadoutClientTest.java` — depends on T049
- [X] T056 [P] [US5] Add retry test cases to `ProbeLoadoutServiceTest.java`
      and `ProbeLoadoutControllerTest.java` (all three spec.md User Story 5
      acceptance scenarios, including no-duplicate-attempt FR-018) —
      depends on T051, T052

**Checkpoint**: All five user stories independently functional.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and end-to-end validation across all stories.

- [X] T057 Update `docs/architecture.md` to describe the new
      `ProbeLoadoutController`/`Service`/`Client`/`Mapper` layer and its
      pass-through relationship to `forge-service`'s `/api/v1/loadouts/*`
      (per `.cursor/rules/docs-maintenance.mdc`)
- [X] T058 [P] Update `docs/entities-enums.md` to note the new transient
      Loadout DTOs (still "no persisted entities") and the opaque
      status-string decoupling extended to Loadout/item/attempt statuses
      (research.md D3)
- [X] T059 [P] Update `docs/external-services.md` if the `forge-service`
      dependency description needs the new endpoints listed
- [X] T060 Run the full `quickstart.md` walkthrough (steps 1–8) against a
      locally running `forge-service` and confirm every step's expected
      response
- [X] T061 Run `./gradlew test` and confirm all new and existing tests pass,
      including the unchanged single-artifact tests (FR-019 regression
      check)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T002) for the gRPC stub —
  BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational. No dependency on
  other stories — this is the true starting point (create must exist before
  anything else can be tracked, cancelled, or retried).
- **User Story 2 (Phase 4)**: Depends on Foundational and reuses
  `RestLoadoutClient`/`GrpcLoadoutClient`/`ProbeLoadoutService`/
  `ProbeLoadoutController` classes created in US1 (T010, T011, T013, T014)
  — cannot start until those classes exist, though it adds no new DTOs.
- **User Story 3 (Phase 5)**: Depends on Foundational and the same shared
  classes from US1; independent of US2/US4/US5.
- **User Story 4 (Phase 6)**: Depends on Foundational and the same shared
  classes from US1; independent of US2/US3/US5.
- **User Story 5 (Phase 7)**: Depends on Foundational and the same shared
  classes from US1; independent of US2/US3/US4.
- **Polish (Phase 8)**: Depends on all desired user stories being complete.

### Note on shared-file dependencies

Because `probe-service` mirrors the existing single-artifact flow's
one-class-per-concern layout, `RestLoadoutClient.java`,
`GrpcLoadoutClient.java`, `ProbeLoadoutService.java`,
`ProbeLoadoutController.java`, and `ProbeLoadoutMapper.java` are each edited
by multiple stories (one new method per story, added to the same file).
Within a single story's phase, edits to these shared files must be done
sequentially (not marked `[P]`); tasks in *different* files within the same
story (e.g., a new DTO alongside a client method) remain parallelizable.

### Parallel Opportunities

- T004 and T005 (Foundational DTOs) can run in parallel.
- Within each user story phase, the `[P]`-marked DTO-creation and
  same-layer test tasks can run in parallel with each other, but not with
  the shared-file implementation tasks in that same story.
- T058 and T059 (Polish docs) can run in parallel.

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 — both P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (create)
4. Complete Phase 4: User Story 2 (track) — creation without tracking has
   little standalone value, so both P1 stories together form the MVP
5. **STOP and VALIDATE**: Run `quickstart.md` steps 1–4 independently
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 + US2 (both P1) → MVP → validate → deploy/demo
3. US3 (attempt history, P2) → validate → deploy/demo
4. US4 (cancel, P2) → validate → deploy/demo
5. US5 (retry, P2) → validate → deploy/demo
6. Polish (Phase 8) → final validation across all stories

US3, US4, and US5 have no dependencies on each other and may be built in
any order (or in parallel by different developers) once US1's shared
classes exist.
