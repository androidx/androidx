# LineHeightStyle.Mode.PerLine Design

## 1. Objective

Provide a layout mode in Jetpack Compose text rendering that calculates line height independently for each line.

### Goals
1. **Per-line Resolution**: Calculate line height per line. This lets lines with different font sizes or scripts have different heights.
2. **Tall Script Fallback**: Use the line's natural height if the configured line height is smaller. This prevents clipping tall scripts (such as Thai, Arabic, or Myanmar) or large characters.
3. **Boundary Trimming & Alignment**: Apply vertical alignment and boundary trimming to each line independently using its own metrics.

### Non-Goals
* **No global re-layout**: Do not rewrite the Android platform's StaticLayout or BoringLayout engines.
* **No behavior change by default**: Do not change default line height behavior for existing modes (`Fixed`, `Minimum`, `Tight`).

---

## 2. Proposed Architecture & Final Design

We add the new layout mode `LineHeightStyle.Mode.PerLine` to `LineHeightStyle.Mode`. The Android implementation calculates each line's vertical metrics independently inside the paragraph's `LineHeightSpan` callback.

Traditional modes (`Fixed`, `Minimum`, `Tight`) calculate target metrics once on the first line and cache them. In contrast, `PerLine` calculates and applies metrics to each line:

1. **Get Natural Metrics**: In `chooseHeight`, get the natural `ascent` and `descent` of the current line from `FontMetricsInt`.
2. **Check Height Constraints**: Calculate the difference between the target line height and the line's natural height.
3. **Natural Fallback**: If the natural height is larger than the target height, use the natural height. This prevents clipping.
4. **Apply Spacing and Alignment**: If the target height is larger, add the extra space to the top and bottom of the line using the configured `Alignment` ratio.
5. **Trim Boundaries**: Apply `Trim` policies based on the line index.

---

## 3. Flow/Structure Diagrams

### Case A: Single Line Case (1 Line)

#### 1. Target Height > Natural Height (e.g. 24.sp specified)
```text
Specified Line Height (e.g. 24.sp)
|======================================================|
+------------------------------------------------------+
|   (ascentRatio extra space / top padding)            |
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |
|   Line 1 Text (natural ascent)                       |
|   =================== Baseline ===================   |
|   Line 1 Text (natural descent)                      |
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |
|   (descentRatio extra space / bottom padding)        |
+------------------------------------------------------+
```

#### 2. Case A-short: Target Height <= Natural Height (e.g. 1.sp specified)
```text
Specified Line Height (1.sp) < Natural Height (e.g. 12.sp) -> Falls back to Natural Height
+------------------------------------------------------+
|   Line 1 Text (natural ascent)                       |
|   =================== Baseline ===================   |
|   Line 1 Text (natural descent)                      |
+------------------------------------------------------+  <-- Renders at natural height (NO clipping)
```

### Case B: Multi-line with Mixed Font Sizes (Comparing Modes)

*This exact scenario is validated in unit tests by `LineHeightScriptsTest.script_middleLarge`.*

We use a paragraph with three lines:
- **Line 1 (Large Font)**: Natural Height = 24px.
- **Line 2 (LARGER Font - e.g. Tall script or large span)**: Natural Height = 50px.
- **Line 3 (Small Font)**: Natural Height = 12px.

**Specified Target Line Height** = 30px.

---

#### 1. Mode.Fixed (Default)
Enforces specified height (30px) on all lines, but adds safety margins (paddings) to the outer edges (top of first line, bottom of last line) if the preferred line height is taller.
```text
Line 1 (Large Font): Specified (30px) > Natural (24px) -> Resolves to 30px + safety top padding
+------------------------------------------------------+  ^
|  (safety top padding / extra space)                  |  | 30px (Padded)
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |  |
|  Line 1 Text                                         |  v
+------------------------------------------------------+

Line 2 (LARGER Font): Forced Target (30px) < Natural (50px) -> Enforces 30px (Overflows/Clipped in middle!)
+------------------------------------------------------+  ^
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  | 30px (Forced target)
|  Line 2 Text (Tall Font)                             |  |
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  v
+------------------------------------------------------+

Line 3 (Small Font): Specified (30px) > Natural (12px) -> Resolves to 30px + safety bottom padding
+------------------------------------------------------+  ^
|  Line 3 Text                                         |  | 30px (Padded)
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |  |
|  (safety bottom padding / extra space)               |  v
+------------------------------------------------------+
```

#### 2. Mode.Minimum
Caches the resolved target height of the first line (30px, since specified 30px > natural 24px) and forces all subsequent lines to use this target height.
```text
Line 1 (Large Font): Specified (30px) > Natural (24px) -> Resolves to 30px
+------------------------------------------------------+  ^
|  extra space                                         |  | 30px (Padded)
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |  |
|  Line 1 Text                                         |  v
+------------------------------------------------------+

Line 2 (LARGER Font): Forced Cached Target (30px) < Natural (50px) -> Enforces 30px (Clipped!)
+------------------------------------------------------+  ^
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  | 30px (Forced target)
|  Line 2 Text (Tall Font)                             |  |
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  v
+------------------------------------------------------+

Line 3 (Small Font): Forced Cached Target (30px) > Natural (12px) -> Resolves to 30px
+------------------------------------------------------+  ^
|  Line 3 Text                                         |  | 30px (Padded)
| - - - - - - - - - - - - - - - - - - - - - - - - - -  |  |
|  extra space                                         |  v
+------------------------------------------------------+
```

