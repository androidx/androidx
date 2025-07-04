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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PlaceholderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun placeholder_initially_shows_when_lambda_true() {
        lateinit var visible: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            visible = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis(PlaceholderStage.HidePlaceholder)

        // Advance placeholder clock without changing the content ready and confirm still in
        // HidePlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.HidePlaceholder
        )
    }

    @Test
    fun placeholder_initially_shows_placeholder_transitions_correctly() {
        lateinit var visible: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            visible = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
            Box(Modifier.placeholderShimmer(placeholderState))
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS,
            PlaceholderStage.ShowPlaceholder,
        )

        // Change visible and confirm that state is now WipeOff
        visible.value = false
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder,
        )
    }

    @Test
    fun placeholder_resets_content_after_show_content_when_content_ready_false() {
        lateinit var visible: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            visible = remember { mutableStateOf(false) }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
            Button(
                modifier = Modifier.fillMaxWidth().placeholderShimmer(placeholderState),
                content = {},
                onClick = {},
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
        }

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis(PlaceholderStage.HidePlaceholder)

        // Advance placeholder clock without changing the content ready and confirm still in
        // ShowPlaceholder
        placeholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
            PlaceholderStage.HidePlaceholder
        )

        visible.value = true

        // Check that the state is set to ResetContent
        placeholderState.advanceFrameMillisAndCheckState(
            (PLACEHOLDER_RESET_ANIMATION_DURATION_MS * 0.5f).toLong(),
            PlaceholderStage.ResetContent,
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
        val placeholderState = PlaceholderState(isVisible = true)
        rule.setContentWithTheme {
            expectedPlaceholderColor = placeholderColor ?: PlaceholderDefaults.color
            expectedBackgroundColor = MaterialTheme.colorScheme.primary
            Button(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .placeholderShimmer(placeholderState)
                        .then(
                            if (placeholderColor != null)
                                Modifier.placeholder(
                                    placeholderState = placeholderState,
                                    color = placeholderColor,
                                )
                            else Modifier.placeholder(placeholderState = placeholderState)
                        ),
                content = {},
                onClick = {},
                colors = ButtonDefaults.buttonColors(),
            )
        }

        rule.waitForIdle()

        // For testing we need to manually manage the frame clock for the placeholder animation
        placeholderState.initializeTestFrameMillis()

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedPlaceholderColor)

        // Change placeholderState.visible and confirm that state is now WipeOff
        placeholderState.isVisible = false
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Advance the clock by one cycle and check we have moved to HidePlaceholder
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder,
        )

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @Test
    fun placeholder_shimmer_visible_during_show_placeholder_only() {
        var expectedBackgroundColor = Color.Transparent
        lateinit var visible: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            visible = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
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
            PlaceholderStage.ShowPlaceholder,
        )

        // The placeholder shimmer effect is faint and largely transparent gradiant, but it should
        // reduce the amount of the normal color.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(expectedBackgroundColor)

        // Change visible and confirm that state is now WipeOff
        visible.value = false
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        // Check the background color is correct
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)

        // Advance the clock by one cycle and check we have moved to ShowContent
        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder,
        )

        // Check that the shimmer is no longer visible
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @Composable
    fun TestPlaceholderButton(contents: String?, currentState: StableRef<PlaceholderState?>) {
        val placeholderState =
            rememberPlaceholderState(isVisible = contents == null).also { currentState.value = it }
        Button(
            modifier = Modifier.testTag(TEST_TAG).placeholderShimmer(placeholderState),
            content = {},
            onClick = {},
        )
    }

    @Test
    fun placeholder_lambda_updates_correctly() {
        val placeholderState = StableRef<PlaceholderState?>(null)
        val contents = mutableStateOf<String?>(null)
        rule.setContentWithTheme {
            TestPlaceholderButton(contents = contents.value, placeholderState)
        }

        rule.waitForIdle()

        placeholderState.value?.initializeTestFrameMillis()

        assertThat(placeholderState.value).isNotNull()
        assertThat(placeholderState.value?.placeholderStage)
            .isEqualTo(PlaceholderStage.ShowPlaceholder)

        contents.value = "Test"

        // Trigger move to WipeOff stage
        placeholderState.value?.advanceFrameMillisAndCheckState(1, PlaceholderStage.WipeOff)

        placeholderState.value?.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder,
        )
    }

    @Test
    fun placeholder_state_with_no_modifier_does_not_animate() {
        val visible = mutableStateOf(true)
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            // Make sure the AnimationCoordinator is running, to ensure it doesn't interfere.
            DisposableEffect(Unit) {
                AnimationCoordinator.register()
                onDispose { AnimationCoordinator.unregister() }
            }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
        }

        placeholderState.initializeTestFrameMillis()

        rule.runOnIdle { visible.value = false }

        rule.waitForIdle()

        // No Modifiers using the state, transition in instant.
        assertThat(placeholderState.placeholderStage).isEqualTo(PlaceholderStage.HidePlaceholder)
    }

    @Ignore("New implementation for Material3") // TODO (b/399615860)
    @Test
    fun placeholder_background_has_correct_color() {
        var expectedPlaceholderBackgroundColor = Color.Transparent
        var expectedBackgroundColor = Color.Transparent
        lateinit var visible: MutableState<Boolean>
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            visible = remember { mutableStateOf(true) }
            placeholderState = rememberPlaceholderState(isVisible = visible.value)
            expectedPlaceholderBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
            expectedBackgroundColor = MaterialTheme.colorScheme.primary
            Button(
                modifier =
                    Modifier.testTag(TEST_TAG).fillMaxWidth().placeholderShimmer(placeholderState),
                content = {},
                onClick = {},
            )
        }

        placeholderState.initializeTestFrameMillis()

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedPlaceholderBackgroundColor)

        // Change visible and confirm that state is now WipeOff
        visible.value = false
        placeholderState.advanceFrameMillisAndCheckState(1L, PlaceholderStage.WipeOff)

        placeholderState.advanceFrameMillisAndCheckState(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS,
            PlaceholderStage.HidePlaceholder,
        )

        // Check the placeholder background has gone and that we can see the buttons background
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    private fun PlaceholderState.advanceFrameMillisAndCheckState(
        timeToAdd: Long,
        expectedStage: PlaceholderStage,
    ) {
        updateFrameMillis(AnimationCoordinator.frameMillis.longValue + timeToAdd)
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    private fun PlaceholderState.advanceToNextPlaceholderAnimationLoopAndCheckStage(
        expectedStage: PlaceholderStage
    ) {
        updateFrameMillis(
            AnimationCoordinator.frameMillis.longValue + PLACEHOLDER_SHIMMER_DURATION_MS
        )
        assertThat(placeholderStage).isEqualTo(expectedStage)
    }

    private fun PlaceholderState.initializeTestFrameMillis(
        initialPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ): Long {
        val currentTime = rule.mainClock.currentTime
        updateFrameMillis(currentTime)
        assertThat(placeholderStage).isEqualTo(initialPlaceholderStage)
        return currentTime
    }

    private fun PlaceholderState.moveToStartOfNextAnimationLoop(
        expectedPlaceholderStage: PlaceholderStage = PlaceholderStage.ShowPlaceholder
    ) {
        val animationLoopStart =
            (AnimationCoordinator.frameMillis.longValue.div(
                PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS
            ) + 1) * PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS
        updateFrameMillis(animationLoopStart)
        assertThat(placeholderStage).isEqualTo(expectedPlaceholderStage)
    }

    private fun PlaceholderState.updateFrameMillis(time: Long) {
        // We manually update the frame clock for testing, since withInfiniteAnimationFrameMillis
        // doesn't work in tests.
        AnimationCoordinator.frameMillis.longValue = time
        rule.waitForIdle()
    }

    // These were part of Placeholder, they are not needed anymore but we leave it here to keep the
    // tests and verify we don't break anything.
    private val PlaceholderState.placeholderStage: PlaceholderStage
        get() =
            if (isVisible) {
                if (isAnimationRunning) PlaceholderStage.ResetContent
                else PlaceholderStage.ShowPlaceholder
            } else {
                if (isAnimationRunning) PlaceholderStage.WipeOff
                else PlaceholderStage.HidePlaceholder
            }

    @JvmInline
    /** Enumerate the possible stages (states) that a placeholder can be in. */
    internal value class PlaceholderStage internal constructor(internal val type: Int) {
        companion object {
            /** Show placeholders and placeholder effects. Use when waiting for content to load. */
            val ShowPlaceholder: PlaceholderStage = PlaceholderStage(0)

            /**
             * Wipe off placeholder effects. Used to animate the wiping away of placeholders and
             * revealing the content underneath. Enter this stage from [ShowPlaceholder] when the
             * next animation loop is started and the content is ready.
             */
            val WipeOff: PlaceholderStage = PlaceholderStage(1)

            /**
             * Indicates that placeholders no longer to be shown. Enter this stage from [WipeOff] in
             * the loop after the wipe-off animation.
             */
            val HidePlaceholder: PlaceholderStage = PlaceholderStage(2)

            /**
             * Resets the component to remove the content and reinstate the placeholders so that new
             * content can be loaded. Enter this stage from [HidePlaceholder] and exit to
             * [ShowPlaceholder].
             */
            val ResetContent: PlaceholderStage = PlaceholderStage(3)
        }

        override fun toString(): String {
            return when (this) {
                ShowPlaceholder -> "PlaceholderStage.ShowPlaceholder"
                WipeOff -> "PlaceholderStage.WipeOff"
                ResetContent -> "PlaceholderStage.ResetContent"
                else -> "PlaceholderStage.HidePlaceholder"
            }
        }
    }
}
