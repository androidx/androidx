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

import android.content.Context
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.test.utils.GoldenScreenshotNameTestRule
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the embedded player ([RcPlayer]) after click, long click and scroll
 * interactions.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerInteractionScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val sizePx = 100f * context.resources.displayMetrics.density

    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo = createCreationDisplayInfo(context, Size(sizePx, sizePx)),
        )

    @get:Rule val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    private val composeTestRule
        get() = remoteComposeTestRule.composeTestRule

    @Test
    fun clickAndScrollInteractions() {
        remoteComposeTestRule.setContent(profile = TEST_PROFILE) {
            val showBlue = remember { MutableRemoteInt(Component.Visibility.GONE) }
            val showGreen = remember { MutableRemoteInt(Component.Visibility.GONE) }
            val scrollState = remember { RemoteScrollState() }

            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                // Red until clicked (blue) or long clicked (green).
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "ColorToggle".rs }
                            .combinedClickable(
                                onClick = valueChange(showBlue, Component.Visibility.VISIBLE.ri),
                                onLongClick =
                                    valueChange(showGreen, Component.Visibility.VISIBLE.ri),
                            )
                ) {
                    LabeledBox("Not clicked", Color.Red, Color.White)
                    LabeledBox(
                        "Clicked",
                        Color.Blue,
                        Color.White,
                        RemoteModifier.visibility(showBlue),
                    )
                    LabeledBox(
                        "Long clicked",
                        Color.Green,
                        Color.Black,
                        RemoteModifier.visibility(showGreen),
                    )
                }

                // A 50dp viewport scrolling from yellow to magenta.
                RemoteColumn(
                    modifier =
                        RemoteModifier.size(100.rdp, 50.rdp)
                            .clip()
                            .verticalScroll(scrollState)
                            .semantics { contentDescription = "ScrollBox".rs }
                ) {
                    LabeledBox("Scroll top", Color.Yellow, Color.Black)
                    LabeledBox("Scroll bottom", Color.Magenta, Color.White)
                }
            }
        }
        composeTestRule.waitForIdle()
        verifyScreenshot("initial")

        composeTestRule.onNodeWithContentDescription("ColorToggle").performClick()
        composeTestRule.waitForIdle()
        verifyScreenshot("afterClick")

        composeTestRule.onNodeWithContentDescription("ColorToggle").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()
        verifyScreenshot("afterLongClick")

        composeTestRule.onNodeWithContentDescription("ScrollBox").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        verifyScreenshot("afterSwipeUp")

        composeTestRule.onNodeWithContentDescription("ScrollBox").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        verifyScreenshot("afterSwipeDown")
    }

    /** A 100x50dp [color] region with a centered [label] describing the state it represents. */
    @Composable
    @RemoteComposable
    private fun LabeledBox(
        label: String,
        color: Color,
        textColor: Color,
        modifier: RemoteModifier = RemoteModifier,
    ) {
        RemoteBox(
            modifier = modifier.size(100.rdp, 50.rdp).background(color.rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(label.rs, color = textColor.rc)
        }
    }

    private fun verifyScreenshot(suffix: String) {
        remoteComposeTestRule.verifyScreenshot(
            goldenScreenshotNameTestRule.getGoldenScreenshotName(suffix)
        )
    }
}
