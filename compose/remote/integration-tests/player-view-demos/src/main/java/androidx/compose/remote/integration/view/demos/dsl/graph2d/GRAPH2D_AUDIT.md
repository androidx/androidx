# graph2d API audit — toward a chart library on top of RemoteCompose

Audited 2026-06-09 against the full surface: 56 public entry points across 10 family files,
~5.8k lines in `lib/`. Companion to `GRAPH2D_PLAN.md` (which stays the catalog/roadmap doc);
this file records what's wrong/missing and the remediation plan.

## What's already good (keep)

- Charts are **`RcScope` canvas components** taking a trailing `(theme, modifier)` — they compose
  like any canvas, size reactively, and the parameter order is uniform. This is the right shape.
- Natural naming (`barChart`, `boxPlot`, `heatmap`, `sparkline`) matches how people name charts —
  agents can guess the function from the chart name.
- Defaults everywhere: the minimal call is one line for all 56 charts.
- `GraphTheme` (~90 fields) centralizes all styling; verified end-to-end (demo 0165).
- Host-computed stats/ticks + reactive geometry split is sound and respects the 32-token cap.

---

## A. Architecture findings

### A1. Two-tier API split (the biggest structural issue)
The `chart2d {}` grammar covers only **bar/line/area** — 3 of ~46 chart types. The other ~40
charts are sealed one-shot functions with bespoke render paths. Consequences:

- **No composition.** Can't overlay a regression line on anything via the builder, can't put
  error bars on a bar chart, can't mix bars + line (pareto is a hardcoded special case).
- **Capability lottery.** `animate`, `interactive`, `subtitle`, `valueLabels`, and every
  `AxisSpec` knob (`min/max`, `tickCount`, `format`, `grid`, `includeZero`) exist **only** in the
  builder; the 40 one-shot charts get whichever params their author happened to expose.
  `histogram` can't set tick count; `scatterPlot` can't format ticks; almost nothing can animate.
- Agents can't predict what's available where; the docs can't be written generically.

**Fix:** promote `ChartScope` to a layered grammar. Marks become *layers* sharing one `Cartesian`:
`bars() / line() / area() / scatter() / errorBars() / band() / boxes() / candles() / refLine() …`,
drop the single-family restriction, dispatch z-ordered layers in `renderChart`. Every cartesian
one-shot chart becomes a thin wrapper over layers (keeping its signature). Polar/grid/special
engines stay separate but get the same treatment later (`polar2d {}`).

### A2. No live/reactive data path
The locked goal was "reactive-first … live named-variable data", but `SeriesSpec.values` is
`FloatArray` only. Geometry reflows with canvas size, yet **data is baked at author time**. For a
RemoteCompose library this is the differentiator: a doc ships to a player and should re-render
when the host updates a named float array — dashboards without re-sending documents.

**Fix:** add `RcFloat`-array / named-array series overloads + an opt-in reactive domain mode
(`arrayMin/arrayMax(...).flush()` scaling, reactive tick labels via `createTextFromFloat`).
The flush idioms are already proven in `DslXYGraph.kt`. Static data stays the precise fast path.

### A3. Document bloat: data arrays serialized repeatedly
`RemoteComposeState.cacheData` **never dedupes** — each `remoteFloatArray(...)` call writes a new
copy into the document. `LineMarks` calls it 3× per series (path start, append, markers); an
interactive line chart serializes each series a 4th time in `TouchMarks`. A 5-series interactive
chart ships ~20 copies of the data.

**Fix:** resolve each `SeriesSpec` once per render into a `ResolvedSeries` carrying the one
`RcFloat` array handle; pass handles to marks. (Optionally also content-dedupe in the writer —
upstream fix benefits everyone.)

### A4. Packaging
The library lives in `integration-tests/player-view-demos`. To become a real library:
own module (e.g. `:compose:remote:remote-charts`), Kotlin explicit-API mode, drop the blanket
`@Suppress("RestrictedApiAndroidX")` by building on public API, tracked API files, KDoc samples.
Demos stay in player-view-demos.

---

## B. API ergonomics (humans + agents)

### B1. Inconsistent data input shapes — 6 conventions in use
| Shape | Used by |
|---|---|
| `(categories: List<String>, values: List<Number>)` | bar/line family |
| `List<Pair<String, Number>>` | pie, dotPlot, waterfall, treemap, waffle, funnel, pareto… |
| `List<Pair<Number, Number>>` | scatter, roc, qq, connectedScatter |
| `List<Triple<String, Number, Number>>` | dumbbell, slope, gantt, populationPyramid |
| Parallel lists | bandChart(categories, center, lower, upper), errorBarChart, fanChart |
| Named records | `Candle`, `ForestRow`, `Bullet` (the good pattern) |

