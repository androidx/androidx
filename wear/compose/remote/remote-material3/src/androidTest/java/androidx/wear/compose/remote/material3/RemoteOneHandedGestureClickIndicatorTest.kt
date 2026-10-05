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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Screenshot tests for [RemoteOneHandedGestureClickIndicator].
 *
 * Samples the 1.82s one-handed gesture click indicator sequence at key milestones:
 * - Rest (0.0): Base content fully visible, indicator invisible.
 * - Enter (0.2): Base content fading out, indicator scaling in.
 * - Peak gesture (0.5): Double-pinch gesture animation playing while fully scaled in.
 * - Exit (0.8): Indicator scaling out, base content fading back in.
 * - Cycle end (1.0): Idle state restored.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteOneHandedGestureClickIndicatorTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val buttonCreationDisplayInfo =
        createCreationDisplayInfo(context, Size(BUTTON_DISPLAY_SIZE_PX, BUTTON_DISPLAY_SIZE_PX))
    private val iconButtonCreationDisplayInfo =
        createCreationDisplayInfo(
            context,
            Size(ICON_BUTTON_DISPLAY_SIZE_PX, ICON_BUTTON_DISPLAY_SIZE_PX),
        )
    private val testAction = hostAction("testAction".rs)

    @Test
    fun clickIndicator_atRest() {
        verifyIndicatorAt(progress = 0f)
    }

    @Test
    fun clickIndicator_entering() {
        verifyIndicatorAt(progress = 0.2f)
    }

    @Test
    fun clickIndicator_peakGesture() {
        verifyIndicatorAt(progress = 0.5f)
    }

    @Test
    fun clickIndicator_exiting() {
        verifyIndicatorAt(progress = 0.8f)
    }

    @Test
    fun clickIndicator_cycleEnd() {
        verifyIndicatorAt(progress = 1f)
    }

    @Test
    fun clickIndicator_iconButton() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = iconButtonCreationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteIconButton(
                    onClick = testAction,
                    colors = RemoteIconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    RemoteOneHandedGestureClickIndicator(
                        hintProgress = 0.1f.rf,
                        gestureIndicatorSize = RemoteOneHandedGestureDefaults.SmallIndicatorSize,
                    ) {
                        RemoteIcon(
                            imageVector = TestImageVectors.VolumeUp,
                            contentDescription = "Volume".rs,
                            modifier =
                                RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
                        )
                    }
                }
            }
        }
    }

    private fun verifyIndicatorAt(progress: Float) {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = buttonCreationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(onClick = testAction) {
                    RemoteOneHandedGestureClickIndicator(hintProgress = progress.rf) {
                        RemoteText("Click Me".rs)
                    }
                }
            }
        }
    }

    private companion object {
        const val BUTTON_DISPLAY_SIZE_PX = 500f
        const val ICON_BUTTON_DISPLAY_SIZE_PX = 180f
    }
}
