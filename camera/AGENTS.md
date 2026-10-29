---
trigger: always_on
description: >-
  Core CameraX architecture, coding/testing standards, multi-model verification
  gate, and mandatory task-to-skill read_file routing table.
---

> [!IMPORTANT]
> **MANDATORY STEP 1 — Task-to-Skill Context Loading Gate (`read_file`)**:
> To prevent auto-load truncation while preserving 100% of CameraX's deep
> architectural, testing, triage, and multi-model verification rules, detailed
> workflows live in modular files under `camera/.agents/skills/*/SKILL.md`
> (each sized `< 45,000 bytes` and `<= 780 lines` so **1 single `read_file`
> call** loads 100% of any file).
>
> **Before starting ANY task or answering ANY CameraX question**, identify all
> matching domains below and call `read_file` (or `view_file`) on each
> corresponding `SKILL.md` in full:
>
> 1. **Technical/Architectural Questions, Codebase Explanations, Code Review, PR
>    Audit, Plan/Report Audit, or Multi-Model Verification** *(Steps 1-7 & Modes
>    1-5)* -> Read `camera/.agents/skills/camerax-code-review/SKILL.md`
>    ([`camerax-code-review`][skill-review]).
> 2. **Writing/Modifying Code, API/Design Questions, Refactoring, Preparing
>    Commits / CLs, or Updating Docs** *(Standards Hierarchy, 4-Layer
>    Package/Object-Job Map, Pre-Placement Checklist, Development Workflow,
>    Git Commit Rules, & `Skill: CameraX Agent Guidelines & Experience
>    Capture`)* -> Read
>    `camera/.agents/skills/camerax-guidelines-governance/SKILL.md`
>    ([`camerax-guidelines-governance`][skill-gov]) (and when modifying code
>    or fixing bugs, also read `camerax-testing/SKILL.md` Section 1 for
>    mandatory pre-coding test case study).
> 3. **Writing/Running Unit (Robolectric) or Device (`androidTest`) Tests, Test
>    Strategy Questions, Debugging Leaks, or Migrating Tests Across the Test
>    Pyramid** *(`Testing` & `Skill: CameraX Troubleshooting & Code
>    Verification` Sections 1-4 & Section 9)* -> Read
>    `camera/.agents/skills/camerax-testing/SKILL.md`
>    ([`camerax-testing`][skill-test]).
> 4. **Bug Triage, Behavioral/HAL Inquiries, Device-Specific Failures, `Quirk`
>    Scoping, or Hardware Capability Abstraction** *(`Skill: CameraX
>    Troubleshooting & Code Verification` Sections 5, 6 (`6.A`-`6.G`), 7, & 8)*
>    -> Read `camera/.agents/skills/camerax-triage-and-quirks/SKILL.md`
>    ([`camerax-triage-and-quirks`][skill-triage]).
>
> **Zero-Checkout / Remote Fallback**: If operating without a local checkout of
> `frameworks/support`, fetch any required skill file via Gitiles HTTPS:
> `https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/camera/.agents/skills/<skill-name>/SKILL.md`
> (or `curl -sf "<url>?format=TEXT" | base64 -d`).
>
> **Optional Internal Companion**: When `$CAMERAX_INTERNAL_TOOLS_DIR` is set and
> `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md` is present (or if unset and a
> `code_search` tool is available in the environment, by querying
> `f:camerax.*AGENTS_INTERNAL\.md$`), also read `AGENTS_INTERNAL.md` (and its
> modular skills in the sibling `skills/` directory) for supplementary internal
> guidelines and physical lab runner workflows.

# Project: CameraX

## Senior CameraX SWE Deep Study Protocol (Questions & Proposals)

Whenever answering any question about CameraX, explaining codebase behavior,
evaluating feasibility, diagnosing an issue, or proposing a solution—even when
no code edit is requested—agents **MUST** operate with the rigor and full-stack
knowledge of a **Senior CameraX SWE** and **NEVER** reply quickly from memory or
isolated search snippets:

