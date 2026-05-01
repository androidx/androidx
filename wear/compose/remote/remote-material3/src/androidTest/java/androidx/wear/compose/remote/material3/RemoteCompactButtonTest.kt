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

import android.annotation.SuppressLint
import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteCompactButtonWithIcon
import androidx.wear.compose.remote.material3.previews.RemoteCompactButtonWithIconAndLabel
import androidx.wear.compose.remote.material3.previews.RemoteCompactButtonWithLabel
import androidx.wear.compose.remote.material3.previews.RemoteCompactButtonWithShape
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@SuppressLint("UnrememberedMutableState")
@RunWith(JUnit4::class)
class RemoteCompactButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun compact_button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteCompactButton(
                    onClick = testAction,
                    modifier = RemoteModifier,
                    enabled = false.rb,
                    label = { RemoteText("disabled".rs) },
                )
            }
        }
    }

    @Test
    fun compact_button_icon_and_label_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            composableWrapper = { content ->
                DeviceConfigurationOverride(DeviceConfigurationOverride.LayoutDirection(Rtl)) {
                    content()
                }
            },
        ) {
            ComponentContainer { RemoteCompactButtonWithIconAndLabel() }
        }
    }

    @Test
    fun compact_button_icon_only() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteCompactButtonWithIcon() }
        }
    }

    @Test
    fun compact_button_label_only() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteCompactButtonWithLabel() }
        }
    }

    @Test
    fun compact_button_icon_and_label() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteCompactButtonWithIconAndLabel() }
        }
    }

    @Test
    fun compact_button_with_shape() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteCompactButtonWithShape() }
        }
    }

    @Test
    fun compact_button_dynamic_color() {
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
            ComponentContainer { RemoteCompactButtonWithIconAndLabel() }
        }
    }
}

private val testAction = HostAction("".rs, 1.rf)
