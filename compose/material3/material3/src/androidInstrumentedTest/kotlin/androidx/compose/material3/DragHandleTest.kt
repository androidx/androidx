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

package androidx.compose.material3

import android.os.Build
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DragHandleTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun verticalDragHandle_defaultAccessibilitySize() {
        rule
            .setMaterialContentForSizeAssertions { VerticalDragHandle() }
            .assertWidthIsEqualTo(defaultAccessibilitySize)
            .assertHeightIsEqualTo(defaultAccessibilitySize)
            .assertTouchWidthIsEqualTo(defaultAccessibilitySize)
            .assertTouchHeightIsEqualTo(defaultAccessibilitySize)
    }

    @Test
    fun verticalDragHandle_customAccessibilitySize() {
        val customAccessibilitySize = 60.dp
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides customAccessibilitySize
                ) {
                    VerticalDragHandle()
                }
            }
            .assertWidthIsEqualTo(customAccessibilitySize)
            .assertHeightIsEqualTo(customAccessibilitySize)
            .assertTouchWidthIsEqualTo(customAccessibilitySize)
            .assertTouchHeightIsEqualTo(customAccessibilitySize)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_defaultVisual() {
        rule.assertContentShape({ defaultVisualShape }, { defaultVisualSize }, { defaultColor }) {
            VerticalDragHandle()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_pressedVisual() {
        rule.assertContentShape(
            { pressedVisualShape },
            { pressedVisualSize },
            { pressedColor },
            { press() },
        ) {
            VerticalDragHandle()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customSize() {
        val customSize = DpSize(20.dp, 50.dp)
        rule.assertContentShape({ defaultVisualShape }, { customSize }, { defaultColor }) {
            VerticalDragHandle(sizes = VerticalDragHandleDefaults.sizes(customSize))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customPressedSize() {
        val customPressedSize = DpSize(20.dp, 70.dp)
        rule.assertContentShape(
            { pressedVisualShape },
            { customPressedSize },
            { pressedColor },
            { press() },
        ) {
            VerticalDragHandle(
                sizes = VerticalDragHandleDefaults.sizes(pressedSize = customPressedSize)
            )
        }
    }

    @Test
    fun verticalDragHandle_customSize_ensureMinimumAccessibilitySize() {
        val customSize = DpSize(20.dp, 50.dp)
        rule
            .setMaterialContentForSizeAssertions {
                VerticalDragHandle(sizes = VerticalDragHandleDefaults.sizes(customSize))
            }
            .assertWidthIsEqualTo(defaultAccessibilitySize)
            .assertHeightIsEqualTo(customSize.height)
            .assertTouchWidthIsEqualTo(defaultAccessibilitySize)
            .assertTouchHeightIsEqualTo(customSize.height)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customShape() {
        val customShape = RectangleShape
        rule.assertContentShape({ customShape }, { defaultVisualSize }, { defaultColor }) {
            VerticalDragHandle(shapes = VerticalDragHandleDefaults.shapes(customShape))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customPressedShape() {
        val customPressedShape = RectangleShape
        rule.assertContentShape(
            { customPressedShape },
            { pressedVisualSize },
            { pressedColor },
            { press() },
        ) {
            VerticalDragHandle(
                shapes = VerticalDragHandleDefaults.shapes(pressedShape = customPressedShape)
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customColor() {
        val customColor = Color.Red
        rule.assertContentShape({ defaultVisualShape }, { defaultVisualSize }, { customColor }) {
            VerticalDragHandle(colors = VerticalDragHandleDefaults.colors(customColor))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalDragHandle_customPressedColor() {
        val customColor = Color.Red
        rule.assertContentShape(
            { pressedVisualShape },
            { pressedVisualSize },
            { customColor },
            { press() },
        ) {
            VerticalDragHandle(
                colors = VerticalDragHandleDefaults.colors(pressedColor = customColor)
            )
        }
    }

    private fun SemanticsNodeInteraction.press() = performTouchInput { down(center) }

    private val defaultAccessibilitySize = 48.dp
    private val defaultWidth = 4.dp
    private val defaultHeight = 48.dp
    private val defaultVisualSize = DpSize(defaultWidth, defaultHeight)
    private val defaultVisualShape
        @Composable get() = MaterialTheme.shapes.fromToken(ShapeKeyTokens.CornerFull)

    private val defaultColor
        @Composable get() = MaterialTheme.colorScheme.outline

    private val pressedWidth = 12.dp
    private val pressedHeight = 52.dp
    private val pressedVisualSize = DpSize(pressedWidth, pressedHeight)
    private val pressedVisualShape
        @Composable get() = MaterialTheme.shapes.fromToken(ShapeKeyTokens.CornerMedium)

    private val pressedColor
        @Composable get() = MaterialTheme.colorScheme.onSurface
}
