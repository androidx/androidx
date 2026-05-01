/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonTonal
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestImageVectors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun remote_icon_button_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }

    @Test
    fun remote_icon_button_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            composableWrapper = { content ->
                DeviceConfigurationOverride(DeviceConfigurationOverride.LayoutDirection(Rtl)) {
                    content()
                }
            },
        ) {
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }

    @Test
    fun remote_icon_button_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(testAction, enabled = RemoteBoolean(false)) {
                    RemoteIcon(
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                        modifier = RemoteModifier.size(24.rdp),
                    )
                }
            }
        }
    }

    @Test
    fun remote_icon_button_tonal_enabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteIconButtonTonal() }
        }
    }

    @Test
    fun remote_icon_button_tonal_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(
                    testAction,
                    enabled = RemoteBoolean(false),
                    colors = FILLED_TONAL_COLOR,
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Test
    fun remote_icon_button_outline_enabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteIconButtonOutlined() }
        }
    }

    @Test
    fun remote_icon_button_outline_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(
                    testAction,
                    border = 1.rdp,
                    borderColor = RemoteMaterialTheme.colorScheme.outline,
                    enabled = RemoteBoolean(false),
                    colors = OUTLINE_COLOR,
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    private companion object {
        val FILLED_TONAL_COLOR
            @Composable
            get() =
                RemoteIconButtonDefaults.iconButtonColors()
                    .copy(
                        containerColor = RemoteMaterialTheme.colorScheme.primary,
                        contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor =
                            RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
                        disabledContentColor =
                            RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
                    )

        val OUTLINE_COLOR
            @Composable
            get() =
                RemoteIconButtonDefaults.iconButtonColors()
                    .copy(
                        containerColor = RemoteColor(Color.Transparent),
                        contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = RemoteColor(Color.Transparent),
                        disabledContentColor =
                            RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
                    )
    }

    @Test
    fun remote_icon_button_dynamic_color() {
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
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }
}

private val testAction = HostAction("testAction".rs, 1.rf)
