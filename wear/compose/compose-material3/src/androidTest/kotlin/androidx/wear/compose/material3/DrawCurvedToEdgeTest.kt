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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.AnchorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class DrawCurvedToEdgeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun update_maxSweepAngle_matchesFreshComposition() {
        var maxSweepAngle by mutableFloatStateOf(40f)
        var remountKey by mutableIntStateOf(0)

        setCaptureBox {
            key(remountKey) {
                Text(
                    "Long text that overflows at narrow sweep angles",
                    Modifier.drawCurvedToEdge(maxSweepAngle = maxSweepAngle),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val initial = capturePixels()
        assertHasNonBackgroundPixels(initial)

        rule.runOnIdle { maxSweepAngle = 120f }
        val mutated = capturePixels()
        assertPixelsNotEqual(initial, mutated)

        rule.runOnIdle { remountKey++ }
        val fresh = capturePixels()
        assertPixelsEqual(fresh, mutated)
    }

    @Test
    fun update_angularDirection_matchesFreshComposition() {
        var angularDirection by mutableStateOf(AngularDirection.Clockwise)
        var remountKey by mutableIntStateOf(0)

        setCaptureBox {
            key(remountKey) {
                Text(
                    "Curved Text",
                    Modifier.drawCurvedToEdge(
                        maxSweepAngle = 120f,
                        angularDirection = angularDirection,
                        anchor = 90f,
                    ),
                    color = Color.White,
                )
            }
        }

        val initial = capturePixels()
        assertHasNonBackgroundPixels(initial)

        rule.runOnIdle { angularDirection = AngularDirection.CounterClockwise }
        val mutated = capturePixels()
        assertPixelsNotEqual(initial, mutated)

        rule.runOnIdle { remountKey++ }
        val fresh = capturePixels()
        assertPixelsEqual(fresh, mutated)
    }

    @Test
    fun update_anchor_clockwise_matchesFreshComposition() {
        var anchor by mutableFloatStateOf(270f)
        var remountKey by mutableIntStateOf(0)

        setCaptureBox {
            key(remountKey) {
                Text(
                    "Anchor CW",
                    Modifier.drawCurvedToEdge(maxSweepAngle = 120f, anchor = anchor),
                    color = Color.White,
                )
            }
        }

        val initial = capturePixels()
        assertHasNonBackgroundPixels(initial)

        rule.runOnIdle { anchor = 180f }
        val mutated = capturePixels()
        assertPixelsNotEqual(initial, mutated)

        rule.runOnIdle { remountKey++ }
        val fresh = capturePixels()
        assertPixelsEqual(fresh, mutated)
    }

    @Test
    fun update_anchor_counterClockwise_matchesFreshComposition() {
        var anchor by mutableFloatStateOf(90f)
        var remountKey by mutableIntStateOf(0)

        setCaptureBox {
            key(remountKey) {
                Text(
                    "Anchor CCW",
                    Modifier.drawCurvedToEdge(
                        maxSweepAngle = 120f,
                        angularDirection = AngularDirection.CounterClockwise,
                        anchor = anchor,
                    ),
                    color = Color.White,
                )
            }
        }

        val initial = capturePixels()
        assertHasNonBackgroundPixels(initial)

        // Exercise 0-degree CounterClockwise anchor update specifically
        rule.runOnIdle { anchor = 0f }
        val mutated = capturePixels()
        assertPixelsNotEqual(initial, mutated)

        rule.runOnIdle { remountKey++ }
        val fresh = capturePixels()
        assertPixelsEqual(fresh, mutated)
    }

    @Test
    fun update_anchorType_matchesFreshComposition() {
        var anchorType by mutableStateOf(AnchorType.Start)
        var remountKey by mutableIntStateOf(0)

        setCaptureBox {
            key(remountKey) {
                // Span > 35 degrees within the 70-degree default sweep
                Text(
                    "Longer Anchor Text",
                    Modifier.drawCurvedToEdge(anchorType = anchorType),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }

        val startPixels = capturePixels()
        assertHasNonBackgroundPixels(startPixels)

        rule.runOnIdle { anchorType = AnchorType.Center }
        val mutatedCenter = capturePixels()
        assertPixelsNotEqual(startPixels, mutatedCenter)

        rule.runOnIdle { remountKey++ }
        val freshCenter = capturePixels()
        assertPixelsEqual(freshCenter, mutatedCenter)

        rule.runOnIdle { anchorType = AnchorType.End }
        val mutatedEnd = capturePixels()
        assertPixelsNotEqual(freshCenter, mutatedEnd)

        rule.runOnIdle { remountKey++ }
        val freshEnd = capturePixels()
        assertPixelsEqual(freshEnd, mutatedEnd)
    }

    @Test
    fun edgePlacement_cancelsParentAlignment() {
        var alignBottom by mutableStateOf(true)
        var remountKey by mutableIntStateOf(0)

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE) {
                Box(
                    Modifier.size(CAPTURE_SIZE).background(BACKGROUND_COLOR).testTag(TEST_TAG),
                    contentAlignment =
                        if (alignBottom) Alignment.BottomCenter else Alignment.TopStart,
                ) {
                    key(remountKey) {
                        Text("Edge Text", Modifier.drawCurvedToEdge(), color = Color.White)
                    }
                }
            }
        }

        val bottomAlignedPixels = capturePixels()
        assertHasNonBackgroundPixels(bottomAlignedPixels)

        // Verify live parent re-alignment without remounting the node
        rule.runOnIdle { alignBottom = false }
        val liveTopStartPixels = capturePixels()
        assertPixelsEqual(liveTopStartPixels, bottomAlignedPixels)

        // Verify fresh composition at TopStart also matches
        rule.runOnIdle { remountKey++ }
        val freshTopStartPixels = capturePixels()
        assertPixelsEqual(freshTopStartPixels, bottomAlignedPixels)
    }

    @Test
    fun edgePlacement_paddedParentCancelsAlignmentAndPreservesInset() {
        var isPadded by mutableStateOf(false)
        var alignBottom by mutableStateOf(true)

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE) {
                Box(
                    Modifier.size(CAPTURE_SIZE)
                        .background(BACKGROUND_COLOR)
                        .testTag(TEST_TAG)
                        .then(if (isPadded) Modifier.padding(16.dp) else Modifier),
                    contentAlignment =
                        if (alignBottom) Alignment.BottomCenter else Alignment.TopStart,
                ) {
                    Text("Padded", Modifier.drawCurvedToEdge(120f), color = Color.White)
                }
            }
        }

        val unpaddedPixels = capturePixels()
        assertHasNonBackgroundPixels(unpaddedPixels)

        rule.runOnIdle { isPadded = true }
        val paddedBottomAlignedPixels = capturePixels()
        assertHasNonBackgroundPixels(paddedBottomAlignedPixels)
        assertPixelsNotEqual(unpaddedPixels, paddedBottomAlignedPixels)

        rule.runOnIdle { alignBottom = false }
        val paddedTopStartPixels = capturePixels()
        assertPixelsEqual(paddedTopStartPixels, paddedBottomAlignedPixels)
    }

    private fun setCaptureBox(content: @Composable BoxScope.() -> Unit) {
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE) {
                Box(
                    Modifier.size(CAPTURE_SIZE).background(BACKGROUND_COLOR).testTag(TEST_TAG),
                    content = content,
                )
            }
        }
    }

    private fun capturePixels(): PixelMap =
        rule.onNodeWithTag(TEST_TAG).captureToImage().toPixelMap()

    private fun assertHasNonBackgroundPixels(pixels: PixelMap) {
        var found = false
        outer@ for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y] != BACKGROUND_COLOR) {
                    found = true
                    break@outer
                }
            }
        }
        assertTrue("Expected non-background pixels to be rendered", found)
    }

    private fun assertPixelsEqual(expected: PixelMap, actual: PixelMap) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                assertEquals("Pixel mismatch at ($x, $y)", expected[x, y], actual[x, y])
            }
        }
    }

    private fun assertPixelsNotEqual(unexpected: PixelMap, actual: PixelMap) {
        assertEquals(unexpected.width, actual.width)
        assertEquals(unexpected.height, actual.height)
        for (y in 0 until unexpected.height) {
            for (x in 0 until unexpected.width) {
                if (unexpected[x, y] != actual[x, y]) return
            }
        }
        throw AssertionError("Expected pixel maps to differ, but all pixels were identical")
    }
}

private val CAPTURE_SIZE = 150.dp
private val BACKGROUND_COLOR = Color.Black
