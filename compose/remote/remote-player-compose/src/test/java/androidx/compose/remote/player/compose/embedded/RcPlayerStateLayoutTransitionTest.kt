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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteEnterTransition
import androidx.compose.remote.creation.compose.modifier.RemoteExitTransition
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animateEnterExit
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.sharedElement
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Per-component enter/exit transitions and shared-element pairing inside state layouts played by
 * the embedded player ([RcPlayer]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerStateLayoutTransitionTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val linear600 = remoteTween(durationMillis = 600, easing = RemoteEasing.Linear)

    /** Plays a toggle box followed by a two-state layout whose branches come from [branch]. */
    private fun setToggledStateContent(branch: @Composable (Boolean) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteColumn {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "toggle".rs }
                            .size(20.rdp)
                            .clickable(onClick = valueChangeAction(checked, !checked))
                )
                RemoteStateLayout(currentState = checked) { state -> branch(state) }
            }
        }
        rule.mainClock.advanceTimeBy(0)
    }

    private fun toggle() {
        rule.onNodeWithContentDescription("toggle").performClick()
        rule.mainClock.advanceTimeByFrame()
    }

    private fun left(description: String): Float =
        rule.onNodeWithContentDescription(description).getUnclippedBoundsInRoot().left.value

    private fun top(description: String): Float =
        rule.onNodeWithContentDescription(description).getUnclippedBoundsInRoot().top.value

    /**
     * How much of the pixel at ([xFraction], [yFraction]) of the node with [description] is covered
     * by its saturated colour, whether drawn over a transparent or a white background.
     */
    private fun coverage(
        description: String,
        xFraction: Float = 0.5f,
        yFraction: Float = 0.5f,
    ): Float {
        val bitmap =
            rule.onNodeWithContentDescription(description).captureToImage().asAndroidBitmap()
        val px =
            bitmap.getPixel((bitmap.width * xFraction).toInt(), (bitmap.height * yFraction).toInt())
        val minChannel = minOf(AndroidColor.red(px), AndroidColor.green(px), AndroidColor.blue(px))
        return AndroidColor.alpha(px) / 255f * (1f - minChannel / 255f)
    }

    /** A spec with no motion whose enter/exit slides over 400ms of visibility duration. */
    private val visibilityOnlySlide =
        RemoteModifier.animationSpec(
            motionSpec = remoteTween(durationMillis = 0, easing = RemoteEasing.Linear),
            visibilitySpec = remoteTween(durationMillis = 400, easing = RemoteEasing.Linear),
            enter = RemoteEnterTransition.SlideInBottom,
            exit = RemoteExitTransition.SlideOutBottom,
        )

    @Test
    fun nestedChildSlideEnter_slidesInsteadOfFadingInPlace() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "slider".rs }
                                .animateEnterExit(
                                    enter = RemoteEnterTransition.SlideInLeft,
                                    exit = RemoteExitTransition.FadeOut,
                                    spec = linear600,
                                )
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            }
        }

        toggle()
        rule.mainClock.advanceTimeBy(300)
        val midLeft = left("slider")

        rule.mainClock.advanceTimeBy(500)
        val settledLeft = left("slider")

        // Halfway through SlideInLeft the 40dp child is still offset by half its width.
        assertThat(settledLeft - midLeft).isWithin(5f).of(20f)
    }

    @Test
    fun nestedChildSlideExit_slidesOut() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "leaver".rs }
                                .animateEnterExit(
                                    enter = RemoteEnterTransition.FadeIn,
                                    exit = RemoteExitTransition.SlideOutRight,
                                    spec = linear600,
                                )
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            }
        }
        val startLeft = left("leaver")

        toggle()
        rule.mainClock.advanceTimeBy(300)

        // Halfway through SlideOutRight the 40dp child has moved half its width to the right.
        assertThat(left("leaver") - startLeft).isWithin(5f).of(20f)

        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithContentDescription("leaver").assertDoesNotExist()
    }

    @Test
    fun paneWithUnpairedAnimationId_slidesIn() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "pane0".rs }
                            .animateEnterExit(spec = linear600)
                            .size(50.rdp)
                            .background(Color.Red.rc)
                )
            } else {
                // The animation id has no partner in the other branch, so this pane runs its own
                // enter transition rather than a shared-bounds transition.
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "pane1".rs }
                            .animateEnterExit(
                                enter = RemoteEnterTransition.SlideInLeft,
                                exit = RemoteExitTransition.SlideOutRight,
                                spec = linear600,
                                animationId = 1001,
                            )
                            .size(50.rdp)
                            .background(Color.Blue.rc)
                )
            }
        }

        toggle()
        rule.mainClock.advanceTimeBy(300)
        val midLeft = left("pane1")

        rule.mainClock.advanceTimeBy(500)
        assertThat(left("pane1") - midLeft).isWithin(5f).of(25f)
    }

    @Test
    fun pairedSharedElementWithSlideSpec_animatesBoundsInsteadOfSliding() {
        setToggledStateContent { state ->
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = if (state) "shared1".rs else "shared0".rs
                        }
                        .sharedElement(
                            key = 5,
                            spec = remoteTween(durationMillis = 1000, easing = RemoteEasing.Linear),
                            enter = RemoteEnterTransition.SlideInLeft,
                            exit = RemoteExitTransition.SlideOutRight,
                        )
                        .size(if (state) 100.rdp else 40.rdp)
                        .background(Color.Red.rc)
            )
        }
        val startLeft = left("shared0")

        toggle()
        rule.mainClock.advanceTimeBy(484)
        val bounds = rule.onNodeWithContentDescription("shared1").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(5f).of(70f)
        // The shared bounds move from the 40dp box's position towards the 100dp box's position
        // without an additional slide offset.
        assertThat(bounds.left.value).isLessThan(startLeft + 1f)

        rule.mainClock.advanceTimeBy(600)
        val settled = rule.onNodeWithContentDescription("shared1").getUnclippedBoundsInRoot()
        assertThat((settled.right - settled.left).value).isWithin(2f).of(100f)
        assertThat(bounds.left.value).isGreaterThan(settled.left.value - 1f)
    }

    @Test
    fun nestedChildRotateEnter_drawsRotated() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(80.rdp))
            } else {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "rotor".rs }
                            .animateEnterExit(
                                enter = RemoteEnterTransition.Rotate,
                                exit = RemoteExitTransition.FadeOut,
                                spec = linear600,
                            )
                            .size(80.rdp)
                ) {
                    // Only the top-start quadrant is red, so a half turn moves it to bottom-end.
                    RemoteBox(modifier = RemoteModifier.size(40.rdp).background(Color.Red.rc))
                }
            }
        }

        fun redness(xFraction: Float, yFraction: Float): Int {
            val bitmap =
                rule.onNodeWithContentDescription("rotor").captureToImage().asAndroidBitmap()
            val px =
                bitmap.getPixel(
                    (bitmap.width * xFraction).toInt(),
                    (bitmap.height * yFraction).toInt(),
                )
            return AndroidColor.red(px) - AndroidColor.green(px)
        }

        toggle()
        // Halfway through the enter the content has turned by half a revolution.
        rule.mainClock.advanceTimeBy(300)
        assertThat(redness(0.75f, 0.75f)).isGreaterThan(30)
        assertThat(redness(0.25f, 0.25f)).isLessThan(10)

        rule.mainClock.advanceTimeBy(500)
        assertThat(redness(0.25f, 0.25f)).isGreaterThan(200)
        assertThat(redness(0.75f, 0.75f)).isLessThan(10)
    }

    @Test
    fun textLeafSharedElement_pairsAcrossStates() {
        setToggledStateContent { state ->
            RemoteText(
                text = if (state) "Shared title expanded".rs else "Shared".rs,
                fontSize = if (state) 20.rsp else 10.rsp,
                modifier =
                    RemoteModifier.sharedElement(
                        key = 42,
                        spec = remoteTween(durationMillis = 1000, easing = RemoteEasing.Linear),
                    ),
            )
        }
        fun width(text: String): Float =
            rule.onNodeWithText(text).getUnclippedBoundsInRoot().let { (it.right - it.left).value }
        val startWidth = width("Shared")

        toggle()
        rule.mainClock.advanceTimeBy(484)
        val midWidth = width("Shared title expanded")

        rule.mainClock.advanceTimeBy(600)
        val endWidth = width("Shared title expanded")

        assertThat(endWidth).isGreaterThan(startWidth + 20f)
        assertThat(midWidth).isWithin(5f).of((startWidth + endWidth) / 2f)
    }

    @Test
    fun visibilityOnlyEnter_slidesWhenMotionDurationIsZero() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "slider".rs }
                                .then(visibilityOnlySlide)
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            }
        }

        toggle()
        rule.mainClock.advanceTimeBy(200)
        val midTop = top("slider")

        rule.mainClock.advanceTimeBy(400)
        val settledTop = top("slider")

        // Halfway through the 400ms SlideInBottom the 40dp child is still half its height lower.
        assertThat(midTop - settledTop).isWithin(5f).of(20f)
    }

    @Test
    fun visibilityOnlyExit_holdsOldBranchOnlyUntilTheSlideEnds() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "leaver".rs }
                                .then(visibilityOnlySlide)
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            }
        }
        val startTop = top("leaver")

        toggle()
        rule.mainClock.advanceTimeBy(200)
        assertThat(top("leaver") - startTop).isWithin(5f).of(20f)

        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithContentDescription("leaver").assertDoesNotExist()
        // Once the slide has ended the layout does no more per-frame work.
        assertThat(rule.framesWithStateChanges(30)).isEqualTo(0)
    }

    @Test
    fun slowChildExit_isNotCutOffByShortBranchMotion() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "leaver".rs }
                                .animationSpec(
                                    motionSpec =
                                        remoteTween(
                                            durationMillis = 150,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    visibilitySpec =
                                        remoteTween(
                                            durationMillis = 500,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    enter = RemoteEnterTransition.FadeIn,
                                    exit = RemoteExitTransition.SlideOutRight,
                                )
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            }
        }
        val startLeft = left("leaver")

        toggle()
        rule.mainClock.advanceTimeBy(250)
        // The 500ms slide is halfway and still fully visible, after the 150ms motion has ended.
        assertThat(left("leaver") - startLeft).isWithin(5f).of(20f)
        assertThat(coverage("leaver")).isGreaterThan(0.9f)

        rule.mainClock.advanceTimeBy(350)
        rule.onNodeWithContentDescription("leaver").assertDoesNotExist()
    }

    @Test
    fun pureSlideEnter_isNotFadedByBranch() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            } else {
                RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "slider".rs }
                                .animateEnterExit(
                                    enter = RemoteEnterTransition.SlideInRight,
                                    exit = RemoteExitTransition.SlideOutRight,
                                    spec = linear600,
                                )
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                }
            }
        }

        toggle()
        rule.mainClock.advanceTimeBy(300)
        // Halfway through the slide the child is fully opaque rather than half faded.
        assertThat(coverage("slider")).isGreaterThan(0.9f)
    }

    @Test
    fun rotateEnter_fadesOnlyOnce() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(80.rdp))
            } else {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "rotor".rs }
                            .animateEnterExit(
                                enter = RemoteEnterTransition.Rotate,
                                exit = RemoteExitTransition.FadeOut,
                                spec = linear600,
                            )
                            .size(80.rdp)
                            .background(Color.Red.rc)
                )
            }
        }

        toggle()
        // Halfway through the enter the rotation's own fade is at half opacity.
        rule.mainClock.advanceTimeBy(300)
        assertThat(coverage("rotor")).isWithin(0.15f).of(0.5f)

        rule.mainClock.advanceTimeBy(500)
        assertThat(coverage("rotor")).isGreaterThan(0.9f)
    }

    @Test
    fun siblingWithoutSpec_fadesBesideSlidingChild() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
            } else {
                RemoteColumn {
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "slider".rs }
                                .animateEnterExit(
                                    enter = RemoteEnterTransition.SlideInLeft,
                                    exit = RemoteExitTransition.SlideOutRight,
                                    spec = linear600,
                                )
                                .size(40.rdp)
                                .background(Color.Red.rc)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.semantics { contentDescription = "sibling".rs }
                                .size(40.rdp)
                                .background(Color.Blue.rc)
                    )
                }
            }
        }

        toggle()
        rule.mainClock.advanceTimeBy(300)
        assertThat(coverage("sibling")).isWithin(0.2f).of(0.5f)

        rule.mainClock.advanceTimeBy(500)
        assertThat(coverage("sibling")).isGreaterThan(0.9f)
        assertThat(rule.framesWithStateChanges(30)).isEqualTo(0)
    }

    @Test
    fun zeroDurations_switchInstantly() {
        val instant = remoteTween(durationMillis = 0, easing = RemoteEasing.Linear)
        setToggledStateContent { state ->
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = if (state) "after".rs else "before".rs
                        }
                        .animationSpec(
                            motionSpec = instant,
                            visibilitySpec = instant,
                            enter = RemoteEnterTransition.SlideInLeft,
                            exit = RemoteExitTransition.SlideOutRight,
                        )
                        .size(40.rdp)
            )
        }

        toggle()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("before").assertDoesNotExist()
        rule.onNodeWithContentDescription("after").assertExists()
        assertThat(rule.framesWithStateChanges(10)).isEqualTo(0)
    }

    @Test
    fun disabledSpec_switchesInstantly() {
        setToggledStateContent { state ->
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = if (state) "after".rs else "before".rs
                        }
                        .animationSpec(enabled = false)
                        .size(40.rdp)
            )
        }

        toggle()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("before").assertDoesNotExist()
        rule.onNodeWithContentDescription("after").assertExists()
        assertThat(rule.framesWithStateChanges(10)).isEqualTo(0)
    }
}
