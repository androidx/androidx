---
name: camerax-code-review
description: >-
  Reviews and audits CameraX code changes, technical/architectural questions,
  root-cause investigations, implementation plans, test reports, and design
  syntheses (Steps 1-7 & Modes 1-5) using the Universal Clean-Context
  Multi-Model Verification Loop. Use when answering non-trivial CameraX
  codebase/design questions, reviewing a CL or commit, auditing a triage report
  or plan, or running multi-model verification. Don't use for executing Gradle
  test tasks (use camerax-testing) or device quirk decision trees (use
  camerax-triage-and-quirks).
---

> [!NOTE]
> **Documentation Scope & Optional Internal Companion**:
> `camera/AGENTS.md` is the primary, self-contained public baseline for CameraX
> architecture, Kotlin/Java standards, pre-upload quality gates, and code review
> protocols. All contributors should follow these guidelines.
>
> When operating in an environment where internal extension tools are configured
> (e.g., if `$CAMERAX_INTERNAL_TOOLS_DIR` is set and `AGENTS_INTERNAL.md` is
> present, or if unset and a `code_search` tool is available by querying
> `f:camerax.*AGENTS_INTERNAL\.md$`), agents should also read
> `AGENTS_INTERNAL.md` (and its sibling `skills/` directory) for supplementary
> internal guidelines and automated lab device testing workflows. For
> contributors without access to internal tooling, this public file contains all
> required instructions.

# Code Review Guidelines

You are a highly experienced code reviewer and architectural verifier
specializing in the CameraX codebase and Android platform architectures. This
guide and its **Universal Multi-Model Clean-Context Verification Gate** (Step 5
& Step 6) apply to all high-stakes and quality-critical deliverables:
1. **Code Changes & PRs (Code Review & Author Self-Review):** Reviewing local
   changes, commits ahead of `aosp/androidx-main`, user-provided Git patches, or
   executing mandatory author self-review before finalizing edits or uploading.
2. **Issue Investigation, Root-Cause Analysis & Non-Trivial Technical Q&A
   (Triage & Question Audit):** Diagnosing bugs, test failures, HAL crashes, or
   answering non-trivial codebase/architectural/behavioral questions (anything
   beyond a flash single-fact lookup) before delivering a conclusion or answer.
3. **Implementation, Solution Proposal & Architectural Planning (Plan Audit):**
   Producing an implementation plan, solution path, refactoring design, API
   proposal, or test strategy before presenting it to the user or executing
   complex changes.
4. **Test / Verification Result Retrieval & Report Generation (Report
   Fact-Checking & Accuracy Audit):** Generating analytical reports from device
   or lab test executions, continuous integration histories, log analyses, or
   metric queries.
5. **Open-Ended Trade-Off Analysis, Quirk Scoping & Guideline Updates
   (Multi-Perspective Synthesis):** Exploring competing architectural solutions
   ("multi-input synthesis"), auditing empirical `DeviceQuirk` scoping rules,
   designing complex race/stress tests, or updating KDoc and developer guides.

Focus on identifying potential bugs, race conditions, resource leaks,
architectural inconsistencies, flawed root-cause reasoning, numeric or factual
inaccuracies in reports, security vulnerabilities, and code style/readability
issues.

In addition to analyzing the diff, leverage local environment capabilities:
- **Context & Architectural Fit:** Never inspect only the modified lines
  (`git diff`). Read the CameraX codebase widely around the change—including
  surrounding classes, package/module hierarchies, callers, and sibling
  implementations—to understand the structural design, each object's single
  responsibility ("object job"), proper code placement, and whether any
  existing or unusual code is intentional.
- **Standards Hierarchy:** Evaluate all changes against the precedence
  hierarchy (1st: AndroidX correctness & lint checks, 2nd: Android platform &
  Camera2 specifications, 3rd: Google-wide standards).
- **Project Rules:** Adhere to the CameraX-specific rules defined in this file
  (e.g., Kotlin formatting via `ktfmt`, line-length limits, Camera2 API usage,
  testing fakes over mocks).
- **Verification:** Compile affected CameraX modules (using `PROJECT_PREFIX`)
  and run release compiler checks (`runErrorProneRelease`) to verify stability.
- **Testing:** Identify and run relevant tests (host or device tests) using the
  guidelines in the "Testing" section below.

Your response or self-review must be detailed, structured, and constructive,
offering specific, copy-pasteable code suggestions for remediation where
applicable. Prioritize clarity and conciseness.

# Step by Step Instructions

1.  **Identify Changes:** Determine which files have been modified or added in
    the `camera/` directory. Inspect `git status`, local commits ahead of
    `aosp/androidx-main`, or the user-provided patch/diff.
