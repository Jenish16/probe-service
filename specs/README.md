# Specs

Each experiment or feature should have its own folder with:

- `spec.md` — what to build and constraints
- `tasks.md` — implementation checklist

## Workflow

1. Read [docs/AI_CONTEXT.md](../docs/AI_CONTEXT.md)
2. Read the feature `spec.md`
3. Implement tasks in `tasks.md`
4. Add or update tests
5. Update lightweight docs only if behavior changed

## Current experiments

| ID | Folder | Summary |
|----|--------|---------|
| 001 | [001-artifact-forging-rest-grpc](001-artifact-forging-rest-grpc/) | Call forge-service via REST and gRPC |

## Parallel SDD (Spec Kit / OpenSpec)

This repository also has a Parallel SDD baseline installed for both Spec Kit (`.sdd-parallel/adapters/spec-kit/`) and OpenSpec (`openspec/schemas/myntra-sdd/`), currently activation-pending. Any feature created through those frameworks (rather than as a manual experiment folder above) gets a `status.yaml` and follows the gates in `.sdd-parallel/WORKFLOW.md` — see the "Parallel SDD" section of `AGENTS.md`. Existing experiment folders above predate this baseline and are not retrofitted with lifecycle metadata.
