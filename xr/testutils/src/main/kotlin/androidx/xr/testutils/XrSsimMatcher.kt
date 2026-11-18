/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.testutils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.MatchResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Default minimum required global mean SSIM across active foreground windows. */
public const val DEFAULT_XR_SSIM_THRESHOLD: Double = 0.95

/** Default minimum required tail-percentile SSIM across active foreground windows. */
public const val DEFAULT_XR_MIN_PERCENTILE_SSIM_THRESHOLD: Double = 0.90

/**
 * Compares bitmaps using downsampled per-channel RGB Structural Similarity Index (SSIM).
 *
 * Evaluates local structural similarity rather than exact pixel equality so benign 3D compositor
 * subpixel antialiasing and scaling noise pass while visual regressions fail. See
 * `xr/testutils/XR_SSIM_MATCHER.md` for the full mathematical specification and worked examples.
 *
 * * **Downsampled box prefiltering with ±0.5 px shifts**: Averages `[downsampleFactor] x
 *   [downsampleFactor]` blocks and scores 9 fractional `±0.5 px` bilinear alignments per window to
 *   absorb subpixel rasterization, crop-scaling, and P3-to-sRGB phase jitter without masking `>= 2
 *   px` layout shifts.
 * * **Normalized SSIM with worst-channel gating**: Evaluates Wang et al. (2004) SSIM in `[0.0,
 *   1.0]` (`K1 = 0.01`, `K2 = 0.06`) over sliding `[windowSize] x [windowSize]` windows and takes
 *   `min(R, G, B)` per window so color swaps and tints cannot hide in luminance.
 * * **Symmetrical foreground isolation**: Skips uniform near-black or near-white canvas windows and
 *   sparse outer edge slivers (`< 15%` non-black or non-white blocks) across both bitmaps so empty
 *   space neither dilutes global scores nor inflates tail noise.
 * * **Dual mean and percentile gate**: Requires the mean score to meet [threshold] and the
 *   worst-region [percentile] score (`p2` by default) to meet [minPercentileThreshold].
 *
 * @param threshold minimum required global mean SSIM across active windows
 * @param minPercentileThreshold minimum required SSIM floor at [percentile] across active windows
 * @param percentile fraction in `[0.0, 1.0]` selecting the tail-percentile floor index
 * @param downsampleFactor spatial box-averaging block size in source pixels per axis
 * @param windowSize sliding SSIM window width and height in downsampled blocks
 * @param stride sliding window step size in downsampled blocks
 */
