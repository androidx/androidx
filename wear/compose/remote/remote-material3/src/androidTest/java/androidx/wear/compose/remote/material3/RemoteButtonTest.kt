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
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.painterRemoteBitmap
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithBorder
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIcon
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIconAndSecondaryLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithSecondaryLabel
import androidx.wear.compose.remote.material3.previews.utils.createImage
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@SuppressLint("UnrememberedMutableState")
@RunWith(JUnit4::class)
class RemoteButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun button_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonEnabled() }
        }
    }

    @Test
    fun button_with_icon_and_label_and_secondary_label_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    enabled = false.rb,
                ) {
                    RemoteText("button_disabled".rs)
                }
            }
        }
    }

    @Test
    fun button_overrides_colors() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            val colors =
                RemoteButtonColors(
                    containerColor = RemoteColor(Color.Yellow),
                    contentColor = RemoteColor(Color.Cyan),
                    secondaryContentColor = RemoteColor(Color.Black),
                    iconColor = RemoteColor(Color.Black),
                    disabledContainerColor = RemoteColor(Color.Black),
                    disabledContentColor = RemoteColor(Color.Black),
                    disabledSecondaryContentColor = RemoteColor(Color.Black),
                    disabledIconColor = RemoteColor(Color.Black),
                )
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    colors = colors,
                ) {
                    RemoteText("button_overrides_colors".rs)
                }
            }
        }
    }

    @Test
    fun button_overrides_padding() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    contentPadding = RemotePaddingValues(50.rdp),
                ) {
                    RemoteText("button_overrides_padding".rs)
                }
            }
        }
    }

    @Test
    fun button_overrides_size() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(180.rdp, 100.rdp),
                    contentPadding = RemotePaddingValues(0.rdp),
                ) {
                    RemoteText("button_overrides_size".rs)
                }
            }
        }
    }

    @Test
    fun button_overrides_textStyle() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    contentPadding = RemotePaddingValues(0.rdp),
                ) {
                    RemoteText(
                        "button_overrides_textStyle".rs,
                        color = null,
                        style = RemoteMaterialTheme.typography.labelSmall.copy(Color.Cyan.rc),
                    )
                }
            }
        }
    }

    @Test
    fun button_with_border() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithBorder() }
        }
    }

    @Test
    fun button_with_circle_shape() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(150.rdp),
                    border = 8.rdp,
                    borderColor = RemoteColor(Color.Green),
                    shape = RemoteCircleShape,
                ) {
                    RemoteText("button_with_circle_shape".rs)
                }
            }
        }
    }

    @Test
    fun button_enabled_container_background_image() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage =
                rememberNamedRemoteBitmap(name = "backgroundImage") {
                    createImage(200, 200).asImageBitmap()
                }
            ComponentContainer {
                val containerPainter =
                    RemoteButtonDefaults.containerPainter(painterRemoteBitmap(backgroundImage))
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    containerPainter = containerPainter,
                ) {
                    RemoteText("image_background".rs)
                }
            }
        }
    }

    @Test
    fun button_disabled_container_background_image() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage =
                rememberNamedRemoteBitmap(name = "button_disabled_container_background_image") {
                    createImage(200, 200).asImageBitmap()
                }
            ComponentContainer {
                val enabled = false.rb
                val containerPainter =
                    RemoteButtonDefaults.containerPainter(painterRemoteBitmap(backgroundImage))
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    enabled = enabled,
                    containerPainter = containerPainter,
                ) {
                    RemoteText("disable_image_background".rs)
                }
            }
        }
    }

    @Test
    fun button_with_icon_and_label_and_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_with_icon_and_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIcon() }
        }
    }

    @Test
    fun button_with_label_and_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithSecondaryLabel() }
        }
    }

    @Test
    fun button_dynamic_color() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            backgroundColor = Color.Black,
            creationDisplayInfo = creationDisplayInfo,
            colorOverrides = colorOverrides,
        ) {
            ComponentContainer { RemoteButtonEnabled() }
        }
    }

    @Test
    fun button_enabled_and_has_action_click_modifier_is_added() {
        runBlocking {
            val document =
                remoteComposeTestRule.captureDocument(context = context) {
                    RemoteButton(
                        modifier = RemoteModifier.buttonSizeModifier(),
                        onClick = testAction,
                        enabled = true.rb,
                    ) {
                        RemoteText("button_enabled".rs)
                    }
                }
            val actualContent = document.displayHierarchy()

            assertThat(actualContent.normalizeWhiteSpace()).contains("CLICK_MODIFIER")
        }
    }

    @Test
    fun button_disabled_click_modifier_is_not_added() {
        runBlocking {
            val document =
                remoteComposeTestRule.captureDocument(context = context) {
                    RemoteButton(
                        onClick = testAction,
                        modifier = RemoteModifier.buttonSizeModifier(),
                        enabled = false.rb,
                    ) {
                        RemoteText("button_disabled".rs)
                    }
                }
            val actualContent = document.displayHierarchy()

            assertThat(actualContent.normalizeWhiteSpace()).doesNotContain("CLICK_MODIFIER")
        }
    }

    // Replace all sequences of whitespace (including newlines, tabs) with a single space. Then
    // trim leading/trailing spaces from the whole string
    private fun String.normalizeWhiteSpace() = this.replace(Regex("``s+"), " ").trim()

    private val testAction = HostAction("testAction".rs, 1.rf)
}
