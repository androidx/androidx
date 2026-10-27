---
name: camerax-testing
description: >-
  CameraX unit (Robolectric) and instrumentation (androidTest) testing
  standards, Truth/fakes conventions, leak debugging, and Test Pyramid
  modernization.
---

# CameraX Testing, Troubleshooting & Test Pyramid Guide

## Testing

CameraX involves complex hardware interactions, making robust testing essential.

- **Assertion Library**: Use the Google **Truth** library for fluent and
  readable assertions. Avoid using traditional JUnit `assert*` methods or
  Hamcrest matchers.
- **Fakes vs. Mocks**: Prioritize the use of fakes and test doubles (e.g.,
  those provided in `camera-testing`) over mocking frameworks like Mockito to
  ensure more reliable and maintainable tests.
- **Testing Libraries**: Utilize `camera-testing`, `camera-common-testing`, and
  `camera-camera2-pipe-testing` for writing robust fakes.
- **Log Management**: To prevent context bloat from excessive tool output, run
  large test suites with minimal verbosity (e.g., using `-q` or redirecting
  output to a file). Only re-run specific failing tests with verbose logging
  when necessary for debugging. Always remove any temporary files (e.g., `.log`
  files) created during the process before finalizing your work.
- **Robolectric Configuration**: When writing Robolectric tests, avoid
  hardcoding `minSdk` values (e.g., `@Config(minSdk = 21)`). Instead, use
  `@Config(sdk = [Config.TARGET_SDK])` for standard tests or
  `@Config(sdk = [Config.ALL_SDKS])` when logic needs verification across all
  supported SDK levels.
- **Lab Test Rules & Physical Test Chart Boxes**: When writing device
  integration tests requiring physical calibration targets (such as QR codes,
  barcodes, or ISO charts), annotate the test method with
  `@LabTestRule.LabTestFrontCamera` or `@LabTestRule.LabTestRearCamera` to match
  the camera lens physically facing the test chart inside lab test boxes. Local
  execution on DUT requires setting debug tags (`adb shell setprop
  log.tag.frontCameraE2E DEBUG` or `rearCameraE2E DEBUG`).
- **Storing & Actively Analyzing Test Output Images**: To inspect output frames,
  debug 3A convergence (AF/AE/AWB), verify image analysis, or triage failures
  across CameraX use cases (`Preview`, `ImageAnalysis`, `ImageCapture`,
  `VideoCapture`), save bitmaps or output frames to
  `/sdcard/Pictures/test_output/<name>.png` (e.g. via
  `LabTestUtil.saveTestBitmap`). In automated testing, test runners pull files
  from this directory and attach them to the test result artifacts for visual
  inspection. When investigating test failures, developers and AI agents must
  examine the test code to understand its physical/visual preconditions and
  inspect captured images:
  - **Physical Fixture / Lab Environment Issue**: Missing or misaligned test
    chart, pitch black frame (box LED lights unpowered or failed), camera lens
    pointed at enclosure wall/ceiling (device improperly mounted or wrong lens
    facing configured), or lens occlusion—frequently causing **3A convergence
    timeouts** (passive AF/AE hunting on blank/dark scenes) or barcode
    detection timeouts on physical DUTs.
  - **CameraX Software / HAL Issue**: Test chart is centered and sharp but 3A
    convergence or detector failed; or frame displays buffer corruption,
    chromatic shearing, incorrect rotation/aspect ratio, or vendor HAL stream
    configuration failures.
  - **Pre-Inspection Image Integrity & Adaptive Context Budget**: Before calling
    `read_file` / `view_file` on downloaded test images, verify binary integrity
    (e.g. PNG `IHDR`/`IDAT`/`IEND` CRC + `zlib.decompress` or JPEG `SOI`/`EOI`
    via Python 3 stdlib `struct`/`zlib` rather than optional `PIL`) so truncated
    files never poison the multi-modal session trajectory, and adaptively
    downscale oversized PNGs or multi-device batches (`convert -resize
    <max_dim>x<max_dim>> -quality 85`) to stay within a `<= 3 MB` cumulative
    session budget.
