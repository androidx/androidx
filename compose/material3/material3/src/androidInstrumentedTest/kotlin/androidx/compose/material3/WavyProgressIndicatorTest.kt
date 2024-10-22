/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.material3.internal.VerticalSemanticsBoundsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertPixelColor
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class WavyProgressIndicatorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun nonMaterialSetContent() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setContent {
            LinearWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value },
            )
        }

        rule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun determinateLinearWavyProgressIndicator_Progress() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value }
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 0.5f }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateLinearWavyProgressIndicator_ProgressIsCoercedInBounds() {
        val tag = "linear"
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value }
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 1.5f }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinateLinearWavyProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions { LinearWavyProgressIndicator(progress = { 0f }) }
            .assertWidthIsEqualTo(WavyProgressIndicatorDefaults.LinearContainerWidth)
            .assertHeightIsEqualTo(WavyProgressIndicatorDefaults.LinearContainerHeight)
    }

    @Test
    fun indeterminateLinearWavyProgressIndicator_Progress() {
        val tag = "linear"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            LinearWavyProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateLinearWavyProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest =
            rule.setMaterialContentForSizeAssertions { LinearWavyProgressIndicator() }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertWidthIsEqualTo(WavyProgressIndicatorDefaults.LinearContainerWidth)
            .assertHeightIsEqualTo(WavyProgressIndicatorDefaults.LinearContainerHeight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun determinateLinearWavyProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val tag = "linear"
        val contentToTest =
            rule.setMaterialContentForSizeAssertions {
                LinearWavyProgressIndicator(
                    modifier = Modifier.size(expectedWidth, expectedHeight).testTag(tag),
                    progress = { 0.5f }
                )
            }

        contentToTest.assertWidthIsEqualTo(expectedWidth).assertHeightIsEqualTo(expectedHeight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun determinateLinearWavyProgressIndicator_colors() {
        val tag = "linear"
        var trackColor = Color.Unspecified
        var progressColor = Color.Unspecified
        rule.setMaterialContentForSizeAssertions {
            trackColor = ProgressIndicatorDefaults.linearTrackColor
            progressColor = ProgressIndicatorDefaults.linearColor

            Box(Modifier.testTag(tag)) { LinearWavyProgressIndicator(progress = { 0.5f }) }
        }

        rule
            .onNodeWithTag(tag)
            .captureToImage()
            .assertContainsColor(trackColor)
            .assertContainsColor(progressColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateLinearWavyProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val tag = "linear"
        val contentToTest =
            rule.setMaterialContentForSizeAssertions {
                LinearWavyProgressIndicator(
                    modifier = Modifier.size(expectedWidth, expectedHeight).testTag(tag),
                )
            }

        contentToTest.assertWidthIsEqualTo(expectedWidth).assertHeightIsEqualTo(expectedHeight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateLinearWavyProgressIndicator_colors() {
        rule.mainClock.autoAdvance = false
        val tag = "linear"
        rule.setMaterialContentForSizeAssertions {
            Box(Modifier.testTag(tag)) { LinearWavyProgressIndicator(color = Color.Blue) }
        }

        rule.mainClock.advanceTimeBy(300)

        rule.onNodeWithTag(tag).captureToImage().toPixelMap().let {
            // Assert that a center pixel relatively at the start of the path has the right
            // progress color.
            it.assertPixelColor(Color.Blue, 5, it.height / 2)
        }
    }

    @Test
    fun indeterminateLinearWavyProgressIndicator_semanticsNodeBounds() {
        val paddingPx = with(rule.density) { VerticalSemanticsBoundsPadding.roundToPx() }

        val expectedWidth =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerWidth.roundToPx() }
        val expectedHeight =
            with(rule.density) {
                WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() + paddingPx * 2
            }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // Adding a testTag on the progress bar itself captures the progress bar as well as its
        // padding.
        val tag = "linear"
        rule.setContent { LinearWavyProgressIndicator(Modifier.testTag(tag)) }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the SemanticsNode bounds of an LinearWavyProgressIndicator include the
        // padding. This means that the SemanticsNode bounds are big enough to trigger TalkBack's
        // green focus indicator.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun indeterminateLinearWavyProgressIndicator_visualBounds() {
        val expectedWidth =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerWidth.roundToPx() }
        val expectedHeight =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected by
        // the padding added on the layout of the progress bar.
        val tag = "linear"
        rule.setContent { Box(Modifier.testTag(tag)) { LinearWavyProgressIndicator() } }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the visual bounds of an LinearWavyProgressIndicator are the expected visual
        // size.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearWavyProgressIndicator_semanticsNodeBounds() {
        val paddingPx = with(rule.density) { VerticalSemanticsBoundsPadding.roundToPx() }

        val expectedWidth =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerWidth.roundToPx() }
        val expectedHeight =
            with(rule.density) {
                WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() + paddingPx * 2
            }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // Adding a testTag on the progress bar itself captures the progress bar as well as its
        // padding.
        val tag = "linear"
        rule.setContent {
            LinearWavyProgressIndicator(modifier = Modifier.testTag(tag), progress = { 1f })
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the SemanticsNode bounds of an LinearWavyProgressIndicator include the
        // padding. This means that the SemanticsNode bounds are big enough to trigger TalkBack's
        // green focus indicator.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearWavyProgressIndicator_visualBounds() {
        val expectedWidth =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerWidth.roundToPx() }
        val expectedHeight =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        val tag = "linear"
        rule.setContent {
            Box(Modifier.testTag(tag)) { LinearWavyProgressIndicator(progress = { 1f }) }
        }

        val node = rule.onNodeWithTag(tag).fetchSemanticsNode("couldn't find node with tag $tag")
        val nodeBounds = node.boundsInRoot

        // Check that the visual bounds of an LinearWavyProgressIndicator are the expected visual
        // size.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    @Test
    fun determinateLinearWavyProgressIndicator_scrollingBounds() {
        val padding = 10.dp
        val paddingPx = with(rule.density) { padding.roundToPx() }

        val expectedWidth =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerWidth.roundToPx() }
        val expectedHeight =
            with(rule.density) { WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        val paddingHeight =
            with(rule.density) {
                WavyProgressIndicatorDefaults.LinearContainerHeight.roundToPx() + paddingPx * 2
            }
        val paddingSize = IntSize(expectedWidth, paddingHeight)

        val withPaddingTag = "with_padding"
        val visualTag = "visual_tag"
        rule.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Add some content to the scrollable column
                repeat(20) { Text("Item $it") }
                // The visual tag will measure the visual bounds of the indicator, while the
                // withPadding tag will measure with padding.
                Box(Modifier.testTag(visualTag)) {
                    LinearWavyProgressIndicator(
                        modifier = Modifier.testTag(withPaddingTag),
                        progress = { 1f },
                    )
                }
                repeat(20) { Text("Item $it") }
            }
        }

        val indicatorNode =
            rule
                .onNodeWithTag(visualTag)
                .fetchSemanticsNode("couldn't find node with tag $visualTag")
        val indicatorBounds = indicatorNode.boundsInRoot
        // Check that the visual bounds of an LinearWavyProgressIndicator are the expected visual
        // size.
        assertEquals(expectedSize.width.toFloat(), indicatorBounds.width)
        assertEquals(expectedSize.height.toFloat(), indicatorBounds.height)

        val semanticsNode =
            rule
                .onNodeWithTag(withPaddingTag)
                .fetchSemanticsNode("couldn't find node with tag $withPaddingTag")
        // Make sure to get the bounds with no clipping applied by using Rect(positionInRoot,
        // size.toSize()).
        val semanticsBound = Rect(semanticsNode.positionInRoot, semanticsNode.size.toSize())
        // Check that the SemanticsNode bounds of the scrolling column are as expected.
        assertEquals(paddingSize.height.toFloat(), semanticsBound.height)
    }

    @Test
    fun determinateCircularWavyProgressIndicator_Progress() {
        val tag = "circular"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            CircularWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value },
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 0.5f }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateCircularWavyProgressIndicator_ProgressIsCoercedInBounds() {
        val tag = "circular"
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            CircularWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value },
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread { progress.value = 1.5f }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun determinateCircularWavyProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions {
                CircularWavyProgressIndicator(progress = { 0f })
            }
            .assertIsSquareWithSize(WavyProgressIndicatorDefaults.CircularContainerSize)
    }

    @Ignore("b/347736702") // TODO: Ignoring this until the underlying issue at b/347771353 is fixed
    @Test
    fun indeterminateCircularWavyProgressIndicator_progress() {
        val tag = "circular"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            CircularWavyProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateCircularWavyProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest =
            rule.setMaterialContentForSizeAssertions { CircularWavyProgressIndicator() }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest.assertIsSquareWithSize(WavyProgressIndicatorDefaults.CircularContainerSize)
    }
}
