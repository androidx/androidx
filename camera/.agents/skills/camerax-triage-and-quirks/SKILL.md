---
name: camerax-triage-and-quirks
description: >-
  Diagnoses CameraX device-specific test failures and guides root-cause
  classification, Simulation Rubric evaluation, Quirk scoping, and the 6-Phase
  Capability Framework. Use when triaging device-specific failures, deciding
  between core fixes and device quirks, or designing hardware capability
  abstractions. Don't use for general unit test execution (use camerax-testing)
  or formatting/commit workflows (use camerax-guidelines-governance).
---

# CameraX Device-Specific Triage, Root-Cause Rubric & Capability Framework

#### 5. Troubleshooting Device-Specific Failures & Daily Test Triaging

> [!IMPORTANT]
> Many issues in CameraX are device-specific due to variations in camera
> hardware and HAL implementations. Always consider whether a failure is
> device-specific, an SoC family limitation, or a general framework issue, and
> do not assume it will behave the same way on all devices or test
> environments.

- **Symptom**: A failure (test failure, crash, or unexpected behavior) occurs
  on specific device models, while working correctly on others, or appears in
  daily automated CI runs.
- **Investigation Steps & Blast-Radius Analysis**:
  1. **Identify Test & Check Daily Suite Status**:
     - Query historical test runs across the physical device fleet over the past
       14 days to determine the issue scope:
       - `[SINGLE_DEVICE]`: Isolated failure on one model (likely vendor HAL or
         tight timeout).
       - `[MULTI_DEVICE_SUBSET]`: Failures across specific SoC or OEM families
         (e.g., Samsung Snapdragon, MediaTek, Exynos, Pixel).
       - `[FLEET_WIDE_ALL_DEVICES]`: Generic regression affecting the entire
         device fleet (>80% failure rate).
     - When internal test tooling is configured, use test runner query
       options (see `AGENTS_INTERNAL.md`) to extract blast radius, AI
       diagnostic insights, and culprit CLs.
  2. **Analyze Failure Point & AI Diagnostic Insights**:
     - Identify the exact failure line and preceding events in the logs.
     - Inspect pre-triaged AI diagnostic verdicts stored in CI test artifacts
       or build metadata (when internal tooling is configured, see
       `AGENTS_INTERNAL.md`) or inspect raw logcat traces and bugreports.
  3. **Compare Logs Across Hardware**: Compare the failing log with a passing
     log from a healthy device to identify differences in HAL behavior or
     timing.
  4. **Determine Temporal Trend & Flakiness**:
     - Classify as `[NEW_REGRESSION]` (previously passing, recently failing),
       `[FLAKY_INTERMITTENT]` (intermittent passes and fails), or
       `[CONSISTENT_FAILURE]`.
     - For flaky tests, run multiple repetitions (5x–10x) across affected models
       to establish statistical confidence before declaring a fix.
  5. **Determine Component Level**: Check if it's a test infrastructure issue
     (e.g., too tight timeout) or a real library/HAL issue (e.g.,
     `Connection timed out` from `libcameraservice` suggesting HAL freeze).
