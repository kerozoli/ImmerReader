# AGENTS.md

## Build & Run

```bash
mvn clean package                # Build
mvn test                         # All tests
mvn test -Dtest=ClassName        # Single test class
mvn spring-boot:run              # Run app
mvn versions:display-dependency-updates  # Check updates
```

**Java version:** 25 (pom.xml, CI, Docker)

## Architecture

Spring Boot 3.4.5 (`@EnableScheduling`) for IP-camera boiler display image processing.

### Dual System

| System   | Prefix       | Intervals / Mechanism                    | Data                       |
|----------|--------------|------------------------------------------|----------------------------|
| Immergas | `Immer*`     | Manual scheduler, 2 s + backoff on error | Temp, throttle, status     |
| Ariston  | `Ariston*`   | `@Scheduled(fixedRate=15000)`            | Percentage (0–100%)        |

### Flow

```
IP Camera (hardcoded URL) → Scheduler → AnalyzerService → SharedData (singletons) → Controller
```

- **Schedulers** call `AnalyzerService` directly; there is no separate `ImageProcessingService`.
- **ImmerScheduler** uses its own `ScheduledExecutorService`. On success the next read is after 2 s; on timeout/error it increments the delay by 1 s up to 60 s.
- **AristonScheduler** uses Spring's `@Scheduled(fixedRate = 15000)` and executor-based timeout.
- **AnalyzerServices** mutate the input `BufferedImage` in place (draw red/white crosses) before analysis.
- Camera URLs are **hardcoded** in scheduler and controller classes, not in `application.properties`.

### Package Structure

- `Controller/` - REST endpoints + Thymeleaf views
- `Service/` - Image analysis logic (`ImmerAnalyzerService`, `AristonAnalyzerService`)
- `Scheduler/` - Polling (`ImmerScheduler`, `AristonScheduler`)
- `SharedData/` - Thread-safe state containers

## Data Persistence

- `ImmerManagerData` → `/data/offset.properties` (offsetX, offsetY, enabled)
- `AristonManagerData` → `/data/ariston.properties` (startX/startY, endX/endY, enabled)
- Both are `@Component` singletons that load on `@PostConstruct` and write on every setter call.

## CI/CD

- **CodeQL:** Repository uses GitHub's default setup (custom workflow removed). Do not re-add a manual workflow.
- **PMD:** `rulesets/java/quickstart.xml`, fails the build on violations.
- **Docker:** Multi-platform (`linux/amd64,linux/arm64`), pushes `kerozoli/immerreader:latest`.

## Configuration

`src/main/resources/application.properties`:
- `server.port=8099`
- Camera URLs are **not** here; they are hardcoded in scheduler/service/controller classes.

## Docker

```bash
docker build -t kerozoli/immerreader .
docker run -p 8099:8099 -v /data:/data kerozoli/immerreader
```

- Data persistence requires `-v /data:/data`; properties and error stats rely on it.
