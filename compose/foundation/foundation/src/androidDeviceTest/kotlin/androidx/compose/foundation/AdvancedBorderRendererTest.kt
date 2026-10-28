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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.floor
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [BorderRenderer] covering specific use cases. Tests that should run against every
 * outline type should go in [BorderRendererTest] instead.
 *
 * See also [AdvancedBorderTest], which covers the same cases through [border].
 */
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class AdvancedBorderRendererTest {

    @get:Rule val rule = createComposeRule()

    val testTag = "BorderParent"

    @Test
    fun drawBorder_triangle_shape() {
        val triangle = GenericShape { size, _ ->
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            val triangleSizeDp: Dp
            val borderWidthDp: Dp
            with(LocalDensity.current) {
                triangleSizeDp = (100f / density).dp
                borderWidthDp = (10f / density).dp
            }
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(triangleSizeDp, triangleSizeDp)
                    .background(Color.White)
                    .drawWithContent {
                        drawContent()
                        renderer.drawBorder(
                            drawScope = this,
                            width = borderWidthDp,
                            brush = SolidColor(Color.Red),
                            outline = triangle.createOutline(size, layoutDirection, this),
                        )
                    }
            )
        }

        val offsetLeft = 5
        val offsetRight = 2
        val offsetTop = 2
        val offsetBottom = 5
        rule.onNodeWithTag(testTag).captureToImage().apply {
            val map = toPixelMap()
            assertEquals(Color.Red, map[offsetLeft, offsetTop]) // Top left
            assertEquals(Color.Red, map[width - offsetRight, offsetTop]) // Top right
            assertEquals(Color.Red, map[width - offsetRight, height - offsetBottom]) // Bottom right
            // inside triangle
            assertEquals(Color.White, map[floor(width * 3f / 4f).toInt(), height / 2])
        }
    }

    @Test
    fun drawBorder_non_simple_rounded_rect() {
        val topleft = 0f
        val topRight = 5f
        val bottomRight = 10f
        val bottomLeft = 15f
        val roundRectTag = "roundRectTag"
        var borderPx = 0f
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            val border = 10.dp

            with(LocalDensity.current) { borderPx = border.toPx() }
            Box(
                Modifier.testTag(roundRectTag).size(50.dp).background(Color.White).drawWithContent {
                    drawContent()
                    renderer.drawBorder(
                        drawScope = this,
                        width = border,
                        brush = SolidColor(Color.Red),
                        outline =
                            RoundedCornerShape(
                                    topStart = topleft,
                                    topEnd = topRight,
                                    bottomStart = bottomLeft,
                                    bottomEnd = bottomRight,
                                )
                                .createOutline(this.size, layoutDirection, this),
                    )
                }
            )
        }

        rule.onNodeWithTag(roundRectTag).captureToImage().apply {
            val map = toPixelMap()
            val offset = 2
            assertEquals(Color.Red, map[offset, offset])
            assertEquals(Color.White, map[width - 1, 0])
            assertEquals(Color.White, map[width - 1, height - 1])
            assertEquals(Color.White, map[0, height - 1])

            assertEquals(Color.White, map[borderPx.toInt() + offset, borderPx.toInt() + offset])
            assertEquals(
                Color.White,
                map[map.width - borderPx.toInt() - offset, borderPx.toInt() + offset],
            )
            assertEquals(
                Color.White,
                map[map.width - borderPx.toInt() - offset, map.height - borderPx.toInt() - offset],
            )
            assertEquals(
                Color.White,
                map[borderPx.toInt() + offset, map.height - borderPx.toInt() - offset],
            )

            val topRightOffset = (topRight / 2).roundToInt()
            assertEquals(Color.Red, map[width - 1 - topRightOffset, topRightOffset])

            val bottomRightOffset = (bottomRight / 2).roundToInt()
            assertEquals(
                Color.Red,
                map[width - 1 - bottomRightOffset, height - 1 - bottomRightOffset],
            )

            val bottomLeftOffset = (bottomLeft / 2).roundToInt()
            assertEquals(Color.Red, map[bottomLeftOffset, height - 1 - bottomLeftOffset])
        }
    }

    @Test
    fun drawBorder_color_with_alpha_tinted_properly() {
        val background = Color.Red
        val borderColor = Color.Blue.copy(alpha = 0.5f)
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(100.dp, 100.dp)
                    .background(background)
                    .drawWithContent {
                        drawContent()
                        renderer.drawBorder(
                            drawScope = this,
                            width = 10.dp,
                            brush = SolidColor(borderColor),
                            outline =
                                GenericShape { size, _ ->
                                        lineTo(size.width, 0f)
                                        lineTo(size.width, size.height)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                    .createOutline(size, layoutDirection, this),
                        )
                    }
            )
        }

        fun assertColorEquals(expected: Color, actual: Color) {
            assertEquals(expected.red, actual.red, 0.01f)
            assertEquals(expected.green, actual.green, 0.01f)
            assertEquals(expected.blue, actual.blue, 0.01f)
            assertEquals(expected.alpha, actual.alpha, 0.01f)
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            val pixelMap = toPixelMap()
            val expected = borderColor.compositeOver(background)
            assertColorEquals(expected, pixelMap[0, 0])
            assertColorEquals(expected, pixelMap[width - 1, 0])
            assertColorEquals(expected, pixelMap[width - 1, height - 1])
            assertColorEquals(expected, pixelMap[0, height - 1])
        }
    }

    @Test
    fun drawBorder_generic_shape_color_to_brush() {
        // Verify that rendering with a solid color initially then with a gradient
        // updates the internal offscreen bitmap config to Argb8888 from Alpha8 (when using an
        // ImageBitmap). For graphics layer implementations, this should work normally.
        val gradient =
            Brush.verticalGradient(
                0.0f to Color.Red,
                0.5f to Color.Red,
                0.5f to Color.Blue,
                1.0f to Color.Blue,
            )
        val borderStrokeDp = 5.dp
        var borderStrokePx = 0f
        val toggle = mutableStateOf(false)
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            val testShape = GenericShape { size, _ ->
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            with(LocalDensity.current) { borderStrokePx = borderStrokeDp.toPx() }
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(20.dp, 20.dp)
                    .background(Color.White)
                    .drawWithContent {
                        drawContent()
                        renderer.drawBorder(
                            drawScope = this,
                            width = borderStrokeDp,
                            brush = if (toggle.value) gradient else SolidColor(Color.Green),
                            outline = testShape.createOutline(size, layoutDirection, this),
                        )
                    }
            )
        }

        val halfBorderStrokePx = (borderStrokePx / 2).toInt()
        rule.onNodeWithTag(testTag).captureToImage().apply {
            val pixelMap = toPixelMap()
            assertEquals(Color.Green, pixelMap[halfBorderStrokePx, halfBorderStrokePx])
            assertEquals(Color.Green, pixelMap[width - halfBorderStrokePx, halfBorderStrokePx])
            assertEquals(Color.Green, pixelMap[halfBorderStrokePx, height - halfBorderStrokePx])
            assertEquals(
                Color.Green,
                pixelMap[width - halfBorderStrokePx, height - halfBorderStrokePx],
            )
            assertEquals(Color.White, pixelMap[width / 2, height / 2])
        }

        rule.runOnIdle { toggle.value = !toggle.value }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            val pixelMap = toPixelMap()
            assertEquals(Color.Red, pixelMap[halfBorderStrokePx, halfBorderStrokePx])
            assertEquals(Color.Red, pixelMap[width - halfBorderStrokePx, halfBorderStrokePx])
            assertEquals(Color.Blue, pixelMap[halfBorderStrokePx, height - halfBorderStrokePx])
            assertEquals(
                Color.Blue,
                pixelMap[width - halfBorderStrokePx, height - halfBorderStrokePx],
            )
            assertEquals(Color.White, pixelMap[width / 2, height / 2])
        }
    }

    @Test
    fun drawBorder_test_with_fixed_size_generic_shape() {
        val bubbleWidthDp = 80.dp
        val bubbleHeightDp = 40.dp
        val borderWidthDp = 10.dp
        val arrowBaseWidthDp = 24.dp
        val arrowTipDp: Dp = 56.dp
        val arrowLengthDp: Dp = 12.dp
        var offset: Offset = Offset.Zero
        var arrowTipPx = 0f
        var arrowBaseWidthPx = 0f
        var arrowLengthPx = 0f
        var borderStrokePx = 0f
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            val bubbleWithArrow =
                calculateContainerShape(
                    LocalDensity.current,
                    arrowBaseWidthDp,
                    arrowTipDp,
                    arrowLengthDp,
                )
            with(LocalDensity.current) {
                val outline =
                    bubbleWithArrow.createOutline(
                        Size(bubbleWidthDp.toPx(), bubbleHeightDp.toPx()),
                        LayoutDirection.Ltr,
                        this,
                    ) as Outline.Generic

                val pathBounds = outline.path.getBounds()
                offset = Offset(-pathBounds.left, -pathBounds.top)

                arrowTipPx = arrowTipDp.toPx()
                arrowBaseWidthPx = arrowBaseWidthDp.toPx()
                arrowLengthPx = arrowLengthDp.toPx()
                borderStrokePx = borderWidthDp.toPx()
            }
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(bubbleWidthDp, bubbleHeightDp)
                    .background(Color.White)
                    .padding(top = arrowLengthDp)
                    .drawWithContent {
                        drawContent()
                        renderer.drawBorder(
                            drawScope = this,
                            width = borderWidthDp,
                            brush = SolidColor(Color.Red),
                            outline = bubbleWithArrow.createOutline(size, layoutDirection, this),
                        )
                    }
            )
        }

        val arrowTipX = arrowTipPx / 2
        rule.onNodeWithTag(testTag).captureToImage().apply {
            val map = toPixelMap()

            // point along the rounded rect but before the triangle is drawn with the border
            var currentX = arrowTipX + arrowBaseWidthPx / 2f - arrowBaseWidthPx
            assertEquals(Color.Red, map[currentX.toInt(), (offset.y + borderStrokePx / 2).toInt()])

            // point halfway up the start of the triangle is drawn within the border
            assertEquals(
                Color.Red,
                map[
                    (currentX + arrowBaseWidthPx / 4).toInt(),
                    (offset.y - arrowLengthPx / 2 + borderStrokePx / 2).toInt()],
            )

            // Tip of the triangle is drawn within the border
            currentX += arrowBaseWidthPx / 2f
            assertEquals(
                Color.Red,
                map[currentX.toInt(), (offset.y - arrowLengthPx + borderStrokePx / 2).toInt()],
            )

            // rounded rectangle directly below the triangle does not have the border rendered
            assertEquals(
                Color.White,
                map[currentX.toInt(), (offset.y + borderStrokePx / 2).toInt()],
            )

            // Midpoint of the end of the triangle being drawn back into the rounded rect
            // has the border rendered
            assertEquals(
                Color.Red,
                map[
                    (currentX + arrowBaseWidthPx / 4f).toInt(),
                    (offset.y - arrowLengthPx / 4 + borderStrokePx / 2).toInt()],
            )

            // Base of the triangle on the rounded rect shape has the border rendered
            currentX += arrowBaseWidthPx / 2f
            assertEquals(Color.Red, map[currentX.toInt(), (offset.y + borderStrokePx / 2).toInt()])
        }
    }

    @Test
    fun drawBorder_rectangleOutline_respectsOutlineBounds() {
        val fixedRectOutlineShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Rectangle(Rect(10f, 10f, 30f, 30f))
            }

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
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    fixedRectOutlineShape.createOutline(
                                        size,
                                        layoutDirection,
                                        this,
                                    ),
                            )
                        }
                ) {}
            }
        }

        // This should draw a 4px rectangle starting at (10,10)
        val outlineBounds = IntRect(10, 10, 30, 30)
        val inset = IntRect(14, 14, 26, 26)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            // Inside the border, and outside the bounds of the border should be blue. The border
            // should be red.
            if (outlineBounds.contains(pos) && !inset.contains(pos)) Color.Red else Color.Blue
        }
    }

    @Test
    fun drawBorder_simpleRoundedOutline_respectsOutlineBounds() {
        val fixedSimpleRoundedOutlineShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(RoundRect(10f, 10f, 30f, 30f, cornerRadius = CornerRadius(8f)))
            }

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
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    fixedSimpleRoundedOutlineShape.createOutline(
                                        size,
                                        layoutDirection,
                                        this,
                                    ),
                            )
                        }
                ) {}
            }
        }

        // This should draw a 4px rounded rectangle starting at (10,10)
        val outlineBounds = IntRect(10, 10, 30, 30)
        val inset = IntRect(14, 14, 26, 26)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            val inCornerSquare = (pos.x !in 18..<22) && (pos.y !in 18..<22)
            val inExtremeCorner = (pos.x !in 12..<28) && (pos.y !in 12..<28)
            when {
                // Outside the bounds of the border should be blue
                !outlineBounds.contains(pos) -> Color.Blue
                // The extreme corner pixels of the outline lie outside the rounded corners of
                // the ring, so they show the background
                inExtremeCorner -> Color.Blue
                // Ignore the corner due to antialiasing
                inCornerSquare -> null
                // Inside should be blue
                inset.contains(pos) -> Color.Blue
                // Border should be red
                else -> Color.Red
            }
        }
    }

    /**
     * Border should respect the outline bounds when using [Outline.Rounded], with a non-simple
     * (different corner radii) outline that starts at (0,0).
     *
     * This test case is separate from
     * [drawBorder_nonSimpleRoundedOutline_nonZeroOffsetOriginOutline_respectsOutlineBounds], which
     * is the same test case but with an outline that does _not_ start at (0,0), to catch
     * b/546074983
     */
    @Test
    fun drawBorder_nonSimpleRoundedOutline_zeroOffsetOriginOutline_respectsOutlineBounds() {
        val fixedOutlineShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(0f, 0f, 20f, 20f, bottomRightCornerRadius = CornerRadius(8f))
                    )
            }

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
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    fixedOutlineShape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }
        val outlineBounds = IntRect(0, 0, 20, 20)
        // Outline inset by the stroke width
        val inset = IntRect(4, 4, 16, 16)
        // The bottom right corner is rounded and so will be antialiased, so we want to ignore it
        val outlineCorner = IntRect(12, 12, 20, 20)
        // The extreme corner pixels lie fully outside the rounded corner
        val outlineCornerTip = IntRect(18, 18, 20, 20)
        // This should draw a 4px rectangle, with the bottom right corner rounded, starting at (0,0)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            when {
                // The extreme corner pixels of the outline lie outside the rounded corner of
                // the ring, so they show the background
                outlineCornerTip.contains(pos) -> Color.Blue
                // Ignore the rest of the corner due to antialiasing
                outlineCorner.contains(pos) -> null
                // Border should be red
                outlineBounds.contains(pos) && !inset.contains(pos) -> Color.Red
                // Inside the border, and outside the bounds of the border should be blue
                else -> Color.Blue
            }
        }
    }

    /**
     * Border should respect the outline bounds when using [Outline.Rounded], with a non-simple
     * (different corner radii) outline that does _not_ start at (0,0).
     *
     * This test case is separate from
     * [drawBorder_nonSimpleRoundedOutline_zeroOffsetOriginOutline_respectsOutlineBounds], which is
     * the same test case but with an outline that _does_ start at (0,0), to catch b/546074983
     */
    @Test
    fun drawBorder_nonSimpleRoundedOutline_nonZeroOffsetOriginOutline_respectsOutlineBounds() {
        val fixedNonSimpleRoundedOutlineShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(
                        RoundRect(10f, 10f, 30f, 30f, bottomRightCornerRadius = CornerRadius(8f))
                    )
            }

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
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    fixedNonSimpleRoundedOutlineShape.createOutline(
                                        size,
                                        layoutDirection,
                                        this,
                                    ),
                            )
                        }
                ) {}
            }
        }

        val outlineBounds = IntRect(10, 10, 30, 30)
        // Outline inset by the stroke width
        val inset = IntRect(14, 14, 26, 26)
        // The bottom right corner is rounded and so will be antialiased, so we want to ignore it
        val outlineCorner = IntRect(22, 22, 30, 30)
        // The extreme corner pixels lie fully outside the rounded corner
        val outlineCornerTip = IntRect(28, 28, 30, 30)
        // This should draw a 4px rectangle, with the bottom right corner rounded, starting at
        // (10,10)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            when {
                // The extreme corner pixels of the outline lie outside the rounded corner of
                // the ring, so they show the background
                outlineCornerTip.contains(pos) -> Color.Blue
                // Ignore the rest of the corner due to antialiasing
                outlineCorner.contains(pos) -> null
                // Border should be red
                outlineBounds.contains(pos) && !inset.contains(pos) -> Color.Red
                // Inside the border, and outside the bounds of the border should be blue
                else -> Color.Blue
            }
        }
    }

    @Test
    fun drawBorder_outlineGeneric_respectsOutlineSize() {
        val fixedGenericOutlineShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Generic(Path().apply { addRect(Rect(10f, 10f, 30f, 30f)) })
            }

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
                                width = 4.0f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    fixedGenericOutlineShape.createOutline(
                                        size,
                                        layoutDirection,
                                        this,
                                    ),
                            )
                        }
                ) {}
            }
        }

        // This should draw a 4px rectangle starting at (10,10)
        val outlineBounds = IntRect(10, 10, 30, 30)
        val inset = IntRect(14, 14, 26, 26)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            // Inside the border, and outside the bounds of the border should be blue. The border
            // should be red.
            if (outlineBounds.contains(pos) && !inset.contains(pos)) Color.Red else Color.Blue
        }
    }

    /**
     * Variant of [drawBorder_rectangleOutline_respectsOutlineBounds] that includes inset logic.
     *
     * The inset translates the border, and the outline bounds are respected, so the border is drawn
     * at the full size of the outline, offset by the inset.
     */
    @Test
    fun drawBorder_insetDrawScope_rectangleOutlineCreatedForFullSize_respectsOutlineBounds() {
        val shape = RectangleShape
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Outline for the full 40x40 size, not the inset 20x20 size
                            val outline = shape.createOutline(size, layoutDirection, this)
                            inset(10f) {
                                renderer.drawBorder(
                                    drawScope = this,
                                    width = 4.0f.toDp(),
                                    brush = SolidColor(Color.Red),
                                    outline = outline,
                                )
                            }
                        }
                ) {}
            }
        }

        // This should draw a 4px border starting at (10,10), at the full 40x40 outline size. The
        // right and bottom edges of the border are at (46,46) - (50,50), outside of the layout
        // bounds.
        val outlineBounds = IntRect(10, 10, 50, 50)
        val inset = IntRect(14, 14, 46, 46)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            // Inside the border, and outside the bounds of the border should be blue. The border
            // should be red.
            if (outlineBounds.contains(pos) && !inset.contains(pos)) Color.Red else Color.Blue
        }
    }

    /**
     * Variant of [drawBorder_simpleRoundedOutline_respectsOutlineBounds] that includes inset logic.
     *
     * The inset translates the border, and the outline bounds are respected, so the border is drawn
     * at the full size of the outline, offset by the inset.
     */
    @Test
    fun drawBorder_insetDrawScope_simpleRoundedOutlineCreatedForFullSize_respectsOutlineBounds() {
        val shape = RoundedCornerShape(8f)
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Outline for the full 40x40 size, not the inset 20x20 size
                            val outline = shape.createOutline(size, layoutDirection, this)
                            inset(10f) {
                                renderer.drawBorder(
                                    drawScope = this,
                                    width = 4.0f.toDp(),
                                    brush = SolidColor(Color.Red),
                                    outline = outline,
                                )
                            }
                        }
                ) {}
            }
        }

        // This should draw a 4px border starting at (10,10), at the full 40x40 outline size, with
        // 8px rounded corners. The right and bottom edges of the border are at (46,46) - (50,50),
        // outside of the layout bounds, so only the top left rounded corner is visible.
        val outlineBounds = IntRect(10, 10, 50, 50)
        val inset = IntRect(14, 14, 46, 46)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            val inCornerSquare = (pos.x in 10..<18) && (pos.y in 10..<18)
            val inExtremeCorner = (pos.x in 10..<12) && (pos.y in 10..<12)
            when {
                // Outside the bounds of the border should be blue
                !outlineBounds.contains(pos) -> Color.Blue
                // The extreme corner pixels of the outline lie outside the rounded corner of the
                // ring, so they show the background
                inExtremeCorner -> Color.Blue
                // Ignore the corner due to antialiasing
                inCornerSquare -> null
                // Inside should be blue
                inset.contains(pos) -> Color.Blue
                // Border should be red
                else -> Color.Red
            }
        }
    }

    /**
     * Variant of [drawBorder_nonSimpleRoundedOutline_zeroOffsetOriginOutline_respectsOutlineBounds]
     * that includes inset logic.
     *
     * The inset translates the border, and the outline bounds are respected, so the border is drawn
     * at the full size of the outline, offset by the inset.
     */
    @Test
    fun drawBorder_insetDrawScope_nonSimpleRoundedOutlineCreatedForFullSize_respectsOutlineBounds() {
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    Outline.Rounded(RoundRect(Rect(Offset.Zero, size), topLeft = CornerRadius(8f)))
            }
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Outline for the full 40x40 size, not the inset 20x20 size
                            val outline = shape.createOutline(size, layoutDirection, this)
                            inset(10f) {
                                renderer.drawBorder(
                                    drawScope = this,
                                    width = 4.0f.toDp(),
                                    brush = SolidColor(Color.Red),
                                    outline = outline,
                                )
                            }
                        }
                ) {}
            }
        }

        // This should draw a 4px border starting at (10,10), at the full 40x40 outline size, with
        // the top left corner rounded. The right and bottom edges of the border are at
        // (46,46) - (50,50), outside of the layout bounds.
        val outlineBounds = IntRect(10, 10, 50, 50)
        val inset = IntRect(14, 14, 46, 46)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            val inCornerSquare = (pos.x in 10..<18) && (pos.y in 10..<18)
            val inExtremeCorner = (pos.x in 10..<12) && (pos.y in 10..<12)
            when {
                // Outside the bounds of the border should be blue
                !outlineBounds.contains(pos) -> Color.Blue
                // The extreme corner pixels of the outline lie outside the rounded corner of the
                // ring, so they show the background
                inExtremeCorner -> Color.Blue
                // Ignore the rest of the corner due to antialiasing
                inCornerSquare -> null
                // Inside should be blue
                inset.contains(pos) -> Color.Blue
                // Border should be red
                else -> Color.Red
            }
        }
    }

    /**
     * Variant of [drawBorder_outlineGeneric_respectsOutlineSize] that includes inset logic.
     *
     * The inset translates the border, and the outline bounds are respected, so the border is drawn
     * at the full size of the outline, offset by the inset.
     */
    @Test
    fun drawBorder_insetDrawScope_genericOutlineCreatedForFullSize_respectsOutlineBounds() {
        val shape = GenericShape { size, _ -> addRect(Rect(Offset.Zero, size)) }
        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Outline for the full 40x40 size, not the inset 20x20 size
                            val outline = shape.createOutline(size, layoutDirection, this)
                            inset(10f) {
                                renderer.drawBorder(
                                    drawScope = this,
                                    width = 4.0f.toDp(),
                                    brush = SolidColor(Color.Red),
                                    outline = outline,
                                )
                            }
                        }
                ) {}
            }
        }

        // This should draw a 4px border starting at (10,10), at the full 40x40 outline size. The
        // right and bottom edges of the border are at (46,46) - (50,50), outside of the layout
        // bounds.
        val outlineBounds = IntRect(10, 10, 50, 50)
        val inset = IntRect(14, 14, 46, 46)
        rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
            // Inside the border, and outside the bounds of the border should be blue. The border
            // should be red.
            if (outlineBounds.contains(pos) && !inset.contains(pos)) Color.Red else Color.Blue
        }
    }

    @Test
    fun drawBorder_changeShape() {
        var roundedCorners by mutableStateOf(false)

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
                                width = 1f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    (if (roundedCorners) RoundedCornerShape(5f) else RectangleShape)
                                        .createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should not be rounded, so left edge should be fully red
            assertEquals(Color.Red, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Red, map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = true }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should now be rounded, so the top and bottom of the left edge will be blue, but
            // the center will still be red
            assertEquals(Color.Blue, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Blue, map[0, height - 1])
        }
    }

    @Test
    fun drawBorder_changeOutline_outlineRounded_simple_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    // Simple rounded shape (same corner radii)
                    val roundRect =
                        if (roundedCorners) {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius(5f))
                        } else {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius.Zero)
                        }
                    return Outline.Rounded(roundRect)
                }
            }

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
                                width = 1f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    roundedCornersShape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should not be rounded, so left edge should be fully red
            assertEquals(Color.Red, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Red, map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = true }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should now be rounded, so the top and bottom of the left edge will be blue, but
            // the center will still be red
            assertEquals(Color.Blue, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Blue, map[0, height - 1])
        }
    }

    @Test
    fun drawBorder_changeOutline_outlineRounded_nonSimple_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    val roundRect =
                        if (roundedCorners) {
                            // Different radii for each corner, so this is not a 'simple' RoundRect
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(5f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius.Zero,
                                bottomLeft = CornerRadius(10f),
                            )
                        } else {
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius.Zero,
                                topRight = CornerRadius(5f),
                                bottomRight = CornerRadius(10f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        }
                    return Outline.Rounded(roundRect)
                }
            }

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
                                width = 1f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    roundedCornersShape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // Only the right corners should be rounded, so left edge should be fully red
            assertEquals(Color.Red, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Red, map[0, height - 1])
            // The right corners are rounded, so they should be blue, but the center will still be
            // red
            assertEquals(Color.Blue, map[width - 1, 0])
            assertEquals(Color.Red, map[width - 1, (height - 1) / 2])
            assertEquals(Color.Blue, map[width - 1, height - 1])
        }

        rule.runOnIdle { roundedCorners = true }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // The left corners are now rounded, so the top and bottom of the left edge will be
            // blue, but the center will still be red
            assertEquals(Color.Blue, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            assertEquals(Color.Blue, map[0, height - 1])
            // The right corners are not rounded, so should still be red
            assertEquals(Color.Red, map[width - 1, 0])
            assertEquals(Color.Red, map[width - 1, (height - 1) / 2])
            assertEquals(Color.Red, map[width - 1, height - 1])
        }
    }

    @Test
    fun drawBorder_changeOutline_outlineGeneric_samePath_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                val path = Path()

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    val roundRect =
                        if (roundedCorners) {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius(50f))
                        } else {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius.Zero)
                        }
                    path.reset()
                    path.addRoundRect(roundRect)
                    return Outline.Generic(path)
                }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(400.0f.toDp(), 400.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 1f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    roundedCornersShape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should not be rounded, so left edge should be fully red
            assertEquals(Color.Red, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertEquals(Color.Red, map[0, height - 2])
        }

        rule.runOnIdle { roundedCorners = true }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should now be rounded, so the top and bottom of the left edge will be blue, but
            // the center will still be red
            assertEquals(Color.Blue, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertEquals(Color.Blue, map[0, height - 2])
        }
    }

    @Test
    fun drawBorder_changeOutline_outlineGeneric_newPath_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    val roundRect =
                        if (roundedCorners) {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius(50f))
                        } else {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius.Zero)
                        }
                    return Outline.Generic(Path().apply { addRoundRect(roundRect) })
                }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            SemanticParent {
                Box(
                    Modifier.size(400.0f.toDp(), 400.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 1f.toDp(),
                                brush = SolidColor(Color.Red),
                                outline =
                                    roundedCornersShape.createOutline(size, layoutDirection, this),
                            )
                        }
                ) {}
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should not be rounded, so left edge should be fully red
            assertEquals(Color.Red, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertEquals(Color.Red, map[0, height - 2])
        }

        rule.runOnIdle { roundedCorners = true }

        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            // We should now be rounded, so the top and bottom of the left edge will be blue, but
            // the center will still be red
            assertEquals(Color.Blue, map[0, 0])
            assertEquals(Color.Red, map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertEquals(Color.Blue, map[0, height - 2])
        }
    }

    @Test
    fun drawBorder_changeOutlineType_rectangleToSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    } else {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_rectangleToNonSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    } else {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_rectangleToGeneric() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    } else {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_simpleRoundedToRectangle() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    } else {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_simpleRoundedToNonSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    } else {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_simpleRoundedToGeneric() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    } else {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_nonSimpleRoundedToRectangle() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    } else {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_nonSimpleRoundedToSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    } else {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_nonSimpleRoundedToGeneric() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    } else {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_genericToRectangle() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    } else {
                        // Outline.Rectangle
                        Outline.Rectangle(Rect(Offset.Zero, size))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = false,
            bottomRight = false,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_genericToSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    } else {
                        // simple Outline.Rounded: all corners rounded
                        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(50f)))
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = true,
            bottomRight = true,
            bottomLeft = true,
        )
    }

    @Test
    fun drawBorder_changeOutlineType_genericToNonSimpleRounded() {
        var changed by mutableStateOf(false)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline =
                    if (!changed) {
                        // Outline.Generic: top right and bottom left corners rounded
                        Outline.Generic(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        Rect(Offset.Zero, size),
                                        topLeft = CornerRadius.Zero,
                                        topRight = CornerRadius(50f),
                                        bottomRight = CornerRadius.Zero,
                                        bottomLeft = CornerRadius(50f),
                                    )
                                )
                            }
                        )
                    } else {
                        // non-simple Outline.Rounded: top left and bottom right corners rounded
                        Outline.Rounded(
                            RoundRect(
                                Rect(Offset.Zero, size),
                                topLeft = CornerRadius(50f),
                                topRight = CornerRadius.Zero,
                                bottomRight = CornerRadius(50f),
                                bottomLeft = CornerRadius.Zero,
                            )
                        )
                    }
            }

        rule.setContent {
            val graphicsLayer = rememberGraphicsLayer()
            val renderer = remember(graphicsLayer) { BorderRenderer { graphicsLayer } }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.background(color = Color.Blue)
                        .padding(8.dp)
                        .testTag(testTag)
                        .size(64.dp)
                        .drawWithContent {
                            drawContent()
                            renderer.drawBorder(
                                drawScope = this,
                                width = 2.dp,
                                brush = SolidColor(Color.Red),
                                outline = shape.createOutline(size, layoutDirection, this),
                            )
                        }
                )
            }
        }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = false,
            topRight = true,
            bottomRight = false,
            bottomLeft = true,
        )

        rule.runOnIdle { changed = true }

        assertRoundedCorners(
            backgroundColor = Color.Blue,
            borderColor = Color.Red,
            topLeft = true,
            topRight = false,
            bottomRight = true,
            bottomLeft = false,
        )
    }

    @Test
    fun drawBorder_rectangleOutline_sameOutline_changeWidth() {
        val outline = Outline.Rectangle(Rect(0f, 0f, 40f, 40f))
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
                                width = widthPx.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(widthPx: Int) {
            val inset = IntRect(widthPx, widthPx, 40 - widthPx, 40 - widthPx)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                if (inset.contains(pos)) Color.Blue else Color.Red
            }
        }

        assertBorder(widthPx = 4)

        rule.runOnIdle { widthPx = 10f }

        assertBorder(widthPx = 10)

        rule.runOnIdle { widthPx = 4f }

        assertBorder(widthPx = 4)
    }

    @Test
    fun drawBorder_simpleRoundedOutline_sameOutline_changeWidth() {
        val outline = Outline.Rounded(RoundRect(0f, 0f, 40f, 40f, cornerRadius = CornerRadius(8f)))
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
                                width = widthPx.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(widthPx: Int) {
            val inset = IntRect(widthPx, widthPx, 40 - widthPx, 40 - widthPx)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                val inCornerSquare = (pos.x !in 8..<32) && (pos.y !in 8..<32)
                val inExtremeCorner = (pos.x !in 2..<38) && (pos.y !in 2..<38)
                when {
                    // The extreme corner pixels lie outside the rounded corners of the ring,
                    // so they show the background
                    inExtremeCorner -> Color.Blue
                    // Ignore the rest of the rounded corners due to antialiasing
                    inCornerSquare -> null
                    inset.contains(pos) -> Color.Blue
                    else -> Color.Red
                }
            }
        }

        assertBorder(widthPx = 4)

        rule.runOnIdle { widthPx = 10f }

        assertBorder(widthPx = 10)

        rule.runOnIdle { widthPx = 4f }

        assertBorder(widthPx = 4)
    }

    @Test
    fun drawBorder_nonSimpleRoundedOutline_sameOutline_changeWidth() {
        val outline =
            Outline.Rounded(RoundRect(0f, 0f, 40f, 40f, bottomRightCornerRadius = CornerRadius(8f)))
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
                                width = widthPx.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(widthPx: Int) {
            val inset = IntRect(widthPx, widthPx, 40 - widthPx, 40 - widthPx)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                val inBottomRightCornerSquare = pos.x >= 32 && pos.y >= 32
                val inBottomRightExtremeCorner = pos.x >= 38 && pos.y >= 38
                when {
                    // The extreme corner pixels lie outside the rounded corner of the ring, so
                    // they show the background
                    inBottomRightExtremeCorner -> Color.Blue
                    // Ignore the rest of the rounded corner due to antialiasing
                    inBottomRightCornerSquare -> null
                    inset.contains(pos) -> Color.Blue
                    else -> Color.Red
                }
            }
        }

        assertBorder(widthPx = 4)

        rule.runOnIdle { widthPx = 10f }

        assertBorder(widthPx = 10)

        rule.runOnIdle { widthPx = 4f }

        assertBorder(widthPx = 4)
    }

    @Test
    fun drawBorder_genericOutline_sameOutline_changeWidth() {
        val outline = Outline.Generic(Path().apply { addRect(Rect(0f, 0f, 40f, 40f)) })
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
                                width = widthPx.toDp(),
                                brush = SolidColor(Color.Red),
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(widthPx: Int) {
            val inset = IntRect(widthPx, widthPx, 40 - widthPx, 40 - widthPx)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                if (inset.contains(pos)) Color.Blue else Color.Red
            }
        }

        assertBorder(widthPx = 4)

        rule.runOnIdle { widthPx = 10f }

        assertBorder(widthPx = 10)

        rule.runOnIdle { widthPx = 4f }

        assertBorder(widthPx = 4)
    }

    @Test
    fun drawBorder_rectangleOutline_sameOutline_changeBrush() {
        val outline = Outline.Rectangle(Rect(0f, 0f, 40f, 40f))
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
                                width = 4.0f.toDp(),
                                brush = brush,
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(borderColor: Color) {
            val inset = IntRect(4, 4, 36, 36)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                if (inset.contains(pos)) Color.Blue else borderColor
            }
        }

        assertBorder(borderColor = Color.Red)

        rule.runOnIdle { brush = SolidColor(Color.Green) }

        assertBorder(borderColor = Color.Green)

        rule.runOnIdle { brush = Brush.linearGradient(listOf(Color.Red, Color.Red)) }

        assertBorder(borderColor = Color.Red)
    }

    @Test
    fun drawBorder_simpleRoundedOutline_sameOutline_changeBrush() {
        val outline = Outline.Rounded(RoundRect(0f, 0f, 40f, 40f, cornerRadius = CornerRadius(8f)))
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
                                width = 4.0f.toDp(),
                                brush = brush,
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(borderColor: Color) {
            val inset = IntRect(4, 4, 36, 36)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                val inCornerSquare = (pos.x !in 8..<32) && (pos.y !in 8..<32)
                val inExtremeCorner = (pos.x !in 2..<38) && (pos.y !in 2..<38)
                when {
                    // The extreme corner pixels lie outside the rounded corners of the ring,
                    // so they show the background
                    inExtremeCorner -> Color.Blue
                    // Ignore the rest of the rounded corners due to antialiasing
                    inCornerSquare -> null
                    inset.contains(pos) -> Color.Blue
                    else -> borderColor
                }
            }
        }

        assertBorder(borderColor = Color.Red)

        rule.runOnIdle { brush = SolidColor(Color.Green) }

        assertBorder(borderColor = Color.Green)

        rule.runOnIdle { brush = Brush.linearGradient(listOf(Color.Red, Color.Red)) }

        assertBorder(borderColor = Color.Red)
    }

    @Test
    fun drawBorder_nonSimpleRoundedOutline_sameOutline_changeBrush() {
        val outline =
            Outline.Rounded(RoundRect(0f, 0f, 40f, 40f, bottomRightCornerRadius = CornerRadius(8f)))
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
                                width = 4.0f.toDp(),
                                brush = brush,
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(borderColor: Color) {
            val inset = IntRect(4, 4, 36, 36)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                val inBottomRightCornerSquare = pos.x >= 32 && pos.y >= 32
                val inBottomRightExtremeCorner = pos.x >= 38 && pos.y >= 38
                when {
                    // The extreme corner pixels lie outside the rounded corner of the ring, so
                    // they show the background
                    inBottomRightExtremeCorner -> Color.Blue
                    // Ignore the rest of the rounded corner due to antialiasing
                    inBottomRightCornerSquare -> null
                    inset.contains(pos) -> Color.Blue
                    else -> borderColor
                }
            }
        }

        assertBorder(borderColor = Color.Red)

        rule.runOnIdle { brush = SolidColor(Color.Green) }

        assertBorder(borderColor = Color.Green)

        rule.runOnIdle { brush = Brush.linearGradient(listOf(Color.Red, Color.Red)) }

        assertBorder(borderColor = Color.Red)
    }

    @Test
    fun drawBorder_genericOutline_sameOutline_changeBrush() {
        val outline = Outline.Generic(Path().apply { addRect(Rect(0f, 0f, 40f, 40f)) })
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
                                width = 4.0f.toDp(),
                                brush = brush,
                                outline = outline,
                            )
                        }
                )
            }
        }

        fun assertBorder(borderColor: Color) {
            val inset = IntRect(4, 4, 36, 36)
            rule.onNodeWithTag(testTag).captureToImage().assertPixels(IntSize(40, 40)) { pos ->
                if (inset.contains(pos)) Color.Blue else borderColor
            }
        }

        assertBorder(borderColor = Color.Red)

        rule.runOnIdle { brush = SolidColor(Color.Green) }

        assertBorder(borderColor = Color.Green)

        rule.runOnIdle { brush = Brush.linearGradient(listOf(Color.Red, Color.Red)) }

        assertBorder(borderColor = Color.Red)
    }

    /**
     * The [GraphicsLayer] returned by the onProvideGraphicsLayer lambda passed to [BorderRenderer]
     * may change between draws (for example, if the previous layer was released), even if the brush
     * and outline do not. The renderer should always use the most recently provided
     * [GraphicsLayer], instead of a stale one from a previous draw.
     */
    // RuntimeShader not available below Tiramisu
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun drawBorder_changeProvidedGraphicsLayer_usesLatestGraphicsLayer() {
        // A RuntimeShader brush with a generic outline requires a GraphicsLayer to draw
        val brush =
            ShaderBrush(
                RuntimeShader("half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }")
            )
        val shape = GenericShape { size, _ -> addRect(Rect(Offset.Zero, size)) }
        // The same outline instance is used across draws, so the renderer's cache is not
        // invalidated by a new outline
        var outline: Outline? = null
        var useSecondLayer by mutableStateOf(false)
        var firstLayerInvocations = 0
        var secondLayerInvocations = 0

        rule.setContent {
            val firstGraphicsLayer = rememberGraphicsLayer()
            val secondGraphicsLayer = rememberGraphicsLayer()
            val renderer =
                remember(firstGraphicsLayer, secondGraphicsLayer) {
                    BorderRenderer {
                        if (useSecondLayer) {
                            secondLayerInvocations++
                            secondGraphicsLayer
                        } else {
                            firstLayerInvocations++
                            firstGraphicsLayer
                        }
                    }
                }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Read useSecondLayer to force a re-draw
                            useSecondLayer
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                brush = brush,
                                outline =
                                    outline
                                        ?: shape.createOutline(size, layoutDirection, this).also {
                                            outline = it
                                        },
                            )
                        }
                )
            }
        }

        rule.runOnIdle {
            assertThat(firstLayerInvocations).isGreaterThan(0)
            assertThat(secondLayerInvocations).isEqualTo(0)
        }

        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )

        rule.runOnIdle { useSecondLayer = true }

        rule.runOnIdle { assertThat(secondLayerInvocations).isGreaterThan(0) }

        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )
    }

    /**
     * If the onProvideGraphicsLayer lambda passed to [BorderRenderer] returns a [GraphicsLayer]
     * that has been released, drawing should not crash, and no border is drawn. When the lambda
     * later returns a new, valid [GraphicsLayer] (for example, after a node is re-attached), the
     * border should be drawn correctly using the new layer, even though the brush and outline did
     * not change.
     */
    // RuntimeShader not available below Tiramisu
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun drawBorder_providedGraphicsLayerReleased_thenNewGraphicsLayerProvided() {
        // A RuntimeShader brush with a generic outline requires a GraphicsLayer to draw
        val brush =
            ShaderBrush(
                RuntimeShader("half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }")
            )
        val shape = GenericShape { size, _ -> addRect(Rect(Offset.Zero, size)) }
        // The same outline instance is used across draws, so the renderer's cache is not
        // invalidated by a new outline
        var outline: Outline? = null
        var useNewLayer by mutableStateOf(false)
        var releasedLayerInvocations = 0
        var newLayerInvocations = 0

        rule.setContent {
            val graphicsContext = LocalGraphicsContext.current
            val releasedGraphicsLayer = remember {
                graphicsContext.createGraphicsLayer().also {
                    graphicsContext.releaseGraphicsLayer(it)
                }
            }
            val newGraphicsLayer = rememberGraphicsLayer()
            val renderer =
                remember(releasedGraphicsLayer, newGraphicsLayer) {
                    BorderRenderer {
                        if (useNewLayer) {
                            newLayerInvocations++
                            newGraphicsLayer
                        } else {
                            releasedLayerInvocations++
                            releasedGraphicsLayer
                        }
                    }
                }
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .drawWithContent {
                            drawContent()
                            // Read useNewLayer to force a re-draw
                            useNewLayer
                            renderer.drawBorder(
                                drawScope = this,
                                width = 10.0f.toDp(),
                                brush = brush,
                                outline =
                                    outline
                                        ?: shape.createOutline(size, layoutDirection, this).also {
                                            outline = it
                                        },
                            )
                        }
                )
            }
        }

        // The released layer was provided, and drawing with it did not crash
        rule.runOnIdle {
            assertThat(releasedLayerInvocations).isGreaterThan(0)
            assertThat(newLayerInvocations).isEqualTo(0)
        }

        // Assert we only have a blue shape (no border was drawn because of the invalid layer)
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Blue,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )

        rule.runOnIdle { useNewLayer = true }

        rule.runOnIdle { assertThat(newLayerInvocations).isGreaterThan(0) }

        // Border should be drawn now
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )
    }

    /**
     * Asserts that rounded corners show the [backgroundColor] outside the rounding, and that
     * non-rounded corners are [borderColor]. Also asserts that the middle of each edge is part of
     * the border, and the center is not.
     */
    private fun assertRoundedCorners(
        backgroundColor: Color,
        borderColor: Color,
        topLeft: Boolean,
        topRight: Boolean,
        bottomRight: Boolean,
        bottomLeft: Boolean,
    ) {
        rule.onNodeWithTag(testTag).captureToImage().run {
            val map = toPixelMap()
            fun cornerColor(rounded: Boolean) = if (rounded) backgroundColor else borderColor
            // The last pixel fails to render properly on some emulators, so assert the one before
            // instead for the right / bottom edges - b/267371353
            val right = width - 2
            val bottom = height - 2
            assertEquals(cornerColor(topLeft), map[0, 0])
            assertEquals(cornerColor(topRight), map[right, 0])
            assertEquals(cornerColor(bottomRight), map[right, bottom])
            assertEquals(cornerColor(bottomLeft), map[0, bottom])
            assertEquals(borderColor, map[width / 2, 0])
            assertEquals(borderColor, map[right, height / 2])
            assertEquals(borderColor, map[width / 2, bottom])
            assertEquals(borderColor, map[0, height / 2])
            assertEquals(backgroundColor, map[width / 2, height / 2])
        }
    }

    private fun calculateContainerShape(
        density: Density,
        arrowBaseWidthDp: Dp,
        arrowTipDp: Dp,
        arrowLengthDp: Dp,
    ): Shape {
        val cornerRadiusDp: Dp = 8.dp
        val cornerRadiusPx: Float
        val arrowBaseWidthPx: Float
        val arrowLengthPx: Float

        val arrowTipPx: Float

        with(density) {
            cornerRadiusPx = cornerRadiusDp.toPx()
            arrowBaseWidthPx = arrowBaseWidthDp.toPx()
            arrowLengthPx = arrowLengthDp.toPx()
            arrowTipPx = arrowTipDp.toPx()
        }

        return GenericShape { size, _ ->
            val width = size.width
            val height = size.height

            val boundingRoundRect =
                Path().apply {
                    addRoundRect(RoundRect(0f, 0f, width, height, cornerRadiusPx, cornerRadiusPx))
                }

            // The tip of the arrow should be positioned in the middle of the icon above.
            val arrowTipX = arrowTipPx / 2

            val boundingArrow =
                Path().apply {
                    moveTo(arrowTipX + arrowBaseWidthPx / 2f, 0f)
                    relativeLineTo(-arrowBaseWidthPx, 0f)
                    relativeLineTo(arrowBaseWidthPx / 2f, -arrowLengthPx)
                    relativeLineTo(arrowBaseWidthPx / 2f, arrowLengthPx)
                }

            op(boundingRoundRect, boundingArrow, PathOperation.Union)
        }
    }

    @Composable
    fun SemanticParent(content: @Composable Density.() -> Unit) {
        Box { Box(modifier = Modifier.testTag(testTag)) { LocalDensity.current.content() } }
    }
}
