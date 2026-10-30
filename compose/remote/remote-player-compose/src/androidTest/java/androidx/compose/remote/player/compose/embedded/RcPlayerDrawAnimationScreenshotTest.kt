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
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot test for animated expressions defined inside `drawWithContent` (`b/574455716`),
 * captured halfway through a linear transition in [RcPlayer].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerDrawAnimationScreenshotTest {

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

    @Test
    fun animatedFloatInDrawWithContent_halfway() {
        val composeRule = screenshotRule.composeTestRule
        composeRule.mainClock.autoAdvance = false
        screenshotRule.setContent {
            val toggled = remember { MutableRemoteBoolean(false) }
            val progress =
                animateRemoteFloatAsState(
                    targetValue = toggled.select(1f.rf, 0f.rf),
                    animationSpec = remoteTween(DURATION_MS, RemoteEasing.Linear),
                )
            val fillPaint = RemotePaint { style = PaintingStyle.Fill }
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .background(Color.LightGray.rc)
                        .clickable(valueChangeAction(toggled, toggled.select(false.rb, true.rb)))
                        .semantics { contentDescription = TOGGLE.rs }
                        .drawWithContent {
                            fillPaint.color = Color.Blue.rc.copy(red = progress)
                            drawRect(
                                paint = fillPaint,
                                topLeft = RemoteOffset(20f.rf, 24f.rf),
                                size = RemoteSize((width - 40f.rf) * progress, height - 48f.rf),
                            )
                        }
            )
        }
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithContentDescription(TOGGLE).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(DURATION_MS / 2L)

        screenshotRule.verifyScreenshot()
    }

    companion object {
        private const val WIDTH_DP = 200f
        private const val HEIGHT_DP = 144f
        private const val DURATION_MS = 400
        private const val TOGGLE = "Toggle"
    }
}
