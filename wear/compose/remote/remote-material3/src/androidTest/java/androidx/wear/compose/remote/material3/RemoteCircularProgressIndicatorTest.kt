/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressEnabled
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressIndicatorCustomColor
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressIndicatorDisabled
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressNoGapCustomAngle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalRemoteCreationComposeApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCircularProgressIndicatorTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        CreationDisplayInfo(500, 500, context.resources.displayMetrics.densityDpi)

    @Test
    fun indicator_enabled() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            RemoteCircularProgressEnabled()
        }
    }

    @Test
    fun indicator_disabled() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            RemoteCircularProgressIndicatorDisabled()
        }
    }

    @Test
    fun indicator_customColors() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            RemoteCircularProgressIndicatorCustomColor()
        }
    }

    @Test
    fun indicator_customEndAngle_and_noGap() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            RemoteCircularProgressNoGapCustomAngle()
        }
    }
}