- **Resolution & Verification Strategy**:
  1. **Test-Level Issues**: If the issue is due to timing or environment,
     increase timeouts or improve test robustness (e.g., add retry or polling).
  2. **Library/HAL Issues**:
     - Check if the behavior is a known device limitation (e.g., some physical
       lenses not supporting reprocessing).
     - Explore if a generic workaround is possible without session
       reconfiguration.
     - **Format/Size Quirks**: If the failure is format-specific (e.g., RAW
       capture crashing):
       a. Check if it's a resolution-specific mismatch (which might be corrected
          by excluding the buggy size via `ExcludedSupportedSizesQuirk`).
       b. If the format is fundamentally broken for all sizes (e.g., HAL
          advertised sizes do not align with physical sensor size required by
          `DngCreator`), disable the format entirely via
          `UnsupportedFormatsQuirk`.
     - **Quirk Precision & Empirical Scope (Zero Magic Assumptions)**: When
       implementing or expanding any `DeviceQuirk` (e.g. format exclusions, size
       filters, physical camera exclusions, ZSL disablers, or surface
       combination overrides):
       a. **Zero Magic Heuristics**: Avoid arbitrary catch-all magic sets,
          speculative ranges, or broad wildcard regexes (e.g. guessing
          resolution ranges or arbitrary ID sets). Every quirk mapping MUST be
          backed by reproducible empirical diagnostics on physical devices.
       b. **OEM Implementation Diversity Awareness**: Never assume all vendor
          HALs follow identical internal conventions (e.g., capability key
          availability, stream ID allocation, or metadata delivery timing). For
          example:
          - *ID Conventions:* Pixel & Samsung assign physical sub-camera IDs
            starting at `"2"`, whereas Sony Xperia assigns physical ID `"0"` to
            the primary physical sensor underlying logical rear camera `"0"`.
          - *Metadata Delivery:* Certain vendor HALs only support fused
            multi-camera engine pipelines and omit individual stream metadata
            callbacks.
       c. **Diagnostic Logging for Quirk Discovery**: Instrument tests to print
          clear, copy-pasteable Quirk mapping code snippets when a hardware
          incompatibility is encountered, enabling instantaneous reproduction
          and triage for future maintainers.
     - **Quirk as Last Resort**: If it is a HAL bug and no generic workaround
       is possible, add the device model to the corresponding Quirk class
       (e.g., `ZslDisablerQuirk`). Ensure all affected devices are included.
  3. **Multi-Device Lab Verification**:
     - When a fix is implemented, **always run the verification test across ALL
       affected device models** identified in the blast-radius analysis (not
       just the single reported device) using the multi-device lab test runner
       (see `AGENTS_INTERNAL.md`).
  4. **Fleet-Wide Baseline Regression Check**:
     - Before submitting changes that touch shared pipeline paths, query the
       daily testing baseline matrix across the physical test device fleet to
       ensure clean pass rates and prevent regressions.

#### 6. Decision Framework: User Impact vs. Test-Only Deep Root-Cause Triage

When encountering test failures, crashes, or timeouts—especially in daily CI
runs or on specific lab devices—agents must NOT jump directly to superficial
test skips (`assumeFalse`) or immediately author a `DeviceQuirk`. Apply this
rigorous evaluation rubric to uncover the true root cause and prioritize
preventing end-user failures in production.

##### A. User Impact vs. Test-Only Scenario Evaluation (The Simulation Rubric)
The fundamental first question when diagnosing any failure is:
**Does the failing test simulate an operation that real end-user apps
could perform, or is it purely an artifact of test harness mechanics?**

1. **User-Representative Scenarios (Production Impact)**:
   - *Characteristics*: Rapid lifecycle state changes (`RESUMED` -> `PAUSED` ->
     `RESUMED`), fast camera switching, capturing images while backgrounding or
     teardown is in flight, layout inflation with unmeasured views before layout
     passes (`CameraController` + `PreviewView`), concurrent UseCase binding.
   - *Guiding Principle*: Even if exercised within an aggressive "stress test"
     or "fragment test", **real users perform these operations in real apps**.
   - *Mandate*: **NEVER** treat these as test-only issues by adding
     `assumeFalse`, skipping the test, or writing ad-hoc test workarounds.
     Prioritize preventing user-facing failure in production by fixing CameraX's
     core asynchronous state machine, error propagation, or view readiness
     guarantees.
   - *Case Study (Asynchronous Lifecycle Cancellation)*: In high-frequency
     capture stress testing, capture requests failed with "Capture request is
     cancelled on closed CameraGraph". While exercised in a stress test, real
     users frequently background apps or switch cameras while capturing photos.
     The fix was properly propagating `ERROR_CAMERA_CLOSED` in CameraX core,
     allowing CameraX's built-in retry mechanism to automatically retry and
     succeed on the reopened camera without ad-hoc device quirks or test skips.

2. **Artificial Test Harness Artifacts (Test-Only Constraints)**:
   - *Characteristics*: Injecting synthetic sensor test patterns
     (`SENSOR_TEST_PATTERN_MODE_SOLID_COLOR` in `SensorPatternUtil`) to verify
     pixel colors in a dark lab cabinet, mocking system services, artificial
     test delays, or synthetic CTS harness mocks.
   - *Guiding Principle*: Real consumer apps capture real physical scenes; they
     never invoke synthetic hardware test patterns.
   - *Mandate*: First verify whether the actual user-facing feature
     (`CameraEffect` processing real frames) is 100% functional on the physical
     hardware. If the underlying feature works for real users, and only the
     artificial test harness assertion fails due to vendor HAL reporting
     discrepancies, it is appropriate to update or scope the test harness
     assumption (`SensorPatternUtil`).

