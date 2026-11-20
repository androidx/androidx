---
name: create-feature-flag-cleanup-bug
description: >-
  Drafts and files the Buganizer cleanup bug ("[feature-flag] Cleanup <FlagsObject>.<flagName>")
  that every Compose feature flag needs, and links it from the TODO comment above the flag. Use
  when adding a new flag to a Compose flags object (such as ComposeUiFlags or
  ComposeFoundationFlags), when the FeatureFlagSetup lint check reports a flag without a TODO
  bug link, or when asked to create a feature flag cleanup, tracking, or soaking bug. Filing or
  modifying the bug requires explicit approval from the user. Don't use for removing a flag
  (use remove-feature-flag) or for listing flags added by an author (use find-my-flags).
---

# Create a Compose Feature Flag Cleanup Bug

Every Compose feature flag needs a cleanup bug, filed when the flag is added. The bug stays open
while the flag soaks: the flag ships enabled by default, syncs downstream, and reaches a stable
release. Rollout progress is recorded on the bug, and the CL that removes the flag closes it. The
`FeatureFlagSetup` lint check fails for any flag that does not have a `// TODO: b/<bug id>`
comment directly above it.

The flag policy and bug template are documented on go/jetpack-compose/feature-flags. If this skill
and that page disagree, follow the page.

> [!IMPORTANT]
> **Bug creation requires explicit approval from the user.** Filing a bug is an external write
> action. Show the user the complete bug draft (title, fields, and description), and create the bug
> only after the user explicitly approves it in the conversation. Approval to add the flag, edit
> code, or upload a CL is not approval to file a bug, and approval for one bug does not cover
> another. Commenting on or modifying the bug later requires explicit approval in the same way.

## Glossary

| Term             | Definition                                                                                      |
|------------------|-------------------------------------------------------------------------------------------------|
| **Feature flag** | A `Boolean` `@JvmField var` in a flags object that selects between a new and an old code path. |
| **Flags object** | The `object` that holds a module's flags, such as `ComposeUiFlags` or `ComposeFoundationFlags`. |
| **Cleanup bug**  | The Buganizer bug that tracks one flag from when it is added until it is removed.               |

## Workflow

Use your configured Buganizer tool or integration for the read and write steps below if one is
available. Otherwise, give the user the drafted fields from Step 3 and the
go/jetpack-compose/feature-flags link so they can file the bug themselves.

### Step 1: Collect the inputs

- **Flags object and flag name**, for example `ComposeFoundationFlags` and `isMyFeatureEnabled`.
- **Assignee**: the flag owner, usually the user adding the flag. Ask if it is not clear.
- **Component**: defaults to `Compose` (`378604`). If the user's team files its bugs in a
  `Compose` subcomponent (for example `Compose > Text`), use that subcomponent instead. Look up and
  verify any other component ID in Buganizer and confirm it with the user rather than guessing.
- **Related bugs** (optional), for example the issue that the flagged change fixes.

### Step 2: Check for an existing cleanup bug

Search Buganizer (read-only) for `<FlagsObject>.<flagName>` before drafting a new bug. If a
cleanup bug for the flag already exists, use it and go to Step 5.

### Step 3: Draft the bug

Use the Compose cleanup bug fields rather than a default reproduction-steps bug template:

| Field     | Value                                                       |
|-----------|-------------------------------------------------------------|
| Title     | `[feature-flag] Cleanup <FlagsObject>.<flagName>`           |
| Component | `Compose` (`378604`), or the subcomponent from Step 1       |
| Type      | `BUG`                                                       |
| Priority  | `P2`                                                        |
| Severity  | `S2`                                                        |
| Assignee  | The flag owner from Step 1                                  |
| Hotlist   | `Compose Feature Flags` (`7485874`)                         |

Description:

```markdown
`<FlagsObject>.<flagName>` is a temporary flag guarding development, which should be removed along
with the old codepaths once the feature has rolled out. Add comments to this bug for tracking
feature flag rollout progress, and when the flag can be removed.

See go/jetpack-compose/feature-flags for an up-to-date documentation for feature flags.
```

If there are related bugs, add a final line such as `Related issues: b/<id>, b/<id>`.

### Step 4: Get approval, then create the bug

Show the user every field and the description from Step 3, and ask whether to file the bug. Create
it in Buganizer only after the user explicitly says yes, and report the new bug ID. If bug creation
fails, report the error and stop; do not retry in a different component.

If the user says no or Buganizer access is not available, do not create the bug; the user can file
it using the "Create Cleanup Bug" button on go/jetpack-compose/feature-flags, which fills in the
same fields.

### Step 5: Link the bug from the flag

Put the TODO comment directly above the flag's annotations, after its KDoc, matching the existing
flags:

```kotlin
/** Whether to use the new code path for my feature. */
// TODO: b/<bug id>
@field:Suppress("MutableBareField") @JvmField public var isMyFeatureEnabled: Boolean = true
```

Add `Bug: <bug id>` to the commit message of the CL that adds the flag. For the rest of the flag
workflow (tests for both flag states and `Relnote:` rules), see go/jetpack-compose/feature-flags.

## Tracking the soak

Record the flag's rollout on the cleanup bug: when its default value changes, when it syncs
downstream enabled by default, and when a stable release ships with it enabled. Remove the flag
during an alpha period once it has synced downstream enabled by default and, if third-party
developers are likely to need it, a stable release has shipped with it enabled. Use the
[`remove-feature-flag`](../remove-feature-flag/SKILL.md) skill for the removal, and put
`Fixes: <bug id>` in the removal CL's commit message so the bug is marked fixed when the CL
merges. Commenting on or updating the bug requires explicit approval from the user, like creating
it.

## Antipatterns

### DO
- **DO** file one cleanup bug per flag.
- **DO** search for an existing cleanup bug before drafting a new one.
- **DO** show the complete draft when asking the user for approval.

### DON'T
- **DON'T** guess component IDs, hotlist IDs, or assignees.
- **DON'T** treat approval for other work (adding the flag, editing code, uploading a CL) as
  approval to file the bug.

### NEVER
- **NEVER** create, comment on, or change the bug without explicit approval from the user.
- **NEVER** put a placeholder or made-up bug number in the TODO comment. The lint check only
  matches the `TODO ... b/<digits>` pattern, so a fake number passes lint and hides the missing
  bug.
