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

package androidx.compose.remote.player.compose.embedded

import android.content.res.Resources
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteEnterTransition
import androidx.compose.remote.creation.compose.modifier.RemoteExitTransition
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.MutableRemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Screenshots of built-in visibility animations in [RcPlayer], captured halfway through a linear
 * transition, for both enter (GONE -> VISIBLE) and exit (VISIBLE -> GONE).
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(Parameterized::class)
class RcPlayerVisibilityAnimationScreenshotTest(private val case: Case) {

    /** A pair of matching enter and exit transitions. */
    class Case(
        private val name: String,
        val enterLabel: String,
        val exitLabel: String,
        val enter: RemoteEnterTransition,
        val exit: RemoteExitTransition,
    ) {
        override fun toString(): String = name
    }

    private val density = Resources.getSystem().displayMetrics.density

    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = ApplicationProvider.getApplicationContext(),
                    size = Size(WIDTH_DP * density, HEIGHT_DP * density),
                ),
        )

    @Test fun exitHalfway() = captureHalfway(initial = Shown.Visible)

    @Test fun enterHalfway() = captureHalfway(initial = Shown.Gone)

    private fun captureHalfway(initial: Shown) {
        val composeRule = screenshotRule.composeTestRule
        composeRule.mainClock.autoAdvance = false
        screenshotRule.setContent { VisibilityAnimationDemo(case, initial) }
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithContentDescription(TOGGLE).performClick()
        // One frame to apply the click and start the transition, then half its duration.
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(DURATION_MS / 2L)

        screenshotRule.verifyScreenshot()
    }

    enum class Shown(val visibility: RemoteInt) {
        Visible(Component.Visibility.VISIBLE.ri),
        Gone(Component.Visibility.GONE.ri),
    }

    @Composable
    @RemoteComposable
    private fun VisibilityAnimationDemo(case: Case, initial: Shown) {
        val shown = remember { MutableRemoteEnum(initial) }
        val headerLabel = if (initial == Shown.Gone) case.enterLabel else case.exitLabel
        RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
            // Tapping toggles between Visible (ordinal 0) and Gone (ordinal 1).
            RemoteBox(
                modifier =
                    RemoteModifier.size(WIDTH_DP.rdp, 24.rdp)
                        .background(Color.DarkGray.rc)
                        .clickable(
                            valueChange(
                                remoteState = shown.remoteInt,
                                updatedValue = (shown.remoteInt + 1) % 2,
                            )
                        )
                        .semantics { contentDescription = TOGGLE.rs },
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(headerLabel.rs, color = Color.White.rc, fontSize = 11.rsp)
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                // Yellow backdrop stripe shows through when alpha < 1 (e.g. Fade @ 50%).
                RemoteBox(
                    modifier = RemoteModifier.size(160.rdp, 20.rdp).background(Color.Yellow.rc)
                )
                // Target outline marks the resting 96x48 bounds of the animated card.
                RemoteBox(
                    modifier = RemoteModifier.size(96.rdp, 48.rdp).border(1.rdp, Color.Black.rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(96.rdp, 48.rdp)
                            .background(Color.Blue.rc)
                            .visibility(shown.toRemoteInt { it.visibility })
                            .animationSpec(
                                // No motion (expand/shrink), so only the visual transition shows.
                                motionSpec = remoteTween(0, RemoteEasing.Linear),
                                visibilitySpec = remoteTween(DURATION_MS, RemoteEasing.Linear),
                                enter = case.enter,
                                exit = case.exit,
                            ),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Card".rs, color = Color.White.rc, fontSize = 14.rsp)
                }
            }
        }
    }

    companion object {
        private const val WIDTH_DP = 200f
        private const val HEIGHT_DP = 144f
        private const val DURATION_MS = 400
        private const val TOGGLE = "Toggle"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Case> =
            listOf(
                Case(
                    "Fade",
                    "FadeIn @ 50%",
                    "FadeOut @ 50%",
                    RemoteEnterTransition.FadeIn,
                    RemoteExitTransition.FadeOut,
                ),
                Case(
                    "SlideLeft",
                    "SlideInLeft @ 50%",
                    "SlideOutLeft @ 50%",
                    RemoteEnterTransition.SlideInLeft,
                    RemoteExitTransition.SlideOutLeft,
                ),
                Case(
                    "SlideBottom",
                    "SlideInBottom @ 50%",
                    "SlideOutBottom @ 50%",
                    RemoteEnterTransition.SlideInBottom,
                    RemoteExitTransition.SlideOutBottom,
                ),
                Case(
                    "Rotate",
                    "Rotate In @ 50%",
                    "Rotate Out @ 50%",
                    RemoteEnterTransition.Rotate,
                    RemoteExitTransition.Rotate,
                ),
            )
    }
}