public class XrSsimMatcher
@JvmOverloads
constructor(
    @FloatRange(from = 0.0, to = 1.0) private val threshold: Double = DEFAULT_XR_SSIM_THRESHOLD,
    @FloatRange(from = 0.0, to = 1.0)
    private val minPercentileThreshold: Double = DEFAULT_XR_MIN_PERCENTILE_SSIM_THRESHOLD,
    @FloatRange(from = 0.0, to = 1.0) private val percentile: Double = DEFAULT_PERCENTILE,
    @IntRange(from = 1) private val downsampleFactor: Int = DEFAULT_DOWNSAMPLE_FACTOR,
    @IntRange(from = 1) private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    @IntRange(from = 1) private val stride: Int = DEFAULT_STRIDE,
) : BitmapMatcher {

    init {
        require(threshold in 0.0..1.0) { "threshold must be in [0.0, 1.0]: $threshold" }
        require(minPercentileThreshold in 0.0..1.0) {
            "minPercentileThreshold must be in [0.0, 1.0]: $minPercentileThreshold"
        }
        require(percentile in 0.0..1.0) { "percentile must be in [0.0, 1.0]: $percentile" }
        require(downsampleFactor >= 1) {
            "downsampleFactor must be >= 1: $downsampleFactor"
        }
        require(windowSize >= 1) { "windowSize must be >= 1: $windowSize" }
        require(stride >= 1) { "stride must be >= 1: $stride" }
    }

    override fun compareBitmaps(
        expected: IntArray,
        given: IntArray,
        width: Int,
        height: Int,
    ): MatchResult {
        require(width > 0 && height > 0) { "Dimensions must be positive: ${width}x$height" }
        val expectedCount = width * height
        require(expected.size == expectedCount) {
            "expected length ${expected.size} does not match ${width}x$height ($expectedCount)"
        }
        require(given.size == expectedCount) {
            "given length ${given.size} does not match ${width}x$height ($expectedCount)"
        }

        // Stage 1: Spatial box downsampling (low-pass prefilter) + 9 half-pixel bilinear
        // alignments.
        // Clamping effectiveFactor lets tiny unit-test bitmaps (< downsampleFactor px) still run.
        val effectiveFactor = min(downsampleFactor, min(width, height)).coerceAtLeast(1)
        val downWidth = (width + effectiveFactor - 1) / effectiveFactor
        val downHeight = (height + effectiveFactor - 1) / effectiveFactor
        val downSize = downWidth * downHeight

        // Precompute the unshifted reference planes once, plus all 9 fractional {0, ±0.5 px}
        // bilinear-shifted planes of `given`. Evaluating alignment per sliding window (instead of
        // globally) lets depth-staggered 3D panels absorb independent subpixel raster jitter.
        val expR = DoubleArray(downSize)
        val expG = DoubleArray(downSize)
        val expB = DoubleArray(downSize)
        val shiftCount = HALF_SHIFT_X.size
        val actR = Array(shiftCount) { DoubleArray(downSize) }
        val actG = Array(shiftCount) { DoubleArray(downSize) }
        val actB = Array(shiftCount) { DoubleArray(downSize) }

        downsampleRgb(expected, width, height, effectiveFactor, downWidth, 0, 0, expR, expG, expB)
        for (s in 0 until shiftCount) {
            downsampleRgb(
                given,
                width,
                height,
                effectiveFactor,
                downWidth,
                HALF_SHIFT_X[s],
                HALF_SHIFT_Y[s],
                actR[s],
                actG[s],
                actB[s],
            )
        }
        val act0R = actR[0]
        val act0G = actG[0]
        val act0B = actB[0]

        // Stage 2: Build overlapping sliding windows across the downsampled grid.
        val effWinW = min(windowSize, downWidth)
        val effWinH = min(windowSize, downHeight)
        val yStarts = computeWindowStarts(downHeight, effWinH, stride)
        val xStarts = computeWindowStarts(downWidth, effWinW, stride)

        val scores = DoubleArray(yStarts.size * xStarts.size)
        val winYs = IntArray(scores.size)
        val winXs = IntArray(scores.size)
        val winSampleCount = effWinW * effWinH
        // Require >= 15% foreground blocks (e.g., 10 / 64 blocks) so outer border windows that
        // graze only a thin antialiased sliver against empty black or white canvas do not create
        // misleading worst-tail SSIM outliers.
        val minActiveCells = ceil(MIN_ACTIVE_WINDOW_FRACTION * winSampleCount).toInt()
        var hasNonUniformWindow = false

        fun scoreWindows(minRequiredCells: Int): Int {
            var count = 0
            for (wy in yStarts) {
                for (wx in xStarts) {
                    // Stage 3: Symmetrical active-foreground gating over the unshifted (s = 0)
                    // pair.
                    // Inspect BOTH `expected` and `given` across all 3 RGB channels so missing
                    // objects (expected foreground -> given canvas) or spurious renders keep the
                    // window active. Counting non-black and non-white cells independently preserves
                    // sharp black-vs-white panel edges (where half the blocks are near 0 and half
                    // are near 1).
                    var maxVal = 0.0
                    var minVal = 1.0
                    var nonBlackCount = 0
                    var nonWhiteCount = 0
                    for (dy in 0 until effWinH) {
                        val row = (wy + dy) * downWidth + wx
                        for (dx in 0 until effWinW) {
                            val idx = row + dx
                            val er = expR[idx]
                            val eg = expG[idx]
                            val eb = expB[idx]
                            val ar = act0R[idx]
                            val ag = act0G[idx]
                            val ab = act0B[idx]
                            val pixelMax = max(max(er, max(eg, eb)), max(ar, max(ag, ab)))
                            val pixelMin = min(min(er, min(eg, eb)), min(ar, min(ag, ab)))
                            maxVal = max(maxVal, pixelMax)
                            minVal = min(minVal, pixelMin)
                            if (pixelMax > NEAR_BLACK_THRESHOLD) {
                                nonBlackCount++
                            }
                            if (pixelMin < NEAR_WHITE_THRESHOLD) {
                                nonWhiteCount++
                            }
                        }
                    }
                    if (maxVal <= NEAR_BLACK_THRESHOLD || minVal >= NEAR_WHITE_THRESHOLD) {
                        continue
                    }
                    hasNonUniformWindow = true
                    if (nonBlackCount < minRequiredCells || nonWhiteCount < minRequiredCells) {
                        continue
                    }

                    // Stage 4: Score all 9 subpixel alignments independently per channel and take
                    // min(R, G, B) so hue/channel swaps cannot cancel out in weighted luma,
                    // keeping the best local subpixel alignment (stopping early on an exact 1.0
                    // match).
                    var bestWindowScore = Double.NEGATIVE_INFINITY
                    for (s in 0 until shiftCount) {
                        val sR =
                            computeChannelSsim(
                                expR,
                                actR[s],
                                downWidth,
                                wx,
                                wy,
                                effWinW,
                                effWinH,
                                winSampleCount,
                            )
                        val sG =
                            computeChannelSsim(
                                expG,
                                actG[s],
                                downWidth,
                                wx,
                                wy,
                                effWinW,
                                effWinH,
                                winSampleCount,
                            )
                        val sB =
                            computeChannelSsim(
                                expB,
                                actB[s],
                                downWidth,
                                wx,
                                wy,
                                effWinW,
                                effWinH,
                                winSampleCount,
                            )
                        val score = min(sR, min(sG, sB))
                        if (score > bestWindowScore) {
                            bestWindowScore = score
                            if (bestWindowScore >= 1.0 - 1e-9) {
                                break
                            }
                        }
                    }
                    scores[count] = bestWindowScore
                    winYs[count] = wy
                    winXs[count] = wx
                    count++
                }
            }
            return count
        }

        var activeCount = scoreWindows(minActiveCells)
        if (activeCount == 0 && hasNonUniformWindow) {
            // If every overlapping window was sparser than minActiveCells (e.g., a tiny object or
            // reticle on an otherwise uniform black/white canvas), re-score non-uniform windows
            // rather than falling into the blank-image 1.0 branch.
            activeCount = scoreWindows(minRequiredCells = 1)
        }

        // Stage 5: Dual mean + low-percentile tail gate.
        // If no foreground windows exist (e.g., two uniform black or white images), report 1.0.
        val meanScore: Double
        val percentileScore: Double
        if (activeCount == 0) {
            meanScore = 1.0
            percentileScore = 1.0
        } else {
            var sum = 0.0
            for (i in 0 until activeCount) {
                sum += scores[i]
            }
            meanScore = sum / activeCount
            val sorted = scores.copyOf(activeCount).also { it.sort() }
            val pIdx = floor(percentile * activeCount).toInt().coerceIn(0, activeCount - 1)
            percentileScore = sorted[pIdx]
        }

        val matches = meanScore >= threshold && percentileScore >= minPercentileThreshold
        val pctLabel = (percentile * 100.0).roundToInt()
        val stats =
            String.format(
                Locale.US,
                "[XrSsim] Required mean >= %.3f & p%d >= %.3f; Actual mean: %.4f, p%d: %.4f (windows=%d)",
                threshold,
                pctLabel,
                minPercentileThreshold,
                meanScore,
                pctLabel,
                percentileScore,
                activeCount,
            )

        if (matches) {
            return MatchResult(matches = true, diff = null, comparisonStatistics = stats)
        }

        // Stage 6: Mark downsampled cells touched by failing windows to localize diff output.
        val failedDownsampled = BooleanArray(downSize)
        val failWindowFloor =
            if (percentileScore < minPercentileThreshold) minPercentileThreshold else threshold
        var markedAnyWindow = false
        for (i in 0 until activeCount) {
            if (scores[i] < failWindowFloor) {
                markedAnyWindow = true
                val wy = winYs[i]
                val wx = winXs[i]
                for (dy in 0 until effWinH) {
                    val row = (wy + dy) * downWidth + wx
                    for (dx in 0 until effWinW) {
                        failedDownsampled[row + dx] = true
                    }
                }
            }
        }
        // Fallback when global mean failed without any individual window falling below floor.
        if (!markedAnyWindow) {
            failedDownsampled.fill(true)
        }

        val diffBitmap =
            buildDiffBitmap(
                expected = expected,
                given = given,
                width = width,
                height = height,
                factor = effectiveFactor,
                downWidth = downWidth,
                failedDownsampled = failedDownsampled,
            )
        return MatchResult(matches = false, diff = diffBitmap, comparisonStatistics = stats)
    }

    private companion object {
        // 4x4 box averaging acts as a spatial low-pass filter on 0.5-0.75px subpixel edge phase
        // while keeping >= 2px layout shifts at >= 50% block-boundary difference.
        private const val DEFAULT_DOWNSAMPLE_FACTOR = 4

        // 8x8 downsampled blocks at stride 2 = 32x32 source-px windows moving in 8x8 source-px
        // steps (75% overlap along each axis).
        private const val DEFAULT_WINDOW_SIZE = 8
        private const val DEFAULT_STRIDE = 2

        // 2nd percentile floor (p2): catches small localized regressions (>= 2% of active windows)
        // that edge-sliver filtering concentrates in the tail without flaking on single-window
        // rasterization outliers.
        private const val DEFAULT_PERCENTILE = 0.02

        // Minimum fraction of non-background downsampled blocks in an active window (ceil(0.15 *
        // 64) = 10 blocks), excluding outer border slivers along black or white margins.
        private const val MIN_ACTIVE_WINDOW_FRACTION = 0.15

        // Wang et al. (2004) SSIM dimensionless stability constants normalized for dynamic range
        // L = 1.0: C1 stabilizes dark regions (near-zero mean), C2 stabilizes flat regions
        // (near-zero variance). Contrast and structure share C3 = C2 / 2 so (c * s) simplifies
        // without square roots. K2 is set to 0.06 (C2 = 3.6e-3, vs. grayscale 0.03 / 9e-4) because
        // evaluating min(R, G, B) on the 2nd percentile across flat mid/dark-grey XR surfaces and
        // minor 1px crop-edge fade noise (std ~ 0.01–0.03) otherwise amplifies benign low-variance
        // channel jitter, while real edge shifts, color swaps, and medium contrast drops still
        // fail.
        private const val K1 = 0.01
        private const val K2 = 0.06
        private const val C1 = K1 * K1
        private const val C2 = K2 * K2

        private const val NEAR_BLACK_THRESHOLD = 0.01
        private const val NEAR_WHITE_THRESHOLD = 0.99
        private const val DIFF_CHANNEL_DELTA = 4

        // 9 bilinear shift modes covering (0, 0) plus all 8 cardinal/diagonal ±0.5 px offsets.
        private val HALF_SHIFT_X = intArrayOf(0, -1, 1, 0, 0, -1, 1, -1, 1)
        private val HALF_SHIFT_Y = intArrayOf(0, 0, 0, -1, 1, -1, -1, 1, 1)

        /**
         * Box-averages [argb] into normalized `[0.0, 1.0]` R, G, and B planes of size `ceil(width /
         * factor) x ceil(height / factor)`, optionally evaluated at a fractional `0.5 px` bilinear
         * shift ([halfShiftX], [halfShiftY]).
         *
         * When `(halfShiftX, halfShiftY) != (0, 0)`, each source pixel `(x, y)` averages four
         * edge-clamped integer samples:
         * - `halfShiftX = +1`: averages `x - 1` and `x`, shifting content `+0.5 px` right.
         * - `halfShiftX = -1`: averages `x` and `x + 1`, shifting content `-0.5 px` left.
         * - Same convention vertically for `halfShiftY` (`+1` shifts down, `-1` shifts up).
         */
        private fun downsampleRgb(
            argb: IntArray,
            width: Int,
            height: Int,
            factor: Int,
            downWidth: Int,
            halfShiftX: Int,
            halfShiftY: Int,
            outR: DoubleArray,
            outG: DoubleArray,
            outB: DoubleArray,
        ) {
            val downHeight = (height + factor - 1) / factor
            val hasShift = halfShiftX != 0 || halfShiftY != 0
            for (by in 0 until downHeight) {
                val y0 = by * factor
                val y1 = min(height, y0 + factor)
                val outRow = by * downWidth
                for (bx in 0 until downWidth) {
                    val x0 = bx * factor
                    val x1 = min(width, x0 + factor)
                    var sumR = 0L
                    var sumG = 0L
                    var sumB = 0L
                    if (!hasShift) {
                        for (y in y0 until y1) {
                            val rowOffset = y * width
                            for (x in x0 until x1) {
                                val pixel = argb[rowOffset + x]
                                sumR += (pixel shr 16) and 0xFF
                                sumG += (pixel shr 8) and 0xFF
                                sumB += pixel and 0xFF
                            }
                        }
                    } else {
                        for (y in y0 until y1) {
                            val yA = if (halfShiftY > 0) (y - 1).coerceAtLeast(0) else y
                            val yB = if (halfShiftY < 0) (y + 1).coerceAtMost(height - 1) else y
                            val rowA = yA * width
                            val rowB = yB * width
                            for (x in x0 until x1) {
                                val xA = if (halfShiftX > 0) (x - 1).coerceAtLeast(0) else x
                                val xB = if (halfShiftX < 0) (x + 1).coerceAtMost(width - 1) else x
                                val p00 = argb[rowA + xA]
                                val p01 = argb[rowA + xB]
                                val p10 = argb[rowB + xA]
                                val p11 = argb[rowB + xB]
                                sumR +=
                                    ((p00 shr 16) and 0xFF) +
                                        ((p01 shr 16) and 0xFF) +
                                        ((p10 shr 16) and 0xFF) +
                                        ((p11 shr 16) and 0xFF)
                                sumG +=
                                    ((p00 shr 8) and 0xFF) +
                                        ((p01 shr 8) and 0xFF) +
                                        ((p10 shr 8) and 0xFF) +
                                        ((p11 shr 8) and 0xFF)
                                sumB +=
                                    (p00 and 0xFF) +
                                        (p01 and 0xFF) +
                                        (p10 and 0xFF) +
                                        (p11 and 0xFF)
                            }
                        }
                    }
                    // Each shifted pixel sums 4 bilinear samples, so normalize by 4x block area *
                    // 255.
                    val samplesPerPixel = if (hasShift) 4.0 else 1.0
                    val invDenom = 1.0 / (samplesPerPixel * (x1 - x0) * (y1 - y0) * 255.0)
                    val outIdx = outRow + bx
                    outR[outIdx] = sumR * invDenom
                    outG[outIdx] = sumG * invDenom
                    outB[outIdx] = sumB * invDenom
                }
            }
        }

        /**
         * Returns sliding-window start offsets along one axis, appending `size - winSize` as a tail
         * window when `(size - winSize)` is not evenly divisible by [stride] so right and bottom
         * border pixels are never dropped.
         */
        private fun computeWindowStarts(size: Int, winSize: Int, stride: Int): IntArray {
            val limit = size - winSize
            if (limit <= 0) return intArrayOf(0)
            val count = limit / stride + 1
            val needsTail = limit % stride != 0
            val result = IntArray(if (needsTail) count + 1 else count)
            for (i in 0 until count) {
                result[i] = i * stride
            }
            if (needsTail) {
                result[count] = limit
            }
            return result
        }

        /**
         * Computes single-channel Wang et al. (2004) SSIM inside a `[winW x winH]` window: `((2 *
         * meanA * meanB + C1) * (2 * cov + C2)) / ((meanA^2 + meanB^2 + C1) * (varA + varB + C2))`
         * using Bessel's correction (`1 / (N - 1)`) for unbiased variance and covariance.
         *
         * Returns `1.0` early if this channel is uniformly near-black (`<= 0.01`) across both
         * images so inactive channels on saturated primary colors (e.g., Blue inside a pure-Red
         * panel on black) do not amplify 1-bit quantization noise.
         */
        private fun computeChannelSsim(
            expChannel: DoubleArray,
            actChannel: DoubleArray,
            downWidth: Int,
            wx: Int,
            wy: Int,
            winW: Int,
            winH: Int,
            sampleCount: Int,
        ): Double {
            var chMax = 0.0
            var sumA = 0.0
            var sumB = 0.0
            for (dy in 0 until winH) {
                val row = (wy + dy) * downWidth + wx
                for (dx in 0 until winW) {
                    val idx = row + dx
                    val va = expChannel[idx]
                    val vb = actChannel[idx]
                    if (va > chMax) chMax = va
                    if (vb > chMax) chMax = vb
                    sumA += va
                    sumB += vb
                }
            }
            if (chMax <= NEAR_BLACK_THRESHOLD) return 1.0

            val meanA = sumA / sampleCount
            val meanB = sumB / sampleCount
            var varA = 0.0
            var varB = 0.0
            var cov = 0.0
            for (dy in 0 until winH) {
                val row = (wy + dy) * downWidth + wx
                for (dx in 0 until winW) {
                    val idx = row + dx
                    val da = expChannel[idx] - meanA
                    val db = actChannel[idx] - meanB
                    varA += da * da
                    varB += db * db
                    cov += da * db
                }
            }
            // Bessel's correction yields unbiased sample variance and covariance estimators.
            if (sampleCount > 1) {
                val invDoF = 1.0 / (sampleCount - 1)
                varA *= invDoF
                varB *= invDoF
                cov *= invDoF
            }
            // Algebraic collapse of luminance * contrast * structure (with C3 = C2 / 2).
            val numerator = (2.0 * meanA * meanB + C1) * (2.0 * cov + C2)
            val denominator = (meanA * meanA + meanB * meanB + C1) * (varA + varB + C2)
            return if (denominator > 0.0) numerator / denominator else 1.0
        }

        /**
         * Builds an `ARGB_8888` diagnostic diff bitmap highlighting pixels in magenta within failed
         * downsampled blocks where any RGB channel differs by more than [DIFF_CHANNEL_DELTA]
         * (falling back to any pixel with an RGB difference if a low-magnitude global drift
         * produces zero pixels above delta).
         */
        private fun buildDiffBitmap(
            expected: IntArray,
            given: IntArray,
            width: Int,
            height: Int,
            factor: Int,
            downWidth: Int,
            failedDownsampled: BooleanArray,
        ): Bitmap {
            val diff = IntArray(width * height)
            var markedPixels = 0
            for (y in 0 until height) {
                val dy = y / factor
                val downRow = dy * downWidth
                val row = y * width
                for (x in 0 until width) {
                    val i = row + x
                    if (failedDownsampled[downRow + x / factor]) {
                        val pA = expected[i]
                        val pB = given[i]
                        val dr = abs(((pA shr 16) and 0xFF) - ((pB shr 16) and 0xFF))
                        val dg = abs(((pA shr 8) and 0xFF) - ((pB shr 8) and 0xFF))
                        val db = abs((pA and 0xFF) - (pB and 0xFF))
                        if (
                            dr > DIFF_CHANNEL_DELTA ||
                                dg > DIFF_CHANNEL_DELTA ||
                                db > DIFF_CHANNEL_DELTA
                        ) {
                            diff[i] = Color.MAGENTA
                            markedPixels++
                        }
                    }
                }
            }
            if (markedPixels == 0) {
                for (y in 0 until height) {
                    val dy = y / factor
                    val downRow = dy * downWidth
                    val row = y * width
                    for (x in 0 until width) {
                        val i = row + x
                        if (
                            failedDownsampled[downRow + x / factor] &&
                                ((expected[i] xor given[i]) and 0x00FFFFFF) != 0
                        ) {
                            diff[i] = Color.MAGENTA
                        }
                    }
                }
            }
            return Bitmap.createBitmap(diff, width, height, Bitmap.Config.ARGB_8888)
        }
    }
}
