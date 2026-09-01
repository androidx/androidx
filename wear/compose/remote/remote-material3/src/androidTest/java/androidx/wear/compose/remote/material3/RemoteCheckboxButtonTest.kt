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
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonChecked
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonDisabledChecked
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonDisabledUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonWithIcon
import androidx.wear.compose.remote.material3.previews.RemoteCheckboxButtonWithSecondaryLabel
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
class RemoteCheckboxButtonTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun checkbox_checked() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonChecked() }
        }
    }

    @Test
    fun checkbox_unchecked() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonUnchecked() }
        }
    }

    @Test
    fun checkbox_disabled_checked() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonDisabledChecked() }
        }
    }

    @Test
    fun checkbox_disabled_unchecked() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonDisabledUnchecked() }
        }
    }

    @Test
    fun checkbox_with_icon() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonWithIcon() }
        }
    }

    @Test
    fun checkbox_with_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteCheckboxButtonWithSecondaryLabel() }
        }
    }
}
