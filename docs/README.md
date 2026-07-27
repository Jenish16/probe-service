# Documentation index

Lightweight docs for AI tools and local development.

| Document | Purpose |
|----------|---------|
| [AI_CONTEXT.md](AI_CONTEXT.md) | Service role, package layout, current experiment |
| [architecture.md](architecture.md) | Module map, request lifecycle, component diagram |
| [entities-enums.md](entities-enums.md) | Enums and why there are no persisted entities |
| [external-services.md](external-services.md) | Downstream `forge-service` dependency, AI guidance |
| [TECH_STACK.md](TECH_STACK.md) | Languages, frameworks, dependencies |
| [SERVICE_BOUNDARY.md](SERVICE_BOUNDARY.md) | What probe-service vs forge-service owns |
| [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md) | How to run and test locally |

## Flows

| Flow | Purpose |
|------|---------|
| [create-forge-job](flows/create-forge-job/README.md) | Create a forge job via probe-service (REST + gRPC downstream) |
| [get-forge-job](flows/get-forge-job/README.md) | Retrieve a forge job via probe-service (REST + gRPC downstream) |

Feature specs live under [../specs/README.md](../specs/README.md).

Agent rules: [../AGENTS.md](../AGENTS.md)
