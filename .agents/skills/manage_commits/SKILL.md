---
name: manage_commits
description: A skill for creating, amending, formatting, and uploading commits in the AndroidX frameworks/support project
---

# Manage Commits Skill (AndroidX)

Enforces AndroidX conventions for formatting, updating APIs, drafting commit messages, and uploading changes in `frameworks/support`. Follow steps in order.

## Steps

- [ ] Step 0: Workspace Preparation & Branching
- [ ] Step 1: Analyze Changes (Initial Status)
- [ ] Step 2: Format Code, Run Lint, and Update APIs
- [ ] Step 3: Review Final Diff
- [ ] Step 4: Draft Commit Message & Handle Tags
- [ ] Step 5: Commit, Best Practices & Upload

---

### 0. Workspace Preparation & Branching

AndroidX development in the `frameworks/support` directory uses **Git** and the **Repo** tool.

- Ensure you are in the correct workspace directory (`frameworks/support/`).
- Ensure you are on a working branch.
  - Start branch: `repo start <branch_name> .`.
  - You can start a branch even with existing uncommitted changes.

### 1. Analyze Changes (Initial Status)

Identify modified or added files to know what needs formatting and API updates.

- Identify changed and untracked files:
  ```bash
  git status
  ```
- Note modified `.kt`, `.ktx`, `.java` files, and potential public API changes.

### 2. Format Code, Run Lint, and Update APIs

Format modified files, run lint, and update public APIs using the list from Step 1.

- **Kotlin Formatting**:
  - Run `ktfmt` on modified `.kt`/`.ktx` files to auto-correct style violations:
    ```bash
    ./gradlew :ktCheckFile --format --file <file_path>
    ```
    Repeat `--file <file_path>` for multiple files.

- **Java Formatting**:
  - If you modified Java files, run `javaFormat` on the affected module (e.g., `:appcompat:appcompat-resources`):
    ```bash
    ./gradlew :appcompat:appcompat-resources:javaFormat
    ```

- **Markdown Files**:
  - There is no automatic formatter for Markdown (`.md`) files.
  - You **must remove trailing whitespaces** (spaces or tabs at the end of lines) in modified `.md` files before committing.
  - Run the following command to remove trailing whitespaces:
    - On macOS:
      ```bash
      sed -i '' 's/[[:space:]]*$//' <file_path>
      ```
    - On Linux:
      ```bash
      sed -i 's/[[:space:]]*$//' <file_path>
      ```

- **Public API Tracking**:
  - If public APIs changed, you must update the corresponding `current.txt` files in the `api/` directory by running `updateApi` on the affected module (e.g., `:appcompat:appcompat-resources`):
    ```bash
    ./gradlew :appcompat:appcompat-resources:updateApi
    ```
    Or globally (takes longer): `./gradlew updateApi`

- **Lint**:
  - Run the Lint task on the affected module (e.g., `:appcompat:appcompat-resources`) to catch
    correctness issues before they fail presubmit:
    ```bash
    ./gradlew :appcompat:appcompat-resources:lint
    ```
  - Prefer **fixing** the issue. If you must ignore one, suppress the *exact* failure at the
    call site with `@Suppress("IssueId") // b/BUG_ID` (Kotlin) or `@SuppressLint("IssueId") // b/BUG_ID` (Java)
    rather than silencing it broadly
  - Only when a call-site suppression isn't possible, update the module's `lint-baseline.xml`:
    ```bash
    ./gradlew :appcompat:appcompat-resources:updateLintBaseline
    ```

### 3. Review Final Diff

Review the final clean state of the changes (including formatting and API updates) before drafting the commit message.

- Review the full diff of all uncommitted changes:
  ```bash
  git diff
  ```
  Or for staged changes: `git diff --staged`
- Verify only intended changes are included.

### 4. Draft Commit Message & Handle Tags

Write a clear commit message adhering to conventions based on the final diff, and group all tags in a contiguous block at the very end.

- **Subject**: Concise imperative summary (e.g., "Fix Popup positioning offset bugs"), <100 characters.
- **Body**: Explain *why* changes were made (rationale/background), not just *what* changed. Separate from subject with a blank line.

