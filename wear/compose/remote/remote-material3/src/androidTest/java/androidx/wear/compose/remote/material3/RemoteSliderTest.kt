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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteSliderCustomColors
import androidx.wear.compose.remote.material3.previews.RemoteSliderDefault
import androidx.wear.compose.remote.material3.previews.RemoteSliderDisabled
import androidx.wear.compose.remote.material3.previews.RemoteSliderNotSegmented
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteSliderTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun remote_slider_default() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteSliderDefault() }
        }
    }

    @Test
    fun remote_slider_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteSliderDisabled() }
        }
    }

    @Test
    fun remote_slider_not_segmented() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteSliderNotSegmented() }
        }
    }

    @Test
    fun remote_slider_custom_colors() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteSliderCustomColors() }
        }
    }
}
