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
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressComplete
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressCustomColor
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressCustomWidth
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressDefault
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressDisabled
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressScaledDot
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressSmallStroke
import androidx.wear.compose.remote.material3.previews.RemoteLinearProgressZero
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteLinearProgressIndicatorTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun indicator_default() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressDefault() }
        }
    }

    @Test
    fun indicator_custom_width() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressCustomWidth() }
        }
    }

    @Test
    fun indicator_scaled_down_dot() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressScaledDot() }
        }
    }

    @Test
    fun indicator_custom_color() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressCustomColor() }
        }
    }

    @Test
    fun indicator_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressDisabled() }
        }
    }

    @Test
    fun indicator_small_stroke() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressSmallStroke() }
        }
    }

    @Test
    fun indicator_zero_progress() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressZero() }
        }
    }

    @Test
    fun indicator_complete() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteLinearProgressComplete() }
        }
    }
}
