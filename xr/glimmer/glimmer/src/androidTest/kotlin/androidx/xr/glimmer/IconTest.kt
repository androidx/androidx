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
package androidx.xr.glimmer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class IconTest {
    @get:Rule val rule = createComposeRule()

    private val iconTag = "Icon"

    @Test
    fun localIconSize_defaultSize() {
        var localIconSize: Dp? = null
        var mediumIconSize: Dp? = null
        rule.setGlimmerThemeContent {
            localIconSize = LocalIconSize.current
            mediumIconSize = GlimmerTheme.iconSizes.medium
        }
        assertThat(localIconSize!!).isEqualTo(mediumIconSize!!)
    }

    @Test
    fun vector_defaultSize() {
        val width = 35.dp
        val height = 83.dp
        val vector =
            ImageVector.Builder(
                    defaultWidth = width,
                    defaultHeight = height,
                    viewportWidth = width.value,
                    viewportHeight = height.value,
                )
                .build()
        rule.setGlimmerThemeContent {
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag),
            )
        }

        // Intrinsic size should be ignored, the actual size should be 40
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun vector_localIconSize() {
        val width = 35.dp
        val height = 83.dp
        val vector =
            ImageVector.Builder(
                    defaultWidth = width,
                    defaultHeight = height,
                    viewportWidth = width.value,
                    viewportHeight = height.value,
                )
                .build()
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalIconSize provides 50.dp) {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    modifier = Modifier.testTag(iconTag),
                )
            }
        }

        // Intrinsic size should be ignored, the actual size should be 50
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun vector_smallerSize() {
        val width = 35.dp
        val height = 83.dp
        val vector =
            ImageVector.Builder(
                    defaultWidth = width,
                    defaultHeight = height,
                    viewportWidth = width.value,
                    viewportHeight = height.value,
                )
                .build()
        val expectedSize = 30.dp
        rule.setGlimmerThemeContent {
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun vector_largerSize() {
        val width = 35.dp
        val height = 83.dp
        val vector =
            ImageVector.Builder(
                    defaultWidth = width,
                    defaultHeight = height,
                    viewportWidth = width.value,
                    viewportHeight = height.value,
                )
                .build()
        val expectedSize = 50.dp
        rule.setGlimmerThemeContent {
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun image_defaultSize() {
        val width = 35.dp
        val height = 83.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }
            Icon(bitmap = image, contentDescription = null, modifier = Modifier.testTag(iconTag))
        }

        // Intrinsic size should be ignored, the actual size should be 40
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun image_localIconSize() {
        val width = 35.dp
        val height = 83.dp
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalIconSize provides 50.dp) {
                val image =
                    with(LocalDensity.current) {
                        ImageBitmap(width.roundToPx(), height.roundToPx())
                    }
                Icon(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.testTag(iconTag),
                )
            }
        }

        // Intrinsic size should be ignored, the actual size should be 50
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun image_smallerSize() {
        val width = 35.dp
        val height = 83.dp
        val expectedSize = 30.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }
            Icon(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun image_largerSize() {
        val width = 35.dp
        val height = 83.dp
        val expectedSize = 50.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }
            Icon(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun painter_noIntrinsicSize_defaultSize() {
        val painter = ColorPainter(Color.Red)
        rule.setGlimmerThemeContent {
            Icon(painter = painter, contentDescription = null, modifier = Modifier.testTag(iconTag))
        }

        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun painter_noIntrinsicSize_localIconSize() {
        val painter = ColorPainter(Color.Red)
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalIconSize provides 50.dp) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.testTag(iconTag),
                )
            }
        }

        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun painter_noIntrinsicSize_smallerSize() {
        val painter = ColorPainter(Color.Red)
        val expectedSize = 30.dp
        rule.setGlimmerThemeContent {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun painter_noIntrinsicSize_largerSize() {
        val painter = ColorPainter(Color.Red)
        val expectedSize = 50.dp
        rule.setGlimmerThemeContent {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun painter_intrinsicSize_defaultSize() {
        val width = 35.dp
        val height = 83.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }

            val painter = BitmapPainter(image)
            Icon(painter = painter, contentDescription = null, modifier = Modifier.testTag(iconTag))
        }

        // Intrinsic size should be ignored, the actual size should be 40
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun painter_intrinsicSize_localIconSize() {
        val width = 35.dp
        val height = 83.dp
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalIconSize provides 50.dp) {
                val image =
                    with(LocalDensity.current) {
                        ImageBitmap(width.roundToPx(), height.roundToPx())
                    }

                val painter = BitmapPainter(image)
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.testTag(iconTag),
                )
            }
        }

        // Intrinsic size should be ignored, the actual size should be 50
        rule.onNodeWithTag(iconTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun painter_intrinsicSize_smallerSize() {
        val width = 35.dp
        val height = 83.dp
        val expectedSize = 30.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }

            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun painter_intrinsicSize_largerSize() {
        val width = 35.dp
        val height = 83.dp
        val expectedSize = 50.dp
        rule.setGlimmerThemeContent {
            val image =
                with(LocalDensity.current) { ImageBitmap(width.roundToPx(), height.roundToPx()) }

            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag).size(expectedSize),
            )
        }

        // Intrinsic size should be ignored, the actual size should be expectedSize
        rule
            .onNodeWithTag(iconTag)
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun vector_scalesToFitSize() {
        // Vector with intrinsic size of 24dp
        val width = 24.dp
        val height = 24.dp
        var expectedIntSize: IntSize? = null
        rule.setGlimmerThemeContent {
            val vector = createVectorWithColor(width, height, Color.Red)
            Icon(
                imageVector = vector,
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be 40x40 and fill the whole size with red pixels
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }
    }

    @Test
    fun image_scalesToFitSize() {
        // Image with intrinsic size of 24dp
        val width = 24.dp
        val height = 24.dp
        var expectedIntSize: IntSize? = null
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            Icon(
                bitmap = image,
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be 40x40 and fill the whole size with red pixels
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }
    }

    @Test
    fun painter_scalesToFitSize() {
        // Image with intrinsic size of 24dp
        val width = 24.dp
        val height = 24.dp
        var expectedIntSize: IntSize? = null
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                tint = null,
                contentDescription = null,
                modifier = Modifier.testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be 40x40 and fill the whole size with red pixels
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }
    }

    @Test
    fun vector_usesContentColorByDefault() {
        // Vector with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var contentColor by mutableStateOf(Color.Blue)
        rule.setGlimmerThemeContent {
            val vector = createVectorWithColor(width, height, Color.Red)
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = contentColor,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted blue from content color
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Blue }

        rule.runOnIdle { contentColor = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from content color
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { contentColor = Color.Unspecified }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // Unspecified content color means that we should not tint the icon, so we will see the
            // underlying red color
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun image_usesContentColorByDefault() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var contentColor by mutableStateOf(Color.Blue)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            Icon(
                bitmap = image,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = contentColor,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted blue from content color
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Blue }

        rule.runOnIdle { contentColor = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from content color
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { contentColor = Color.Unspecified }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // Unspecified content color means that we should not tint the icon, so we will see the
            // underlying red color
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun painter_usesContentColorByDefault() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var contentColor by mutableStateOf(Color.Blue)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = contentColor,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted blue from content color
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Blue }

        rule.runOnIdle { contentColor = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from content color
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { contentColor = Color.Unspecified }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // Unspecified content color means that we should not tint the icon, so we will see the
            // underlying red color
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun vector_explicitTint() {
        // Vector with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf(Color.Green)
        rule.setGlimmerThemeContent {
            val vector = createVectorWithColor(width, height, Color.Red)
            Icon(
                imageVector = vector,
                tint = tint,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted green from the tint
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Green }

        rule.runOnIdle { tint = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }
    }

    @Test
    fun image_explicitTint() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf(Color.Green)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            Icon(
                bitmap = image,
                tint = tint,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted green from the tint
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Green }

        rule.runOnIdle { tint = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }
    }

    @Test
    fun painter_explicitTint() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf(Color.Green)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                tint = { tint },
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be tinted green from the tint
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Green }

        rule.runOnIdle { tint = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }
    }

    @Test
    fun vector_unspecifiedTint() {
        // Vector with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf(Color.Unspecified)
        rule.setGlimmerThemeContent {
            val vector = createVectorWithColor(width, height, Color.Red)
            Icon(
                imageVector = vector,
                tint = tint,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }

        rule.runOnIdle { tint = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { tint = Color.Unspecified }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun image_unspecifiedTint() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf(Color.Unspecified)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            Icon(
                bitmap = image,
                tint = tint,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }

        rule.runOnIdle { tint = Color.Yellow }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { tint = Color.Unspecified }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun painter_nullTint() {
        // Image with intrinsic size of 40dp
        val width = 40.dp
        val height = 40.dp
        var expectedIntSize: IntSize? = null
        var tint by mutableStateOf<ColorProducer?>(null)
        rule.setGlimmerThemeContent {
            val image = createBitmapWithColor(LocalDensity.current, width, height, Color.Red)
            val painter = BitmapPainter(image)
            Icon(
                painter = painter,
                tint = tint,
                contentDescription = null,
                modifier =
                    Modifier.surface(
                            shape = RectangleShape,
                            color = Color.Black,
                            contentColor = Color.Blue,
                            border = null,
                        )
                        .testTag(iconTag),
            )
            with(LocalDensity.current) {
                val dimension = 40.dp.roundToPx()
                expectedIntSize = IntSize(dimension, dimension)
            }
        }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize!!) { Color.Red }

        rule.runOnIdle { tint = ColorProducer { Color.Yellow } }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should now be tinted yellow from the tint
            .assertPixels(expectedSize = expectedIntSize) { Color.Yellow }

        rule.runOnIdle { tint = null }

        rule
            .onNodeWithTag(iconTag)
            .captureToImage()
            // The icon should be red from the underlying vector, no tint should be applied
            .assertPixels(expectedSize = expectedIntSize) { Color.Red }
    }

    @Test
    fun vector_defaultSemanticsWhenContentDescriptionProvided() {
        val testTag = "TestTag"
        rule.setContent {
            Icon(
                imageVector =
                    ImageVector.Builder(
                            defaultWidth = 10.dp,
                            defaultHeight = 10.dp,
                            viewportWidth = 1f,
                            viewportHeight = 1f,
                        )
                        .build(),
                contentDescription = "qwerty",
                modifier = Modifier.testTag(testTag),
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assertContentDescriptionEquals("qwerty")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    @Test
    fun image_defaultSemanticsWhenContentDescriptionProvided() {
        val testTag = "TestTag"
        rule.setContent {
            Icon(
                bitmap = ImageBitmap(100, 100),
                contentDescription = "qwerty",
                modifier = Modifier.testTag(testTag),
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assertContentDescriptionEquals("qwerty")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    @Test
    fun painter_defaultSemanticsWhenContentDescriptionProvided() {
        val testTag = "TestTag"
        rule.setContent {
            Icon(
                painter = BitmapPainter(ImageBitmap(100, 100)),
                contentDescription = "qwerty",
                modifier = Modifier.testTag(testTag),
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assertContentDescriptionEquals("qwerty")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    private fun createVectorWithColor(width: Dp, height: Dp, color: Color): ImageVector {
        return ImageVector.Builder(
                defaultWidth = width,
                defaultHeight = height,
                viewportWidth = 1f,
                viewportHeight = 1f,
            )
            .apply {
                path(fill = SolidColor(color)) {
                    lineTo(x = 1f, y = 0f)
                    lineTo(x = 1f, y = 1f)
                    lineTo(x = 0f, y = 1f)
                    close()
                }
            }
            .build()
    }

    private fun createBitmapWithColor(
        density: Density,
        width: Dp,
        height: Dp,
        color: Color,
    ): ImageBitmap {
        val size = with(density) { Size(width.toPx(), height.toPx()) }
        val image = ImageBitmap(size.width.roundToInt(), size.height.roundToInt())
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), size) {
            drawRect(color)
        }
        return image
    }
}
