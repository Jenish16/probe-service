---
schema_version: 2
gate: implementation-conflict
result: dependency
checked_at: "2026-08-04T00:35:00+05:30"
input_fingerprint: "52c3cb98d3ad6ca1bfd517511baaaa0b875fa96f0b5e022dfdbdeb7ec95163f7"
source_snapshots:
  - url: https://github.com/Jenish16/forge-service/pull/4
    kind: pr
    revision: 16ceb0a9e78526e3baa4b76f1585e6a4f78a87e4
    content_sha256: e59cc323fabd23af461dcad7ee9438e82d3a1d9b695fa7355130121b7fc24240
  - url: https://github.com/Jenish16/forge-service/pull/6
    kind: pr
    revision: 490335b185752d92d75e5797c3d7e40bcfee4df7
    content_sha256: ddb3188737b828e69b32bd76e6fbc932e8670c74f9d95b6977332c6bb2df6b97
completion:
  result: not-run
  checked_at:
  merge_commits: []
  mandatory_tasks_complete: false
  tests_passed: false
  documentation_updated: false
  contexts_refreshed: false
  integration_validation: pending
---

# Implementation Conflict and Reuse Review

## Discovery scope

### Local active specifications

- `specs/002-probe-quest-loadouts/` (self — active feature; `tech-approved`)

### Current-repository open PRs

- None open in `Jenish16/probe-service`. (`probe-service#5`, the draft
  Technical Spec PR for this feature, has since been reviewed and approved
  — see `status.yaml`; no new SDT PR is open at time of this check.)

### Related-repository open PRs

