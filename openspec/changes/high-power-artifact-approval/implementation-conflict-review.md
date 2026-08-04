---
schema_version: 2
gate: implementation-conflict
result: dependency
checked_at: "2026-08-03T18:43:00Z"
input_fingerprint: c2d4c8b38f999d15553c2d9ec42bd46d5abcb8a9f56ecd991d1950a9a5504831
source_snapshots:
  - repo: "Jenish16/forge-service"
    pr: "https://github.com/Jenish16/forge-service/pull/5"
    ref: "f99f4b26fa0f769ad37da6c67048e7882e16bdae"
    state: "OPEN (draft)"
    file: "openspec/changes/high-power-artifact-approval/design.md"
    sha256: "f7caddccd1967a4bfc9ae2080f17bd37334220f7d43fe06d93bfe529b11da385"
  - repo: "Jenish16/forge-service"
    pr: "https://github.com/Jenish16/forge-service/pull/5"
    ref: "f99f4b26fa0f769ad37da6c67048e7882e16bdae"
    state: "OPEN (draft)"
    file: "openspec/changes/high-power-artifact-approval/tasks.md"
    sha256: "1af79e615d56654baaf40b8cc30d614c6812c4394701cb539c1391f4db687647"
  - repo: "Jenish16/forge-service"
    pr: "https://github.com/Jenish16/forge-service/pull/5"
    ref: "f99f4b26fa0f769ad37da6c67048e7882e16bdae"
    state: "OPEN (draft)"
    file: "openspec/changes/high-power-artifact-approval/implementation-conflict-review.md"
    sha256: "3f3f93858db17805ff7b4c374a07b9702622a25991a43afdc2278c7fcc401de6"
  - repo: "Jenish16/probe-service"
    ref: "local-working-tree"
    file: "openspec/changes/high-power-artifact-approval/design.md"
    sha256: "4e83a1217c4fa2e14e1a08b65ad91f8f395a91ffae981223018115e364e16227"
  - repo: "Jenish16/probe-service"
    ref: "local-working-tree"
    file: "openspec/changes/high-power-artifact-approval/tasks.md"
    sha256: "aa9d9e217789bd5564b3185dc4e11731e8b12e8a2888d725f917ab316cea5d16"
completion:
  result: passed
  checked_at: "2026-08-04T05:45:27Z"
  merge_commits:
    - "8b0aaa690650a1c085916053a6ea33e8d8c16cc2"
    - "c7fa80dfc1d2b55ed2f7803a020f3f43d30b4cf6"
  mandatory_tasks_complete: true
  tests_passed: true
  documentation_updated: true
  contexts_refreshed: true
  integration_validation: passed
---

# Implementation Conflict and Reuse Review

## Discovery scope

### Local active specifications

- `openspec/changes/high-power-artifact-approval/` (this change) — `status:
  tech-approved`; `proposal`, `specs`, `parallel-design-review`, `design`, and
  `tasks` are `done`. This is the active initiative under review.
- No other active (non-legacy) `openspec/changes/` folder in this repository
  touches `ProbeRequestController`, `ProbeRequestService`, the forge-service
  REST/gRPC client layer, or approval-adjacent DTOs.

### Current-repository open PRs

