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

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class XrSsimMatcherTest {

    private val matcher = XrSsimMatcher()

    @Test
    fun constructor_invalidArguments_throws() {
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(threshold = -0.1) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(threshold = 1.1) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(minPercentileThreshold = -0.1) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(minPercentileThreshold = 1.1) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(percentile = -0.01) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(percentile = 1.01) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(downsampleFactor = 0) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(windowSize = 0) }
        assertFailsWith<IllegalArgumentException> { XrSsimMatcher(stride = 0) }
    }

    @Test
    fun compareBitmaps_invalidArrayOrDimensions_throws() {
        assertFailsWith<IllegalArgumentException> {
            matcher.compareBitmaps(IntArray(4), IntArray(4), width = 0, height = 2)
        }
        assertFailsWith<IllegalArgumentException> {
            matcher.compareBitmaps(IntArray(3), IntArray(4), width = 2, height = 2)
        }
        assertFailsWith<IllegalArgumentException> {
            matcher.compareBitmaps(IntArray(4), IntArray(3), width = 2, height = 2)
        }
    }

    @Test
    fun compareBitmaps_identicalSolidAndOddSizedImages_matchWithNullDiff() {
        for (color in intArrayOf(Color.BLACK, Color.WHITE, Color.RED, Color.rgb(64, 140, 210))) {
            for ((w, h) in listOf(1 to 1, 4 to 4, 6 to 5, 53 to 41, 160 to 120)) {
                val pixels = IntArray(w * h) { color }
                val result = matcher.compareBitmaps(pixels, pixels.copyOf(), w, h)
                assertTrue(
                    result.matches,
                    "Expected identical ${w}x$h solid $color to match: ${result.comparisonStatistics}",
                )
                assertNull(result.diff)
            }
        }
    }

    @Test
    fun compareBitmaps_subpixelAntiAliasedCircleShiftBy1px_matches() {
        val w = 160
        val h = 160
        val expected = renderSoftCircleScene(w, h, shiftX = 0f, shiftY = 0f)
        val shifted = renderSoftCircleScene(w, h, shiftX = 0.75f, shiftY = 0.5f)

        val result = matcher.compareBitmaps(expected, shifted, w, h)
        assertTrue(
            result.matches,
            "Expected subpixel shifted 3D scene to match: ${result.comparisonStatistics}",
        )
        assertNull(result.diff)
    }

    @Test
    fun compareBitmaps_largeShiftOrMirror_fails() {
        val w = 160
        val h = 160
        val expected =
            renderMultiObjectScene(w, h, shiftX = 0, rColor = Color.RED, bColor = Color.BLUE)
        val shifted6px =
            renderMultiObjectScene(w, h, shiftX = 6, rColor = Color.RED, bColor = Color.BLUE)
        val mirrored = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                mirrored[y * w + x] = expected[y * w + (w - 1 - x)]
            }
        }

        val shiftResult = matcher.compareBitmaps(expected, shifted6px, w, h)
        assertFalse(shiftResult.matches)
        assertNotNull(shiftResult.diff)

        val mirrorResult = matcher.compareBitmaps(expected, mirrored, w, h)
        assertFalse(mirrorResult.matches)
        assertNotNull(mirrorResult.diff)
    }

    @Test
    fun compareBitmaps_redBlueChannelSwap_fails() {
        val w = 160
        val h = 160
        val expected =
            renderMultiObjectScene(
                w,
                h,
                shiftX = 0,
                rColor = Color.rgb(210, 80, 40),
                bColor = Color.rgb(40, 120, 220),
            )
        val swapped =
            IntArray(expected.size) { i ->
                val p = expected[i]
                (p and 0xFF00FF00.toInt()) or ((p and 0xFF) shl 16) or ((p shr 16) and 0xFF)
            }

        val result = matcher.compareBitmaps(expected, swapped, w, h)
        assertFalse(result.matches, "Expected R<->B swap to fail: ${result.comparisonStatistics}")
        assertNotNull(result.diff)
    }

    @Test
    fun compareBitmaps_blackBackgroundPaddingDoesNotMaskMissingObject() {
        val w = 256
        val h = 256
        val expected =
            renderMultiObjectScene(
                w,
                h,
                shiftX = 0,
                rColor = Color.RED,
                bColor = Color.BLUE,
                bgColor = Color.BLACK,
            )
        // Erase the smaller right-side object (occupies only ~3% of total pixels).
        val missingRight = expected.copyOf()
        for (y in 90 until 170) {
            for (x in 170 until 230) {
                missingRight[y * w + x] = Color.BLACK
            }
        }

        val result = matcher.compareBitmaps(expected, missingRight, w, h)
        assertFalse(
            result.matches,
            "Expected missing object on black background to fail: ${result.comparisonStatistics}",
        )
        val diff = assertNotNull(result.diff)
        try {
            // Missing region is highlighted; intact left object stays transparent.
            assertEquals(Color.MAGENTA, diff.getPixel(200, 130))
            assertEquals(Color.TRANSPARENT, diff.getPixel(60, 120))
        } finally {
            diff.recycle()
        }
    }

    @Test
    fun compareBitmaps_whiteBackgroundPaddingDoesNotMaskMissingObject() {
        val w = 256
        val h = 256
        val expected =
            renderMultiObjectScene(
                w,
                h,
                shiftX = 0,
                rColor = Color.RED,
                bColor = Color.BLUE,
                bgColor = Color.WHITE,
            )
        val missingRight = expected.copyOf()
        for (y in 90 until 170) {
            for (x in 170 until 230) {
                missingRight[y * w + x] = Color.WHITE
            }
        }

        val result = matcher.compareBitmaps(expected, missingRight, w, h)
        assertFalse(
            result.matches,
            "Expected missing object on white background to fail: ${result.comparisonStatistics}",
        )
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_dimmedLighting_fails() {
        val w = 160
        val h = 160
        val expected = renderSoftCircleScene(w, h, shiftX = 0f, shiftY = 0f)
        val dimmed =
            IntArray(expected.size) { i ->
                val p = expected[i]
                val r = (((p shr 16) and 0xFF) * 0.72).toInt()
                val g = (((p shr 8) and 0xFF) * 0.72).toInt()
                val b = ((p and 0xFF) * 0.72).toInt()
                Color.rgb(r, g, b)
            }

        val result = matcher.compareBitmaps(expected, dimmed, w, h)
        assertFalse(
            result.matches,
            "Expected dimmed lighting to fail: ${result.comparisonStatistics}",
        )
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_localizedCorruptionCaughtByPercentileFloor() {
        val w = 480
        val h = 480
        val expected = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                expected[y * w + x] = Color.rgb(80 + (x * 120) / w, 100 + (y * 100) / h, 160)
            }
        }
        // Corrupt a ~76x76 (~2.5% area, >2% of sliding windows) patch while leaving ~97.5% of
        // pixels identical so mean stays >= 0.95, yet p2 drops below 0.90.
        val actual = expected.copyOf()
        for (y in 32 until 108) {
            for (x in 32 until 108) {
                actual[y * w + x] = Color.rgb(40, 50, 80)
            }
        }

        val meanOnlyMatcher = XrSsimMatcher(minPercentileThreshold = 0.0)
        val meanOnlyResult = meanOnlyMatcher.compareBitmaps(expected, actual, w, h)
        assertTrue(
            meanOnlyResult.matches,
            "Expected mean-only check to pass: ${meanOnlyResult.comparisonStatistics}",
        )

        val defaultResult = matcher.compareBitmaps(expected, actual, w, h)
        assertFalse(
            defaultResult.matches,
            "Expected localized regression to fail p2 floor: ${defaultResult.comparisonStatistics}",
        )
        defaultResult.diff?.recycle()
    }

    @Test
    fun compareBitmaps_smallLocalizedRegressionAboveP2Fails() {
        val w = 480
        val h = 480
        val expected = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                expected[y * w + x] = Color.rgb(80 + (x * 120) / w, 100 + (y * 100) / h, 160)
            }
        }
        // Corrupt a 48x48 patch (1.0% area, ~2.5% of sliding windows): above the p2 tail floor
        // even though it would fall below a p5 floor, while mean stays >= 0.95.
        val actual = expected.copyOf()
        for (y in 32 until 80) {
            for (x in 32 until 80) {
                actual[y * w + x] = Color.rgb(30, 40, 70)
            }
        }

        val meanOnlyResult =
            XrSsimMatcher(minPercentileThreshold = 0.0).compareBitmaps(expected, actual, w, h)
        assertTrue(
            meanOnlyResult.matches,
            "Expected mean-only check to pass: ${meanOnlyResult.comparisonStatistics}",
        )

        val result = matcher.compareBitmaps(expected, actual, w, h)
        assertFalse(
            result.matches,
            "Expected ~2.5%-window localized regression to fail p2 floor: " +
                result.comparisonStatistics,
        )
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_depthAlignedSharpPanelsWithSubpixelBordersMatchOnBlackBackground() {
        val w = 480
        val h = 480
        val expected = renderDepthAlignedPanelsScene(w, h, Color.BLACK)
        // Simulate independent ~0.5px per-panel rasterization edges plus faint 2px border slivers
        // in opposite directions (no single global frame shift aligns all three panels).
        val actual =
            renderDepthAlignedPanelsScene(
                w,
                h,
                Color.BLACK,
                leftHalfShiftX = -1,
                midHalfShiftY = 1,
                rightHalfShiftX = 1,
            )

        val result = matcher.compareBitmaps(expected, actual, w, h)
        assertTrue(
            result.matches,
            "Expected sharp panels with independent ~0.5px borders to match: " +
                result.comparisonStatistics,
        )
    }

    @Test
    fun compareBitmaps_depthAlignedSharpPanelsTwoPixelShiftFails() {
        val w = 480
        val h = 480
        val expected = renderDepthAlignedPanelsScene(w, h, Color.BLACK)
        val actual = renderDepthAlignedPanelsScene(w, h, Color.BLACK, wholeShiftX = 2)

        val result = matcher.compareBitmaps(expected, actual, w, h)
        assertFalse(
            result.matches,
            "Expected 2px shift of sharp depth-aligned panels to fail: " +
                result.comparisonStatistics,
        )
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_sharpPanelsWhiteBackgroundSubpixelMatchesAndMissingPanelFails() {
        val w = 480
        val h = 480
        val expected = renderDepthAlignedPanelsScene(w, h, Color.WHITE)
        val subpixelShifted =
            renderDepthAlignedPanelsScene(
                w,
                h,
                Color.WHITE,
                leftHalfShiftX = 1,
                midHalfShiftY = -1,
                rightHalfShiftX = -1,
            )

        val shiftResult = matcher.compareBitmaps(expected, subpixelShifted, w, h)
        assertTrue(
            shiftResult.matches,
            "Expected white-background sharp panels with ~0.5px borders to match: " +
                shiftResult.comparisonStatistics,
        )

        val missingPanel =
            renderDepthAlignedPanelsScene(w, h, Color.WHITE, includeRightPanel = false)
        val missingResult = matcher.compareBitmaps(expected, missingPanel, w, h)
        assertFalse(
            missingResult.matches,
            "Expected missing panel on white background to fail: " +
                missingResult.comparisonStatistics,
        )
        missingResult.diff?.recycle()
    }

    @Test
    fun compareBitmaps_midGreyPanelMinorEdgeNoiseMatchesAndBadgeRegressionFails() {
        val w = 320
        val h = 240
        // Mimic an auto-cropped mid-grey UI panel (0x444444) with crisp colored axes, a
        // high-contrast UI label/badge, and subtle 1-2px edge fade/dither noise.
        fun renderMidGreyUiScene(
            edgeNoiseAmp: Int = 0,
            badgeContrast: Int = 60,
        ): IntArray {
            val bg = Color.rgb(68, 68, 68)
            val pixels = IntArray(w * h) { bg }
            // Outer vignette/edge fade band + subtle dither (variance ~0.015-0.025 across R/G/B)
            for (y in 0 until h) {
                for (x in (w - 2) until w) {
                    val v = (68 - (x - (w - 3)) * (3 + edgeNoiseAmp)).coerceIn(0, 255)
                    pixels[y * w + x] = Color.rgb(v, v, v)
                }
                if (edgeNoiseAmp != 0 && y in 16 until 80) {
                    for (x in 16 until 80) {
                        val n = if ((x + y) % 2 == 0) edgeNoiseAmp else -edgeNoiseAmp
                        val c = (68 + n).coerceIn(0, 255)
                        pixels[y * w + x] = Color.rgb(c, c, c)
                    }
                }
            }
            // Central colored widget (ensures scene stays actively foreground-populated)
            for (y in 80 until 180) {
                for (x in 100 until 220) {
                    val isAxisX = y in 126..134 && x in 140..200
                    val isAxisY = x in 156..164 && y in 92..134
                    pixels[y * w + x] =
                        when {
                            isAxisX -> Color.rgb(220, 50, 50)
                            isAxisY -> Color.rgb(50, 210, 70)
                            else -> Color.rgb(74, 74, 74)
                        }
                }
            }
            // Striped UI badge (64x64 -> ~6% of active windows on 320x240)
            val badgeVal = 68 + badgeContrast
            for (y in 24 until 88) {
                if ((y / 4) % 2 == 0) {
                    for (x in 224 until 288) {
                        pixels[y * w + x] = Color.rgb(badgeVal, badgeVal, badgeVal)
                    }
                }
            }
            return pixels
        }

        val expected = renderMidGreyUiScene()
        val withEdgeNoise = renderMidGreyUiScene(edgeNoiseAmp = 4)
        val noiseResult = matcher.compareBitmaps(expected, withEdgeNoise, w, h)
        assertTrue(
            noiseResult.matches,
            "Expected minor flat-grey edge noise to match under K2=0.06: " +
                noiseResult.comparisonStatistics,
        )

        val withBadgeRegression = renderMidGreyUiScene(badgeContrast = 16)
        val badgeResult = matcher.compareBitmaps(expected, withBadgeRegression, w, h)
        assertFalse(
            badgeResult.matches,
            "Expected dimmed UI badge contrast regression to fail: " +
                badgeResult.comparisonStatistics,
        )
        badgeResult.diff?.recycle()
    }

    @Test
    fun compareBitmaps_tinyForegroundObjectAgainstBlankCanvas_fails() {
        val w = 128
        val h = 128
        // An 8x8 pixel dot maps to 2x2 = 4 downsampled blocks (< 10 minActiveCells).
        val blackCanvas = IntArray(w * h) { Color.BLACK }
        val tinyWhiteDot =
            IntArray(w * h) { Color.BLACK }
                .also { pixels ->
                    for (y in 16 until 24) {
                        for (x in 16 until 24) {
                            pixels[y * w + x] = Color.WHITE
                        }
                    }
                }
        val dotVsBlackResult = matcher.compareBitmaps(tinyWhiteDot, blackCanvas, w, h)
        assertFalse(
            dotVsBlackResult.matches,
            "Tiny foreground dot must fail against blank black canvas: " +
                dotVsBlackResult.comparisonStatistics,
        )
        dotVsBlackResult.diff?.recycle()

        val dotMatchResult = matcher.compareBitmaps(tinyWhiteDot, tinyWhiteDot.copyOf(), w, h)
        assertTrue(dotMatchResult.matches, dotMatchResult.comparisonStatistics)
        assertNull(dotMatchResult.diff)

        val whiteCanvas = IntArray(w * h) { Color.WHITE }
        val tinyDarkDot =
            IntArray(w * h) { Color.WHITE }
                .also { pixels ->
                    for (y in 16 until 24) {
                        for (x in 16 until 24) {
                            pixels[y * w + x] = Color.BLACK
                        }
                    }
                }
        val dotVsWhiteResult = matcher.compareBitmaps(tinyDarkDot, whiteCanvas, w, h)
        assertFalse(
            dotVsWhiteResult.matches,
            "Tiny foreground dot must fail against blank white canvas: " +
                dotVsWhiteResult.comparisonStatistics,
        )
        dotVsWhiteResult.diff?.recycle()
    }

    @Test
    fun compareBitmaps_diffFallbackPass2_ignoresAlphaOnlyDifferences() {
        val w = 64
        val h = 64
        // Difference of 4/255 is <= DIFF_CHANNEL_DELTA (4), so Pass 1 marks 0 pixels and Pass 2
        // runs on any failed windows.
        val expected =
            IntArray(w * h) { i ->
                val y = i / w
                if ((y / 4) % 2 == 0) {
                    Color.argb(255, 104, 104, 104)
                } else {
                    Color.argb(255, 100, 100, 100)
                }
            }
        val given =
            IntArray(w * h) { i ->
                val y = i / w
                if ((y / 4) % 2 == 1) {
                    // Identical RGB to expected (100, 100, 100), differing only in alpha.
                    Color.argb(128, 100, 100, 100)
                } else {
                    Color.argb(255, 100, 100, 100)
                }
            }
        val strictMatcher = XrSsimMatcher(threshold = 0.99, minPercentileThreshold = 0.99)
        val result = strictMatcher.compareBitmaps(expected, given, w, h)
        assertFalse(result.matches, result.comparisonStatistics)
        val diff = assertNotNull(result.diff)
        try {
            assertEquals(Color.MAGENTA, diff.getPixel(0, 0))
            assertTrue(diff.getPixel(0, 4) != Color.MAGENTA)
        } finally {
            diff.recycle()
        }
    }

    @Test
    fun compareBitmaps_iblSpecularGlintOnIsoMatchedSpectatorChannelOverDarkGreyPanel_matches() {
        val w = 267
        val h = 279
        val expected = renderCenteredGizmoOnDarkGreyPanel(w, h)
        val withSpecularGlint = renderCenteredGizmoOnDarkGreyPanel(w, h, specularGlint = true)

        val result = matcher.compareBitmaps(expected, withSpecularGlint, w, h)
        assertTrue(
            result.matches,
            "Expected 1-2px white IBL specular glint on green shaft over #444444 panel to match: " +
                result.comparisonStatistics,
        )
        assertNull(result.diff)
        val (mean, p2) = parseMeanAndP2(result.comparisonStatistics)
        assertTrue(mean >= 0.950, "Expected mean >= 0.950: ${result.comparisonStatistics}")
        assertTrue(p2 >= 0.900, "Expected p2 >= 0.900: ${result.comparisonStatistics}")
    }

    @Test
    fun compareBitmaps_missingLowContrastThinGreyDividerLineOnDarkGreyPanel_fails() {
        val w = 267
        val h = 279
        val basePanel = IntArray(w * h) { Color.BLACK }
        for (y in 16 until (h - 16)) {
            for (x in 16 until (w - 16)) {
                basePanel[y * w + x] = Color.rgb(68, 68, 68)
            }
        }
        // Keep panel actively foreground-populated in lower half while top half tests the thin
        // line.
        for (y in 180 until 230) {
            for (x in 40 until 220) {
                basePanel[y * w + x] = Color.rgb(60, 140, 220)
            }
        }
        // Add a low-contrast (+30/255 -> 98/255) 4px vertical grey divider line on #444444
        // (68/255).
        // When missing in `basePanel`, cov == 0 across all channels (maxPosCov == 0), so contrast
        // stabilization grants zero relaxation.
        val withDividerLine = basePanel.copyOf()
        for (y in 50 until 138) {
            for (x in 130 until 134) {
                withDividerLine[y * w + x] = Color.rgb(98, 98, 98)
            }
        }

        val result = matcher.compareBitmaps(withDividerLine, basePanel, w, h)
        assertFalse(
            result.matches,
            "Expected missing 4px +30/255 grey divider line on #444444 to fail: " +
                result.comparisonStatistics,
        )
        val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
        assertTrue(p2 < 0.900, "Expected p2 < 0.900: ${result.comparisonStatistics}")
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_missingAndSpuriousLowContrastFeaturesNearColoredEdge_fails() {
        val w = 267
        val h = 279
        val basePanel = IntArray(w * h) { Color.BLACK }
        for (y in 16 until (h - 16)) {
            for (x in 16 until (w - 16)) {
                basePanel[y * w + x] = Color.rgb(68, 68, 68)
            }
        }
        // Place a high-contrast green edge across the exact same 32x32 windows as the test features
        // so maxPosCov (covG) is large while the missing/spurious channel has cov == 0.
        for (y in 50 until 138) {
            for (x in 112 until 126) {
                basePanel[y * w + x] = Color.rgb(68, 220, 68)
            }
        }

        val withGreyDivider = basePanel.copyOf()
        for (y in 50 until 138) {
            for (x in 130 until 134) {
                withGreyDivider[y * w + x] = Color.rgb(98, 98, 98)
            }
        }

        val withBlueStripe = basePanel.copyOf()
        for (y in 50 until 138) {
            for (x in 130 until 134) {
                withBlueStripe[y * w + x] = Color.rgb(68, 68, 108)
            }
        }

        for ((label, exp, act) in
            listOf(
                Triple("missing +30/255 grey divider near green edge", withGreyDivider, basePanel),
                Triple("spurious +30/255 grey divider near green edge", basePanel, withGreyDivider),
                Triple("missing +40/255 blue stripe near green edge", withBlueStripe, basePanel),
                Triple("spurious +40/255 blue stripe near green edge", basePanel, withBlueStripe),
            )) {
            val result = matcher.compareBitmaps(exp, act, w, h)
            assertFalse(
                result.matches,
                "Expected $label to fail: ${result.comparisonStatistics}",
            )
            val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
            assertTrue(p2 < 0.900, "Expected $label p2 < 0.900: ${result.comparisonStatistics}")
            result.diff?.recycle()
        }
    }

    @Test
    fun compareBitmaps_singleChannelContrastWashoutAlongsideStrongGreenEdge_fails() {
        val w = 240
        val h = 240
        val expected = IntArray(w * h)
        val washedOut = IntArray(w * h)
        // Left half vs right half split with a 0 -> 255 Green edge (covG ~ 0.25) and a co-located
        // 76/255 Blue edge (0.25 -> 0.55) whose contrast washes out by 2/3 (0.35 -> 0.45) at
        // identical mean (meanA_B == meanB_B == 0.40, r = 1.0).
        for (y in 0 until h) {
            for (x in 0 until w) {
                val isRight = x >= w / 2
                expected[y * w + x] = if (isRight) Color.rgb(64, 255, 140) else Color.rgb(64, 0, 64)
                washedOut[y * w + x] =
                    if (isRight) Color.rgb(64, 255, 115) else Color.rgb(64, 0, 89)
            }
        }

        val result = matcher.compareBitmaps(expected, washedOut, w, h)
        assertFalse(
            result.matches,
            "Expected mean-preserving Blue contrast washout alongside 0->255 Green edge to fail: " +
                result.comparisonStatistics,
        )
        val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
        assertTrue(p2 < 0.900, "Expected p2 < 0.900: ${result.comparisonStatistics}")
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_centeredShaftTwoPixelShift_fails() {
        val w = 267
        val h = 279
        val expected = renderCenteredGizmoOnDarkGreyPanel(w, h)
        val shifted2px = renderCenteredGizmoOnDarkGreyPanel(w, h, shaftShiftX = 2)

        val result = matcher.compareBitmaps(expected, shifted2px, w, h)
        assertFalse(
            result.matches,
            "Expected +2px shift on centered 3D shaft to fail: ${result.comparisonStatistics}",
        )
        val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
        assertTrue(p2 < 0.900, "Expected p2 < 0.900: ${result.comparisonStatistics}")
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_sixteenPixelBadgeDefect_fails() {
        val w = 240
        val h = 240
        val expected = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                expected[y * w + x] = Color.rgb(70 + (x * 60) / w, 80 + (y * 60) / h, 110)
            }
        }
        for (y in 40 until 56) {
            for (x in 40 until 56) {
                expected[y * w + x] = Color.rgb(220, 220, 230)
            }
        }
        val dimmedBadge = expected.copyOf()
        for (y in 40 until 56) {
            for (x in 40 until 56) {
                dimmedBadge[y * w + x] = Color.rgb(95, 100, 120)
            }
        }

        val meanOnlyResult =
            XrSsimMatcher(minPercentileThreshold = 0.0).compareBitmaps(expected, dimmedBadge, w, h)
        assertTrue(
            meanOnlyResult.matches,
            "Expected mean-only check to pass on 16x16 badge defect: " +
                meanOnlyResult.comparisonStatistics,
        )

        val result = matcher.compareBitmaps(expected, dimmedBadge, w, h)
        assertFalse(
            result.matches,
            "Expected 16x16 badge defect to fail p2 floor: ${result.comparisonStatistics}",
        )
        val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
        assertTrue(p2 < 0.900, "Expected p2 < 0.900: ${result.comparisonStatistics}")
        result.diff?.recycle()
    }

    @Test
    fun compareBitmaps_greenBlueSwapAndMaterialDesaturation_fails() {
        val w = 267
        val h = 279
        val expected = renderCenteredGizmoOnDarkGreyPanel(w, h)
        val greenBlueSwapped =
            IntArray(expected.size) { i ->
                val p = expected[i]
                (p and 0xFFFF0000.toInt()) or ((p and 0xFF) shl 8) or ((p shr 8) and 0xFF)
            }
        val desaturatedWhiteArrow = renderCenteredGizmoOnDarkGreyPanel(w, h, arrowMode = "white")
        val yellowShiftedArrow = renderCenteredGizmoOnDarkGreyPanel(w, h, arrowMode = "yellow")

        for ((label, candidate) in
            listOf(
                "G<->B channel swap" to greenBlueSwapped,
                "Green->White desaturation" to desaturatedWhiteArrow,
                "Green->Yellow hue shift" to yellowShiftedArrow,
            )) {
            val result = matcher.compareBitmaps(expected, candidate, w, h)
            assertFalse(
                result.matches,
                "Expected $label to fail: ${result.comparisonStatistics}",
            )
            val (_, p2) = parseMeanAndP2(result.comparisonStatistics)
            assertTrue(p2 < 0.900, "Expected $label p2 < 0.900: ${result.comparisonStatistics}")
            result.diff?.recycle()
        }
    }

    private fun parseMeanAndP2(stats: String): Pair<Double, Double> {
        val match =
            Regex("""Actual mean: ([0-9.]+), p\d+: ([0-9.]+)""").find(stats)
                ?: error("Unexpected comparisonStatistics format: $stats")
        return match.groupValues[1].toDouble() to match.groupValues[2].toDouble()
    }

    private fun renderCenteredGizmoOnDarkGreyPanel(
        w: Int,
        h: Int,
        specularGlint: Boolean = false,
        shaftShiftX: Int = 0,
        arrowMode: String = "green",
    ): IntArray {
        val pixels = IntArray(w * h) { Color.BLACK }
        for (y in 16 until (h - 16)) {
            for (x in 16 until (w - 16)) {
                pixels[y * w + x] = Color.rgb(68, 68, 68)
            }
        }
        // +X red horizontal shaft
        for (y in 146 until 152) {
            for (x in (131 + shaftShiftX) until (226 + shaftShiftX)) {
                pixels[y * w + x] = Color.rgb(195, 65, 58)
            }
        }
        // +Z blue origin sphere
        for (y in 138 until 161) {
            for (x in (97 + shaftShiftX) until (120 + shaftShiftX)) {
                val dx = x - (108 + shaftShiftX)
                val dy = y - 149
                if (dx * dx + dy * dy <= 100) {
                    pixels[y * w + x] = Color.rgb(110, 130, 195)
                }
            }
        }
        // +Y vertical green cylinder shaft (88px tall; diffuse Blue ~ 75 iso-matches #444444 = 68)
        for (y in 58 until 146) {
            for (dx in -2..2) {
                val x = 131 + shaftShiftX + dx
                var r = if (abs(dx) <= 1) 98 else 84
                var g = if (abs(dx) <= 1) 163 else 138
                var b = if (abs(dx) <= 1) 75 else 72
                when {
                    arrowMode == "white" -> {
                        r = g
                        b = g
                    }
                    arrowMode == "yellow" -> {
                        r = g
                    }
                    specularGlint && (dx == -2 || dx == -1) -> {
                        val scale = if (dx == -2) 1.0 else 0.5
                        r = (r + (49 * scale).toInt()).coerceIn(0, 255)
                        g = (g + (23 * scale).toInt()).coerceIn(0, 255)
                        b = (b + (60 * scale).toInt()).coerceIn(0, 255)
                    }
                }
                pixels[y * w + x] = Color.rgb(r, g, b)
            }
        }
        // +Y cone tip
        for (y in 40 until 58) {
            val halfW = (y - 40) / 2 + 1
            for (dx in -halfW..halfW) {
                val x = 131 + shaftShiftX + dx
                val r = if (arrowMode == "white" || arrowMode == "yellow") 170 else 102
                val g = 170
                val b = if (arrowMode == "white") 170 else 76
                pixels[y * w + x] = Color.rgb(r, g, b)
            }
        }
        return pixels
    }

    private fun renderSoftCircleScene(w: Int, h: Int, shiftX: Float, shiftY: Float): IntArray {
        val pixels = IntArray(w * h) { Color.BLACK }
        val cx = w / 2f + shiftX
        val cy = h / 2f + shiftY
        val radius = minOf(w, h) * 0.36f
        for (y in 0 until h) {
            for (x in 0 until w) {
                val dist = hypot(x - cx, y - cy)
                val edge = ((radius + 1.5f - dist) / 3f).coerceIn(0f, 1f)
                if (edge > 0f) {
                    val nx = ((x - cx) / radius).coerceIn(-1f, 1f)
                    val ny = ((y - cy) / radius).coerceIn(-1f, 1f)
                    val shade = (0.55f + 0.35f * (-nx * 0.5f - ny * 0.7f)).coerceIn(0.15f, 1f)
                    val r = (220f * shade * edge).toInt()
                    val g = (140f * shade * edge).toInt()
                    val b = (90f * shade * edge).toInt()
                    pixels[y * w + x] = Color.rgb(r, g, b)
                }
            }
        }
        return pixels
    }

    private fun renderMultiObjectScene(
        w: Int,
        h: Int,
        shiftX: Int,
        rColor: Int,
        bColor: Int,
        bgColor: Int = Color.BLACK,
    ): IntArray {
        val pixels = IntArray(w * h) { bgColor }
        // Left shaded rectangle (larger object)
        val lx0 = (w * 0.14).toInt() + shiftX
        val lx1 = (w * 0.48).toInt() + shiftX
        val ly0 = (h * 0.22).toInt()
        val ly1 = (h * 0.78).toInt()
        for (y in ly0 until ly1) {
            for (x in lx0 until lx1) {
                if (x in 0 until w) {
                    val grad = (x - lx0).toFloat() / (lx1 - lx0).coerceAtLeast(1)
                    val r = (((rColor shr 16) and 0xFF) * (0.5f + 0.5f * grad)).toInt()
                    val g = (((rColor shr 8) and 0xFF) * (0.5f + 0.5f * grad)).toInt()
                    val b = ((rColor and 0xFF) * (0.5f + 0.5f * grad)).toInt()
                    pixels[y * w + x] = Color.rgb(r, g, b)
                }
            }
        }
        // Right smaller object (asymmetrical)
        val rx0 = (w * 0.70).toInt() + shiftX
        val rx1 = (w * 0.86).toInt() + shiftX
        val ry0 = (h * 0.38).toInt()
        val ry1 = (h * 0.62).toInt()
        for (y in ry0 until ry1) {
            for (x in rx0 until rx1) {
                if (x in 0 until w) {
                    val stripe = if (abs(x - y) % 8 < 4) 1.0f else 0.6f
                    val r = (((bColor shr 16) and 0xFF) * stripe).toInt()
                    val g = (((bColor shr 8) and 0xFF) * stripe).toInt()
                    val b = ((bColor and 0xFF) * stripe).toInt()
                    pixels[y * w + x] = Color.rgb(r, g, b)
                }
            }
        }
        return pixels
    }

    private fun renderDepthAlignedPanelsScene(
        w: Int,
        h: Int,
        bgColor: Int,
        wholeShiftX: Int = 0,
        leftHalfShiftX: Int = 0,
        midHalfShiftY: Int = 0,
        rightHalfShiftX: Int = 0,
        includeRightPanel: Boolean = true,
    ): IntArray {
        val pixels = IntArray(w * h) { bgColor }

        fun drawPanel(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            color: Int,
            halfShiftX: Int = 0,
            halfShiftY: Int = 0,
        ) {
            val sx0 = x0 + wholeShiftX
            val sx1 = x1 + wholeShiftX
            val pr = (color shr 16) and 0xFF
            val pg = (color shr 8) and 0xFF
            val pb = color and 0xFF
            val br = (bgColor shr 16) and 0xFF
            val bg = (bgColor shr 8) and 0xFF
            val bb = bgColor and 0xFF

            fun blendBg(alpha: Float): Int =
                Color.rgb(
                    (br + (pr - br) * alpha).toInt(),
                    (bg + (pg - bg) * alpha).toInt(),
                    (bb + (pb - bb) * alpha).toInt(),
                )

            val minY = (y0 - 1).coerceAtLeast(0)
            val maxY = (y1 + 1).coerceAtMost(h)
            val minX = (sx0 - 1).coerceAtLeast(0)
            val maxX = (sx1 + 1).coerceAtMost(w)
            for (y in minY until maxY) {
                val fy =
                    when {
                        halfShiftY == 0 -> if (y in y0 until y1) 1f else 0f
                        halfShiftY < 0 ->
                            when {
                                y == y0 - 1 || y == y1 - 1 -> 0.5f
                                y in y0 until (y1 - 1) -> 1f
                                else -> 0f
                            }
                        else ->
                            when {
                                y == y0 || y == y1 -> 0.5f
                                y in (y0 + 1) until y1 -> 1f
                                else -> 0f
                            }
                    }
                if (fy <= 0f) continue
                for (x in minX until maxX) {
                    val fx =
                        when {
                            halfShiftX == 0 -> if (x in sx0 until sx1) 1f else 0f
                            halfShiftX < 0 ->
                                when {
                                    x == sx0 - 1 || x == sx1 - 1 -> 0.5f
                                    x in sx0 until (sx1 - 1) -> 1f
                                    else -> 0f
                                }
                            else ->
                                when {
                                    x == sx0 || x == sx1 -> 0.5f
                                    x in (sx0 + 1) until sx1 -> 1f
                                    else -> 0f
                                }
                        }
                    val cov = fx * fy
                    if (cov > 0f) {
                        pixels[y * w + x] = if (cov >= 1f) color else blendBg(cov)
                    }
                }
            }
        }

        // Depth-staggered sharp solid panels with subtle border proximity across sliding windows.
        drawPanel(
            x0 = 60,
            y0 = 80,
            x1 = 188,
            y1 = 240,
            color = Color.rgb(60, 120, 230),
            halfShiftX = leftHalfShiftX,
        )
        drawPanel(
            x0 = 172,
            y0 = 112,
            x1 = 308,
            y1 = 268,
            color = Color.rgb(50, 190, 110),
            halfShiftY = midHalfShiftY,
        )
        if (includeRightPanel) {
            drawPanel(
                x0 = 292,
                y0 = 140,
                x1 = 420,
                y1 = 300,
                color = Color.rgb(220, 160, 50),
                halfShiftX = rightHalfShiftX,
            )
        }
        return pixels
    }
}
