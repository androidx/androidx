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
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
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
import com.google.common.truth.Truth.assertWithMessage
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
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun button_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonEnabled() }
        }
    }

    @Test
    fun button_with_icon_and_label_and_secondary_label_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithBorder() }
        }
    }

    @Test
    fun button_with_circle_shape() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
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
    fun button_with_border_and_large_corner_radius_scaling() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(120.rdp, 50.rdp),
                    border = 4.rdp,
                    borderColor = RemoteColor(Color.Green),
                    shape = RemoteRoundedCornerShape(topStart = 80.rdp, bottomStart = 80.rdp),
                ) {
                    RemoteText("scale".rs)
                }
            }
        }
    }

    // Tests that the corner radius is clamped to 0f when half the stroke (4.rdp)
    // exceeds the corner size (2.rdp), preventing negative radius values.
    @Test
    fun button_with_thick_border_clamping_corner_radius() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(120.rdp, 50.rdp),
                    border = 8.rdp,
                    borderColor = RemoteColor(Color.Green),
                    shape = RemoteRoundedCornerShape(2.rdp),
                ) {
                    RemoteText("clamp".rs)
                }
            }
        }
    }

    @Test
    fun button_enabled_container_background_image() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage =
                rememberNamedRemoteImageBitmap(name = "backgroundImage") {
                    createImage(200, 200).asImageBitmap()
                }
            ComponentContainer {
                val containerPainter = RemoteButtonDefaults.containerPainter(backgroundImage)
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
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage =
                rememberNamedRemoteImageBitmap(
                    name = "button_disabled_container_background_image"
                ) {
                    createImage(200, 200).asImageBitmap()
                }
            ComponentContainer {
                val enabled = false.rb
                val containerPainter = RemoteButtonDefaults.containerPainter(backgroundImage)
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
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_with_icon_and_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIcon() }
        }
    }

    @Test
    fun button_with_label_and_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
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
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player ->
                colorOverrides.forEach { name, colorInt ->
                    player.setUserLocalColor(name, colorInt)
                }
            },
        ) {
            ComponentContainer { RemoteButtonEnabled() }
        }
    }

    @Test
    fun button_enabled_and_has_action_click_modifier_is_added() {
        runBlocking {
            val captureRule = RemoteCaptureTestRule()
            val document =
                captureRule.captureDocument(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                ) {
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
            val captureRule = RemoteCaptureTestRule()
            val document =
                captureRule.captureDocument(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                ) {
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

    @Test
    fun button_border_width_is_scaled_with_density() {
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        val density = displayInfo.density.density
        remoteComposeTestRule.setContent(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = displayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    modifier = RemoteModifier.size(100.rdp, 50.rdp),
                    onClick = testAction,
                    border = 8.rdp,
                    borderColor = RemoteColor(Color.Red),
                    colors =
                        RemoteButtonDefaults.buttonColors(
                            containerColor = RemoteColor(Color.Black)
                        ),
                    shape = RemoteRectangleShape,
                ) {
                    RemoteText("button".rs)
                }
            }
        }

        val bitmap =
            remoteComposeTestRule.composeTestRule
                .onNodeWithTag(RemoteScreenshotTestRule.ROOT_TEST_TAG)
                .captureToImage()
                .asAndroidBitmap()

        val y = bitmap.height / 2
        var redPixelsCount = 0
        var firstRedX = -1
        var lastRedX = -1
        for (x in 0 until bitmap.width / 2) {
            val color = Color(bitmap.getPixel(x, y))
            if (color.red > 0.8f && color.green < 0.2f && color.blue < 0.2f) {
                redPixelsCount++
                if (firstRedX == -1) firstRedX = x
                lastRedX = x
            }
        }

        val expectedBorderWidthPx = (8 * density).toInt()
        assertWithMessage(
                "Expected border width of $expectedBorderWidthPx px (border=8.rdp * density=$density), " +
                    "found $redPixelsCount red pixels at y=$y in bitmap size ${bitmap.width}x${bitmap.height} " +
                    "(firstRedX=$firstRedX, lastRedX=$lastRedX)"
            )
            .that(kotlin.math.abs(redPixelsCount - expectedBorderWidthPx))
            .isAtMost(1)
    }

    // Replace all sequences of whitespace (including newlines, tabs) with a single space. Then
    // trim leading/trailing spaces from the whole string
    private fun String.normalizeWhiteSpace() = this.replace(Regex("\\s+"), " ").trim()

    private val testAction = hostAction("testAction".rs, 1.rf)
}
