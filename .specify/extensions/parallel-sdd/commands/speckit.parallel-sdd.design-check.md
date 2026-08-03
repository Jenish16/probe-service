---
description: "Run Parallel Design Discovery before Spec Kit planning"
---

# Parallel Design Discovery

Run `python3 .sdd-parallel/bin/sdd_parallel_preflight.py --framework speckit`
and stop if it fails.

Read `.sdd-parallel/WORKFLOW.md`. Locate the active feature exactly as the
initialize command does. Initialize missing metadata by asking the developer;
do not require a separate command rerun.

Execute **Discovery** and **Gate 1**:

1. inspect current-repository non-implemented specs;
2. inspect all matching open PRs in the current repository and every confirmed
   `related_repositories` entry;
3. follow exact URLs in `counterpart_specs` and add newly discovered relevant
   PR/spec URLs;
4. snapshot every remote revision and compute the combined fingerprint defined
   in the workflow;
5. compare capability, workflow, ownership, service, and possible contract
   overlap;
6. ask about material ambiguity;
7. preserve clarifications and decisions as stable `PD-*` records;
8. update `parallel-design-review.md`, `parallel_discovery`, and `updated_at`;
9. set lifecycle to `tech-design-in-progress` when planning begins.

Use `scan-unavailable` when required remote evidence cannot be inspected.
Potential overlap may continue into planning; unresolved ownership or contract
decisions prevent later Technical Spec approval.
