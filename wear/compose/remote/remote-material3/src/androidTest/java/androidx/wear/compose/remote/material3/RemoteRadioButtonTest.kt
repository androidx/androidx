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
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonDisabledSelected
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonDisabledUnselected
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonSelected
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonUnselected
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonWithIcon
import androidx.wear.compose.remote.material3.previews.RemoteRadioButtonWithSecondaryLabel
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
class RemoteRadioButtonTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun radio_button_selected() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonSelected() }
        }
    }

    @Test
    fun radio_button_unselected() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonUnselected() }
        }
    }

    @Test
    fun radio_button_disabled_selected() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonDisabledSelected() }
        }
    }

    @Test
    fun radio_button_disabled_unselected() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonDisabledUnselected() }
        }
    }

    @Test
    fun radio_button_with_icon() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonWithIcon() }
        }
    }

    @Test
    fun radio_button_with_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteRadioButtonWithSecondaryLabel() }
        }
    }
}
