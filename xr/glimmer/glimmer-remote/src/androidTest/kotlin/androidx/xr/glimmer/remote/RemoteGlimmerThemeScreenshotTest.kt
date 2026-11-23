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

package androidx.xr.glimmer.remote

import android.content.Context
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteGlimmerThemeScreenshotTest {

    @get:Rule(order = 0) val composeTestRule = RcPlayerTestRule()

    @get:Rule(order = 1) val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun colors() {
        composeTestRule.setRemoteGlimmerThemeContent(context) {
            val colors = RemoteGlimmerTheme.colors
            RemoteColumn(
                modifier = RemoteModifier.padding(RemoteGlimmerTheme.componentSpacingValues.medium),
                verticalArrangement =
                    RemoteArrangement.spacedBy(
                        RemoteGlimmerTheme.componentSpacingValues.extraLarge
                    ),
            ) {
                ColorItem(colors.primary, colorName = "primary")
                ColorItem(colors.secondary, colorName = "secondary")
                ColorItem(colors.negative, colorName = "negative")
                ColorItem(colors.positive, colorName = "positive")
                ColorItem(colors.background, colorName = "background")
                ColorItem(colors.surface, colorName = "surface")
            }
        }
        composeTestRule.assertRootAgainstGolden("glimmerTheme_colors", screenshotRule)
    }

    @Test
    fun shapes() {
        composeTestRule.setRemoteGlimmerThemeContent(context) {
            val shapes = RemoteGlimmerTheme.shapes
            RemoteColumn(
                modifier = RemoteModifier.padding(RemoteGlimmerTheme.componentSpacingValues.medium),
                verticalArrangement =
                    RemoteArrangement.spacedBy(
                        RemoteGlimmerTheme.componentSpacingValues.extraLarge
                    ),
            ) {
                ShapeItem("small", shape = shapes.small)
                ShapeItem("medium", shape = shapes.medium)
                ShapeItem("large", shape = shapes.large)
            }
        }
        composeTestRule.assertRootAgainstGolden("glimmerTheme_shapes", screenshotRule)
    }

    @Test
    fun iconSizes() {
        composeTestRule.setRemoteGlimmerThemeContent(context) {
            val iconSizes = RemoteGlimmerTheme.iconSizes
            RemoteColumn(
                verticalArrangement = RemoteArrangement.spacedBy(20.rdp),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteIcon(
                    imageVector = FavoriteIcon,
                    contentDescription = null,
                    modifier = RemoteModifier.size(iconSizes.small),
                )
                RemoteIcon(
                    imageVector = FavoriteIcon,
                    contentDescription = null,
                    modifier = RemoteModifier.size(iconSizes.medium),
                )
                RemoteIcon(
                    imageVector = FavoriteIcon,
                    contentDescription = null,
                    modifier = RemoteModifier.size(iconSizes.large),
                )
            }
        }
        composeTestRule.assertRootAgainstGolden("glimmerTheme_iconSizes", screenshotRule)
    }

    @Composable
    @RemoteComposable
    private fun ColorItem(color: RemoteColor, colorName: String) {
        // Theme colors are constant, so they can be inspected at creation time.
        val constantColor = Color(color.constantValue)
        val contentColor = (if (constantColor.luminance() < 0.179f) Color.White else Color.Black).rc
        val textStyle = RemoteGlimmerTheme.typography.caption
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().background(color),
            horizontalArrangement = RemoteArrangement.SpaceBetween,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteText(
                text = colorName.rs,
                color = contentColor,
                style = textStyle,
                modifier = RemoteModifier.padding(16.rdp),
            )
            RemoteText(
                text = constantColor.toHexString().rs,
                color = contentColor,
                style = textStyle,
                modifier = RemoteModifier.padding(16.rdp),
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun ShapeItem(name: String, shape: RemoteShape) {
        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth().height(120.rdp).surface(shape = shape),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                text = name.rs,
                color = Color.White.rc,
                style = RemoteGlimmerTheme.typography.caption,
            )
        }
    }
}

/** Returns a hex string, dropping the leading alpha channel and any trailing zeroes. */
private fun Color.toHexString() =
    this.value.toString(16).padStart(16, '0').substring(2, 8).uppercase()

/** Icon taken from material-icons-core */
private val FavoriteIcon: RemoteImageVector =
    RemoteImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.rdp,
            defaultHeight = 24.rdp,
            viewportWidth = 24f.rf,
            viewportHeight = 24f.rf,
            tintColor = Color.Unspecified.rc,
        )
        .apply {
            path(fill = RemoteBrush.solidColor(Color.White.rc)) {
                moveTo(12.0f.rf, 21.35f.rf)
                lineToRelative((-1.45f).rf, (-1.32f).rf)
                curveTo(5.4f.rf, 15.36f.rf, 2.0f.rf, 12.28f.rf, 2.0f.rf, 8.5f.rf)
                curveTo(2.0f.rf, 5.42f.rf, 4.42f.rf, 3.0f.rf, 7.5f.rf, 3.0f.rf)
                curveToRelative(1.74f.rf, 0.0f.rf, 3.41f.rf, 0.81f.rf, 4.5f.rf, 2.09f.rf)
                curveTo(13.09f.rf, 3.81f.rf, 14.76f.rf, 3.0f.rf, 16.5f.rf, 3.0f.rf)
                curveTo(19.58f.rf, 3.0f.rf, 22.0f.rf, 5.42f.rf, 22.0f.rf, 8.5f.rf)
                curveToRelative(0.0f.rf, 3.78f.rf, (-3.4f).rf, 6.86f.rf, (-8.55f).rf, 11.54f.rf)
                lineTo(12.0f.rf, 21.35f.rf)
                close()
            }
        }
        .build()