1. **Full-Stack Call-Chain, Package Responsibility & Test Study**: Scan callers
   and tests (`git --no-lazy-fetch grep -- camera/<module>/src/...`) and read
   complete target classes, sibling implementations, and `src/test/` +
   `src/androidTest/` test cases across all 4 layers (`public API / UseCase /
   View / Video / Extensions / Compose / Effects` -> `CameraInternal /
   core.internal.CameraUseCaseAdapter / StreamSharing` -> `camera-camera2`
   (`UseCaseCamera`, `CapturePipeline`, `adapter/`, `impl/`, `internal/`,
   `config/`, `compat/`) -> `camera-camera2-pipe` (`CameraGraph`,
   `RequestProcessor`); full map in [`camerax-testing`][skill-test] Section 1).
   Verify package responsibility ("object job"), `@CameraScope` vs.
   `@UseCaseCameraScope` boundaries, and partial re-bind (`hasUseCases == true`)
   vs. full unbind (`hasUseCases == false`) before proposing any solution,
   calling internal APIs, or placing code changes.
2. **Historical Intent & Guidelines Check (`git blame` / `git log` / KDoc)**:
   Inspect KDoc, inline comments, and git history (`git --no-lazy-fetch blame`,
   `git --no-lazy-fetch log`) to verify *why* existing ordering, state checks,
   or workarounds exist before forming a conclusion, and cross-check the
   **Standards Hierarchy** (AndroidX -> Android Platform/Camera2 -> Google-wide)
   plus the **Simulation Rubric (`Section 6.A`)**, **3-Step Root-Cause Hierarchy
   (`Section 6.B-6.F`)**, and **6-Phase Capability Framework (`Section 8`)**.
3. **Evidence Citations & Conditional Clean-Context Multi-Model Plan/Verify
   Gate**: Ground every claim in exact `file:line` references (and commit SHAs /
   guideline sections). Whenever a question or problem is **not** a trivial
   flash single-fact lookup (e.g., root-cause "why" inquiries, concurrency or
   lifecycle analysis, design/feasibility trade-offs, `Quirk` scoping, or
   solution proposals), write the candidate answer/plan and raw evidence paths
   to `<scratch_or_tmp>/draft_deliverable.md` and run the **Clean-Context
   Multi-Model Verification Gate** ([`camerax-code-review`][skill-review] Modes
   2, 3, or 5 with the Two-Turn Blind Read Barrier) **before** presenting the
   final answer to the user.

## Standards & Guidelines Hierarchy

When answering technical questions, writing new code, reviewing existing code,
or fixing bugs in CameraX,
agents **MUST** actively read and strictly adhere to the relevant guidelines,
style guides, and correctness checks in this precedence order (full recursive
traversal protocol in [`camerax-guidelines-governance`][skill-gov]):

1. **1st Priority — AndroidX Standards & Correctness Checks**:
   - [`docs/api_guidelines/index.md`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/docs/api_guidelines/)
     (recursively read `kotlin.md`, `checks.md`, `async.md`, `annotations.md`,
     `compat.md`).
   - Strict adherence to `@RestrictTo(LIBRARY_GROUP)` and
     `./gradlew <project>:updateApi`.
