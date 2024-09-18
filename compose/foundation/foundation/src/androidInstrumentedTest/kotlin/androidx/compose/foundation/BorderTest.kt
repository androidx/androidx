/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.math.floor
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderTest(val shape: Shape) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initShapes(): Array<Any> =
            arrayOf(
                namedShape("Rectangle", RectangleShape),
                namedShape("Circle", CircleShape),
                namedShape("Rounded", RoundedCornerShape(5.0f))
            )

        private fun namedShape(name: String, shape: Shape): Shape =
            object : Shape by shape {
                override fun toString(): String = name
            }
    }

    @get:Rule val rule = createComposeRule()

    val testTag = "BorderParent"

    private val rtlAwareShape =
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) =
                if (layoutDirection == LayoutDirection.Ltr) {
                    Outline.Rectangle(Rect(0f, 1f, 0f, 1f))
                } else {
                    shape.createOutline(size, layoutDirection, density)
                }
        }

    @Test
    fun border_color() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f
        )
    }

    @Test
    fun border_brush() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), SolidColor(Color.Red)), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f
        )
    }

    @Test
    fun border_biggerThanLayout_fills() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1500.0f.toDp(), Color.Red), shape)
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
            antiAliasingGap = 2.0f
        )
    }

    @Test
    fun border_lessThanZero_doesNothing() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(-5.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Blue,
            shape = shape,
            backgroundShape = shape,
            antiAliasingGap = 2.0f
        )
    }

    @Test
    fun border_zeroSizeLayout_drawsNothing() {
        rule.setContent {
            SemanticParent {
                Box(Modifier.size(40.0f.toDp(), 40.0f.toDp()).background(Color.White)) {
                    Box(
                        Modifier.size(0.0f.toDp(), 40.0f.toDp())
                            .border(BorderStroke(4.0f.toDp(), Color.Red), shape)
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
            antiAliasingGap = 1.0f
        )
    }

    @Test
    fun border_triangle_shape() {
        val testTag = "testTag"
        val triangle = GenericShape { size, _ ->
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        rule.setContent {
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
                    .border(BorderStroke(borderWidthDp, Color.Red), triangle)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = 32) // b/257069369
    @Test
    fun border_non_simple_rounded_rect() {
        val topleft = 0f
        val topRight = 5f
        val bottomRight = 10f
        val bottomLeft = 15f
        val roundRectTag = "roundRectTag"
        var borderPx = 0f
        rule.setContent {
            val size = 50.dp
            val border = 10.dp

            with(LocalDensity.current) { borderPx = border.toPx() }
            Box(
                Modifier.testTag(roundRectTag)
                    .size(size)
                    .background(Color.White)
                    .border(
                        BorderStroke(border, Color.Red),
                        RoundedCornerShape(
                            topStart = topleft,
                            topEnd = topRight,
                            bottomStart = bottomLeft,
                            bottomEnd = bottomRight
                        )
                    )
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
                map[map.width - borderPx.toInt() - offset, borderPx.toInt() + offset]
            )
            assertEquals(
                Color.White,
                map[map.width - borderPx.toInt() - offset, map.height - borderPx.toInt() - offset]
            )
            assertEquals(
                Color.White,
                map[borderPx.toInt() + offset, map.height - borderPx.toInt() - offset]
            )

            val topRightOffset = (topRight / 2).roundToInt()
            assertEquals(Color.Red, map[width - 1 - topRightOffset, topRightOffset])

            val bottomRightOffset = (bottomRight / 2).roundToInt()
            assertEquals(Color.Red, map[width - 1 - bottomRightOffset, bottomRightOffset])

            val bottomLeftOffset = (bottomLeft / 2).roundToInt()
            assertEquals(Color.Red, map[bottomLeftOffset, height - 1 - bottomLeftOffset])
        }
    }

    @Test
    fun border_rtl_initially() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                ) {}
            }
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
                antiAliasingGap = 3.0f
            )
    }

    @Test
    fun border_rtl_after_switch() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            SemanticParent {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Box(
                        Modifier.size(40.0f.toDp(), 40.0f.toDp())
                            .background(color = Color.Blue)
                            .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                    ) {}
                }
            }
        }

        rule.runOnIdle { direction.value = LayoutDirection.Rtl }
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
                antiAliasingGap = 3.0f
            )
    }

    @Test
    fun border_color_with_alpha_tinted_properly() {
        val testTag = "testTag"
        val background = Color.Red
        val borderColor = Color.Blue.copy(alpha = 0.5f)
        rule.setContent {
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(100.dp, 100.dp)
                    .background(background)
                    .border(
                        10.dp,
                        borderColor,
                        GenericShape { size, _ ->
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    )
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
    fun border_generic_shape_color_to_brush() {
        // Verify that rendering with a solid color initially then with a gradient
        // updates the internal offscreen bitmap config to Argb8888 from Alpha8
        val gradient =
            Brush.verticalGradient(
                0.0f to Color.Red,
                0.5f to Color.Red,
                0.5f to Color.Blue,
                1.0f to Color.Blue
            )
        val testTag = "testTag"
        val borderStrokeDp = 5.dp
        var borderStrokePx = 0f
        val toggle = mutableStateOf(false)
        rule.setContent {
            val testShape = GenericShape { size, _ ->
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            with(LocalDensity.current) { borderStrokePx = borderStrokeDp.toPx() }
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(20.dp, 20.dp)
                    .background(Color.White)
                    .border(
                        BorderStroke(
                            borderStrokeDp,
                            if (toggle.value) gradient else SolidColor(Color.Green)
                        ),
                        testShape
                    )
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
                pixelMap[width - halfBorderStrokePx, height - halfBorderStrokePx]
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
                pixelMap[width - halfBorderStrokePx, height - halfBorderStrokePx]
            )
            assertEquals(Color.White, pixelMap[width / 2, height / 2])
        }
    }

    @Test
    fun border_test_with_fixed_size_generic_shape() {
        val testTag = "testTag"
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
            val bubbleWithArrow =
                calculateContainerShape(
                    LocalDensity.current,
                    arrowBaseWidthDp,
                    arrowTipDp,
                    arrowLengthDp
                )
            with(LocalDensity.current) {
                val outline =
                    bubbleWithArrow.createOutline(
                        Size(bubbleWidthDp.toPx(), bubbleHeightDp.toPx()),
                        LayoutDirection.Ltr,
                        this
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
                    .border(BorderStroke(borderWidthDp, Color.Red), bubbleWithArrow)
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
                    (offset.y - arrowLengthPx / 2 + borderStrokePx / 2).toInt()]
            )

            // Tip of the triangle is drawn within the border
            currentX += arrowBaseWidthPx / 2f
            assertEquals(
                Color.Red,
                map[currentX.toInt(), (offset.y - arrowLengthPx + borderStrokePx / 2).toInt()]
            )

            // rounded rectangle directly below the triangle does not have the border rendered
            assertEquals(
                Color.White,
                map[currentX.toInt(), (offset.y + borderStrokePx / 2).toInt()]
            )

            // Midpoint of the end of the triangle being drawn back into the rounded rect
            // has the border rendered
            assertEquals(
                Color.Red,
                map[
                    (currentX + arrowBaseWidthPx / 4f).toInt(),
                    (offset.y - arrowLengthPx / 4 + borderStrokePx / 2).toInt()]
            )

            // Base of the triangle on the rounded rect shape has the border rendered
            currentX += arrowBaseWidthPx / 2f
            assertEquals(Color.Red, map[currentX.toInt(), (offset.y + borderStrokePx / 2).toInt()])
        }
    }

    @Test
    fun border_changeShape() {
        var roundedCorners by mutableStateOf(false)

        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(
                            BorderStroke(1f.toDp(), Color.Red),
                            if (roundedCorners) RoundedCornerShape(5f) else RectangleShape
                        )
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
    fun border_changeOutline_outlineRounded_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
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
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1f.toDp(), Color.Red), roundedCornersShape)
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
    fun border_changeOutline_outlineGeneric_samePath_observableShape() {
        var roundedCorners by mutableStateOf(false)
        val roundedCornersShape =
            object : Shape {
                val path = Path()

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
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
            SemanticParent {
                Box(
                    Modifier.size(400.0f.toDp(), 400.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1f.toDp(), Color.Red), roundedCornersShape)
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
    fun equalInputs_shouldResolveToEquals_withColor() {
        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.border(10.dp, Color.Red)
            } else {
                Modifier.border(5.dp, Color.Red)
            }
        }
    }

    @Test
    fun equalInputs_shouldResolveToEquals_withBorderStroke() {
        val borderStroke1 = BorderStroke(10.dp, Color.Blue)
        val borderStroke2 = BorderStroke(5.dp, Color.Blue)
        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.border(borderStroke1)
            } else {
                Modifier.border(borderStroke2)
            }
        }
    }

    @Test
    fun equalInputs_shouldResolveToEquals_withBrush() {
        val brush1 = Brush.horizontalGradient()
        val brush2 = Brush.verticalGradient()
        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.border(10.dp, brush1, shape)
            } else {
                Modifier.border(10.dp, brush2, shape)
            }
        }
    }

    private fun calculateContainerShape(
        density: Density,
        arrowBaseWidthDp: Dp,
        arrowTipDp: Dp,
        arrowLengthDp: Dp
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
