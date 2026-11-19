# `XrSsimMatcher` Mathematical & Architectural Specification

## 1. Why `XrSsimMatcher` Exists

Standard 2D Android screenshot matchers (`PixelPerfectMatcher` and `MSSIMMatcher` in
`androidx.test.screenshot`) are built for bit-exact or 2D Skia UI captures rendered onto uniform
surfaces. On Android XR compositor screenshots (such as CPM / SpaceFlinger captures on Aura), they
fail unpredictably on valid passes and leak real regressions on dark scenes for four structural
reasons:

1. **Non-deterministic subpixel rasterization & crop resampling**:
   Tiny floating-point pose/projection differences, 1 px bounding-box crop jitter
   (`BitmapUtils.scaleBilinear`), and Display P3 -> sRGB conversion shift thin edges by
   **`~0.5–0.75 px`** independently across depth-staggered panels and 3D entities.
2. **Broken `C2` dynamic-range scaling in `MSSIMMatcher`**:
   `androidx.test.screenshot.matchers.MSSIMMatcher` normalizes pixel intensities into `[0.0, 1.0]`,
   yet uses an unscaled luminance constant for `C2` (`(254 * 3e-5)^2 = 5.8e-5` instead of
   `0.03^2 = 9e-4`). Any antialiased 1 px edge block with tiny variance (`var ≈ 1e-3`) and
   near-zero covariance collapses to `SSIM ≈ 0.01–0.26`, pulling global averages down to ~`0.88–0.92`
   on visually identical XR renders.
3. **White-only background skipping vs. dark XR scenes**:
   `MSSIMMatcher` skips only pure-white (`Color.WHITE`) windows. In XR Void / Full Space renders
   (predominantly pure black `Color.BLACK`), every empty black window scores `1.0` and dilutes genuine
   regressions, hiding mid-sized scene errors behind empty background scores.
4. **Global mean dilution in large `1920x1200` viewports**:
   Missing badges, corrupted UI text labels, or small object failures often occupy `< 3%` of pixels,
   leaving a frame's global mean above `0.95`.

`XrSsimMatcher` addresses all four issues with a deterministic 6-stage pipeline:
`4x4` box downsampling + per-window `±0.5 px` bilinear shift alignment, standard `[0.0, 1.0]` SSIM
math, symmetric near-black/near-white foreground gating, worst-channel `min(R, G, B)` scoring, and
a dual `mean >= 0.95` + `p2 >= 0.90` tail acceptance gate.

---

## 2. End-to-End Pipeline

```text
  expected (W x H ARGB)                     given / actual (W x H ARGB)
           |                                             |
           v                                             v
  1x unshifted 4x4 box downsample             9x 4x4 box downsamples with
  (expR, expG, expB)                          bilinear {0, ±0.5 px} shifts
           |                                  (actR[0..8], actG[0..8], actB[0..8])
           |                                             |
           +-------------------+-------------------------+
                               |
                               v
         Slide 8x8 block windows at stride 2
         (= 32x32 source px windows, 8x8 px steps, plus edge tail)
                               |
                               v
         Symmetric Active-Foreground Filter (using unshifted s = 0)
         Skip if ALL near-black (<= 0.01), ALL near-white (>= 0.99),
         nonBlackCells < ceil(0.15 * N), or nonWhiteCells < ceil(0.15 * N)
                               |
                               v (active window)
         For each shift s in 0..8 (short-circuits if >= 1.0 - 1e-9):
           score(s) = min(SSIM(R), SSIM(G), SSIM(B))   [K1=0.01, K2=0.06, N-1 DoF]
         windowScore = max_{s=0..8} score(s)
                               |
                               v
         Dual Gate across active windows:
           1) mean(windowScore) >= 0.95         (global frame fidelity)
           2) sorted[floor(0.02 * M)] >= 0.90   (p2 localized tail fidelity)
                               |
                +--------------+--------------+
                |                             |
             PASS                           FAIL
       (diff = null)               Mark blocks belonging to windows with
                                   windowScore < failWindowFloor and highlight
                                   differing pixels (delta > 4) in MAGENTA
```

---

## 3. Stage-by-Stage Mathematical Breakdown

### Stage 1: Box-Filter Downsampling & `±0.5 px` Bilinear Shift Grid (`downsampleRgb`)