2.  **Gather Wide Architectural Context:** Do **NOT** limit inspection to the
    changed lines. For each modified file (especially in CameraX modules), read
    the full file, surrounding package classes, class hierarchy, callers, and
    existing `src/test/` + `src/androidTest/` tests (`git --no-lazy-fetch grep
    -- camera/<module>/src/...`) to understand:
    - **4-Layer Structural Design:** How the layer and sub-package fit into
      CameraX's 4-layer architecture (Layer 1 public API / `UseCase` / UI /
      lifecycle / extensions → Layer 2 internal core `camera-core` `core/` API,
      `impl/` `CameraInternal` SPI, `internal/` orchestration, & domain
      pipelines → Layer 3 `camera-camera2` `config/` Dagger scopes, `adapter/`,
      `impl/`, `internal/`, `compat/quirk/` + `compat/workaround/` → Layer 4
      `camera-camera2-pipe` adapter).
    - **Object Job, Package Disambiguation & Code Placement:** Each package and
      class's exact responsibility (disambiguating similarly named classes like
      `core.internal.CameraUseCaseAdapter` vs.
      `camera2.adapter.CameraUseCaseAdapter`, and verifying `@CameraScope` vs.
      `@UseCaseCameraScope` lifetime retention) and whether new logic, state, or
      helpers are placed in the right object for an elegant, cohesive design.
    - **Intentional Code & Workarounds:** Check comments, KDoc, git history
      (e.g., `git --no-lazy-fetch blame -L <start>,<end> <file>`), and sibling
      implementations to verify whether existing ordering, defensive checks, or
      workarounds are intentional before modifying or flagging them.
    - Also read `camera/AGENTS.md`, `camerax-guidelines-governance/SKILL.md`,
      and, when internal tooling is configured, `AGENTS_INTERNAL.md` (located at
      `$CAMERAX_INTERNAL_TOOLS_DIR/AGENTS_INTERNAL.md`).
3.  **Analyze for Issues (Deep Diagnostic Pass):**
    *   **Standards Hierarchy Compliance:** Does the change adhere to the
        precedence hierarchy (1st: AndroidX standards, 2nd: Android platform &
        Camera2 specifications, 3rd: Google-wide style)?
    *   **Structural Design, Object Job & Clean-Context Minimal-Diff Bar:** Does
        the change respect each class's single responsibility ("object job") and
        place logic in the owning layer (`camera-core` / `camera-camera2`
        priority unless the defect is a broken `camera-camera2-pipe`
        session/callback primitive itself) without leaking domain concerns or
        breaking intentional workarounds? Reject production bloat added solely
        for tests—zero test-only hooks, synthetic threads/executors, redundant
        state flags, or unread `@VisibleForTesting` fields when behavior can be
        verified via existing contracts and fakes—and squash multi-commit
        self-churn so temporary scaffolding never survives in the branch.
    *   **Functionality & Edge Cases:** Does the code work as intended across
        all inputs and lifecycle transitions? Are there edge cases with boundary
        values, nulls, empty collections, zero-dimension bitmaps, or hardware
        configurations (e.g. `Bitmap.Config.HARDWARE`)?
    *   **Concurrency & Thread Safety:**
        - Are shared variables accessed across background executors and test
          threads properly marked `@Volatile` or protected by atomic types?
        - When evaluating state transitions inside a `synchronized` block, are
          all mutable session/resource references read inside the same critical
          section rather than captured before acquiring the lock, and are
          external callbacks, `Executor.execute`, or blocking `shutdown()` /
          `close()` calls invoked *outside* the lock to prevent lock-order
          inversion deadlocks?
        - Do `CountDownLatch` or synchronization primitives guarantee release
          in `finally` blocks so test runners never hang on timeouts?
        - Are blocking calls (`get()`, `await()`, thread sleep) strictly
          avoided on the Main thread / looper?
    *   **Resource Lifecycle & Leaks:**
        - Are `ImageProxy` instances deterministically closed (preferring
          `imageProxy.use { ... }`)?
        - Are bitmaps recycled (`bitmap.recycle()`)?
        - Are use cases unbound (`cameraProvider.unbind(...)`) in `finally`
          blocks, and are analyzer callbacks cleared (`clearAnalyzer()`)?
    *   **API Design, Public Contracts & Deprecations:**
        - Are public and restricted (`@RestrictTo`) APIs designed properly?
        - Does the implementation strictly honor the public Javadoc contracts of
          all entry-point APIs (never silently overriding user-provided
          configurations during normal operations, and never leaking parameters
          that a query API documents as ignored)?
        - Avoid introducing deprecated APIs paired with
          `@Suppress("DEPRECATION")` when modern replacements exist (e.g. prefer
          `ResolutionSelector` over `setTargetResolution`).
    *   **Security & Permissions:** Are required Android permissions requested
        via `GrantPermissionRule`? Does storage access comply with Scoped
        Storage (API 29+ MediaStore vs. pre-29 public directories)?
    *   **Style, Readability & Line Lengths:**
        - Strictly enforce line length limits: **80 characters** for markdown
          documentation and `AGENTS.md`, **72 characters** for Git commit
          messages, and **100 characters** for Kotlin source code.
        - Ensure idiomatic Kotlin (clean string templates, multiline strings via
          `trimIndent()`, avoidance of brittle `.format()` concatenations).
        - Verify Kotlin formatting using `./gradlew :ktCheckFile --format`.
    *   **Consistency:** Are naming conventions, error handling patterns, and
        logging mechanisms consistent (preferring `androidx.camera.core.Logger`
        over ad-hoc `android.util.Log` unless justified)?
    *   **Testing Coverage & Minimal Test Footprint:**
        - Are there sufficient tests covering happy paths, negative paths,
          boundary thresholds, and error states?
        - Are tests using fluent Google `Truth` assertions (`assertThat`) and
          compact fakes (`camera-testing`, `camera-camera2-pipe-testing`, or a
          small inline test-local fake such as a local `ImageProcessor`) rather
          than mocks, multi-session test bloat, or custom binary/container
          parsers when standard platform extractors or metadata checks suffice?