2. **2nd Priority — Android / AOSP Platform Guidelines**:
   - [Android Platform API Guidelines](https://source.android.com/docs/setup/contribute/api-guidelines),
     [Kotlin-Java Interop Guide](https://developer.android.com/kotlin/interop),
     [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide),
     [AOSP Java Style Guide](https://source.android.com/docs/setup/contribute/code-style),
     and [Android Camera2 API specifications](https://developer.android.com/reference/android/hardware/camera2/package-summary).
3. **3rd Priority — Google-Wide Engineering Standards**:
   - [Google Style Guides](https://google.github.io/styleguide/) and
     [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## General Instructions:

- **Parent AndroidX Baseline (`frameworks/support/AGENTS.md`) & CameraX
  Overrides**: When operating in an AndroidX checkout where the repository-root
  `AGENTS.md` and repo-wide skills (`manage_commits`, `manage_cl_workspaces`,
  `run_tests`) are auto-discovered by upward directory traversal, use the parent
  AndroidX definitions for workspace handling (standard `repo` checkouts vs. CoG
  `/google/cog/cloud/...` workspaces), module lookup (grepping `settings.gradle`
  instead of `./gradlew projects`), `PROJECT_PREFIX` scoping,
  `<project>:javaFormat`, and commit/amend mechanics, while applying CameraX
  overrides (`:testReleaseUnitTest --tests` instead of `:test --tests`, `<= 50`
  char commit titles, and CameraX's Two-Stage Pre-Upload Gate). If operating
  standalone without parent AndroidX skills, use the defaults below.
- **Kotlin Formatting**: When modifying any .kt file, format it using `ktfmt`
  via the following command:
  `./gradlew :ktCheckFile --format --file <file>`.
  If more than one file needs formatting, continue adding
  `--file <next-file>` to the command.
  **CRITICAL**: Only format the files you have modified. Do not perform
  project-wide or unrelated formatting to keep git diffs clean.
- **Respect User's Local Changes**: Before modifying or creating any file,
  check for local uncommitted changes in the workspace. Do not overwrite,
  revert, or discard the user's modifications without explicit instructions.
  If your changes might conflict with theirs, seek clarification first.
- **Public API**: When a public API is changed or when asked to update the
  public API files, execute: `./gradlew <project>:updateApi`. The projects and
  their root paths can be found in `settings.gradle`.
- **File Management**: When moving files, use `git mv` to keep version control
  history.
- **Git Commits & Uploads**: Do not make a git commit or run `repo upload`
  unless specifically requested.
- **Strict Scope Isolation**: Maintain clean and atomic changes. Do not bundle
  unrelated modifications (such as fixing pre-existing lint issues or test
  cleanup in unrelated files) into a feature or bug-fix CL. Always split
  unrelated issues into separate tracking bugs and independent CLs.
- **Scoping Builds**: Always use `PROJECT_PREFIX` to speed up Gradle
  configuration, e.g.,
  `PROJECT_PREFIX=:camera:camera-core ./gradlew :camera:camera-core:assemble`.


## Core Architecture, Development & Verification Invariants

Read [`camerax-guidelines-governance`][skill-gov] before editing code. Key
invariants:

- **Pre-Flight Upstream Sync, Agent-Checkout Stash & `repo start`**: In
  standard `repo` checkouts, fetch `aosp androidx-main` first and create
  feature branches with `repo start <branch_name> .` (never modify a
  developer's personal checkout without permission; in an agent-managed
  `repo init`/`repo sync` triage checkout, auto-stash leftover
  uncommitted/untracked scan or task files via `git stash push -u`, preserve
  detached unmerged commits on `wip-preserve-<sha>`, optionally prune already-
  merged local branches if opted in, and detach/switch `HEAD` to clean
  `aosp/androidx-main`; in CoG workspaces, follow `manage_cl_workspaces`).
- **CameraPipe Backend & Lifecycle Scopes**: `camera-camera2` now runs on top of
  `camera-camera2-pipe` (`CameraPipe`). Never conflate:
  - `@CameraScope` (`CameraInternalAdapter`, `CameraControlAdapter`,
    `UseCaseManager` in `camera-camera2`, wired via `CameraConfig` and backing
    `camera-core`'s `CameraInternal` / `CameraControlInternal`): survives across
    use-case binds/unbinds (`setActiveResumeMode`, zoom/torch,
    `Closing -> Closed` teardown). Never retain a `@UseCaseCameraScope`
    reference inside a `@CameraScope` owner without invalidating on re-bind.
  - `@UseCaseCameraScope` (`UseCaseCamera`, `UseCaseCameraRequestControl`,
    `CapturePipeline`, `UseCaseSurfaceManager`, and per-session `CameraGraph`,
    wired via `UseCaseCameraConfig`): recreated whenever the active use-case set
    or session config changes. Distinguish **partial re-bind** (`hasUseCases ==
    true`) from **full unbind** (`hasUseCases == false`, tearing down
    `UseCaseCamera` and closing the underlying `CameraGraph` / `CameraDevice`).
- **Public API Contract Fidelity & Idiomatic Kotlin**:
  - Honor explicit caller configurations (`ImageCapture`, `Preview`,
    `VideoCapture`, `ResolutionSelector`, `DynamicRange`) unless hardware or
    framework constraints genuinely prevent them.
  - Prefer `val x by lazy(LazyThreadSafetyMode.PUBLICATION) { ... }` over
    nullable backing vars when a dependency is non-null at access time, and use
    eager `val` initialization for cheap objects instead of `by lazy`.
  - Keep internal helpers package-private or `@RestrictTo(LIBRARY_GROUP)`.
  - Enforce defensive programming (`@GuardedBy`, copy-on-write, zero resource
    leaks on exception/cancellation paths) and clean multi-commit stack hygiene
    (no temporary workarounds added in one commit and deleted in the next).
- **Mandatory Two-Stage Pre-Upload Quality Gate**:
  1. **Stage 1 (Multi-Model Clean-Context Verification)**: Execute the Step 5/6
     verification loop in [`camerax-code-review`][skill-review].
  2. **Stage 2 (Compiler, Lint, Format & Dry-Run Hook)**:
     - Format modified `.kt` files: `./gradlew :ktCheckFile --format --file <f>`
     - Run ErrorProne (Java) & Lint:
       ```bash
       PROJECT_PREFIX=:camera:camera-core ./gradlew <proj>:runErrorProneRelease <proj>:lintRelease
       ```
     - Verify `repo upload` hooks non-interactively:
       `timeout 300 repo upload --dry-run --verify -y --cbr . </dev/null`

## Core Testing, Triage & Multi-Model Verification Gate

- **Testing Standards (Sections 1-4 & Section 9)** (full guide in
  [`camerax-testing`][skill-test]):
  - Use Google **Truth** assertions and fakes (`camera-testing`,
    `camera-camera2-pipe-testing`) over Mockito.
  - Use `@Config(sdk = [Config.TARGET_SDK])` or `Config.ALL_SDKS` in
    Robolectric (never hardcode `minSdk`).
  - Annotate chart-dependent device tests with
    `@LabTestRule.LabTestFrontCamera` / `@LabTestRule.LabTestRearCamera`; save
    debug frames to `/sdcard/Pictures/test_output/<name>.png` via
    `LabTestUtil.saveTestBitmap`.
  - Never use `Thread.sleep()` for async sync; use `CountDownLatch`,
    `CompletableDeferred`, or `Semaphore`. Always call
    `DeferrableSurface.close()` in `@After` to prevent Robolectric
    `FutureGarbageCollectedException` leaks.
  - **Section 9 Test Pyramid & Smoke Guardrail**: When migrating resolver/config
    tests from `androidTest/` to `test/`, retain at least **one end-to-end smoke
    test on real devices** in `androidTest/` (and sync downstream CI targets per
    `AGENTS_INTERNAL.md` Section 4.5 when internal tools are configured).
- **Root-Cause Triage, Quirk Scoping & Capability Framework (Sections 5-8)**
  (full guide in [`camerax-triage-and-quirks`][skill-triage]):
  - **Section 5 (Device-Specific Triage)** & **Section 6.A (Simulation
    Rubric)**: Distinguish realistic production app journeys from test-only
    stress/fixture artifacts before changing core library code.
  - **Section 6.B-6.F (3-Step Root-Cause Hierarchy, Quirk OS Ceilings & API
    Contracts)**:
    1. *Step 1*: Fix CameraX state/concurrency/lifecycle bugs in core code
       (never mask a CameraX bug with a `DeviceQuirk` or test skip).
    2. *Step 2*: Check standard Android/Camera2 capabilities before adding any
       model-specific `Quirk`.
    3. *Step 3*: Add a narrowly scoped `Quirk` (with OS version ceilings per
       `Section 6.E` and empirical verification) only for genuine OEM/HAL bugs.
  - **Section 7 (Safety Auditing)** & **Section 8 (6-Phase Capability
    Problem-Solving Framework)**: Follow the 6-phase hardware abstraction and
    caller audit protocol in [`camerax-triage-and-quirks`][skill-triage].
- **Universal Clean-Context Multi-Model Verification Gate (Step 5 & Step 6)**
  (full protocol in [`camerax-code-review`][skill-review]):
  - Trigger at **Checkpoint 1** (upfront root-cause & design exploration) and
    **Checkpoint 2** (pre-delivery audit across Modes 1-5: Code Review, Triage,
    Plan, Report, and Synthesis).
  - Route verifiers **Opposite-Family First** (e.g. Claude reviews Gemini;
    Gemini reviews Claude) with **Zero Author Narrative** in verifier briefs,
    the **Fix-Scope Boundary Rule** (distinguishing in-scope bugs/regressions
    from `[Optional / Out-of-Scope]` pre-existing caller trade-offs so
    out-of-scope notes never block convergence or cause scope creep), a
    **Two-Turn Read Barrier** in Modes 2-5 (Turn 1: raw evidence blind
    diagnosis; Turn 2: **Post-Blind Adjudication Turn** auditing the draft), and
    three hostile critic lenses (Specification, Quantitative/State,
    Counter-Hypothesis).
  - Track findings in an ephemeral `convergence_ledger.md` using strict status
    transitions (`OPEN`, `PATCHED_PENDING_REAUDIT`,
    `REVERTED_VERIFICATION_FAILURE`, `DISMISSED_WITH_PROOF`,
    `VERIFIED_RESOLVED`, `ESCALATED`) across a default 3-round (up to 5-round)
    clean-context loop until `0` `OPEN`, `0` `REGRESSED`, and `0`
    `PATCHED_PENDING_REAUDIT` items remain.

## Prompt & Skill Update Governance Rule (Cross-File Refresh & Size Caps)

> [!IMPORTANT]
> **Cross-File Refresh & Size Restriction Guardrail**:
> Whenever you update `camera/AGENTS.md` or any
> `camera/.agents/skills/*/SKILL.md` file (or capture lessons from a coding
> session), you **MUST** read [`camerax-guidelines-governance`][skill-gov]
> (`Skill: CameraX Agent Guidelines & Experience Capture`) and enforce in
> priority order:
> 0. **Prioritize Checked-In Project `.md` & `SKILL.md` Files Over Local
>    Workstation Rules (Last Resort Only)**: Always land workflow, triage, and
>    governance updates in checked-in project `.md` and `SKILL.md` files
>    (`camera/AGENTS.md`, `camera/.agents/skills/*/SKILL.md`, and
>    `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md` + sibling `skills/` when
>    configured) so all team members and future sessions automatically inherit
>    them; treat local user/workstation rules strictly as a last resort.
> 1. **Prompt Update Precedence — 1st: Google-Wide Skill, Customization &
>    Readability Standards**: Rely on automatic upward directory discovery
>    (`AGENTS.md`, `.agents/skills/`, `skills.json`) and reference shared skills
>    by registered skill name (`manage_commits`, `run_tests`) or repo-root /
>    canonical URLs (never write manual `../` parent-traversal links, `file://`
>    URLs, or `< 50` line redirect stubs). Keep YAML frontmatter
>    `description: >-` in third person (`< 1024` chars) with `"Use when..."` and
>    `"Don't use for..."` triggers and zero CLI flags/paths (Progressive
>    Disclosure), explain *why*, generalize examples (`e.g.`), and avoid
>    duplicating parent rules (DRY).
> 2. **Prompt Update Precedence — 2nd: Parent Repository (`AndroidX`)
>    Baseline**: Defer to repo-wide `AGENTS.md` and `.agents/skills/*` when
>    present, documenting only CameraX-specific overrides.
> 3. **Prompt Update Precedence — 3rd: CameraX Project Prompt Governance**:
>    Apply **The 3-Tier Signal Filter**, **The 1-Year Litmus Test**, and the
>    **All-Stage `.md`, `SKILL.md` & Tool/Script Update Checklist** (split/move
>    to a sibling `SKILL.md` with headroom instead of deleting knowledge; design
>    scripts for multi-env fallbacks and unit tests; wrap prose at
>    **`<= 80` characters** with 100% public purity), keep `camera/AGENTS.md`
>    under **`24,000 bytes`** (no `@`-prefixed `[label](path)` inline includes),
>    keep every `SKILL.md` file under **`45,000 bytes` and `780 lines`**, and
>    refresh all 5 files (`camera/AGENTS.md` + 4 `SKILL.md` files, plus
>    `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md` when available) together.

## Git Commit Messages & Amend (CameraX Overrides)

Read [`camerax-guidelines-governance`][skill-gov] (`## Git Commit Messages` and
`### Git Commit Amend`) and the repo-wide `manage_commits` skill before
committing or amending:

```
<Commit Title>

<Additional details about the change.>

RelNote: <release note text>
Bug: <bug id>
Test: <test instructions>
```

- **Commit Title & Body**: Imperative summary strictly `<= 50` chars (or `< 60`
  chars) to prevent Gerrit upload warnings; body lines `<= 72` chars explaining
  the problem, context, and approach.
- **RelNote (CameraX Override over `manage_commits`)**: Focus on observable
  developer impact (never mention internal test apps like `core-test-app`). Add
  **only** for public API changes (`camera/**/api/`) or bugs impacting public
  users; **omit `RelNote` entirely** for internal fixes, unreleased API
  follow-ups, refactorings, test/doc updates, or `@RestrictTo` changes without
  user-facing impact (`RelNote: N/A` only when reverting a public API change).
- **Bug, Test & `Change-Id` Preservation on Amend**: Prefix each bug ID with
  `Bug: <id>` on its own line and state added/modified tests in `Test:`. When
  running `git commit --amend`, **never** use `-m` alone without appending the
  existing `Change-Id:` line from `git log -1` (recover via `git reflog` if
  lost).

## Description of sub-projects:

- camera-camera2: The implementation layer that bridges `camera-core`
  abstractions to the Android Camera2 platform APIs. Under the hood, this
  module has been migrated to use `camera-camera2-pipe` (CameraPipe) as its
  default and only backend engine (`Camera2Config` instantiates CameraPipe).
- camera-camera2-pipe: A low-level, high-performance Camera2 abstraction layer
  that powers CameraX's `camera-camera2` implementation. It manages camera
  devices (`Camera2DeviceManager`), capture sessions (`CameraGraph`), and
  low-level request/frame pipelines.
- camera-camera2-pipe-testing: Testing library for `camera-camera2-pipe`.
- camera-common: Contains common utility classes and constants used across
  CameraX modules.
- camera-common-testing: Provides testing utilities and fakes for
  `camera-common`.
- camera-compose: A library that provides Jetpack Compose integration for
  CameraX.
- camera-core: The core library of CameraX. It provides the basic camera
  functionalities. Its Camera2-dependent implementations are provided by
  `camera-camera2`.
- camera-effects: A library for applying visual effects to camera streams.
- camera-extensions: A library that provides access to device-specific camera
  effects and features.
- camera-extensions-stub: A stub implementation for camera extensions.
- camera-lifecycle: A library that provides lifecycle management for CameraX.
- camera-mlkit-vision: A library for integrating ML Kit Vision with CameraX.
- camera-testing: A library providing testing utilities for CameraX.
- camera-testlib-extensions: Testing libraries for camera extensions.
- camera-video: A library that provides video recording functionalities.
- camera-view: A library that provides a custom View for camera preview.
- featurecombinationquery: A group of standalone Camera2 libraries for querying
  supported combinations of camera features. These can be used independently of
  CameraX. Includes `featurecombinationquery` and
  `featurecombinationquery-play-services`.
- integration-tests: A project containing integration tests for the camera
  libraries.
- media3-effect: A library that provides seamless integration for applying
  Media3-based effects to camera streams in CameraX.
- viewfinder: A group of standalone Camera2 libraries providing a `Viewfinder`
  widget for camera previews. These can be used independently of CameraX.
  Includes `viewfinder-compose`, `viewfinder-core`, and `viewfinder-view`.

## Documentation links:

- https://developer.android.com/training/camerax
- https://developer.android.com/training/camerax/architecture
- https://developer.android.com/training/camerax/configuration
- https://developer.android.com/training/camerax/preview
- https://developer.android.com/training/camerax/take-photo
- https://developer.android.com/training/camerax/analyze
- https://developer.android.com/training/camerax/video-capture
- https://android-developers.googleblog.com/search?q=camerax
- Camera2 API Reference:
  https://developer.android.com/reference/android/hardware/camera2/package-summary

[skill-review]: .agents/skills/camerax-code-review/SKILL.md
[skill-gov]: .agents/skills/camerax-guidelines-governance/SKILL.md
[skill-test]: .agents/skills/camerax-testing/SKILL.md
[skill-triage]: .agents/skills/camerax-triage-and-quirks/SKILL.md
