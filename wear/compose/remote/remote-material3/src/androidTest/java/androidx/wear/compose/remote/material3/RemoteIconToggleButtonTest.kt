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
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonDisabledChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonDisabledUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonExtraLarge
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonSmall
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonVariantChecked
import androidx.wear.compose.remote.material3.previews.RemoteIconToggleButtonVariantUnchecked
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconToggleButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test fun icon_toggle_button_checked() = screenshotTest { RemoteIconToggleButtonChecked() }

    @Test fun icon_toggle_button_unchecked() = screenshotTest { RemoteIconToggleButtonUnchecked() }

    @Test
    fun icon_toggle_button_disabled_checked() = screenshotTest {
        RemoteIconToggleButtonDisabledChecked()
    }

    @Test
    fun icon_toggle_button_disabled_unchecked() = screenshotTest {
        RemoteIconToggleButtonDisabledUnchecked()
    }

    @Test
    fun icon_toggle_button_variant_checked() = screenshotTest {
        RemoteIconToggleButtonVariantChecked()
    }

    @Test
    fun icon_toggle_button_variant_unchecked() = screenshotTest {
        RemoteIconToggleButtonVariantUnchecked()
    }

    @Test fun icon_toggle_button_small() = screenshotTest { RemoteIconToggleButtonSmall() }

    @Test fun icon_toggle_button_large() = screenshotTest { RemoteIconToggleButtonLarge() }

    @Test
    fun icon_toggle_button_extra_large() = screenshotTest { RemoteIconToggleButtonExtraLarge() }

    private fun screenshotTest(content: @Composable () -> Unit) {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.ANDROIDX,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { content() }
        }
    }
}
