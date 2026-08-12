---
name: manage_commits
description: A skill for creating, amending, formatting, and uploading commits in the AndroidX frameworks/support project
---

# Manage Commits Skill (AndroidX)

Enforces AndroidX conventions for formatting, updating APIs, drafting commit messages, and uploading changes in `frameworks/support`. Follow steps in order.

> [!IMPORTANT]
> **CoG / Isolated Workspace Execution Directive (`/google/cog/cloud/...`)**:
> In CoG workspaces, standard `repo` commands and standard repository-modifying `git` commands are **disabled or will fail** (`fatal: not a git repository`).
> You **MUST ONLY** use `git citc` CLI / API tools when operating inside `/google/cog/cloud/...`:
>
> | Action | Standard Git Checkout | CoG Workspace (`/google/cog/cloud/...`) | Notes |
> | :--- | :--- | :--- | :--- |
> | **Branching** | `repo start <branch> .` | *Managed automatically* | **DO NOT** run `repo start`. |
> | **Check Status** | `git status` | `git citc cli.status` | **DO NOT** run `git status`. |
> | **Review Diff** | `git diff` | `git citc cli.diff` | **DO NOT** run `git diff`. |
> | **Commit / Amend** | `git commit -m "msg"` / `--amend` | `git citc cli.describe -m "msg"` | Edits auto-update `@`. **DO NOT** run `git commit`. |
> | **Upload / Publish** | `repo upload --cbr -t .` | `git citc publish` | **DO NOT** run `repo upload`. |
> | **Rebase** | `git rebase <base>` | `git citc api.call Rebase ...` | Rebase via `git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "@" new_base: "<sha>"'`. |
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
- **CoG Workspaces**:
  > [!IMPORTANT]
  > **Provision Clean Draft Node BEFORE Editing Files**:
  > At session start or when starting a new CL, CoG working copy `@` inherits the local workstation state.
  > **BEFORE making any code modifications or running formatting commands**, inspect draft nodes using `git citc api.call GetDrafts ''` and create a clean draft node off your intended parent node:
  > ```bash
  > git citc api.call NewCommit 'repo_root: "android/frameworks/support" parent_node_id: "<parent_cog_node_id>"'
  > ```
  > **Parent Selection Rule**: Before calling `NewCommit`, inspect `git citc api.call GetDrafts ''`. By default, set `parent_node_id` to the **top leaf node of the existing draft stack** (the draft node with no `child_node_ids`). **Exception**: If the top draft nodes are un-published local duplicate drafts (e.g. copied from `cl_session_setup.py sync` with `needs_publish: true` and a `Change-Id` that is already published on Gerrit), select the highest imported node with an assigned `change_number` as your `parent_node_id` instead. Only set `parent_node_id` to upstream `androidx-main` if explicitly requested to create an independent, non-stacked CL.
  > Creating `NewCommit` upfront ensures your edits accumulate directly on a fresh draft node and prevents `NewCommit` from wiping out file edits later in the workflow.

### 1. Analyze Changes (Initial Status)

Identify modified or added files to know what needs formatting and API updates:
- **Standard Git Checkout**: `git status`
- **CoG Workspaces**: `git citc cli.status` and `git citc cli.diff`

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
- **CoG Workspaces**: `git citc cli.diff`

### 4. Draft Commit Message & Handle Tags

Write a clear commit message, placing all tags in a contiguous block at the end:
- **Subject**: Concise imperative summary (<100 characters).
- **Body**: Explain *why* changes were made (rationale/background).
- **`Test:` tag (REQUIRED)**: State exact test command (e.g. `Test: ./gradlew :compose:ui:ui:connectedAndroidTest -P...`) or `Test: markdown file change only`. **NEVER** use `none` or `N/A`.
- **`Bug:` / `Fixes:` tag**: Buganizer integer ID (e.g. `Fixes: 484057256`).
- **`Relnote:` tag**: Required for release artifact changes under `src/main/` or `src/commonMain/`. Use `Relnote: "Description"` or `Relnote: N/A`.
- **`Change-Id:` tag**: Generated automatically on first commit in standard Git checkouts via `commit-msg` hook. In CoG workspaces (`git citc`), generate and append a fresh `Change-Id` (`Change-Id: I<40_hex_chars>`) when drafting a new commit via `git citc cli.describe` to prevent collisions. **NEVER modify or remove `Change-Id` when amending.**

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
> **NEVER upload or push a CL without explicitly asking the user for permission first.**

