---
name: camerax-guidelines-governance
description: >-
  Enforces CameraX coding standards hierarchy, recursive guideline traversal,
  Senior CameraX SWE deep codebase study, 4-layer package/object-job placement
  map, multi-path call-chain and Dagger/thread invariants, pre-upload gates, and
  long-term prompt/skill governance rules. Use when answering CameraX
  API/architecture questions, studying module/package responsibilities before
  using internal APIs or placing code, writing or refactoring CameraX code,
  preparing a commit, or updating AGENTS.md and SKILL.md files. Don't use for
  device-specific failure triage (use camerax-triage-and-quirks) or test
  execution mechanics (use camerax-testing).
---

# Project: CameraX

## Standards & Guidelines Hierarchy

When answering technical/API questions, writing new code, reviewing existing
code, or fixing bugs in CameraX, agents **MUST** operate as a **Senior CameraX
SWE**: trace the full end-to-end call chain (`UseCase / View / Video /
Extensions` -> `CameraInternal / CameraUseCaseAdapter / StreamSharing` ->
`camera-camera2` -> `camera-camera2-pipe`), verify `@CameraScope` vs.
`@UseCaseCameraScope` and partial re-bind vs. full unbind, inspect KDoc and
`git --no-lazy-fetch blame` / `log` history, and actively read and adhere to
the relevant guidelines, style guides, and correctness checks before proposing
answers or modifying code (and for any non-flash question or solution proposal,
stage `<scratch_or_tmp>/draft_deliverable.md` and run the Step 6 Clean-Context
Multi-Model Verification Gate in `camerax-code-review`), following the
precedence order (AndroidX → Android Platform → Google-Wide).

### Automatic Recursive Index & Reference Traversal Protocol:
When directed to an index page, directory list, or guide linking to sub-guides
(such as `docs/api_guidelines/index.md`, web style directories like
`https://google.github.io/styleguide/`, or API reference landing pages), agents
**MUST follow this recursive ingestion protocol**:

1. **Read Root Index**: Execute `view_file` (for local files) or
   `read_url_content` (for web URLs) on the root index document.
2. **Extract Child References**:
   - For in-repo files with `<!--#include file=".../(?<file>.*\.md)"-->` or
     links `[...](<relative_path>.md)`: extract all referenced markdown paths
     (e.g., `kotlin.md`, `checks.md`, `async.md`, `annotations.md`,
     `compat.md`).
   - For web index pages with `<a>` links or markdown URLs: extract all relevant
     sub-topic links.
3. **Recursive Ingestion**:
   - Sequentially invoke `view_file` on each extracted in-repo markdown file
     (e.g., in `docs/api_guidelines/`).
   - Sequentially invoke `read_url_content` on each extracted child URL relevant
     to the current task (e.g., Kotlin style guide, Java style guide).
4. **Complete Ingestion Verification**: Ensure all pertinent rules, constraints,
   and code examples from child documents are loaded into active context before
   generating code edits or writing review comments.

### Precedence Order:

1. **1st Priority — AndroidX Standards & Correctness Checks**:
   - **AndroidX Library & API Guidelines**: Follow
     [`docs/api_guidelines/index.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/)
     (recursively read constituent topic files: `kotlin.md`, `checks.md`,
     `async.md`, `annotations.md`, `compat.md`).
   - **Kotlin in AndroidX**: Follow
     [`docs/api_guidelines/kotlin.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/kotlin.md)
     for nullability annotations (JSpecify), data class restrictions in public
     APIs, exhaustive `when`, and Flow return types.
   - **Asynchronous & Non-Blocking Guidelines**: Follow
     [`docs/api_guidelines/async.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/async.md)
     for Coroutines, Flow, and `ListenableFuture` conventions.
   - **Correctness & Linting Checks**: You **MUST** reference and follow
     [`docs/api_guidelines/checks.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/checks.md)
     for lint rules, call-site suppression practices, and baseline policies.
   - **Public API Rules**: Strict adherence to `@RestrictTo(LIBRARY_GROUP)`
     and `./gradlew <project>:updateApi` procedures.

