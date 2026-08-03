---
name: speckit-parallel-sdd-initialize
description: Initialize or validate Parallel SDD lifecycle metadata
compatibility: Requires spec-kit project structure with .specify/ directory
metadata:
  author: github-spec-kit
  source: parallel-sdd:commands/speckit.parallel-sdd.initialize.md
---

# Initialize Parallel SDD

Run `python3 .sdd-parallel/bin/sdd_parallel_preflight.py --framework speckit`
and stop if it fails. This refreshes the installed Spec Kit integration and
Parallel SDD extension when the CLI version or managed extension changes.

Read `.sdd-parallel/WORKFLOW.md` and follow **Initialize**.

Use the active feature directory provided by the current Spec Kit command
context. It must be the feature created or explicitly selected by the current
command, not an existing folder inferred by scanning `specs/`. If the command
does not provide an exact feature root, ask the developer. Do not select the
only folder, the newest folder, or a folder marked in-progress in an index.

During Repo AI-fication or adapter upgrade there is no active feature. Treat
every pre-existing metadata-free spec folder as `legacy-completed` and do not
create `status.yaml` or either review report in it.

If `status.yaml` is missing:

1. read the approved functional/specification input for an initiative ID;
2. if it is absent or ambiguous, ask the developer for it;
3. copy `.sdd-parallel/templates/status.yaml`;
4. replace every template value and set an ISO-8601 `updated_at`; and
5. validate against `.sdd-parallel/status.schema.json`.

Create the two review files from `.sdd-parallel/templates/` if missing.
Preserve existing reports and decisions.

Read the Functional Spec, Functional Code Context, and repository dependency
documentation. Propose a bounded list of likely affected repositories, ask the
developer to confirm it, and store canonical `owner/repository` entries in
`related_repositories`. Do not perform organization-wide discovery or silently
assume an empty list.