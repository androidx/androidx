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
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteButtonChildColors
import androidx.wear.compose.remote.material3.previews.RemoteButtonChildColorsDisabled
import androidx.wear.compose.remote.material3.previews.RemoteButtonCustomConfig
import androidx.wear.compose.remote.material3.previews.RemoteButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteButtonTwoLineText
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithBorder
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIcon
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIconAndLongLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIconAndSecondaryLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithIconAndSecondaryLabelDisabled
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithLongLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithMultilineLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithSecondaryLabel
import androidx.wear.compose.remote.material3.previews.RemoteButtonWithSmallImageBackground
import androidx.wear.compose.remote.material3.previews.utils.createImage
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.PathClip
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import androidx.wear.compose.remote.material3.util.withPathClip
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@SuppressLint("UnrememberedMutableState")
@RunWith(TestParameterInjector::class)
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
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonEnabled() }
        }
    }

    @Test
    fun button_with_custom_config() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonCustomConfig() }
        }
    }

    @Test
    fun button_two_lined_text() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonTwoLineText() }
        }
    }

    @Test
    fun button_with_icon_and_label_and_secondary_label_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_with_icon_and_secondary_label_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabelDisabled() }
        }
    }

    @Test
    fun button_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithBorder() }
        }
    }

    @Test
    fun button_with_circle_shape() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(150.rdp),
                    border = RemoteBorderStroke(width = 8.rdp, color = Color.Green.rc),
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
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(120.rdp, 50.rdp),
                    border = RemoteBorderStroke(width = 4.rdp, color = Color.Green.rc),
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
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.size(120.rdp, 50.rdp),
                    border = RemoteBorderStroke(width = 8.rdp, color = Color.Green.rc),
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
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage = remember {
                createNamedRemoteImageBitmap(name = "backgroundImage") {
                    createImage(200, 200).asImageBitmap()
                }
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
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage = remember {
                createNamedRemoteImageBitmap(name = "button_disabled_container_background_image") {
                    createImage(200, 200).asImageBitmap()
                }
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
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIconAndSecondaryLabel() }
        }
    }

    @Test
    fun button_with_icon_and_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIcon() }
        }
    }

    @Test
    fun button_with_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithLabel() }
        }
    }

    @Test
    fun button_with_multiline_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithMultilineLabel() }
        }
    }

    @Test
    fun button_with_long_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithLongLabel() }
        }
    }

    @Test
    fun button_with_icon_and_long_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithIconAndLongLabel() }
        }
    }

    @Test
    fun button_with_label_and_secondary_label() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
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
            profile = TestProfiles.wearWidgetsWithCoreText,
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
    fun button_child_colors() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonChildColors() }
        }
    }

    @Test
    fun button_child_colors_disabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonChildColorsDisabled() }
        }
    }

    @Test
    fun button_container_background_small_image_with_scrim() {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteButtonWithSmallImageBackground() }
        }
    }

    @Test
    fun button_enabled_container_background_image_ab(@TestParameter pathClip: PathClip) {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText.withPathClip(pathClip),
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage = remember {
                createNamedRemoteImageBitmap(name = "backgroundImage") {
                    createImage(200, 200).asImageBitmap()
                }
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

    /**
     * A hexagon shape can only be clipped with a path clip. Without one, [RemoteModifier.clip]
     * falls back to a rectangle so the container image fills the full button bounds.
     */
    @Test
    fun button_hexagon_shape_container_background_image_ab(@TestParameter pathClip: PathClip) {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText.withPathClip(pathClip),
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val backgroundImage = remember {
                createNamedRemoteImageBitmap(name = "backgroundImage") {
                    createImage(200, 200).asImageBitmap()
                }
            }
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    containerPainter = RemoteButtonDefaults.containerPainter(backgroundImage),
                    shape = HexagonShape,
                ) {
                    RemoteText("hexagon".rs)
                }
            }
        }
    }

    /**
     * The container painter draws over the full rectangular bounds, including the corners outside
     * the default stadium shape. These corners are clipped away both by the rounded rectangle clip
     * of [RemoteModifier.clip] and by the path clip, so both variants should match.
     */
    @Test
    fun button_container_painter_draws_in_corners_ab(@TestParameter pathClip: PathClip) {
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgetsWithCoreText.withPathClip(pathClip),
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    containerPainter = CornerMarkersPainter,
                ) {
                    RemoteText("corners".rs)
                }
            }
        }
    }

    private val testAction = hostAction("testAction".rs, 1.rf)
}

/** A hexagon with points at the start and end, which can't be expressed as a rounded rectangle. */
private object HexagonShape : RemoteShape {
    override fun createOutline(
        size: RemoteSize,
        layoutDirection: LayoutDirection,
        density: RemoteDensity,
    ): RemoteOutline {
        val w = size.width
        val h = size.height
        val inset = h / 2f.rf
        return RemoteOutline.Generic {
            moveTo(0f.rf, h / 2f.rf)
            lineTo(inset, 0f.rf)
            lineTo(w - inset, 0f.rf)
            lineTo(w, h / 2f.rf)
            lineTo(w - inset, h)
            lineTo(inset, h)
            close()
        }
    }
}

/**
 * Fills the full bounds and draws a marker in each corner, so drawing outside of a rounded shape is
 * visible.
 */
private object CornerMarkersPainter : RemotePainter() {
    override val intrinsicSize: RemoteSize? = null

    override fun RemoteDrawScope.onDraw() {
        drawRect(RemotePaint { color = Color.Blue.rc })
        val marker = RemoteSize(height / 3f.rf, height / 3f.rf)
        val markerPaint = RemotePaint { color = Color.Yellow.rc }
        drawRect(markerPaint, RemoteOffset(0f.rf, 0f.rf), marker)
        drawRect(markerPaint, RemoteOffset(width - marker.width, 0f.rf), marker)
        drawRect(markerPaint, RemoteOffset(0f.rf, height - marker.height), marker)
        drawRect(markerPaint, RemoteOffset(width - marker.width, height - marker.height), marker)
    }
}