- **`Test:` tag (REQUIRED)**:
  - AndroidX is a test-first repository. Almost all meaningful code changes should include new or updated tests.
  - Every commit MUST have a `Test:` stanza detailing how it was verified. This should describe the test that validates the behavior changed.
  - Provide exact test command:
    `Test: ./gradlew :compose:ui:ui:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=androidx.compose.ui.window.PopupTest`
  - If tests aren't applicable (e.g., docs), provide a clear rationale:
    `Test: markdown file change only`
  - **NEVER** use `none` or `N/A`.

- **`Bug:` or `Fixes:` tag**:
  - For Buganizer issues (provided by the user):
    - Use `Fixes: <bug_id>` for full resolution.
    - Use `Bug: <bug_id>` for partial/tracking.
    - Use the integer ID only (e.g., `484057256`). Do not include the `b/` prefix.
  - Ask the user for bug ID if not provided, or ask user to make one, keep no bugs to a minimum

- **`Relnote:` tag**:
  - Required for changes in release artifacts (source files under `src/main/`, `src/commonMain/`, or `src/androidMain/`, excluding `buildSrc/`).
  - This must be a one-sentence description of the public API change or observable behavior change to a library. Note that relnotes must be specific about the observable changes from the CL that a developer may see.
  - Format: `Relnote: "Developer-friendly release note"` (quotes recommended if it has special characters).
  - Omit entirely if not applicable (e.g., tests, tooling, docs).
  - Use `Relnote: N/A` only to bypass presubmits for minor source-only changes that don't need a public note.

- **`Change-Id:` tag**:
  - **NEVER** generate this tag manually. It is automatically generated by the git commit hook when you first commit.
  - **CRITICAL**: When amending a commit, you **must always preserve the existing `Change-Id` line**.
  - **CRITICAL: NEVER MODIFY OR REMOVE THE `Change-Id` LINE.** Altering it will break Gerrit tracking for patchsets.

### Sample Commit Message

Here is an example of a complete, correctly formatted commit message:

```
Fix: Avoid redundant recomposition in LazyColumn animations

This change optimizes LazyColumn to prevent unnecessary recompositions
when item animations are playing. Previously, even if only the offset
of an item was changing due to animation, a full recomposition was
triggered. By separating the animated offset from the layout pass,
we can achieve smoother animations with less overhead.

Test: ./gradlew :compose:foundation:foundation:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=androidx.compose.foundation.lazy.LazyColumnAnimationTest
Relnote: Improved performance of LazyColumn animations by reducing redundant recompositions.
Fixes: 298765432
Change-Id: Iabcdef1234567890abcdef1234567890abcdef12345
```

### 5. Commit, Best Practices & Upload

- **CRITICAL**: **NEVER** upload or push a CL without explicitly asking the user for permission first.

- **Gerrit & Commit Best Practices**:
  - **One Logical Change Per Commit**: Each Gerrit change should represent a single, complete logical change. Avoid creating multiple local commits for a single feature or bug fix.
  - **Addressing Review Feedback**: If you need to address review feedback, fix presubmit failures, or make minor adjustments, **do not create a new commit**.
  - **Use `git commit --amend`**: Always apply fixes directly to the existing commit using `git commit --amend`. This keeps the Gerrit patchset history clean and keeps all iterations grouped under a single Gerrit Change.
  - **Preserve `Change-Id`**: When amending, ensure the `Change-Id` line at the very end of the commit message is never altered or removed. This is crucial for Gerrit to group your updates as a new patchset under the same CL. If accidentally removed, duplicate CLs will be created and this should be avoided.
  - **Keep Commit Messages Updated**: If your changes alter the scope of the CL, update the body/subject of the commit message during the amend process while preserving the `Change-Id`.

- **Commit Commands**:
  - **New Commit**: `git commit -m "{commit_message}"`
  - **Amend/Update Commit**: `git commit --amend` (ensure message is updated but retains the exact `Change-Id`).

