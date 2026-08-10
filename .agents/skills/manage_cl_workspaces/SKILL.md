---
name: manage_cl_workspaces
description: Workspace session management for independent or stacked Gerrit CLs with CoG workspaces (gLinux) or Local Git Worktrees (macOS), handling base refs, boundary enforcement, and shared build caches.
---

# Gerrit CL Session Management (AndroidX)

AndroidX development uses isolated workspace providers to give agents fast, isolated working copies for parallel Gerrit CL development without cold-rebuilding or losing state across sessions.

The harness automatically selects the appropriate workspace strategy based on the host environment:
1. **CoG Workspaces (gLinux / Cloudtop)**: Native `git citc create cl_<name> android/platform/superproject --ref=refs/heads/androidx-main --target_branch=refs/heads/androidx-main`.
2. **Local Git Worktrees (macOS / Fallback)**: Git worktree created under `<checkout_root>/.worktrees/cl_<name>/frameworks/support` with auto-symlinked sibling toolchains (`prebuilts`, `tools`, `golden`, `external`, `build`).
3. **Shared Build & Download Caches**: Gradle user home (`~/androidxout/.gradle`) and Konan cache (`~/androidxout/.konan`) are shared across sessions.
---

## Mandatory Agent Execution Directive

> [!IMPORTANT]
> **Before running any build, test, or file edit when this skill is invoked:**
> 1. Verify/provision the isolated workspace by running:
>    ```bash
>    python3 .agents/scripts/cl_session_setup.py sync <conversation_id>
>    ```
> 2. Execute **all** commands, builds, and file modifications with `Cwd` set to the isolated workspace path (`/google/cog/cloud/$USER/cl_<conversation_id>/android/frameworks/support` on gLinux or `.worktrees/cl_<conversation_id>/frameworks/support` on macOS) instead of the main checkout directory.

---
## Workspace Lifecycle Workflow

### 1. Starting a New CL Session

At session start, the `SessionStart` hook invokes [`cl_session_setup.py`](../../scripts/cl_session_setup.py) to provision an isolated workspace:

```bash
# Executed automatically by SessionStart hook:
python3 .agents/scripts/cl_session_setup.py
```

* **On gLinux/Cloudtop (CoG)**: Workspaces live in `/google/cog/cloud/$USER/cl_<name>/android/frameworks/support`.
  *Note*: If CoG workspace creation fails, the harness automatically falls back to `LocalWorktreeProvider`. Setting `ANTIGRAVITY_USE_WORKTREE=1` forces local worktree usage on Linux.
* **On macOS / Fallback (Local Git Worktrees)**: Workspaces live in `<checkout_root>/.worktrees/cl_<name>/frameworks/support`.

#### Default Base Behavior & Mid-Session Syncing

By default, the workspace is automatically provisioned and synced from local workstation `HEAD` (including uncommitted edits) at session start.

If you make new changes in your main workspace mid-session or want to re-target the workspace to a specific commit or Gerrit CL revision:

```bash
# Sync workspace to local workstation HEAD (includes local workstation WIP):
python3 .agents/scripts/cl_session_setup.py sync <conversation_id>

# Sync workspace to a specific target ref or Gerrit CL (ALWAYS use --clean when targeting a specific CL/ref):
python3 .agents/scripts/cl_session_setup.py sync --clean <target_ref> <conversation_id> [workspace_root]
```

> [!IMPORTANT]
> **Target Ref Sync Directive**:
> Whenever syncing to a specific Gerrit CL, change number, or git ref (e.g. `aosp/4209909` or `refs/changes/...`), **ALWAYS pass `--clean`**. Syncing to a target ref implies checking out that exact revision without overlaying local workstation WIP.

> [!NOTE]
> **Stacked Local Commits & CoG Workspace Sync**:
> When `cl_session_setup.py sync` provisions a CoG workspace, local workstation commits are copied over as un-published local draft nodes.
> If the parent commits were already published to Gerrit, CoG will throw `Base <node_id> doesn't exist` when publishing `@`. To resolve this, check if local parent nodes have un-uploaded edits (publish/amend parent first if so), run `git citc patch <parent_cl_number>`, and rebase `@` onto the imported Gerrit node ID as documented in [`manage_commits`](../manage_commits/SKILL.md).

> [!WARNING]
> **Do NOT re-run `cl_session_setup.py sync` to fix Gerrit `Base doesn't exist` publish errors mid-session**, as syncing resets working copy edits. Instead, use `git citc patch` and `git citc api.call Rebase` as documented in [`manage_commits`](../manage_commits/SKILL.md) to resolve base dependencies in place.

#### Committing & Uploading Changes

For creating commits, amending, rebasing, squashing CoG nodes, and uploading changes to Gerrit, refer to the [`manage_commits`](../manage_commits/SKILL.md) skill.

### 2. Isolated Development & Building

When running Gradle commands in the isolated workspace:
* `OUT_DIR` is isolated per session (`~/androidxout/$cl_name`) for both CoG and Git worktree sessions.
* `GRADLE_USER_HOME` (`~/androidxout/.gradle`) and `KONAN_DATA_DIR` (`~/androidxout/.konan`) are shared across sessions.

To build or test in your session (always scope the build using `PROJECT_PREFIX`):
```bash
PROJECT_PREFIX=<project-prefix> ./gradlew <project-path>:<task>
```

### 3. Cleaning Up / Finishing a Session

To clean up a session workspace manually (removes both workspace repository files and `~/androidxout/$cl_name`):
```bash
python3 .agents/scripts/cl_session_setup.py cleanup <conversation_id>
```