Given source dimensions `W x H` and `f = min(downsampleFactor, min(W, H)).coerceAtLeast(1)`
(default `4`):

- **Downsampled grid dimensions** (ceiling division so right/bottom remainder strips are included):
  ```text
  downWidth  = (W + f - 1) / f
  downHeight = (H + f - 1) / f
  ```
- Each downsampled cell `(bx, by)` averages the source rectangle
  `[bx * f .. min(W, (bx + 1) * f)) x [by * f .. min(H, (by + 1) * f))` and normalizes each RGB
  channel into `[0.0, 1.0]` by dividing by `blockPixels * 255.0`.

#### Why 4x4 box filtering?
A `4x4` box filter acts as a spatial low-pass prefilter:
- High-frequency phase jitter (`~0.5 px` antialiasing fringe along curves or P3-to-sRGB rounding)
  blends smoothly into fractional block averages `[0.0, 1.0]`.
- Real layout or structural geometry shifts (`>= 2 px`) still change boundary block intensities by
  `>= 50%` (e.g., 2 out of 4 columns flip between foreground and background), preserving high
  sensitivity to real layout misalignments.

#### How the 9 bilinear `±0.5 px` shifts work (`HALF_SHIFT_X`, `HALF_SHIFT_Y`)
Integer ARGB buffers store pixel centers at integer `(x, y)`. Rather than allocating extra `W x H`
shifted scratch bitmaps, `downsampleRgb` evaluates `given` directly at 9 `(dxHalf, dyHalf)` modes:
`(0,0), (-1,0), (+1,0), (0,-1), (0,+1), (-1,-1), (+1,-1), (-1,+1), (+1,+1)`:

- **Shift `s = 0` (`(0, 0)`)**: Unshifted exact integer lookup `given[y, x]`.
- **Non-zero shifts**: Each source pixel `(x, y)` inside the block averages 4 edge-clamped integer
  samples `(yA, xA)`, `(yA, xB)`, `(yB, xA)`, `(yB, xB)`:
  ```text
  xA = (dxHalf > 0) ? max(0, x - 1)     : x
  xB = (dxHalf < 0) ? min(W - 1, x + 1) : x
  yA = (dyHalf > 0) ? max(0, y - 1)     : y
  yB = (dyHalf < 0) ? min(H - 1, y + 1) : y
  sample(x, y) = (p(yA, xA) + p(yA, xB) + p(yB, xA) + p(yB, xB)) / (4 * 255.0)
  ```

Note why the sign maps cleanly to half-pixel image translation:
- When `dxHalf = +1, dyHalf = 0`: `xA = x - 1, xB = x, yA = yB = y`, computing
  `0.5 * (given[y, x - 1] + given[y, x])` — **shifting `given` content `+0.5 px` right** (to match
  an `expected` feature sitting `0.5 px` farther right).
- When `dxHalf = -1, dyHalf = 0`: `xA = x, xB = x + 1, yA = yB = y`, computing
  `0.5 * (given[y, x] + given[y, x + 1])` — **shifting `given` content `-0.5 px` left**.
- When both are non-zero (`±1, ±1`): computes the exact `0.25`-weighted diagonal `±0.5 px` midpoint.

#### Worked numerical example: Why `±0.5 px` passes AA jitter while catching a `2 px` layout shift
Suppose `expected` has a crisp vertical black-to-white panel step at `x = 60` (`4 * 15`):
- Block 14 (`x in [56..59]`) = `[0, 0, 0, 0] -> 0.000`
- Block 15 (`x in [60..63]`) = `[1, 1, 1, 1] -> 1.000`

1. **Case A — 0.5 px subpixel jitter (`actual[59] = 0.5`, `actual[60..63] = 1.0`)**:
   - Without shifting (`s = 0`): Block 14 = `0.125`, Block 15 = `1.000`. Across a crisp 32x32 edge
     window with no other variance, that edge quantization delta pulls SSIM below `0.90`.
   - With `dxHalf = -1` (sampling `0.5 * (actual[x] + actual[x + 1])` along the border), the filter
     smoothly redistributes the subpixel phase transition across the boundary so best-shift SSIM rises
     above `0.94`.
