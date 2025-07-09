/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CenteredText
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.setContentWithTheme
import androidx.wear.compose.material3.verifyScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun button_enabled() = verifyScreenshot { BaseButton() }

    @Test fun button_disabled() = verifyScreenshot { BaseButton(enabled = false) }

    @Test
    fun button_default_alignment() = verifyScreenshot {
        // Uses the base Button overload, should be vertically centered by default
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().testTag(TEST_TAG)) {
            Text("Button")
        }
    }

    @Test
    fun button_top_alignment() = verifyScreenshot {
        // Uses RowScope to override the default vertical alignment to be top
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().testTag(TEST_TAG)) {
            Text("Button", modifier = Modifier.align(Alignment.Top))
        }
    }

    @Test
    fun three_slot_button_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { ThreeSlotButton() }

    @Test
    fun three_slot_button_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) { ThreeSlotButton() }

    @Test fun button_outlined_enabled() = verifyScreenshot() { OutlinedButton() }

    @Test fun button_outlined_disabled() = verifyScreenshot() { OutlinedButton(enabled = false) }

    @Test
    fun button_image_background_enabled() = verifyScreenshot {
        ImageBackgroundButton(
            enabled = true,
            containerImage = painterResource(R.drawable.backgroundimage1),
            sizeToIntrinsics = false,
        )
    }

    @Test
    fun button_image_background_disabled() = verifyScreenshot {
        ImageBackgroundButton(
            enabled = false,
            containerImage = painterResource(R.drawable.backgroundimage1),
            sizeToIntrinsics = false,
        )
    }

    @Test
    fun button_image_background_with_alignment_center_end() = verifyScreenshot {
        ImageBackgroundButton(
            sizeToIntrinsics = true,
            alignment = Alignment.CenterEnd,
            contentScale = ContentScale.None,
        )
    }

    @Test
    fun button_image_background_with_alignment_center() = verifyScreenshot {
        ImageBackgroundButton(
            sizeToIntrinsics = true,
            alignment = Alignment.Center,
            contentScale = ContentScale.None,
        )
    }

    @Test
    fun button_label_only_center_aligned() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
        )
    }

    @Test
    fun button_icon_and_label_start_aligned() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
        )
    }

    @Test
    fun button_large_icon() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label", modifier = Modifier.fillMaxWidth()) },
            secondaryLabel = { Text("Secondary label", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.LargeIconSize) },
            contentPadding = ButtonDefaults.ButtonWithLargeIconContentPadding,
        )
    }

    @Test
    fun button_extra_large_icon() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label", modifier = Modifier.fillMaxWidth()) },
            secondaryLabel = { Text("Secondary label", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.ExtraLargeIconSize) },
            contentPadding = ButtonDefaults.ButtonWithExtraLargeIconContentPadding,
        )
    }

    @Test
    fun filled_tonal_button_label_only_center_aligned() = verifyScreenshot {
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
        )
    }

    @Test
    fun filled_tonal_button_icon_and_label_start_aligned() = verifyScreenshot {
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
        )
    }

    @Test
    fun outlined_tonal_button_label_only_center_aligned() = verifyScreenshot {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
        )
    }

    @Test
    fun outlined_tonal_button_icon_and_label_start_aligned() = verifyScreenshot {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
        )
    }

    @Test
    fun child_button_label_only_center_aligned() = verifyScreenshot {
        ChildButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
        )
    }

    @Test
    fun child_button_icon_and_label_start_aligned() = verifyScreenshot {
        ChildButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
        )
    }

    @Test fun compact_button_enabled() = verifyScreenshot { CompactButton() }

    @Test fun compact_button_disabled() = verifyScreenshot { CompactButton(enabled = false) }

    @Test
    fun compact_button_label_only_center_aligned() = verifyScreenshot {
        CompactButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
        )
    }

    @Test
    fun compact_button_icon_and_label_start_aligned() = verifyScreenshot {
        CompactButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Icon & label", modifier = Modifier.fillMaxWidth()) },
            icon = { ButtonIcon(size = ButtonDefaults.ExtraSmallIconSize) },
        )
    }

    @Test
    fun button_with_morphing_and_content_alpha_transformation() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            transformation =
                morphingSurfaceTransformation(heightProportion = 0.6f, contentAlpha = 0.5f),
        )
    }

    @Test
    fun outline_button_with_morphing_and_content_alpha_transformation() = verifyScreenshot {
        OutlinedButton(
            onClick = {},
            enabled = true,
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            transformation = morphingSurfaceTransformation(heightProportion = 0.6f),
        )
    }

    @Test
    fun button_with_faded_content_transformation() = verifyScreenshot {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
            label = { Text("Label only", modifier = Modifier.fillMaxWidth()) },
            transformation = morphingSurfaceTransformation(heightProportion = 1f, contentAlpha = 0f),
        )
    }

    @Composable
    private fun BaseButton(enabled: Boolean = true) {
        Button(enabled = enabled, onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {
            CenteredText("Base Button")
        }
    }

    @Composable
    private fun ThreeSlotButton(enabled: Boolean = true) {
        Button(
            enabled = enabled,
            onClick = {},
            label = { Text("Three Slot Button") },
            secondaryLabel = { Text("Secondary Label") },
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun OutlinedButton(enabled: Boolean = true) {
        OutlinedButton(enabled = enabled, onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {
            CenteredText("Outlined Button")
        }
    }

    @Composable
    private fun ImageBackgroundButton(
        sizeToIntrinsics: Boolean,
        containerImage: Painter =
            painterResource(androidx.wear.compose.material3.samples.R.drawable.backgroundimage),
        enabled: Boolean = true,
        alignment: Alignment = Alignment.Center,
        contentScale: ContentScale = ContentScale.Fit,
    ) {
        Button(
            enabled = enabled,
            onClick = {},
            label = { Text("Image Button") },
            secondaryLabel = { Text("Secondary Label") },
            containerPainter =
                ButtonDefaults.containerPainter(
                    image = containerImage,
                    sizeToIntrinsics = sizeToIntrinsics,
                    alignment = alignment,
                    contentScale = contentScale,
                ),
            icon = { ButtonIcon(size = ButtonDefaults.IconSize) },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun CompactButton(enabled: Boolean = true) {
        CompactButton(
            onClick = {},
            label = { Text("Compact Button") },
            icon = { ButtonIcon(size = ButtonDefaults.ExtraSmallIconSize) },
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Composable
    private fun ButtonIcon(
        size: Dp,
        modifier: Modifier = Modifier,
        iconLabel: String = "ButtonIcon",
    ) {
        val testImage = Icons.Outlined.AccountCircle
        Icon(
            imageVector = testImage,
            contentDescription = iconLabel,
            modifier = modifier.testTag(iconLabel).size(size),
        )
    }

    private fun morphingSurfaceTransformation(heightProportion: Float, contentAlpha: Float = 1f) =
        object : SurfaceTransformation {
            override fun createContainerPainter(
                painter: Painter,
                shape: Shape,
                border: BorderStroke?,
            ): Painter =
                object : Painter() {
                    override val intrinsicSize: Size
                        get() = Size.Unspecified

                    override fun DrawScope.onDraw() {
                        val shapeOutline =
                            shape.createOutline(
                                size.copy(height = size.height * heightProportion),
                                layoutDirection,
                                this@onDraw,
                            )
                        clipPath(Path().apply { addOutline(shapeOutline) }) {
                            with(painter) {
                                draw(size.copy(height = size.height * heightProportion))
                            }
                        }
                    }
                }

            override fun GraphicsLayerScope.applyContentTransformation() {
                alpha = contentAlpha
            }

            override fun GraphicsLayerScope.applyContainerTransformation() {
                clip = true
                val shape = this.shape
                this.shape =
                    object : Shape {
                        override fun createOutline(
                            size: Size,
                            layoutDirection: LayoutDirection,
                            density: Density,
                        ): Outline =
                            shape.createOutline(
                                size = size.copy(height = size.height * heightProportion),
                                layoutDirection = layoutDirection,
                                density = density,
                            )
                    }
            }
        }
}
