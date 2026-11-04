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
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rs
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
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonChecked
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonDisabledChecked
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonDisabledUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonExtraLarge
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonUnchecked
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonVariantChecked
import androidx.wear.compose.remote.material3.previews.RemoteTextToggleButtonVariantUnchecked
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.PathClip
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.withPathClip
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class RemoteTextToggleButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test fun text_toggle_button_checked() = screenshotTest { RemoteTextToggleButtonChecked() }

    @Test
    fun text_toggle_button_checked_ab(@TestParameter pathClip: PathClip) =
        screenshotTest(RcPlatformProfiles.ANDROIDX.withPathClip(pathClip)) {
            RemoteTextToggleButtonChecked()
        }

    @Test fun text_toggle_button_unchecked() = screenshotTest { RemoteTextToggleButtonUnchecked() }

    @Test
    fun text_toggle_button_disabled_checked() = screenshotTest {
        RemoteTextToggleButtonDisabledChecked()
    }

    @Test
    fun text_toggle_button_disabled_unchecked() = screenshotTest {
        RemoteTextToggleButtonDisabledUnchecked()
    }

    @Test
    fun text_toggle_button_variant_checked() = screenshotTest {
        RemoteTextToggleButtonVariantChecked()
    }

    @Test
    fun text_toggle_button_variant_unchecked() = screenshotTest {
        RemoteTextToggleButtonVariantUnchecked()
    }

    @Test fun text_toggle_button_large() = screenshotTest { RemoteTextToggleButtonLarge() }

    @Test
    fun text_toggle_button_extra_large() = screenshotTest { RemoteTextToggleButtonExtraLarge() }

    @Test
    fun text_toggle_button_animated_shapes_pressed() = pressedScreenshotTest {
        RemoteTextToggleButton(
            checked = false.rb,
            onCheckedChange = RemoteAction.None,
            shapes = RemoteTextToggleButtonDefaults.animatedShapes(),
        ) {
            RemoteText("ABC".rs)
        }
    }

    @Test
    fun text_toggle_button_variant_animated_checked_pressed() = pressedScreenshotTest {
        RemoteTextToggleButton(
            checked = true.rb,
            onCheckedChange = RemoteAction.None,
            shapes = RemoteTextToggleButtonDefaults.variantAnimatedShapes(),
        ) {
            RemoteText("ABC".rs)
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