2. **2nd Priority — Android / AOSP Platform Guidelines**:
   - **Android Platform API Guidelines**: Follow the [Android Platform API
     Guidelines](https://source.android.com/docs/setup/contribute/api-guidelines).
   - **Android Kotlin-Java Interoperability**: Follow the [Android Developers
     Kotlin-Java Interop Guide](https://developer.android.com/kotlin/interop)
     for seamless Java/Kotlin cross-language support.
   - **Android Kotlin Style Guide**: Follow the [Android Kotlin Style
     Guide](https://developer.android.com/kotlin/style-guide)
     for formatting, naming conventions, and idiomatic constructs.
   - **AOSP Java Code Style**: Follow the [AOSP Java Style
     Guide](https://source.android.com/docs/setup/contribute/code-style)
     for Java code formatting and naming conventions.
   - **Camera2 Framework Contracts**: Adhere strictly to the official
     [Android Camera2 API specifications](https://developer.android.com/reference/android/hardware/camera2/package-summary).

3. **3rd Priority — Google-Wide Engineering Standards**:
   - **Google Style Guides Index**: Refer to the
     [Google Style Guides](https://google.github.io/styleguide/).
   - **Kotlin Style Guide**: Follow the Google-adopted
     [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
     referenced by the Google Style Guides index.
   - **Google Java Style Guide**: Follow the [Google Java Style
     Guide](https://google.github.io/styleguide/javaguide.html).
   - **Idiomatic Coding Practices**: Prioritize clean, expressive, functional
     designs, avoiding excessive allocations or unnecessary synchronization.

> [!NOTE]
> **Document Relationship for Internal Contributors**: `camera/AGENTS.md` is
> the primary public baseline for code architecture, style, and lint policies.
> When operating in an environment where internal tools are configured, refer
> to `AGENTS_INTERNAL.md` (at `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md`,
> or located via `code_search` query `f:camerax.*AGENTS_INTERNAL\.md$` when
> `$CAMERAX_INTERNAL_TOOLS_DIR` is unset) and its sibling `skills/` directory
> for supplementary coding guidelines, documentation links, and physical lab
> test runner commands.


## Development Workflow & Refactoring:

- **Parent AndroidX Baseline (`frameworks/support/AGENTS.md`) & Workspace Modes
  (`repo` vs. CoG)**:
  - When operating in an AndroidX checkout where the repository-root `AGENTS.md`
    and the repo-wide `manage_commits` and `manage_cl_workspaces` skills are
    auto-discovered by upward directory traversal, use the parent AndroidX
    workflow definitions as the baseline (including `gob_vcs` in CoG
    `/google/cog/cloud/...` workspaces where `repo` is not used, grepping
    `settings.gradle` instead of `./gradlew projects`, `<project>:javaFormat`,
    and `Relnote: N/A` for internal `src/main/` or `src/commonMain/` changes),
    while applying CameraX's stricter commit limits (`<= 50` char title) and
    Stage 1 + Stage 2 Pre-Upload Gate below. If operating standalone without
    parent skills, use the standard `repo` defaults below.
- **Pre-Flight Upstream Sync, Agent-Checkout Stash/Branch Preservation & AOSP
  Feature Branch Creation (`repo start` vs. `git branch` in Standard `repo`
  Checkouts)**:
  - In standard `repo`/Git checkouts, before starting root-cause analysis,
    running test scans, or creating a feature branch:
    1. Never auto-clean or modify a developer's personal AOSP checkout without
       explicit permission. Automatic pre-flight cleanup applies **only** to an
       agent-managed checkout (initialized by the agent via `repo init` and
       `repo sync`, or when the user explicitly opts in): stash any leftover
       uncommitted or untracked scan/task files
       (`git stash push -u -m "camerax-auto-stash-..."`) so work is preserved
       without blocking sync or builds.
    2. Fetch the latest upstream manifest branch (`git fetch aosp androidx-main`
       and `git log --oneline HEAD..aosp/androidx-main -- camera/`), preserve
       any detached unmerged commits on a `wip-preserve-<sha>` branch while
       keeping local unmerged CL branches intact, and optionally prune local
       branches already merged into `aosp/androidx-main` (`git branch -d
       <branch>`) only when the user opts in (`--prune_merged_branches`).
    3. Sync `frameworks/support` and `prebuilts/androidx/internal`
       (`repo sync -c -j8 frameworks/support prebuilts/androidx/internal`) and
       detach/switch `HEAD` to clean `aosp/androidx-main` in the agent-managed
       checkout so builds and troubleshooting never get blocked.
  - Prefer using `repo start <branch_name> .` when creating a new feature
    branch in a standard `repo` checkout. `repo start` automatically establishes
    upstream tracking against the manifest remote (`aosp/androidx-main`),
    ensuring seamless integration with `repo upload`.
  - If creating a branch via `git checkout -b <branch_name>`, always explicitly
    configure upstream tracking immediately:
    `git branch --set-upstream-to=aosp/androidx-main <branch_name>`.
- **Backend Architecture & CameraPipe Migration (`Camera2Config`)**:
  - **CRITICAL ARCHITECTURAL CONTEXT**: The `camera-camera2` implementation
    module has been migrated to use `camera-camera2-pipe` (CameraPipe) as its
    underlying backend engine.
  - While public entry points like `Camera2Config.defaultConfig()` retain the
    `Camera2` naming for backward compatibility with existing applications, they
    instantiate a `CameraFactoryProvider` backed by `CameraPipe` (`CameraGraph`,
    `Camera2CameraController`, `Camera2DeviceManager`).
  - **DO NOT** assume `Camera2Config` implies the legacy pre-migration Camera2
    pipeline or that CameraPipe is an optional or experimental alternative;
    CameraPipe **is** the default active engine driving `camera-camera2`.
  - When debugging device quirks, capture session lifecycle, or frame streaming
    issues, look for CameraPipe concepts (`CameraGraph.Config`, `CXCP` logcat
    tags, `Camera2DeviceCloser`, `CloseCameraDeviceOnCameraGraphCloseQuirk`)
    within `camera-camera2`.
  - **Module Ownership & Lifecycle Scope Invariants (`@CameraScope` vs.
    `@UseCaseCameraScope`)**:
    - Prioritize fixing CameraX use-case, surface, and session contracts in
      `camera-camera2` or `camera-core` before modifying low-level
      `camera-camera2-pipe` primitives.
    - Distinguish `@CameraScope` components (`CameraInternalAdapter`,
      `CameraControlAdapter`, `UseCaseManager`, wired in `CameraConfig` and
      living across the camera's lifetime) from `@UseCaseCameraScope`
      components (`UseCaseCamera`, `UseCaseCameraRequestControl`,
      `CapturePipeline`, `UseCaseSurfaceManager`, and the per-session
      `CameraGraph`, wired in `UseCaseCameraConfig` and recreated per binding
      session). Never retain a `@UseCaseCameraScope` reference inside a
      `@CameraScope` owner without invalidating on re-bind.
    - Distinguish **partial UseCase re-binding** (`useCases` remains non-empty
      and surfaces migrate across consecutive `UseCaseCamera` instances) from
      **full unbind / teardown** (`useCases.isEmpty()` or `close()`). Never tie
      camera-wide surface/resource teardown to a single transient
      `@UseCaseCameraScope` session without guarding against stale callbacks
      cancelling newly active sessions.
  - **4-Layer Architecture, Package Responsibility ("Object Job") & Pre-Coding
    Study (Full Map in `camerax-testing/SKILL.md` Section 1)**:
    Before calling internal helpers or placing code, verify the target package's
    intended responsibility across CameraX's 4 layers and read the complete
    **4-Layer Package Responsibility Map, Multi-Path Call-Chain Anchors,
    Pre-Placement Checklist, and Pre-Coding Test Study** in
    `camerax-testing/SKILL.md` (`#### 1. Research, Code Structure & Context
    Gathering (Before Modifying Code)`):
    1. **Layer 1 (`camera-lifecycle`, `camera-view`, `camera-compose`,
       `camera-viewfinder`, `camera-extensions`)**: Lifecycle binding
       (`ProcessCameraProvider`), UI controllers/widgets (`CameraController`,
       `PreviewView`, `CameraXViewfinder`, `Viewfinder`), and OEM extension
       delegation (`VendorExtender` vending a `camera-core` `SessionProcessor`
       via `createSessionProcessor()`). Never embed core session state machines
       or HAL workarounds here.
    2. **Layer 2 (`camera-core`, `camera-video`, `camera-effects`)**: `core/`
       (public APIs in `api/current.txt`), `core/impl/` (`@RestrictTo` SPIs in
       `api/restricted_current.txt`), `core/internal/` (binding & stream spec
       calculation like `core.internal.CameraUseCaseAdapter`,
       `StreamSpecsCalculator`), and domain pipelines (`imagecapture/`,
       `processing/`, `streamsharing/`, `featuregroup/`, `resolutionselector/`,
       and `camera-video` `Recorder`/`video/internal/`).
    3. **Layer 3 (`camera-camera2`)**: `config/` (Dagger `CameraConfig`
       `@CameraScope` vs. `UseCaseCameraConfig` `@UseCaseCameraScope`),
       `adapter/` (`CameraInternalAdapter`, `CameraControlAdapter`,
       `SupportedSurfaceCombination`, and `camera2.adapter.CameraUseCaseAdapter`
       for `UseCaseConfigFactory`—distinct from
       `core.internal.CameraUseCaseAdapter`), `impl/` (`@UseCaseCameraScope`
       `UseCaseCamera` / `CapturePipeline` alongside `@CameraScope`
       `UseCaseManager` / `StillCaptureRequestControl`), `internal/` (unscoped
       resolvers like `DynamicRangeResolver`, `StreamUseCaseUtil`),
       `compat/quirk/` & `compat/workaround/` (per-module `DeviceQuirks` vs.
       `CameraQuirks` + encapsulated workaround consumers), and `interop/`.
    4. **Layer 4 (`camera-camera2-pipe`)**: Low-level `CameraGraph`,
       `GraphLoop` / `CaptureSequenceProcessor`, and `Camera2DeviceManager`.
- **Mandatory Pre-Coding Guideline Reading & Recursive Traversal**: Before
  modifying or creating code, agents MUST use `view_file` to read the relevant
  local guideline files into active context. When encountering index pages,
  summaries, or links to companion documents, agents MUST recursively follow
  and read the referenced files directly:
  - For Kotlin coding and API design: read `docs/api_guidelines/kotlin.md`
  - For linting and suppression policies: read `docs/api_guidelines/checks.md`
  - For coroutines, Flow, and threading: read `docs/api_guidelines/async.md`
  - For index pages (e.g. `docs/api_guidelines/index.md` or style guide lists):
    follow and read the specific linked sub-guides applicable to the current
    task.
- **Language**: Prefer Kotlin to Java for new files. When migrating files,
  convert them from Java to idiomatic Kotlin following AndroidX and Android
  Kotlin-Java interop guidelines.
- **Coding Style & Elegance**: Adhere to the code style guides in precedence
  order: AndroidX (`docs/api_guidelines/kotlin.md`), Android Kotlin Style Guide,
  and Google Kotlin/Java Style Guides.
- **API Design**: For public API design, follow the [Android API
  guidelines](https://source.android.com/docs/setup/contribute/api-guidelines)
  and the
  [AndroidX API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/).
  New APIs should prioritize Kotlin users over Java users while still ensuring
  they are easy to use from Java. For more details, see
  https://developer.android.com/kotlin/interop.
- **New Public APIs**: If a new public API needs to be added and the current
  project version is not an alpha version (e.g., it is in beta or rc), do NOT
  bump the version yourself. Instead, mark the new API with
  `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` and add a `TODO` comment right
  above it with the bug ID, e.g.,
  `// TODO: b/1234567 - Make this public in next alpha`.
- **Public API Contract Fidelity (Honoring User Configs & Documented
  Invariants)**:
  - Before modifying internal plumbing, adapters, or simulators, always inspect
    the public Javadoc of all entry-point APIs (`CameraInfo`, `CameraXConfig`,
    `SessionConfig`, `UseCase` builders) that feed into that code path.
  - **Never Violate Public Contracts as a Shortcut**: When fixing edge-case
    configurations, never silently override, ignore, or repurpose user-provided
    configurations (such as custom `Executor`s, `Handler`s, selectors, dynamic
    ranges, or callbacks) during normal operations in a way that contradicts
    their public Javadoc. Scope any internal workaround strictly to the specific
    lifecycle phase that requires it (e.g., async teardown).
  - **Enforce Documented Query & Simulation Isolation**: When a public
    capability or query API documents that specific settings on an input config
    are ignored (e.g., `CameraInfo.getSupportedFrameRateRanges(SessionConfig)`
    ignoring a target frame rate already set on `SessionConfig`), ensure
    internal simulators and calculators strip or bypass those settings so they
    cannot leak into and constrain the query result.
- **Linting & Correctness Checks**: Ensure code quality and adherence to
  AndroidX standards by running `./gradlew <project>:lintRelease` after
  completing a meaningful set of changes. Developers and reviewers **MUST**
  reference and adhere to
  [`docs/api_guidelines/checks.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/checks.md)
  for suppression rules and baseline policies.
- **Code Elegance & Idiomatic Design**: After implementing a functional solution
  and passing tests, always conduct an elegance review across all code changes:
  - **Wide Structural Design, Object Job & Code Placement**: Never evaluate only
    the modified lines. Read the surrounding CameraX classes, package structure,
    and callers widely to verify each class's single responsibility ("object
    job"), confirm new logic is placed in the right object/layer, and ensure any
    intentional existing behavior or workaround is preserved.
  - Prefer idiomatic Kotlin functional operations (e.g., `.filter`, `.map`,
    `.filterNotNull()`) over verbose imperative loops where appropriate.
  - **Deep Modules & Eliminating Special Cases**: Favor deep modules (simple,
    general-purpose interfaces hiding complex internal state), separate
    general-purpose core mechanisms from special-purpose policies/quirks, and
    design normal state transitions (e.g., empty collections, idempotent
    teardown) to handle edge conditions naturally without special-case
    branching, duplicate conditions, or dead code paths.
  - **Thread-Aware Initialization (`lazy` vs. Eager)**: Evaluate the use of
    `by lazy` on a case-by-case basis considering the component's lifecycle and
    calling threads:
    - If a property might be accessed on the main thread (e.g., public query
      methods), but its initialization involves waiting on asynchronous
      operations or heavy metadata loading, avoid `by lazy` because first
      access on the main thread could cause jank or blocking. If the enclosing
      class is constructed during a background initialization phase (e.g.,
      CameraX background init worker like `CameraInfoAdapter`), prioritize
      eagerly initializing that data in the background constructor, documenting
      the background thread-safety clearly in comments.
    - Conversely, use `by lazy` when initialization is genuinely deferred for
      optional or rarely accessed code paths, or when initialization is
      lightweight and safe on any calling thread.
  - Extract complex logic into well-named private helper functions for maximum
    readability.
- **Dependency & Constructor Simplification**: For lightweight compatibility
  layers, wrappers, or utility classes, prefer instantiating internal helper
  dependencies internally (rather than passing them as constructor parameters)
  to keep the API clean and reduce boilerplate for callers, provided it does
  not hinder testability. Avoid over-engineering constructors with parameters
  that are primarily implementation details of the class.
- **Regression Prevention**: Scan the codebase and analyze the impact of your
  changes on related components to ensure no potential regressions are
  introduced.
- **Refactoring & Caller Updates**: When modifying the signature or behavior of
  a class, constructor, or method (especially public or internal APIs used
  across modules), you **MUST** actively scan the codebase to identify and
  update all callers and corresponding usages. Verify that all affected modules
  compile successfully. Be thorough and ensure you update:
  a. Production code callers.
  b. Host-side unit tests (typically under `src/test/`).
  c. Device-side integration/instrumented tests (typically under
     `src/androidTest/`).
- **Documentation & KDoc Updates**: When modifying a class, interface, method,
  or property (especially when changing constructor signatures, parameters, or
  public/internal behaviors), always review and update its KDoc/JavaDoc. Ensure
  the documentation accurately reflects the new behavior and signature.
- **Camera2 API Usage**: When writing code that utilizes Android Camera2 APIs
  (directly or indirectly, including modifying behavior that relies on them),
  always revisit the official
  [Android Camera2 API reference](https://developer.android.com/reference/android/hardware/camera2/package-summary)
  to check the API usage guidelines and contracts before finalizing the code
  change, ensuring all usage aligns with the framework's design.
- **Upfront Multi-Model Root-Cause & Solution Exploration (Step 6 Checkpoint 1
  Gate)**:
  When investigating a non-trivial bug, concurrency race, HAL/codec failure, or
  architectural change, do **not** jump from a single model's first hypothesis
  straight to implementing production fix code. Execute **Step 6 Checkpoint 1**
  (Modes 2, 3, and 5—with evidence-first blind diagnosis, the Section 6.A
  User-Impact vs. Test-Only Gate, and Deep-Module Simplicity) before coding the
  fix or finalizing any plan, followed by **Step 6 Checkpoint 2** and Stage 2
  verification on the resulting diff.
- **Mandatory Two-Stage Verification & Pre-Upload Quality Gate**:
  Never skip verification or declare a task complete without executing both
  stages:
  - **Stage 1: Pre-Finalization Deep Self-Review & Universal Multi-Model
    Verification Loop**:
    Before declaring any code modification, root-cause investigation,
    implementation plan, test verification report, or design trade-off /
    quirk-scoping synthesis complete, agents **MUST** conduct a full diagnostic
    audit against the **Code Review Guidelines** at the top of this file,
    including the **Universal Clean-Context Multi-Model Verification Loop** in
    Step 5 & Step 6:
    1. *Wide Structural Design, Object Job & Intentionality:* Read surrounding
       CameraX classes widely (not just changed lines) to verify structural
       design, object responsibility, code placement elegance, and preservation
       of intentional code.
    2. *Concurrency & Latches:* Latches guaranteed release in `finally`, no Main
       thread blocking.
    3. *Resource Leaks:* `imageProxy.use { ... }`, bitmap recycling,
       `clearAnalyzer()`, use cases unbound in `finally`.
    4. *Modern APIs:* No deprecated API usage paired with
       `@Suppress("DEPRECATION")` when modern replacements exist.
    5. *Strict Line Lengths:* 80 chars for markdown, 72 chars for commit body,
       100 chars for Kotlin.
    6. *Default Multi-Model Verification Loop (Step 6 Checkpoint 1 & Checkpoint
       2):* Run deterministic checks (`ktfmt`, compile, unit tests) before
       spawning verifiers and execute the canonical Step 6 autonomous
       convergence loop (cross-family verifiers, zero-priming briefs, two-turn
       read barrier, per-CL + stack-level granularity, ephemeral defect ledger,
       and priority repair with pattern sweep until the latest round reports `0`
       `OPEN`, `0` `REGRESSED`, `0` `PATCHED_PENDING_REAUDIT`, and `0`
       unadjudicated `DISMISSED_WITH_PROOF` items). If Stage 2 requires any
       semantic code fix, loop back to Stage 1 (counting toward the same 5-round
       cap) before uploading.
  - **Stage 2: Pre-Upload Full Verification & Tooling Gate**:
    Before committing or uploading a CL (`repo upload`), you **MUST** execute:
    1. **Kotlin Formatting**:
       `./gradlew :ktCheckFile --format --file <modified_file.kt>` (or
       `:camera:camera-<module>:ktFormat`).
    2. **Java Formatting, Checkstyle & Import Order**: Format modified Java
       files via `./gradlew :camera:camera-<module>:javaFormat` (revert any
       unrelated file formatting in `git diff`), and verify strict alphabetical
       Java import ordering without unused imports via the pre-upload
       `checkstyle_hook` (`repo upload --dry-run --verify`; if `./gradlew` hits
       host `inotify` file-watcher limits across multiple checkouts, append
       `--no-watch-fs` to `./gradlew` commands).
    3. **ErrorProne Release Variant Compilation**:
       ```bash
       ./gradlew :camera:camera-core:runErrorProneRelease \
         :camera:camera-camera2:runErrorProneRelease
       ```
    4. **Compilation & Relevant Tests**: Run host/device tests to confirm zero
       regressions.
    5. **Non-Interactive Pre-Upload Repohook & Gerrit Upload Verification**:
       Always pass `--verify -y --cbr . </dev/null` with a `timeout` wrapper so
       `repo upload` never hangs waiting on interactive hook/branch prompts or
       an editor in non-interactive shells:
       ```bash
       # Pre-upload repohook dry-run (Checkstyle, ktfmt, API, commit format):
       timeout 300 repo upload --dry-run --verify -y --cbr . </dev/null

       # Actual Gerrit upload (ONLY when explicitly requested by the user):
       timeout 600 repo upload --verify -y --cbr . </dev/null
       ```
- **Defensive Programming & Code Reviewer Standards**:
  - **ContentResolver & Cursor Null/Empty Safety**: Always check if
    `cursor.moveToFirst()` returns `true` before attempting to access column
    data (`cursor.getString(...)`, `cursor.getLong(...)`). Calling getter
    methods on empty or unpositioned cursors results in
    `CursorIndexOutOfBoundsException`.
  - **Tightly Scoped Deprecation Suppression**: Avoid class-level
    `@Suppress("DEPRECATION")`. When referencing deprecated constants or APIs
    (e.g. `MediaStore.MediaColumns.DATA`), scope `@Suppress("DEPRECATION")`
    inline directly at the specific function or statement level.
  - **MediaStore Volume URI Precision**: When targeting API 29+ Scoped Storage
    in MediaStore helpers, use explicit primary volume URIs
    (`getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)`) rather than broad
    default collection URIs.
  - **NonNull Assertion Cleanliness (`requireNotNull` over `!!`)**: In tests,
    avoid Kotlin force non-null operators (`!!`). Use `requireNotNull(...)` or
    Truth assertions (`assertThat(...).isNotNull()`) to provide descriptive
    failure diagnostics.
  - **Thread Safety & Latches in Background Callbacks**: Analyzer callbacks run
    concurrently on `ioExecutor` background threads. Shared state objects (such
    as `CountDownLatch` or frame save flags) accessed across test threads and
    analyzer callbacks MUST be marked `@Volatile` or use `AtomicBoolean`, and
    must be assigned *before* registering the analyzer callback on
    `ImageAnalysis`.
- **Multi-Commit Stack Hygiene & Anti-Churn Rule**:
  When developing a multi-CL series (e.g. core fix → device quirk → cleanup):
  - **Zero Intermediate Test Churn**: Do not leave temporary test workarounds
    (e.g. temporary `assumeFalse`) in earlier commits that are subsequently
    modified or removed in later commits.
  - **Self-Contained Atomicity**: Each commit in the stack must compile
    cleanly, pass its unit tests, and be fully self-contained with its own
    scoped test coverage.
  - Rebase and squash intermediate workarounds before uploading to ensure
    clean git review histories.


## Git Commit Messages

When the repo-wide `manage_commits` skill is auto-discovered, follow its
commit/amend and workspace rules (`repo upload` in standard Git checkouts vs.
`gob_vcs` in CoG workspaces) alongside CameraX's stricter length limits (`<= 50`
char title, `<= 72` char body lines), with each section separated by a blank
line:

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
- Strictly <= 50 characters (or < 60 characters) to prevent Gerrit upload
  warnings.

**Additional Details (Optional):**
- Keep commit messages simple, direct, and easy to scan.
- Explain the problem the change solves and the approach taken.
- Provide context for the change.
- Prefer concise, clear, and readable messages without losing any required
  info. Using bullet points to summarize specific changes can help with
  readability.

**RelNote:**
- Focus on the observable impact for developers using the library (the "What",
  not the "How"). Do not mention implementation details or internal test apps
  (like `core-test-app`) in the RelNote.
- This will be used to generate release notes.
- **Requirement**: Should only be added for public API changes (e.g., changes
  in `camera/**/api/` directories) or bugs impacting public users.
- **When to omit** (CameraX override over generic `manage_commits`): If a change
  is an internal bug fix, a fix/follow-up for an unreleased feature or API not
  yet released to developers, refactoring, test/doc update, or change to
  restricted APIs marked with `@RestrictTo` (without a public API or
  user-facing behavior change), **do not add the RelNote tag at all**.
- **Edge cases**: `RelNote: N/A` can be used if a release note is explicitly
  not applicable despite a public API change, e.g. when reverting a CL that had
  a public API change meant for release.
- Refer to https://developer.android.com/jetpack/androidx/releases/camera for
  previous examples.

**Bug:**
- The ID(s) of the bug(s) this commit fixes. List each bug ID on a new line,
  prefixed with 'Bug:'.
  Example:
  ```
  Bug: 123456
  Bug: 123457
  ```

**Test:**
- Describe the tests that were added or modified to verify the change.
- Mention any manual testing steps if applicable. Use project names like
  `core-test-app` instead of specific activity names where appropriate.

### Git Commit Amend

Some git commit messages contain a `Change-Id:` line (usually as the last line)
required by the Gerrit code review system. This ID is essential for updating
existing CLs.

When amending a previous commit (e.g., with `git commit --amend`):
- **Do not modify or remove this line.**
- **CRITICAL: NEVER use the `-m` flag** alone when amending. It replaces the
  *entire* message, stripping the `Change-Id` and causing CL upload to create a
  duplicate CL.
- **Best Practice**: Run `git log -1` first to capture the existing message/ID.
  When using `-m`, ensure you manually append the correct `Change-Id` to the
  end of the new message.
- **Recovery**: If the ID is lost, find it in the previous commit via
  `git reflog` and `git log`, then re-amend to restore it.


## Skill: CameraX Agent Guidelines & Experience Capture

### Use when:
- Capturing lessons, troubleshooting workflows, compiler checks, or decision
  rubrics from a coding session into `camera/AGENTS.md` (public) or
  `AGENTS_INTERNAL.md` (internal companion).
- The user requests to update agent guidelines based on recent findings.

### Workflow & Long-Term Governance:

#### 0. Prompt Update Precedence: Project `.md` First -> Google -> AndroidX
When updating any prompt (`AGENTS.md`, `SKILL.md`) or helper script, prioritize
standards in this strict order:
0. **Prioritize Checked-In Project `.md` & `SKILL.md` Files Over Local
   Workstation Rules (Last Resort Only)**:
   - Always land workflow, triage, and governance updates in checked-in project
     `.md` and `SKILL.md` files in the repository (`camera/AGENTS.md`,
     `camera/.agents/skills/*/SKILL.md`, and
     `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md` + sibling `skills/` when
     internal tools are configured) so all team members and future sessions in
     either repository automatically inherit them. Treat local user/workstation
     rules strictly as a last resort for personal environment bootstrapping.
1. **Prompt Update Precedence — 1st: Google-Wide Skill, Customization &
   Readability Standards**:
   - **Automatic Discovery & Canonical Referencing (No Manual `../` Links /
     DRY)**: Rely on upward directory traversal (`AGENTS.md`, `.agents/skills/`,
     `skills.json`, `rules.json`) and reference shared skills by their
     registered skill name (`manage_commits`, `run_tests`) or repo-root /
     canonical URLs. Never write fragile `../` parent-traversal links, `file://`
     URLs, or hardcoded workstation paths, and never duplicate parent rules or
     leave `< 50`-line redirect stubs.
   - **YAML Frontmatter & Progressive Disclosure**: Use `description: >-` in
     third person (`< 1024` chars) with explicit `"Use when..."` and `"Don't use
     for..."` trigger contracts; never expose executable binary paths, CLI
     subcommands, or flag syntax (`--flag`) in frontmatter descriptions so
     agents do not bypass reading `SKILL.md`.
   - **Theory of Mind & Generalization**: Explain the *why* behind non-obvious
     rules instead of relying on bare ALL-CAPS directives, generalize from
     specific incidents with open-ended examples (`e.g.`), and follow Google
     Python/Bash/Markdown readability standards for helper scripts and docs.
2. **Prompt Update Precedence — 2nd: Parent Repository (`AndroidX`) Baseline**:
   - Defer to repo-wide `AGENTS.md` and `.agents/skills/` when present,
     recording only the CameraX-specific delta/overrides.
3. **Prompt Update Precedence — 3rd: CameraX Project Prompt Governance (Sections
   1–5 Below)**.

#### 1. The 3-Tier Signal Filter (Signal vs. Noise)
Before proposing documentation updates, filter session findings through three
levels of abstraction and durability:
* **Tier 1 (Transient Noise — DO NOT ADD)**: Temporary workarounds, specific
  bug IDs, daily flake reports, or raw log traces belong in issue tracker
  comments and CL descriptions, not in long-term guidelines.
* **Tier 2 (Implementation Details & Local Skills — Keep in Code/KDoc or
  Lowest-Priority Tooling)**: Specific device model strings, OEM build
  identifiers, private helper mechanics, or quirk class implementation details
  belong in source code KDoc and Quirk classes where they evolve with the code.
  Treat tool-specific local skills as a lowest-priority fallback—prefer
  explaining durable design and implementation principles in `AGENTS.md` over
  relying on local skills.
* **Tier 3 (Universal Engineering Patterns — ADD to AGENTS.md by Durability
  Priority)**: Prioritize additions from longest-lived to most specific,
  integrating into existing sections rather than duplicating them:
  1. *1st Priority — Universal Design Principles & Public Specifications*
     (`Standards & Guidelines Hierarchy`, `Code Elegance & Idiomatic Design`):
     official API/style guides, deep modules, single responsibility ("object
     job"), separating general-purpose core from special-purpose quirks, and
     eliminating special cases.
  2. *2nd Priority — High-Level Architecture & Timeless Domain Contracts*
     (`Backend Architecture & CameraPipe Migration`, `@CameraScope` vs.
     `@UseCaseCameraScope`, Section 6.F public contracts, concurrency).
  3. *3rd Priority — High-Level Implementation Rationale & Decision Rubrics*
     (Section 6 Root-Cause Hierarchy, Section 8 Capability Framework, Section 9
     Test Pyramid, pre-upload quality gates, and commit anti-churn rules—explain
     the *why*, not fragile method-by-method traces).

#### 2. The 1-Year Litmus Test
Evaluate every prospective rule against this long-term criterion:
> *"Will this guideline save time and prevent regressions for an engineer
> working on an unrelated CameraX feature 1–2 years from now?"*
* If **Yes**: Distill into a generalized principle with a concise case study.
  Diagnostic signatures (exception names, log strings) may accompany a
  general principle as compact examples; keep OEM- or lab-specific signatures
  in `AGENTS_INTERNAL.md`.
* If **No**: Preserve it in the issue tracker.

#### 3. Consolidate & Refine (Prevent Unbounded Growth)
Do not infinitely append new bullet points. When updating guidelines:
1. Locate existing relevant sections (`General Instructions`, `Testing`,
   `Troubleshooting`).
2. Refine, clarify, or consolidate existing guidelines with new insights.
3. Maintain clean Markdown formatting wrapped strictly at **80 characters**.

#### 4. Target Repository Routing & CL Staging
Route documentation updates according to repository scope:
* **Public Guidelines (`frameworks/support/camera/AGENTS.md`)**:
  Public architecture, Kotlin/Java style, Checkstyle, ErrorProne release
  checks, pre-upload quality gates, commit hygiene, and general troubleshooting.
* **Internal Extension Guidelines (`AGENTS_INTERNAL.md`)**:
  Internal lab test runners, host memory concurrency estimation, physical lab
  device health triage, and environment protocols.


#### 5. Modular Size Budgets & Cross-File Refresh Protocol
* **Hard Size & Line Restrictions (Zero Truncation Invariant)**:
  - Keep `camera/AGENTS.md` strictly under `24,000 bytes` (`<= 450 lines`) so
    agent hosts auto-load 100% of the root rules without truncation. Never use
    `@`-prefixed `[label](path)` inline includes in `AGENTS.md` (they expand
    before the 24 KB limit check).
  - Keep every `camera/.agents/skills/*/SKILL.md` file strictly under `45,000
    bytes` and `780 lines` (`46,080 B / 800-line` single-call `read_file`
    limit) so 1 `read_file` call loads 100% of any skill file.
* **Mandatory Cross-File Refresh Check**:
  Whenever updating any `.md` file in `camera/`, inspect `camera/AGENTS.md` and
  all 4 `camera/.agents/skills/*/SKILL.md` files (plus `AGENTS_INTERNAL.md` and
  its companion skills when internal tools are configured) to refresh any
  affected routing tables, chapter/section numbers, or shared rubrics.

#### 6. All-Stage `.md`, `SKILL.md` & Tool/Script Update Checklist
Whenever updating any `.md` guide, `SKILL.md` skill, or helper script/tool,
enforce this 4-stage checklist:
1. **Stage 1 — Pre-Update Sync & Zero-Knowledge-Loss Splitting**:
   - Sync target repositories to latest `HEAD` before editing.
   - **Never Delete Valid Existing Knowledge to Fit Size Caps**: When a `.md` or
     `SKILL.md` file nears its byte/line budget, **never strip or gut valid
     existing rules, examples, or capabilities** (remove content *only* if
     genuinely obsolete or contradicted by current architecture). Instead,
     **split or move** self-contained sections to a sibling `SKILL.md` file that
     has headroom and leave a clear cross-reference pointer.
2. **Stage 2 — Long-Term Durability (`1-Year Litmus Test`)**:
   - Express rules in terms of durable architectural roles, package contracts
     ("object jobs"), and dynamic verification steps using representative `e.g.`
     anchors rather than brittle line numbers or transient IDs.
3. **Stage 3 — Multi-Environment & Infra-Resilient Tool/Script Design**:
   - When creating or updating any script or CLI tool (`.sh`, `.py`), design for
     diverse team member machines, headless/container/team-agent environments,
     and future infra/folder shifts: avoid hardcoded personal usernames, `$HOME`
     paths, or single-environment assumptions; implement multi-tier discovery
     and fallback chains (e.g., CLI flag -> env var -> upward CWD walk -> MRU
     state file -> standard default / bounded marker scan) with automatic
     self-healing when a cached folder or marker is moved or deleted.
4. **Stage 4 — Test Coverage, Cross-File Refresh & Multi-Model Audit**:
   - Add/update unit and edge-case tests (e.g., Gradle `test` tasks or script
     unit test suites) covering normal flows, non-default environments, and
     recovery paths; run all tests and doc bundle format/alignment checks
     (including AOSP Gerrit presubmit legal-contract keyword checks); and
     complete the Step 6 Clean-Context Multi-Model Verification Loop before
     upload.


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
