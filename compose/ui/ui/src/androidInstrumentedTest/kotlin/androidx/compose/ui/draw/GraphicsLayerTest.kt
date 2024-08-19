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

package androidx.compose.ui.draw

import android.os.Build
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.OffsetEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scale
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GraphicsLayerTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule =
        androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)

    @get:Rule val rule = createComposeRule()

    @Test
    fun testLayerBoundsPosition() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            FixedSize(
                30,
                Modifier.padding(10).graphicsLayer().onGloballyPositioned { coords = it }
            ) { /* no-op */
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            assertEquals(Offset(10f, 10f), layoutCoordinates.positionInRoot())
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10f, 10f, 40f, 40f), bounds)
            val global = layoutCoordinates.boundsInWindow()
            val position = layoutCoordinates.positionInWindow()
            assertEquals(position.x, global.left)
            assertEquals(position.y, global.top)
            assertEquals(30f, global.width)
            assertEquals(30f, global.height)
        }
    }

    @Test
    fun testScale() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleX = 2f, scaleY = 3f).onGloballyPositioned {
                        coords = it
                    }
                ) {}
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceXY() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.scale(scaleX = 2f, scaleY = 3f).onGloballyPositioned { coords = it }
                ) {}
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceUniform() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.scale(scale = 2f).onGloballyPositioned { coords = it }) {}
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 5f, 25f, 25f), bounds)
            assertEquals(Offset(5f, 5f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotation() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleY = 3f, rotationZ = 90f).onGloballyPositioned {
                        coords = it
                    }
                ) {}
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(0f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationConvenience() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.rotate(90f).onGloballyPositioned { coords = it }) {}
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10.0f, 10f, 20f, 20f), bounds)
            assertEquals(Offset(20f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationPivot() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(
                            rotationZ = 90f,
                            transformOrigin = TransformOrigin(1.0f, 1.0f)
                        )
                        .onGloballyPositioned { coords = it }
                )
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(20f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testTranslationXY() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(translationX = 5.0f, translationY = 8.0f)
                        .onGloballyPositioned { coords = it }
                )
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(15f, 18f, 25f, 28f), bounds)
            assertEquals(Offset(15f, 18f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testClip() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(
                        10,
                        Modifier.graphicsLayer(scaleX = 2f).onGloballyPositioned { coords = it }
                    ) {}
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10f, 10f, 20f, 20f), bounds)
            // Positions aren't clipped
            assertEquals(Offset(5f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testSiblingComparisons() {
        var coords1: LayoutCoordinates? = null
        var coords2: LayoutCoordinates? = null
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.requiredSize(25.toDp()).graphicsLayer(rotationZ = 30f, clip = true)) {
                    Box(
                        Modifier.graphicsLayer(
                                rotationZ = 90f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                clip = true
                            )
                            .requiredSize(20.toDp(), 10.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned { coords1 = it }
                    )
                    Box(
                        Modifier.graphicsLayer(
                                rotationZ = -90f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                clip = true
                            )
                            .requiredSize(10.toDp())
                            .align(AbsoluteAlignment.BottomRight)
                            .onGloballyPositioned { coords2 = it }
                    )
                }
            }
        }

        rule.onRoot().apply {
            assertEquals(Offset(15f, 5f), coords2!!.localPositionOf(coords1!!, Offset.Zero))
            assertEquals(Offset(-5f, 5f), coords2!!.localPositionOf(coords1!!, Offset(20f, 0f)))
            assertEquals(Rect(-5f, -5f, 15f, 5f), coords2!!.localBoundingBoxOf(coords1!!, false))
            assertEquals(Rect(0f, 0f, 10f, 5f), coords2!!.localBoundingBoxOf(coords1!!, true))
        }
    }

    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCameraDistanceWithRotationY() {
        if (Build.VERSION.SDK_INT == 28) return // b/260095151
        val testTag = "parent"
        rule.setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(
                    modifier =
                        Modifier.requiredSize(100.dp)
                            .background(Color.Gray)
                            .graphicsLayer(rotationY = 25f, cameraDistance = 1.0f)
                            .background(Color.Red)
                ) {
                    Box(modifier = Modifier.requiredSize(100.dp))
                }
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(0, height - 1))
            assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1, 0))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1, height - 1))
        }
    }

    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testEmptyClip() {
        val EmptyRectangle =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ) = Outline.Rectangle(Rect.Zero)
            }
        val tag = "testTag"
        rule.setContent {
            Box(modifier = Modifier.testTag(tag).requiredSize(100.dp).background(Color.Blue)) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .graphicsLayer(clip = true, shape = EmptyRectangle)
                            .background(Color.Red)
                )
            }
        }

        // Results should match background color of parent. Because the child Box is clipped to
        // an empty rectangle, no red pixels from its background should be visible
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun testTotalClip() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(10, Modifier.padding(20).onGloballyPositioned { coords = it }) {}
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            // should be completely clipped out
            assertEquals(0f, bounds.width)
            assertEquals(0f, bounds.height)
        }
    }

    @Composable
    fun BoxBlur(tag: String, size: Float, blurRadius: Float) {
        BoxRenderEffect(
            tag,
            (size / LocalDensity.current.density).dp,
            ({ BlurEffect(blurRadius, blurRadius, TileMode.Decal) })
        ) {
            inset(blurRadius, blurRadius) { drawRect(androidx.compose.ui.graphics.Color.Blue) }
        }
    }

    @Composable
    fun BoxRenderEffect(
        tag: String,
        size: Dp,
        renderEffectCreator: () -> RenderEffect,
        drawBlock: DrawScope.() -> Unit
    ) {
        Box(
            Modifier.testTag(tag)
                .size(size)
                .background(Color.Black)
                .graphicsLayer { renderEffect = renderEffectCreator() }
                .drawBehind(drawBlock)
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testBlurEffect() {
        val tag = "blurTag"
        val size = 100f
        val blurRadius = 10f
        rule.setContent { BoxBlur(tag, size, blurRadius) }
        rule.onNodeWithTag(tag).captureToImage().apply {
            val pixelMap = toPixelMap()
            var nonPureBlueCount = 0
            for (x in (blurRadius).toInt() until (width - (blurRadius)).toInt()) {
                for (y in (blurRadius).toInt() until (height - (blurRadius)).toInt()) {
                    val pixelColor = pixelMap[x, y]
                    if (pixelColor.red > 0 || pixelColor.green > 0) {
                        fail("Only blue colors are expected. Pixel at [$x, $y] $pixelColor")
                    }
                    if (pixelColor.blue > 0 && pixelColor.blue < 1f) {
                        nonPureBlueCount++
                    }
                }
            }
            assertTrue(nonPureBlueCount > 0)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testZeroRadiusBlurDoesNotCrash() {
        val tag = "blurTag"
        val size = 100f
        rule.setContent { BoxBlur(tag, size, 0f) }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.R)
    fun testBlurNoopOnUnsupportedPlatforms() {
        val tag = "blurTag"
        val size = 100f
        val blurRadius = 10f
        rule.setContent { BoxBlur(tag, size, blurRadius) }
        rule.onNodeWithTag(tag).captureToImage().apply {
            with(toPixelMap()) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        if (
                            x >= blurRadius &&
                                x < width - blurRadius &&
                                y >= blurRadius &&
                                y < height - blurRadius
                        ) {
                            assertPixelColor(Color.Blue, x, y) { "Index $x, $y should be blue" }
                        } else {
                            assertPixelColor(Color.Black, x, y) { "Index $x, $y should be black" }
                        }
                    }
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testOffsetEffect() {
        val tag = "blurTag"
        val size = 100f
        rule.setContent {
            BoxRenderEffect(
                tag,
                (size / LocalDensity.current.density).dp,
                { OffsetEffect(20f, 20f) }
            ) {
                drawRect(Color.Blue, size = Size(this.size.width - 20, this.size.height - 20))
            }
        }
        rule.onNodeWithTag(tag).captureToImage().apply {
            val pixelMap = toPixelMap()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (x >= 20f && y >= 20f) {
                        assertEquals("Index $x, $y should be blue", Color.Blue, pixelMap[x, y])
                    } else {
                        assertEquals("Index $x, $y should be black", Color.Black, pixelMap[x, y])
                    }
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareLayerOffset() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 100
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width - 1, 0])
                assertEquals(Color.LightGray, this[0, height - 1])
                assertEquals(Color.LightGray, this[width - 1, height - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare).toInt()
                val scaledBottom = (offset + scaledSquare).toInt()

                assertPixelColor(blended, scaledLeft + 3, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 3, scaledTop + 3)
                assertPixelColor(blended, scaledLeft + 3, scaledBottom - 3)
                assertPixelColor(blended, scaledRight - 3, scaledBottom - 3)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareLayerRectangularClip() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 100
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .drawBehind {
                            // Draw a rectangle twice the size of the original bounds
                            // to verify rectangular clipping is applied properly and ignores
                            // the pixels outside of the original size
                            val width = this.size.width
                            val height = this.size.height
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(-width, -height),
                                size = Size(width * 2, height * 2)
                            )
                        }
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width - 1, 0])
                assertEquals(Color.LightGray, this[0, height - 1])
                assertEquals(Color.LightGray, this[width - 1, height - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare).toInt()
                val scaledBottom = (offset + scaledSquare).toInt()

                assertPixelColor(Color.LightGray, scaledLeft - 3, scaledTop - 3)
                assertPixelColor(Color.LightGray, scaledRight + 3, scaledTop - 3)
                assertPixelColor(Color.LightGray, scaledLeft - 3, scaledBottom + 3)
                assertPixelColor(Color.LightGray, scaledRight + 3, scaledBottom + 3)

                assertPixelColor(blended, scaledLeft + 3, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 3, scaledTop + 3)
                assertPixelColor(blended, scaledLeft + 3, scaledBottom - 3)
                assertPixelColor(blended, scaledRight - 3, scaledBottom - 3)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareCircularShapeClip() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 200
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    shape = CircleShape
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .drawBehind {
                            // Draw a rectangle twice the size of the original bounds
                            // to verify rectangular clipping is applied properly and ignores
                            // the pixels outside of the original size
                            val width = this.size.width
                            val height = this.size.height
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(-width, -height),
                                size = Size(width * 2, height * 2)
                            )
                        }
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width - 1, 0])
                assertEquals(Color.LightGray, this[0, height - 1])
                assertEquals(Color.LightGray, this[width - 1, height - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare).toInt()
                val scaledBottom = (offset + scaledSquare).toInt()

                assertPixelColor(Color.LightGray, scaledLeft + 1, scaledTop + 1)
                assertPixelColor(Color.LightGray, scaledRight - 1, scaledTop + 1)
                assertPixelColor(Color.LightGray, scaledLeft + 1, scaledBottom - 1)
                assertPixelColor(Color.LightGray, scaledRight - 1, scaledBottom - 1)

                val scaledMidHorizontal = (scaledLeft + scaledRight) / 2
                val scaledMidVertical = (scaledTop + scaledBottom) / 2
                assertPixelColor(blended, scaledMidHorizontal, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 3, scaledMidVertical)
                assertPixelColor(blended, scaledMidHorizontal, scaledBottom - 3)
                assertPixelColor(blended, scaledLeft + 3, scaledMidVertical)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareLayerManualClip() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 100
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    shape = GenericShape { size, _ ->
                                        lineTo(size.width, 0f)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .drawBehind {
                            // Draw a rectangle twice the size of the original bounds
                            // to verify rectangular clipping is applied properly and ignores
                            // the pixels outside of the original size
                            val width = this.size.width
                            val height = this.size.height
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(-width, -height),
                                size = Size(width * 2, height * 2)
                            )
                        }
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width - 1, 0])
                assertEquals(Color.LightGray, this[0, height - 1])
                assertEquals(Color.LightGray, this[width - 1, height - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare).toInt()
                val scaledBottom = (offset + scaledSquare).toInt()

                assertPixelColor(Color.LightGray, scaledLeft - 3, scaledTop - 3)
                assertPixelColor(Color.LightGray, scaledRight + 3, scaledTop - 3)
                assertPixelColor(Color.LightGray, scaledLeft - 3, scaledBottom + 3)
                assertPixelColor(Color.LightGray, scaledRight + 3, scaledBottom + 3)

                assertPixelColor(blended, scaledLeft + 3, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 5, scaledTop + 1)
                assertPixelColor(blended, scaledLeft + 1, scaledBottom - 5)

                assertPixelColor(
                    Color.LightGray,
                    (scaledLeft + scaledRight) / 2 + 3,
                    (scaledTop + scaledBottom) / 2 + 3
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareLayerOffsetRectangularClip() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 100
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    clip = true
                                    shape =
                                        object : Shape {
                                            override fun createOutline(
                                                size: Size,
                                                layoutDirection: LayoutDirection,
                                                density: Density
                                            ): Outline {
                                                return Outline.Rectangle(
                                                    Rect(
                                                        left = -size.width / 2,
                                                        top = -size.height / 2,
                                                        right = size.width / 2,
                                                        bottom = size.height / 2,
                                                    )
                                                )
                                            }
                                        }
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width / 2 - 1, 0])
                assertEquals(Color.LightGray, this[0, height / 2 - 1])
                assertEquals(Color.LightGray, this[width / 2 - 1, height / 2 - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare / 2).toInt()
                val scaledBottom = (offset + scaledSquare / 2).toInt()

                assertPixelColor(blended, scaledLeft + 3, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 3, scaledTop + 3)
                assertPixelColor(blended, scaledLeft + 3, scaledBottom - 3)
                assertPixelColor(blended, scaledRight - 3, scaledBottom - 3)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSoftwareLayerOffsetRoundedRectClip() {
        val testTag = "box"
        var offset = 0
        val scale = 0.5f
        val boxAlpha = 0.5f
        val sizePx = 200
        val squarePx = sizePx / 2
        rule.setContent {
            LocalView.current.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val density = LocalDensity.current.density
            val size = (sizePx / density)
            val squareSize = (squarePx / density)
            offset = (20f / density).roundToInt()
            Box(Modifier.size(size.dp).background(Color.LightGray).testTag(testTag)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(offset, offset) {
                                    alpha = boxAlpha
                                    scaleX = scale
                                    scaleY = scale
                                    clip = true
                                    shape =
                                        object : Shape {
                                            override fun createOutline(
                                                size: Size,
                                                layoutDirection: LayoutDirection,
                                                density: Density
                                            ): Outline {
                                                return Outline.Rounded(
                                                    RoundRect(
                                                        left = -size.width / 2,
                                                        top = -size.height / 2,
                                                        right = size.width / 2,
                                                        bottom = size.height / 2,
                                                        cornerRadius =
                                                            CornerRadius(
                                                                size.width / 2,
                                                                size.height / 2
                                                            )
                                                    )
                                                )
                                            }
                                        }
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                        .size(squareSize.dp)
                        .background(Color.Red)
                )
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            with(toPixelMap()) {
                assertEquals(Color.LightGray, this[0, 0])
                assertEquals(Color.LightGray, this[width / 2 - 1, 0])
                assertEquals(Color.LightGray, this[0, height / 2 - 1])
                assertEquals(Color.LightGray, this[width / 2 - 1, height / 2 - 1])

                val blended = Color.Red.copy(alpha = boxAlpha).compositeOver(Color.LightGray)

                val scaledSquare = squarePx * scale
                val scaledLeft = offset
                val scaledTop = offset
                val scaledRight = (offset + scaledSquare / 2).toInt()
                val scaledBottom = (offset + scaledSquare / 2).toInt()

                assertPixelColor(blended, scaledLeft + 3, scaledTop + 3)
                assertPixelColor(blended, scaledRight - 3, scaledTop + 3)
                assertPixelColor(blended, scaledLeft + 3, scaledBottom - 3)

                // The bottom right corner should be clipped out and the background should be
                // revealed
                assertPixelColor(Color.LightGray, scaledRight, scaledBottom)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun invalidateWhenWeHaveSemanticModifierAfterLayer() {
        var color by mutableStateOf(Color.Red)
        rule.setContent { FixedSize(5, Modifier.graphicsLayer().testTag("tag").background(color)) }

        rule.runOnIdle { color = Color.Green }

        rule.onNodeWithTag("tag").captureToImage().assertPixels { color }
    }

    @Test
    fun testDpPixelConversions() {
        var density: Density? = null
        var testDpConversion = 0f
        var testFontScaleConversion = 0f
        rule.setContent {
            density = LocalDensity.current
            // Verify that the current density is passed to the graphics layer
            // implementation and that density dependent methods are consuming it
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        testDpConversion = 2.dp.toPx()
                        testFontScaleConversion = 3.dp.toSp().toPx()
                    }
            )
        }

        rule.runOnIdle {
            with(density!!) {
                assertEquals(2.dp.toPx(), testDpConversion)
                assertEquals(3.dp.toSp().toPx(), testFontScaleConversion)
            }
        }
    }

    @Test
    fun testClickOnScaledElement() {
        var firstClicked = false
        var secondClicked = false
        rule.setContent {
            Layout(
                content = {
                    Box(Modifier.fillMaxSize().clickable { firstClicked = true })
                    Box(Modifier.fillMaxSize().clickable { secondClicked = true })
                },
                modifier = Modifier.testTag("layout")
            ) { measurables, _ ->
                val itemConstraints = Constraints.fixed(100, 100)
                val first = measurables[0].measure(itemConstraints)
                val second = measurables[1].measure(itemConstraints)
                layout(100, 200) {
                    val layer: GraphicsLayerScope.() -> Unit = {
                        scaleX = 0.5f
                        scaleY = 0.5f
                    }
                    first.placeWithLayer(0, 0, layerBlock = layer)
                    second.placeWithLayer(0, 100, layerBlock = layer)
                }
            }
        }

        rule.onNodeWithTag("layout").performTouchInput { click(position = Offset(50f, 170f)) }

        rule.runOnIdle {
            assertFalse("First element is clicked", firstClicked)
            assertTrue("Second element is not clicked", secondClicked)
        }
    }

    @Test
    fun usingNestedDerivedStateInGraphicsLayerBlock() {
        val mutableState = mutableStateOf(1f)
        val derivedState by derivedStateOf { mutableState.value }
        val nestedDerivedState by derivedStateOf { derivedState }
        var valueReadInGraphicsLayer = Float.MIN_VALUE

        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                        // update the state during the measure pass
                        mutableState.value = 2f

                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .graphicsLayer {
                        // read during updateLayerParameters
                        valueReadInGraphicsLayer = nestedDerivedState
                    }
            )
        }

        rule.runOnIdle { assertEquals(2f, valueReadInGraphicsLayer) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCompositingStrategyModulateAlpha() {
        val tag = "testTag"
        val dimen = 200
        rule.setContent {
            Canvas(
                modifier =
                    Modifier.testTag(tag)
                        .size((dimen / LocalDensity.current.density).dp)
                        .background(Color.Black)
                        .graphicsLayer(
                            alpha = 0.5f,
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        )
            ) {
                inset(0f, 0f, size.width / 3, size.height / 3) { drawRect(color = Color.Red) }
                inset(size.width / 3, size.height / 3, 0f, 0f) { drawRect(color = Color.Blue) }
            }
        }

        rule.onNodeWithTag(tag).captureToImage().apply {
            with(toPixelMap()) {
                val redWithAlpha = Color.Red.copy(alpha = 0.5f)
                val blueWithAlpha = Color.Blue.copy(alpha = 0.5f)
                val bg = Color.Black
                val expectedTopLeft = redWithAlpha.compositeOver(bg)
                val expectedBottomRight = blueWithAlpha.compositeOver(bg)
                val expectedCenter = blueWithAlpha.compositeOver(redWithAlpha).compositeOver(bg)
                assertPixelColor(expectedTopLeft, 0, 0)
                assertPixelColor(Color.Black, width - 1, 0)
                assertPixelColor(expectedBottomRight, width - 1, height - 1)
                assertPixelColor(Color.Black, 0, height - 1)
                assertPixelColor(expectedCenter, width / 2, height / 2)
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCompositingStrategyAlways() {
        val tag = "testTag"
        val dimen = 200
        rule.setContent {
            Canvas(
                modifier =
                    Modifier.testTag(tag)
                        .size((dimen / LocalDensity.current.density).dp)
                        .background(Color.LightGray)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                inset(0f, 0f, size.width / 3, size.height / 3) { drawRect(color = Color.Red) }
                inset(size.width / 3, size.height / 3, 0f, 0f) {
                    drawRect(color = Color.Blue, blendMode = BlendMode.Xor)
                }
            }
        }

        rule.onNodeWithTag(tag).captureToImage().apply {
            with(toPixelMap()) {
                assertPixelColor(Color.Red, 0, 0)
                assertPixelColor(Color.LightGray, width - 1, 0)
                assertPixelColor(Color.Blue, width - 1, height - 1)
                assertPixelColor(Color.LightGray, 0, height - 1)
                assertPixelColor(Color.LightGray, width / 2, height / 2)
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCompositingStrategyAuto() {
        val tag = "testTag"
        val dimen = 200
        rule.setContent {
            Canvas(
                modifier =
                    Modifier.testTag(tag)
                        .size((dimen / LocalDensity.current.density).dp)
                        .background(Color.Black)
                        .graphicsLayer(alpha = 0.5f, compositingStrategy = CompositingStrategy.Auto)
            ) {
                inset(0f, 0f, size.width / 3, size.height / 3) { drawRect(color = Color.Red) }
                inset(size.width / 3, size.height / 3, 0f, 0f) { drawRect(color = Color.Blue) }
            }
        }

        rule.onNodeWithTag(tag).captureToImage().apply {
            with(toPixelMap()) {
                val redWithAlpha = Color.Red.copy(alpha = 0.5f)
                val blueWithAlpha = Color.Blue.copy(alpha = 0.5f)
                val bg = Color.Black
                val expectedTopLeft = redWithAlpha.compositeOver(bg)
                val expectedBottomRight = blueWithAlpha.compositeOver(bg)
                val expectedCenter = blueWithAlpha.compositeOver(bg)
                assertPixelColor(expectedTopLeft, 0, 0)
                assertPixelColor(Color.Black, width - 1, 0)
                assertPixelColor(expectedBottomRight, width - 1, height - 1)
                assertPixelColor(Color.Black, 0, height - 1)
                assertPixelColor(expectedCenter, width / 2, height / 2)
            }
        }
    }

    @Test
    fun testGraphicsLayerScopeSize() {
        val widthDp = 200.dp
        val heightDp = 500.dp

        var graphicsLayerWidth = 0f
        var graphicsLayerHeight = 0f
        var drawScopeWidth = -1f
        var drawScopeHeight = -1f
        rule.setContent {
            Box(
                modifier =
                    Modifier.size(widthDp, heightDp)
                        .graphicsLayer {
                            graphicsLayerWidth = size.width
                            graphicsLayerHeight = size.height
                        }
                        .drawBehind {
                            drawScopeWidth = size.width
                            drawScopeHeight = size.height
                        }
            )
        }
        rule.runOnIdle {
            assertEquals(drawScopeWidth, graphicsLayerWidth)
            assertEquals(drawScopeHeight, graphicsLayerHeight)
        }
    }

    @Test
    fun testGraphicsLayerSizeAfterRelayout() {
        var composableSize by mutableStateOf(20.dp)
        var graphicsLayerWidth = -1f
        var graphicsLayerHeight = -1f
        var drawScopeWidth = 0f
        var drawScopeHeight = 0f

        var density: Density? = null

        rule.setContent {
            density = LocalDensity.current
            Box(
                modifier =
                    Modifier.size(composableSize, composableSize)
                        .graphicsLayer {
                            graphicsLayerWidth = size.width
                            graphicsLayerHeight = size.height
                        }
                        .drawBehind {
                            drawScopeWidth = size.width
                            drawScopeHeight = size.height
                        }
            )
        }

        rule.waitForIdle()

        assertNotNull(density)

        var sizePx = with(density!!) { ceil(composableSize.toPx()) }
        assertEquals(sizePx, graphicsLayerWidth)
        assertEquals(sizePx, graphicsLayerHeight)
        assertEquals(sizePx, drawScopeWidth)
        assertEquals(sizePx, drawScopeHeight)

        composableSize = 40.dp

        rule.waitForIdle()

        sizePx = with(density!!) { ceil(composableSize.toPx()) }
        assertEquals(sizePx, graphicsLayerWidth)
        assertEquals(sizePx, graphicsLayerHeight)
        assertEquals(sizePx, drawScopeWidth)
        assertEquals(sizePx, drawScopeHeight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun removingGraphicsLayerInvalidatesParentLayer() {
        var toggle by mutableStateOf(true)
        val size = 100
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { size.toDp() }
            LazyColumn(Modifier.testTag("lazy").background(Color.Blue)) {
                items(4) {
                    Box(
                        Modifier.then(if (toggle) Modifier.graphicsLayer(alpha = 0f) else Modifier)
                            .background(Color.Red)
                            .size(sizeDp)
                    )
                }
            }
        }

        rule.onNodeWithTag("lazy").captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Blue.toArgb(), getPixel(10, (size * 1.5f).roundToInt()))
            assertEquals(Color.Blue.toArgb(), getPixel(10, (size * 2.5f).roundToInt()))
        }

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("lazy").captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(10, (size * 1.5f).roundToInt()))
            assertEquals(Color.Red.toArgb(), getPixel(10, (size * 2.5f).roundToInt()))
        }
    }

    // Repro test for b/298520326. Unfortunately, this test does not successfully reproduce the
    // issue prior to the "fix", so is not a valid regression test. The act of calling
    // `captureToImage()` causes the bug to disappear.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun removingGraphicsLayerInvalidatesParentLayer2() {
        var toggle by mutableStateOf(false)
        val size = 100
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { size.toDp() }
            Box(
                Modifier.testTag("outer")
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .then(
                        if (toggle)
                            Modifier.graphicsLayer(scaleX = 1f).layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                        else Modifier
                    )
            ) {
                Box(Modifier.background(Color.Red).size(sizeDp))
            }
        }

        val pt = (size * 0.5f).roundToInt()

        rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(pt, pt))
        }

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(pt, pt))
        }

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(pt, pt))
        }
    }

    @Test
    fun removingGraphicsLayerModifierResetsItsAction() {
        var addGraphicsLayer by mutableStateOf(true)
        lateinit var coordinates: LayoutCoordinates
        rule.setContent {
            Box(
                if (addGraphicsLayer) {
                    Modifier.graphicsLayer(translationX = 10f)
                } else {
                    Modifier
                }
            ) {
                Layout(Modifier.onGloballyPositioned { coordinates = it }) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            assertEquals(Rect(10f, 0f, 20f, 10f), coordinates.boundsInRoot())
            addGraphicsLayer = false
        }

        rule.runOnIdle { assertEquals(Rect(0f, 0f, 10f, 10f), coordinates.boundsInRoot()) }
    }

    @Test
    fun invalidationAfterMovingMovableContentWithLayer() {
        var moveContent by mutableStateOf(false)
        var counter by mutableStateOf(0)
        var counterReadInDrawing = -1
        val content = movableContentOf {
            Box(Modifier.size(5.dp).graphicsLayer().drawBehind { counterReadInDrawing = counter })
        }

        rule.setContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }

        rule.runOnIdle { moveContent = true }

        rule.runOnIdle {
            assertThat(counterReadInDrawing).isEqualTo(counter)
            counter++
        }

        rule.runOnIdle { assertThat(counterReadInDrawing).isEqualTo(counter) }
    }

    @Test
    fun updatingLayerPropertiesAfterMovingMovableContent() {
        var moveContent by mutableStateOf(false)
        var counter by mutableStateOf(0)
        var counterReadInLayerBlock = -1
        val content = movableContentOf {
            Box(Modifier.size(5.dp).graphicsLayer { counterReadInLayerBlock = counter })
        }

        rule.setContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }

        rule.runOnIdle { moveContent = true }

        rule.runOnIdle {
            assertThat(counterReadInLayerBlock).isEqualTo(counter)
            counter++
        }

        rule.runOnIdle { assertThat(counterReadInLayerBlock).isEqualTo(counter) }
    }

    @Test
    fun updatingValueIsNotCausingRemeasureOrRelayout() {
        var translationX by mutableStateOf(0f)
        lateinit var coordinates: LayoutCoordinates
        var remeasureCount = 0
        var relayoutCount = 0
        val layoutModifier =
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                remeasureCount++
                layout(placeable.width, placeable.height) {
                    relayoutCount++
                    placeable.place(0, 0)
                }
            }
        rule.setContent {
            Box(Modifier.graphicsLayer(translationX = translationX).then(layoutModifier)) {
                Layout(Modifier.onGloballyPositioned { coordinates = it }) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0f, coordinates.boundsInRoot().left)
            // reset counters
            remeasureCount = 0
            relayoutCount = 0
            // update translation
            translationX = 10f
        }

        rule.runOnIdle {
            assertEquals(10f, coordinates.boundsInRoot().left)
            assertEquals(0, remeasureCount)
            assertEquals(0, relayoutCount)
        }
    }

    @Test
    fun updatingLambdaIsNotCausingRemeasureOrRelayout() {
        var lambda by mutableStateOf<GraphicsLayerScope.() -> Unit>({})
        lateinit var coordinates: LayoutCoordinates
        var remeasureCount = 0
        var relayoutCount = 0
        val layoutModifier =
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                remeasureCount++
                layout(placeable.width, placeable.height) {
                    relayoutCount++
                    placeable.place(0, 0)
                }
            }
        rule.setContent {
            Box(Modifier.graphicsLayer(lambda).then(layoutModifier)) {
                Layout(Modifier.onGloballyPositioned { coordinates = it }) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0f, coordinates.boundsInRoot().left)
            // reset counters
            remeasureCount = 0
            relayoutCount = 0
            // update lambda
            lambda = { translationX = 10f }
        }

        rule.runOnIdle {
            assertEquals(10f, coordinates.boundsInRoot().left)
            assertEquals(0, remeasureCount)
            assertEquals(0, relayoutCount)
        }
    }

    @Test
    fun addingLayerForChildDoesntTriggerChildRelayout() {
        var relayoutCount = 0
        var modifierRelayoutCount = 0
        var needLayer by mutableStateOf(false)
        var layerBlockCalled = false
        rule.setContent {
            Layout(
                content = {
                    Layout(
                        modifier =
                            Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    modifierRelayoutCount++
                                    placeable.place(0, 0)
                                }
                            }
                    ) { _, _ ->
                        layout(10, 10) { relayoutCount++ }
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (needLayer) {
                        placeable.placeWithLayer(0, 0) { layerBlockCalled = true }
                    } else {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            relayoutCount = 0
            modifierRelayoutCount = 0
            needLayer = true
        }

        rule.runOnIdle {
            assertEquals(0, relayoutCount)
            assertTrue(layerBlockCalled)
            assertEquals(0, modifierRelayoutCount)
        }
    }

    @Test
    fun movingChildsLayerDoesntTriggerChildRelayout() {
        var relayoutCount = 0
        var modifierRelayoutCount = 0
        var position by mutableStateOf(0)
        rule.setContent {
            Layout(
                content = {
                    Layout(
                        modifier =
                            Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    modifierRelayoutCount++
                                    placeable.place(0, 0)
                                }
                            }
                    ) { _, _ ->
                        layout(10, 10) { relayoutCount++ }
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) { placeable.placeWithLayer(position, 0) }
            }
        }

        rule.runOnIdle {
            relayoutCount = 0
            modifierRelayoutCount = 0
            position = 10
        }

        rule.runOnIdle {
            assertEquals(0, relayoutCount)
            assertEquals(0, modifierRelayoutCount)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun placingWithExplicitLayerDraws() {
        rule.setContent {
            val layer = rememberGraphicsLayer()
            Canvas(
                modifier =
                    Modifier.testTag("tag").layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(10, 10))
                        layout(placeable.width, placeable.height) {
                            placeable.placeWithLayer(0, 0, layer)
                        }
                    }
            ) {
                drawRect(Color.Blue)
            }
        }

        rule.onNodeWithTag("tag").captureToImage().assertPixels(IntSize(10, 10)) { Color.Blue }
    }

    @Test
    fun placingWithExplicitLayerSetsCorrectSizeAndOffset() {
        lateinit var layer: GraphicsLayer
        rule.setContent {
            layer = rememberGraphicsLayer()
            Canvas(
                modifier =
                    Modifier.layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(20, 20))
                        layout(placeable.width, placeable.height) {
                            placeable.placeWithLayer(10, 10, layer)
                        }
                    }
            ) {
                drawRect(Color.Blue)
            }
        }

        rule.runOnIdle {
            assertThat(layer.size.width).isEqualTo(20)
            assertThat(layer.size.height).isEqualTo(20)
            assertThat(layer.topLeft.x).isEqualTo(10)
            assertThat(layer.topLeft.y).isEqualTo(10)
        }
    }

    @Test
    fun layerIsNotReleasedWhenWeStopPlacingIt() {
        lateinit var layer: GraphicsLayer
        var needChild by mutableStateOf(true)
        rule.setContent {
            layer = rememberGraphicsLayer()
            if (needChild) {
                Canvas(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(1, 0, layer)
                            }
                        }
                ) {
                    drawRect(Color.Blue)
                }
            }
        }

        rule.runOnIdle { needChild = false }

        rule.runOnIdle { assertThat(layer.isReleased).isFalse() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchingFromExplicitLayerToImplicit() {
        var useExplicitLayer by mutableStateOf(true)
        rule.setContent {
            val layer =
                if (useExplicitLayer) {
                    rememberGraphicsLayer()
                } else {
                    null
                }
            Canvas(
                modifier =
                    Modifier.testTag("tag")
                        .layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(10, 10))
                            layout(placeable.width, placeable.height) {
                                if (layer != null) {
                                    placeable.placeWithLayer(0, 0, layer)
                                } else {
                                    placeable.place(0, 0)
                                }
                            }
                        }
                        .then(if (layer != null) Modifier else Modifier.graphicsLayer())
            ) {
                drawRect(Color.Blue)
            }
        }

        rule.runOnIdle { useExplicitLayer = false }

        rule.onNodeWithTag("tag").captureToImage().assertPixels(IntSize(10, 10)) { Color.Blue }
    }

    @Test
    fun centerPivotIsUsedWhenWeCalculateBoundsBeforeLayerWasFirstDrawn() {
        var bounds: Rect = Rect.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(10.dp).rotate(180f).onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(0f, 0f, 10f, 10f)) }
    }

    @Test
    fun centerPivotIsCorrectlyCalculatedForOddSize() {
        var bounds: Rect = Rect.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(9.dp).rotate(180f).onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(0f, 0f, 9f, 9f)) }
    }

    @Test
    fun customPivotIsCalculatedCorrectlyWhenWeCalculateBoundsBeforeLayerWasFirstDrawn() {
        var bounds: Rect = Rect.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .graphicsLayer(
                                rotationZ = 180f,
                                transformOrigin = TransformOrigin(1f, 1f)
                            )
                            .onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(10f, 10f, 20f, 20f)) }
    }
}
