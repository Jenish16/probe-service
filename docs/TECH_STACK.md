# Tech stack

| Area | Choice |
|------|--------|
| Language | Java (see `build.gradle.kts` toolchain) |
| Build | Gradle (Kotlin DSL) |
| Framework | Spring Boot |
| Public API | Spring Web MVC (REST) |
| Downstream REST | Spring RestClient via `BaseApiClient` |
| Downstream gRPC | gRPC Java + protobuf |
| Resilience | Resilience4j circuit breaker + retry (`FORGE_REST`, `FORGE_GRPC`) |
| Contracts | protobuf (`proto/artifact_forge_service.proto`) |
| Testing | JUnit 5, Spring Boot Test, MockMvc |

Version numbers are defined in `build.gradle.kts` and the Gradle wrapper — do not hard-code versions here.

## Persistence

Current experiment has no database or cache.

## Downstream dependency

| Transport | Location |
|-----------|----------|
| forge-service REST | `http://localhost:8081` |
| forge-service gRPC | `localhost:9091` |