- **Upload**:
  - Upload to Gerrit:
    ```bash
    repo upload --cbr -t .
    ```
    *Note*: `--cbr` uploads the current branch, and `.` specifies the project in the current directory.
    *Tip*: The command may prompt interactively to run hook scripts. You can automate this bypass using either `yes yes | repo upload --cbr -t .` or using the native flags `repo upload --verify -y --cbr -t .`.
  - **Topic (`-t`) Nuances & Rules**:
    - The `-t` flag sets the Gerrit topic to the local branch name.
    - **Cross-Repo Linking (Screenshot Goldens, Prebuilts, etc.)**: Use topics (by passing `-t` during `repo upload` to share the same topic name) to link CLs across repositories (e.g., `platform/frameworks/support` and `platform/frameworks/support-goldens`). This ensures they run presubmits together and are submitted together.
    - **Presubmits**: Presubmit verification for any single CL in a topic acts as the presubmit for all of them.
    - **Same-Repo Changes (Stacked CLs)**: Do NOT use topics to group multiple code changes within the same repository. Instead, those changes should be stacked (dependent commits). Stacked CLs automatically track dependencies and can be tested/submitted incrementally.
    - **Retaining Topics**: When a topic is set on a CL, any updates/amends uploaded to that CL must retain the same topic.
  - **Fallback**: If the above command fails or requires interactive prompts, **do not attempt to proceed interactively**. Report the issue to the user immediately. Agents cannot handle interactive prompts from `repo upload`.

Once uploaded successfully, present the Gerrit URL to the user and explain that Treehugger presubmit checks will run automatically on the Gerrit change page.

### 6. Presubmit Triggering & Monitoring

After committing changes locally, the agent should coordinate verification and upload:

- **Pre-Upload Verification**:
  - Always check if the modified files are formatted correctly using the `ktCheckFile --format` task (or standard `ktFormat` as described in Step 2) to avoid formatting failures.
  - **Pre-run Presubmits**: Run `./development/validate_changes.sh` to catch common issues before uploading.
    ```bash
    ./development/validate_changes.sh
    ```
  - Ask the user if they want to run local unit/instrumentation tests. Use the `run_tests` skill to identify and run Gradle tasks.
  - If public APIs have changed, suggest running the `api_review` skill to update API signature files and review guidelines compliance.
  - **Ask for Presubmits**: Ask the user if they want to start presubmit checks and have you monitor the results.
    - If **yes**, use the CLI option to trigger presubmits immediately upon upload:
      ```bash
      repo upload --cbr -o label=Presubmit-Ready+1
      ```
    - If **no** (or if you already uploaded without the option), run upload normally:
      ```bash
      repo upload --cbr
      ```
      You can later trigger it manually in the Gerrit UI by voting `Presubmit-Ready+1`.
    - **Note on Interactive Prompts**: The upload command can block on interactive verification hooks or upload confirmation prompts. If automating the execution, you should either type "yes" when prompted, or prefix the command using `yes yes | repo upload ...` to bypass them.

- **Post-Upload Monitoring**:
  - A helper script `.agents/skills/manage_commits/scripts/watch_gerrit.py` is available to automate polling and monitoring Gerrit presubmits.
  - **Why to run**: To automate tracking the build status of your CL without checking the Gerrit webpage repeatedly, and automatically trigger presubmits if not already active.
  - **How to run**:
    To monitor the current branch:
    ```bash
    .agents/skills/manage_commits/scripts/watch_gerrit.py
    ```
    To watch a specific CL number or Change-Id:
    ```bash
    .agents/skills/manage_commits/scripts/watch_gerrit.py <cl_number_or_change_id>
    ```
  - **Options**:
    - `-p <patchset_number>`: Watch a specific patchset (defaults to current).
    - `-i <interval_seconds>`: Custom polling interval (defaults to 180s).
    - `--trigger`: Automatically trigger presubmits without prompting.
    - `--no-trigger`: Only monitor status without triggering.
    - `-c`, `--comments`: Watch for reviewer comments (enabled by default). Use `--no-comments` to disable.
  - **Comment Handling Strategy**: `watch_gerrit.py` monitors both presubmit status and reviewer comments by default. When watching changes or when comments arrive, prompt the user to choose a strategy: (1) don't monitor comments, (2) report to user, (3) fix simple obvious comments, or (4) address all comments automatically.
  - **Checking results**: The script exits with `0` on success (`Presubmit-Verified+1`), `1` on build failure, or `2` if a new patchset is uploaded (superseded).
  - If a presubmit check fails, review the failures, diagnose them (using the `auto-repair` or `sponge` skills), and report findings.


