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
- **Scoping Builds**: Always use `PROJECT_PREFIX` to speed up Gradle configuration, e.g.,
  `PROJECT_PREFIX=:camera:camera-core ./gradlew :camera:camera-core:assemble`.

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
  `./gradlew <project>:lintRelease` after completing a meaningful set of changes.
- **Code Elegance**: After implementing a solution and passing tests, always
  review the code to ensure it is clean, elegant, and readable (e.g., extract
  complex conditional logic into descriptive helper methods).
- **Regression Prevention**: Scan the codebase and analyze the impact of your
  changes on related components to ensure no potential regressions are
  introduced.
- **Camera2 API Usage**: When writing code that utilizes Android Camera2 APIs
  (directly or indirectly, including modifying behavior that relies on them),
  always revisit the official [Android Camera2 API reference](https://developer.android.com/reference/android/hardware/camera2/package-summary)
  to check the API usage guidelines and contracts before finalizing the code change,
  ensuring all usage aligns with the framework's design.
- **Standard Verification Procedure**: Never skip the verification steps.
  Always compile (build), run related tests, run lint, and **verify code
  elegance and regression prevention** before finalizing any code changes.

## Testing

CameraX involves complex hardware interactions, making robust testing essential.

- **Assertion Library**: Use the Google **Truth** library for fluent and readable assertions.
  Avoid using traditional JUnit `assert*` methods or Hamcrest matchers.
- **Fakes vs. Mocks**: Prioritize the use of fakes and test doubles (e.g., those provided in
  `camera-testing`) over mocking frameworks like Mockito to ensure more reliable and
  maintainable tests.
- **Testing Libraries**: Utilize `camera-testing`, `camera-common-testing`, and
  `camera-camera2-pipe-testing` for writing robust fakes.
- **Log Management**: To prevent context bloat from excessive tool output, run large test suites
  with minimal verbosity (e.g., using `-q` or redirecting output to a file). Only re-run specific
  failing tests with verbose logging when necessary for debugging. Always remove any temporary
  files (e.g., `.log` files) created during the process before finalizing your work.
- **Robolectric Configuration**: When writing Robolectric tests, avoid hardcoding `minSdk` values
  (e.g., `@Config(minSdk = 21)`). Instead, use `@Config(sdk = [Config.TARGET_SDK])` for standard
  tests or `@Config(sdk = [Config.ALL_SDKS])` when logic needs verification across all supported
  SDK levels.
- **Internal Testing (Googlers Only)**: If you are in a Google-internal environment
  (e.g., you have access to Google-internal search or repositories), you MUST
  load `AGENTS_INTERNAL.md`. If your environment is set up, this file can be
  found in the directory specified by the `CAMERAX_INTERNAL_TOOLS_DIR` environment
  variable. Follow its instructions to actively use the lab device testing
  infrastructure to verify your changes on physical devices before finalizing
  any code modifications.

## Skill: CameraX Troubleshooting & Code Verification

### Use when:
- Modifying existing CameraX functionality or fixing bugs.
- Writing new CameraX code or adding new integration tests.
- Troubleshooting test failures or device-specific issues.

> [!IMPORTANT]
> Whenever you modify code or tests, you **MUST** ensure that the library
> compiles successfully and all related tests pass. Never submit untested code.

### Workflow:

#### 1. Research & Context Gathering (Before Modifying Code)
- **Read Documentation & API Contracts**: Carefully read the JavaDoc and API contracts of
  the class/interface you are modifying. Understand the design intent and constraints.
- **Camera2 API Check**: If the change interacts with or modifies behavior
  relying on Android Camera2 APIs, read the official Camera2 API reference
  (e.g., [`StreamConfigurationMap`](https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap),
  [`CameraCharacteristics`](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics))
  to verify that the proposed changes align with the documented framework behavior
  and constraints.
- **Analyze Existing Code & Style**: Reference existing implementations in the same module.
  Observe the coding style, threading model, and check for any "intentional" workarounds (e.g.,
  device-specific workarounds or deprecation usage) that must be preserved.
- **API Change Check**: Verify if your change introduces public API modifications. Remember:
  - Do not introduce public API changes in a bug fix CL.
  - If a new API is necessary, mark it `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` and add a
    `TODO` with a bug ID to make it public in the next alpha.
- **Reference Existing Tests**: Search for existing tests targeting the component you are
  modifying. They serve as "Case Studies" for how the component is expected to behave and how to
  verify it. Key directories:
  - `camera/integration-tests/coretestapp/src/androidTest/`
  - `camera/camera-core/src/androidTest/`
  - `camera/camera-camera2/src/androidTest/`
  - `camera/camera-video/src/androidTest/`

#### 2. Verification Plan (Writing Tests)
- **Headless Execution**:
  - Use `FakeLifecycleOwner` instead of `ActivityScenarioRule` to avoid activity-lifecycle
    race conditions, unless testing UI controllers (`CameraController`, `PreviewView`).
  - Transition the lifecycle to active using `fakeLifecycleOwner.startAndResume()` to trigger
    camera output.
  - For `Preview` headless tests, use `SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()`
    to simulate a UI surface and prevent frame buffer stalls.
- **Main Thread Requirements**:
  - Ensure lifecycle binding (`bindToLifecycle`, `unbindAll`) and surface provider
    interactions are executed on the Main thread (using `runBlocking(Dispatchers.Main) { ... }`).
- **Anti-Flakiness & Synchronization**:
  - **NEVER use `Thread.sleep()`**. Use `CountDownLatch` or event listeners (`VideoRecordEvent`
    for video) to await state changes (e.g., wait for `VideoRecordEvent.Status` to confirm
    active recording).
- **Hardware & Capability Checks**:
  - Check lens support: `CameraUtil.hasCameraWithLensFacing(lensFacing)`. Use
    `Assume.assumeTrue` to skip if unsupported.
  - Check video capabilities: `Recorder.getVideoCapabilities(cameraInfo)` before running
    video tests.
- **Combinatorial & Lifecycle Stress**:
  - Test sequence of calls, subsets, and conditional bindings to ensure isolated stability.
  - Simulate background/foreground transitions using `fakeLifecycleOwner.pauseAndStop()` and
    `startAndResume()` to verify pipeline recovery.

#### 3. Execution & Validation
- **Local Compile Check**: Compile the entire CameraX project tests and debug APKs using:
  ```bash
  ./gradlew -p camera assembleAndroidTest assembleDebug
  ```
  Alternatively, use `PROJECT_PREFIX` to scope compilation to specific modules to save time:
  ```bash
  PROJECT_PREFIX=:camera:camera-core ./gradlew :camera:camera-core:assemble
  ```
- **Local Test Run**:
  - **Host Tests (Robolectric)**: Run JVM-based tests using
    `./gradlew <project>:test` (runs all variants).
    To run a specific test class or method, you **must use the variant-specific
    task** (usually `testReleaseUnitTest` in AndroidX) with the `--tests` flag
    (as the anchor `:test` task does not support filtering):
    ```bash
    ./gradlew :camera:camera-core:testReleaseUnitTest --tests "androidx.camera.core.streamsharing.StreamSharingTest.methodName"
    ```
    > [!WARNING]
    > While running a single method is faster during development, always run the
    > **full test class** (e.g., `--tests "androidx.camera.core.streamsharing.StreamSharingTest"`)
    > before committing to catch inter-test leaks or side effects.
  - **Device Tests**: Run instrumented tests on a connected device using:
    ```bash
    ./gradlew <project>:connectedCheck
    ```
- **FTL Run (if no device connected)**:
  - Discover FTL tasks for your project: `./gradlew <project>:tasks --all | grep ftl` (e.g.,
    `ftlpixel2api30debugAndroidTest` for apps, or `releaseAndroidTest` variants for libraries).
  - Run a specific test in FTL using `--className`:
    ```bash
    PROJECT_PREFIX=:camera:integration-tests:camera-testapp-core \
    ./gradlew :camera:integration-tests:camera-testapp-core:ftlpixel2api30debugAndroidTest \
    --className androidx.camera.integration.core.StreamSharingTest#recordingCanProceedAfterSiblingUnbind
    ```
- **Code Quality**: Format modified Kotlin files using `ktfmt` (see General Instructions) and run
  Lint (specifically `lintRelease` to catch release-only issues) before committing:
  ```bash
  ./gradlew <project>:lintRelease
  ```

#### 4. Troubleshooting Unit Test Leaks (Robolectric)
- **Symptom**: `IllegalStateException: Camera surface session should only fail with request cancellation. Instead failed due to: FutureGarbageCollectedException: The completer object was garbage collected...`
- **Root Cause**: A test binds a UseCase to a `FakeCamera` (or real camera) but does not properly detach it before the test finishes. This leaves the camera session active and leaks internal `DeferrableSurface` termination futures. When GC runs in subsequent tests, these leaked futures are collected, throwing exceptions in unrelated tests.
- **Solution**: Always ensure proper cleanup in `@After` / `tearDown()` block of your test class:
  - If using `FakeCamera` directly, call `camera.detachUseCases(listOf(useCase))` before unbinding the use case.
  - Ensure the main looper is idled after cleanup: `shadowOf(getMainLooper()).idle()`.

#### 5. Troubleshooting Device-Specific Failures
- **Symptom**: A failure (test failure, crash, or unexpected behavior) occurs
  only on specific device models, while working correctly on others.
- **Investigation Steps**:
  1. **Acquire Logs**: Extract the logcat, test output, or system logs associated with the failure.
     - For public AOSP developers, obtain the logs from your test runner output
       or device logcat.
     - For Google-internal developers, follow the instructions in
       `AGENTS_INTERNAL.md` to use internal CLI tools to download and read
       test artifacts from the test results repository.
  2. **Analyze Failure Point**: Identify the exact line of failure and the
     preceding events in the log (e.g., check for timeouts, crashes, or
     specific error codes).
  3. **Compare Logs**: Compare the failing log with a passing log from a
     different device to identify differences in HAL behavior or timing.
  4. **Check Related Devices**: Identify if similar devices (e.g., same
     manufacturer, chipset, or model family like Fold/Flip series) might share
     the same HAL characteristics and exhibit the same issue.
     - *Fallback Strategy*: If the target device (or related devices) is not
       available in the lab or experiences persistent allocation
       timeouts/failures:
       a. Search the lab device list for a similar alternative (same brand,
          Android OS level, or camera capabilities) to run verification.
       b. If no suitable alternative is available to verify the device-specific
          behavior, proceed with static analysis and code verification (unit
          tests), and explicitly inform the user that device-level verification
          was not possible due to resource constraints.
  5. **Determine Component Level**: Check if it's a test infrastructure issue
     (e.g., too tight timeout) or a real library/HAL issue (e.g.,
     `Connection timed out` from `libcameraservice` suggesting HAL freeze).
- **Resolution Strategy**:
  1. **Test-Level Issues**: If the issue is due to timing or environment,
     increase timeouts or improve test robustness (e.g., add retry or polling).
  2. **Library/HAL Issues**:
     - Check if the behavior is a known device limitation (e.g., some physical
       lenses not supporting reprocessing).
     - Explore if a generic workaround is possible without session
       reconfiguration.
     - **Quirk as Last Resort**: If it is a HAL bug and no generic workaround
       is possible, add the device model to the corresponding Quirk class
       (e.g., `ZslDisablerQuirk`).
       *   *Note*: When adding a device to a Quirk, check if other related
           devices identified in step 4 (and available in the lab) also
           exhibit the issue and should be included. Verify the fix (ZSL
           disabled/fallback working) on all of them.

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
- https://developer.android.com/reference/android/hardware/camera2/package-summary (Camera2 API Reference)

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