2. **Case B — `2 px` layout shift (`actual[60..61] = 0`, `actual[62..63] = 1.0`)**:
   - Unshifted Block 15 = `[0, 0, 1, 1] -> 0.500` (vs `expected` `1.000`).
   - Even with maximum `0.5 px` compensation (`dxHalf = -1`), Block 15 only becomes
     `[0, 0.5, 1, 1] -> 0.625` vs `1.000` (a `37.5%` intensity drop along the whole vertical edge),
     keeping window SSIM well below `0.90` so the regression fails reliably.

#### Why shift per window instead of globally?
In 3D XR scenes with multiple depth-staggered panels (such as
`SpatialLayoutScreenshotTest.spatialBox_depthAlignedPanels_rendersInFullSpace`), perspective
projection and independent surface bounds can nudge a foreground left panel `-0.5 px` while nudging
a deeper right panel `+0.5 px`. No single global frame offset aligns both simultaneously; picking
`max_{s=0..8}` independently per sliding window absorbs local fractional raster phase without ever
passing `>= 2 px` panel shifts.

---

### Stage 2: Sliding Window Grid (`computeWindowStarts`)

With defaults `windowSize = 8` and `stride = 2`:
- Each window spans `effWinW x effWinH = 8 x 8 = 64` downsampled blocks (`32 x 32` source pixels).
- Adjacent windows overlap by `6` downsampled blocks (`75%` overlap per axis), advancing `8` source
  pixels per step.
- Along an axis of downsampled size `size`:
  ```text
  winSize = min(windowSize, size)
  limit   = size - winSize
  starts  = [0, stride, 2*stride, ...] while <= limit
  if (limit % stride != 0) append limit   // tail window covers right/bottom edge
  ```

---

### Stage 3: Symmetrical Active-Foreground Gating

For each `(wx, wy)` window, all 6 channels of the **unshifted (`s = 0`)** blocks
`(expR, expG, expB, act0R, act0G, act0B)` are scanned across the `N = effWinW * effWinH` blocks:

```text
pixelMax      = max(expR, expG, expB, act0R, act0G, act0B)
pixelMin      = min(expR, expG, expB, act0R, act0G, act0B)
maxVal        = max over window of pixelMax
minVal        = min over window of pixelMin
nonBlackCount = count of blocks with pixelMax > 0.01  (NEAR_BLACK_THRESHOLD)
nonWhiteCount = count of blocks with pixelMin < 0.99  (NEAR_WHITE_THRESHOLD)
minActive     = ceil(0.15 * N)                        (10 out of 64 for full 8x8 windows)
```

A window is **skipped** (excluded from both `mean` and `p2`) if ANY of these 4 guards hold:
1. `maxVal <= 0.01`: Both images are uniformly near-black (`<= ~2.5 / 255`).
2. `minVal >= 0.99`: Both images are uniformly near-white (`>= ~252.5 / 255`).
3. `nonBlackCount < minActive`: Fewer than 15% of blocks contain non-black content (e.g., an outer
   window grazing only a 1–9 block sliver of a panel on a black canvas).
4. `nonWhiteCount < minActive`: Fewer than 15% of blocks contain non-white content (the symmetric
   white-canvas sliver guard).

#### Window Gating Truth Table

| Window Contents (Expected vs. Given) | `maxVal` | `minVal` | `nonBlack` | `nonWhite` | Active? | Why |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Pure black Void canvas (`#000000`) | `0.00` | `0.00` | `0` | `64` | Skip | Empty space must not pad `sorted` or dilute `mean` |
| Pure white background (`#FFFFFF`) | `1.00` | `1.00` | `64` | `0` | Skip | Symmetric empty-canvas exclusion for white-bg tests |
| Border sliver (`<= 9` lit blocks on black) | `0.80` | `0.00` | `6` (<10) | `64` | Skip | Prevents sparse outer AA slivers from distorting `p2` |
| Border sliver (`<= 9` dark blocks on white) | `1.00` | `0.20` | `64` | `6` (<10) | Skip | Symmetric sliver guard on white backgrounds |
| High-contrast black vs. white panel edge (`32` + `32` blocks) | `1.00` | `0.00` | `32` | `32` | **Active** | Counting `nonBlack` & `nonWhite` separately keeps crisp contrast edges active! |
| Missing object (`expected` lit, `given` black or white) | `0.80`/`1.0` | `0.00` | `40` | `64`/`24` | **Active** | Inspecting `max`/`min` over BOTH images catches disappearances |
| Spurious render (`expected` black, `given` lit) | `0.80` | `0.00` | `40` | `64` | **Active** | Extra geometry activates the window and fails cleanly |