- None open in `Jenish16/probe-service` at review time (`gh pr list --state
  open` returned `[]`). `probe-service#4` (this change's Technical Spec) is
  merged (`6fb0c51`, `2026-08-03T16:57:15Z`).

### Related-repository open PRs

- `Jenish16/forge-service` (confirmed `related_repositories` entry):
  - `https://github.com/Jenish16/forge-service/pull/5` —
    `[SDD-IMPL][high-power-artifact-approval] Implement High-Power Artifact
    Approval` (draft, open, `implementation-in-progress` per forge-service's
    own `status.yaml`). Same initiative ID. This is forge-service's
    **implementation** PR, already containing a completed Gate 2 review on
    their side (`IC-001`, `IC-002`) reconciled against this repository's
    merged Technical Spec (`probe-service#4`). Fetched and compared in full
    (see Exact resource comparison).
  - `https://github.com/Jenish16/forge-service/pull/6` — a PR for a different
    initiative (`POC-QUEST-LOADOUTS`). Excluded from this review per Gate 2
    scope (only the confirmed `related_repositories` entry's same-initiative
    work is compared).

### Explicit counterpart specifications and PRs

- `status.yaml`'s `counterpart_specs` already records
  `https://github.com/Jenish16/forge-service/pull/3` (forge-service's
  Technical Spec PR, now merged at `6ac12b0`). Its successor implementation
  work is `forge-service#5`, compared below.

### Legacy specifications excluded

- `specs/001-artifact-forging-rest-grpc/` has no `status.yaml` and predates
  Parallel SDD. Treated as `legacy-completed` and excluded from exact
  resource comparison, per Gate 1's prior treatment.

## Exact resource comparison

| Related work | APIs/events/data/components/config/tests | Ownership/reuse/sequencing | Classification |
|---|---|---|---|
| `forge-service#5` — ownership | forge-service implements full approval state ownership (`ApprovalStatus`, `ApprovalDecision`, `InMemoryApprovalDecisionRepository`); probe-service remains a stateless facade | Matches this repo's `design.md` Decision 1 / PD-001 exactly. No conflict. | Reuse (consistent) |
| `forge-service#5` — `POST /api/v1/forge-jobs`, `GET /api/v1/forge-jobs/{forgeJobId}` request/response fields | `CreateForgeJobRequest` gains `requesterReference` (required), `originalRequestId` (optional); `ForgeJobResponse` gains `requesterReference`, `originalRequestId`, `approvalStatus`, `approvalExpiresAt`, `rejectionReason` | Matches this repo's `design.md` D-1/D-2 dependency exactly in substance. **Field-name gap found**: forge-service names the retry-link field `originalRequestId`; this repo's public DTOs use `originalRequestReference` for the equivalent concept. | **IC-002 (resolved)** — see Decisions |
| `forge-service#5` — new REST paths | `GET /api/v1/forge-jobs/pending-approval`, `POST /api/v1/forge-jobs/{forgeJobId}/{approve,reject,cancel}`, `GET /api/v1/forge-jobs/history?requesterReference={ref}` (query param) | Satisfies D-3. No conflict with this repo's public `.../pending/rest`, `.../approve/rest`, etc. paths — those are probe's own public API, independent of forge's internal path naming. Note: forge's history lookup is a **query parameter**, not a path segment; `RestApprovalForgeClient.getDecisionHistory` must call it accordingly. | Reuse (consistent), implementation note only |
| `forge-service#5` — new gRPC RPCs | `ListPendingApprovals`, `ApproveForgeJob`, `RejectForgeJob`, `CancelForgeJob`, `GetApprovalHistory` added to `ArtifactForgeService`; new fields on `CreateForgeJobGrpcRequest`/`ForgeJobGrpcResponse` (additive, per `AGENTS.md`) | Satisfies D-3/D-4 exactly; RPC names line up 1:1 with this repo's `ApprovalForgeClient` interface method set. No conflict. | Reuse (consistent) |
| `forge-service#5` — `ApproveForgeJobRequest{operatorId}`, `RejectForgeJobRequest{operatorId, reason}` | Operator identity field name/shape | Matches this repo's `ApprovalDecisionRequest{operatorId, reason}` exactly (D-5). No conflict. | Reuse (consistent) |
| `forge-service#5` — `CancelForgeJobRequest{requestedBy}` (IC-002 on forge's side) | Cancel is requester-initiated, not operator-initiated | Matches this repo's `CancelRequestRequest{requestedBy}` exactly, field-for-field. No conflict. | Reuse (consistent) |
| `forge-service#5` — error status table (404/409/400/409/200/409) | forge's **Error handling** table | Matches this repo's `design.md` **Error handling** table condition-for-condition. No conflict. | Reuse (consistent) |
| `forge-service#5` — Migration Plan "joint rollout dependency" | `requesterReference` becomes a **required** field on submission — a breaking change for any caller not yet supplying it. forge-service explicitly defers the exact sequencing to this repository's Gate 2 / `tasks.md`. | **Sequencing decision required**: whether probe-service's implementation PR may merge independently of forge-service's PR #5, or must land jointly. | **IC-001 (resolved)** — see Decisions |

## Decisions

### IC-001: Require joint rollout of forge-service#5 and probe-service's implementation PR

- **Question**: forge-service's design (`forge-service#5`) makes
  `requesterReference` a required field on `CreateForgeJobRequest` — a
  breaking change — and explicitly asks this repository's Gate 2 to decide
  whether probe-service's implementation must land together with
  `forge-service#5`, or can roll out independently relying on forge-service's
  stated fallback (defaulting a missing `requesterReference` to a generated
  value).
- **Clarification asked**: Should this be a hard joint-rollout dependency, or
  can probe-service merge independently? (Asked as a discrete decision.)
- **Decision**: Joint rollout is required. probe-service's implementation PR
  for this initiative must not merge before, or independently of,
  `forge-service#5` (or its eventual merged successor). `tasks.md` §9 records
  this as an explicit pre-merge coordination step.
- **Rationale**: Relying on forge-service's fallback-default behavior for a
  missing `requesterReference` would mean probe-service's own idempotency/
  retry logic (which assumes every submission always carries a real,
  caller-supplied `requesterReference`) could silently receive a
  server-generated value it never sees echoed correctly, breaking the
  idempotency and retry-linkage guarantees this change exists to provide.
  Explicit joint rollout removes that ambiguity entirely.
- **Owner**: Developer (Jenish16).
- **Approvers**: Developer (Jenish16).
- **Date**: 2026-08-03.
- **Counterpart link**: [forge-service#5](https://github.com/Jenish16/forge-service/pull/5), Migration Plan.
- **Supersedes**: None (this repository's `design.md` Rollout §3 already
  anticipated "ship together, or forge-service first"; this decision makes
  the joint-rollout branch mandatory rather than optional).

### IC-002: Mirror forge-service's exact `originalRequestId` field name at the downstream client boundary

- **Question**: forge-service's `CreateForgeJobRequest`/`ForgeJobResponse`
  name the retry-link field `originalRequestId`. This repository's public API
  (`CreateProbeForgeJobRequest`/`ProbeForgeJobResponse`) already uses
  `originalRequestReference` for the same concept (Decision 5, `design.md`).
- **Clarification asked**: Should probe-service's downstream (`dto.client.forge.*`)
  DTOs mirror forge-service's exact field name, translating at the mapper
  boundary, or should probe-service ask forge-service to rename?
- **Decision**: Mirror forge-service's exact field name.
  `ForgeCreateJobRestRequest`/`ForgeJobRestResponse` (and their gRPC
  counterparts) use `originalRequestId`, matching forge-service's wire
  contract exactly. `ProbeRequestMapper` translates between the public
  `originalRequestReference` field and the downstream `originalRequestId`
  field. The public API field name is unchanged.
- **Rationale**: forge-service owns this contract (PD-001); its field name is
  already implemented and tested in `forge-service#5`. Asking forge-service to
  rename a shipped, tested field would create unnecessary rework on the
  side that owns the state, whereas translating one field name at
  probe-service's own mapper boundary is a one-line, zero-risk change that
  keeps probe's already-published public API name stable for its own
  callers.
- **Owner**: Developer (Jenish16).
- **Approvers**: Developer (Jenish16).
- **Date**: 2026-08-03.
- **Counterpart link**: [forge-service#5](https://github.com/Jenish16/forge-service/pull/5), `CreateForgeJobRequest.originalRequestId` / `ForgeJobResponse.originalRequestId`.
- **Supersedes**: `design.md`'s **Interfaces** section's implicit assumption
  that the downstream DTO field name matches the public API field name
  one-for-one (updated in place with an `IC-002` cross-reference).

## Unresolved blockers

None. Both exact-resource gaps found (`IC-001`, `IC-002`) are resolved by
developer decision above. `dependency` is recorded (not `passed`) because
`IC-001` makes this repository's implementation PR sequencing-dependent on
`forge-service#5` reaching at least the same implementation state before
probe-service's own implementation PR merges — implementation work itself may
proceed.

## Conclusion

Discovery found one true counterpart with implementation already in progress
(`forge-service#5`, same initiative, draft, open). Comparison against this
repository's `design.md`/`tasks.md` found no functional or ownership
conflicts — forge-service's actual shipped contract matches this repository's
D-1 through D-5 dependency assumptions almost exactly, with two exact-resource
gaps (`IC-001` sequencing, `IC-002` field naming) resolved above. Result
recorded as `dependency`: implementation may proceed on `tasks.md`, but
probe-service's implementation PR must land jointly with `forge-service#5`
per `IC-001`.

## Implementation completion evidence

| Evidence | Result | Source |
|---|---|---|
| Implementation PRs merged | Pass — required rollout order preserved | [forge-service#5](https://github.com/Jenish16/forge-service/pull/5) merged before [probe-service#7](https://github.com/Jenish16/probe-service/pull/7) |
| Mandatory tasks complete | Pass — all 38 tasks marked `[x]` | `tasks.md` |
| Required tests passed | Pass — 112 tests, 0 failures | `./gradlew clean test` (2026-08-04) |
| Documentation updated | Pass | `docs/AI_CONTEXT.md`, `docs/architecture.md`, `docs/entities-enums.md`, `docs/external-services.md`, `docs/flows/` |
| Functional and Technical Contexts refreshed | Pass | Product Spec unchanged; repository AI/technical context refreshed |
| Required integration validation | Pass — live Probe → Forge REST and gRPC paths | Pending approval, Loadout cancellation, rejection and retry validated (2026-08-04) |