3. **Unrealistic Test Construction (Test Not Written Like Real User Behavior)**:
   - *Characteristics*: The test asserts stream states or queries view
     dimensions before the Android view hierarchy has executed its first
     measure and layout pass (`width=0, height=0`), or asserts impossible
     zero-millisecond synchronization contrary to the Android View lifecycle.
   - *Action*: Update the test to follow realistic application patterns (e.g.
     awaiting `PreviewView` layout readiness via `waitUntilPreviewViewIsReady()`
     before querying view dimensions in Fragment tests).

##### B. The 3-Step Root Cause & Prevention Hierarchy (The Deep "Why" Chain)
When investigating why an operation failed, systematically investigate these
three layers in order:

```
[Test Failure / Defect Reported]
               │
               ▼
[Step 1: Internal Implementation Audit]
 Did CameraX implement the contract correctly?
 (State machines, error mapping, retry logic, async readiness)
   ├─► NO  ──► [FIX CAMERAX CORE] (Retry logic, async lifecycle readiness)
   └─► YES ──► [Step 2: Specification & Capability Audit]
                Did CameraX query standard platform capabilities before use?
                (CameraCharacteristics, StreamConfigurationMap keys)
                  ├─► NO  ──► [UPDATE CAMERAX TO QUERY CAPABILITY]
                  └─► YES ──► [Step 3: Hardware / Vendor HAL Audit]
                               Is the device HAL falsely advertising support?
                                 ├─► Impacts Users ──► [AUTHOR DEVICE QUIRK]
                                 └─► Test-Only     ──► [SCOPE TEST HARNESS SKIP]
```

1. **Step 1: Did CameraX implement the contract correctly? (Internal Audit)**
   - Investigate CameraX's internal state machines, lifecycle transitions,
     error propagation, and retry mechanisms.
   - Did CameraX fail to retry an operation, misclassify an internal error
     code, or fail to handle asynchronous state changes?
   - *Example*: In `UseCaseCameraRequestControl`, returning
     `ERROR_CAPTURE_FAILED` instead of `ERROR_CAMERA_CLOSED` when the
     `CameraGraph` closed broke `StillCaptureRequestControl`'s retry logic.
     Correcting the error code allowed the request to automatically retry
     when the camera reopened, resolving the issue across all devices without
     any hardware quirks.

2. **Step 2: Did CameraX fail to check device capabilities before use?
   (Specification Audit)**
   - Does the Android Camera2 specification define a capability key (in
     `CameraCharacteristics` or `StreamConfigurationMap`) that CameraX should
     query before issuing requests or configuring sessions?
   - Did CameraX assume a capability without querying `CameraCharacteristics`
     (e.g., supported stream combinations, flash modes, zoom ranges, dynamic
     ranges, or test patterns)?
   - If CameraX neglected to check a standard capability key, update CameraX to
     inspect and respect that capability dynamically across all devices.

3. **Step 3: Is device capability reporting defective (OEM HAL lying)?
   (Hardware/HAL Audit)**
   - When CameraX *does* check the capability (or if no standard key exists),
     but the device HAL falsely advertises support while failing or freezing
     at runtime:
     - **Sub-case 3A: Identifiable in advance / Impacts production users:**
       Implement a `DeviceQuirk` under `camera-core` or `camera-camera2` to
       filter the unsupported capability from public queries (e.g.,
       `ImageCaptureCapabilities.getSupportedOutputFormats()`) or adapt the
       pipeline gracefully (e.g., falling back to software blanking/conversion)
       so production apps never crash or fail silently.
     - **Sub-case 3B: Unidentifiable in advance / Test-only artifact:**
       If the issue cannot be identified via platform metadata and only
       impacts artificial test harness constructs (e.g., a front camera sensor
       failing to output synthetic RGB test patterns despite advertising
       `SOLID_COLOR` in `SENSOR_AVAILABLE_TEST_PATTERN_MODES`):
       1. Document the exact OEM sensor/HAL limitation in the issue tracker.
       2. Confirm production user features (`CameraEffect`) work with real
          frames.
       3. Tightly scope the test skip to the affected camera/device in the
          test utility (`SensorPatternUtil`).

##### C. Solution Matrix: Core Fix, Quirk, Test Scope, or Test Cleanup
1. **Core Architecture Fix**:
   - **When**: The issue stems from CameraX internal pipeline transformations,
     surface configuration, state machines, error classification, retry logic,
     or UseCase format and resolution routing.
   - **Action**: Fix directly in core framework abstractions (`camera-core` or
     `camera-camera2`).
