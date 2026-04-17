# AGENTS.md

## Build & Run

```bash
mvn clean package           # Build
mvn test                    # All tests
mvn test -Dtest=ClassName   # Single test class
mvn spring-boot:run         # Run app
mvn versions:display-dependency-updates  # Check updates
```

**Java version:** Java 25 throughout (pom.xml, CI workflows, Docker)

## Architecture

Spring Boot 3.4.5 app (`@EnableScheduling`) for boiler display image processing.

### Dual System

| System   | Prefix       | Interval | Data                    |
|----------|--------------|----------|-------------------------|
| Immergas | `Immer*`     | 2 sec    | Temp, throttle, status  |
| Ariston  | `Ariston*`   | 15 sec   | Percentage (0-100%)     |

### Flow

```
IP Camera → ImageProcessingService → Scheduler → SharedData (singleton) → REST Controller
```

### Package Structure

- `Controller/` - REST endpoints + Thymeleaf views
- `Service/` - Image analysis logic (`ImmerAnalyzerService`, `AristonAnalyzerService`)
- `Scheduler/` - Polling (`ImmerScheduler`, `AristonScheduler`)
- `SharedData/` - Thread-safe state containers

## CI/CD

- **CodeQL:** Java/Kotlin analysis on push/PR to `main`
- **PMD:** `rulesets/java/quickstart.xml`, fails on violations
- **Docker:** Multi-platform (amd64/arm64), pushes to `kerozoli/immerreader:latest`

## Configuration

`src/main/resources/application.properties`:
- `server.port=8099`
- Camera URLs are hardcoded in scheduler/service classes

## Docker

```bash
docker build -t kerozoli/immerreader .
docker run -p 8099:8099 -v /data:/data kerozoli/immerreader
```
