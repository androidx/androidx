# CameraX Multi-Model Verification Harness

This companion reference to
`camera/.agents/skills/camerax-code-review/SKILL.md` (Step 6) defines
orchestrator/coordinator trigger scope, 4-role model routing, fail-closed
execution runner gates, live model discovery, billing-aware effort and
quota-floor policies, user notifications, and raw evidence/draft handoff
mechanics. Read-only leaf verifiers skip this file and read
`camerax-code-review/SKILL.md` directly; delegated `runner` workers read
only the *Phase-Matched Search, Fail-Closed Test Runner & `architect`
Authoring* bullet below alongside the fail-closed contract in their brief.

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
    - **Zero-Cost Live Catalog, 4-Role Definitions, Billing-Aware Effort &
      Quota-Floor Stability:** When the harness provides a lightweight
      model-catalog or quota query (zero token cost), always query the live
      catalog before Round 1 to dynamically discover and select, **per role and
      per model family**, the highest-capability healthy model within that
      role's capability class without hardcoding static model versions or
      codenames:
      1. `monitor` *(high-throughput utility class)*: Periodic board/task
         liveness watchdog (`0` code/triage decisions; never extra-high
         effort).
      2. `runner` *(high-throughput utility class)*: Mechanical `./gradlew`
         builds, test execution, and raw log collection (`0` fault attribution
         or code edits).
      3. `architect` *(frontier class)*: Architecture design, state-machine
         implementation, non-PASS test-failure attribution, and code fixes.
      4. `verifier-r{1..5}` *(frontier class)*: Clean-context cross-vendor
         reviewers (with same-family tier fallback when cross-vendor is
         unavailable) for zero-priming Mode 1–5 audits and convergence
         adjudication.
      - *Eligibility & Deterministic Ordering:* Gate eligibility using boolean
        quota health floors rather than sorting by fluctuating remaining-quota
        percentages, honor explicit operator model-preference overrides first at
        rank `#0` within each role's healthy pool, and order remaining
        candidates deterministically by capability tier, semantic generation,
        and output token capacity—strictly excluding lite, image, non-thinking,
        router/compaction, or untiered redirect aliases across all roles (and,
        for frontier `architect` / `verifier-*` roles, also excluding
        utility-class models)—so default selections remain stable across runs
        for reproducible comparison.
      - *Billing-Aware Reasoning Effort:* On per-call/request-billed utility
        pools, `runner` defaults to `high` effort because `high` consumes the
        same single call unit as lower tiers while improving command/log
        fidelity (and may opt into extra-high effort when deep multi-log
        synthesis outweighs latency; on token-weighted utility pools, down-tier
        `runner` to `medium` for routine builds). On token-weighted frontier
        pools and daily quota-limited model families, `architect` and
        `verifier-*` select `high` effort for complex code changes,
        concurrency/lifecycle audits, deep root-cause investigations, or
        architectural plans, down-tier to `medium` for conserving quota or
        routine/lightweight tasks (e.g., documentation edits, simple test/quirk
        updates, or numeric test report fact-checking), and reserve opt-in
        extra-high effort strictly for bounded single-turn adjudication or
        synthesis (`0` multi-turn file-read loops; never auto-select maximum
        tiers unless explicitly requested).
    - **Role Specialization, Fail-Closed `runner` & Opposite-Vendor-First
      Routing:** Read-only post-hoc reviews alone cannot turn a flawed initial
      architecture into clean code if the primary authoring model repeatedly
      patches around its own design (see `AGENTS_INTERNAL.md` when configured):
      1. *Phase-Matched Search, Fail-Closed Test Runner & `architect`
         Authoring:*
         - When distinct utility and frontier capability tiers/classes or model
           families are available, use fast high-throughput utility models
           first for codebase exploration and log/query ingestion while keeping
           single-command checks inline on the coordinator. For long
           multi-module/device `./gradlew` runs or bulk log collection, you may
           delegate mechanical execution to a healthy `runner` worker under a
           single-flight workspace lock (no workspace edits or concurrent
           builds while `runner` runs), arming a watchdog scaled to expected
           build time (e.g., `600s`) and confirming it has stalled (not
           actively compiling or running tests) before any recovery or kill
           action. On `PASS` (`0` exit code, and `> 0` executed tests for test
           tasks), `runner` returns concise pass counts and the raw CLI log
           path without frontier escalation.
         - *Fail-Closed Escalation:* Include this fail-closed contract
           verbatim in every delegated `runner` brief. Any non-PASS test
           status, `0` executed tests on a test task, or non-zero exit code
           escalates deterministically to the frontier `architect` role with
           verbatim exit codes and raw CLI/test log file paths (never replacing
           raw logs with a summary, and allowing at most `1` retry on an
           explicit device-busy allocation timeout on the exact same requested
           device without swapping device models). `runner` never classifies,
           attributes, or fixes failures; `architect` owns root-cause
           attribution and fixes, then re-verifies via a fresh `runner` pass.
         - *Frontier Code-Authoring Escalation Gate:* After the Mandatory
           Pre-Coding Study & Solution-Cooking Gate
           (`camerax-guidelines-governance`) and *before* designing and writing
           non-trivial multi-class, concurrency, or lifecycle state-machine
           code—whenever a stronger frontier-class coding model than the active
           coordinator is healthy—check live quota (preferring cross-vendor
           strongest coding/architecture families when healthy) and switch the
           coordinator model in-place (or delegate code authoring to a dedicated
           `architect` worker).
      2. *Opposite-Vendor-First (`Opposite-Family-First`) Round 1 (`R1`) &
         Vendor Diversity:* When multiple vendor model families are available,
         distinct quota buckets from one vendor provide quota isolation only,
         **never** cross-vendor review diversity. Never start Round 1 (`R1`) on
         the **same** vendor as the active code/draft author when a
         cross-vendor model has available quota; always assign Round 1
         (`verifier-r1`) at both Checkpoint 1 and Checkpoint 2 to an
         **opposite-vendor** model family from the author so correlated
         same-vendor blind spots are broken on the first pass (when only one
         vendor has quota, still prefer a distinct model family/bucket for
         `R1`).
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
      their cross-family verification and 4-role routing rules constitute
      explicit project authorization to pin `verifier-*` and `architect`
      workers to frontier models and pin execution `runner` / `monitor`
      workers to high-throughput utility models when supported by the runtime
      (overriding generic harness tool-schema defaults to leave `model` unset,
      which apply only to unconfigured projects).
