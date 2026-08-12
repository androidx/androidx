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

import android.annotation.SuppressLint
import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonDisabled
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonExtraSmall
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonFilledTonal
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonFilledVariant
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonMedium
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonSmall
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonWithBorder
import androidx.wear.compose.remote.material3.previews.RemoteEdgeButtonWithIcon
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
class RemoteEdgeButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun edge_button_extra_small() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonExtraSmall() }
        }
    }

    @Test
    fun edge_button_small() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonSmall() }
        }
    }

    @Test
    fun edge_button_medium() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonMedium() }
        }
    }

    @Test
    fun edge_button_large() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonLarge() }
        }
    }

    @Test
    fun edge_button_filled_tonal() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonFilledTonal() }
        }
    }

    @Test
    fun edge_button_filled_variant() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonFilledVariant() }
        }
    }

    @Test
    fun edge_button_outlined() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonOutlined() }
        }
    }

    @Test
    fun edge_button_with_border() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonWithBorder() }
        }
    }

    @Test
    fun edge_button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonDisabled() }
        }
    }

    @Test
    fun edge_button_with_icon() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteEdgeButtonWithIcon() }
        }
    }

    @Test
    fun edge_button_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComponentContainer { RemoteEdgeButtonSmall() }
        }
    }

    @Test
    fun edge_button_dynamic_color() {
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
            ComponentContainer { RemoteEdgeButtonSmall() }
        }
    }
}

private val testAction = hostAction("testAction".rs, 1.rf)
