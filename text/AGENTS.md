# Project Tate: Vertical Text Layout (`androidx.text`)

## Project Structure

| Module | Directory | Scope |
| --- | --- | --- |
| `:text:text-vertical` | `text/text-vertical/` | `VerticalTextLayout`, `TextOrientation`, `TextOrientationSpan`, `FontShearSpan`, and vertical/horizontal `RubySpan` and `EmphasisSpan`. |
| `:text:text-vertical-compose` | `text/text-vertical-compose/` | `VerticalText`, `VerticalTextStyle`, and `buildVerticalText` / `VerticalTextScope` DSL. |
| `:text:text-vertical-testapp` | `text/text-vertical/testapp/` | Demo and test app (`VerticalTextSampleActivity`; outputs in `../../out/androidx/text/text-vertical-testapp/`). Update for user-visible changes. |

## Build & Workflow Rules

- **Scope & Gradle:** Touch only `:text:text-vertical`,
  `:text:text-vertical-compose`, and `:text:text-vertical-testapp` unless asked.
  Include `:annotation` in `PROJECT_PREFIX` so
  `:annotation:annotation-keep-lint` resolves, and run `:updateApi` only on the
  two library modules:
  ```bash
  PROJECT_PREFIX=:text,:annotation ./gradlew <tasks>
  ```
- **Commits:** Follow
  [`manage_commits`](../.agents/skills/manage_commits/SKILL.md) with subject
  prefix `[Tate] `, ask the user if `Bug:` is unknown, and list
  `:connectedCheck` for affected modules in `Test:` (`./gradlew buildOnServer`
  for version-config-only CLs).

## Architecture & Testing

- **SDK Tiers (`24..28`, `29..33`, `34..35`, `36+`):** Verify all four
  `SDK_INT` branches: API 36+ (`BAKLAVA`) uses `Paint.VERTICAL_TEXT_FLAG`;
  API 34–35 (`UPSIDE_DOWN_CAKE`..`VANILLA_ICE_CREAM`) uses
  `getFontMetricsInt(CharSequence, ...)`, `getRunCharacterAdvance(...)`,
  `"vert" on` cluster drawing (`CanvasCompat.kt`), and `1em` vertical advances
  (`PaintCompat.kt`); API 29–33 (`Q`..`TIRAMISU`) uses `getTextRunAdvances(...)`
  and ICU `UCharacter` `VERTICAL_ORIENTATION` (`Orientations.kt`); API 24–28
  uses `getTextWidths(...)`, `getFontMetricsInt(out)`, and
  `Character.UnicodeBlock`.
- **Spans & `TextPaint` State:** `RubySpan` and `EmphasisSpan` render in both
  `VerticalTextLayout` (top-right origin, right-to-left columns;
  `AnnotationPosition.Before` = right, `After` = left) and horizontal
  `StaticLayout` (`ReplacementSpan` via `HorizontalSpanImpl` and
  `cloneWithoutReplacementSpan`; `Before` = above, `After` = below). Copy
  `Paint` parameters with `TextPaint.setFrom(paint)` (`HorizontalSpanHelper.kt`)
  since `Paint.set(Paint)` and `Paint.reset()` skip `TextPaint` fields
  (`bgColor`, `baselineShift`, `underlineColor`); reset pooled `tempPaint`
  before background fills, and scope `Paint` mutations with `withVerticalFlag`,
  `withTextScale`, `withTextScaleX`, or `try / finally`.
- **Testing:** Follow [`run_tests`](../.agents/skills/run_tests/SKILL.md) with
  Google `Truth` (`assertThat`). Keep `src/androidTest/` enabled across all API
  levels via `PaintCompat` (`measureTextVertical`, `getFontMetricsIntCompat`),
  using `@SdkSuppress` only for single-branch tests. Only `:text:text-vertical`
  has `src/test/`; `:text:text-vertical-compose` and
  `:text:text-vertical-testapp` use `:connectedCheck` (`src/androidTest/`).