*Note on shift invariance*: Gating always uses unshifted `act0` (`s = 0`) so all 9 shifts score the
exact same `activeCount` windows. If `activeCount == 0` after the primary pass and at least one
window is non-uniform (`max > 0.02` and `min < 0.98`, such as a tiny `< 10`-block foreground object
or reticle on a blank canvas), `compareBitmaps` re-scores those non-uniform windows with
`minActive = 1` instead of treating the pair as blank. Only when no non-uniform windows exist
(two truly uniform black or white frames) does `compareBitmaps` return `mean = 1.0, p2 = 1.0`
(`matches = true`).

---

### Stage 4: Per-Channel RGB SSIM (`populateChannelStats` & `computeChannelSsim`)

Inside each active window (`N = effWinW * effWinH` samples in `[0.0, 1.0]`), `XrSsimMatcher`
evaluates SSIM (Wang et al., 2004) over dynamic range `L = 1.0` independently on Red, Green, and
Blue:

#### Step 4a: Near-black single-channel short-circuit
If `chMax <= 0.01` across both `expected` and `given` for that specific channel, `populateChannelStats`
marks `ChannelWindowStats.isInactive = true` (zeroing its mean, variance, and covariance) and
`computeChannelSsim` returns `1.0` for that channel.
- *Why*: On a pure-red panel (`RGB = (220, 0, 0)`) over black, Blue is `~0.000` in both images while
  the window itself is active (`R > 0.01`). Without this check, 1-bit `1/255` P3->sRGB quantization
  noise in Blue (`var ~ 1e-6`, `cov ~ 0`) would yield spurious low Blue SSIM even though the Blue
  channel has no visual signal. If a blue tint regression occurs, `chMax > 0.01` and full SSIM runs.

#### Step 4b: Mean, Unbiased Sample Variance, and Covariance
For channel samples `A[i]` (`expected`) and `B[i]` (`given`) over `i in 0 until N`:

```text
meanA = (1 / N) * sum(A[i])
meanB = (1 / N) * sum(B[i])

invDoF = 1.0 / (N - 1)   // Bessel's correction for N > 1
varA   = invDoF * sum((A[i] - meanA)^2)
varB   = invDoF * sum((B[i] - meanB)^2)
cov    = invDoF * sum((A[i] - meanA) * (B[i] - meanB))
```

#### Step 4c: Stability Constants & Cross-Channel Co-Aligned Contrast Stabilization
With normalized range `L = 1.0`, dimensionless constants `K1 = 0.01` and `K2 = 0.06` give:

```text
C1 = (K1 * L)^2 = 0.01^2 = 0.0001   (1e-4, stabilizes near-zero means)
C2 = (K2 * L)^2 = 0.06^2 = 0.0036   (3.6e-3, stabilizes near-zero variances)
C3 = C2 / 2.0   = 0.0018            (1.8e-3, stabilizes structure correlation)
```

Why `K2 = 0.06` instead of Wang et al.'s canonical grayscale `K2 = 0.03` (`C2 = 9e-4`)? Because
Stage 4d takes `min(R, G, B)` per window and Stage 5 checks the `p2` worst-tail percentile across
solid-filled XR UI surfaces (such as `#444444` dark-grey panels or letterboxed video crop margins).
In those nearly flat windows (`sigma ~ 0.01–0.03`), benign 1–2 px edge fade tapers or minor GPU
shading jitter can make one channel's standard deviation slightly different (e.g., `0.008` vs `0.023`
even at `corr = 1.0`). When `C2` is smaller than that jitter (`9e-4`), the contrast factor
`c(A, B) = (2 * sA * sB + C2) / (sA^2 + sB^2 + C2)` plunges below `0.90` on pure `1–2%` edge noise;
setting `K2 = 0.06` (`C2 = 3.6e-3`) anchors the variance floor above benign sub-4% noise while
still dropping steeply on real `>= 16/255` contrast defects, missing thin geometry, or `2 px` shifts.