`Triple` is hostile to both humans and agents — the second/third meanings are unguessable
(`Triple(label, start, end)`? `(label, before, after)`?). **Fix:** named records for every
multi-field row (`GanttTask(label, start, end)`, `DumbbellRow(label, a, b)`, `SlopeRow`,
`PyramidRow`); keep `Pair(label, value)` for the natural case; deprecate Triple overloads.

### B2. Dead/stale bits
- **`ChartScope.title` is never read** — `chart2d { title = "…" }` silently does nothing (title
  only comes from the function parameter). Honor it (`title ?: scope.title`) or remove the var.
- `ChartScope.bare` KDoc says "set by the sparkline shortcut", but `sparkline()` has its own
  render path and never touches `ChartScope`.

### B3. Hard-coded color defaults bypass the theme
~20 shortcuts default `color: Int = 0xFF4C78A8` (et al). A custom `theme.palette` does **not**
flow into scatter/histogram/gauge/etc. — the one styling system has holes. **Fix:** default
`color: Int? = null` → `theme.seriesColor(0)`; add missing `colors`/per-slice params (pie, donut,
funnel, waffle, treemap currently accept none).

### B4. Parameter coverage is inconsistent
`barChart`/`lineChart` lack `xTitle` (scatter has it); `animate` exists on 4 shortcuts only;
`interactive` only on the line family; `subtitle` builder-only. A1's layer unification fixes the
root; in the meantime normalize the high-traffic shortcuts.

### B5. No input validation
Length mismatches are silently clipped (`if (i >= spec.values.size) continue`), NaN passes
through to geometry. Failures are author-time (host), so loud is cheap: add `require()` at every
public boundary with actionable messages ("categories.size=4 but values.size=5"). Agents iterate
against error messages; silence costs them a device round-trip.

### B6. Discoverability at 56 entry points
No index maps "chart I want" → "function + data shape + demo". **Fix:** a catalog table in the
package README/KDoc: *name → function → data shape → demo id → screenshot*, plus a "choosing a
chart" paragraph. This is the single highest-leverage agent aid.

---

## C. Flexibility gaps

1. **Annotations layer (biggest practical gap).** No reference/threshold lines, shaded target
   bands, event markers, or text callouts — every real dashboard needs these.
   Add `refLine(value, label, axis)`, `refBand(lo, hi)`, `annotate(x, y, text)`, `eventLine(cat)`.
