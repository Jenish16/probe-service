# probe-service

Caller-side Spring Boot lab service for exploring service-to-service communication, gRPC clients, REST clients, AI-native development, and spec-driven implementation.

## Purpose

`probe-service` is part of the Code Istari backend learning lab.

This service acts as the caller side of backend experiments. It is used to call `forge-service` through APIs such as REST or gRPC and to validate client-side concerns like request handling, deadlines, retries, error mapping, metadata propagation, and observability.

The goal is to practice modern backend engineering with an AI-native workflow:

1. Write the spec first.
2. Use AI to assist implementation.
3. Run the generated code.
4. Review and tweak the code manually.
5. Add tests.
6. Document actual learnings after implementation.

This repo should stay simple and practical. Future experiments should be added to `specs/` and `experiments/` only when they are actually started or completed.

## Companion Service

This service works with:

```text
forge-service
```

Typical flow:

```text
probe-service  --->  forge-service
```

`probe-service` initiates requests.  
`forge-service` exposes APIs and backend behavior.

## Repository Structure

```text
probe-service/
├── README.md
├── AGENTS.md
├── CLAUDE.md
├── .cursor/
│   └── rules/
├── docs/
├── specs/
├── experiments/
├── proto/
├── src/
└── tools/
```

## Key Directories

### `docs/`

Contains AI-readable repository context, architecture notes, tech stack, important flows, and service guidance.

These files are mainly for AI tools such as Cursor, Claude Code, Spec Kit, or OpenSpec-style workflows.

### `specs/`

Contains feature or experiment specifications before implementation.

Each meaningful feature or experiment should have its own folder.

Recommended structure:

```text
specs/
└── <feature-or-experiment-name>/
    ├── spec.md
    └── tasks.md
```

Use `spec.md` to describe what needs to be built.  
Use `tasks.md` to break implementation into small, reviewable steps.

### `experiments/`

Contains completed or in-progress experiment notes.

Do not add future experiment notes just for planning. Add notes here only after an experiment is actually started or completed.

Each experiment note should capture:

```text
Goal
What was built
What AI generated
What was changed manually
Issues faced
Tests added
Production learnings
Final conclusion
```

### `proto/`

Contains protobuf contracts used by this service.

For early learning, protobuf files can exist inside both `probe-service` and `forge-service`.

If contracts become larger or shared across more services, they can be moved to a separate contract module later.

### `src/`

Contains the Spring Boot application code.

Expected package style:

```text
com.codeistari.probe
```

### `tools/`

Contains small helper scripts for local development, code generation, or repeatable commands.

## AI-Native Development Workflow

For every meaningful change:

```text
1. Read AGENTS.md
2. Read docs/AI_CONTEXT.md
3. Create or update a spec under specs/
4. Break the work into tasks
5. Use AI to assist implementation
6. Review generated code manually
7. Add or update tests
8. Run the service locally
9. Update docs or experiments only if actual behavior changed
```

## Development Rules

- Keep this repo lightweight.
- Do not add future experiment details to README.
- Do not add new frameworks without a clear reason.
- Do not change public API or protobuf contracts casually.
- Keep specs small and implementation-focused.
- Keep generated code readable and reviewable.
- AI-generated code must be manually reviewed before accepting.
- Tests should be updated when behavior changes.
- Documentation should describe actual implementation, not imagined future plans.

## Local Development

Run the service:

```bash
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```

Expected companion service:

```text
forge-service
```

## Status

Learning lab. Not production software.