- **On-Demand Lab Diagnostic Testing (`@LabTestRule.LabTestOnly`)**:
  When triaging suspected lab fixture failures (e.g. pitch-black 0-lux frames or
  camera orientation mismatches), execute on-demand diagnostic test suites
  annotated with `@LabTestRule.LabTestOnly` (such as
  `LabEnvironmentDiagnosticTest`). Diagnostic tests capture multi-camera frames
  (front/rear), compute relative luminance
  (`LabTestUtil.calculateBitmapLuminance`), log structured `[LAB_DIAGNOSTIC]`
  summaries, export photos via `LabTestUtil.saveTestBitmap`, and complete with
  PASS status so test runners automatically pull and attach artifacts for
  visual or AI inspection (see `AGENTS_INTERNAL.md` for internal lab runner
  details).
- **Internal Guidelines & Testing**: When operating in an environment where
  internal tools are configured, refer to `AGENTS_INTERNAL.md` (located in the
  directory specified by the `CAMERAX_INTERNAL_TOOLS_DIR` environment variable,
  if available). It serves as an extension of this guide, providing
  supplementary coding guidelines, extended documentation links, and
  instructions to use physical lab device testing infrastructure to verify
  changes when accessible.

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
- **Read Documentation & API Contracts**: Carefully read the JavaDoc and API
  contracts of the class/interface you are modifying. Understand the design
  intent and constraints.
- **Camera2 API Check**: If the change interacts with or modifies behavior
  relying on Android Camera2 APIs, read the official Camera2 API reference
  (e.g.,
  [`StreamConfigurationMap`](https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap),
  [`CameraCharacteristics`](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics))
  to verify that the proposed changes align with the documented framework
  behavior and constraints.
- **Read CameraX Code Widely (Structure, Object Job & Intentional Code)**:
  Do not look only at the target lines you plan to edit. Read surrounding
  classes, package/module hierarchies, and callers widely to understand the
  structural design, each object's responsibility ("object job"), and proper
  code placement so your change is elegant and idiomatic. Check comments, KDoc,
  and sibling classes to determine whether any existing ordering, workaround, or
  deprecation usage is intentional and must be preserved. When inspecting git
  history in `frameworks/support`, prefer fast
  `git --no-lazy-fetch blame -L <start>,<end> <file>` followed by
  `git --no-lazy-fetch show <sha> -- <path>` over unbounded `git log -L` or
  `git log -S` scans, which traverse the full commit graph and time out.
- **API Change Check**: Verify if your change introduces public API
  modifications. Remember:
  - Do not introduce public API changes in a bug fix CL.
  - If a new API is necessary, mark it
    `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` and add a `TODO` with a bug
    ID to make it public in the next alpha.
- **Reference Existing Tests**: Search for existing tests targeting the
  component you are modifying. They serve as "Case Studies" for how the
  component is expected to behave and how to verify it. Key directories:
  - `camera/integration-tests/coretestapp/src/androidTest/`
  - `camera/camera-core/src/androidTest/`
  - `camera/camera-camera2/src/androidTest/`
  - `camera/camera-video/src/androidTest/`
- **Multi-Model Root-Cause & Solution Finding (Step 6 Checkpoint 1 Gate)**:
  For any non-trivial bug investigation, subtle concurrency/HAL issue, or
  architectural design decision, **MUST** (by default) execute **Step 6
  Checkpoint 1** (Modes 2, 3, and 5) **during investigation and before
  implementing production fix code or finalizing any plan/investigation**
  (temporary diagnostic logging or reproduction tests are permitted to gather
  raw evidence).

