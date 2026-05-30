# Local development

## Prerequisites

- JDK matching `build.gradle.kts` toolchain
- `forge-service` running locally for end-to-end calls

## Run probe-service

```bash
./gradlew bootRun
```

probe-service listens on port **8082** (see `application.yml`).

## Run tests

```bash
./gradlew test
```

## Companion service

Start `forge-service` before manual API testing:

| Transport | Address |
|-----------|---------|
| REST | `http://localhost:8081` |
| gRPC | `localhost:9091` |

## Example create request (REST transport via probe)

```bash
curl -s -X POST http://localhost:8082/api/v1/probe-requests/forge-jobs/rest \
  -H 'Content-Type: application/json' \
  -d '{
    "artifactName": "ember-ring",
    "artifactType": "RING",
    "material": "MITHRIL",
    "requestedBy": "ranger",
    "powerLevel": 7
  }'
```

Use `/grpc` instead of `/rest` to exercise the gRPC downstream path.
