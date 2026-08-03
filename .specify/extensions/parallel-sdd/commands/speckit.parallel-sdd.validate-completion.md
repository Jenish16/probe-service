---
description: "Record implementation PRs and validate completion evidence"
---

# Validate Parallel SDD Completion

Run `python3 .sdd-parallel/bin/sdd_parallel_preflight.py --framework speckit`
and stop if it fails.

Read **Implementation completion validation** in
`.sdd-parallel/WORKFLOW.md`. Locate the active feature and current branch.

1. Find implementation PRs titled `[SDD-IMPL][<initiative-id>]` and store
   their canonical URLs in `pull_requests.implementation`.
   If none exists yet, ask the developer to create/provide the implementation
   PR and record completion as pending; do not invent a URL.
2. Inspect PR state and record merge commits only when the provider reports the
   PR merged.
3. Validate mandatory tasks, required tests, documentation, context refresh,
   integration validation, and required dependencies.
4. Update the `completion` front matter and evidence table in
   `implementation-conflict-review.md`.
5. Update `updated_at`.

The automatic `after_implement` execution normally records
`completion.result: pending` because code is not yet merged. Keep
`implementation-in-progress`.

Rerun this command after merge and Functional/Technical Context refresh. Set
`status: implemented` only when every required evidence item passes. Never
infer completion from code similarity or unchecked task boxes.