#### 2. Verification Plan (Writing Tests)
- **Headless Execution**:
  - Use `FakeLifecycleOwner` instead of `ActivityScenarioRule` to avoid
    activity-lifecycle race conditions, unless testing UI controllers
    (`CameraController`, `PreviewView`).
  - Transition the lifecycle to active using
    `fakeLifecycleOwner.startAndResume()` to trigger camera output.
  - For `Preview` headless tests, use
    `SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()` to
    simulate a UI surface and prevent frame buffer stalls.
- **Activity-Based Tests (Deterministic Setup & Window Readiness)**:
  - *Principle*: Any precondition a test depends on must be established
    before the code that depends on it runs, and the test `Activity` must be
    able to reach `RESUMED` regardless of device state (keyguard, screen off,
    OEM launch policies, or system overlays).
  - *Rule ordering*: When a class has multiple `@Rule`s with side-effect
    dependencies (lab gating, foreground cleanup, `Assume` checks, permission
    grants, Activity launch), declare `@get:Rule(order = N)` on **every**
    rule. Without explicit `order`, JUnit's reflection-based rule order is
    unspecified. Lower `order` values run outermost, so preconditions
    (`LabTestRule`, `RequireForegroundRule` and its `withAssumptions { ... }`,
    `GrantPermissionRule`) must have lower `order` than `ActivityScenarioRule`
    or `AndroidComposeTestRule`.
  - *Window readiness*: Test-only Activities must configure lockscreen
    visibility in `onCreate()` (`setShowWhenLocked(true)` and
    `setTurnScreenOn(true)` on API 27+, or matching
    `WindowManager.LayoutParams` flags on older APIs, plus
    `FLAG_KEEP_SCREEN_ON`).
  - *Signal*: `NoActivityResumedException` with the Activity stuck in
    `STARTED` or `CREATED` typically indicates a violated rule/window
    precondition or an environment overlay rather than a camera bug. Fix the
    test setup instead of adding retries or sleeps.
- **Main Thread Requirements**:
  - Ensure lifecycle binding (`bindToLifecycle`, `unbindAll`) and surface
    provider interactions are executed on the Main thread (using
    `runBlocking(Dispatchers.Main) { ... }`).
- **Anti-Flakiness & Synchronization**:
  - **NEVER use `Thread.sleep()`**. Use `CountDownLatch` or event listeners
    (`VideoRecordEvent` for video) to await state changes (e.g., wait for
    `VideoRecordEvent.Status` to confirm active recording).
- **Hardware & Capability Checks**:
  - Check lens support: `CameraUtil.hasCameraWithLensFacing(lensFacing)`. Use
    `Assume.assumeTrue` to skip if unsupported.
  - Check video capabilities: `Recorder.getVideoCapabilities(cameraInfo)`
    before running video tests.
- **Combinatorial & Lifecycle Stress**:
  - Test sequence of calls, subsets, and conditional bindings to ensure
    isolated stability.
  - Simulate background/foreground transitions using
    `fakeLifecycleOwner.pauseAndStop()` and `startAndResume()` to verify
    pipeline recovery.
- **Deterministic Testing of In-Flight Async Transitions**:
  - When fixing races during asynchronous transitions (e.g. a request arriving
    while a session or codec is configuring, stopping, or releasing), use
    delegating test doubles that hold back completion callbacks until
    explicitly triggered. This deterministically pauses the state machine in
    intermediate states to verify interleaved calls and cancellations on the
    JVM without timing flakiness.
- **Hierarchical Pre-Flight Verification & Fault Domain Isolation**:
  - **Pre-Flight Primary Baseline Check**: When writing integration or device
    tests for specialized, secondary, or composite camera features (e.g.
    Physical Sub-Cameras, Camera Extensions, DynamicRange/HDR, RAW+JPEG
    simultaneous capture, VideoRecording, High-Speed FPS), always verify that
    the **primary baseline camera pipeline** can open and produce frames
    *first*. If a device cannot even open or stream its primary camera (e.g.,
    due to dead hardware, environment faults, or missing permissions),
    gracefully skip/ignore the test via `Assume.assumeTrue` rather than causing
    false-positive test suite failures on the advanced feature.
  - **Sequential Sub-Feature Isolation & Diagnostic Reporting**: When verifying
    multiple permutations, modes, or sub-streams (e.g., testing multiple
    resolutions, multiple extension modes, or physical sub-sensors), evaluate
    each sub-stream sequentially with individual fault boundaries so that a
    single failing sub-stream does not mask the health of the remaining
    streams. Log structured failure summaries with the **Parent ID / Lens
    Facing**, failing **Sub-Feature / Stream Parameters**, and a copy-pasteable
    **Suggested Quirk Entry** to streamline future triage.

