---
name: manage_commits
description: A skill for creating, amending, formatting, and uploading commits in the AndroidX frameworks/support project
---

# Manage Commits Skill (AndroidX)

Enforces AndroidX conventions for formatting, updating APIs, drafting commit messages, and uploading changes in `frameworks/support`. Follow steps in order.

> [!IMPORTANT]
> **CoG / Isolated Workspace Execution Directive (`/google/cog/cloud/...`)**:
> In CoG workspaces, standard `repo` commands and standard repository-modifying `git` commands are **disabled or will fail** (`fatal: not a git repository`).
> You **MUST ONLY** use the `gob_vcs` skill and its `cog_api.py` script.
>
> **NEVER run `repo upload`, `repo start`, `git status`, or `git commit` in a CoG workspace.**

## Steps

- [ ] Step 0: Workspace Preparation & Branching
- [ ] Step 1: Analyze Changes (Initial Status)
- [ ] Step 2: Format Code, Run Lint, and Update APIs
- [ ] Step 3: Review Final Diff
- [ ] Step 4: Draft Commit Message & Handle Tags
- [ ] Step 5: Commit, Best Practices & Upload
- [ ] Step 6: Presubmit Triggering & Monitoring

---

### 0. Workspace Preparation & Branching

Ensure you are in `frameworks/support/` and on a working branch:
- **Standard Git Checkout**: `repo start <branch_name> .`

### 1. Analyze Changes (Initial Status)

Identify modified or added files to know what needs formatting and API updates:
- **Standard Git Checkout**: `git status`

### 2. Format Code, Run Lint, and Update APIs

- **Kotlin Formatting**: Run `ktfmt` on modified `.kt`/`.ktx` files:
  ```bash
  ./gradlew :ktCheckFile --format --file <file_path>
  ```
- **Java Formatting**: Run `javaFormat` on modified Java files:
  ```bash
  ./gradlew <project>:javaFormat
  ```
- **Markdown Files**: Remove trailing whitespaces in `.md` files before committing:
  - macOS: `sed -i '' 's/[[:space:]]*$//' <file_path>`
  - Linux: `sed -i 's/[[:space:]]*$//' <file_path>`
- **Public API Tracking**: If public APIs changed, update signature files:
  ```bash
  ./gradlew <project>:updateApi
  ```
- **Lint**: Run Lint on the affected module:
  ```bash
  ./gradlew <project>:lint
  ```
  Fix issues at call-site (`@Suppress("IssueId") // b/BUG_ID`), or update `lint-baseline.xml` via `./gradlew <project>:updateLintBaseline` if needed.

### 3. Review Final Diff

Review final clean state before drafting the commit message:
- **Standard Git Checkout**: `git diff` (or `git diff --staged`)

### 4. Draft Commit Message & Handle Tags

Write a clear commit message, placing all tags in a contiguous block at the end:
- **Subject**: Concise imperative summary (<100 characters).
- **Body**: Explain *why* changes were made (rationale/background).
- **`Test:` tag (REQUIRED)**: State exact test command (e.g. `Test: ./gradlew :compose:ui:ui:connectedAndroidTest -P...`) or `Test: markdown file change only`. **NEVER** use `none` or `N/A`.
- **`Bug:` / `Fixes:` tag**: Buganizer integer ID (e.g. `Fixes: 484057256`).
- **`Relnote:` tag**: Required for release artifact changes under `src/main/` or `src/commonMain/`. Use `Relnote: "Description"` or `Relnote: N/A`.
- **`Change-Id:` tag**: Generated automatically on first commit in standard Git checkouts via `commit-msg` hook. In CoG workspaces `Change-Id` is also added automatically. **NEVER modify or remove `Change-Id` when amending.**

#### Sample Commit Message

```
Fix: Avoid redundant recomposition in LazyColumn animations

This change optimizes LazyColumn to prevent unnecessary recompositions
when item animations are playing.

Test: ./gradlew :compose:foundation:foundation:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=androidx.compose.foundation.lazy.LazyColumnAnimationTest
Relnote: Improved performance of LazyColumn animations by reducing redundant recompositions.
Fixes: 298765432
Change-Id: Iabcdef1234567890abcdef1234567890abcdef12345
```

### 5. Commit, Best Practices & Upload

> [!CRITICAL]
> **NEVER upload, push or publish a CL without explicitly asking the user for permission first.**

- **Commit Best Practices**:
  - **One Logical Change Per Commit**: Keep each Gerrit change focused on a single logical change.
  - **Addressing Review Feedback**: Use `git commit --amend` (in git). Do not create new commits for feedback or presubmit fixes.
  - **Preserve `Change-Id`**: Always keep the exact `Change-Id` line when amending.

- **Workflow A: Standard Git Checkout**
  - **New Commit**: `git commit -m "{commit_message}"`
  - **Amend Commit**: `git commit --amend`
  - **Upload**:
    - Upload to Gerrit:
      ```bash
      repo upload --cbr .
      ```
      *Note*: `--cbr` uploads the current branch, and `.` specifies the project in the current directory.
      *Tip*: The command may prompt interactively to run hook scripts. You can automate this bypass using either `yes yes | repo upload --cbr .` or using the native flags `repo upload --verify -y --cbr .`.
    - **Topic (`-t`) Nuances & Rules**:
      - **No Topic by Default**: Standard uploads should not set a topic (`repo upload --cbr .`).
      - **Running Multi-CL Presubmits Together**: A topic can be added (e.g. `repo upload --cbr -t <topic_name> .` or `repo upload --cbr -t .` to use the branch name) to run presubmits of multiple CLs together. This is especially useful for cross-repo changes that must be verified as a unit (e.g., linking code in `platform/frameworks/support` with screenshot goldens in `platform/frameworks/support-goldens`). Presubmit verification on any single CL in a topic will test all CLs in that topic together.
      - **Same-Repo Changes (Stacking is Best)**: Within the same repository, do NOT use topics to group multiple code changes. **Stacking commits (dependent commits in git / Gerrit)** is typically the best option. Stacked CLs automatically track dependencies and can be tested and submitted incrementally without topic coupling.
      - **Retaining Topics**: When a topic has been set on a CL (e.g., for cross-repo linking), any updates or amends uploaded to that CL must retain the same topic.
    - **Fallback**: If the above command fails or requires interactive prompts, **do not attempt to proceed interactively**. Report the issue to the user immediately. Agents cannot handle interactive prompts from `repo upload`.

- **Workflow B: CoG Workspaces (`/google/cog/cloud/...`)**
  See gob_vcs skill.
- **Topic Rules**:
  - Use Gerrit topics (`-t`) to link cross-repo CLs (e.g. `support` and `support-goldens`).
  - Do NOT use topics for same-repo changes; stack them as dependent CLs instead.

### 6. Presubmit Triggering & Monitoring

- **Pre-Upload Verification**:
  - Run `./development/validate_changes.sh` before uploading.
  - Confirm formatting (`ktCheckFile`) and public APIs (`updateApi`).
- **Trigger Presubmits**:
  - **Standard Git Checkout**: `repo upload --cbr -o label=Presubmit-Ready+1 .`
  - **CoG Workspaces**: Run `watch_gerrit.py` with `--trigger` after publishing:
    ```bash
    python3 .agents/skills/manage_commits/scripts/watch_gerrit.py <CL_NUMBER> --trigger
    ```
- **Post-Upload Monitoring**:
  - Use `.agents/skills/manage_commits/scripts/watch_gerrit.py <CL_NUMBER>` to poll presubmit results.
