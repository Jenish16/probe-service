---
description: "Record the Technical Spec PR and validate design decisions"
---

# Validate Parallel SDD Plan

Run `python3 .sdd-parallel/bin/sdd_parallel_preflight.py --framework speckit`
and stop if it fails.

Read `.sdd-parallel/WORKFLOW.md`. Locate the active feature and current branch.

1. Rerun Gate 1 when the plan changed after the report fingerprint.
2. Check that material `PD-*` records are not left as unrecorded chat context.
3. Find an existing open PR for the current branch.
4. Require its title to begin
   `[SDD-TECH][<initiative-id>]`.
5. Store its canonical URL in `pull_requests.technical_spec` without
   duplication.
6. If no PR exists, ask the developer whether to create a draft PR or provide
   its URL; do not invent a link.
7. Update `updated_at`.

Do not set `tech-approved`. That transition requires the normal human
Technical Spec review and all blocking design decisions to be approved.
