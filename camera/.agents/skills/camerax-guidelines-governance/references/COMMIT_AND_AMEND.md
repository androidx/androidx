# CameraX Workspace, Pre-Upload Gate, Stack Hygiene & Commit/Amend Reference

This companion reference to
`camera/.agents/skills/camerax-guidelines-governance/SKILL.md` defines
workspace/branch setup, Stage 2 pre-upload formatting and repohook gates,
multi-commit stack hygiene, and CameraX commit/amend message rules. Read-only
leaf verifiers skip this file unless auditing commit messages or workflow docs.

## Workspace Baseline, Upstream Sync & Branch Creation

- **Parent AndroidX Baseline (`frameworks/support/AGENTS.md`) & Workspace Modes
  (`repo` vs. CoG)**:
  - When operating in an AndroidX checkout where the repository-root `AGENTS.md`
    and the repo-wide `manage_commits` and `manage_cl_workspaces` skills are
    auto-discovered by upward directory traversal, use the parent AndroidX
    workflow definitions as the baseline (including `gob_vcs` in CoG
    `/google/cog/cloud/...` workspaces where `repo` is not used, grepping
    `settings.gradle` instead of `./gradlew projects`, `<project>:javaFormat`,
    and `Relnote: N/A` for internal `src/main/` or `src/commonMain/` changes),
    while applying CameraX's stricter commit limits (`<= 50` char title),
    `RelNote` omission override, and Stage 1 + Stage 2 Pre-Upload Gate. If
    operating standalone without parent skills, use the standard `repo`
    defaults below.
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

## Mandatory Two-Stage Verification & Pre-Upload Quality Gate

Never skip verification or declare a task complete without executing both
stages:
- **Stage 1: Pre-Finalization Deep Self-Review & Universal Multi-Model
  Verification Loop**:
  Before declaring any code modification, root-cause investigation,
  implementation plan, test verification report, or design trade-off /
  quirk-scoping synthesis complete, agents **MUST** conduct a full diagnostic
  audit against the **Code Review Guidelines** (`camerax-code-review/SKILL.md`),
  including the **Universal Clean-Context Multi-Model Verification Loop** in
  Step 5 & Step 6:
  1. *Wide Structural Design, Object Job & Intentionality:* Read surrounding
     CameraX classes widely (not just changed lines) to verify structural
     design, object responsibility, code placement elegance, and preservation of
     intentional code.
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
     spawning verifiers and execute the canonical Step 6 autonomous convergence
     loop (cross-family verifiers, zero-priming briefs, two-turn read barrier,
     per-CL + stack-level granularity, ephemeral defect ledger, and priority
     repair with pattern sweep until the latest round reports `0` `OPEN`, `0`
     `REGRESSED`, `0` `PATCHED_PENDING_REAUDIT`, and `0` unadjudicated
     `DISMISSED_WITH_PROOF` items). If Stage 2 requires any semantic code fix,
     loop back to Stage 1 (counting toward the same 5-round cap) before
     uploading.
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

## Multi-Commit Stack Hygiene & Anti-Churn Rule

When developing a multi-CL series (e.g. core fix → device quirk → cleanup):
- **Zero Intermediate Test Churn**: Do not leave temporary test workarounds
  (e.g. temporary `assumeFalse`) in earlier commits that are subsequently
  modified or removed in later commits.
- **Self-Contained Atomicity**: Each commit in the stack must compile cleanly,
  pass its unit tests, and be fully self-contained with its own scoped test
  coverage.
- Rebase and squash intermediate workarounds before uploading to ensure clean
  git review histories.

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

---

> [!IMPORTANT]
> **Cross-File Refresh & Size Restriction Guardrail**:
> - **Single-Call `read_file` Budget**: Keep this companion `.md` strictly
>   under `45,000 bytes` and `780 lines` (`46,080 B / 800-line` single-read
>   limit), and keep `camera/AGENTS.md` under `24,000 bytes`.
>   Wrap prose lines at `<= 80` characters and maintain 100% public purity.
> - **Cross-File Refresh**: Keep synchronized with `camera/AGENTS.md` and
>   `camera/.agents/skills/camerax-guidelines-governance/SKILL.md`.
