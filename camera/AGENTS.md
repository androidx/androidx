# Project: CameraX

## General Instructions:

- **Kotlin Formatting**: When modifying any .kt file, format it using `ktfmt` via the
  following command: `./gradlew :ktCheckFile --format --file <file>`. If more than one file needs
  formatting, continue adding `--file <next-file>` to the command.
- **Public API**: When a public API is changed or when asked to update the public API files,
  execute: `./gradlew <project>:updateApi`. The projects and their root paths can be found in
  `settings.gradle`.
- **File Management**: When moving files, use `git mv` to keep version control history.
- **Git Commits**: Do not make a git commit unless specifically requested.

## Development Workflow & Refactoring:

- **Language**: Prefer Kotlin to Java for new files. When migrating files, convert them from Java to
  idiomatic Kotlin.
- **Kotlin Idioms**: Prefer modern Kotlin idioms for readability.
- **API Design**: For public API design, follow the
  [Android API guidelines](https://source.android.com/docs/setup/contribute/api-guidelines)
  and the
  [AndroidX API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/).
  New APIs should prioritize Kotlin users over Java users while still ensuring they are easy to use
  from Java. For more details, see https://developer.android.com/kotlin/interop.
- **New Public APIs**: If a new public API needs to be added and the current project version is
  not an alpha version (e.g., it is in beta or rc), do NOT bump the version yourself. Instead,
  mark the new API with `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` and add a `TODO` comment
  right above it with the bug ID, e.g., `// TODO: b/1234567 - Make this public in next alpha`.
- **Linting**: Ensure code quality and adherence to AndroidX standards by running
  `./gradlew <project>:lint` after completing a meaningful set of changes.

## Testing

CameraX involves complex hardware interactions, making robust testing essential.

- **Assertion Library**: Use the Google **Truth** library for fluent and readable assertions.
  Avoid using traditional JUnit `assert*` methods or Hamcrest matchers.
- **Fakes vs. Mocks**: Prioritize the use of fakes and test doubles (e.g., those provided in
  `camera-testing`) over mocking frameworks like Mockito to ensure more reliable and
  maintainable tests.
- **Host Tests**: Run JVM-based unit tests using `./gradlew <project>:test` (e.g.,
  `./gradlew :camera:camera-core:test`). This is the preferred task as it automatically
  maps to the available build variant (e.g., `release` for libraries, `debug` for apps)
  without redundant execution.
- **Task Discovery**: If unsure of the correct test task, use `./gradlew <project>:tasks --all | grep test`
  to identify available variants.
- **Device Tests**: Run instrumented tests on a connected device using
  `./gradlew <project>:connectedCheck`.
- **Testing Libraries**: Utilize `camera-testing`, `camera-common-testing`, and
  `camera-camera2-pipe-testing` for writing robust fakes.
- **Log Management**: To prevent context bloat from excessive tool output, run large test suites
  with minimal verbosity (e.g., using `-q` or redirecting output to a file). Only re-run specific
  failing tests with verbose logging when necessary for debugging. Always remove any temporary
  files (e.g., `.log` files) created during the process before finalizing your work.

## Git Commit Messages

Use the following format for your commit messages. Each section should be separated by a blank line.
The commit title should not exceed 50 characters, and body lines should not exceed 72 characters.

```
<Commit Title>

<Additional details about the change.>

RelNote: <release note text>
Bug: <bug id>
Test: <test instructions>
```

**Commit Title:**
- A short, descriptive summary of the change.
- Use the imperative mood (e.g., "Add feature" not "Added feature").

**Additional Details (Optional):**
- Explain the problem the change solves and the approach taken.
- Provide context for the change.
- Prefer concise, clear, and readable messages without losing any required info. Using bullet
  points to summarize specific changes can help with readability.

**RelNote:**
- Focus on the observable impact for developers using the library (the "What", not the "How").
  Do not mention implementation details or internal test apps (like `core-test-app`) in the RelNote.
- This will be used to generate release notes.
- **Requirement**: Should only be added for public API changes (e.g., changes in `camera/**/api/`
  directories) or bugs impacting public users.
- **When to omit**: If a change is internal-only (e.g., refactoring, test updates, or changes to
  restricted APIs marked with `@RestrictTo`), **do not add the RelNote tag at all**.
- **Edge cases**: `RelNote: N/A` can be used if a release note is explicitly not applicable
  despite a public API change, e.g. when reverting a CL that had a public API change meant for
  release.
- Refer to https://developer.android.com/jetpack/androidx/releases/camera for previous examples.

**Bug:**
- The ID(s) of the bug(s) this commit fixes. List each bug ID on a new line, prefixed with 'Bug:'.
  Example:
  ```
  Bug: 123456
  Bug: 123457
  ```

**Test:**
- Describe the tests that were added or modified to verify the change.
- Mention any manual testing steps if applicable. Use project names like `core-test-app`
  instead of specific activity names where appropriate.

## Description of sub-projects:

- camera-camera2: The implementation layer that bridges `camera-core` abstractions to the
  `camera-camera2-pipe` backend.
- camera-camera2-pipe: A performance-oriented Camera2 abstraction layer that provides a flexible
  shim to power high-efficiency camera applications.
- camera-camera2-pipe-testing: Testing library for `camera-camera2-pipe`.
- camera-common: Contains common utility classes and constants used across CameraX modules.
- camera-common-testing: Provides testing utilities and fakes for `camera-common`.
- camera-compose: A library that provides Jetpack Compose integration for CameraX.
- camera-core: The core library of CameraX. It provides the basic camera functionalities. Its
  Camera2-dependent implementations are provided by `camera-camera2`.
- camera-effects: A library for applying visual effects to camera streams.
- camera-extensions: A library that provides access to device-specific camera effects and features.
- camera-extensions-stub: A stub implementation for camera extensions.
- camera-lifecycle: A library that provides lifecycle management for CameraX.
- camera-mlkit-vision: A library for integrating ML Kit Vision with CameraX.
- camera-testing: A library providing testing utilities for CameraX.
- camera-testlib-extensions: Testing libraries for camera extensions.
- camera-video: A library that provides video recording functionalities.
- camera-view: A library that provides a custom View for camera preview.
- featurecombinationquery: A group of standalone Camera2 libraries for querying supported
  combinations of camera features. These can be used independently of CameraX. Includes
  `featurecombinationquery` and `featurecombinationquery-play-services`.
- integration-tests: A project containing integration tests for the camera libraries.
- media3-effect: A library that provides seamless integration for applying Media3-based effects to
  camera streams in CameraX.
- viewfinder: A group of standalone Camera2 libraries providing a `Viewfinder` widget for camera
  previews. These can be used independently of CameraX. Includes `viewfinder-compose`,
  `viewfinder-core`, and `viewfinder-view`.

## Documentation links:

- https://developer.android.com/training/camerax
- https://developer.android.com/training/camerax/architecture
- https://developer.android.com/training/camerax/configuration
- https://developer.android.com/training/camerax/preview
- https://developer.android.com/training/camerax/take-photo
- https://developer.android.com/training/camerax/analyze
- https://developer.android.com/training/camerax/video-capture
- https://android-developers.googleblog.com/search?q=camerax

## AndroidX-specific Instructions

### Git Commit Amend

Some git commit messages contain a `Change-Id:` line (usually as the last line) required by the
Gerrit code review system. This ID is essential for updating existing CLs.

When amending a previous commit (e.g., with `git commit --amend`):
- **Do not modify or remove this line.**
- **CRITICAL: NEVER use the `-m` flag** alone when amending. It replaces the *entire* message,
  stripping the `Change-Id` and causing CL upload to create a duplicate CL.
- **Best Practice**: Run `git log -1` first to capture the existing message/ID. When using `-m`,
  ensure you manually append the correct `Change-Id` to the end of the new message.
- **Recovery**: If the ID is lost, find it in the previous commit via `git reflog` and `git log`,
  then re-amend to restore it.