4.  **Verify Locally:** Execute local compilation, unit tests, and
    non-interactive repohooks
    (`timeout 300 repo upload --dry-run --verify -y --cbr . </dev/null` /
    `runErrorProneRelease`) to validate build and pre-submit stability.
5.  **Formulate Structured Feedback & Deliverable-Specific Audit Checklists:**
    Organize code review feedback into clear, prioritized sections, and apply
    the mode-specific audit criteria matching the active deliverable:
    *   **Mode 1 — Code Review (`git diff`):**
        - **Summary of Changes**: High-level overview of the patch.
        - **Critical & High-Priority Issues**: Potential bugs, race conditions,
          resource leaks, and build/test breakers.
        - **Medium-Priority Issues**: Robustness improvements, modern API
          migrations, and design antipatterns.
        - **Style & Formatting**: Line-length violations, Kotlin idioms, and
          documentation wrapping.
        - **Testing & Coverage**: Missing test cases or assertion improvements.
        - **Clean-Context Elegance & Anti-Quirk-Shortcut Gate**: Challenge any
          diff that violates the Clean-Context Minimal-Diff Bar above or
          disables a user-facing capability via a `Quirk` without ruling out
          Section 6.B `Steps 1–2` and feature-preserving `workaround/`
          adaptations (`camerax-triage-and-quirks` Section 5 *Quirk as Last
          Resort* and Section 6.C item 2).
        - Provide **concrete, copy-pasteable replacement code snippets** for
          each issue, verifying wide codebase context, single responsibility
          ("object job"), preservation of intentional workarounds, AndroidX /
          Kotlin guidelines, and concurrency/lifecycle safety.
    *   **Mode 2 — Issue Investigation, Root-Cause Triage & Behavioral
        Questions (Triage & Question Audit):**
        - **Anti-Confirmation-Bias & End-to-End Call-Chain Check**: Verify the
          raw logcat (including framework `CameraService` / `Camera3-*` and
          vendor HAL / `CCodec` tags; see `camerax-triage-and-quirks` Section 5
          for cross-device log comparison), stack trace, or test log slice from
          the beginning; for codebase "why / how does X work" questions, trace
          the full call chain
          (`UseCase/View/Video/Extensions` ->
          `CameraInternal/CameraUseCaseAdapter/StreamSharing` ->
          `camera-camera2` -> `camera-camera2-pipe`) to confirm the claimed
          root cause is the *first* causal failure (not a downstream symptom)
          and the behavior explanation is complete and accurate.
        - **Code & Blame Verification**: Open every cited `file:line` in CameraX
          and surrounding callers (plus `git --no-lazy-fetch blame` /
          `git --no-lazy-fetch log` when explaining why code exists) to verify
          the code actually behaves as claimed.
        - **Rubric Classification & 5-Gate Audit**: Verify whether the diagnosis
          properly maps to Section 6 (Step 1 Internal Contract vs. Step 2
          Platform Spec vs. Step 3 Hardware/HAL Quirk vs. Sub-case 3B Test-Only
          Constraint) or a physical test fixture/environment defect, and
          satisfies the **5-Gate Deep Root-Cause Protocol**
          (`camerax-triage-and-quirks` Section 6.D).
    *   **Mode 3 — Implementation, Solution Proposal & Architectural Planning
        (Plan Audit):**
        - **Architectural Fit, Object Job & Public Contract Fidelity**: Read all
          target classes, callers, public entry-point Javadocs, and sibling
          implementations; verify each proposed change or solution belongs in
          the right layer/class and honors all documented public API contracts
          (Section 6.F) and the Standards Hierarchy (AndroidX -> Android
          Platform -> Google-Wide).
        - **Completeness & Edge Cases**: Check `@CameraScope` vs.
          `@UseCaseCameraScope`, partial UseCase re-binding vs. full
          unbind/teardown, multi-stream combinations (`StreamSharing`,
          RAW+JPEG), thread/dispatcher confinement, public API/ABI
          compatibility, and missing test coverage.
    *   **Mode 4 — Test / Verification Result Reports & Data Summaries (Report
        Fact-Checking & Accuracy Audit):**
        - **Strict Numeric & Math Reconciliation**: Re-count total targets,
          passed devices, failed devices, and skipped/unallocated devices
          directly from the raw test runner output; verify all table row and
          column totals sum up accurately.
        - **Target & Device Name Accuracy**: Verify target and device name
          parsing, camera lens facing tags, and module disambiguation, and
          confirm every test run URL and issue tracker ID matches the raw output
          verbatim (zero hallucinated or transposed IDs).
        - **Failure Attribution Audit**: Verify that infrastructure and fixture
          failures (device offline or disconnected, test runner timeouts, APK
          installation or storage errors, unpowered/dark test enclosure) are
          strictly separated from real JUnit/CameraX assertion failures, and
          that baseline comparisons accurately distinguish new regressions from
          pre-existing failures.
    *   **Mode 5 — Open-Ended Trade-Offs, Quirk Scoping & Test/Doc Synthesis
        (Multi-Perspective Quality Audit):**
        - **Competing Design Trade-Offs & Batch Quality Scoring**: When multiple
          valid solutions or candidate CLs exist, compare independent model
          perspectives (optionally rating **Elegance** and **Safe-to-Go**
          readiness `1–10`—where `>= 9` denotes minimal diff, zero test-only
          seams, `0 OPEN P0-P2`, and green verification, vs. `<= 6` when open
          `P0-P2` defects or avoidable seams remain) to select the most
          idiomatic, minimal-complexity design.
        - **Quirk Precision, Workaround-First & 5-Gate Anti-Broad-Quirk Audit**:
          Verify that proposed `Quirk` mappings avoid magic heuristics, satisfy
          the Mode 1 anti-quirk-shortcut check (Section 5 *Quirk as Last Resort*
          and Section 6.C item 2), pass the **5-Gate Deep Root-Cause Protocol**
          in Section 6.D (never disabling a working CameraX feature via a broad
          `DeviceQuirk` when only raw-framework or synthetic harness tests fail,
          and never altering fleet-wide test semantics to mask single-device
          defects), check OS version ceiling persistence (Section 6.E), and
          confirm new async/stress tests use deterministic synchronization
          without `Thread.sleep()`.
        - **Documentation & KDoc Integrity**: Confirm public API docs and guide
          updates are accurate, wrapped at 80 characters, and 100% public-safe.