2. **Device Quirk**:
   - **When**: The issue stems from OEM vendor HAL non-compliance, driver
     limitations, or hardware capabilities advertised in `CameraCharacteristics`
     that fail at runtime during camera session creation or streaming, and
     impacts real-world applications.
   - **Action**: Implement a `DeviceQuirk` (under `camera-core` or
     `camera-camera2` compat quirks) to gracefully adapt pipeline behavior or
     filter unsupported modes from public capability queries (e.g.,
     `ImageCaptureCapabilities`). **Avoid** adding ad-hoc test assumptions
     (`assumeFalse`) when a quirk can protect production applications.
   - *Case Study*: Exclude unsupported simultaneous multi-stream combinations
     via a quirk on devices where HAL rejects concurrent maximum-resolution
     streams.
3. **Test Harness Utility Scoping**:
   - **When**: The failure occurs purely within an artificial test harness
     construct (e.g. synthetic test patterns) due to an OEM HAL metadata
     defect, and production user features are fully functional.
   - **Action**: Scope the skip within the test harness utility
     (`SensorPatternUtil`) citing the exact device/sensor defect.
4. **Test Assumption Cleanup**:
   - **When**: Previous manual test skips (e.g., `assumeFalse(DEVICE)`) are
     rendered obsolete because a companion quirk or core pipeline fix now
     resolves the root problem.
   - **Action**: Create a dedicated cleanup CL removing the obsolete
     assumptions to re-enable continuous regression testing on physical devices.

##### D. Implementation Correctness vs. Device Quirks (Investigation-First Rule)
   - **Principle**: Always investigate whether CameraX's own implementation,
     parameter configuration, or capability registry is the root cause before
     concluding an issue is an OEM device hardware or HAL defect. **Never jump
     directly to writing a `DeviceQuirk` without first auditing CameraX's own
     logic.**
   - **When to Apply**: When a test failure, crash, timeout, or unexpected
     behavior occurs across multiple device models, an entire OEM fleet, a
     specific Android OS level, or on a newly introduced format/feature (e.g.,
     video codecs, HDR dynamic ranges, high-speed sessions, or stream sharing).
   - **Investigation Checklist Before Considering a Quirk**:
     a. **Capability & Registry Correctness**: Are CameraX's capability
        registries, lookup tables, or default resolvers (e.g.
        `DynamicRangeFormatComboRegistry`, `DynamicRangeUtil`,
        `EncoderProfilesResolver`) advertising invalid combinations or omitting
        required profile/dataspace parameters?
     b. **Specification Compliance**: Does the configuration adhere strictly to
        the underlying Android platform and industry standards (e.g., Android
        `MediaCodec` profiles, Camera2 stream constraints, ISO/IEC
        specifications)?
        *(Case Study: APV codec `video/apv` is an intra-frame 10/12-bit format.
        Erroneously registering it under `buildSdrRegistry()` caused 8-bit SDR
        surfaces to feed a 10-bit encoder without profile keys, producing
        timeouts. The fix was correcting the capability registry, not a quirk).*
     c. **Pipeline & Surface Configuration**: Is CameraX creating the
        appropriate surface format, color space, buffer queue depth, or
        repeating request parameters?
     d. **State Machine & Lifecycle**: Are buffers, surfaces, or encoders being
        prematurely closed, stalled, or failing to receive warmup frames?
   - **Decision Rubric**:
     - If CameraX can fix the behavior by correctly configuring parameters,
       aligning with standards, or avoiding invalid combinations, **fix the
       core implementation**.
     - Only implement a `DeviceQuirk` when CameraX's configuration is 100%
       compliant with Android platform specifications, and the device failure is
       conclusively proven to be an unrecoverable vendor driver/HAL defect.

##### E. Device Quirk OS Version Ceilings (The Upstream OS Upgrade Trap)
When inspecting or authoring quirks with OS version checks (e.g.,
`Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`):
- **Audit Underlying Driver/HAL Persistence**: Verify whether the underlying
  vendor driver or HAL bug is genuinely resolved in newer Android OS releases
  before placing an upper bound on a quirk.