2. **Axes:** no log scale; no date/time axis (smart tick stepping + formatting); no secondary
   y-axis (combo charts; pareto's right axis is internal-only); no category label rotation or
   auto-skip (long/dense labels collide today); no explicit tick positions.
3. **Legend:** bottom-only. Add position (right/top/inline) and per-call visibility.
4. **Dashed/styled strokes:** `RcPaintScope.pathEffect(FloatArray)` exists in the stable DSL and
   is unused — dashed gridlines, forecast lines, and target lines are free. Add
   `GraphTheme.gridDash`, `SeriesSpec.dash`. Also: per-series marker shapes (circle-only today),
   optional bar corner radius, gradient fills (shader scope), fontFamily/weight.
5. **Faceting / small multiples:** charts compose via Row/Column already (undocumented pattern);
   add a `chartGrid(rows, cols) {}` helper + a worked demo.
6. **Interaction:** crosshair only. Roadmap: bar tap-highlight + tooltip, scatter
   nearest-point readout, pie slice emphasis, two-finger zoom/pan on numeric axes (addTouch ×2).

---

## D. Missing chart types

**Tier 1 — high value, fits the current engines:**
combo bar+line (needs A1 + dual axis) · bump chart · stream graph · calendar heatmap ·
Marimekko/mosaic · beeswarm (non-overlapping strip) · raincloud (needs layering) ·
hexbin/2D-density · contour (host marching-squares → paths) · horizon chart ·
win/loss + bar sparklines · linear gauge/progress bar · span (floating-bar) chart ·
multi-ring donut / activity rings.

**Tier 2 — new layout engines, still 2D-primitive-safe:**
sunburst (polar partition) · icicle (rect partition; reuse treemap math) · arc diagram ·
sankey/alluvial (node layout + bezier ribbons) · chord diagram ·
candlestick+volume multi-pane (falls out of faceting).

**Out of scope (reaffirm):** geo/maps, network force layouts, word clouds, 3D (own demos).

---

## E. Remediation plan (phased)

**Phase A — Foundation refactor** ✅ *delivered 2026-06-10; no breaking changes — all flat
functions remain (Triple overloads deprecated, everything else source-compatible)*
1. ✅ Layered `ChartScope`: every mark appends a `ChartLayer`, drawn in declaration order on one
   shared `Cartesian`; one-family rule dropped (combo = `bars(...); line(...)`, demo 0166 +
   `comboChart` shortcut). New layer methods: `points`/`xyLine`/`fitLine` (numeric x — the builder
   now supports a continuous x axis), `errorBars`, `band`, `candles`. Migrated onto layers:
   scatterPlot, bubbleChart, regressionPlot, rocCurve, functionPlot, qqPlot, connectedScatter,
   bandChart, fanChart, errorBarChart, candlestickChart. Kept bespoke (own layouts): quadrant,
   dotPlot, dumbbell, slope, pyramid, forest + the polar/grid/special engines.
2. ✅ Array-handle dedup: line/area data resolved into ONE doc array per series, shared with the
   touch crosshair (was 3–4 copies per series).
3. ✅ Named records: `BubblePoint`, `DumbbellRow`, `SlopeRow`, `PyramidRow`, `GanttTask`; Triple
   overloads `@Deprecated` with `ReplaceWith`; demos migrated.
4. ✅ `ChartScope.title` honored (param wins); `bare` KDoc fixed.
5. ✅ `require()` validation: layer-vs-categories size checks, intra-layer size checks, non-finite
   value checks — all at author time with "graph2d: x.size=4 but y.size=5" style messages.
6. ✅ Theme flow-through: hard-coded `0xFF…` defaults became `Int? = null` → `theme.seriesColor(k)`
   (k preserving each chart's old default hue); `colors: IntArray?` added to pie/donut/radialBar/
   rose/polarBar/waffle/treemap; `xTitle` added to bar/line/area shortcut signatures.
   Device-verified: combo/scatter/band/candle/regression/crosshair render correctly; baseline
   grouped-bar capture is byte-identical pre/post refactor.

**Phase B — Flexibility core** ✅ *delivered 2026-06-10*
7. ✅ Annotations layer: `refLine(value, label)` (dashed by default) / `refBand(from, to)` /
   `annotate(category|x, value, text)` / `eventLine(category|x, label)` — all are ChartLayers
   (declaration-order z), work in both categorical and numeric-x charts, both orientations for
   ref marks, and extend the auto-scaled domain so they're always visible. Demo 0167.
8. ✅ Axis upgrades: `AxisSpec.log` (log10 — host `log10` + reactive `LOG` op in `LinearScale`,
   decade ticks via `logAxis`, SI tick labels by default; demo 0169); `AxisSpec.time` (epoch-day x
   values, calendar-aware day/month/year ticks + smart labels via Hinnant civil-date arithmetic —
   no java.time; `daysFromCivil()` public; demo 0170); `AxisSpec.ticks` (explicit values);
   `AxisSpec.labelEvery` + `labelAngle` (declutter/rotate dense category labels); **secondary
   y axis** — `line(..., secondary = true)` + `y2Axis { … }`, right-side labeled axis sharing the
   plot rect (`drawSecondaryAxis`), `comboChart(secondaryLines = true, y2Title = …)`; pareto
   re-expressed on it (old bespoke right-axis code deleted). Demo 0168. Auto-skip stays deferred
   (it needs host knowledge of the live canvas width).
9. ✅ Legend `LegendPosition.Right` (column, reserves right margin) + dashed strokes via the
   paint `pathEffect`: `dashEffect(...)` helper, `GraphTheme.gridDash`, `line(dash = ...)`,
   dashed ref/event lines. Demo 0171 (also exercises rotated labels + right legend).

**Phase C — Reactive data (the RemoteCompose differentiator)**
10. `RcFloat`/named-array series overloads; reactive domain mode + `createTextFromFloat` tick
    labels; live-dashboard demo driven by host-updated named arrays.

**Phase D — Catalog expansion:** Tier 1 list, then Tier 2 (sunburst/icicle first — cheap wins).

**Phase E — Interaction expansion:** bar tooltips, scatter nearest-point, pie selection,
zoom/pan investigation.

**Phase F — Productization**
11. Own module + explicit API + no RestrictedApi suppressions + API tracking.
12. Tests: unit tests for the pure host logic (Ticks, Stats, Scale, treemap layout, margin math);
    screenshot goldens per demo via the DocCapture flow.
13. Docs: catalog README (B6), KDoc example on every entry point, agent guide.

Suggested order: A (1 wk) → B (1 wk) → C (0.5 wk) → D/E interleaved → F before first release.