After computing `(meanA, meanB, varA, varB, cov)` across all three RGB channels for candidate shift
`s`, `XrSsimMatcher` stabilizes only the contrast variance floor on low-baseline-variance spectator
channels that exhibit genuine positive structural alignment (`cov > 1e-8` and Pearson correlation
`cov / (sigmaA * sigmaB) >= 0.25`, rejecting IEEE-754 rounding noise on flat surfaces and
uncorrelated noise), bounding the cross-channel covariance at `0.04` (`maxBoost = 0.03`) and
attenuating by `(C2 / (C2 + varA))^2` so channels with strong baseline contrast keep tight
sensitivity to single-channel contrast washout:

```text
maxPosCov  = min(0.04, max(0.0, covR, covG, covB))
coAligned  = (cov > 1e-8) && (cov >= 0.25 * sigmaA * sigmaB)
specWeight = C2 / (C2 + varA)
channelC2  = coAligned ? (C2 + 0.75 * maxPosCov * specWeight^2) : C2

Luminance:  l(A, B) = (2 * meanA * meanB + C1) / (meanA^2 + meanB^2 + C1)
Contrast:   c(A, B) = (2 * sigmaA * sigmaB + channelC2) / (varA + varB + channelC2)
Structure:  s(A, B) = (cov + C3) / (sigmaA * sigmaB + C3)      // C3 = C2 / 2.0 untouched!

SSIM(A, B) = l(A, B) * c(A, B) * s(A, B)
```

Why decouple `channelC2` from `C3`, bound `maxPosCov`, and attenuate by `(C2 / (C2 + varA))^2`?
When a colored 3D object sits over a non-black UI panel (for example, a diffuse green cylinder with
`Blue ≈ 75/255` over a `#444444` = `68/255` panel), its spectator channel has near-zero baseline
variance (`sigmaA_B ≈ 0.007`, `varA_B ≈ 5e-5 << C2`, `specWeight^2 ≈ 0.97`) without hitting the
`chMax <= 0.01` black-canvas fast path. A 1–2 px white IBL specular glint caused by per-device
optical FOV calibration (`~0.39°` between physical glasses units) adds a correlated highlight across
R, G, and B, inflating `sigmaB_B` (`~0.034`) and collapsing isolated single-channel contrast
`c_B(A, B)` even though the geometry is structurally aligned (`cov_G` positive and strong,
`cov_B > 0`). Applying bounded, variance-attenuated `channelC2` only when `coAligned` holds while
keeping `C3 = C2 / 2.0` untouched prevents false `p2` failures on co-aligned specular glints
without relaxing:
- **Single-channel contrast washout alongside a strong colored edge** (`varA >> C2` drives
  `specWeight^2 -> 0`, preserving sensitivity even when another channel has `cov = 0.25`),
- **Missing or spurious monochrome / low-contrast features** (including inside the same `32x32`
  window as a high-contrast colored edge: `varB == 0` or `varA == 0` gives `cov == 0`, so
  `channelC2 == C2`, preserving exact single-channel Wang et al. SSIM),
- **Spatial layout shifts (`>= 2 px`)** (penalized directly by unrelaxed `s(A, B)`), or
- **Color swaps and material desaturation** (penalized by negative/zero `cov` in `s(A, B)` and
  mean shifts in `l(A, B)`).

#### Physical Intuition Across Failure Modes
- **Identical window** (`meanA == meanB`, `varA == varB == cov`): numerator equals denominator ->
  `SSIM = 1.0`.
- **Flat solid regions** (`varA = varB = cov = 0`): `(2 * 0 + C2) / (0 + 0 + C2) = 1.0`, so SSIM
  reduces cleanly to Luminance `l(A, B)`.
- **Uniform lighting attenuation (`0.72x` brightness)**: Even on a flat panel (`var = 0`),
  `meanB = 0.72 * meanA` yields `l(A, B) ≈ (2 * 0.72) / (1 + 0.72^2) = 0.948 < 0.95` (and lower in
  darker areas due to `C1`), reliably catching IBL or shader exposure drops.
- **Corrupted texture / inverted contrast** (`cov <= 0`): `s(A, B) = (cov + C3) / (sigmaA * sigmaB + C3)`
  drops sharply toward `0` (or negative when anti-correlated) even if average brightness
  `meanA == meanB` is unchanged.
