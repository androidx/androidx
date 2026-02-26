# Testing Guide

This document provides instructions on how to run tests for Remote Compose.

## Running Unit Tests

To run unit tests for a specific module, use the following command:

```bash
./gradlew :compose:remote:<module-name>:test
```

For Android modules, use:

```bash
./gradlew :compose:remote:<module-name>:testDebugUnitTest
```

### Key Unit Tests

| Module | Test Class | Description |
| :--- | :--- | :--- |
| `remote-core` | `WireTest` | Tests the binary wire format. |
| `remote-creation-core` | `RemoteComposeWriterTest` | Tests the procedural Java writer. |
| `remote-creation-compose` | `RemoteComposeV2Test` | Tests the V2 Compose-based creation API. |
| `player-view-demos` | `RemoteComposeConverterTest` | Performs round-trip conversion tests for `.rc` files. |

#### Running RemoteComposeConverterTest

```bash
./gradlew :compose:remote:integration-tests:player-view-demos:testDebugUnitTest --tests "androidx.compose.remote.integration.view.convert.RemoteComposeConverterTest"
```

## Running Instrumentation Tests

To run instrumentation (on-device) tests:

```bash
./gradlew :compose:remote:<module-name>:connectedCheck
```

### Key Instrumentation Tests

| Module | Test Class | Description |
| :--- | :--- | :--- |
| `remote-creation-compose` | `BasicLayoutTest` | Verifies basic layout rendering and capture. |
| `remote-player-view` | `PlayerScreenshotTest` | Verifies rendering accuracy via screenshots. |
| `remote-player-view` | `DisplayDocumentTest` | Tests document loading and display. |

## Running All Tests

To run all tests in the project (caution: this may take a long time):

```bash
./gradlew :compose:remote:test
```

For instrumentation tests:

```bash
./gradlew :compose:remote:connectedCheck
```
