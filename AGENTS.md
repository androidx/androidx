# AndroidX Agent Guide

This is the AndroidX (Jetpack) source tree, `frameworks/support`, checked out through the
AOSP `repo` tool. Libraries live in top-level directories (`activity`, `appcompat`,
`compose`, `core`, …), build logic is in `buildSrc/`, and documentation is in `docs/`. This
guide targets the **AOSP Gerrit / Googler** path — the `repo` tool, Gerrit code review, and
Treehugger presubmit. (The GitHub pull-request flow in `CONTRIBUTING.md` is a separate path
and out of scope here.) All build output is written **two levels above** the source root, in
`../../out`.

Module-specific guidance lives in per-directory `AGENTS.md` files (e.g.
[`camera/AGENTS.md`](camera/AGENTS.md)) — read the one nearest your change. Deeper,
task-specific workflows live in **skills**; see [Skills](#skills) below.

## Canonical commands

Replace `<project>` with the module's Gradle project path. Paths are declared in
[`settings.gradle`](settings.gradle) and *often* mirror the directory (e.g.
`appcompat/appcompat-resources/` → `:appcompat:appcompat-resources`), but many projects point
at a different directory, so don't assume — look up the exact path by grepping `settings.gradle`
(e.g. `grep appcompat-resources settings.gradle`), which is instant. Avoid `./gradlew projects`;
it configures the whole build and is slow.

**Scope work to a module.** Configuring the whole repo is slow, so narrow it. Target a
`<project>` instead of a bare task, and cap which projects Gradle configures with the
**`PROJECT_PREFIX`** env var — a comma-separated list of project-path prefixes matched against
the project name: `PROJECT_PREFIX=:compose: ./gradlew <task>` configures only `:compose:*`
projects; `PROJECT_PREFIX=:core,:appcompat` limits to those groups. In Studio,
`./studiow :core:,:work:` scopes to a subset.

**Tests** (full details in the [`run_tests`](.agents/skills/run_tests/SKILL.md) skill).
**Read it before you test code.**

## Committing & uploading

The canonical workflow — branching (`repo start <branch> .`), formatting, `updateApi`, the
commit-message stanzas (`Test:` required, `Bug:`/`Fixes:`, `Relnote:`, `Change-Id:`), and
`repo upload --cbr -t .` → Treehugger presubmit → amend-the-same-commit to iterate — lives in
the [`manage_commits`](.agents/skills/manage_commits/SKILL.md) skill. **Read it before you
commit or upload.**

## Skills

Repo-wide and module-scoped agent skills are indexed, with one-line "use when" descriptions, in
[`.agents/README.md`](.agents/README.md) — load a skill's `SKILL.md` when its description
matches your task.
