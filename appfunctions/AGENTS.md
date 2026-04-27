# Project: androidx.appfunctions

## Project Description
AppFunctions allow Android apps to share pieces of functionality that the system and various AI agents can discover and invoke.
This Jetpack library has two types of users: callers and target apps that implement AppFunctions.
For callers, this library provides backward-compatible APIs to discover and invoke AppFunctions.
For target apps, this library simplifies implementation using KSP code generation. Developers write functions and annotate them with `@AppFunction`, and an `AppFunctionService` implementation will be generated.

## General Instructions

- Run commands from `frameworks/support/`.
- Run `./gradlew :appfunctions:<module>:ktFormat` to format code whenever you edited a file.
- If you change a published public API, run `./gradlew :appfunctions:<module>:updateApi` for the affected published module.
- In unit tests, prefer robolectric to mock.

## Validation

- Find and run the smallest relevant Gradle task from this file while iterating. The file `appfunctions/local_tests.sh` lists all available test commands.
- Before completing work, always execute `bash appfunctions/local_tests.sh`.
- If tests fail due to a missing connected device, prompt the user.

## Subprojects

- `appfunctions`: Runtime library included by both Callers and Target apps. It provides the APIs Callers use to discover and invoke functions, along with the core classes shared between callers and target apps.
- `appfunctions-service`: Additional runtime library required exclusively by target apps. It contains APIs consumed only by target apps, as well as the logic to route execution requests to the developer's annotated functions.
- `appfunctions-compiler`: annotation processor / code generation.
- `appfunctions-testing`: Public testing helpers for AppFunctions.
- `appfunctions-stubs`: compile-only stubs for the AppFunctions SDK Extension, which is a way we backported AppFunction APIs to reviously released API levels. This module should be frozen.
- `integration-tests`: end-to-end instrumented tests with a testing caller and a testing target app.

