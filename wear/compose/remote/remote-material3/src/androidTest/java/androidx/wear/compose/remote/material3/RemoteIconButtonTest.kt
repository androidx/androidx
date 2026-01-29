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
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonTonal
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
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        CreationDisplayInfo(500, 500, context.resources.displayMetrics.densityDpi)

    @Test
    fun remote_icon_button_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) { RemoteIconButtonEnabled() }
        }
    }

    @Test
    fun remote_icon_button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) {
                RemoteIconButton(testAction, enabled = RemoteBoolean(false)) {
                    RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
                }
            }
        }
    }

    @Test
    fun remote_icon_button_tonal_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) { RemoteIconButtonTonal() }
        }
    }

    @Test
    fun remote_icon_button_tonal_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) {
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
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) { RemoteIconButtonOutlined() }
        }
    }

    @Test
    fun remote_icon_button_outline_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            Center(RemoteModifier.fillMaxSize()) {
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
}

@Composable
@RemoteComposable
private fun Center(modifier: RemoteModifier, content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}

private val testAction = HostAction("testAction".rs, 1.rf)
