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
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
import androidx.wear.compose.remote.material3.util.PathClip
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestImageVectors
import androidx.wear.compose.remote.material3.util.withPathClip
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
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

    @Test
    fun icon_toggle_button_checked_ab(@TestParameter pathClip: PathClip) =
        screenshotTest(RcPlatformProfiles.ANDROIDX.withPathClip(pathClip)) {
            RemoteIconToggleButtonChecked()
        }

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

    @Test
    fun icon_toggle_button_animated_shapes_pressed() = pressedScreenshotTest {
        RemoteIconToggleButton(
            checked = false.rb,
            onCheckedChange = RemoteAction.None,
            shapes = RemoteIconToggleButtonDefaults.animatedShapes(),
        ) {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.DefaultIconSize),
            )
        }
    }

    @Test
    fun icon_toggle_button_variant_animated_checked_pressed() = pressedScreenshotTest {
        RemoteIconToggleButton(
            checked = true.rb,
            onCheckedChange = RemoteAction.None,
            shapes = RemoteIconToggleButtonDefaults.variantAnimatedShapes(),
        ) {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.DefaultIconSize),
            )
        }
    }

    private fun screenshotTest(
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        content: @Composable () -> Unit,
    ) {
        remoteComposeTestRule.runScreenshotTest(
            profile = profile,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { content() }
        }
    }

    private fun pressedScreenshotTest(content: @Composable @RemoteComposable () -> Unit) {
        lateinit var player: RemoteComposePlayer
        remoteComposeTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player = it },
        ) {
            ComponentContainer { content() }
        }
        val root =
            remoteComposeTestRule.composeTestRule.onNodeWithTag(
                RemoteScreenshotTestRule.ROOT_TEST_TAG
            )
        root.performTouchInput { down(center) }
        remoteComposeTestRule.composeTestRule.waitUntil(PressAnimationTimeoutMillis) {
            player.document.document.needsRepaint() < 0
        }
        remoteComposeTestRule.verifyScreenshot()
        root.performTouchInput { up() }
    }

    private companion object {
        const val PressAnimationTimeoutMillis = 5_000L
    }
}
