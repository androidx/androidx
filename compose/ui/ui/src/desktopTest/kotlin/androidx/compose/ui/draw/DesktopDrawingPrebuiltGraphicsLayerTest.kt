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

package androidx.compose.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Surface
import org.junit.After
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(InternalTestApi::class)
class DesktopDrawingPrebuiltGraphicsLayerTest {

    @get:Rule
    val rule = DesktopComposeTestRule()

    private val size = 2
    private val sizeDp = with(rule.density) { size.toDp() }
    private val expectedSize = IntSize(size, size)

    private var layer: GraphicsLayer? = null
    private var context: GraphicsContext? = null
    private var drawPrebuiltLayer by mutableStateOf(false)

    @After
    fun releaseLayer() {
        rule.runOnUiThread {
            layer?.let {
                context!!.releaseGraphicsLayer(it)
            }
            layer = null
        }
    }

    @Test
    fun continueDrawingPrebuiltLayer() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red }
    }

    @Test
    fun sizeIsCorrect() {
        rule.setContent {
            ColoredBox()
        }

        rule.runOnIdle {
            Truth.assertThat(layer!!.size).isEqualTo(IntSize(size, size))
        }
    }

    @Test
    fun drawingWithAlpha() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
            layer!!.alpha = 0.5f
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red.copy(alpha = 0.5f).compositeOver(Color.White) }
    }

    @Ignore("b/329262831")
    @Test
    fun keepComposingTheNodeWeTookLayerFrom() {
        var color by mutableStateOf(Color.Blue)

        rule.setContent {
            Column {
                ColoredBox(color = { color })
                if (drawPrebuiltLayer) {
                    LayerDrawingBox()
                }
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
        }

        rule.onNodeWithTag(ColoredBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Blue }
        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Blue }

        rule.runOnUiThread {
            color = Color.Green
        }

        rule.onNodeWithTag(ColoredBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Green }
        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Green }
    }

    @Test
    fun drawNestedLayers_drawLayer() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Canvas(
                        Modifier
                            .size(sizeDp)
                            .drawIntoLayer(rememberGraphicsLayer())
                    ) {
                        drawRect(Color.Red)
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red }
    }

    @Test
    fun keepDrawingNestedLayers_drawLayer_deeper() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Box(
                        Modifier.drawIntoLayer(
                            rememberGraphicsLayer()
                        )
                    ) {
                        Canvas(
                            Modifier
                                .size(sizeDp)
                                .drawIntoLayer(rememberGraphicsLayer())
                        ) {
                            drawRect(Color.Red)
                        }
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red }
    }

    @Ignore("remove annotation when Modifier.graphicsLayer() will use the same layer mechanism")
    @Test
    fun keepDrawingNestedLayers_graphicsLayerModifier() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Box(
                        Modifier.graphicsLayer()
                    ) {
                        Canvas(
                            Modifier
                                .size(sizeDp)
                                .graphicsLayer()
                        ) {
                            drawRect(Color.Red)
                        }
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red }
    }

    @Test
    fun keepDrawingLayerFromANodeScheduledForInvalidation() {
        val counter = mutableStateOf(0)
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox(color = {
                    counter.value
                    Color.Red
                })
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true

            // changing the counter to trigger the layer invalidation. the invalidation should
            // be ignored in the end as we will release the layer before it will be drawn
            counter.value++
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red }
    }

    @Ignore("b/329417380")
    @Test
    fun updateLayerProperties() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
            layer!!.alpha = 1f
        }

        rule.runOnIdle {
            layer!!.alpha = 0.5f
        }

        rule.onNodeWithTag(LayerDrawingBoxTag)
            .captureToImage()
            .assertPixels(expectedSize) { Color.Red.copy(alpha = 0.5f).compositeOver(Color.White) }
    }

    @Composable
    private fun ColoredBox(modifier: Modifier = Modifier, color: () -> Color = { Color.Red }) {
        Canvas(
            modifier
                .size(sizeDp)
                .testTag(ColoredBoxTag)
                .drawIntoLayer()
        ) {
            drawRect(color())
        }
    }

    @Composable
    private fun obtainLayer(): GraphicsLayer {
        context = LocalGraphicsContext.current
        return layer ?: context!!.createGraphicsLayer().also { layer = it }
    }

    @Composable
    private fun Modifier.drawIntoLayer(
        layer: GraphicsLayer = obtainLayer()
    ): Modifier {
        return drawWithContent {
            layer.buildLayer {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        }
    }

    @Composable
    private fun LayerDrawingBox() {
        Canvas(
            Modifier
                .size(sizeDp)
                .testTag(LayerDrawingBoxTag)
        ) {
            drawRect(Color.White)
            layer?.let {
                drawLayer(it)
            }
        }
    }

    fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
        val size = rule.scene.contentSize
        val surface = Surface.makeRasterN32Premul(size.width, size.height)
        val canvas = surface.canvas
        rule.scene.render(canvas, rule.mainClock.currentTime * 1_000_000)

        val bounds = getBoundsInRoot()
        val rect = with(rule.density) {
            IRect.makeLTRB(
                bounds.left.roundToPx(),
                bounds.top.roundToPx(),
                bounds.right.roundToPx(),
                bounds.bottom.roundToPx(),
            )
        }
        return surface.makeImageSnapshot(rect)!!.toComposeImageBitmap()
    }
}

private val LayerDrawingBoxTag = "LayerDrawingBoxTag"
private val ColoredBoxTag = "RedBoxTag"

// next two functions are copies from our Android utils:

/**
 * A helper function to run asserts on [ImageBitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
fun ImageBitmap.assertPixels(
    expectedSize: IntSize? = null,
    expectedColorProvider: (pos: IntOffset) -> Color?
) {
    if (expectedSize != null) {
        if (width != expectedSize.width || height != expectedSize.height) {
            throw AssertionError(
                "Bitmap size is wrong! Expected '$expectedSize' but got " +
                    "'$width x $height'"
            )
        }
    }

    val pixel = toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pxPos = IntOffset(x, y)
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null) {
                pixel.assertPixelColor(expectedClr, x, y)
            }
        }
    }
}

/**
 * Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected].
 */
fun PixelMap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) {
    val color = this[x, y]
    val errorString = error(color)
    Assert.assertEquals(errorString, expected.red, color.red, 0.02f)
    Assert.assertEquals(errorString, expected.green, color.green, 0.02f)
    Assert.assertEquals(errorString, expected.blue, color.blue, 0.02f)
    Assert.assertEquals(errorString, expected.alpha, color.alpha, 0.02f)
}
