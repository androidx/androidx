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

package androidx.wear.compose.material

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class PlaceholderTest {
    @get:Rule
    val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_initially_show_content() {
        var contentReady = true
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .fillMaxWidth(),
                content = {},
                onClick = {},
                colors = ChipDefaults.secondaryChipColors(),
                border = ChipDefaults.chipBorder()
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis(PlaceholderStage.ShowContent)

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.ShowContent
        )

        contentReady = false

        // Check that the state does not go to ShowPlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.ShowContent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_initially_show_placeholder_transitions_correctly() {
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .fillMaxWidth(),
                content = {},
                onClick = {},
                colors = ChipDefaults.secondaryChipColors(),
                border = ChipDefaults.chipBorder()
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.ShowPlaceholder)

        // Change contentReady and confirm that state is still ShowPlaceholder
        contentReady = true
        placeholderState.advanceFrameMillisAndCheckState(
            0L,
            PlaceholderStage.ShowPlaceholder
        )

        // Advance the clock by one cycle and check we have moved to WipeOff
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.WipeOff
        )

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.ShowContent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun default_placeholder_is_correct_color() {
        placeholder_is_correct_color(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun custom_placeholder_is_correct_color() {
        placeholder_is_correct_color(Color.Blue)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    fun placeholder_is_correct_color(placeholderColor: Color?) {
        var expectedPlaceholderColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            expectedPlaceholderColor =
                placeholderColor
                    ?: MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                        .compositeOver(MaterialTheme.colors.surface)
            expectedBackgroundColor = MaterialTheme.colors.primary
            Chip(
                modifier = Modifier
                    .testTag("test-item")
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
                colors = ChipDefaults.primaryChipColors(),
                border = ChipDefaults.chipBorder()
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedPlaceholderColor
            )

        contentReady = true

        // Advance the clock to the next placeholder animation loop and check for wipe-off mode
        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.WipeOff)

        // Advance the clock to the next placeholder animation loop and check for show content mode
        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.ShowContent)

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedBackgroundColor
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_shimmer_visible_during_showplaceholder_only() {
        var expectedBackgroundColor = Color.Transparent
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        var expectedShimmerColor = Color.Transparent
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            expectedBackgroundColor = MaterialTheme.colors.surface
            expectedShimmerColor = MaterialTheme.colors.onSurface.copy(0.13f)
                .compositeOver(expectedBackgroundColor)
            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .fillMaxWidth()
                    .placeholderShimmer(placeholderState = placeholderState),
                content = {},
                onClick = {},
                colors = ChipDefaults.secondaryChipColors(),
                border = ChipDefaults.chipBorder()
            )
        }

        placeholderState.initializeTestFrameMillis()

        // Check the background color is correct
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedBackgroundColor, 80f
            )
        // Check that there is no shimmer color
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertDoesNotContainColor(
                expectedShimmerColor
            )

        // Move the start of the next placeholder animation loop and them advance the clock 200
        // milliseconds (PLACEHOLDER_PROGRESSION_DURATION_MS / 4) to show the shimmer.
        //
        // We choose (PLACEHOLDER_PROGRESSION_DURATION_MS / 4) as this should put the center of the
        // shimmer gradiant at the top left (0,0) of the screen and as we have placed the component
        // at the top of the screen this should ensure we have some shimmer gradiant cast over the
        // component regardless of the screen size/shape. So should work for round, square or
        // rectangular screens.
        placeholderState.moveToStartOfNextAnimationLoop()
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_PROGRESSION_DURATION_MS / 4,
            PlaceholderStage.ShowPlaceholder
        )

        // The placeholder shimmer effect is faint and largely transparent gradiant, so we are
        // looking for a very small amount to be visible.
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedShimmerColor, 1f
            )

        // Prepare to start to wipe off and show contents.
        contentReady = true

        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.WipeOff)

        // Check that the shimmer is no longer visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertDoesNotContainColor(
                expectedShimmerColor
            )

        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.ShowContent)

        // Check that the shimmer is no longer visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertDoesNotContainColor(
                expectedShimmerColor
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_background_is_correct_color() {
        var expectedPlaceholderBackgroundColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            expectedPlaceholderBackgroundColor = MaterialTheme.colors.surface
            expectedBackgroundColor = MaterialTheme.colors.primary
            Chip(
                modifier = Modifier
                    .testTag("test-item"),
                content = {},
                onClick = {},
                colors = PlaceholderDefaults.placeholderChipColors(
                    originalChipColors = ChipDefaults.primaryChipColors(),
                    placeholderState = placeholderState,
                ),
                border = ChipDefaults.chipBorder()
            )
            LaunchedEffect(placeholderState) {
                placeholderState.startPlaceholderAnimation()
            }
        }

        placeholderState.initializeTestFrameMillis()

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedPlaceholderBackgroundColor
            )

        contentReady = true

        // Advance the clock to the next placeholder animation loop to move into wipe-off mode
        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.WipeOff)

        // Advance the clock to the next placeholder animation loop to move into show content mode
        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.ShowContent)

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedBackgroundColor
            )
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun PlaceholderState.advanceFrameMillisAndCheckState(
        timeToAdd: Long,
        expectedStage: PlaceholderStage
    ) {
        frameMillis.value += timeToAdd
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun PlaceholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
        expectedStage: PlaceholderStage
    ) {
        frameMillis.value += PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun PlaceholderState.initializeTestFrameMillis(
        initialPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ): Long {
        val currentTime = rule.mainClock.currentTime
        frameMillis.value = currentTime
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(initialPlaceholderStage)
        return currentTime
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    private fun PlaceholderState.moveToStartOfNextAnimationLoop(
        expectedPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ) {
        val animationLoopStart =
            (frameMillis.value.div(PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS) + 1) *
            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS
        frameMillis.value = animationLoopStart
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedPlaceholderStage)
    }
}