- **Commit Best Practices**:
  - **One Logical Change Per Commit**: Keep each Gerrit change focused on a single logical change.
  - **Addressing Review Feedback**: Use `git commit --amend` (Standard) or update `@` (CoG). Do not create new commits for feedback or presubmit fixes.
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
  - **Drafting & Message**: Working copy edits automatically update node `@`. Set description and target branch:
    ```bash
    git citc api.call Describe 'repo_root: "android/frameworks/support" node_id: "@" change_metadata { target_branch: "refs/heads/androidx-main" }'
    git citc cli.describe -m "{commit_message}"
    ```
    *Note*: To set or update the commit message of an intermediate/non-@ draft node in a stack, use:
    ```bash
    git citc api.call Describe 'repo_root: "android/frameworks/support" node_id: "<target_node_id>" change_metadata { message: "<commit_message>" }'
    ```
  - **Publishing Single CL**: Run `git citc publish` or `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "@"'`.
  - **Publishing Stacked CLs (Bottom-Up)**:
    > [!IMPORTANT]
    > **CoG Publish Mechanics**: Bare `git citc publish` publishes working-copy node `@`. If `@` is an empty node at the top of a stack, `git citc publish` will report success as a no-op without pushing parent draft nodes that have `needs_publish: true`.
    > To reliably publish stacked CLs, publish each 40-char `node_id` explicitly in bottom-up order using `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<node_id>"'`.

    **Case 1: Publishing In-Session Draft Stacks (A -> B -> C)**
    If all draft nodes were created in the current CoG session, their parentage in `GetDrafts ''` is already established. Simply publish each node from bottom to top:
    1. `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<bottom_parent_node_id>"'`
    2. `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<middle_child_node_id>"'`
    3. `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<top_leaf_node_id>"'`

    **Case 2: Publishing Child of an Un-imported or External Gerrit Parent**
    If the local parent node has an un-imported SHA (e.g. from `cl_session_setup.py sync` throwing `Base doesn't exist`):
    1. Publish/amend parent first or locate parent Gerrit CL number.
    2. Import Parent: Run `git citc patch <parent_gerrit_cl_number>`.
    3. Locate imported 40-char `node_id` in `GetDrafts ''` (has `change_number: <parent_gerrit_cl_number>`).
    4. Rebase Child: `git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "<child_node_id>" new_base: "<imported_parent_node_id>"'`
    5. Publish Child: `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<child_node_id>"'`

  - **Creating Clean Isolated Drafts (`NewCommit`)**:
    Ensure `NewCommit` was called in **Step 0** before making any file edits. Calling `NewCommit` upfront gives `@` a fresh draft node and prevents `NewCommit` from discarding edits made mid-session.
    ```bash
    git citc api.call NewCommit 'repo_root: "android/frameworks/support" parent_node_id: "<parent_cog_node_id>"'
    ```
    > [!IMPORTANT]
    > **Stack Selection Rule**: Before calling `NewCommit`, inspect `git citc api.call GetDrafts ''`. By default, set `parent_node_id` to the **top leaf node of the existing draft stack** (the draft node with no `child_node_ids`). Only set `parent_node_id` to upstream `androidx-main` if explicitly requested to create an independent, non-stacked CL.

    > [!WARNING]
    > **Change-Id Collision Guard**: If `@` inherits a parent draft node's `Change-Id` or if `git citc cli.describe` is run without an explicit fresh `Change-Id`, running `git citc publish` will fail with `ResourceConflictException: A change with Change-Id ... already exists`. When drafting a new commit in a CoG workspace, always call `NewCommit` off the parent node in Step 0 **and** include a fresh explicit `Change-Id: I<40_hex_chars>` stanza in the commit message passed to `git citc cli.describe`.

  - **Amending Existing Gerrit CLs (`Squash`)**:
    To amend an existing Gerrit CL without dropping files:
    > [!CRITICAL]
    > **Working Copy Cleanliness Guard**: Before rebasing `@` to edit a non-leaf node, run `git citc cli.status` and ensure `@` has no working-copy modifications (`The working copy has no changes`). Any uncommitted files in `@` will bleed into the target node when squashed.

    1. Rebase `@` onto the target CL's node ID:
       ```bash
       git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "@" new_base: "<existing_cl_node_id>"'
       ```
    2. Stage edits and verify working copy diff via `git citc cli.diff`.
    3. Squash `@` into the target node: `git citc api.call Squash 'repo_root: "android/frameworks/support" commit_id: "@"'`
       > [!WARNING]
       > **Squash Guard**: NEVER squash a child draft node into a parent draft node (doing so merges child files into the parent commit).
    4. **Validate Stack Graph & Reparent Children (Bottom-Up)**: Call `git citc api.call GetDrafts ''`. When a parent node is squashed, CoG creates a new draft node ID for the parent. You **MUST** verify that child nodes' parent IDs match the new parent node ID. If you have a stack of children (`Child1 -> Child2`), rebase them **sequentially bottom-up**:
       a. Rebase `Child1` onto `NewParent_node_id`:
          `git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "<Child1_node_id>" new_base: "<NewParent_node_id>"'`
       b. Call `GetDrafts ''` to find `Child1`'s newly generated node ID (`Child1_New`).
       c. Rebase `Child2` onto `Child1_New`:
          `git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "<Child2_node_id>" new_base: "<Child1_New_node_id>"'`
    5. **Publish Bottom-Up**: Call `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "<node_id>"'` for each node from bottom to top.
  - **Remote Verification**:
    Verify published files AND remote commit parentage via Gerrit REST API (strip leading magic prefix `)]}'\n` before JSON parsing):
    ```bash
    curl -s "https://android-review.googlesource.com/changes/<CL_NUMBER>/revisions/current/commit" | sed '1s/^)]}'\''//'
    ```
    Assert that `current_revision_number` was incremented and `parents[0].commit` matches the parent CL's current revision commit SHA on Gerrit.
  - **CoG Guards & Stack Integrity**:
    - **Base Error Handling**: If `git citc publish` fails with `Base <node_id> doesn't exist`, `@`'s parent in CoG is an un-imported local SHA (e.g. from `cl_session_setup.py sync`).
      > [!CRITICAL]
      > **NEVER rebase `@` directly onto upstream `androidx-main` to bypass this error.** Doing so detaches `@` from the stack.

      To resolve:
      1. **Find Parent `Change-Id`**: Run `git citc api.call GetDrafts ''` and locate `@`'s parent node (`@-`).
      2. **Check Parent on Gerrit**: Run `curl -s "https://android-review.googlesource.com/changes/?q=<Parent-Change-Id>"` to check if the parent CL is already published.
      3. **Check for Un-Uploaded Parent Edits**: If the local parent node contains un-uploaded local edits not yet on Gerrit, **publish/amend the parent CL first** before proceeding (`git citc publish` on parent node or squash edits) so parent changes are not lost.
      4. **Import & Rebase**:
         - Run `git citc patch <parent_gerrit_cl_number>` to import the parent's published Gerrit revision node into CoG.
         - Find the imported parent `node_id` in `GetDrafts ''` (has `change_number: <parent_gerrit_cl_number>`).
         - Rebase `@` onto that node:
           ```bash
           git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "@" new_base: "<imported_parent_node_id>"'
           ```
      5. **Publish**: Re-run `git citc publish` or `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "..."'`.
    - **Duplicate Change-Id & ResourceConflict Handling**: If publishing fails with `ResourceConflictException: A change with Change-Id ... already exists`, a local draft node in the parent stack has a `Change-Id` that is already published on Gerrit.
      To resolve:
      1. Inspect `git citc api.call GetDrafts ''` for the imported/published node that has `change_number: <cl_number>` for that Gerrit CL.
      2. Rebase `@` directly onto that imported node ID:
         ```bash
         git citc api.call Rebase 'repo_root: "android/frameworks/support" source_node_id: "@" new_base: "<imported_published_node_id>"'
         ```
      3. Re-run publish: `git citc api.call Publish 'repo_root: "android/frameworks/support" node_id: "@"'`.
    - **No `sync` for Base Errors**: Never run `cl_session_setup.py sync` to fix `Base doesn't exist` or publish errors mid-session, as syncing resets working copy edits.
    - **Use CoG Node IDs**: `git citc api.call Rebase` / `NewCommit` / `Publish` require 40-char CoG `node_id`s (from `GetDrafts ''`), NOT 7-digit Gerrit change numbers.


- **Topic Rules**:
  - Use Gerrit topics (`-t`) to link cross-repo CLs (e.g. `support` and `support-goldens`).
  - Do NOT use topics for same-repo changes; stack them as dependent CLs instead.

### 6. Presubmit Triggering & Monitoring

- **Pre-Upload Verification**:
  - Run `./development/validate_changes.sh` before uploading.
  - Confirm formatting (`ktCheckFile`) and public APIs (`updateApi`).
- **Trigger Presubmits**:
  - **Standard Git Checkout**: `repo upload --cbr -o label=Presubmit-Ready+1 .`
  - **CoG Workspaces**: Run `watch_gerrit.py` with `--trigger` after `git citc publish`:
    ```bash
    python3 .agents/skills/manage_commits/scripts/watch_gerrit.py <CL_NUMBER> --trigger
    ```
- **Post-Upload Monitoring**:
  - Use `.agents/skills/manage_commits/scripts/watch_gerrit.py <CL_NUMBER>` to poll presubmit results.