#### 3. Mode.Tight
Enforces specified height (30px) strictly on all lines with no safety rails or paddings at all.
```text
Line 1 (Large Font): Specified (30px) > Natural (24px) -> Resolves to 30px (No safety padding)
+------------------------------------------------------+  ^
|  Line 1 Text                                         |  | 30px
+------------------------------------------------------+  v

Line 2 (LARGER Font): Forced Target (30px) < Natural (50px) -> Enforces 30px (Clipped!)
+------------------------------------------------------+  ^
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  | 30px
|  Line 2 Text (Tall Font)                             |  |
|  xxxxxxxxxxxxxxxxxxxxx CLIPPED xxxxxxxxxxxxxxxxxxxxx |  v
+------------------------------------------------------+

Line 3 (Small Font): Specified (30px) > Natural (12px) -> Resolves to 30px (No safety padding)
+------------------------------------------------------+  ^
|  Line 3 Text                                         |  | 30px
+------------------------------------------------------+  v
```

#### 4. Mode.PerLine
Calculates line height independently per-line. Uses natural line height as a fallback if the specified line height is too short.
```text
Line 1 (Large Font): Specified (30px) > Natural (24px) -> Pads to 30px
+------------------------------------------------------+  ^
|  extra space                                         |  | 30px (Padded)
|  Line 1 Text                                         |  v
+------------------------------------------------------+

Line 2 (LARGER Font): Specified (30px) < Natural (50px) -> Falls back to Natural (50px) (No clipping!)
+------------------------------------------------------+  ^
|                                                      |  |
|  Line 2 Text (Tall Font)                             |  | 50px (Natural Height)
|                                                      |  v
+------------------------------------------------------+

Line 3 (Small Font): Specified (30px) > Natural (12px) -> Pads to 30px
+------------------------------------------------------+  ^
|  Line 3 Text                                         |  | 30px (Padded)
|  extra space                                         |  v
+------------------------------------------------------+
```

---

## 4. Alternatives Considered & Rejected

### Modifying `LineHeightStyle.Mode.Minimum` behavior directly
* **Description**: Instead of adding a new mode, update the behavior of the existing `LineHeightStyle.Mode.Minimum` to resolve line heights per-line.
* **Why Rejected**: This was rejected because it would introduce an unacceptable breaking behavior change. Existing applications relying on `Minimum` to maintain uniform line heights across a paragraph would see their layout bounds and text spacing change, potentially breaking UI designs.

### Forcing `elegantTextHeight = true` dynamically under `Mode.PerLine`
* **Description**: On API < 35, the system defaults `elegantTextHeight` to `false`, mapping Burmese to the compact font `NotoSansMyanmarUI-Regular.otf` (natural height ~57px at 16sp). On API 35+, it defaults to `true`, resolving to the elegant font `NotoSansMyanmar-Regular.otf` (natural height ~70px at 16sp). To guarantee consistent expanded metrics across all Android versions, we proposed forcing `elegantTextHeight = true` on the `TextPaint` whenever `PerLine` is configured.
* **Why Rejected**: This proposal was rejected because it couples layout alignment behavior (`PerLine`) with typography/font selection (`elegantTextHeight`). Dynamically switching the underlying font fallback family variant (from compact shapes to elegant shapes) based on a line height mode is highly surprising and prevents developers from controlling typography independently.

---

## 5. Compose Multiplatform (CMP) Impact & Target Behavior

### API Exposure in commonMain
`LineHeightStyle.Mode` is a common API. Adding `PerLine` exposes the option to all CMP targets.

### Platform Target Behavior
* **Android**: Fully resolved and applied per-line in `LineHeightStyleSpan.android.kt` using independent line metrics.
* **Non-Android CMP Targets (iOS, Desktop, Web)**: The text layout engines on these platforms (e.g. CoreText for iOS, Skia for Desktop) will initially treat `PerLine` as a fallback to uniform resolution (equivalent to `Minimum`) until they implement native per-line calculations. Adding the common enum value enables future native implementations on these targets without API drift.

---

## 6. Code Links

* [LineHeightStyle.kt](../../ui/ui-text/src/commonMain/kotlin/androidx/compose/ui/text/style/LineHeightStyle.kt)
* [LineHeightStyleSpan.android.kt](../../ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/android/style/LineHeightStyleSpan.android.kt)
* [LineHeightScriptsTest.kt](../../ui/ui-text/src/androidDeviceTest/kotlin/androidx/compose/ui/text/LineHeightScriptsTest.kt)
