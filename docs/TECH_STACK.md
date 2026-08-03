# Tech stack

| Area | Choice |
|---|---|
| Language / runtime | Java 25 (Gradle toolchain, `build.gradle.kts`) |
| Build | Gradle (Kotlin DSL), Gradle wrapper (`gradlew`) |
| Framework | Spring Boot 4.0.6 (`spring-boot-starter-webmvc`, `-restclient`, `-validation`, `-json`, `-aspectj`) |
| Public API | Spring Web MVC (REST), port `8082` |
| Downstream REST | Spring `RestClient` via `BaseApiClient`, timeouts via `JdkClientHttpRequestFactory` |
| Downstream gRPC | gRPC Java 1.69.0 (`grpc-netty-shaded`, `grpc-stub`, `grpc-protobuf`) + protobuf 3.25.5 |
| Resilience | Resilience4j 2.4.0 (`resilience4j-spring-boot4`) circuit breaker + retry (`FORGE_REST`, `FORGE_GRPC`) |
| Contracts | protobuf (`proto/artifact_forge_service.proto`), generated via `com.google.protobuf` Gradle plugin |
| Data stores | None (no database or cache; see `docs/entities-enums.md`) |
| Messaging | None (no Kafka/queue producer or consumer in this codebase) |
| Testing | JUnit 5, Spring Boot Test, MockMvc (`spring-boot-starter-webmvc-test`, `-restclient-test`) |

Exact dependency versions live in `build.gradle.kts`; this table mirrors the current values and should be re-verified there when the build file changes.

## Persistence

Current experiment has no database or cache.

## Downstream dependency

| Transport | Location |
|---|---|
| forge-service REST | `http://localhost:8081` |
| forge-service gRPC | `localhost:9091` |

## Internal libraries

None. This repository does not depend on any shared/internal Myntra or Code Istari libraries — all code is local to `probe-service`.

## Commands

```bash
./gradlew bootRun   # run the service (REST :8082)
./gradlew test       # run unit, controller, mapper, and client tests
```

See `docs/LOCAL_DEVELOPMENT.md` for example REST calls and the `forge-service` companion setup.
