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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlin.math.max

class PlaceholderTest {
    @get:Rule
    val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_initially_show_content() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState {
                contentReady.value
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

        contentReady.value = false

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
            PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.ShowPlaceholder)

        // Change contentReady and confirm that state is now WipeOff
        contentReady = true
        placeholderState.advanceFrameMillisAndCheckState(
            0L,
            PlaceholderStage.WipeOff
        )

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
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
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState {
                contentReady.value
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

        contentReady.value = true

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
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState {
                contentReady.value
            }
            expectedBackgroundColor = MaterialTheme.colors.surface

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

        placeholderState.moveToStartOfNextAnimationLoop(PlaceholderStage.ShowPlaceholder)

        // Move the start of the next placeholder shimmer animation loop and them advance the
        // clock to show the shimmer.
        placeholderState.advanceFrameMillisAndCheckState(
                (PLACEHOLDER_SHIMMER_DURATION_MS * 0.5f).toLong(),
            PlaceholderStage.ShowPlaceholder
        )

        // The placeholder shimmer effect is faint and largely transparent gradiant, but it should
        // reduce the amount of the normal color.
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertDoesNotContainColor(expectedBackgroundColor)

        // Prepare to start to wipe off and show contents.
        contentReady.value = true

        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.WipeOff)

        // Check the background color is correct
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedBackgroundColor, 80f
            )

        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.ShowContent)

        // Check that the shimmer is no longer visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedBackgroundColor, 80f
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun wipeoff_takes_background_offset_into_account() {
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        var expectedBackgroundColor = Color.Transparent
        var expectedBackgroundPlaceholderColor: Color = Color.Transparent
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState {
                contentReady.value
            }
            val maxScreenDimensionPx = with(LocalDensity.current) {
                Dp(max(screenHeightDp(), screenWidthDp()).toFloat()).toPx()
            }
            // Set the offset to be 50% of the screen
            placeholderState.backgroundOffset =
                Offset(maxScreenDimensionPx / 2f, maxScreenDimensionPx / 2f)
            expectedBackgroundColor = MaterialTheme.colors.primary
            expectedBackgroundPlaceholderColor = MaterialTheme.colors.surface

            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .fillMaxWidth(),
                content = {},
                onClick = {},
                colors = PlaceholderDefaults.placeholderChipColors(
                    originalChipColors = ChipDefaults.primaryChipColors(),
                    placeholderState = placeholderState,
                ),
                border = ChipDefaults.chipBorder()
            )
        }

        placeholderState.initializeTestFrameMillis()

        // Check the background color is correct
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor, 80f)
        // Check that there is primary color showing
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertDoesNotContainColor(
                expectedBackgroundColor
            )

        // Prepare to start to wipe off and show contents.
        contentReady.value = true

        placeholderState
            .advanceToNextPlaceholderAnimationLoopAndCheckStage(PlaceholderStage.WipeOff)

        // Check that placeholder background is still visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor, 80f)

        // Move forward by 25% of the wipe-off and confirm that no wipe-off has happened yet due
        // to our offset
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS / 4,
            PlaceholderStage.WipeOff
        )

        // Check that placeholder background is still visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(expectedBackgroundPlaceholderColor, 80f)

        // Now move the end of the wipe-off and confirm that the proper chip background is visible
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.ShowContent
        )

        // Check that normal chip background is now visible
        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(expectedBackgroundColor, 80f)
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    fun TestPlaceholderChip(contents: String?, currentState: StableRef<PlaceholderState?>) {
        val placeholderState = rememberPlaceholderState {
            contents != null
        }.also { currentState.value = it }
        Chip(
            modifier = Modifier
                .testTag("test-item")
                .placeholderShimmer(placeholderState),
            content = {},
            onClick = {},
            colors = PlaceholderDefaults.placeholderChipColors(
                originalChipColors = ChipDefaults.primaryChipColors(),
                placeholderState = placeholderState,
            ),
            border = ChipDefaults.chipBorder(),
        )
        LaunchedEffect(placeholderState) {
            placeholderState.startPlaceholderAnimation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_lambda_update_works() {
        val placeholderState = StableRef<PlaceholderState?>(null)
        val contentsHolder = StableRef<MutableState<String?>>(mutableStateOf(null))
        rule.setContentWithTheme {
            val contents: MutableState<String?> = remember { mutableStateOf(null) }
            contentsHolder.value = contents
            TestPlaceholderChip(contents = contents.value, placeholderState)
        }

        rule.waitForIdle()

        placeholderState.value?.initializeTestFrameMillis()

        assertThat(placeholderState.value).isNotNull()
        assertThat(placeholderState.value?.placeholderStage)
            .isEqualTo(PlaceholderStage.ShowPlaceholder)

        contentsHolder.value.value = "Test"

        // Trigger move to WipeOff stage
        placeholderState.value?.advanceFrameMillisAndCheckState(
            1, PlaceholderStage.WipeOff)

        placeholderState.value?.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS, PlaceholderStage.ShowContent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_background_is_correct_color() {
        var expectedPlaceholderBackgroundColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        lateinit var contentReady: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            contentReady = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState {
                contentReady.value
            }
            expectedPlaceholderBackgroundColor = MaterialTheme.colors.surface
            expectedBackgroundColor = MaterialTheme.colors.primary
            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .placeholderShimmer(placeholderState),
                content = {},
                onClick = {},
                colors = PlaceholderDefaults.placeholderChipColors(
                    originalChipColors = ChipDefaults.primaryChipColors(),
                    placeholderState = placeholderState,
                ),
                border = ChipDefaults.chipBorder(),
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

        contentReady.value = true

        // Trigger move to WipeOff stage
        placeholderState.advanceFrameMillisAndCheckState(
            1, PlaceholderStage.WipeOff)

        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS, PlaceholderStage.ShowContent)

        // Check the placeholder background has gone and that we can see the chips background
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
        frameMillis.value += PLACEHOLDER_SHIMMER_DURATION_MS
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
            (frameMillis.longValue.div(PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS) + 1) *
                PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS
        frameMillis.longValue = animationLoopStart
        rule.waitForIdle()
        assertThat(placeholderStage).isEqualTo(expectedPlaceholderStage)
    }
}