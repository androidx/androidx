/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ProgressIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nonMaterialSetContent() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setContent {
            LinearProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = progress.value
            )
        }

        rule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun determinateLinearProgressIndicator_Progress() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearProgressIndicator(modifier = Modifier.testTag(tag), progress = progress.value)
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateLinearProgressIndicator_ProgressIsCoercedInBounds() {
        val tag = "linear"
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearProgressIndicator(modifier = Modifier.testTag(tag), progress = progress.value)
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 1.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinateLinearProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions {
                LinearProgressIndicator(progress = 0f)
            }
            .assertWidthIsEqualTo(LinearIndicatorWidth)
            .assertHeightIsEqualTo(LinearIndicatorHeight)
    }

    @Test
    fun indeterminateLinearProgressIndicator_Progress() {
        val tag = "linear"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            LinearProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateLinearProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule
            .setMaterialContentForSizeAssertions {
                LinearProgressIndicator()
            }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertWidthIsEqualTo(LinearIndicatorWidth)
            .assertHeightIsEqualTo(LinearIndicatorHeight)
    }

    @Test
    fun determinateCircularProgressIndicator_Progress() {
        val tag = "circular"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = progress.value
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateCircularProgressIndicator_ProgressIsCoercedInBounds() {
        val tag = "circular"
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = progress.value
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 1.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinateCircularProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions {
                CircularProgressIndicator(progress = 0f)
            }
            .assertIsSquareWithSize(CircularIndicatorDiameter)
    }

    @Test
    fun indeterminateCircularProgressIndicator_progress() {
        val tag = "circular"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            CircularProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateCircularProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule
            .setMaterialContentForSizeAssertions {
                CircularProgressIndicator()
            }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertIsSquareWithSize(CircularIndicatorDiameter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun determinateLinearProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val expectedSize = with(rule.density) {
            IntSize(expectedWidth.roundToPx(), expectedHeight.roundToPx())
        }
        val tag = "progress_indicator"
        rule.setContent {
            Box(Modifier.testTag(tag)) {
                LinearProgressIndicator(
                    modifier = Modifier.size(expectedWidth, expectedHeight),
                    progress = 1f,
                    color = Color.Blue)
            }
        }

        rule.onNodeWithTag(tag)
            .captureToImage()
            .assertPixels(expectedSize = expectedSize) {
                Color.Blue
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateLinearProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val expectedSize = with(rule.density) {
            IntSize(expectedWidth.roundToPx(), expectedHeight.roundToPx())
        }
        rule.mainClock.autoAdvance = false
        val tag = "progress_indicator"
        rule.setContent {
            Box(Modifier.testTag(tag)) {
                LinearProgressIndicator(
                    modifier = Modifier.size(expectedWidth, expectedHeight),
                    color = Color.Blue)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag(tag)
            .captureToImage()
            .toPixelMap()
            .let {
                assertEquals(expectedSize.width, it.width)
                assertEquals(expectedSize.height, it.height)
                // Assert on the first pixel column, to make sure that the progress indicator draws
                // to the expect height.
                // We can't assert width as the width dynamically changes during the animation
                for (i in 0 until it.height) {
                    it.assertPixelColor(Color.Blue, 0, i)
                }
            }
    }

    @Test
    fun indeterminateLinearProgressIndicator_semanticsNodeBounds() {
        val padding = 10.dp
        val paddingPx = with(rule.density) { padding.roundToPx() }

        val expectedWidth = with(rule.density) {
            LinearIndicatorWidth.roundToPx()
        }
        val expectedHeight = with(rule.density) {
            LinearIndicatorHeight.roundToPx() + paddingPx * 2
        }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // Adding a testTag on the progress bar itself captures the progress bar as well as its
        // padding.
        val tag = "progress_indicator"
        rule.setContent {
            LinearProgressIndicator(Modifier.testTag(tag))
        }

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the SemanticsNode bounds of a LinearProgressIndicator include the padding.
        // This means that the SemanticsNode bounds are big enough to trigger TalkBack's green
        // focus indicator.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun indeterminateLinearProgressIndicator_visualBounds() {
        val expectedWidth = with(rule.density) { LinearIndicatorWidth.roundToPx() }
        val expectedHeight = with(rule.density) { LinearIndicatorHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        val tag = "progress_indicator"
        rule.setContent {
            Box(Modifier.testTag(tag)) {
                LinearProgressIndicator()
            }
        }

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the visual bounds of a LinearProgressIndicator are the expected visual size.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearProgressIndicator_semanticsNodeBounds() {
        val padding = 10.dp
        val paddingPx = with(rule.density) { padding.roundToPx() }

        val expectedWidth = with(rule.density) {
            LinearIndicatorWidth.roundToPx()
        }
        val expectedHeight = with(rule.density) {
            LinearIndicatorHeight.roundToPx() + paddingPx * 2
        }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // Adding a testTag on the progress bar itself captures the progress bar as well as its
        // padding.
        val tag = "progress_indicator"
        rule.setContent {
            LinearProgressIndicator(modifier = Modifier.testTag(tag), progress = 1f)
        }

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the SemanticsNode bounds of a LinearProgressIndicator include the padding.
        // This means that the SemanticsNode bounds are big enough to trigger TalkBack's green
        // focus indicator.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearProgressIndicator_visualBounds() {
        val expectedWidth = with(rule.density) { LinearIndicatorWidth.roundToPx() }
        val expectedHeight = with(rule.density) { LinearIndicatorHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        val tag = "progress_indicator"
        rule.setContent {
            Box(Modifier.testTag(tag)) {
                LinearProgressIndicator(progress = 1f)
            }
        }

        val node = rule.onNodeWithTag(tag)
            .fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the visual bounds of a LinearProgressIndicator are the expected visual size.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearProgressIndicator_scrollingBounds() {
        val padding = 10.dp
        val paddingPx = with(rule.density) { padding.roundToPx() }

        val expectedWidth = with(rule.density) { LinearIndicatorWidth.roundToPx() }
        val expectedHeight = with(rule.density) { LinearIndicatorHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        val paddingHeight = with(rule.density) { LinearIndicatorHeight.roundToPx() + paddingPx * 2 }
        val paddingSize = IntSize(expectedWidth, paddingHeight)

        val withPaddingTag = "with_padding"
        val visualTag = "visual_tag"
        rule.setContent {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // Add some content to the scrollable column
                repeat(20) {
                    Text("Item $it")
                }
                // The visual tag will measure the visual bounds of the indicator, while
                // the withPadding tag will measure with padding.
                Box(Modifier.testTag(visualTag)) {
                    LinearProgressIndicator(
                        modifier = Modifier.testTag(withPaddingTag), progress = 1f
                    )
                }
                repeat(20) {
                    Text("Item $it")
                }
            }
        }

        val indicatorNode = rule.onNodeWithTag(visualTag)
            .fetchSemanticsNode("couldn't find node with tag $visualTag")
        val indicatorBounds = indicatorNode.boundsInRoot
        // Check that the visual bounds of a LinearProgressIndicator are the expected visual size.
        assertEquals(expectedSize.width.toFloat(), indicatorBounds.width)
        assertEquals(expectedSize.height.toFloat(), indicatorBounds.height)

        val semanticsNode = rule.onNodeWithTag(withPaddingTag)
            .fetchSemanticsNode("couldn't find node with tag $withPaddingTag")
        // Make sure to get the bounds with no clipping applied by
        // using Rect(positionInRoot, size.toSize()).
        val semanticsBound = Rect(semanticsNode.positionInRoot, semanticsNode.size.toSize())
        // Check that the SemanticsNode bounds of the scrolling column are as expected.
        assertEquals(paddingSize.height.toFloat(), semanticsBound.height)
    }
}
