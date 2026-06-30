# AndroidX Agent Skills

Index of the agent skills in this repository. Each entry links to a `SKILL.md`
with a full, step-by-step workflow. Load a skill when its "use when" matches the
task. For the repo-wide build/test/commit/upload workflow, start from the root
[`AGENTS.md`](../AGENTS.md).

## Repo-wide skills (`.agents/skills/`)

| Skill | Use when |
| --- | --- |
| [`manage_commits`](skills/manage_commits/SKILL.md) | Formatting, updating APIs, drafting the commit message, and uploading a CL to Gerrit. |
| [`run_tests`](skills/run_tests/SKILL.md) | Finding and running unit, connected (instrumentation), or Firebase Test Lab tests for a module. |
| [`api_review`](skills/api_review/SKILL.md) | Reviewing pending public-API changes against the Jetpack API guidelines. |
| [`benchmark`](skills/benchmark/SKILL.md) | Running, analyzing, or writing Compose micro/macrobenchmarks. |
| [`ktdoc_quality`](skills/ktdoc_quality/SKILL.md) | Improving Kotlin KDoc so it is concise, active, and linked to API elements. |
| [`find-my-flags`](skills/find-my-flags/SKILL.md) | Finding Compose feature flags introduced by an author and mapping them to a library version. |
| [`remove-feature-flag`](skills/remove-feature-flag/SKILL.md) | Removing an obsolete feature flag and its dead code without behavior changes. |
| [`health-connect`](skills/health-connect/SKILL.md) | Adding or modifying any Health Connect APIs (under `health/connect/`). |

## Module-scoped skills

These live inside a module's own `.agents/skills/` and apply only within that module.

| Skill | Module | Use when |
| --- | --- | --- |
| [`scaffold-remote-component`](../compose/remote/remote-creation-compose/.agents/skills/scaffold-remote-component/SKILL.md) | `compose/remote/remote-creation-compose` | Scaffolding a new RemoteCompose creation component with test, sample, and preview. |
| [`screenshot_testing`](../compose/remote/remote-creation-compose/.agents/skills/screenshot_testing/SKILL.md) | `compose/remote/remote-creation-compose` | Building Remote Compose screenshot tests with `RemoteScreenshotTestRule`. |
| [`grid_screenshot_testing`](../compose/remote/remote-creation-compose/.agents/skills/grid_screenshot_testing/SKILL.md) | `compose/remote/remote-creation-compose` | Building Remote Compose instrumented tests with `GridScreenshotUI`. |
| [`scaffold-remote-component`](../wear/compose/remote/remote-material3/.agents/skills/scaffold-remote-component/SKILL.md) | `wear/compose/remote/remote-material3` | Scaffolding a new RemoteCompose Wear Material 3 component with test, sample, and preview. |