- **The Upgrade Trap**: Hardcoding an upper bound (such as
  `<= UPSIDE_DOWN_CAKE` / Android 14) causes the quirk to automatically
  deactivate when devices upgrade to Android 15 (API 35) or Android 16
  (API 36). If the vendor driver retains the flaw across platform updates,
  this causes regressions in the wild.
- **Triage Protocol for Post-Upgrade Regressions**: When an issue spikes
  immediately after a major Android upgrade (e.g., Android 16 on Samsung
  devices), inspect existing quirks in `androidx.camera.camera2.compat.quirk`
  to check whether an existing quirk previously protected the device family
  but was turned off by an outdated OS version ceiling.

##### F. Preserving Public API Contracts & Documented Invariants
When fixing bugs, handling edge-case configurations, or refactoring internal
plumbing across any CameraX module, treat public Javadoc contracts as strict
behavioral invariants:
- **Honor User-Provided Configurations During Normal Operations**: Never
  silently replace, ignore, or fall back from user-configured parameters (such
  as custom executors or handlers via `CameraXConfig.setCameraExecutor` or
  `setSchedulerHandler`, `ResolutionSelector`s, or `UseCase` builder settings)
  during normal initialization or runtime operations if doing so violates the
  public Javadoc contract.
- **Surgical Lifecycle Scoping for Edge Cases**: When an edge-case user
  configuration is incompatible with a specific internal lifecycle step (e.g.,
  blocking or asynchronous teardown on a direct executor), scope the fallback
  strictly to that incompatible internal step while honoring the user's
  configuration everywhere else.
- **Enforce Documented Query & Precondition Isolation**: When a public query or
  simulation API documents that certain parameters on an input object are
  ignored (e.g., `CameraInfo.getSupportedFrameRateRanges(SessionConfig)`
  ignoring `SessionConfig.getFrameRateRange()`), ensure internal simulators and
  adapters (`UseCaseAdditionSimulator`, `StreamSpecsCalculator`) explicitly
  omit or strip those parameters so they do not circularly constrain the query.

##### G. Mandatory Clean-Context Verification Gate for Triage & Fix Plans
Before implementing production fix code or delivering any root-cause diagnosis,
triage classification (Step 1 Core Fix vs. Step 2 Capability Check vs. Step 3
Device Quirk vs. Sub-case 3B Test-Only Scope), or proposed implementation plan,
agents **MUST** stage the draft analysis and raw evidence paths in a
scratch/temp file and execute **Step 6 Checkpoint 1** (Mode 2: Issue
Investigation Audit and Mode 3: Plan Audit). Each clean-context verifier must
inspect the raw evidence *before* reading the draft plan (two-turn read
barrier), confirm the first causal failure in the raw logs, and enforce the
Section 6.A User-Impact vs. Test-Only Gate; the orchestrating agent must
recursively re-verify any subsequent semantic plan or code edit per **Step 6
Checkpoint 2**.

> [!NOTE]
> **Internal Lab Automation & Fleet Query Tooling**:
> When operating in environments where internal lab tooling is configured with
> access to automated physical device testing labs and continuous test history,
> refer to `AGENTS_INTERNAL.md` (Phase 4 and Section 5.14) for instructions on
> using fleet-wide blast radius analysis, CI diagnostic properties, and
> multi-model verification to operationalize this decision hierarchy.

#### 7. Safety & Code Path Auditing Protocol
Before finalizing changes to shared infrastructure (e.g. `UseCase`,
`ImageInputConfig`, `UseCaseManager`, `SessionConfig`, `Node` pipelines):
1. **Audit Standard Single-Stream Flow**: Verify that standard single-stream use
   cases (`Preview`, `VideoCapture`, `ImageAnalysis`, standard `ImageCapture`)
   execute identical code paths with 100% backward compatibility.
2. **Audit Multi-Stream / Advanced Flows**: Verify that concurrent, dual-stream,
   or composite configurations map surfaces, formats, and sizes accurately by
   stream index.
3. **Audit Binary & API Compatibility**: Verify that public and restricted
   (`LIBRARY_GROUP`) API contracts and binary signatures remain fully backward
   compatible.
4. **Audit Composite State Machines & Coupled Observables**: When adding a
   deferral or early-return branch to a state transition:
   - Verify all entry states so pending teardown/reset signals from the
     previous state are preserved rather than silently dropped.
   - Update any coupled secondary or observable states (e.g. backed-up
     non-pending state, `StreamState.ACTIVE` / `INACTIVE`) alongside the
     primary state so downstream observers and cancellation paths remain
     consistent.

