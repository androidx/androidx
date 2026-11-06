# CameraX Multi-Model Verification Harness

This companion reference to
`camera/.agents/skills/camerax-code-review/SKILL.md` (Step 6) defines
orchestrator/coordinator trigger scope, cross-family routing, live model
discovery, quota-floor routing, user notifications, and raw evidence/draft
handoff mechanics. Read-only leaf verifiers skip this file and read
`camerax-code-review/SKILL.md` directly.

*   **Smart & Flexible Trigger Scope (When to Auto-Invoke vs. Skip):**
    - **Auto-Trigger at Both Investigation/Design Time AND Pre-Delivery Time
      (Mandatory by Default)**: Automatically trigger for (1) Code Review /
      Pre-Upload Self-Review, (2) Issue Investigation, Root-Cause Triage &
      Non-Trivial Technical/Behavioral Questions, (3) Implementation,
      Solution Proposals & Architectural Planning, (4) Test / Verification
      Reports & Data Summaries, and (5) Open-Ended Design Trade-Offs, Quirk
      Scoping, Complex Test Synthesis, or any question/task where high
      quality or multiple independent perspectives can uncover a better
      solution.
      - *Checkpoint 1 — Hard Pre-Coding Investigation & Design Gate (Before
        Implementing Production Fixes or Finalizing Any Plan/Triage):*
        When investigating a non-trivial bug, race condition, HAL failure,
        or multi-class change, you may add temporary diagnostic logging or
        write a failing reproduction test first (staging their output as
        raw evidence), but do **not** implement the production fix or
        finalize an investigation/plan without running this gate first.
        Spawn independent clean-context cross-family verifiers (Modes 2, 3,
        and 5) to explicitly verify: (a) the true first causal failure in
        the raw logs, (b) the **Section 6.A User-Impact vs. Test-Only Gate**
        (rejecting test-only workarounds when real application users can
        trigger the issue in library code), and (c) **Deep-Module
        Simplicity & Object Job** (selecting the simplest minimal-state
        design before coding the fix).
      - *Checkpoint 2 — Deterministic Pre-Gate & Recursive Post-Fix
        Convergence Audit:* Run deterministic checks first—formatting
        (`./gradlew :ktCheckFile --format`), compilation/ErrorProne, and
        relevant unit tests—*before* spawning clean-context verifiers (and
        after every fix pass) so reviewers focus 100% on logic, concurrency,
        and architecture rather than syntax or build noise. Then run the
        clean-context verification loop (Modes 1–5), autonomously applying
        fixes and re-verifying in fresh clean contexts until the
        convergence exit gate is satisfied (`0` `OPEN`
        `Critical`/`High`/`Medium` or factual/architectural issues, `0`
        `REGRESSED` issues, and `0` `PATCHED_PENDING_REAUDIT` items).
    - **Skip (Lightweight Flash Single-Fact Lookup or Direct Status
      Check)**: Skip **only** for simple, single-step operational commands
      or status checks (e.g., listing devices, checking device status, or
      initializing a workspace) or a flash single-fact lookup (e.g., a
      single file path, constant value, or exact method signature verified
      via `read_file`) where no root-cause analysis, concurrency/lifecycle
      reasoning, architectural trade-off, solution proposal, or code diff is
      being delivered. Whenever a user question asks *why* a CameraX
      behavior occurs, how to solve/design a feature or fix, or spans
      multiple interacting layers, do **not** skip—stage
      `<scratch_or_tmp>/draft_deliverable.md` and run Modes 2, 3, or 5
      before replying.
