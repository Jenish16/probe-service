# Parallel SDD Workflow

## Initialize

1. Obtain the exact active Spec Kit feature root or OpenSpec change root from
   the current framework command. The active root must have been created or
   explicitly selected for the current initiative; never infer it by scanning
   existing spec folders.
2. If `status.yaml` is missing, obtain `initiative_id` from the approved
   Functional Spec. Ask the developer when it is missing or ambiguous; create
   and validate the file in the same interaction.
3. Read the Functional Spec, Functional Code Context, and documented
   upstream/downstream dependencies. Propose a bounded list of repositories
   likely to be affected, ask the developer to confirm it, and store canonical
   `owner/repository` values in `related_repositories`.
4. Do not perform organization-wide discovery. An empty confirmed list is
   allowed, but do not silently assume it.

Repo AI-fication and adapter upgrades do not have an active feature. During
those operations, every pre-existing feature/spec folder without `status.yaml`
is `legacy-completed`, regardless of task checkboxes, index labels, apparent
implementation state, modification time, or whether it is the only folder.
Do not add `status.yaml`, `parallel-design-review.md`, or
`implementation-conflict-review.md` to those folders.
5. Replace all template values and never leave placeholders.
6. Update `updated_at` after every lifecycle, check-result, PR-link, related
   repository, or counterpart change.

## Discovery

Inspect:

1. non-implemented status files in the current checkout;
2. all open `[SDD-TECH]` and `[SDD-IMPL]` PRs in the current repository;
3. matching open PRs in each confirmed `related_repositories` entry; and
4. every exact URL in `counterpart_specs`.

For any discovery candidate other than the active feature/change:

- if `status.yaml` exists, use its lifecycle normally;
- if `status.yaml` is missing, classify it as `legacy-completed`, exclude it
  from overlap comparison, and do not create/backfill metadata or flag a
  potential conflict.

This compatibility rule applies only to peer/legacy candidates. A missing
`status.yaml` for an explicitly identified active feature must be created
through **Initialize**. Being the only, newest, or index-listed feature does
not make an existing folder active.
An open standardized `[SDD-TECH]` or `[SDD-IMPL]` PR remains active work and is
still inspected; the legacy rule applies to old spec folders, not current open
SDD PRs.

Use `git remote get-url origin` for current repository identity. For GitHub,
use the developer's authenticated `gh` CLI. List related-repository PRs with
`gh pr list --repo <owner/repository> --state open` and fetch only matching SDD
PRs. If required remote evidence is unavailable, record `scan-unavailable`.

When a related PR/spec is relevant, add its canonical URL to
`counterpart_specs`. This converts a discovered relationship into an explicit
link for future checks.

Reports may list `legacy-completed` candidates under discovery exclusions for
auditability, but must not put them in impact comparison or use them to produce
`potential-overlap`, `dependency`, or `blocked`.

## Remote source snapshots and freshness

For every remote source, record under report front matter `source_snapshots`:

```yaml
- url: <canonical URL>
  kind: pr | github-file | public-file
  revision: <PR head SHA, Git blob SHA, or immutable revision>
  content_sha256: <SHA-256 of inspected content>
```

Fetch rules:

- **GitHub PR URL:** use `gh pr view <URL>` for metadata/head SHA and
  `gh pr diff <URL>` for content.
- **GitHub blob/file URL:** resolve owner, repository, ref, and path; use
  `gh api repos/<owner>/<repo>/contents/<path>?ref=<ref>`. Record the returned
  blob SHA and hash the decoded content.
- **Public raw/file URL:** fetch via HTTPS, hash the body, and record an
  immutable revision or response validator when available.
- **Private or unsupported URL:** use an authenticated provider client; if it
  cannot be read or revisioned, record `scan-unavailable`.

Compute `input_fingerprint` as SHA-256 over the sorted local input hashes plus
sorted `url|revision|content_sha256` snapshot entries. Before accepting a
previous result, re-fetch every remote source. Any changed/missing revision,
new matching SDD PR, or changed local input makes the report `stale` and
requires a new comparison.

## Gate 1: Parallel Design Discovery

Compare capabilities, services, workflows, ownership, and possible contracts.
Write `parallel-design-review.md`. Ask about material ambiguity and store the
clarification. Record approved decisions as `PD-*`; AI must not self-approve
cross-service ownership.

Set `parallel_discovery` to the report result. Potential overlap may continue
through planning, but unresolved blocking decisions prevent `tech-approved`.

## Technical Spec PR

Require:

```text
[SDD-TECH][<initiative-id>] <title>
```

Store its URL in `pull_requests.technical_spec`. Search all open matching PRs,
including PRs no longer marked draft.

## Gate 2: Implementation Conflict and Reuse

Require lifecycle `tech-approved`. Compare exact APIs, events, payloads,
tables, migrations, components, libraries, configuration, tasks, tests, and
rollout dependencies. Write `implementation-conflict-review.md`, including
remote snapshots and the combined input fingerprint.

Ask about material ambiguity and record decisions as `IC-*`. Set
`implementation_conflict` to the result. Do not start implementation on
`blocked`, `stale`, or `scan-unavailable`.

Implementation PRs use:

```text
[SDD-IMPL][<initiative-id>] <title>
```

Store their URLs in `pull_requests.implementation`.

## Implementation completion validation

Run once after implementation generation and rerun after merge/context refresh.
Update the `completion` front matter and evidence table in
`implementation-conflict-review.md`.

Set `status: implemented` only when all are evidenced:

1. every required implementation PR is merged and merge commits are recorded;
2. mandatory local tasks are complete;
3. required automated, regression, compatibility, and integration tests pass;
4. required integration validation is `passed` or explicitly `not-required`;
5. repository documentation is updated; and
6. Functional and Technical Code Contexts are refreshed.

After implementation but before merge, record available evidence, set
`completion.result: pending`, and keep `implementation-in-progress`. If the
evidence is contradictory or a required dependency is unresolved, set
`completion.result: blocked`. Never infer completion from similar code.

## Decision record

Every decision records status, question, clarification, decision, rationale,
affected resources, owner, approvers, date, counterpart URL, and superseded
decision. Use the same decision ID and outcome at every explicit counterpart.
