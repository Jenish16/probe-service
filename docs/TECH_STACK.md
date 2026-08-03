# Tech stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 25 (Gradle toolchain, `build.gradle.kts`) |
| Build | Gradle (Kotlin DSL), `com.google.protobuf` plugin 0.9.5 for proto codegen |
| Framework | Spring Boot 4.0.6 |
| Public API | Spring Web MVC (REST, JSON) |
| Downstream REST | Spring `RestClient` (`spring-boot-starter-restclient`) via `BaseApiClient` |
| Downstream gRPC | gRPC Java 1.69.0 + protobuf 3.25.5 (`grpc-netty-shaded`, `grpc-stub`, `grpc-protobuf`) |
| Data stores | None — see Persistence below |
| Messaging | None — no Kafka/queue producer or consumer in this codebase |
| Resilience | Resilience4j 2.4.0 circuit breaker + retry (`FORGE_REST`, `FORGE_GRPC`, see `resilience4j.*` in `application.yml`) |
| Contracts | protobuf (`proto/artifact_forge_service.proto`) |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Mockito |

Version numbers above are read from `build.gradle.kts`; treat that file as the source of truth if it changes.

## Persistence

Current experiment has no database or cache.

## Downstream dependency

| Transport | Location |
|-----------|----------|
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
