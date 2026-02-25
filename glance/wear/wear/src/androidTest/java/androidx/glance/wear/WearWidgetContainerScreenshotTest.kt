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

package androidx.glance.wear

import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.wear.composable.WearWidgetContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class WearWidgetContainerScreenshotTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun containerSmall_appliesCornerAndPadding() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = getCreationDisplayInfo(SMALL_WIDGET_HEIGHT),
            backgroundColor = Color.Black,
            profile = RcPlatformProfiles.WEAR_WIDGETS,
        ) {
            TestWearWidget()
        }
    }

    @Test
    fun containerLarge_appliesCornerAndPadding() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = getCreationDisplayInfo(LARGE_WIDGET_HEIGHT),
            backgroundColor = Color.Black,
            profile = RcPlatformProfiles.WEAR_WIDGETS,
        ) {
            TestWearWidget()
        }
    }

    private companion object {
        const val WIDGET_WIDTH = 216
        const val SMALL_WIDGET_HEIGHT = 88
        const val LARGE_WIDGET_HEIGHT = 128
        const val DENSITY_DPI = 160

        private fun getCreationDisplayInfo(height: Int): CreationDisplayInfo {
            return CreationDisplayInfo(WIDGET_WIDTH, height, DENSITY_DPI)
        }
    }
}

@RemoteComposable
@Composable
private fun TestWearWidget() {
    WearWidgetContainer(
        horizontalPadding = 8.rdp,
        verticalPadding = 8.rdp,
        cornerRadius = 26.dp,
        backgroundColor = Color.Blue,
    ) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Yellow))
    }
}