#### 8. Capability & Hardware Abstraction Problem-Solving Framework

When resolving hardware capability, format negotiation, or platform abstraction
issues (e.g. video/audio codecs, dynamic ranges, stream combinations, or
physical camera sensors), agents and developers MUST follow this 6-phase
pathway to engineer robust, architecturally sound solutions:

1. **Deconstruct Operational Asymmetries (Real-Time Surface vs. Byte-Buffer)**:
   - Framework metadata (e.g. `CodecCapabilities.profileLevels` or
     `StreamConfigurationMap`) often reflects offline, CPU-mediated, or
     byte-buffer processing capabilities.
   - Real-time camera feeds (`COLOR_FormatSurface`, zero-copy buffer queues,
     hardware HAL pipelines) have fundamentally different constraints. A codec
     or configuration that works for offline file processing may deadlock or
     trigger fatal IPC disconnects (`DEAD_OBJECT` in `mediaswcodec`) when fed
     real-time camera surfaces.
   - Software fallbacks are rarely designed to sustain real-time camera
     capture. Confirm whether real-time execution requires hardware
     acceleration silicon (VPU/ISP).

2. **Triangulate Ground Truth Across Three Ecosystem Pillars**:
   - **Pillar 1: Authoritative Specifications**: Consult the Android CDD (e.g.
     §5.12 for video encoders, §5.4.1 for audio capture guarantees, §7.5 for
     camera specifications) and underlying standards (3GPP, RFCs). What does
     the platform strictly guarantee or mandate across all compliant devices?
   - **Pillar 2: First-Party Ecosystem Precedent**: Inspect how first-party
     Android libraries handle the same ambiguity (e.g. Media3 Transformer's
     `EncoderUtil`, CTS verifiers, `frameworks/av` source). Match platform
     design patterns (e.g. handling `FEATURE_HlgEditing` alongside
     `FEATURE_HdrEditing`).
   - **Pillar 3: Silicon & HAL Reality**: Determine whether a failure is an
     unrecoverable vendor driver defect or an architectural constraint that
     CameraX can safely avoid through proper capability negotiation.

3. **Calibrate Capability Gating (Avoid Both False Positives and Negatives)**:
   - **The Permissive Trap (False Positives)**: Declaring support based solely
     on advertised profile levels causes native crashes on unsupported
     software emulations.
   - **The Overly Strict Trap (False Negatives)**: Enforcing newly introduced
     platform feature flags (e.g. API 33+ or API 35+ features) indiscriminately
     across all formats breaks functional legacy hardware that works reliably
     on older OS versions (e.g. Android 10–12 devices lacking
     `FEATURE_HdrEditing`).
   - **Surgical Scoping**: Scope new constraints strictly to the modalities
     that require them (e.g. gating hardware editing checks with
     `dynamicRange.is10BitHdr` so SDR variants are never blocked).

4. **Architect Context-Aware Fallbacks (No Blind Constants)**:
   - When device metadata (e.g. `CamcorderProfile`) lacks an entry for a
     requested format, NEVER fall back to a hardcoded generic constant (e.g.
     blindly applying 44.1 kHz / 156 kbps to speech or VoIP codecs).
   - Resolve fallbacks based on target format characteristics (e.g. AMR-NB
     8 kHz, AMR-WB 16 kHz, Opus 48 kHz).
   - Ensure fallback configurations strictly align with platform-guaranteed
     hardware baselines (e.g. CDD §5.4.1 mandatory capture sample rates).

5. **Permutation Testing & Regression Immunity**:
   - Test happy paths (compliant hardware acceleration).
   - Test negative paths (software emulators cleanly filtered out).
   - Test edge-case encodings (e.g. SDR variants, partial feature support).
   - Verify that dominant baseline configurations (AVC/HEVC SDR/HDR, AAC)
     maintain 100% behavioral backward compatibility.

6. **Clean-Context Multi-Model Verification Gate (Step 6)**:
   - Before presenting a capability investigation, architectural proposal,
     code change, multi-device verification report, or quirk-scoping synthesis
     engineered under this framework, pass the draft deliverable and raw
     evidence through the **Universal Clean-Context Multi-Model Verification
     Loop** (Step 5 & Step 6, Modes 1–5; default 3 rounds, auto-scaling up to 5
     rounds while fixes converge) to independently audit specification claims,
     architectural placement, test coverage, and report accuracy.



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
