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

## Package structure

Use normal Java backend packages under `com.codeistari.probe`:

`controller`, `dto`, `service`, `client`, `mapper`, `config`, `exception`, `domain`

Do not create a top-level `forge` module or `com.codeistari.probe.forge` package.