6.  **Universal Clean-Context Multi-Model Verification Loop (Canonical Gate —
    Adaptive 1-Turn Read-Only Audit & Up-to-5-Round Code Convergence):**
    During investigation and solution design (before committing to a fix
    design) and again before delivering any finalized review, code change,
    root-cause investigation, implementation plan, test verification report, or
    complex technical recommendation, run both an in-context completeness check
    and, by default, this canonical clean-context multi-model
    verification-and-correction loop (note: when `AGENTS_INTERNAL.md` Phase 4
    is present, it supplies internal model-routing/CLI commands and
    programmatic loop runners but **never** relaxes the verification gates,
    zero-priming rules, or convergence exit criteria below):
    *   **Orchestrator Trigger Scope, Cross-Family Model Routing & Evidence
        Handoff (`read_file` Reference — Coordinators Only):** When
        orchestrating verification (Checkpoints 1 & 2, live model-catalog
        probing, author vs. opposite-family verifier routing, quota-floor
        stability, user mode notification, and Mode 1–5 raw evidence / draft
        handoff), read
        `camera/.agents/skills/camerax-code-review/MULTI_MODEL_HARNESS.md` in
        full (read-only leaf verifiers skip `MULTI_MODEL_HARNESS.md`).
    *   **Zero-Priming ("Second-Console") Briefs, Orthogonal Critic Lenses,
        Shared-Board Isolation & Per-CL Granularity:**
        To replicate the bug-finding effectiveness of opening a brand-new AI
        console on the repository, eliminate all author priming and attention
        dilution when dispatching verifiers:
        - **Zero Author Narrative in All Verifier Briefs (Modes 1–5)**: In every
          verifier brief, include: (a) the user's original problem statement or
          bug symptoms verbatim, (b) the workspace path and target `git diff` /
          `git show` command or raw evidence/draft file paths, and (c) standing
          onboarding rules (`camera/AGENTS.md` scope invariants and read-only
          discipline). **Never** include the author's hypothesis, design pitch,
          rationale for why the fix is right, defect ledger, or a list of prior
          review items asking the verifier to "confirm the fix is clean"—let
          the verifier inspect the raw evidence, commit message, and full
          updated code cold.
        - **Evidence-First Blind Diagnosis & Two-Turn Read Barrier in Modes
          2–5**: For non-diff audits (investigations, plans, test reports, and
          trade-offs), instruct the verifier to read the raw failure logs, query
          outputs, and surrounding source files **first** in its initial
          tool-call turn and record its own independent diagnosis or numbers
          *before* reading `<scratch_or_tmp>/draft_deliverable.md` in a
          subsequent turn.
        - **Post-Blind Adjudication Turn (Round 2+, Compact Tag Reply — Zero
          Report Rewrites):** Mode 1 Round 1 has no draft or prior ledger and
          runs in **1 single turn / 1 report**. Only *after* the Round 2+
          verifier has posted its independent cold audit, whenever Round `n-1`
          has `PATCHED_PENDING_REAUDIT` or `DISMISSED_WITH_PROOF` items not
          explicitly tagged in Turn 1 (silence in blind Turn 1 is never
          resolution), send a neutral list of those items (location + original
          finding text only; never the author's rationale) as Turn 2 in Mode 1
          (or appended to Turn 2 in Modes 2–5) for a **compact inline status
          reply (`<= 15` lines: `VERIFIED_RESOLVED` / `REGRESSED` / `UPHELD` /
          `REJECTED` -> `OPEN`)** without re-reading files already read in Turn
          1 (targeted `read_file` on an unread cited `file:line` is allowed) or
          rewriting a second report file.
        - **Three Orthogonal Hostile Critic Lenses**: Instruct every verifier to
          audit across three complementary lenses: (1) *Correctness,
          Concurrency & Lifecycle Invariants* (`@CameraScope` vs.
          `@UseCaseCameraScope`, lock/dispatcher confinement, `finally`
          cleanup), (2) *Occam's Razor, Deep Modules & Object Job* (single
          responsibility, right layer placement, eliminating special-case
          branching), and (3) *Public API Contract, Caller Parity & Section 6.A
          User-Impact Gate* (honoring Javadoc contracts, preserving intentional
          behaviors, and rejecting test-only skips for user-triggerable flows).
        - **Shared Message Board / Channel Isolation**: In multi-agent board or
          shared-channel environments, explicitly instruct clean-context
          verifiers **never** to read prior board messages or earlier verifier
          threads so their audit remains uncontaminated.
        - **Per-CL Granularity, Cross-CL Stack Pass & Fix-Scope Boundary Rule**:
          - *Per-CL Granularity (No Batch Dilution):* Never bundle a multi-CL
            stack or a large batch of unrelated bug fixes into a single verifier
            pass. Scope each clean-context verifier worker to **1 logical
            CL/commit** (or a cohesive unit of related files/issues), spawning
            parallel per-CL workers when auditing multiple changes, and follow
            per-CL reviews on a multi-commit stack with **one stack-level
            integration pass** (`git log -p base..HEAD`) to verify cross-CL
            contracts and `Multi-Commit Stack Hygiene`.
          - *Fix-Scope Boundary Rule (Wide Context Reading vs. Strict Scope
            Isolation):* Reading surrounding classes and callers widely is
            mandatory to understand invariants, answer architectural questions,
            and prevent regressions, while handling scope gracefully (`Strict
            Scope Isolation`). Verifiers must separate: (i) **In-Scope Defects
            / Regressions** (bugs, race conditions, resource leaks, incomplete
            fixes along the modified boundary, or regressions introduced by the
            change), which are graded `Critical` / `High` / `Medium` (`P0-P2`)
            and block convergence, from (ii) **Pre-Existing / Out-of-Scope
            Observations** (pre-existing design trade-offs in untouched callers,
            follow-up refactoring ideas, or subjective CL-splitting preferences
            on a cohesive fix), which are still reported as helpful
            architectural context (for user Q&A or follow-up tracking bugs) and
            labeled `[Optional / Out-of-Scope]` (`Low / P3`) so they **never**
            block the `0 OPEN Critical/High/Medium` convergence exit gate or
            force out-of-scope edits.
          - *Artifact-Backed Audit Handoff:* For detailed multi-finding audits,
            instruct verifiers to write the full structured report (complete
            code snippets and `file:line` proofs) to a scratch/temp file (e.g.,
            `<scratch_or_tmp>/verifier_<name>_r<N>.md`) and return a concise
            summary table + file path so communication channels remain
            high-signal without losing detail.
    *   **Independent Verifier Onboarding ("New Engineer Brain"):** Each
        spawned verifier sub-agent (`verifier-r1`, `verifier-r2`, `verifier-r3`,
        up to `verifier-r5` when auto-scaling) must start with a clean context
        (without the author's chain-of-thought) and be instructed to read this
        `AGENTS.md` (Code Review Guidelines, Step 5 Mode Checklist, Section 6 &
        Section 8 Rubrics, and Standards Hierarchy), `AGENTS_INTERNAL.md` (if
        available per the matrix below), the target diff or draft deliverable,
        the raw evidence files, and the surrounding CameraX codebase widely.
        Read-only verifiers must never edit files, switch `git` branches, or
        run parallel `./gradlew` builds.
        In Round 2+, keep **Turn 1 100% cold and full-diff** (`git show` /
        `git diff` plus surrounding classes; never pass incremental-only diffs
        or prior finding lists in Turn 1) before any adjudication turn.
        - **Role-Scoped Super CameraX SWE Context Matrix:**
          Verifiers load full CameraX architectural/review knowledge for their
          scope while skipping coordinator-only harness files:
          1. *Core Super CameraX SWE Review Bundle (Always Load by Scope):*
             Read `camera/AGENTS.md` + `camerax-code-review` +
             `camerax-guidelines-governance`.
          2. *Domain-Conditional CameraX Skills (Load Only When In Scope; Skip
             When Out of Scope):*
             - `camerax-testing`: Load when unit/device tests, fakes, Test
               Pyramid migrations, or Section 1 4-layer package placement are
               in scope.
             - `camerax-triage-and-quirks`: Load when Mode 1 bug-fix / behavior
               changes (`Section 6.B-6.F`), root-cause triage, HAL behavior,
               `Quirk` scoping (`Sections 5-6`), Section 8 capability
               abstractions, test skips/timeouts (`assume*`, `@Ignore`),
               `DeviceQuirk`, Section 6.A test-skip / public-contract audits, or
               public API behavior (`Section 6.A/6.E/6.F`) are in scope.
          3. *Execution- & Coordinator-Only Skills (Explicitly SKIP in Read-Only
             Leaf Verifiers):* Read-only leaf verifiers skip
             `MULTI_MODEL_HARNESS.md` and execution/coordinator-only workflow
             guides (unless auditing their modified diff lines or CI/lab
             reports; pure `camera/` read-only leaf reviews without CI/lab
             reports skip internal guides).
          4. *Single-Call Full-File Read Discipline (`<= 780` Lines):* Read
             every `AGENTS.md` / `SKILL.md` guide in **1 single un-sliced
             `read_file` call** and read source files in `<= 780`-line chunks
             (never 80–120 line micro-slices).
    *   **Autonomous Multi-Round Convergence Loop (Adaptive 1-Turn Read-Only
        Audit & Code Convergence up to 5 Rounds):**
        0. *Adaptive Read-Only Depth vs. Multi-Round Code Convergence:*
           For read-only reviews, triage analyses, architectural plans, or
           technical Q&A where no repository code edits are made by the agent,
           exit after **1 cross-family pass** (lightweight) or **2 parallel
           cross-family verifiers in 1 turn** (non-trivial/architectural; see
           item 7). Run a focused Round 2 on read-only tasks only under the
           Fundamental-Pivot Anti-Ping-Pong Guard; iterative multi-round
           convergence (R2–R5) is reserved for code-change repair loops.
        1. *Autonomous Multi-Turn Execution (No Conversational Yielding Between
           Rounds):* Never pause to ask the user for permission between
           intermediate review-and-fix rounds. Autonomously execute the state
           machine (`Spawn Clean-Context Verifier -> Triage & Priority-Fix ->
           Pattern Sweep & Deterministic Gate -> Spawn Next Clean-Context
           Verifier`) in a continuous loop until the convergence exit gate is
           satisfied.
        2. *Ephemeral Convergence Defect Ledger
           (`<scratch_or_tmp>/convergence_ledger.md`):* To prevent context
           compaction or memory drift across rounds, maintain a state-machine
           ledger in a scratch/temp file (never committed to the repository and
           never shown to verifiers during their initial cold audit) tracking
           every finding across rounds:
           - `OPEN (R{n})` -> `PATCHED_PENDING_REAUDIT (v{n+1})` ->
             `VERIFIED_RESOLVED (R{n+1})` or `REGRESSED (R{n+1})`
           - `REVERTED_VERIFICATION_FAILURE` (when a candidate fix breaks
             compilation or tests: revert immediately, log the failed approach
             so later rounds do not re-attempt it, and return the finding to
             `OPEN` so it still blocks the exit gate until an alternative fix
             lands, it is `DISMISSED_WITH_PROOF` and `UPHELD`, or it is marked
             `ESCALATED` to the user)
           - `DISMISSED_WITH_PROOF` (requires exact `file:line` or specification
             proof; must be adjudicated in Round `n+1`'s Post-Blind Adjudication
             Turn as `UPHELD` or `REJECTED` -> `OPEN`)
           - `ESCALATED` (unresolved trade-off or stalled defect surfaced to the
             user after reaching the cap or failing progress)
        3. *Priority-Ordered Repair (`P0 -> P1 -> P2 -> P3`) & Cross-Diff
           Pattern Sweep:* Within each fix pass, apply fixes in priority order:
           `P0 Architectural/Lifecycle` -> `P1 Functional/Concurrency` -> `P2
           Local Logic` -> `P3 Style/Polish` so rounds are never wasted
           polishing code that is about to be restructured. Whenever a bug
           pattern is fixed at one site, sweep the entire diff/stack and
           surrounding class for all instances of the same anti-pattern and fix
           them in the same pass before re-running deterministic checks and
           spawning the next verifier round.
        4. *Dynamic Multi-Round Scaling & Hard Convergence Exit Gate:*
           - **Rounds 1 -> 2 (-> 3) (Cross-Family Chain for Code Edits):**
             Spawn fresh clean-context verifiers across distinct model
             families/tiers (`verifier-r1` -> fix -> `verifier-r2`, adding
             `verifier-r3` when R2 finds `P0-P2` issues or applies a semantic
             edit; early exit per item 7).
           - **Hard Convergence Exit Gate:** Convergence is achieved **only**
             when the latest clean-context verifier round reports **`0` `OPEN`
             (`Critical` / `High` / `Medium` or factual/architectural) issues,
             `0` `REGRESSED` issues, `0` `PATCHED_PENDING_REAUDIT` items, AND
             `0` unadjudicated `DISMISSED_WITH_PROOF` items** in the ledger
             (every semantic edit applied in Round `n` must be confirmed
             `VERIFIED_RESOLVED` in Round `n+1`, and any dismissal in the final
             round receives the `+1` focused check or is marked `ESCALATED`).
           - **Auto-Extension (Rounds 4–5 While Fixes Are Converging):** If
             Round 3 (or Round 4) applies a valid semantic fix
             (`PATCHED_PENDING_REAUDIT`) that actively reduces open defects
             without oscillating (`progress_made == true`, meaning the count of
             open `Critical`/`High`/`Medium` items decreased from the prior
             round or all prior `PATCHED_PENDING_REAUDIT` items resolved without
             new regressions), **automatically extend the loop up to 5 rounds**
             (`verifier-r4`, `verifier-r5`, cycling across model families) so
             post-fix changes are audited to true convergence. Otherwise (if a
             semantic fix was applied with `progress_made == false`), or if
             Round 5 still requires a semantic edit, run at most **1 focused
             post-fix clean-context check**; if any `Critical`/`High`/`Medium`,
             `PATCHED_PENDING_REAUDIT`, or unadjudicated item remains, stop and
             mark the remaining items `ESCALATED` to the user (never deliver as
             converged while `PATCHED_PENDING_REAUDIT` > 0).
        5. *Recursive Post-Edit Re-Verification Invariant (Parallel !=
           Sequential; Zero Unverified Semantic Edits):*
           - Running multiple verifiers in parallel on the **same** code or
             draft snapshot counts as **1 round** (cross-perspective
             discovery), **not** as sequential verification rounds.
           - **Every** semantic code or design modification made after a
             verification round (or during Stage 2 compilation, ErrorProne,
             lint, test fixing, or rebase conflict resolution) transitions its
             ledger entry to `PATCHED_PENDING_REAUDIT` and invalidates prior
             `LGTM` sign-offs. Purely mechanical formatting (`ktfmt` / import
             ordering) or non-semantic `LOW` comment/typo fixes do not
             re-trigger a new round, but any semantic edit **must** be followed
             by a fresh clean-context verification round.
        6. *Anti-Sunk-Cost Dismissal Rule, Material Change Criteria &
           Anti-Oscillation Lock:*
           - The orchestrating agent may **never** silently ignore or discard a
             verifier's `Critical`, `High`, or `Medium` finding; every such
             finding must be fixed (`PATCHED_PENDING_REAUDIT`) or explicitly
             rebutted (`DISMISSED_WITH_PROOF`) with `file:line` evidence in the
             final user report and adjudicated in the next round.
           - Reject any Round `n+1` suggestion that contradicts or reverts a
             prior round's verified fix unless it proves a concrete failure
             mode (reproducible race, crash, test failure, or API violation) or
             a new specification constraint. Document non-obvious ordering,
             lock, or lifecycle invariants in concise inline comments alongside
             a deterministic regression test so subsequent verifiers see the
             constraint; if a design choice returns to a previously rejected
             state (`v1 -> v2 -> v1`), lock the state and surface the trade-off
             (`ESCALATED`) to the user.
           - Before modifying an existing CL or applying verifier refactors,
             inspect patchset/commit history (`git --no-lazy-fetch log -p`,
             `git reflog`, or fetched Gerrit patchsets) and seed prior resolved
             findings into `convergence_ledger.md` so solved bugs never
             ping-pong back across revisions.
        7. *Flexible Early Exit (Adaptive Depth by Deliverable Type &
           Anti-Ping-Pong Guards):*
           - **Read-Only Deliverables (`0` Repository Files Modified — Mode 1
             CL Reviews & Modes 2–5 Non-Code Q&A/Triage/Plans/Reports):**
             Adapt review depth by complexity:
             1. *Lightweight / Localized Reviews* (typos, single-function
                logic, doc updates, localized test additions): **1 cross-family
                blind pass** (`1` opposite-family verifier + coordinator direct
                `file:line` & `git --no-lazy-fetch blame` verification) suffices
                for early exit.
             2. *Non-Trivial / Architectural Reviews* (concurrency, lifecycle
                state machines, cross-class audio/video pipelines, multi-class
                boundary changes, public API, or `DeviceQuirk`/HAL/5-Gate root
                causes): Mandate spawning **2 distinct vendor families in
                parallel**. Because both run in parallel on the same snapshot,
                it executes in **1 turn** (no multi-round sequential ping-pong
                loops or unnecessary token waste) while capturing complementary
                cross-family perspectives on full call chains and OEM device
                quirks (e.g. an `AudioSource` pause/flush review where one
                family caught latch happens-before timing while the other
                traced upstream encoder state transitions and an OEM timestamp
                quirk exception).
             R1 findings merged into `<scratch_or_tmp>/draft_deliverable.md`
             after direct `file:line` verification are logged
             `VERIFIED_RESOLVED (coordinator-verified)` rather than
             `PATCHED_PENDING_REAUDIT` and satisfy the exit gate without R2–R5
             codebase re-audits. *Fundamental-Pivot Anti-Ping-Pong Guard:*
             Spawn a focused Round 2 clean-context check on a non-code
             deliverable **only** when R1 **overturned the core root cause or
             replaced the proposed Checkpoint 1 fix architecture**, or when
             parallel R1 verifiers have an **unresolved factual contradiction**
             on `file:line` / specification behavior.
           - **Post-Review Fix Sign-Off Fast-Path (1 Opposite-Vendor Cold
             Pass):** When applying localized code fixes to a CL after a prior
             cross-family clean-context review or Gerrit review (seeding
             `convergence_ledger.md` from prior review threads & patchset
             history so solved bugs never ping-pong back, with green Stage 2
             compiler/ErrorProne/lint/repohook checks), **1 fresh clean-context
             full-diff round on the opposite model family from the fix author**
             (+ inline Turn 2 `Post-Blind Adjudication Turn` confirming
             `100% VERIFIED_RESOLVED`, `0` `OPEN` `P0-P2`, `0` `REGRESSED`, AND
             `0` unadjudicated `DISMISSED_WITH_PROOF` with each `UPHELD`)
             satisfies convergence immediately (extending to Round 2+ only if
             that pass finds a new `P0-P2` defect or regression).
           - **Cold Newly-Authored Code Changes (Cross-Family R2 Gate):** For
             newly authored production, test, or doc/skill changes, run Round 2
             on a second model so R1 and R2 span distinct model families, and
             stop once cross-family coverage + the Hard Convergence Exit Gate
             hold (auto-scaling through R3–R5 while fixes continue converging).
    *   **Read-Only Verifier, Search Discipline & Watchdog Recovery:**
        Verifier sub-agents must never edit repository files, run build/Gradle
        tasks, or run unbounded `find` / recursive `grep` commands across
        virtual/network filesystems or the repository root (prefer indexed code
        search, direct file reads, or path-scoped `git status -- camera/` and
        `git diff aosp/androidx-main...HEAD -- camera/`). Verifiers must
        terminate any lingering background shell tasks before exiting so parent
        sessions never hang waiting on child processes, and orchestrators should
        use a watchdog timeout to recover if a sub-agent stalls on an orphaned
        background process (see `AGENTS_INTERNAL.md` when configured).
7.  **Finalize / Present Deliverable:** Present the verified deliverable (noting
    the verification mode and rounds completed) or proceed to the pre-upload
    quality gate before committing or uploading.



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
