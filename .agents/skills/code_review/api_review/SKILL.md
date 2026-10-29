---
name: androidx-api-review
description: Reviews public and restricted AndroidX API changes in Gerrit CLs against AndroidX and Jetpack Compose API design guidelines.
---

# AndroidX Public API Code Review

Evaluate new or modified public and restricted APIs in this Gerrit change
against the AndroidX and Jetpack Compose API guidelines.

## Scope & Rules

1. **Applicable Guidelines**:
   * **General AndroidX API Guidelines**: Read the relevant files under
     `docs/api_guidelines/` (such as `index.md`, `kotlin.md`, `async.md`,
     `annotations.md`, `compat.md`, `deprecation.md`, `behavior_changes.md`).
   * **Jetpack Compose API Guidelines**: For changes under `compose/` or using
     `@Composable`, also evaluate against
     `docs/api_guidelines/compose_api_guidelines/` (`index.md`, `naming.md`,
     `basic_patterns.md`, `layering.md`, `cross_platform_patterns.md`,
     `terms.md`).
2. **Reporting Findings**:
   * Stay silent on compliant APIs (no praise or compliance summaries).
   * Report only concrete guideline violations bound to changed lines
     (`[FILE:LINE]`) in the source file or `api/*.txt` file.
   * Cite the specific rule or section from `docs/api_guidelines/` and provide a
     concise recommended fix.