- **Subpixel AA fringe** (`L = 1.0` with `C2 = 3.6e-3` instead of `MSSIMMatcher`'s `5.8e-5`): small
  `~1e-3` quantization variance differences along curves do not blow up the denominator.

#### Step 4d: Worst-Channel Floor (`min(R, G, B)`)
For each candidate shift `s in 0..8`:

```text
score(s) = min(SSIM_R(s), SSIM_G(s), SSIM_B(s))
```

Using `min(R, G, B)` instead of luminance (`0.299R + 0.587G + 0.114B`) ensures a color defect (such
as a Red <-> Blue swap or green material tint error) cannot mask itself by matching luminance. Each
window records `bestWindowScore = max_{s=0..8} score(s)`, breaking early if `bestWindowScore >= 1.0 - 1e-9`.

---

### Stage 5: Dual Mean & `p2` Tail Acceptance Gate

Over `M = activeCount` window scores sorted ascending in `sorted[0 until M]`:

```text
meanScore       = sum(scores[0 until M]) / M                // Required >= 0.95 (threshold)
pIdx            = floor(0.02 * M).coerceIn(0, M - 1)        // 2nd percentile index (p2)
percentileScore = sorted[pIdx]                              // Required >= 0.90 (minPercentileThreshold)
matches         = (meanScore >= threshold) && (percentileScore >= minPercentileThreshold)
```

#### Why `p2` instead of `min` (`p0`) or `p5`?
- Filtering out sparse background border slivers reduces active window counts in panel scenes from
  `~1800` down to `~350–600`, concentrating genuine small UI defects (like a missing panel label
  touching `~15–20` windows) into `~2.5%–5%` of active windows.
- `p5` can sit just above a `~3%`-area localized defect (`sorted[floor(0.05 * M)] == 1.000`),
  letting missing text slip through.
- `p2` catches localized regressions covering `>= 2%` of active foreground windows while remaining
  tolerant of up to `1–2%` extreme single-window rasterization outliers that would make strict `min`
  (`p0`) flaky.

---

### Stage 6: Diagnostic Diff Bitmap (`buildDiffBitmap`)

When either `meanScore` or `percentileScore` fails:
1. Choose cutoff `failWindowFloor = minPercentileThreshold` if `percentileScore < minPercentileThreshold`,
   else `threshold`.
2. Mark every downsampled cell inside any window with `bestWindowScore < failWindowFloor` in
   `failedDownsampled` (falling back to marking the entire image if `mean` failed globally without any
   single window dipping below `failWindowFloor`).
3. Render a `W x H` `ARGB_8888` diff bitmap:
   - **Pass 1**: Highlight pixels `Color.MAGENTA` inside failed blocks where any RGB channel differs
     by `> 4` (`DIFF_CHANNEL_DELTA`), keeping identical background pixels dark/transparent for readability.
   - **Pass 2 (fallback)**: If subtle `< 4/255` wide-area brightness drift failed SSIM with zero
     pixels exceeding delta `4`, highlight any pixel with a non-zero RGB difference
     (`((expected xor given) and 0x00FFFFFF) != 0`) inside failed blocks.

---

## 4. Constructor Parameter Guide

| Parameter | Default | Role & Effect of Tuning |
| :--- | :--- | :--- |
| `threshold` | `0.95` | Minimum global `meanScore` across active windows. |
| `minPercentileThreshold` | `0.90` | Minimum `p2` worst-tail window SSIM. Catches small localized defects. |
| `percentile` | `0.02` | Quantile index (`floor(percentile * activeCount)`). Smaller values inspect narrower tails. |
| `downsampleFactor` | `4` | Box-averaging block size in source px (`4` absorbs `<= 0.75 px` jitter, catches `>= 2 px` shifts). |
| `windowSize` | `8` | Window dimension in downsampled blocks (`8 * 4 = 32 px`). |
| `stride` | `2` | Window step in downsampled blocks (`2 * 4 = 8 px`). |

*Tests*: See `src/test/kotlin/androidx/xr/testutils/XrSsimMatcherTest.kt` for unit tests verifying
`±0.5 px` subpixel borders, `2 px` shift detection, `p2` localized regressions, black/white canvas
sliver exclusion, dimmed lighting detection, and Red <-> Blue channel swaps.
