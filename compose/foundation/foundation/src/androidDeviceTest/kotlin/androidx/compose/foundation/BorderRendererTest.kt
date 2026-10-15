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

package androidx.compose.foundation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Common tests for [BorderRenderer] that are run against every type of [Outline]. Non-parameterized
 * tests for specific use cases should go in [AdvancedBorderRendererTest] instead.
 *
 * See also [BorderTest], which covers the same rendering through [border].
 */
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderRendererTest(val shape: Shape) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "shape={0}")
        fun initParameters(): Array<Any> =
            arrayOf(
                // Outline.Rectangle
                namedShape("Rectangle", RectangleShape),
                // Outline.Rounded, simple (all corner radii equal)
                namedShape("Circle", CircleShape),
                // Outline.Rounded, simple (all corner radii equal)
                namedShape("Rounded", RoundedCornerShape(5.0f)),
                // Outline.Rounded, non-simple (different corner radii)
                namedShape(
                    "NonSimpleRounded",
                    RoundedCornerShape(
                        topStartPercent = 5,
                        topEndPercent = 10,
                        bottomEndPercent = 15,
                        bottomStartPercent = 20,
                    ),
                ),
                // Outline.Generic
                namedShape("Generic", GenericShape { size, _ -> addRect(Rect(Offset.Zero, size)) }),
            )

        private fun namedShape(name: String, shape: Shape): Shape =
            object : Shape by shape {
                override fun toString(): String = name
            }
    }

    @get:Rule val rule = createComposeRule()

    private val testTag = "BorderParent"

    @Test
    fun drawBorder_solidColor() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }
        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)
    }

    @Test
    fun drawBorder_linearGradientBrush() {
        // A gradient between the same color, so we can assert the color of every pixel
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Red))
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                brush = brush,
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }
        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)
    }

    // RuntimeShader not available below Tiramisu
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun drawBorder_shaderBrush() {
        // Shader that just draws red
        val brush =
            ShaderBrush(
                RuntimeShader("half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }")
            )
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                brush = brush,
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }
        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)
    }

    @Test
    fun drawBorder_biggerThanBounds_fills() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 1500.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            antiAliasingGap = 2.0f,
        )
    }

    @Test
    fun drawBorder_negativeWidth_drawsNothing() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = -5.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = shape,
                backgroundShape = shape,
                antiAliasingGap = 2.0f,
            )
    }

    @Test
    fun drawBorder_unspecifiedWidth_drawsNothing() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = Dp.Unspecified,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = shape,
                backgroundShape = shape,
                antiAliasingGap = 2.0f,
            )
    }

    @Test
    fun drawBorder_zeroSizeBounds_drawsNothing() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(Modifier.size(40.0f.toDp(), 40.0f.toDp()).background(Color.White)) {
                    Box(
                        Modifier.size(0.0f.toDp(), 40.0f.toDp()).drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                    ) {}
                }
            }
        }

        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.White,
            shape = RectangleShape,
            antiAliasingGap = 1.0f,
        )
    }

    @Test
    fun drawBorder_hairlineWidth_drawsOnePixelBorder() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = Dp.Hairline,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        // Nothing should be drawn inside the 1px border. assertBorder ignores pixels within its
        // antialiasing gap of either edge of the border, so this does not check the border itself.
        assertBorder(
            borderColor = Color.Red,
            backgroundColor = Color.Blue,
            innerSize = Size(38f, 38f),
        )
        rule.onNodeWithTag(testTag).captureToImage().toPixelMap().run {
            // The midpoints of each edge lie on the outline for every tested shape. The first
            // pixel should be the border, and the second pixel should be the background. Only
            // check the top and left edges, as the last pixel fails to render properly on some
            // emulators - b/267371353
            assertColorEquals(Color.Red, this[width / 2, 0])
            assertColorEquals(Color.Blue, this[width / 2, 1])
            assertColorEquals(Color.Red, this[0, height / 2])
            assertColorEquals(Color.Blue, this[1, height / 2])
        }
    }

    @Test
    fun drawBorder_fractionalWidth_roundsUpToWholePixels() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                // Should be rounded up to 10px
                                width = 9.25f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        // Nothing should be drawn inside the 10px border. assertBorder ignores pixels within its
        // antialiasing gap of either edge of the border, so this does not check the edge of the
        // border.
        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)
        rule.onNodeWithTag(testTag).captureToImage().toPixelMap().run {
            // The midpoints of each edge lie on the outline for every tested shape. If the width
            // was not rounded up, the 10th pixel would only be partially covered by the border.
            // Only check the top and left edges, as the last pixel fails to render properly on
            // some emulators - b/267371353
            assertColorEquals(Color.Red, this[width / 2, 9])
            assertColorEquals(Color.Blue, this[width / 2, 10])
            assertColorEquals(Color.Red, this[9, height / 2])
            assertColorEquals(Color.Blue, this[10, height / 2])
        }
    }

    @Test
    fun drawBorder_changeWidth() {
        var widthPx by mutableFloatStateOf(4f)
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                // Read during draw, so changes only invalidate drawing
                                width = widthPx.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertBorder(
            borderColor = Color.Red,
            backgroundColor = Color.Blue,
            innerSize = Size(32f, 32f),
        )

        rule.runOnIdle { widthPx = 10f }

        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)

        rule.runOnIdle { widthPx = 4f }

        assertBorder(
            borderColor = Color.Red,
            backgroundColor = Color.Blue,
            innerSize = Size(32f, 32f),
        )
    }

    @Test
    fun drawBorder_changeBrush() {
        var brush: Brush by mutableStateOf(SolidColor(Color.Red))
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                // Read during draw, so changes only invalidate drawing
                                brush = brush,
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)

        rule.runOnIdle { brush = SolidColor(Color.Green) }

        assertBorder(borderColor = Color.Green, backgroundColor = Color.Blue)

        rule.runOnIdle { brush = Brush.linearGradient(listOf(Color.Red, Color.Red)) }

        assertBorder(borderColor = Color.Red, backgroundColor = Color.Blue)
    }

    /**
     * The outline is created for the inset size, so it matches the size of the inset DrawScope.
     * This is the expected usage, and so the result is the same for every outline type.
     */
    @Test
    fun drawBorder_insetDrawScope_outlineCreatedForInsetSize_drawsWithinInsetBounds() {
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(60.0f.toDp(), 60.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            inset(10f) {
                                renderer.drawBorder(
                                    drawScope = this,
                                    width = 10.0f.toDp(),
                                    brush = SolidColor(Color.Red),
                                    // size is the inset size (40x40) here
                                    outline = shape.createOutline(size, layoutDirection, this),
                                )
                            }
                        }
                )
            }
        }

        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        // Border should be drawn within the inset 40x40 bounds
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            backgroundSize = Size(40f, 40f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f,
        )
        // Nothing should be drawn outside of the inset bounds
        val map = bitmap.toPixelMap()
        assertEquals(Color.Blue, map[2, 2])
        assertEquals(Color.Blue, map[57, 57])
        assertEquals(Color.Blue, map[30, 5])
        assertEquals(Color.Blue, map[5, 30])
        assertEquals(Color.Blue, map[30, 54])
        assertEquals(Color.Blue, map[54, 30])
    }

    @Test
    fun drawBorder_multipleRenderers_sameDraw() {
        rule.setContent {
            val outerGraphicsLayer = rememberGraphicsLayer()
            val innerGraphicsLayer = rememberGraphicsLayer()
            val outerRenderer =
                remember(outerGraphicsLayer) { BorderRenderer { outerGraphicsLayer } }
            val innerRenderer =
                remember(innerGraphicsLayer) { BorderRenderer { innerGraphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            outerRenderer.drawBorder(
                                drawScope = this,
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                            inset(10f) {
                                innerRenderer.drawBorder(
                                    drawScope = this,
                                    width = 4.0f.toDp(),
                                    brush = SolidColor(Color.Green),
                                    outline = shape.createOutline(size, layoutDirection, this),
                                )
                            }
                        }
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().toPixelMap().run {
            // Midpoints of each edge lie on the outline for every tested shape, so assert along
            // the top and left edges towards the center.
            // Outer border: [0, 4)
            assertEquals(Color.Red, this[20, 1])
            assertEquals(Color.Red, this[1, 20])
            // Gap between borders: [4, 10)
            assertEquals(Color.Blue, this[20, 7])
            assertEquals(Color.Blue, this[7, 20])
            // Inner border: [10, 14)
            assertEquals(Color.Green, this[20, 12])
            assertEquals(Color.Green, this[12, 20])
            // Center
            assertEquals(Color.Blue, this[20, 20])
        }
    }

    /**
     * Asserts a border of [borderColor] around an inner area of [innerSize] filled with
     * [backgroundColor], for a 40x40 layout. The default values match a 10px border.
     */
    private fun assertBorder(
        borderColor: Color,
        backgroundColor: Color,
        innerSize: Size = Size(20f, 20f),
        shape: Shape = this.shape,
    ) {
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = borderColor,
                shape = shape,
                backgroundShape = shape,
                shapeSize = innerSize,
                shapeColor = backgroundColor,
                antiAliasingGap = 3.0f,
            )
    }

    /**
     * Asserts that [actual] is within [tolerance] of [expected] for each channel, to allow for
     * antialiasing differences.
     */
    private fun assertColorEquals(expected: Color, actual: Color, tolerance: Float = 0.1f) {
        assertEquals(expected.red, actual.red, tolerance)
        assertEquals(expected.green, actual.green, tolerance)
        assertEquals(expected.blue, actual.blue, tolerance)
        assertEquals(expected.alpha, actual.alpha, tolerance)
    }

    @Composable
    private fun SemanticParent(content: @Composable Density.() -> Unit) {
        Box { Box(modifier = Modifier.testTag(testTag)) { LocalDensity.current.content() } }
    }
}