#### 3. Execution & Validation
- **Local Compile Check**: Compile the entire CameraX project tests and debug
  APKs using:
  ```bash
  ./gradlew -p camera assembleAndroidTest assembleDebug
  ```
  Alternatively, use `PROJECT_PREFIX` to scope compilation to specific modules
  to save time:
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
    ./gradlew :camera:camera-core:testReleaseUnitTest \
      --tests "androidx.camera.core.streamsharing.StreamSharingTest.methodName"
    ```
    > [!WARNING]
    > While running a single method is faster during development, always run the
    > **full test class** (e.g.,
    > `--tests "androidx.camera.core.streamsharing.StreamSharingTest"`)
    > before committing to catch inter-test leaks or side effects.
  - **Device Tests**: Run instrumented tests on a connected device using:
    ```bash
    ./gradlew <project>:connectedCheck
    ```
- **FTL Run (if no device connected)**:
  - Discover FTL tasks for your project:
    `./gradlew <project>:tasks --all | grep ftl` (e.g.,
    `ftlpixel2api30debugAndroidTest` for apps, or `releaseAndroidTest` variants
    for libraries).
  - Standard AndroidX presubmit FTL emulator tasks are
    `ftlmediumphoneapi35debugAndroidTest` (API 35) and
    `ftlnexus5xapi24debugAndroidTest` (API 24) for test apps (or the
    corresponding `releaseAndroidTest` variants for library modules).
  - Run a specific test in FTL using `--className`:
    ```bash
    PROJECT_PREFIX=:camera:integration-tests:camera-testapp-core \
    ./gradlew \
      :camera:integration-tests:camera-testapp-core:ftlpixel2api30debugAndroidTest \
      --className \
      androidx.camera.integration.core.StreamSharingTest#recordingCanProceedAfterSiblingUnbind
    ```
- **Code Quality**: Format modified Kotlin files using `ktfmt` (see General
  Instructions) and run Lint (specifically `lintRelease` to catch release-only
  issues) before committing:
  ```bash
  ./gradlew <project>:lintRelease
  ```
- **Universal Clean-Context Multi-Model Verification Loop (Step 6 Checkpoint
  2)**: Before committing edits or delivering any root-cause investigation,
  implementation plan, test verification report, or trade-off / quirk-scoping
  synthesis, execute the canonical **Step 6** verification loop (Modes 1–5). If
  any semantic code edit is made during or after review, recursively re-verify
  in a fresh clean context and re-run compile, test, `ktfmt`, and `lintRelease`
  checks on the final diff before declaring the task complete.


#### 4. Troubleshooting Unit Test Leaks (Robolectric)
- **Symptom**: `IllegalStateException: Camera surface session should only fail`
  `with request cancellation. Instead failed due to:`
  `FutureGarbageCollectedException: The completer object was garbage`
  `collected...`
- **Root Cause**: A test binds a UseCase to a `FakeCamera` (or real camera) but
  does not properly detach it before the test finishes. This leaves the camera
  session active and leaks internal `DeferrableSurface` termination futures.
  When GC runs in subsequent tests, these leaked futures are collected,
  throwing exceptions in unrelated tests.
- **Solution**: Always ensure proper cleanup in `@After` / `tearDown()` block
  of your test class:
  - If using `FakeCamera` directly, call
    `camera.detachUseCases(listOf(useCase))` before unbinding the use case.
  - Ensure the main looper is idled after cleanup:
    `shadowOf(getMainLooper()).idle()`.


#### 9. Solution Engineering & Test Pyramid Modernization Protocol

When evolving CameraX architecture, refactoring legacy components, or optimizing
test execution across the development lifecycle, apply this 4-step protocol to
engineer robust, maintainable solutions while preserving hardware testability:

1. **Deconstruct Algorithmic Logic from Hardware Platform Dependencies**:
   - Complex configuration pipelines (e.g. video/audio profile resolvers, stream
     combination sort algorithms, dynamic range mapping, format selection)
     often entangle pure algorithmic calculations with physical HAL queries.
   - Separate pure deterministic calculations (resolving bitrates, sample rates,
     clamping ranges, sorting candidate resolutions) from hardware state
     accessors (`CamcorderProfile`, `CameraCharacteristics`, `MediaCodecList`).
   - Pure algorithmic and resolver logic can be tested deterministically across
     dozens of permutations on the host JVM without device flakiness, while
     hardware interaction is cleanly isolated behind minimal interfaces.

2. **Review-Driven Refactoring & Domain Boundary Hygiene**:
   - Maintain strict domain boundaries: constants and domain semantics belonging
     to one media format (e.g. video profile bitrates vs. audio bitrates, or
     camera sensor properties vs. encoder profiles) must never leak across
     format boundaries.
   - Heed reviewer signals: when code reviews reveal out-of-place constants,
     leaky abstractions, or misplaced defaults, resist applying ad-hoc patches
     or local suppressions. Instead, treat reviewer feedback as a prompt for
     structural refactoring that places domain knowledge in its rightful owner.

3. **Two-Tier Test Modernization & The Single Smoke Test Guardrail**:
   - To optimize CI lab resources and accelerate local developer velocity, apply
     the test pyramid:
     a. **Host-Side Migration (Robolectric / JVM)**: Migrate pure calculation,
        data specification (`MediaSpec`, `OutputOptions`), buffer manipulations,
        and profile resolver permutation suites from `androidTest/` to `test/`.
     b. **The Single Smoke Test Guardrail**: When migrating resolver or config
        test suites from `androidTest` to `test` (Robolectric), NEVER completely
        eliminate device-level testing if the component interfaces with real OEM
        framework profiles (`CamcorderProfile`, `MediaCodecList`). Always retain
        at least **one end-to-end smoke test on real devices** in `androidTest/`
        (e.g. verifying that resolving profiles against real device hardware
        produces valid, non-crashing configurations). This ensures real OEM
        hardware idiosyncrasies remain guarded while offloading combinatorial
        testing to fast host unit tests.

4. **Cross-Repository Downstream CI Synchronization**:
   - Maintain lifecycle awareness across repository boundaries. When migrating,
     renaming, or deleting test classes in `androidTest/`, downstream continuous
     testing suites may depend on these target mappings.
   - When internal tooling is configured, refer to `AGENTS_INTERNAL.md`
     (Section 4.5) to audit and synchronize downstream test target definitions
     simultaneously to prevent broken references in daily CI pipelines.



---

> [!IMPORTANT]
> **Cross-File Refresh & Size Restriction Guardrail**:
> - **Single-Call `read_file` Budget**: Keep this `SKILL.md` strictly under
>   `45,000 bytes` and `780 lines` (`46,080 B / 800-line` single-read limit),
>   and keep `camera/AGENTS.md` under `24,000 bytes` (`MaxRuleContentBytes`
>   auto-load limit). Wrap prose lines at `<= 80` characters and maintain 100%
>   public AOSP purity.
> - **Cross-File Refresh**: Whenever you update this file, check whether
>   `camera/AGENTS.md` or the sibling `camera/.agents/skills/*/SKILL.md` files
>   (and `AGENTS_INTERNAL.md` if internal tools are configured) need
>   synchronized updates to section references, routing tables, or shared
>   rubrics. Follow
>   `camera/.agents/skills/camerax-guidelines-governance/SKILL.md`.