- `Jenish16/forge-service` [#6](https://github.com/Jenish16/forge-service/pull/6)
  `[SDD-IMPL][POC-QUEST-LOADOUTS] Quest Loadout Fulfilment` — same
  initiative, **new since Gate 1**. Contains the actual implemented
  `LoadoutController`/`LoadoutService`/`LoadoutGrpcService`/DTOs for
  `forge-service`'s side of this initiative. Inspected in full for this
  check (see Exact resource comparison below).
- `Jenish16/forge-service` [#5](https://github.com/Jenish16/forge-service/pull/5)
  `[SDD-IMPL][high-power-artifact-approval] Implement High-Power Artifact
  Approval` — different initiative (`POC-HIGH-POWER-APPROVAL`), still open
  (not merged). Confirms the approval-owned fields
  (`approvalStatus`/`approvalExpiresAt`/`rejectionReason`) remain absent
  from every live `forge-service` Loadout response today, consistent with
  research.md D10. Not a counterpart of `POC-QUEST-LOADOUTS`; excluded from
  exact resource comparison.

### Explicit counterpart specifications and PRs

- `Jenish16/forge-service` [#4](https://github.com/Jenish16/forge-service/pull/4)
  (recorded at Gate 1) — re-snapshotted at its final merge commit
  (`16ceb0a9`, merged `2026-08-03T17:42:20Z`). Its REST contract
  (`contracts/quest-loadouts-rest.md`) and proto
  (`contracts/quest_loadout_service.proto`) are **byte-identical** to the
  Gate 1 snapshot (`e5e9316c`) — no diff found; the design did not change
  between Gate 1 and merge.

### Legacy specifications excluded

- `specs/001-artifact-forging-rest-grpc/` — no `status.yaml`;
  `legacy-completed`.

## Exact resource comparison

| Related work | APIs/events/data/components/config/tests | Ownership/reuse/sequencing | Classification |
|---|---|---|---|
| `forge-service#4` (merged Technical Plan) | REST/proto contracts unchanged since Gate 1 (byte-identical diff). Endpoint paths (`POST /api/v1/loadouts`, `GET .../{id}`, `POST .../cancel`, `POST .../retry`, `GET .../items/{id}/attempts`), status codes (`201`/`200`/`400`/`404`/`409`), and response field names/nullability (`@JsonInclude(NON_NULL)`) match `contracts/quest-loadouts-probe-rest.md` exactly. | No change to PD-001 (`probe-service` role) or D1–D10 needed. | `dependency` (non-blocking, confirms the fixed contract) |
| `forge-service#6` (open implementation) | **IC-001**: `LoadoutItemRequest`/`RetryLoadoutItemRequest` use raw, unvalidated `String` fields (no Bean Validation) so `LoadoutService.validationFailureReason` can report invalid items individually (FR-002/FR-003); the 2–10 item bound (FR-001) is a manual check with a dedicated message, not Bean Validation. This directly conflicted with `probe-service`'s planned typed/bounded item DTOs (original research.md D7), which would reject the whole request at `probe-service` for any structurally-invalid item, unreachable by `forge-service`'s per-item rejection path. **IC-002**: `LoadoutGrpcService.createLoadout` uses gRPC `Status.ALREADY_EXISTS` for the duplicate-conflict case; the existing `GrpcForgeJobClient.mapGrpcException` switch has no case for it and would default to `502`. | Both resolved this check (see Decisions) — `probe-service`'s DTOs and `GrpcLoadoutClient`'s own error mapping are corrected to match `forge-service`'s actual behavior before implementation starts. | `dependency` (resolved; proceed) |
| `forge-service#5` (High-Power Approval implementation, different initiative) | Confirms approval-owned fields are still absent from every Loadout response (`forge-service`'s own `LoadoutItemResponse`/`AttemptResponse` javadocs: "Always null/omitted... until that initiative's implementation lands"). | No overlap with `probe-service`'s planned resources — excluded per legacy/counterpart rules (not a `POC-QUEST-LOADOUTS` PR). | Excluded from comparison |

## Decisions

- **IC-001**
  - **Question**: `forge-service`'s actual `LoadoutItemRequest`/
    `RetryLoadoutItemRequest` use raw, unvalidated fields specifically so
    invalid item values become individual `rejectedItems` entries rather
    than failing the whole request. `probe-service`'s planned item DTOs
    (research.md D7) use typed `ArtifactType`/`ForgeMaterial` enums and
    `@Min`/`@Max`/`@Size` bounds. Should `probe-service` keep its
    originally-planned typed/bounded DTOs (accepting a whole-request `400`
    for structurally-invalid items instead of per-item rejection), or
    mirror `forge-service`'s raw/unvalidated fields exactly?
  - **Clarification**: A typed enum field fails JSON deserialization before
    Bean Validation runs; a `@Min`/`@Max` violation fails Bean Validation
    before the request ever reaches `forge-service`. Either way,
    `probe-service` would reject the entire request with a generic `400`,
    making spec.md User Story 1 Acceptance Scenario 2 ("3 valid items
    accepted, 1 invalid item reported individually") unreachable for any
    `probe-service` caller whose invalid item fails at the `probe-service`
    layer instead of `forge-service`'s.
  - **Decision**: `probe-service`'s `ProbeLoadoutItemRequest` and
    `RetryProbeLoadoutItemRequest` mirror `forge-service`'s
    `LoadoutItemRequest`/`RetryLoadoutItemRequest` exactly — raw `String`
    fields, no per-field Bean Validation, no `@Size` bound on `items`.
    `probe-service` retains only the container-level `@NotBlank`/
    `@NotNull @NotEmpty` checks that exactly match `forge-service`'s own
    Bean Validation footprint. See research.md D7 (revised).
  - **Rationale**: A caller-side pre-check is only equivalent (not
    duplicative) when it produces the identical outcome as the
    authoritative check for every input; this holds for blank
    container-level fields but not for typed/bounded item fields.
  - **Affected resources**: `specs/002-probe-quest-loadouts/research.md`
    (D7), `data-model.md` (`ProbeLoadoutItemRequest`,
    `RetryProbeLoadoutItemRequest`, `CreateProbeLoadoutRequest`),
    `contracts/quest-loadouts-probe-rest.md`, `tasks.md` (T007, T019, T046,
    T054–T056).
  - **Owner**: Quest Loadout Fulfilment initiative (`probe-service` side).
  - **Approvers**: Developer (via `/speckit-implement` Gate 2 session,
    2026-08-03).
  - **Date**: 2026-08-03
  - **Counterpart URL**: https://github.com/Jenish16/forge-service/pull/6
  - **Supersedes**: The item-field-validation portion of research.md D7 as
    originally written during Technical Planning.

- **IC-002**
  - **Question**: `forge-service`'s Loadout gRPC service returns
    `Status.ALREADY_EXISTS` for the duplicate-`requestReference` conflict
    case (REST's `409`). `probe-service`'s existing
    `GrpcForgeJobClient.mapGrpcException` switch has no `ALREADY_EXISTS`
    case. Should the new `GrpcLoadoutClient` reuse that method unmodified
    (silently mapping `409`-equivalents to `502`), extend the shared
    method, or define its own?
  - **Clarification**: Editing the shared `GrpcForgeJobClient` method would
    change the single-artifact flow's gRPC error mapping, which has no
    idempotency-conflict scenario in its own fixed contract (FR-019 must
    stay unmodified).
  - **Decision**: `GrpcLoadoutClient` defines its own gRPC-status-mapping
    method, structurally mirroring `GrpcForgeJobClient.mapGrpcException`'s
    existing cases, with one addition: `ALREADY_EXISTS → HttpStatus.CONFLICT`.
  - **Rationale**: Satisfies FR-020 (consistent REST/gRPC behavior) without
    touching the existing single-artifact flow's fixed contract.
  - **Affected resources**: `specs/002-probe-quest-loadouts/research.md`
    (new D11), `contracts/quest-loadouts-probe-rest.md`, `plan.md`
    (Failure Handling, Risks), `tasks.md` (T011, T017, T049).
  - **Owner**: Quest Loadout Fulfilment initiative (`probe-service` side).
  - **Approvers**: Self-resolved during Gate 2 (single reasonable fix,
    mechanical extension of an already-established pattern; no competing
    trade-off requiring developer judgment).
  - **Date**: 2026-08-03
  - **Counterpart URL**: https://github.com/Jenish16/forge-service/pull/6
  - **Supersedes**: None (research.md D9 did not previously address the
    gRPC transport's per-code error mapping in this detail).

## Unresolved blockers

None. Both IC-001 and IC-002 are resolved; `research.md`, `data-model.md`,
`contracts/quest-loadouts-probe-rest.md`, and `tasks.md` have been updated
to reflect the corrected design before any implementation code is written.

## Conclusion

`dependency` — `forge-service#6`'s actual implementation surfaced two
concrete, resolvable conflicts with `probe-service`'s planned design
(IC-001, IC-002), both corrected in the Technical Plan artifacts above.
`forge-service#4`'s fixed contract itself is unchanged since Gate 1.
Implementation may proceed.

## Implementation completion evidence

| Evidence | Result | Source |
|---|---|---|
| Implementation PRs merged | Not checked | |
| Mandatory tasks complete | Not checked | |
| Required tests passed | Not checked | |
| Documentation updated | Not checked | |
| Functional and Technical Contexts refreshed | Not checked | |
| Required integration validation | Not checked | |
