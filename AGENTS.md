# Agent guide for probe-service

Read these files before making changes:

1. [docs/AI_CONTEXT.md](docs/AI_CONTEXT.md) — service role, package layout, current experiment
2. [docs/README.md](docs/README.md) — documentation index
3. [specs/README.md](specs/README.md) — spec-driven workflow
4. Relevant feature spec and tasks under `specs/<feature>/`

## Rules

- Keep the repo lightweight; this is a learning POC, not production software.
- Do not add frameworks or infrastructure without a clear reason.
- Do not structure the whole service around one downstream dependency (`forge-service` is a caller target, not the root module).
- Do not duplicate `forge-service` business logic or validation rules beyond caller-side pre-checks.
- Preserve REST and gRPC contracts documented in specs and `proto/`.
- Keep public probe-service DTOs (`dto/request`, `dto/response`) separate from downstream forge REST DTOs (`dto/client/forge/`).
- Treat the `forge-service` API contract in specs and docs as the integration source of truth.
- Ask if requirements are unclear before guessing.
- Update tests when behavior changes.
- Update docs only when actual behavior changes — no speculative future docs.
- `docs/architecture.md`, `docs/entities-enums.md`, `docs/external-services.md`, and `docs/flows/` are part of the AI-fication baseline and now exist — keep them in sync with real code (see `.cursor/rules/docs-maintenance.mdc`), but keep additions proportional to this repo's small size. Do not expand them speculatively.

## Parallel SDD

- This repo has both a Spec Kit adapter (`.sdd-parallel/adapters/spec-kit/`, natively registered as the `parallel-sdd` extension under `.specify/extensions/parallel-sdd/`) and an OpenSpec schema (`openspec/schemas/myntra-sdd/`) installed. Both frameworks are initialized and **ready** in this repo — Spec Kit's `after_specify`/`before_plan`/`after_plan`/`before_implement`/`after_implement` hooks and OpenSpec's `continue` workflow are active.
- Before technical planning: read the feature/change `status.yaml` and both review reports; run Gate 1 (`.sdd-parallel/WORKFLOW.md`) before/during technical planning.
- Confirm bounded `related_repositories` proposed from the Functional Spec and service dependency context before relying on cross-repo discovery.
- Open a Technical Spec PR titled `[SDD-TECH][<initiative-id>] <title>` and store its URL in `pull_requests.technical_spec`; run Gate 2 before implementation.
- Store implementation PRs in `pull_requests.implementation` and rerun completion validation after merge/context refresh — never infer `implemented` from similar code.
- Stop on `blocked`, `stale`, or `scan-unavailable`; store clarifications/decisions as stable `PD-*`/`IC-*` records and synchronize them through explicit counterpart URLs. Update `updated_at` whenever status, checks, links, or counterparts change.
- AI-fication itself created no `status.yaml` for the existing `specs/001-artifact-forging-rest-grpc` folder — it predates Parallel SDD and is treated as legacy-completed, not retrofitted.

## Package structure

Use normal Java backend packages under `com.codeistari.probe`:

`controller`, `dto`, `service`, `client`, `mapper`, `config`, `exception`, `domain`

Do not create a top-level `forge` module or `com.codeistari.probe.forge` package.
