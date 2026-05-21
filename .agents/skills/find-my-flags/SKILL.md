---
name: find-my-flags
description: Use this skill to find Compose feature flags introduced by a specific git user or email and map them to the library version in which they were added.
---

## Glossary

Term             | Definition
:--------------- | :---------
**Feature Flag** | A toggle mechanism located in any of the Compose flag files (e.g., `ComposeUiFlags`, `ComposeMaterial3Flags`, `ComposeRuntimeFlags`, etc.) to guard regressions or features.
**Flag Module**  | The library sub-component holding the flag: `Ui`, `UiText`, or `Foundation`.
**Version TOML** | The central AndroidX library versioning definition file `libraryversions.toml`.

## Triggers & Realistic Sample Prompts

-   "find all compose feature flags introduced by me"
-   "use git to find when compose 1.12-alpha01 was set as the version for this
    branch, and then find all feature flags in Compose flag files introduced by
    me (<my-email@google.com>) and show me a list of which version it was added
    in"
-   "show me a pretty-printed terminal table of compose flags added by a
    specific email"

## Operational Workflow: How to Discover Flag Versions

Follow this automated workflow to trace and map flags to the library versions
they were introduced in.

### Step 1: Execute the analysis script

The repository includes a dedicated script
`.agents/skills/find-my-flags/scripts/find_my_flags` which automatically queries
the local git history, determines the active user's git config email (or custom
email), parses new additions, and performs mapping relative to the repository
root.

Run the script directly from the repository root:

```bash
./.agents/skills/find-my-flags/scripts/find_my_flags
```

Or query a specific author:

```bash
./.agents/skills/find-my-flags/scripts/find_my_flags --author <email>
```

### Step 2: Expected Output Alignment

The script outputs a space-padded dynamically aligned terminal table:

```
| Feature Flag                                        | Module     | Commit        | Added In Version |
| :-------------------------------------------------- | :--------- | :------------ | :--------------- |
| `isAccessibilityShowOnScreenNestedScrollingEnabled` | Ui         | `83b18f2eae1` | `1.11.0-alpha03` |
| `isCorrectShadowLerpWithNullsEnabled`               | UiText     | `a8f0a350817` | `1.11.0-alpha03` |
| `isConcurrentTextFieldSelectionFixEnabled`          | Foundation | `d27fdc43aba` | `1.12.0-alpha01` |
| `isExploreByTouchHoverHandled`                      | Ui         | `acbdc65476d` | `1.12.0-alpha02` |
| `isInteractionSoundEffectOnClickEnabled`            | Foundation | `e5e99dea07a` | `1.12.0-alpha03` |
```