*   **Runtime Capability Detection, Live Model Query & Notification:** Check
    whether the agent console/harness supports spawning independent
    sub-agents across distinct model families or tiers (see
    `AGENTS_INTERNAL.md` when configured).
    - **Prefer Cross-Family Multi-Model Verification Over Same-Family
      Tiers:** Do **not** default to different reasoning tiers of the same
      model family if the harness (or `AGENTS_INTERNAL.md` when configured)
      supports dispatching or dynamically registering verifier sub-agents
      across genuinely distinct model families. Fall back to same-family
      reasoning tiers only when cross-family model selection is unavailable
      in the current environment.
    - **Author vs. Verifier Model Specialization & Opposite-Family-First
      Routing:** Read-only post-hoc reviews alone cannot turn a flawed
      initial architecture into clean code if the primary authoring model
      repeatedly patches around its own design. When multiple model families
      are available in the harness (see `AGENTS_INTERNAL.md` when
      configured):
      1. *Phase-Matched Authoring:* Use high-throughput search/tool models
         for fast codebase exploration, log/query ingestion, and test
         runner execution, and switch the active coordinator model in-place
         (or delegate to a dedicated coding/architect worker) on the
         strongest available coding/architecture model family *before*
         designing and authoring non-trivial Kotlin/Java state-machine or
         concurrency changes.
      2. *Opposite-Family-First Round 1 (`R1`):* Never start Round 1 (`R1`)
         on the **same** model family as the active code/draft author when a
         cross-family model has available quota. Always assign Round 1
         (`verifier-r1`) at both Checkpoint 1 and Checkpoint 2 to the
         **opposite** model family from the author so correlated same-family
         blind spots are broken immediately on the first pass.
    - **Zero-Cost Live Catalog, Auto-Discovery & Quota-Floor Stability:**
      When the harness provides a lightweight model-catalog or quota query
      (zero token cost), always query the live catalog before Round 1 to
      dynamically discover and select the highest-capability available
      frontier model in each model family without hardcoding static model
      versions. Gate eligibility using boolean quota health floors rather
      than sorting by fluctuating remaining-quota percentages, and order
      eligible frontier candidates deterministically by capability tier,
      semantic generation, and output token capacity (strictly excluding
      lightweight/lite, image, non-thinking, or untiered redirect aliases)
      so default selections remain stable across runs for reproducible
      comparison. For daily quota-limited model families, match the
      reasoning tier to task complexity: select a high-reasoning tier for
      complex code changes, concurrency/lifecycle audits, deep root-cause
      investigations, or architectural plans, and select a medium/standard
      tier for routine or lightweight tasks (e.g., documentation edits,
      simple test/quirk updates, or numeric test report fact-checking),
      while avoiding quota-heavy maximum tiers unless explicitly requested.
    - **Automatic User Notification:** Inform the user of the active
      verification mode and the models/tiers selected:
      - `CROSS_FAMILY_MULTI_MODEL` (`MULTI_MODEL`): Spawning independent
        verifier sub-agents across distinct model families.
      - `TIER_MULTI_MODEL`: Spawning independent verifier sub-agents across
        distinct reasoning tiers within a single model family (used only
        when cross-family model selection is unavailable).
      - `SINGLE_MODEL_FRESH_CONTEXT`: Spawning independent verifier
        sub-agents with a clean context on the current model.
      - `IN_CONTEXT_FALLBACK` (`UNSUPPORTED`): Falling back to single-agent
        in-context verification.
    - **Model-Pinning Authorization Note:** Because `camera/AGENTS.md` and
      this skill are the user-authored project workflow rules for CameraX,
      their `CROSS_FAMILY_MULTI_MODEL` requirement constitutes explicit
      project authorization to pin verifier workers to cross-family models
      when supported by the runtime (overriding generic harness tool-schema
      defaults to leave `model` unset, which apply only to unconfigured
      projects).
*   **Evidence & Draft Handoff Mechanism for Non-Diff Tasks:**
    Because clean-context verifiers start with zero conversation history:
    - For **Code Review (Mode 1)**, if a target CL or commit is not checked
      out at `HEAD` in the initial directory, gracefully locate the matching
      local checkout/branch (`git branch -a`, `git log --all --grep`) or
      fetch the remote change ref (`git fetch aosp refs/changes/...` ->
      `FETCH_HEAD`, or inspect the target VCS workspace/changelist) and
      specify the resolved workspace path and target revision/diff in the
      brief (never pass only an incremental delta since the last round).
      Verifiers inspect `git status` / `git diff` / `git show` (or the
      workspace VCS diff) and full surrounding source files at that target
      revision directly.
    - For **Non-Diff Deliverables (Modes 2–5)**, the orchestrating agent
      MUST write:
      1. The **Full Updated Draft Deliverable** (e.g.,
         `<scratch_or_tmp>/draft_deliverable.md`) AND
      2. The **Raw Evidence / Source Data** (or exact file paths to raw CLI
         or test runner outputs, saved log slices, and source `file:line`
         references)
      so each clean-context verifier can independently cross-examine the
      complete draft against the raw ground truth without anchoring bias.

---

> [!IMPORTANT]
> **Cross-File Refresh & Size Restriction Guardrail**:
> - **Single-Call `read_file` Budget**: Keep this companion `.md` strictly
>   under `45,000 bytes` and `780 lines` (`46,080 B / 800-line` single-read
>   limit), and keep `camera/AGENTS.md` under `24,000 bytes`.
>   Wrap prose lines at `<= 80` characters and maintain 100% public purity.
> - **Cross-File Refresh**: Keep synchronized with `camera/AGENTS.md` and
>   `camera/.agents/skills/camerax-code-review/SKILL.md`.
