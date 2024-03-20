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

package androidx.compose.ui.graphics.layer

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.captureToImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.TestActivity
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.center
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// Relies on View.captureToImage which is Android O+ only
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AndroidGraphicsLayerTest {

    companion object {
        const val TEST_WIDTH = 600
        const val TEST_HEIGHT = 400

        val TEST_SIZE = IntSize(TEST_WIDTH, TEST_HEIGHT)
    }

    @Test
    fun testDrawLayer() {
        var layer: GraphicsLayer? = null
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    assertEquals(IntSize.Zero, this.size)
                    buildLayer {
                        drawRect(Color.Red)
                    }
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(TEST_SIZE, layer!!.size)
                assertEquals(IntOffset.Zero, layer!!.topLeft)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            }
        )
    }

    @Test
    fun testBuildLayerWithSize() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer(IntSize(TEST_WIDTH / 2, TEST_HEIGHT / 2)) {
                        drawRect(Color.Red)
                    }
                }
                drawLayer(layer)
            },
            verify = {
                it.verifyQuadrants(Color.Red, Color.Black, Color.Black, Color.Black)
            }
        )
    }

    @Test
    fun testBuildLayerWithOffset() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset(TEST_WIDTH / 2, TEST_HEIGHT / 2)
        val size = IntSize(TEST_WIDTH, TEST_HEIGHT)
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                }.apply {
                    this.topLeft = topLeft
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Black, Color.Red)
            }
        )
    }

    @Test
    fun testSetOffset() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset(4, 4)
        val size = IntSize(TEST_WIDTH, TEST_HEIGHT)
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        inset(0f, 0f, -4f, -4f) {
                            drawRect(Color.Red)
                        }
                    }
                    this.topLeft = topLeft
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                assertEquals(Color.Red, it[topLeft.x + 1, topLeft.y + 1])
                assertEquals(Color.Black, it[topLeft.x + 1, topLeft.y - 1])
                assertEquals(Color.Black, it[topLeft.x - 1, topLeft.y + 1])
                assertEquals(Color.Red, it[size.width - 2, size.height - 2])
            }
        )
    }

    @Test
    fun testSetAlpha() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                    alpha = 0.5f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                val compositedColor = Color.Red.copy(0.5f).compositeOver(Color.Black)
                it.verifyQuadrants(
                    compositedColor,
                    compositedColor,
                    compositedColor,
                    compositedColor
                )
            }
        )
    }

    @Test
    fun testSetScaleX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            size = Size(this.size.width / 2, this.size.height / 2)
                        )
                    }
                    scaleX = 2f
                    pivotOffset = Offset.Zero
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Black, Color.Black)
            }
        )
    }

    @Test
    fun testSetScaleY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            size = Size(this.size.width / 2, this.size.height / 2)
                        )
                    }
                    scaleY = 2f
                    pivotOffset = Offset.Zero
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Red, Color.Black, Color.Red, Color.Black)
            }
        )
    }

    @Test
    fun testDefaultPivot() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        inset(this.size.width / 4, this.size.height / 4) {
                            drawRect(Color.Red)
                        }
                    }
                    scaleY = 2f
                    scaleX = 2f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            }
        )
    }

    @Test
    fun testBottomRightPivot() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                    scaleY = 0.5f
                    scaleX = 0.5f
                    pivotOffset = Offset(this.size.width.toFloat(), this.size.height.toFloat())
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Black, Color.Red)
            }
        )
    }

    @Test
    fun testTranslationX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red, size = this.size / 2f)
                    }
                    translationX = this.size.width / 2f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Red, Color.Black, Color.Black)
            }
        )
    }

    @Test
    fun testTranslationY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red, size = this.size / 2f)
                    }
                    translationY = this.size.height / 2f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Red, Color.Black)
            }
        )
    }

    @Test
    fun testRotationX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            size = Size(this.size.width, this.size.height / 2)
                        )
                    }
                    rotationX = 45f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                assertEquals(Color.Red, it[size.width / 2, size.height / 4])
                assertEquals(Color.Black, it[size.width / 2, size.height / 2 + 2])

                assertEquals(Color.Black, it[4, size.height / 4])
                assertEquals(Color.Black, it[size.width - 4, size.height / 4])
            }
        )
    }

    @Test
    fun testRotationY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                    pivotOffset = Offset(0f, this.size.height / 2f)
                    rotationY = 45f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                assertEquals(Color.Red, it[2, size.height / 2])
                assertEquals(Color.Red, it[size.width / 4, size.height / 2])
                assertEquals(Color.Black, it[size.width / 2, size.height / 2])
                assertEquals(Color.Black, it[size.width / 4, 4])
                assertEquals(Color.Black, it[size.width / 4, size.height - 4])
            }
        )
    }

    @Test
    fun testRotationZ() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        val rectSize = 100
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            topLeft = Offset(
                                this.size.width / 2f - rectSize / 2f,
                                this.size.height / 2 - rectSize / 2f
                            ),
                            Size(rectSize.toFloat(), rectSize.toFloat())
                        )
                    }
                    rotationZ = 45f
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Black, Color.Black)
                assertEquals(Color.Red, it[size.width / 2, size.height / 2])
                assertEquals(Color.Red, it[size.width / 2, size.height / 2 - rectSize / 2])
                assertEquals(Color.Red, it[size.width / 2, size.height / 2 + rectSize / 2])
                assertEquals(Color.Red, it[size.width / 2 - rectSize / 2, size.height / 2])
                assertEquals(Color.Red, it[size.width / 2 + rectSize / 2, size.height / 2])
                assertEquals(
                    Color.Black,
                    it[size.width / 2 - rectSize / 3, size.height / 2 - rectSize / 2 + 4]
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 - rectSize / 3, size.height / 2 + rectSize / 2 - 4]
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 + rectSize / 3, size.height / 2 - rectSize / 2 + 4]
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 + rectSize / 3, size.height / 2 + rectSize / 2 - 4]
                )
            }
        )
    }

    @Test
    fun testUnboundedClip() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            size = Size(100000f, 100000f)
                        )
                    }
                    // Layer clipping is disabled by default
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(Color.Red, it[0, 0])
                assertEquals(Color.Red, it[it.width - 1, 0])
                assertEquals(Color.Red, it[0, it.height - 1])
                assertEquals(Color.Red, it[it.width - 1, it.height - 1])
                assertEquals(Color.Red, it[it.width / 2, it.height / 2])
            },
            entireScene = true
        )
    }

    @Test
    fun testBoundedClip() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(
                            Color.Red,
                            size = Size(100000f, 100000f)
                        )
                    }
                    clip = true
                }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(Color.Red, it[0, 0])
                assertEquals(Color.Red, it[TEST_WIDTH - 1, 0])
                assertEquals(Color.Red, it[0, TEST_HEIGHT - 1])
                assertEquals(Color.Red, it[TEST_WIDTH - 1, TEST_HEIGHT - 1])
                assertEquals(Color.Red, it[TEST_WIDTH / 2, 0])
                assertEquals(Color.Red, it[TEST_WIDTH / 2, TEST_HEIGHT / 2])

                assertEquals(Color.White, it[0, TEST_HEIGHT + 2])
                assertEquals(Color.White, it[0, it.height - 1])
                assertEquals(Color.White, it[TEST_WIDTH - 1, TEST_HEIGHT + 2])
                assertEquals(Color.White, it[TEST_WIDTH + 1, TEST_HEIGHT])
                assertEquals(Color.White, it[it.width - 1, TEST_HEIGHT - 2])
            },
            entireScene = true
        )
    }

    @Test
    fun testElevation() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize = IntSize(
                    (this.size.width / 2f).toInt(),
                    (this.size.height / 2f).toInt()
                )

                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer(halfSize) {
                        drawRect(targetColor)
                    }
                    shadowElevation = 10f
                }
                drawRect(targetColor)

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height
                translate(this.size.width / 4, this.size.height / 4) {
                    drawLayer(layer!!)
                }
            },
            verify = { pixmap ->
                var shadowPixelCount = 0
                with(pixmap) {
                    for (x in left until right) {
                        for (y in top until bottom) {
                            if (this[x, y] != targetColor) {
                                shadowPixelCount++
                            }
                        }
                    }
                }
                assertTrue(shadowPixelCount > 0)
            }
        )
    }

    @Test
    fun testElevationPath() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize = IntSize(
                    (this.size.width / 2f).toInt(),
                    (this.size.height / 2f).toInt()
                )

                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer(halfSize) {
                        drawRect(targetColor)
                    }
                    setPathOutline(
                        Path().apply {
                            addRect(
                                Rect(
                                    0f,
                                    0f,
                                    halfSize.width.toFloat(),
                                    halfSize.height.toFloat()
                                )
                            )
                        }
                    )
                    shadowElevation = 10f
                }
                drawRect(targetColor)

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height
                translate(this.size.width / 4, this.size.height / 4) {
                    drawLayer(layer!!)
                }
            },
            verify = { pixmap ->
                var shadowPixelCount = 0
                with(pixmap) {
                    for (x in left until right) {
                        for (y in top until bottom) {
                            if (this[x, y] != targetColor) {
                                shadowPixelCount++
                            }
                        }
                    }
                }
                Assert.assertTrue(shadowPixelCount > 0)
            }
        )
    }

    @Test
    fun testElevationRoundRect() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        val radius = 50f
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize = IntSize(
                    (this.size.width / 2f).toInt(),
                    (this.size.height / 2f).toInt()
                )

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height

                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer(halfSize) {
                        drawRect(targetColor)
                    }
                    setRoundRectOutline(IntOffset.Zero, halfSize, radius)
                    shadowElevation = 20f
                }

                drawRect(targetColor)
                translate(left.toFloat(), top.toFloat()) {
                    drawLayer(layer!!)
                }
            },
            verify = { pixmap ->
                fun PixelMap.hasShadowPixels(
                    targetColor: Color,
                    l: Int,
                    t: Int,
                    r: Int,
                    b: Int
                ): Boolean {
                    var shadowCount = 0
                    for (i in l until r) {
                        for (j in t until b) {
                            if (this[i, j] != targetColor) {
                                shadowCount++
                            }
                        }
                    }
                    return shadowCount > 0
                }
                with(pixmap) {
                    assertTrue(
                        hasShadowPixels(
                            targetColor,
                            left,
                            top,
                            left + radius.toInt(),
                            top + radius.toInt()
                        )
                    )
                    assertTrue(
                        hasShadowPixels(
                            targetColor,
                            right - radius.toInt(),
                            top,
                            right,
                            top + radius.toInt()
                        )
                    )
                    assertTrue(
                        hasShadowPixels(
                            targetColor,
                            left,
                            bottom - radius.toInt(),
                            left + radius.toInt(),
                            bottom
                        )
                    )
                    assertTrue(
                        hasShadowPixels(
                            targetColor,
                            right - radius.toInt(),
                            bottom - radius.toInt(),
                            right,
                            bottom
                        )
                    )
                }
            }
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testRenderEffect() {
        var layer: GraphicsLayer?
        val blurRadius = 10f
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                    renderEffect = BlurEffect(blurRadius, blurRadius, TileMode.Decal)
                }
                drawRect(Color.Black)
                drawLayer(layer!!)
            },
            verify = {
                var nonPureRedCount = 0
                for (x in 0 until it.width - blurRadius.toInt()) {
                    for (y in 0 until it.height - blurRadius.toInt()) {
                        val pixelColor = it[x, y]
                        if (pixelColor.blue > 0 || pixelColor.green > 0) {
                            Assert.fail(
                                "Only blue colors are expected. Pixel at [$x, $y] $pixelColor"
                            )
                        }
                        if (pixelColor.red > 0 && pixelColor.red < 1f) {
                            nonPureRedCount++
                        }
                    }
                }
                assertTrue(nonPureRedCount > 0)
            },
            entireScene = false
        )
    }

    @Test
    fun testCompositingStrategyAuto() {
        var layer: GraphicsLayer?
        val bgColor = Color.Black
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        inset(0f, 0f, size.width / 3, size.height / 3) {
                            drawRect(color = Color.Red)
                        }
                        inset(size.width / 3, size.height / 3, 0f, 0f) {
                            drawRect(color = Color.Blue)
                        }
                    }
                    alpha = 0.5f
                    compositingStrategy = CompositingStrategy.Auto
                }
                drawRect(bgColor)
                drawLayer(layer!!)
            },
            verify = { pixelMap ->
                with(pixelMap) {
                    val redWithAlpha = Color.Red.copy(alpha = 0.5f)
                    val blueWithAlpha = Color.Blue.copy(alpha = 0.5f)
                    val expectedTopLeft = redWithAlpha.compositeOver(bgColor)
                    val expectedBottomRight = blueWithAlpha.compositeOver(bgColor)
                    val expectedCenter = blueWithAlpha.compositeOver(bgColor)
                    assertPixelColor(expectedTopLeft, 0, 0)
                    assertPixelColor(Color.Black, width - 1, 0)
                    assertPixelColor(expectedBottomRight, width - 1, height - 1)
                    assertPixelColor(Color.Black, 0, height - 1)
                    assertPixelColor(expectedCenter, width / 2, height / 2)
                }
            }
        )
    }

    @Test
    fun testCompositingStrategyOffscreen() {
        var layer: GraphicsLayer?
        val bgColor = Color.LightGray
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        inset(0f, 0f, size.width / 3, size.height / 3) {
                            drawRect(color = Color.Red)
                        }
                        inset(size.width / 3, size.height / 3, 0f, 0f) {
                            drawRect(color = Color.Blue, blendMode = BlendMode.Xor)
                        }
                    }
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                drawRect(bgColor)
                drawLayer(layer!!)
            },
            verify = { pixelMap ->
                with(pixelMap) {
                    assertPixelColor(Color.Red, 0, 0)
                    assertPixelColor(bgColor, width - 1, 0)
                    assertPixelColor(Color.Blue, width - 1, height - 1)
                    assertPixelColor(bgColor, 0, height - 1)
                    assertPixelColor(bgColor, width / 2, height / 2)
                }
            }
        )
    }

    @Test
    fun testCompositingStrategyModulateAlpha() {
        var layer: GraphicsLayer?
        val bgColor = Color.Black
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        inset(0f, 0f, size.width / 3, size.height / 3) {
                            drawRect(color = Color.Red)
                        }
                        inset(size.width / 3, size.height / 3, 0f, 0f) {
                            drawRect(color = Color.Blue)
                        }
                    }
                    alpha = 0.5f
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                drawRect(bgColor)
                drawLayer(layer!!)
            },
            verify = { pixelMap ->
                with(pixelMap) {
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
        )
    }

    @Test
    fun testCameraDistanceWithRotationY() {
        var layer: GraphicsLayer?
        val bgColor = Color.Gray
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }
                    cameraDistance = 5.0f
                    rotationY = 25f
                }
                drawRect(bgColor)
                drawLayer(layer!!)
            },
            verify = { pixelMap ->
                with(pixelMap) {
                    assertPixelColor(Color.Red, 0, 0)
                    assertPixelColor(Color.Red, 0, height - 1)
                    assertPixelColor(Color.Red, width / 2 - 10, height / 2)
                    assertPixelColor(Color.Gray, width - 1 - 10, height / 2)
                    assertPixelColor(Color.Gray, width - 1, 0)
                    assertPixelColor(Color.Gray, width - 1, height - 1)
                }
            }
        )
    }

    @Test
    fun testTintColorFilter() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(Color.Red)
                    }.apply {
                        colorFilter = tint(Color.Blue)
                    }
                }
                drawLayer(layer!!)
            },
            verify = { pixelMap ->
                with(pixelMap) {
                    assertPixelColor(Color.Blue, 0, 0)
                    assertPixelColor(Color.Blue, width - 1, 0)
                    assertPixelColor(Color.Blue, 0, height - 1)
                    assertPixelColor(Color.Blue, width - 1, height - 1)
                    assertPixelColor(Color.Blue, width / 2, height / 2)
                }
            }
        )
    }

    @Test
    fun testBlendMode() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                val drawScopeSize = this.size
                layer = graphicsContext.createGraphicsLayer().apply {
                    val topLeft = IntOffset(
                        (drawScopeSize.width / 4).toInt(),
                        (drawScopeSize.height / 4).toInt()
                    )
                    val layerSize = IntSize(
                        (drawScopeSize.width / 2).toInt(),
                        (drawScopeSize.height / 2).toInt()
                    )
                    buildLayer(layerSize) {
                        drawRect(Color.Red)
                    }.apply {
                        this.topLeft = topLeft
                        this.blendMode = BlendMode.Xor
                    }
                }
                drawRect(Color.Green)
                drawLayer(layer!!)
                // The layer should clear the original pixels in the destination rendered by the
                // layer. Draw blue underneath the destination to fill the transparent pixels
                // cleared by the layer
                drawRect(Color.Blue, blendMode = BlendMode.DstOver)
            },
            verify = { pixelMap ->
                with(pixelMap) {
                    assertPixelColor(Color.Green, 0, 0)
                    assertPixelColor(Color.Green, width - 1, 0)
                    assertPixelColor(Color.Green, 0, height - 1)
                    assertPixelColor(Color.Green, width - 1, height - 1)

                    val insetLeft = width / 4 + 2
                    val insetTop = height / 4 + 2
                    val insetRight = width - width / 4 - 2
                    val insetBottom = height - height / 4 - 2

                    assertPixelColor(Color.Blue, insetLeft, insetTop)
                    assertPixelColor(Color.Blue, insetRight, insetTop)
                    assertPixelColor(Color.Blue, insetLeft, insetBottom)
                    assertPixelColor(Color.Blue, insetRight, insetBottom)
                    assertPixelColor(Color.Blue, width / 2, height / 2)
                }
            }
        )
    }

    @Test
    fun testRectOutlineClip() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val bgColor = Color.Black
        val targetColor = Color.Red
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(targetColor)
                    }
                    setRectOutline(this.size.center, this.size / 2)
                    clip = true
                }
                drawRect(bgColor)

                left = this.size.center.x.toInt()
                top = this.size.center.y.toInt()
                right = this.size.width.toInt()
                bottom = this.size.height.toInt()

                drawLayer(layer!!)
            },
            verify = { pixmap ->
                with(pixmap) {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val expected = if (x in left until right &&
                                y in top until bottom) {
                                targetColor
                            } else {
                                bgColor
                            }
                            Assert.assertEquals(this[x, y], expected)
                        }
                    }
                }
            }
        )
    }

    @Test
    fun testPathOutlineClip() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val bgColor = Color.Black
        val targetColor = Color.Red
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(targetColor)
                    }
                    setPathOutline(Path().apply {
                        addRect(
                            Rect(
                                size.center.x.toFloat(),
                                size.center.y.toFloat(),
                                size.center.x + size.width.toFloat(),
                                size.center.y + size.height.toFloat()
                            )
                        )
                    })
                    clip = true
                }
                drawRect(bgColor)

                left = this.size.center.x.toInt()
                top = this.size.center.y.toInt()
                right = this.size.width.toInt()
                bottom = this.size.height.toInt()

                drawLayer(layer!!)
            },
            verify = { pixmap ->
                with(pixmap) {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val expected = if (x in left until right &&
                                y in top until bottom) {
                                targetColor
                            } else {
                                bgColor
                            }
                            Assert.assertEquals(this[x, y], expected)
                        }
                    }
                }
            }
        )
    }

    @Test
    fun testRoundRectOutlineClip() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val radius = 50
        val bgColor = Color.Black
        val targetColor = Color.Red
        graphicsLayerTest(
            block = { graphicsContext ->
                layer = graphicsContext.createGraphicsLayer().apply {
                    buildLayer {
                        drawRect(targetColor)
                    }
                    setRoundRectOutline(
                        this.size.center,
                        this.size / 2,
                        radius.toFloat()
                    )
                    clip = true
                }
                drawRect(bgColor)

                left = this.size.center.x.toInt()
                top = this.size.center.y.toInt()
                right = (left + this.size.width / 2).toInt()
                bottom = (top + this.size.height / 2).toInt()

                drawLayer(layer!!)
            },
            verify = { pixmap ->
                with(pixmap) {
                    val offset = 5
                    val startX = left + radius + offset
                    val startY = top + radius + offset
                    val endX = right - radius - offset
                    val endY = bottom - radius - offset
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            if (
                                x in startX until endX &&
                                y in startY until endY) {
                                assertEquals(targetColor, this[x, y])
                            }
                        }
                    }
                    Assert.assertEquals(bgColor, this[offset, offset])
                    Assert.assertEquals(bgColor, this[width - offset, offset])
                    Assert.assertEquals(bgColor, this[offset, height - offset])
                    Assert.assertEquals(bgColor, this[width - offset, height - offset])
                }
            }
        )
    }

    @Test
    fun setOutlineExtensionAppliesValuesCorrectly() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer = graphicsContext.createGraphicsLayer()

                val rectangle = Outline.Rectangle(Rect(1f, 2f, 3f, 4f))
                layer.setOutline(rectangle)
                assertEquals(rectangle, layer.outline)

                val rounded = Outline.Rounded(RoundRect(10f, 20f, 30f, 40f, 5f, 5f))
                layer.setOutline(rounded)
                assertEquals(rounded, layer.outline)

                val path = Path().also { it.addOval(Rect(1f, 2f, 3f, 4f)) }
                val generic = Outline.Generic(path)
                layer.setOutline(generic)
                assertEquals(generic, layer.outline)
            }
        )
    }

    private fun PixelMap.verifyQuadrants(
        topLeft: Color,
        topRight: Color,
        bottomLeft: Color,
        bottomRight: Color
    ) {
        val left = this.width / 4
        val right = this.width / 4 + this.width / 2
        val top = this.height / 4
        val bottom = this.height / 4 + this.height / 2
        assertPixelColor(topLeft, left, top) { "$left, $top is incorrect color" }
        assertPixelColor(topRight, right, top) { "$right, $top is incorrect color" }
        assertPixelColor(bottomLeft, left, bottom) { "$left, $bottom is incorrect color" }
        assertPixelColor(bottomRight, right, bottom) { "$right, $bottom is incorrect color" }
    }

    private fun graphicsLayerTest(
        block: DrawScope.(GraphicsContext) -> Unit,
        verify: ((PixelMap) -> Unit)? = null,
        entireScene: Boolean = false
    ) {
        var scenario: ActivityScenario<TestActivity>? = null
        try {
            var container: ViewGroup? = null
            var contentView: View? = null
            scenario = ActivityScenario.launch(TestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    container = FrameLayout(it).apply {
                        setBackgroundColor(Color.White.toArgb())
                        clipToPadding = false
                        clipChildren = false
                    }
                    val graphicsContext = GraphicsContext(container!!)
                    val content = FrameLayout(it).apply {
                        setLayoutParams(
                            FrameLayout.LayoutParams(
                                TEST_WIDTH,
                                TEST_HEIGHT
                            )
                        )
                        setBackgroundColor(Color.Black.toArgb())
                        foreground = GraphicsContextHostDrawable(graphicsContext, block)
                    }
                    container!!.addView(content)
                    contentView = content
                    it.setContentView(container)
                }
            val resumed = CountDownLatch(1)
            scenario.moveToState(Lifecycle.State.RESUMED)
                .onActivity { activity ->
                    activity.runOnUiThread {
                        contentView!!.invalidate()
                        resumed.countDown()
                    }
                }
            Assert.assertTrue(resumed.await(3000, TimeUnit.MILLISECONDS))

            if (verify != null) {
                val target = if (entireScene) {
                    container!!
                } else {
                    contentView!!
                }
                verify(target.captureToImage().toPixelMap())
            }
        } finally {
            scenario?.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    private class GraphicsContextHostDrawable(
        val graphicsContext: GraphicsContext,
        val block: DrawScope.(GraphicsContext) -> Unit
    ) : Drawable() {

        val canvasDrawScope = CanvasDrawScope()

        override fun draw(canvas: Canvas) {
            val bounds = getBounds()
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()
            canvasDrawScope.draw(
                Density(1f, 1f),
                Ltr,
                androidx.compose.ui.graphics.Canvas(canvas),
                Size(width, height)
            ) {
                block(graphicsContext)
            }
        }

        override fun setAlpha(alpha: Int) {
            // NO-OP
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            // NO-OP
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }
    }
}
