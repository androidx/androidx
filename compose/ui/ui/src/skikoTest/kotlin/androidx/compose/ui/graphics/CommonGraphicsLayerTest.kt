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

package androidx.compose.ui.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
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
import androidx.compose.ui.assertColorsEqual
import androidx.compose.ui.assertOffsetEqual
import androidx.compose.ui.assertRectEqual
import androidx.compose.ui.assertThat
import androidx.compose.ui.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.isEqualTo
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.jetbrains.skia.Bitmap

// A copy from androidInstrumentedTest/kotlin/androidx/compose/ui/draw/GraphicsLayerTest.kt

@OptIn(ExperimentalTestApi::class)
class CommonGraphicsLayerTest {

    @Test
    fun testLayerBoundsPosition() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            FixedSize(
                30,
                Modifier.padding(10).graphicsLayer().onGloballyPositioned { coords = it },
            ) { /* no-op */
            }
        }

        onRoot().apply {
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
    fun testScale() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleX = 2f, scaleY = 3f).onGloballyPositioned {
                        coords = it
                    },
                ) {}
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceXY() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.scale(scaleX = 2f, scaleY = 3f).onGloballyPositioned { coords = it },
                ) {}
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceUniform() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(10, Modifier.scale(scale = 2f).onGloballyPositioned { coords = it }) {}
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 5f, 25f, 25f), bounds)
            assertEquals(Offset(5f, 5f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotation() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleY = 3f, rotationZ = 90f).onGloballyPositioned {
                        coords = it
                    },
                ) {}
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(0f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationConvenience() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(10, Modifier.rotate(90f).onGloballyPositioned { coords = it }) {}
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10.0f, 10f, 20f, 20f), bounds)
            assertEquals(Offset(20f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationPivot() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(
                            rotationZ = 90f,
                            transformOrigin = TransformOrigin(1.0f, 1.0f),
                        )
                        .onGloballyPositioned { coords = it },
                )
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(20f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testTranslationXY() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(translationX = 5.0f, translationY = 8.0f)
                        .onGloballyPositioned { coords = it },
                )
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(15f, 18f, 25f, 28f), bounds)
            assertEquals(Offset(15f, 18f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testClip() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(
                        10,
                        Modifier.graphicsLayer(scaleX = 2f).onGloballyPositioned { coords = it },
                    ) {}
                }
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10f, 10f, 20f, 20f), bounds)
            // Positions aren't clipped
            assertEquals(Offset(5f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testSiblingComparisons() = runComposeUiTest {
        var coords1: LayoutCoordinates? = null
        var coords2: LayoutCoordinates? = null
        setContent {
            with(LocalDensity.current) {
                Box(Modifier.requiredSize(25.toDp()).graphicsLayer(rotationZ = 30f, clip = true)) {
                    Box(
                        Modifier.graphicsLayer(
                                rotationZ = 90f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                clip = true,
                            )
                            .requiredSize(20.toDp(), 10.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned { coords1 = it }
                    )
                    Box(
                        Modifier.graphicsLayer(
                                rotationZ = -90f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                clip = true,
                            )
                            .requiredSize(10.toDp())
                            .align(AbsoluteAlignment.BottomRight)
                            .onGloballyPositioned { coords2 = it }
                    )
                }
            }
        }

        onRoot().apply {
            assertOffsetEqual(Offset(15f, 5f), coords2!!.localPositionOf(coords1!!, Offset.Zero))
            assertOffsetEqual(Offset(-5f, 5f), coords2!!.localPositionOf(coords1!!, Offset(20f, 0f)))
            assertRectEqual(Rect(-5f, -5f, 15f, 5f), coords2!!.localBoundingBoxOf(coords1!!, false))
            assertRectEqual(Rect(0f, 0f, 10f, 5f), coords2!!.localBoundingBoxOf(coords1!!, true))
        }
    }

    @Test
    fun testCameraDistanceWithRotationY() = runComposeUiTest {
        val testTag = "parent"
        setContent {
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

        onNodeWithTag(testTag).captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Red.toArgb(), getColor(0, 0))
            assertEquals(Color.Red.toArgb(), getColor(0, height - 1))
            assertEquals(Color.Red.toArgb(), getColor(width / 2 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getColor(width - 1 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getColor(width - 1, 0))
            assertEquals(Color.Gray.toArgb(), getColor(width - 1, height - 1))
        }
    }

    @Test
    fun testEmptyClip() = runComposeUiTest {
        val EmptyRectangle =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ) = Outline.Rectangle(Rect.Zero)
            }
        val tag = "testTag"
        setContent {
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
        onNodeWithTag(tag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun testTotalClip() = runComposeUiTest {
        var coords: LayoutCoordinates? = null
        setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(10, Modifier.padding(20).onGloballyPositioned { coords = it }) {}
                }
            }
        }

        onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            // should be completely clipped out
            assertEquals(0f, bounds.width)
            assertEquals(0f, bounds.height)
        }
    }

    @Test
    fun testColorFilter() = runComposeUiTest {
        val testTag = "colorFilterTag"
        setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(modifier = Modifier.requiredSize(10.dp).background(Color.Green))
                Box(
                    modifier =
                        Modifier.requiredSize(10.dp)
                            .graphicsLayer(
                                colorFilter = LightingColorFilter(Color.White, Color.Red)
                            )
                            .background(Color.Black)
                )
            }
        }

        onNodeWithTag(testTag).captureToImage().asSkiaBitmap().apply {
            assertColor(Color.Red, 0, 0)
            assertColor(Color.Red, 0, height - 1)
            assertColor(Color.Red, width / 2 - 10, height / 2)
        }
    }

    @Test
    fun testColorFilterAsScope() = runComposeUiTest {
        val testTag = "colorFilterTag"
        setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(modifier = Modifier.requiredSize(10.dp).background(Color.Green))
                Box(
                    modifier =
                        Modifier.requiredSize(10.dp)
                            .graphicsLayer {
                                colorFilter = LightingColorFilter(Color.White, Color.Red)
                            }
                            .background(Color.Black)
                )
            }
        }

        onNodeWithTag(testTag).captureToImage().asSkiaBitmap().apply {
            assertColor(Color.Red, 0, 0)
            assertColor(Color.Red, 0, height - 1)
            assertColor(Color.Red, width / 2 - 10, height / 2)
        }
    }

    @Test
    fun testBlendMode() = runComposeUiTest {
        val testTag = "blendModeTag"
        setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(modifier = Modifier.requiredSize(10.dp).background(Color.Yellow))
                Box(
                    modifier =
                        Modifier.requiredSize(10.dp)
                            .graphicsLayer(blendMode = BlendMode.Dst)
                            .background(Color.Blue)
                )
            }
        }

        onNodeWithTag(testTag).captureToImage().asSkiaBitmap().apply {
            assertColor(Color.Yellow, 0, 0)
            assertColor(Color.Yellow, 0, height - 1)
            assertColor(Color.Yellow, width / 2 - 10, height / 2)
        }
    }

    @Test
    fun testBlendModeAsScope() = runComposeUiTest {
        val testTag = "blendModeTag"
        setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(modifier = Modifier.requiredSize(10.dp).background(Color.Yellow))
                Box(
                    modifier =
                        Modifier.requiredSize(10.dp)
                            .graphicsLayer { blendMode = BlendMode.Dst }
                            .background(Color.Blue)
                )
            }
        }

        onNodeWithTag(testTag).captureToImage().asSkiaBitmap().apply {
            assertColor(Color.Yellow, 0, 0)
            assertColor(Color.Yellow, 0, height - 1)
            assertColor(Color.Yellow, width / 2 - 10, height / 2)
        }
    }

    @Composable
    fun BoxBlur(tag: String, size: Float, blurRadius: Float) {
        BoxRenderEffect(
            tag,
            (size / LocalDensity.current.density).dp,
            ({ BlurEffect(blurRadius, blurRadius, TileMode.Decal) }),
        ) {
            inset(blurRadius, blurRadius) { drawRect(Color.Blue) }
        }
    }

    @Composable
    fun BoxRenderEffect(
        tag: String,
        size: Dp,
        renderEffectCreator: () -> RenderEffect,
        drawBlock: DrawScope.() -> Unit,
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
    fun testBlurEffect() = runComposeUiTest {
        val tag = "blurTag"
        val size = 100f
        val blurRadius = 10f
        setContent { BoxBlur(tag, size, blurRadius) }
        onNodeWithTag(tag).captureToImage().apply {
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
    fun testZeroRadiusBlurDoesNotCrash() = runComposeUiTest {
        val tag = "blurTag"
        val size = 100f
        setContent { BoxBlur(tag, size, 0f) }
    }

    @Test
    fun testOffsetEffect() = runComposeUiTest {
        val tag = "blurTag"
        val size = 100f
        setContent {
            BoxRenderEffect(
                tag,
                (size / LocalDensity.current.density).dp,
                { OffsetEffect(20f, 20f) },
            ) {
                drawRect(Color.Blue, size = Size(this.size.width - 20, this.size.height - 20))
            }
        }
        onNodeWithTag(tag).captureToImage().apply {
            val pixelMap = toPixelMap()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (x >= 20f && y >= 20f) {
                        assertEquals(Color.Blue, pixelMap[x, y], "Index $x, $y should be blue")
                    } else {
                        assertEquals(Color.Black, pixelMap[x, y], "Index $x, $y should be black")
                    }
                }
            }
        }
    }

    @Test
    fun invalidateWhenWeHaveSemanticModifierAfterLayer() = runComposeUiTest {
        var color by mutableStateOf(Color.Red)
        setContent { FixedSize(5, Modifier.graphicsLayer().testTag("tag").background(color)) }

        runOnIdle { color = Color.Green }

        onNodeWithTag("tag").captureToImage().assertPixels { color }
    }

    @Test
    fun testDpPixelConversions() = runComposeUiTest {
        var density: Density? = null
        var testDpConversion = 0f
        var testFontScaleConversion = 0f
        setContent {
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

        runOnIdle {
            with(density!!) {
                assertEquals(2.dp.toPx(), testDpConversion)
                assertEquals(3.dp.toSp().toPx(), testFontScaleConversion)
            }
        }
    }

    @Test
    fun testClickOnScaledElement() = runComposeUiTest {
        var firstClicked = false
        var secondClicked = false
        setContent {
            Layout(
                content = {
                    Box(Modifier.fillMaxSize().clickable { firstClicked = true })
                    Box(Modifier.fillMaxSize().clickable { secondClicked = true })
                },
                modifier = Modifier.testTag("layout"),
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

        onNodeWithTag("layout").performTouchInput { click(position = Offset(50f, 170f)) }

        runOnIdle {
            assertFalse(firstClicked, "First element is clicked")
            assertTrue(secondClicked, "Second element is not clicked")
        }
    }

    @Test
    fun usingNestedDerivedStateInGraphicsLayerBlock() = runComposeUiTest {
        val mutableState = mutableStateOf(1f)
        val derivedState by derivedStateOf { mutableState.value }
        val nestedDerivedState by derivedStateOf { derivedState }
        var valueReadInGraphicsLayer = Float.MIN_VALUE

        setContent {
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

        runOnIdle { assertEquals(2f, valueReadInGraphicsLayer) }
    }

    @Test
    fun testCompositingStrategyModulateAlpha() = runComposeUiTest {
        val tag = "testTag"
        val dimen = 200
        setContent {
            Canvas(
                modifier =
                    Modifier.testTag(tag)
                        .size((dimen / LocalDensity.current.density).dp)
                        .background(Color.Black)
                        .graphicsLayer(
                            alpha = 0.5f,
                            compositingStrategy = CompositingStrategy.ModulateAlpha,
                        )
            ) {
                inset(0f, 0f, size.width / 3, size.height / 3) { drawRect(color = Color.Red) }
                inset(size.width / 3, size.height / 3, 0f, 0f) { drawRect(color = Color.Blue) }
            }
        }

        onNodeWithTag(tag).captureToImage().apply {
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

    @Test
    fun testCompositingStrategyAlways() = runComposeUiTest {
        val tag = "testTag"
        val dimen = 200
        setContent {
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

        onNodeWithTag(tag).captureToImage().apply {
            with(toPixelMap()) {
                assertPixelColor(Color.Red, 0, 0)
                assertPixelColor(Color.LightGray, width - 1, 0)
                assertPixelColor(Color.Blue, width - 1, height - 1)
                assertPixelColor(Color.LightGray, 0, height - 1)
                assertPixelColor(Color.LightGray, width / 2, height / 2)
            }
        }
    }

    @Test
    fun testCompositingStrategyAuto() = runComposeUiTest {
        val tag = "testTag"
        val dimen = 200
        setContent {
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

        onNodeWithTag(tag).captureToImage().apply {
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
    fun testGraphicsLayerScopeSize() = runComposeUiTest {
        val widthDp = 200.dp
        val heightDp = 500.dp

        var graphicsLayerWidth = 0f
        var graphicsLayerHeight = 0f
        var drawScopeWidth = -1f
        var drawScopeHeight = -1f
        setContent {
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
        runOnIdle {
            assertEquals(drawScopeWidth, graphicsLayerWidth)
            assertEquals(drawScopeHeight, graphicsLayerHeight)
        }
    }

    @Test
    fun testGraphicsLayerSizeAfterRelayout() = runComposeUiTest {
        var composableSize by mutableStateOf(20.dp)
        var graphicsLayerWidth = -1f
        var graphicsLayerHeight = -1f
        var drawScopeWidth = 0f
        var drawScopeHeight = 0f

        var density: Density? = null

        setContent {
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

        waitForIdle()

        assertNotNull(density)

        var sizePx = with(density!!) { ceil(composableSize.toPx()) }
        assertEquals(sizePx, graphicsLayerWidth)
        assertEquals(sizePx, graphicsLayerHeight)
        assertEquals(sizePx, drawScopeWidth)
        assertEquals(sizePx, drawScopeHeight)

        composableSize = 40.dp

        waitForIdle()

        sizePx = with(density!!) { ceil(composableSize.toPx()) }
        assertEquals(sizePx, graphicsLayerWidth)
        assertEquals(sizePx, graphicsLayerHeight)
        assertEquals(sizePx, drawScopeWidth)
        assertEquals(sizePx, drawScopeHeight)
    }

    @Test
    fun removingGraphicsLayerInvalidatesParentLayer() = runComposeUiTest {
        var toggle by mutableStateOf(true)
        val size = 100
        setContent {
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

        onNodeWithTag("lazy").captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Blue.toArgb(), getColor(10, (size * 1.5f).roundToInt()))
            assertEquals(Color.Blue.toArgb(), getColor(10, (size * 2.5f).roundToInt()))
        }

        runOnIdle { toggle = !toggle }

        onNodeWithTag("lazy").captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Red.toArgb(), getColor(10, (size * 1.5f).roundToInt()))
            assertEquals(Color.Red.toArgb(), getColor(10, (size * 2.5f).roundToInt()))
        }
    }

    // Repro test for b/298520326. Unfortunately, this test does not successfully reproduce the
    // issue prior to the "fix", so is not a valid regression test. The act of calling
    // `captureToImage()` causes the bug to disappear.
    @Test
    fun removingGraphicsLayerInvalidatesParentLayer2() = runComposeUiTest {
        var toggle by mutableStateOf(false)
        val size = 100
        setContent {
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
                Box(Modifier.Companion.background(Color.Red).size(sizeDp))
            }
        }

        val pt = (size * 0.5f).roundToInt()

        onNodeWithTag("outer").captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Red.toArgb(), getColor(pt, pt))
        }

        runOnIdle { toggle = !toggle }

        onNodeWithTag("outer").captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Red.toArgb(), getColor(pt, pt))
        }

        runOnIdle { toggle = !toggle }

        onNodeWithTag("outer").captureToImage().asSkiaBitmap().apply {
            assertEquals(Color.Red.toArgb(), getColor(pt, pt))
        }
    }

    @Test
    fun removingGraphicsLayerModifierResetsItsAction() = runComposeUiTest {
        var addGraphicsLayer by mutableStateOf(true)
        lateinit var coordinates: LayoutCoordinates
        setContent {
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

        runOnIdle {
            assertEquals(Rect(10f, 0f, 20f, 10f), coordinates.boundsInRoot())
            addGraphicsLayer = false
        }

        runOnIdle { assertEquals(Rect(0f, 0f, 10f, 10f), coordinates.boundsInRoot()) }
    }

    @Test
    fun invalidationAfterMovingMovableContentWithLayer() = runComposeUiTest {
        var moveContent by mutableStateOf(false)
        var counter by mutableStateOf(0)
        var counterReadInDrawing = -1
        val content = movableContentOf {
            Box(Modifier.size(5.dp).graphicsLayer().drawBehind { counterReadInDrawing = counter })
        }

        setContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }

        runOnIdle { moveContent = true }

        runOnIdle {
            assertThat(counterReadInDrawing).isEqualTo(counter)
            counter++
        }

        runOnIdle { assertThat(counterReadInDrawing).isEqualTo(counter) }
    }

    @Test
    fun updatingLayerPropertiesAfterMovingMovableContent() = runComposeUiTest {
        var moveContent by mutableStateOf(false)
        var counter by mutableStateOf(0)
        var counterReadInLayerBlock = -1
        val content = movableContentOf {
            Box(Modifier.size(5.dp).graphicsLayer { counterReadInLayerBlock = counter })
        }

        setContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }

        runOnIdle { moveContent = true }

        runOnIdle {
            assertThat(counterReadInLayerBlock).isEqualTo(counter)
            counter++
        }

        runOnIdle { assertThat(counterReadInLayerBlock).isEqualTo(counter) }
    }

    @Test
    fun updatingValueIsNotCausingRemeasureOrRelayout() = runComposeUiTest {
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
        setContent {
            Box(Modifier.graphicsLayer(translationX = translationX).then(layoutModifier)) {
                Layout(Modifier.onGloballyPositioned { coordinates = it }) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        runOnIdle {
            assertEquals(0f, coordinates.boundsInRoot().left)
            // reset counters
            remeasureCount = 0
            relayoutCount = 0
            // update translation
            translationX = 10f
        }

        runOnIdle {
            assertEquals(10f, coordinates.boundsInRoot().left)
            assertEquals(0, remeasureCount)
            assertEquals(0, relayoutCount)
        }
    }

    @Test
    fun updatingLambdaIsNotCausingRemeasureOrRelayout() = runComposeUiTest {
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
        setContent {
            Box(Modifier.graphicsLayer(lambda).then(layoutModifier)) {
                Layout(Modifier.onGloballyPositioned { coordinates = it }) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        runOnIdle {
            assertEquals(0f, coordinates.boundsInRoot().left)
            // reset counters
            remeasureCount = 0
            relayoutCount = 0
            // update lambda
            lambda = { translationX = 10f }
        }

        runOnIdle {
            assertEquals(10f, coordinates.boundsInRoot().left)
            assertEquals(0, remeasureCount)
            assertEquals(0, relayoutCount)
        }
    }

    @Test
    fun addingLayerForChildDoesntTriggerChildRelayout() = runComposeUiTest {
        var relayoutCount = 0
        var modifierRelayoutCount = 0
        var needLayer by mutableStateOf(false)
        var layerBlockCalled = false
        setContent {
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

        runOnIdle {
            relayoutCount = 0
            modifierRelayoutCount = 0
            needLayer = true
        }

        runOnIdle {
            assertEquals(0, relayoutCount)
            assertTrue(layerBlockCalled)
            assertEquals(0, modifierRelayoutCount)
        }
    }

    @Test
    fun movingChildsLayerDoesntTriggerChildRelayout() = runComposeUiTest {
        var relayoutCount = 0
        var modifierRelayoutCount = 0
        var position by mutableStateOf(0)
        setContent {
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

        runOnIdle {
            relayoutCount = 0
            modifierRelayoutCount = 0
            position = 10
        }

        runOnIdle {
            assertEquals(0, relayoutCount)
            assertEquals(0, modifierRelayoutCount)
        }
    }

    @Test
    fun placingWithExplicitLayerDraws() = runComposeUiTest {
        setContent {
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

        onNodeWithTag("tag").captureToImage().assertPixels(IntSize(10, 10)) { Color.Blue }
    }

    @Test
    fun placingWithExplicitLayerSetsCorrectSizeAndOffset() = runComposeUiTest {
        lateinit var layer: GraphicsLayer
        setContent {
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

        runOnIdle {
            assertThat(layer.size.width).isEqualTo(20)
            assertThat(layer.size.height).isEqualTo(20)
            assertThat(layer.topLeft.x).isEqualTo(10)
            assertThat(layer.topLeft.y).isEqualTo(10)
        }
    }

    @Test
    fun layerIsNotReleasedWhenWeStopPlacingIt() = runComposeUiTest {
        lateinit var layer: GraphicsLayer
        var needChild by mutableStateOf(true)
        setContent {
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

        runOnIdle { needChild = false }

        runOnIdle { assertFalse(layer.isReleased) }
    }

    @Test
    fun switchingFromExplicitLayerToImplicit() = runComposeUiTest {
        var useExplicitLayer by mutableStateOf(true)
        setContent {
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

        runOnIdle { useExplicitLayer = false }

        onNodeWithTag("tag").captureToImage().assertPixels(IntSize(10, 10)) { Color.Blue }
    }

    @Test
    fun centerPivotIsUsedWhenWeCalculateBoundsBeforeLayerWasFirstDrawn() = runComposeUiTest {
        var bounds: Rect = Rect.Zero
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(10.dp).rotate(180f).onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        runOnIdle { assertRectEqual(bounds, Rect(0f, 0f, 10f, 10f)) }
    }

    @Test
    fun centerPivotIsCorrectlyCalculatedForOddSize() = runComposeUiTest {
        var bounds: Rect = Rect.Zero
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(9.dp).rotate(180f).onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        runOnIdle { assertRectEqual(bounds, Rect(0f, 0f, 9f, 9f)) }
    }

    @Test
    fun customPivotIsCalculatedCorrectlyWhenWeCalculateBoundsBeforeLayerWasFirstDrawn() = runComposeUiTest {
        var bounds: Rect = Rect.Zero
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .graphicsLayer(
                                rotationZ = 180f,
                                transformOrigin = TransformOrigin(1f, 1f),
                            )
                            .onPlaced { bounds = it.boundsInRoot() }
                )
            }
        }

        runOnIdle { assertRectEqual(bounds, Rect(10f, 10f, 20f, 20f)) }
    }

    @Test
    fun reusedLayerIsRedrawn() = runComposeUiTest {
        val drawRed = mutableStateOf(true)
        setContent {
            Box(Modifier.graphicsLayer().testTag("content")) {
                ReusableContent(drawRed.value) {
                    val thirtyPixelsInDp = with(LocalDensity.current) { 30.toDp() }
                    Box(Modifier.size(thirtyPixelsInDp).graphicsLayer()) {
                        if (drawRed.value) {
                            Box(Modifier.fillMaxSize().background(Color.Red))
                        } else {
                            Box(Modifier.fillMaxSize().background(Color.Blue))
                        }
                    }
                }
            }
        }
        onNodeWithTag("content").captureToImage().asSkiaBitmap().apply {
            assertThat(width).isEqualTo(30)
            assertThat(height).isEqualTo(30)
            assertThat(getColor(15, 15)).isEqualTo(Color.Red.toArgb())
        }
        drawRed.value = false
        waitForIdle()
        onNodeWithTag("content").captureToImage().asSkiaBitmap().apply {
            assertThat(width).isEqualTo(30)
            assertThat(height).isEqualTo(30)
            assertThat(getColor(15, 15)).isEqualTo(Color.Blue.toArgb())
        }
    }

    @Test
    fun layerIsCorrectlyRecreatedWithClipAppliedAfterReuse() = runComposeUiTest {
        var switch by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.testTag("tag").background(Color.Blue)) {
                    ReusableContent(switch) {
                        Canvas(
                            modifier = Modifier.padding(5.dp).size(5.dp).graphicsLayer(clip = true)
                        ) {
                            drawRect(Color.Red, Offset(-5f, -5f), Size(15f, 15f))
                        }
                    }
                }
            }
        }

        fun assertPixels() {
            onNodeWithTag("tag").captureToImage().assertPixels(IntSize(15, 15)) {
                if (it.x in 5 until 10 && it.y in 5 until 10) {
                    Color.Red
                } else {
                    Color.Blue
                }
            }
        }

        assertPixels()

        runOnIdle { switch = !switch }

        assertPixels()
    }

    @Test
    fun layerIsCorrectlyRecreatedWithClipAppliedWhenMoved() = runComposeUiTest {
        var switch by mutableStateOf(true)
        val moveable = movableContentOf {
            Canvas(modifier = Modifier.padding(5.dp).size(5.dp).graphicsLayer(clip = true)) {
                drawRect(Color.Red, Offset(-5f, -5f), Size(15f, 15f))
            }
        }
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.testTag("tag").background(Color.Blue)) {
                    if (switch) {
                        moveable()
                    } else {
                        moveable()
                    }
                }
            }
        }

        fun assertPixels() {
            onNodeWithTag("tag").captureToImage().assertPixels(IntSize(15, 15)) {
                if (it.x in 5 until 10 && it.y in 5 until 10) {
                    Color.Red
                } else {
                    Color.Blue
                }
            }
        }

        assertPixels()

        runOnIdle { switch = !switch }

        assertPixels()
    }

    @Test
    fun testLayerOutsetsWithImplicitClipToBounds() = runComposeUiTest {
        val outerBoxSizePx = 100
        val innerBoxSizePx = 50
        val outsetsPx = 20
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val outerBoxSizeDp = outerBoxSizePx.dp
                val outsetsDp = outsetsPx.dp
                Box(Modifier.size(outerBoxSizeDp).background(Color.White)) {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = 0.5f
                            outsets = LayerOutsets(outsetsDp)
                        }
                    ) {
                        val innerBoxSizeDp = innerBoxSizePx.dp
                        Box(
                            Modifier.size(innerBoxSizeDp).drawBehind {
                                drawRect(
                                    Color.Red,
                                    size = Size(outerBoxSizePx.toFloat(), outerBoxSizePx.toFloat()),
                                )
                            }
                        )
                    }
                }
            }
        }

        val compositedColor = Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
        onRoot().captureToImage().apply {
            with(toPixelMap()) {
                assertEqualsWithTolerance(compositedColor, this[0, 0])
                assertEqualsWithTolerance(
                    compositedColor,
                    this[innerBoxSizePx + outsetsPx - 3, innerBoxSizePx + outsetsPx - 3],
                )
                assertEqualsWithTolerance(compositedColor, this[0, innerBoxSizePx + outsetsPx - 3])
                assertEqualsWithTolerance(compositedColor, this[innerBoxSizePx + outsetsPx - 3, 0])
                assertEqualsWithTolerance(Color.White, this[outerBoxSizePx - 5, outerBoxSizePx - 5])
            }
        }
    }

    @Test
    fun testLayerOutsetsUpdatesCorrectly() = runComposeUiTest {
        val outerBoxSizePx = 100
        val innerBoxSizePx = 50
        var outsetsPx by mutableStateOf(20)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val outerBoxSizeDp = outerBoxSizePx.dp
                val outsetsDp = outsetsPx.dp
                Box(Modifier.size(outerBoxSizeDp).background(Color.White)) {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = 0.5f
                            outsets = LayerOutsets(outsetsDp)
                        }
                    ) {
                        val innerBoxSizeDp = innerBoxSizePx.dp
                        Box(
                            Modifier.size(innerBoxSizeDp).drawBehind {
                                drawRect(
                                    Color.Red,
                                    size = Size(outerBoxSizePx.toFloat(), outerBoxSizePx.toFloat()),
                                )
                            }
                        )
                    }
                }
            }
        }

        val compositedColor = Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
        onRoot().captureToImage().apply {
            with(toPixelMap()) {
                assertEqualsWithTolerance(compositedColor, this[0, 0])
                assertEqualsWithTolerance(
                    compositedColor,
                    this[innerBoxSizePx + outsetsPx - 3, innerBoxSizePx + outsetsPx - 3],
                )
                assertEqualsWithTolerance(compositedColor, this[0, innerBoxSizePx + outsetsPx - 3])
                assertEqualsWithTolerance(compositedColor, this[innerBoxSizePx + outsetsPx - 3, 0])
                assertEqualsWithTolerance(Color.White, this[outerBoxSizePx - 5, outerBoxSizePx - 5])
            }
        }

        // Reduce outsets to zero — the overflow must now be clipped.
        runOnIdle { outsetsPx = 0 }
        onRoot().captureToImage().apply {
            with(toPixelMap()) {
                assertEqualsWithTolerance(compositedColor, this[0, 0])
                assertEqualsWithTolerance(Color.White, this[innerBoxSizePx, innerBoxSizePx])
                assertEqualsWithTolerance(compositedColor, this[0, innerBoxSizePx - 3])
                assertEqualsWithTolerance(Color.White, this[innerBoxSizePx, 0])
                assertEqualsWithTolerance(Color.White, this[outerBoxSizePx - 5, outerBoxSizePx - 5])
            }
        }
    }

    @Test
    fun testLayerOutsetsWithPivot() = runComposeUiTest {
        val outerBoxSizePx = 100
        val innerBoxSizePx = 50
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val outerBoxSizeDp = outerBoxSizePx.dp
                Box(Modifier.size(outerBoxSizeDp).background(Color.White)) {
                    Box(
                        Modifier.graphicsLayer {
                            rotationZ = 90f
                            outsets = LayerOutsets(10.dp, 100.dp, 5.dp, 50.dp)
                            // Pivot must be calculated on the original layer size (without outsets)
                            transformOrigin = TransformOrigin(1.0f, 1.0f)
                        }
                    ) {
                        val innerBoxSizeDp = innerBoxSizePx.dp
                        Box(Modifier.size(innerBoxSizeDp).background(Color.Red))
                    }
                }
            }
        }

        val pixelMap = onRoot().captureToImage().toPixelMap()
        for (i in 0 until outerBoxSizePx) {
            for (j in 0 until outerBoxSizePx) {
                if (innerBoxSizePx in (j + 1)..i) {
                    assertEqualsWithTolerance(Color.Red, pixelMap[i, j])
                } else {
                    assertEqualsWithTolerance(Color.White, pixelMap[i, j])
                }
            }
        }
    }
}

private fun assertEqualsWithTolerance(expected: Color, actual: Color, tolerance: Float = 0.03f) {
    assertColorsEqual(expected, actual, alphaTolerance = tolerance) {
        "Expected $expected but was $actual"
    }
}

fun Bitmap.assertColor(expectedColor: Color, x: Int, y: Int) {
    val pixel = Color(getColor(x, y))
    assertColorsEqual(expectedColor, pixel) {
        "Pixel [$x, $y] is expected to be $expectedColor, but was $pixel"
    }
}
