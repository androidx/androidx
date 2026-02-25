# Project: Jetpack Compose (General)

This directory contains the libraries that make up Jetpack Compose. All code
generated or modified must follow the guidelines in
`frameworks/support/docs/api_guidelines` and
`frameworks/support/docs/api_guidelines/compose_api_guidelines`, as well as the
[Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).

## Project Structure Map
- **Runtime (`compose:runtime`):** The core engine for state management and the
  composition tree.
- **UI (`compose:ui`):** Orchestration layer for layout, input, graphics, and
  text primitives.
- **Foundation (`compose:foundation`):** Design-system-agnostic building blocks
  (e.g., `LazyColumn`, gestures).
- **Material/Material3 (`compose:material`):** Design-system-specific
  components implementing Material Design.

## General Instructions
- **Formatting:** Use `./gradlew :ktCheckFile --format --file <file>` for all
  `.kt` files. Multiple files can be formatted by appending more `--file <path>`
  arguments.
- **API Updates:** Run `./gradlew <project>:updateApi` (and
  `<project>:updateAbiNative` where applicable) after any public API change.
- **Context:** Always execute commands from the `frameworks/support` directory.
- **Git:** Use `git mv` when moving files to preserve history. Do not create
  git commits unless explicitly requested.
- **Build Verification:** Ensure changes compile by running
  `./gradlew <project>:compileDebugKotlinAndroid`.

## Coding & Performance Standards
- **Multiplatform:** Jetpack Compose is a multiplatform project. Implement
  logic in `commonMain` whenever possible. Use `androidMain` or other
  platform-specific directories only for platform-specific API implementations.
- **Modifiers:** Prefer `Modifier.Node` implementations for high-performance
  modifiers. Avoid `composed {}`.
- **API Visibility:** Prefer **Public APIs** where possible. Use
  `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` only when an API is strictly
  internal to AndroidX and would be difficult or dangerous for external
  developers to use correctly.
- **Opt-in APIs:** Use `@Experimental...Api` annotations only when introducing
  new API surfaces that are likely to remain unstable. When in doubt, don't use
  them.
- **Dependencies:** Avoid introducing new dependencies without verifying their
  stability and usage elsewhere in the project. Consult `libraryversions.toml`
  for version management. Always check with the user when adding a new
  dependency.

## Testing & Documentation
- **Samples:** Every new public API should have a corresponding sample in the
  project's `samples` module. Link it in the KDoc using the `@sample` tag.
- **Screenshot Tests:** Use `androidDeviceTest` for rendering verification.
  Screenshot tests must use `AndroidXScreenshotTestRule` and should be
  restricted to a specific SDK version (typically
  `SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)`) to ensure
  deterministic output.
- **JVM Tests:** Use standard unit tests in the `test` directory for non-UI
  logic.
- **Test Naming:** Test classes should be named `*Test` and placed in the same
  package as the code they test.
