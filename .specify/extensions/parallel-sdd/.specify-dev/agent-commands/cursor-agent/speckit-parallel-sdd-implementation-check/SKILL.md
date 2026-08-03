---
name: speckit-parallel-sdd-implementation-check
description: Run the exact Parallel SDD implementation conflict and reuse check
compatibility: Requires spec-kit project structure with .specify/ directory
metadata:
  author: github-spec-kit
  source: parallel-sdd:commands/speckit.parallel-sdd.implementation-check.md
---

# Implementation Conflict and Reuse Check

Run `python3 .sdd-parallel/bin/sdd_parallel_preflight.py --framework speckit`
and stop if it fails.

Read `.sdd-parallel/WORKFLOW.md`. Locate the active feature and require
`status: tech-approved`.

Execute **Discovery** and **Gate 2**:

1. inspect non-implemented specs and matching open PRs in the current and
   confirmed related repositories;
2. follow exact URLs in `counterpart_specs` and snapshot every remote revision;
3. recompute the combined fingerprint and treat changed sources as stale;
4. compare APIs, events, payloads, schemas, tables, migrations, components,
   libraries, configuration, tasks, tests, and rollout dependencies;
5. ask about material ambiguity;
6. preserve clarifications and decisions as stable `IC-*` records;
7. update `implementation-conflict-review.md`,
   `implementation_conflict`, and `updated_at`.

Stop before code changes on `blocked`, `stale`, or `scan-unavailable`. When the
gate passes, set lifecycle to `implementation-in-progress`. Require future
implementation PR titles to begin `[SDD-IMPL][<initiative-id>]` and store their
URLs in `pull_requests.implementation`.