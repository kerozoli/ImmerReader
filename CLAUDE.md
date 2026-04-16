# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the project
mvn clean package

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=HelloWorldControllerTest

# Run application
mvn spring-boot:run

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Architecture Overview

**ImmerReader** is a Spring Boot 3.4.5 application (Java 21) that performs automatic display detection for heating/boiler systems using camera image processing.

### Dual-System Design

The application monitors two boiler brands with parallel architectures:

| System | Component Prefix | Polling Interval | Data Extracted |
|--------|------------------|------------------|----------------|
| Immergas | `Immer*` | 2 seconds | Temperature, throttle (1-4), heating status, boiler status |
| Ariston | `Ariston*` | 15 seconds | Percentage (0-100% in 5% steps) |

### Core Flow

```
IP Camera (JPEG) → ImageProcessingService → Scheduled Parser → SharedData Container → REST Controller
```

1. **Schedulers** (`ImmerScheduler`, `AristonScheduler`) fetch images from IP cameras with timeout protection
2. **ImageProcessingService** performs pixel-level light detection at hardcoded coordinates
3. **SharedData** components (`ImmerData`, `AristonData`) hold latest parsed values as Spring singletons
4. **Controllers** expose cropped/uncropped images and JSON data endpoints

### Image Processing Logic

- **Light Detection**: Averages RGB values in a cross pattern around specified coordinates, compares against threshold
- **Immergas**: Decodes two 7-segment digits (14 coordinate checks) plus status LEDs (6 coordinate checks)
- **Ariston**: Checks 21 LED positions in a row to determine percentage value

### Configuration

Camera URLs in `src/main/resources/application.properties`:
- `camera.immer.url` - Immergas camera endpoint
- `camera.ariston.url` - Ariston camera endpoint
- `server.port` - Application port (default: 8099)
