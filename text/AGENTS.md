# Project Tate: Vertical Text Layout (Group ID: `androidx.text`)

## Project Structure

| Module | Directory | Description |
| --- | --- | --- |
| `:text:text-vertical` | `text/text-vertical/` | Core vertical text layout (`VerticalTextLayout`), ruby (`RubySpan`), emphasis (`EmphasisSpan`), and tate-chu-yoko (`TextOrientationSpan.CombineUpright`). |
| `:text:text-vertical-compose` | `text/text-vertical-compose/` | Jetpack Compose integration (`VerticalText`, `VerticalTextScope`, DSL builders). |
| `:text:text-vertical-testapp` | `text/text-vertical/testapp/` | Interactive sample and test app. Update when adding user-facing capabilities. |

## Build & Workflow Rules

- **Scope:** Modify and test only `:text:text-vertical`,
  `:text:text-vertical-compose`, and `:text:text-vertical-testapp` unless asked
  otherwise.
- **Gradle Prefix:** Run from `frameworks/support` with
  `PROJECT_PREFIX=:text,:annotation` (`:annotation` is required so
  `:annotation:annotation-keep-lint` resolves):
  ```bash
  PROJECT_PREFIX=:text,:annotation ./gradlew <tasks>
  ```
- **Pre-Commit Checks:**
  - Format modified Kotlin files: `:ktCheckFile --format --file <file>`
  - Update public API signatures:
    `:text:text-vertical:updateApi :text:text-vertical-compose:updateApi`
  - Run Lint across affected modules: `<module>:lint`
- **Commit Message Stanzas:** Follow
  [manage_commits](../.agents/skills/manage_commits/SKILL.md) and the
  [Relnote guideline](https://g3doc.corp.google.com/company/teams/androidx/releasing_overview.md?cl=head#relnote).
  - `Bug:` must contain a valid Buganizer ID (ask the user if unknown).
  - `Test:` must list `:connectedCheck` for all affected code modules,
    `./gradlew buildOnServer` for version-config-only CLs, or
    `"markdown file change only"` / `"documentation change only"` for doc-only
    CLs.
- **KDoc:** Follow [ktdoc_quality](../.agents/skills/ktdoc_quality/SKILL.md).

## Architecture & Testing

- **API-Level Compatibility:** Vertical text uses platform
  `Paint.VERTICAL_TEXT_FLAG` on API 36+ (`Build.VERSION_CODES.BAKLAVA`) and
  fallback layout/drawing paths on pre-API 36. Verify layout, metrics, and
  rendering across both paths. Use `@SdkSuppress` only for behavior gated to
  specific API levels.
- **Test Strategy (use Google `Truth` `assertThat`):**
  - **Bug Fixes:** Write a failing reproduction test before changing production
    code.
  - **JVM Unit Tests (`src/test/`, `<module>:test`):** Pure API contracts and
    builder validation.
  - **Instrumented Tests (`src/androidTest/`, `<module>:connectedCheck`):**
    `Canvas`, `Paint`, `LayoutRun`, and Compose UI verification.
