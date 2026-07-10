---
name: remove-feature-flag
description: Use this skill to remove a feature flag when the flag is no longer needed and its
  current state must be made permanent. This skill provides a step-by-step workflow for safely
  removing obsolete feature flags and their associated dead code without introducing regressions
  or observable behavior changes.
---

## Glossary

| Term                | Definition                                                                                                                                               |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Feature Flag**    | A mechanism to toggle functionality without deploying new code. The value can be a boolean, enum, or similar type.                                       |
| **Refactoring**     | The process of restructuring existing computer code without changing its external, observable behavior. Removing a feature flag is a refactoring change. |
| **Dead Code**       | Code that is part of the source but is unreachable and can never be executed.                                                                            |

## Prerequisites

- The feature flag to be removed has a determined static value that is permanently enabled or disabled.
- The project has a standard validation process including tests and linting.

## Limitations

- This skill only covers the removal of the flag and its associated code. It does not cover the process of rolling out or rolling back the feature itself.

## Workflow: How to Remove a Feature flag

The primary goal is to remove the feature flag and codepaths related to the feature flag without any
observable impact on library behavior.

### Step 1: Identify the current value

Determine the final, permanent, static value of the feature flag, which will be used to replace all
usages of the feature flag. For a boolean flag, this will either be `true` or `false`.

### Step 2: Replace usages of the feature flag with the current value

In the codebase, find all instances where the feature flag is used, and replace the usage of
the feature flag with the static value from Step 1.

#### Example:

`ComposeUiFlags.isMyFeatureEnabled` is a boolean feature flag that being replaced with `true`.

Before:
```kotlin
if (myState && ComposeUiFlags.isMyFeatureEnabled) {
    newBehavior()
} else {
    oldBehavior()
}
```

After:
```kotlin
if (myState) {
    newBehavior()
} else {
    oldBehavior()
}
```

### Step 3: Remove dead code

After replacing the feature flag with a static value, some code paths will become unreachable.
This dead code must be removed.

For example:
- If the static value is `true`, then a codepath that is only used when the feature flag was `false`
  is dead code and must be removed.
- If the static value is `false`, then a codepath that is only used when the feature flag was `true`
  is dead code and must be removed.

#### Example:

`ComposeUiFlags.isMyFeatureEnabled` is a boolean feature flag that is being replaced with `true`.

Before:
```kotlin
if (ComposeUiFlags.isMyFeatureEnabled) {
    newBehavior()
} else {
    oldBehavior()
}
```

After:
```kotlin
newBehavior()
```

### Step 4: Update tests

Find any tests that are related to the feature flag. If a test was validating a codepath that was
removed in Step 3, then that test must be removed. If the only place where code is used is in a
test, then that code can still be considered unreachable, and must be removed.

### Step 5: Remove the feature flag

All places where the feature flag is being read to or written should now be removed.
Remove the declaration of the feature flag.

### Step 6: Validate the removal

1.  **Run All Checks:** The project must be validated in the normal manner. Execute all tests and lint checks. All checks must pass.
2.  **Handle API Changes:** The removal of a feature flag can often cause an API change. If it does, the API files must be updated according to project standards.

## Antipatterns

### DO
- **DO** replace the feature flag check with its static, constant value.
- **DO** remove all code paths that have become unreachable (dead code) as a result of the flag's removal.
- **DO** remove any tests that were specifically written to cover the deleted, unreachable code.
- **DO** run all standard project validations (tests, linting) to confirm that the change has no observable impact.
- **DO** update API files if the removal resulted in an API surface change.

### DON'T
- **DON'T** introduce any change to the application's observable behavior. The removal must be a pure refactoring.

### NEVER
- **NEVER** leave dead code or obsolete tests in the codebase after removing a feature flag.
