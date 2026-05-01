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
import androidx.collection.buildObjectIntMap
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection as TestLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupThreeButtons
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupTwoButtons
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteButtonGroupTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun buttonGroup_threeButton() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }

    @Test
    fun buttonGroup_threeButton_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            composableWrapper = { composable ->
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.TestLayoutDirection(LayoutDirection.Rtl)
                ) {
                    composable()
                }
            },
        ) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }

    @Test
    fun buttonGroup_twoButton() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteButtonGroupTwoButtons() }
        }
    }

    @Test
    fun buttonGroup_dynamicColor() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player ->
                colorOverrides.forEach { name, colorInt ->
                    player.setUserLocalColor(name, colorInt)
                }
            },
        ) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }
}