*   **Evidence & Draft Handoff Mechanism for Non-Diff Tasks:**
    Because clean-context verifiers start with zero conversation history:
    - For **Code Review (Mode 1)**, if a target CL or commit is not checked
      out at `HEAD` in the initial directory, gracefully locate the matching
      local checkout/branch (`git branch -a`, `git log --all --grep`) or
      fetch the remote change ref (`git fetch aosp refs/changes/...` ->
      `FETCH_HEAD`, or inspect the target VCS workspace/changelist) and
      specify the resolved workspace path and target revision/diff in the
      brief (never pass only an incremental delta since the last round, re-check
      the latest remote Gerrit patchset immediately before delivering any Mode 1
      review, and include `camera/camera-camera2-pipe/AGENTS.md` in the brief
      whenever `camera/camera-camera2-pipe/` is in scope).
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
*   **Orchestrator Convergence State Machine, Auto-Extension (Rounds 4–5),
    Post-Fix Holistic Simplification & Unreviewed-CL Re-Evaluation:**
    - **Ephemeral Convergence Defect Ledger
      (`<scratch_or_tmp>/convergence_ledger.md`):**
      Maintain a state-machine ledger in a scratch/temp file (never committed
      to the repository and never shown to verifiers during their initial cold
      audit) tracking every finding across rounds:
      - `OPEN (R{n})` -> `PATCHED_PENDING_REAUDIT (v{n+1})` ->
        `VERIFIED_RESOLVED (R{n+1})` or `REGRESSED (R{n+1})`
      - `REVERTED_VERIFICATION_FAILURE` (when a candidate fix breaks
        compilation or tests: revert immediately, log the failed approach so
        later rounds do not re-attempt it, and return the finding to `OPEN` so
        it still blocks the exit gate until an alternative fix lands, it is
        `DISMISSED_WITH_PROOF` and `UPHELD`, or it is marked `ESCALATED` to the
        user)
      - `DISMISSED_WITH_PROOF` (requires exact `file:line` or specification
        proof; must be adjudicated in Round `n+1`'s Post-Blind Adjudication
        Turn as `UPHELD` or `REJECTED` -> `OPEN`)
      - `ESCALATED` (unresolved trade-off or stalled defect surfaced to the
        user after reaching the cap or failing progress)
      The ledger exists to prevent context compaction or memory drift across
      rounds. Every semantic edit applied in Round `n` must be confirmed
      `VERIFIED_RESOLVED` in Round `n+1` (invalidating prior LGTM sign-offs),
      and any dismissal in the final round receives the `+1` focused check or
      is marked `ESCALATED`. Fix in priority order so rounds are never wasted
      polishing code that is about to be restructured.
    - **Auto-Extension (Rounds 4–5 While Fixes Are Converging):** If Round 3
      (or Round 4) applies a valid semantic fix (`PATCHED_PENDING_REAUDIT`)
      that actively reduces open defects without oscillating (`progress_made ==
      true`, meaning the count of open `Critical`/`High`/`Medium` items
      decreased from the prior round or all prior `PATCHED_PENDING_REAUDIT`
      items resolved without new regressions), automatically extend the loop up
      to 5 rounds (`verifier-r4`, `verifier-r5`, cycling across model families)
      so post-fix changes are audited to true convergence. Otherwise (if a
      semantic fix was applied with `progress_made == false`), or if Round 5
      still has a semantic edit or unadjudicated `DISMISSED_WITH_PROOF`, run at
      most **1 focused post-fix clean-context check**; if any
      `Critical`/`High`/`Medium`, `PATCHED_PENDING_REAUDIT`, or unadjudicated
      item remains, stop and mark the remaining items `ESCALATED` to the user
      (never deliver as converged while `PATCHED_PENDING_REAUDIT` > 0).
    - **Anti-Sunk-Cost Dismissal, CL-History Seeding & Anti-Oscillation Lock:**
      Never silently ignore or discard a verifier's `Critical`, `High`, or
      `Medium` finding; every such finding must be fixed
      (`PATCHED_PENDING_REAUDIT`) or explicitly rebutted
      (`DISMISSED_WITH_PROOF`) with `file:line` evidence in the final user
      report and adjudicated in the next round. Reject any Round `n+1`
      suggestion that contradicts or reverts a prior round's verified fix
      unless it proves a concrete failure mode (reproducible race, crash, test
      failure, or API violation) or a new specification constraint. Document
      non-obvious ordering, lock, or lifecycle invariants in concise inline
      comments alongside a deterministic regression test so subsequent
      verifiers see the constraint; if a design choice returns to a previously
      rejected state (`v1 -> v2 -> v1`), lock the state and surface the
      trade-off (`ESCALATED`) to the user. Before modifying an existing CL or
      applying verifier refactors, inspect patchset/commit history (`git
      --no-lazy-fetch log -p`, `git reflog`, or fetched Gerrit patchsets) and
      seed prior resolved findings into `convergence_ledger.md` so solved bugs
      never ping-pong back across revisions.
    - **Post-Fix Holistic Simplification & Unreviewed-CL Re-Evaluation:**
      After resolving multi-round findings (`R2+`) on code edits and before
      uploading, audit the full `base..HEAD` diff (`git log -p` per commit
      across stacks) once to collapse patch-on-patch scaffolding and revert
      causally unnecessary legacy hunk churn (any semantic simplification must
      pass a final clean-context check). For author-owned unreviewed CLs (`0`
      human reviewers/comments) scoring `< 9/10` vs. assigned-reviewer or
      other-author CLs, follow the canonical rule in
      `camerax-guidelines-governance/references/COMMIT_AND_AMEND.md`
      (*Unreviewed-CL Holistic Re-Evaluation vs. Assigned/Active Review Lock*).

---

> [!IMPORTANT]
> **Cross-File Refresh & Size Restriction Guardrail**:
> - **Single-Call `read_file` Budget**: Keep this companion `.md` strictly
>   under `45,000 bytes` and `780 lines` (`46,080 B / 800-line` single-read
>   limit), and keep `camera/AGENTS.md` under `24,000 bytes`.
>   Wrap prose lines at `<= 80` characters and maintain 100% public purity.
> - **Cross-File Refresh**: Keep synchronized with `camera/AGENTS.md` and
>   `camera/.agents/skills/camerax-code-review/SKILL.md`.
