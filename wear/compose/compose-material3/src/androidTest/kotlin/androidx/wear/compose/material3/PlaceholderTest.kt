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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import org.junit.Rule
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.O)
class PlaceholderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun placeholder_initially_shows_content_when_content_ready_true() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState { contentReady.value }
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis(PlaceholderStage.HidePlaceholder)

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.HidePlaceholder
        )
    }

    @Test
    fun placeholder_initially_shows_placeholder_transitions_correctly() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState { contentReady.value }
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.ShowPlaceholder
        )

        // Change contentReady and confirm that state is now WipeOff
        contentReady.value = true
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )
    }

    @Test
    fun placeholder_resets_content_after_show_content_when_content_ready_false() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState { contentReady.value }
            Button(
                modifier = Modifier.fillMaxWidth(),
                content = {},
                onClick = {},
                colors = ButtonDefaults.filledTonalButtonColors()
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis(PlaceholderStage.HidePlaceholder)

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.HidePlaceholder
        )

        contentReady.value = false

        // Check that the state is set to ResetContent
        placeholderState.advanceFrameMillisAndCheckState(
            (PLACEHOLDER_RESET_ANIMATION_DURATION_MS * 0.5f).toLong(),
            PlaceholderStage.ResetContent
        )
    }

    @Test
    fun default_placeholder_sets_correct_colors() {
        placeholder_sets_correct_colors(null)
    }

    @Test
    fun custom_placeholder_sets_correct_colors() {
        placeholder_sets_correct_colors(Color.Blue)
    }

    private fun placeholder_sets_correct_colors(placeholderColor: Color?) {
        var expectedPlaceholderColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState { contentReady.value }
            expectedPlaceholderColor =
                placeholderColor
                    ?: MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.1f)
                        .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
            expectedBackgroundColor = MaterialTheme.colorScheme.primary
            Button(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .then(
                            if (placeholderColor != null)
                                Modifier.placeholder(
                                    placeholderState = placeholderState,
                                    color = placeholderColor
                                )
                            else Modifier.placeholder(placeholderState = placeholderState)
                        ),
                content = {},
                onClick = {},
                colors = ButtonDefaults.buttonColors(),
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedPlaceholderColor)

        // Change contentReady and confirm that state is now WipeOff
        contentReady.value = true
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @Test
    fun placeholder_shimmer_visible_during_show_placeholder_only() {
        var expectedBackgroundColor = Color.Transparent
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState { contentReady.value }
            expectedBackgroundColor = MaterialTheme.colorScheme.surfaceContainer

            Button(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .fillMaxWidth()
                        .placeholderShimmer(placeholderState = placeholderState),
                content = {},
                onClick = {},
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
        }

        placeholderState.initializeTestFrameMillis()

        // Check the background color is correct
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)

        placeholderState.moveToStartOfNextAnimationLoop(PlaceholderStage.ShowPlaceholder)

        // Move the start of the next placeholder shimmer animation loop and them advance the
        // clock to show the shimmer.
        placeholderState.advanceFrameMillisAndCheckState(
            (PLACEHOLDER_SHIMMER_DURATION_MS * 0.5f).toLong(),
            PlaceholderStage.ShowPlaceholder
        )

        // The placeholder shimmer effect is faint and largely transparent gradiant, but it should
        // reduce the amount of the normal color.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(expectedBackgroundColor)

        // Change contentReady and confirm that state is now WipeOff
        contentReady.value = true
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Check the background color is correct
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )

        // Check that the shimmer is no longer visible
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @Test
    fun wipeoff_takes_background_offset_into_account() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        var expectedBackgroundColor = Color.Transparent
        var expectedBackgroundPlaceholderColor: Color = Color.Transparent
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState { contentReady.value }
            val maxScreenDimensionPx =
                with(LocalDensity.current) {
                    Dp(max(screenHeightDp(), screenWidthDp()).toFloat()).toPx()
                }
            // Set the offset to be 50% of the screen
            placeholderState.backgroundOffset =
                Offset(maxScreenDimensionPx / 2f, maxScreenDimensionPx / 2f)
            expectedBackgroundColor = MaterialTheme.colorScheme.primary
            expectedBackgroundPlaceholderColor = MaterialTheme.colorScheme.surfaceContainer

            Button(
                modifier = Modifier.testTag(TEST_TAG).fillMaxWidth(),
                content = {},
                onClick = {},
                colors =
                    PlaceholderDefaults.placeholderButtonColors(
                        originalButtonColors = ButtonDefaults.buttonColors(),
                        placeholderState = placeholderState,
                    ),
            )
        }

        placeholderState.initializeTestFrameMillis()

        // Check the background color is correct
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor)
        // Check that there is primary color showing
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(expectedBackgroundColor)

        // Change contentReady and confirm that state is now WipeOff
        contentReady.value = true
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Check that placeholder background is still visible
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor)

        // Move forward by 25% of the wipe-off and confirm that no wipe-off has happened yet due
        // to our offset
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS / 4,
            PlaceholderStage.WipeOff
        )

        // Check that placeholder background is still visible
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor)

        // Now move the end of the wipe-off and confirm that the proper button background is visible
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )

        // Check that normal button background is now visible
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @Composable
    fun TestPlaceholderButton(contents: String?, currentState: StableRef<PlaceholderState?>) {
        val placeholderState =
            rememberPlaceholderState { contents != null }.also { currentState.value = it }
        Button(
            modifier = Modifier.testTag(TEST_TAG).placeholderShimmer(placeholderState),
            content = {},
            onClick = {},
            colors =
                PlaceholderDefaults.placeholderButtonColors(
                    originalButtonColors = ButtonDefaults.buttonColors(),
                    placeholderState = placeholderState,
                ),
        )
        LaunchedEffect(placeholderState) { placeholderState.animatePlaceholder() }
    }

    @Test
    fun placeholder_lambda_updates_correctly() {
        val placeholderState = StableRef<PlaceholderState?>(null)
        val contentsHolder = StableRef<MutableState<String?>>(mutableStateOf(null))
        rule.setContentWithTheme {
            val contents: MutableState<String?> = remember { mutableStateOf(null) }
            contentsHolder.value = contents
            TestPlaceholderButton(contents = contents.value, placeholderState)
        }

        rule.waitForIdle()

        placeholderState.value?.initializeTestFrameMillis()

        assertThat(placeholderState.value).isNotNull()
        assertThat(placeholderState.value?.placeholderStage)
            .isEqualTo(PlaceholderStage.ShowPlaceholder)

        contentsHolder.value.value = "Test"

        // Trigger move to WipeOff stage
        placeholderState.value?.advanceFrameMillisAndCheckState(1, PlaceholderStage.WipeOff)

        placeholderState.value?.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )
    }

    @Test
    fun placeholder_background_has_correct_color() {
        var expectedPlaceholderBackgroundColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState { contentReady.value }
            expectedPlaceholderBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
            expectedBackgroundColor = MaterialTheme.colorScheme.primary
            Button(
                modifier =
                    Modifier.testTag(TEST_TAG).fillMaxWidth().placeholderShimmer(placeholderState),
                content = {},
                onClick = {},
                colors =
                    PlaceholderDefaults.placeholderButtonColors(
                        originalButtonColors = ButtonDefaults.buttonColors(),
                        placeholderState = placeholderState,
                    ),
            )
            LaunchedEffect(placeholderState) { placeholderState.animatePlaceholder() }
        }

        placeholderState.initializeTestFrameMillis()

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedPlaceholderBackgroundColor)

        // Change contentReady and confirm that state is now WipeOff
        contentReady.value = true
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder
        )

        // Check the placeholder background has gone and that we can see the buttons background
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    private fun PlaceholderState.advanceFrameMillisAndCheckState(
        timeToAdd: Long,
        expectedStage: PlaceholderStage
    ) {
        frameMillis.value += timeToAdd
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    private fun PlaceholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
        expectedStage: PlaceholderStage
    ) {
        frameMillis.value += PLACEHOLDER_SHIMMER_DURATION_MS
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    private fun PlaceholderState.initializeTestFrameMillis(
        initialPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ): Long {
        val currentTime = rule.mainClock.currentTime
        frameMillis.value = currentTime
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(initialPlaceholderStage)
        return currentTime
    }

    private fun PlaceholderState.moveToStartOfNextAnimationLoop(
        expectedPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ) {
        val animationLoopStart =
            (frameMillis.longValue.div(PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS) + 1) *
                PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS
        frameMillis.longValue = animationLoopStart
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedPlaceholderStage)
    }
}
