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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.internal.VerticalSemanticsBoundsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertPixelColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WavyProgressIndicatorTest {
    private val motionDurationScale =
        object : MotionDurationScale {
            override var scaleFactor: Float = 1f
        }

    @get:Rule val rule = createComposeRule(ComposeUiTestConfig(motionDurationScale))

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
    fun determinateLinearWavyProgressIndicator_NaNProgress() {
        val tag = "linear"
        val progress = mutableStateOf(Float.NaN)
        rule.setMaterialContent(lightColorScheme()) {
            LinearWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value },
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)

        rule.runOnUiThread { progress.value = Float.POSITIVE_INFINITY }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun determinateLinearWavyProgressIndicator_ProgressIsCoercedInBounds() {
        val tag = "linear"
        val progress = mutableStateOf(-1f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearWavyProgressIndicator(
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
        val contentToTest = rule.setMaterialContentForSizeAssertions {
            LinearWavyProgressIndicator()
        }

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
        val contentToTest = rule.setMaterialContentForSizeAssertions {
            LinearWavyProgressIndicator(
                modifier = Modifier.size(expectedWidth, expectedHeight).testTag(tag),
                progress = { 0.5f },
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
        val contentToTest = rule.setMaterialContentForSizeAssertions {
            LinearWavyProgressIndicator(
                modifier = Modifier.size(expectedWidth, expectedHeight).testTag(tag)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateLinearWavyProgressIndicator_lambdaUpdate() {
        rule.mainClock.autoAdvance = false
        data class Data(val progress: Float)
        data class Data2(val data: Data)
        val tag = "linear"
        var indicatorColor: Color? = null
        lateinit var flow: MutableStateFlow<Data2>
        rule.setContent {
            // Have a flow that updates the progress by creating a new Data object each time.
            flow = remember { MutableStateFlow(Data2(Data(0f))) }
            val state = flow.collectAsState()
            // Extract the data here and read the progress in the progress lambda we pass to the
            // indicator. The test ensures that the indicator is properly invalidating its cache
            // to re-read from the progress lambda when the lambda reference is changing here.
            val data = state.value.data
            LinearWavyProgressIndicator(
                progress = { data.progress },
                // Disable the stop indicator for this test as we check for just indicator colors.
                stopSize = 0.dp,
                modifier = Modifier.testTag(tag),
            )
            indicatorColor = WavyProgressIndicatorDefaults.indicatorColor
        }
        rule.onNodeWithTag(tag).captureToImage().assertDoesNotContainColor(indicatorColor!!)
        rule.runOnIdle { flow.update { Data2(Data(progress = flow.value.data.progress + 0.1f)) } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(tag).captureToImage().assertContainsColor(indicatorColor)
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
    fun determinateCircularWavyProgressIndicator_NaNProgress() {
        val tag = "circular"
        val progress = mutableStateOf(Float.NaN)
        rule.setMaterialContent(lightColorScheme()) {
            CircularWavyProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = { progress.value },
            )
        }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)

        rule.runOnUiThread { progress.value = Float.POSITIVE_INFINITY }

        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
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
        val contentToTest = rule.setMaterialContentForSizeAssertions {
            CircularWavyProgressIndicator()
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest.assertIsSquareWithSize(WavyProgressIndicatorDefaults.CircularContainerSize)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateCircularWavyProgressIndicator_lambdaUpdate() {
        rule.mainClock.autoAdvance = false
        data class Data(val progress: Float)
        data class Data2(val data: Data)
        val tag = "circular"
        var indicatorColor: Color? = null
        lateinit var flow: MutableStateFlow<Data2>
        rule.setContent {
            // Have a flow that updates the progress by creating a new Data object each time.
            flow = remember { MutableStateFlow(Data2(Data(0f))) }
            val state = flow.collectAsState()
            // Extract the data here and read the progress in the progress lambda we pass to the
            // indicator. The test ensures that the indicator is properly invalidating its cache
            // to re-read from the progress lambda when the lambda reference is changing here.
            val data = state.value.data
            CircularWavyProgressIndicator(
                progress = { data.progress },
                modifier = Modifier.testTag(tag),
            )
            indicatorColor = WavyProgressIndicatorDefaults.indicatorColor
        }
        rule.onNodeWithTag(tag).captureToImage().assertDoesNotContainColor(indicatorColor!!)
        rule.runOnIdle { flow.update { Data2(Data(progress = flow.value.data.progress + 0.1f)) } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(tag).captureToImage().assertContainsColor(indicatorColor)
    }

    @Test
    fun circularWavyProgressIndicator_ZeroSize() {
        val tag = "circular"
        rule.mainClock.autoAdvance = false
        rule.setMaterialContentForSizeAssertions {
            Box(modifier = Modifier.size(50.dp)) {
                CircularWavyProgressIndicator(modifier = Modifier.requiredSize(0.dp).testTag(tag))
            }
        }

        rule.mainClock.advanceTimeBy(100)

        // Test that we allow zero sizing without any exception.
        rule.onNodeWithTag(tag).assertIsSquareWithSize(0.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun linearWavyProgressIndicator_nanToProgress_handsOffWhenLinesLeave() {
        rule.mainClock.autoAdvance = false
        val tag = "linear"
        val referenceTag = "reference"
        val progress = mutableStateOf(Float.NaN)
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
                LinearWavyProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.testTag(tag),
                    waveSpeed = 0.dp,
                )
                LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(referenceTag),
                    waveSpeed = 0.dp,
                )
            }
        }

        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle { progress.value = 0.5f }

        rule.mainClock.advanceTimeBy(100)
        assertDifferentDrawings(tag, referenceTag)
        rule.mainClock.advanceTimeBy(1300)
        assertSameDrawings(tag, referenceTag)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun circularWavyProgressIndicator_nanToProgress_settlesAtTrackStart() {
        rule.mainClock.autoAdvance = false
        val tag = "circular"
        val referenceTag = "reference"
        val progress = mutableStateOf(Float.NaN)
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
                CircularWavyProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.testTag(tag),
                    waveSpeed = 0.dp,
                )
                CircularWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(referenceTag),
                    waveSpeed = 0.dp,
                )
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle { progress.value = 0.5f }

        rule.mainClock.advanceTimeBy(100)
        assertDifferentDrawings(tag, referenceTag)
        rule.mainClock.advanceTimeBy(1300)
        assertSameDrawings(tag, referenceTag)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun wavyProgressIndicators_progressToNaN_restartIndeterminateCycle() {
        rule.mainClock.autoAdvance = false
        val progress = mutableStateOf(Float.NaN)
        setIndicatorsWithReferences { progress.value }
        rule.onNodeWithTag(LinearTag).captureToImage()
        rule.mainClock.advanceTimeBy(300)
        val linear = rule.onNodeWithTag(LinearTag).captureToImage()
        val circular = rule.onNodeWithTag(CircularTag).captureToImage()

        rule.runOnIdle { progress.value = 0.5f }
        rule.mainClock.advanceTimeBy(3000)
        rule.runOnIdle { progress.value = Float.NaN }
        rule.onNodeWithTag(LinearTag).captureToImage()
        rule.mainClock.advanceTimeBy(300)

        assertTrue(rule.onNodeWithTag(LinearTag).captureToImage().contentEquals(linear))
        assertTrue(rule.onNodeWithTag(CircularTag).captureToImage().contentEquals(circular))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun wavyProgressIndicators_nanDuringHandoff_returnToIndeterminate() {
        rule.mainClock.autoAdvance = false
        val progress = mutableStateOf(Float.NaN)
        setIndicatorsWithReferences { progress.value }

        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle { progress.value = 0.5f }
        rule.mainClock.advanceTimeBy(100)
        rule.runOnIdle { progress.value = Float.NaN }
        rule.mainClock.advanceTimeBy(1500)
        assertDifferentDrawings(LinearTag, LinearReferenceTag)
        assertDifferentDrawings(CircularTag, CircularReferenceTag)

        rule.runOnIdle { progress.value = 0.5f }
        rule.mainClock.advanceTimeBy(2000)
        assertSameDrawings(LinearTag, LinearReferenceTag)
        assertSameDrawings(CircularTag, CircularReferenceTag)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun wavyProgressIndicators_nanAfterMove_keepAnimating() {
        rule.mainClock.autoAdvance = false
        val moved = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            val indicators = remember {
                movableContentOf {
                    LinearWavyProgressIndicator(
                        progress = { Float.NaN },
                        modifier = Modifier.testTag(LinearTag),
                        waveSpeed = 0.dp,
                    )
                    CircularWavyProgressIndicator(
                        progress = { Float.NaN },
                        modifier = Modifier.testTag(CircularTag),
                        waveSpeed = 0.dp,
                    )
                }
            }
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
                Column { if (!moved.value) indicators() }
                Column { if (moved.value) indicators() }
            }
        }

        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { moved.value = true }
        rule.mainClock.advanceTimeBy(300)
        val linear = rule.onNodeWithTag(LinearTag).captureToImage()
        val circular = rule.onNodeWithTag(CircularTag).captureToImage()
        rule.mainClock.advanceTimeBy(300)

        assertFalse(rule.onNodeWithTag(LinearTag).captureToImage().contentEquals(linear))
        assertFalse(rule.onNodeWithTag(CircularTag).captureToImage().contentEquals(circular))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun wavyProgressIndicators_animationsDisabled_holdStillAndShowProgress() {
        motionDurationScale.scaleFactor = 0f
        rule.mainClock.autoAdvance = false
        val progress = mutableStateOf(Float.NaN)
        setIndicatorsWithReferences { progress.value }

        rule.mainClock.advanceTimeBy(100)
        val linear = rule.onNodeWithTag(LinearTag).captureToImage()
        val circular = rule.onNodeWithTag(CircularTag).captureToImage()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(rule.onNodeWithTag(LinearTag).captureToImage().contentEquals(linear))
        assertTrue(rule.onNodeWithTag(CircularTag).captureToImage().contentEquals(circular))

        rule.runOnIdle { progress.value = 0.5f }
        rule.mainClock.advanceTimeBy(100)
        assertSameDrawings(LinearTag, LinearReferenceTag)
        assertSameDrawings(CircularTag, CircularReferenceTag)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun wavyProgressIndicators_nanToProgress_settleWithAutoAdvance() {
        val progress = mutableStateOf(Float.NaN)
        setIndicatorsWithReferences { progress.value }

        rule.runOnIdle { progress.value = 0.5f }

        assertSameDrawings(LinearTag, LinearReferenceTag)
        assertSameDrawings(CircularTag, CircularReferenceTag)
    }

    private fun setIndicatorsWithReferences(progress: () -> Float) {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(48.dp)) {
                LinearWavyProgressIndicator(
                    progress = progress,
                    modifier = Modifier.testTag(LinearTag),
                    waveSpeed = 0.dp,
                )
                LinearWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(LinearReferenceTag),
                    waveSpeed = 0.dp,
                )
                CircularWavyProgressIndicator(
                    progress = progress,
                    modifier = Modifier.testTag(CircularTag),
                    waveSpeed = 0.dp,
                )
                CircularWavyProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.testTag(CircularReferenceTag),
                    waveSpeed = 0.dp,
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertSameDrawings(tag: String, otherTag: String) {
        assertTrue(
            "Expected the drawings to be the same",
            rule
                .onNodeWithTag(tag)
                .captureToImage()
                .contentEquals(rule.onNodeWithTag(otherTag).captureToImage()),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertDifferentDrawings(tag: String, otherTag: String) {
        assertFalse(
            "Expected the drawings to be different",
            rule
                .onNodeWithTag(tag)
                .captureToImage()
                .contentEquals(rule.onNodeWithTag(otherTag).captureToImage()),
        )
    }

    private fun ImageBitmap.contentEquals(other: ImageBitmap): Boolean {
        if (width != other.width || height != other.height) return false
        return toPixelMap().buffer.contentEquals(other.toPixelMap().buffer)
    }
}

private const val LinearTag = "linear"
private const val LinearReferenceTag = "linearReference"
private const val CircularTag = "circular"
private const val CircularReferenceTag = "circularReference"
