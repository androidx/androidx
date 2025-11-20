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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.StrictMode
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.TestActivity
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidGraphicsLayerTest {

    companion object {
        const val TEST_WIDTH = 600
        const val TEST_HEIGHT = 400

        val TEST_SIZE = IntSize(TEST_WIDTH, TEST_HEIGHT)
    }

    @Test
    fun testGraphicsLayerBitmap() {
        lateinit var layer: GraphicsLayer
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        assertEquals(IntSize.Zero, this.size)
                        record {
                            drawRect(Color.Red, size = size / 2f)
                            drawRect(
                                Color.Blue,
                                topLeft = Offset(size.width / 2f, 0f),
                                size = size / 2f,
                            )
                            drawRect(
                                Color.Green,
                                topLeft = Offset(0f, size.height / 2f),
                                size = size / 2f,
                            )
                            drawRect(
                                Color.Black,
                                topLeft = Offset(size.width / 2f, size.height / 2f),
                                size = size / 2f,
                            )
                        }
                    }
            },
            verify = {
                val bitmap: ImageBitmap = layer.toImageBitmap()
                assertNotNull(bitmap)
                assertEquals(TEST_SIZE, IntSize(bitmap.width, bitmap.height))
                bitmap.toPixelMap().verifyQuadrants(Color.Red, Color.Blue, Color.Green, Color.Black)
            },
        )
    }

    @Test
    fun testGraphicsLayerBitmapAfterDependencyReleased() {
        class ColorProvider {
            val color: Color = Color.Red
        }
        var provider: ColorProvider? = ColorProvider()
        var graphicsLayer: GraphicsLayer? = null
        graphicsLayerTest(
            block = { graphicsContext ->
                graphicsContext.createGraphicsLayer().apply {
                    graphicsLayer = this
                    assertEquals(IntSize.Zero, this.size)
                    record { drawRect(provider!!.color) }
                }
            },
            verify = {
                // Nulling out the dependency here should be safe despite attempting to obtain an
                // ImageBitmap afterwards
                provider = null
                graphicsLayer!!
                    .toImageBitmap()
                    .toPixelMap()
                    .verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            },
            verifySoftwareRender = false, // Only supported in hardware accelerated use cases
        )
    }

    @Test
    fun testDrawLayer() {
        var layer: GraphicsLayer? = null
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        assertEquals(IntSize.Zero, this.size)
                        record { drawRect(Color.Red) }
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(TEST_SIZE, layer!!.size)
                assertEquals(IntOffset.Zero, layer!!.topLeft)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            },
        )
    }

    @Test
    fun testDrawLayerAfterDiscard() {
        var layer: GraphicsLayer? = null
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        assertEquals(IntSize.Zero, this.size)
                        record { drawRect(Color.Red) }
                    }
                layer!!.discardDisplayList()
                layer!!.record { drawRect(Color.Red) }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(TEST_SIZE, layer!!.size)
                assertEquals(IntOffset.Zero, layer!!.topLeft)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            },
        )
    }

    @Test
    fun testDrawAfterDiscard() {
        var layer: GraphicsLayer? = null
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        assertEquals(IntSize.Zero, this.size)
                        record { drawRect(Color.Red) }
                        emulateTrimMemory()
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(TEST_SIZE, layer!!.size)
                assertEquals(IntOffset.Zero, layer!!.topLeft)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            },
        )
    }

    @Test
    fun testPersistenceDrawAfterHwuiDiscardsDisplaylists() {
        // Layer persistence calls should not fail even if the DisplayList is discarded beforehand
        // This differs from testDrawAfterDiscard as this invokes the internal discardDisplaylist
        // call in order to mirror the corresponding system call made to cull out displaylists
        // without updating GraphicsLayer internal state
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        assertEquals(IntSize.Zero, this.size)
                        record { drawRect(Color.Red) }
                        this.impl.discardDisplayList()
                    }
                drawIntoCanvas { layer.drawForPersistence(it) }
            },
            verify = { it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red) },
            verifySoftwareRender = false,
        )
    }

    @Test
    fun testRecordLayerWithSize() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(IntSize(TEST_WIDTH / 2, TEST_HEIGHT / 2)) { drawRect(Color.Red) }
                    }
                drawLayer(layer)
            },
            verify = { it.verifyQuadrants(Color.Red, Color.Black, Color.Black, Color.Black) },
        )
    }

    @Test
    fun testRecordLayerWithOffset() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset(TEST_WIDTH / 2, TEST_HEIGHT / 2)
        val size = IntSize(TEST_WIDTH, TEST_HEIGHT)
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
                        this.topLeft = topLeft
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Black, Color.Red)
            },
        )
    }

    @Test
    fun testSetOffset() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset(4, 4)
        val size = IntSize(TEST_WIDTH, TEST_HEIGHT)
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { inset(0f, 0f, -4f, -4f) { drawRect(Color.Red) } }
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
            },
        )
    }

    @Test
    fun testSetAlpha() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
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
                    compositedColor,
                )
            },
        )
    }

    @Test
    fun testSetScaleX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            drawRect(
                                Color.Red,
                                size = Size(this.size.width / 2, this.size.height / 2),
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
            },
        )
    }

    @Test
    fun testSetScaleY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            drawRect(
                                Color.Red,
                                size = Size(this.size.width / 2, this.size.height / 2),
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
            },
        )
    }

    @Test
    fun testDefaultPivot() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            inset(this.size.width / 4, this.size.height / 4) { drawRect(Color.Red) }
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
            },
        )
    }

    @Test
    fun testBottomRightPivot() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
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
            },
        )
    }

    @Test
    fun testResettingToDefaultPivot() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            inset(this.size.width / 4, this.size.height / 4) { drawRect(Color.Red) }
                        }
                        scaleY = 2f
                        scaleX = 2f
                        // first set to some custom value
                        pivotOffset = Offset(this.size.width.toFloat(), this.size.height.toFloat())
                        // and then get back to the default
                        pivotOffset = Offset.Unspecified
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red)
            },
        )
    }

    @Test
    fun testTranslationX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red, size = this.size / 2f) }
                        translationX = this.size.width / 2f
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Red, Color.Black, Color.Black)
            },
        )
    }

    @Test
    fun testRectOutlineWithNonZeroTopLeft() {
        graphicsLayerTest(
            block = { graphicsContext ->
                var layerSize = Size.Zero
                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            layerSize = this.size
                            drawRect(Color.Red, size = this.size / 2f)
                        }
                        topLeft = IntOffset(20, 30)
                        setRectOutline()
                    }
                drawLayer(layer)
                val outline = layer.outline
                assertEquals(Rect(0f, 0f, layerSize.width, layerSize.height), outline.bounds)
            }
        )
    }

    @Test
    fun testRoundRectOutlineWithNonZeroTopLeft() {
        graphicsLayerTest(
            block = { graphicsContext ->
                var layerSize = Size.Zero
                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            layerSize = this.size
                            drawRect(Color.Red, size = this.size / 2f)
                        }
                        topLeft = IntOffset(20, 30)
                        setRoundRectOutline()
                    }
                drawLayer(layer)
                val outline = layer.outline
                assertEquals(Rect(0f, 0f, layerSize.width, layerSize.height), outline.bounds)
            }
        )
    }

    @Test
    fun testRecordOverwritesPreviousRecord() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer =
                    graphicsContext.createGraphicsLayer().apply { record { drawRect(Color.Red) } }
                layer.record { drawRect(Color.Blue) }
                drawLayer(layer)
            },
            verify = { it.verifyQuadrants(Color.Blue, Color.Blue, Color.Blue, Color.Blue) },
        )
    }

    @Test
    fun testTranslationY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red, size = this.size / 2f) }
                        translationY = this.size.height / 2f
                    }
                drawLayer(layer!!)
            },
            verify = {
                assertEquals(topLeft, layer!!.topLeft)
                assertEquals(size, layer!!.size)
                it.verifyQuadrants(Color.Black, Color.Black, Color.Red, Color.Black)
            },
        )
    }

    @Test
    fun testRotationX() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            drawRect(Color.Red, size = Size(this.size.width, this.size.height / 2))
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
            },
        )
    }

    @Test
    fun testRotationY() {
        var layer: GraphicsLayer? = null
        val topLeft = IntOffset.Zero
        val size = TEST_SIZE
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
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
            },
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
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
                            drawRect(
                                Color.Red,
                                topLeft =
                                    Offset(
                                        this.size.width / 2f - rectSize / 2f,
                                        this.size.height / 2 - rectSize / 2f,
                                    ),
                                Size(rectSize.toFloat(), rectSize.toFloat()),
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
                    it[size.width / 2 - rectSize / 3, size.height / 2 - rectSize / 2 + 4],
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 - rectSize / 3, size.height / 2 + rectSize / 2 - 4],
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 + rectSize / 3, size.height / 2 - rectSize / 2 + 4],
                )
                assertEquals(
                    Color.Black,
                    it[size.width / 2 + rectSize / 3, size.height / 2 + rectSize / 2 - 4],
                )
            },
        )
    }

    @Test
    fun testUnboundedClip() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red, size = Size(100000f, 100000f)) }
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
            entireScene = true,
        )
    }

    @Test
    fun testBoundedClip() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red, size = Size(100000f, 100000f)) }
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
            entireScene = true,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testElevation() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize =
                    IntSize((this.size.width / 2f).toInt(), (this.size.height / 2f).toInt())

                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(halfSize) { drawRect(targetColor) }
                        shadowElevation = 10f
                    }
                drawRect(targetColor)

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height
                translate(this.size.width / 4, this.size.height / 4) { drawLayer(layer!!) }
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
            },
            usePixelCopy = true,
            verifySoftwareRender = false, // Elevation only supported with hardware acceleration
        )
    }

    @Test
    fun testShadowColors() {
        var layer: GraphicsLayer? = null
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize =
                    IntSize((this.size.width / 2f).toInt(), (this.size.height / 2f).toInt())

                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(halfSize) { drawRect(targetColor) }
                        shadowElevation = 10f
                        spotShadowColor = Color.Red
                        ambientShadowColor = Color.Blue
                    }
                drawRect(targetColor)

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height
                translate(this.size.width / 4, this.size.height / 4) { drawLayer(layer!!) }
            },
            verify = { pixmap ->
                var shadowPixelCount = 0
                var redCount = 0
                var blueCount = 0
                val ambientShadowColor: Color
                val spotShadowColor: Color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ambientShadowColor = Color.Blue
                    spotShadowColor = Color.Red
                } else {
                    ambientShadowColor = Color.Black
                    spotShadowColor = Color.Black
                }

                // Verify the correct shadow color behavior on the supported platform version
                assertEquals(layer!!.spotShadowColor, spotShadowColor)
                assertEquals(layer!!.ambientShadowColor, ambientShadowColor)

                // Shadow verification requires the PixelCopy API which is only introduced
                // in Android O.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    with(pixmap) {
                        for (x in left until right) {
                            for (y in top until bottom) {
                                if (this[x, y] != targetColor) {
                                    shadowPixelCount++
                                    if (this[x, y].red >= 0f) {
                                        redCount++
                                    }
                                    if (this[x, y].blue >= 0f) {
                                        blueCount++
                                    }
                                }
                            }
                        }
                    }
                    assertTrue(shadowPixelCount > 0)
                    // Colored shadows are only supported on Android P and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        assertTrue(redCount > 0)
                        assertTrue(blueCount > 0)
                    }
                }
            },
            usePixelCopy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
            verifySoftwareRender = false, // Elevation only supported with hardware acceleration
        )
    }

    // Test requires validation of elevation shadows which require readback operations from
    // the PixelCopy APIs
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testElevationPath() {
        var layer: GraphicsLayer?
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val targetColor = Color.White
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize =
                    IntSize((this.size.width / 2f).toInt(), (this.size.height / 2f).toInt())

                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(halfSize) { drawRect(targetColor) }
                        setPathOutline(
                            Path().apply {
                                addRect(
                                    Rect(
                                        0f,
                                        0f,
                                        halfSize.width.toFloat(),
                                        halfSize.height.toFloat(),
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
                translate(this.size.width / 4, this.size.height / 4) { drawLayer(layer!!) }
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
            },
            usePixelCopy = true,
            verifySoftwareRender = false, // Elevation only supported with hardware acceleration
        )
    }

    @Test
    fun testConcaveOutlineClipsOutsideBounds() {
        val bgColor = Color.White
        val targetColor = Color.Red
        var layerSize = IntSize.Zero
        graphicsLayerTest(
            block = { graphicsContext ->
                layerSize = IntSize(size.width.toInt(), size.height.toInt() / 2)
                val outlinePath =
                    Path().apply {
                        addRect(Rect(0f, 0f, layerSize.width.toFloat(), layerSize.height.toFloat()))

                        addRect(
                            Rect(
                                layerSize.width / 4f,
                                layerSize.height.toFloat(),
                                layerSize.width / 2f + layerSize.width / 4f,
                                layerSize.height + layerSize.height / 2f,
                            )
                        )
                    }

                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(size = layerSize) {
                            drawRect(
                                targetColor,
                                size = Size(layerSize.width.toFloat(), layerSize.height * 2f),
                            )
                        }
                        setPathOutline(outlinePath)
                        clip = true
                        shadowElevation = 20f
                    }
                drawRect(bgColor)
                drawLayer(layer)
            },
            verify = { pixmap ->
                val width = pixmap.width
                assertEquals(targetColor, pixmap[0, 0])
                assertEquals(targetColor, pixmap[width - 2, 0])
                assertEquals(targetColor, pixmap[0, layerSize.height - 2])
                assertEquals(targetColor, pixmap[width - 2, layerSize.height - 2])

                assertEquals(bgColor, pixmap[0, layerSize.height + 2])
                assertEquals(bgColor, pixmap[width - 2, layerSize.height + 2])
                assertEquals(bgColor, pixmap[width / 4 - 2, layerSize.height + 2])
                assertEquals(bgColor, pixmap[width / 2 + width / 4, layerSize.height + 2])

                assertEquals(targetColor, pixmap[width / 2, layerSize.height + 2])
                assertEquals(targetColor, pixmap[width / 2 + width / 4 - 2, layerSize.height + 2])
                assertEquals(targetColor, pixmap[width / 2, layerSize.height + 2])
            },
        )
    }

    @Test
    fun testRectOutlineClipsOutsideBounds() {
        val bgColor = Color.Black
        val targetColor = Color.Red
        var outlineSize = Size.Zero
        graphicsLayerTest(
            block = { graphicsContext ->
                val containerSize = size
                outlineSize = Size(containerSize.width, containerSize.height - 10f)
                val layerSize = IntSize(size.width.toInt(), size.height.toInt() / 2)

                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(size = layerSize) {
                            drawRect(
                                targetColor,
                                size = Size(layerSize.width.toFloat(), layerSize.height * 2f),
                            )
                        }
                        setRectOutline(Offset.Zero, outlineSize)
                        clip = true
                    }
                drawRect(bgColor)
                drawLayer(layer)
            },
            verify = { pixmap ->
                val width = pixmap.width
                val height = pixmap.height
                assertEquals(targetColor, pixmap[0, 0])
                assertEquals(targetColor, pixmap[width - 2, 0])
                assertEquals(targetColor, pixmap[0, (outlineSize.height - 2).toInt()])
                assertEquals(targetColor, pixmap[width - 2, (outlineSize.height - 2).toInt()])

                assertEquals(bgColor, pixmap[0, height - 2])
                assertEquals(bgColor, pixmap[width - 2, height - 2])
            },
        )
    }

    @Test
    fun testConcaveOutlineClip() {
        val bgColor = Color.White
        val targetColor = Color.Red
        graphicsLayerTest(
            block = { graphicsContext ->
                val rectPath =
                    Path().apply {
                        addRect(
                            Rect(
                                left = 0f,
                                top = size.height / 2,
                                right = size.width,
                                bottom = size.height,
                            )
                        )
                    }

                val tabPath =
                    Path().apply {
                        addRect(
                            Rect(
                                left = size.width / 4,
                                top = 0f,
                                right = size.width / 2 + size.width / 4,
                                bottom = size.height / 2,
                            )
                        )
                    }

                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(targetColor) }
                        setPathOutline(rectPath + tabPath)
                        clip = true
                    }
                drawRect(bgColor)
                drawLayer(layer)
            },
            verify = { pixmap ->
                val width = pixmap.width
                val height = pixmap.height
                assertEquals(bgColor, pixmap[0, 0])
                assertEquals(bgColor, pixmap[width / 4 - 2, 0])
                assertEquals(targetColor, pixmap[width / 4, 0])
                assertEquals(targetColor, pixmap[width / 2 + width / 4 - 1, 0])
                assertEquals(bgColor, pixmap[width / 2 + width / 4 + 2, 0])
                assertEquals(bgColor, pixmap[width - 1, 0])
                assertEquals(targetColor, pixmap[0, height - 1])
                assertEquals(targetColor, pixmap[width - 1, height - 1])
            },
        )
    }

    // Test requires validation of elevation shadows which require readback operations from
    // the PixelCopy APIs
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
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
                val halfSize =
                    IntSize((this.size.width / 2f).toInt(), (this.size.height / 2f).toInt())

                left = (this.size.width / 4f).toInt()
                top = (this.size.width / 4f).toInt()
                right = left + halfSize.width
                bottom = top + halfSize.height

                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(halfSize) { drawRect(targetColor) }
                        setRoundRectOutline(Offset.Zero, halfSize.toSize(), radius)
                        shadowElevation = 20f
                    }

                drawRect(targetColor)
                translate(left.toFloat(), top.toFloat()) { drawLayer(layer!!) }
            },
            verify = { pixmap ->
                fun PixelMap.hasShadowPixels(
                    targetColor: Color,
                    l: Int,
                    t: Int,
                    r: Int,
                    b: Int,
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
                    // Verify that pixels above top edge have some shadow
                    assertTrue(hasShadowPixels(targetColor, left, top - 4, right, top))
                    // Verify that pixels to the left of the left edge have some shadow
                    assertTrue(hasShadowPixels(targetColor, left - 4, top, left, bottom))
                    // Verify that pixels to the right of the right edge have some shadow
                    assertTrue(hasShadowPixels(targetColor, right, top, right + 4, bottom))
                    // Verify that pixels to the below the bottom edge have some shadow
                    assertTrue(hasShadowPixels(targetColor, left, bottom, right, bottom + 4))
                    // Verify that interior top left region does not have shadow pixels
                    assertFalse(
                        hasShadowPixels(
                            targetColor,
                            left,
                            top,
                            left + radius.toInt(),
                            top + radius.toInt(),
                        )
                    )
                    // Verify that interior top right region does not have shadow pixels
                    assertFalse(
                        hasShadowPixels(
                            targetColor,
                            right - radius.toInt(),
                            top,
                            right,
                            top + radius.toInt(),
                        )
                    )
                    // Verify that interior bottom left region does not have shadow pixels
                    assertFalse(
                        hasShadowPixels(
                            targetColor,
                            left,
                            bottom - radius.toInt(),
                            left + radius.toInt(),
                            bottom,
                        )
                    )
                    // Verify that interior bottom right region does not have shadow pixels
                    assertFalse(
                        hasShadowPixels(
                            targetColor,
                            right - radius.toInt(),
                            bottom - radius.toInt(),
                            right,
                            bottom,
                        )
                    )
                }
            },
            usePixelCopy = true,
            verifySoftwareRender = false, // Elevation only supported with hardware acceleration
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun testRenderEffect() {
        var layer: GraphicsLayer?
        val blurRadius = 10f
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
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
            entireScene = false,
            verifySoftwareRender = false, // RenderEffect only supported with hardware acceleration
        )
    }

    @Test
    fun testCompositingStrategyAuto() {
        var layer: GraphicsLayer?
        val bgColor = Color.Black
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
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
            },
        )
    }

    @Test
    fun testCompositingStrategyOffscreen() {
        var layer: GraphicsLayer?
        val bgColor = Color.LightGray
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
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
            },
        )
    }

    @Test
    fun testCompositingStrategyModulateAlpha() {
        var layer: GraphicsLayer?
        val bgColor = Color.Black
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record {
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
            },
            verifySoftwareRender = false, // ModulateAlpha only supported with hardware acceleration
        )
    }

    @Test
    fun testCameraDistanceWithRotationY() {
        var layer: GraphicsLayer?
        val bgColor = Color.Gray
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
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
            },
        )
    }

    @Test
    fun testTintColorFilter() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(Color.Red) }
                        colorFilter = tint(Color.Blue)
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
            },
        )
    }

    @Test
    fun testBlendMode() {
        var layer: GraphicsLayer?
        graphicsLayerTest(
            block = { graphicsContext ->
                val drawScopeSize = this.size
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        val topLeft =
                            IntOffset(
                                (drawScopeSize.width / 4).toInt(),
                                (drawScopeSize.height / 4).toInt(),
                            )
                        val layerSize =
                            IntSize(
                                (drawScopeSize.width / 2).toInt(),
                                (drawScopeSize.height / 2).toInt(),
                            )
                        record(layerSize) { drawRect(Color.Red) }
                        this.topLeft = topLeft
                        this.blendMode = BlendMode.Xor
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
            },
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
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(targetColor) }
                        setRectOutline(this.size.center.toOffset(), (this.size / 2).toSize())
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
                            val expected =
                                if (x in left until right && y in top until bottom) {
                                    targetColor
                                } else {
                                    bgColor
                                }
                            Assert.assertEquals(this[x, y], expected)
                        }
                    }
                }
            },
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
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(targetColor) }
                        setPathOutline(
                            Path().apply {
                                addRect(
                                    Rect(
                                        size.center.x.toFloat(),
                                        size.center.y.toFloat(),
                                        size.center.x + size.width.toFloat(),
                                        size.center.y + size.height.toFloat(),
                                    )
                                )
                            }
                        )
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
                            val expected =
                                if (x in left until right && y in top until bottom) {
                                    targetColor
                                } else {
                                    bgColor
                                }
                            Assert.assertEquals(this[x, y], expected)
                        }
                    }
                }
            },
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
                layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record { drawRect(targetColor) }
                        setRoundRectOutline(
                            this.size.center.toOffset(),
                            (this.size / 2).toSize(),
                            radius.toFloat(),
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
                            if (x in startX until endX && y in startY until endY) {
                                assertEquals(targetColor, this[x, y])
                            }
                        }
                    }
                    Assert.assertEquals(bgColor, this[offset, offset])
                    Assert.assertEquals(bgColor, this[width - offset, offset])
                    Assert.assertEquals(bgColor, this[offset, height - offset])
                    Assert.assertEquals(bgColor, this[width - offset, height - offset])
                }
            },
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
                // We wrap the path in a different Outline object from what we pass in, so compare
                // the paths instead of the outline instances
                assertEquals(generic.path, (layer.outline as Outline.Generic).path)
            }
        )
    }

    @Test
    fun testSwitchingFromClipToBoundsToClipToOutline() {
        val targetColor = Color.Red
        val inset = 50f
        graphicsLayerTest(
            block = { graphicsContext ->
                val fullSize = size
                val layerSize =
                    Size(
                            fullSize.width.roundToInt() - inset * 2,
                            fullSize.height.roundToInt() - inset * 2,
                        )
                        .toIntSize()

                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        record(size = layerSize) { inset(-inset) { drawRect(targetColor) } }
                        // as no outline is provided yet, this command will enable clipToBounds
                        clip = true
                        // then with providing an outline we should disable clipToBounds and start
                        // using clipToOutline instead
                        setRectOutline(Offset(-inset, -inset), fullSize)
                    }

                drawRect(Color.Black)
                inset(inset) { drawLayer(layer) }
            },
            verify = { pixmap ->
                with(pixmap) {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            assertEquals(this[x, y], targetColor)
                        }
                    }
                }
            },
        )
    }

    @Test
    fun testReleaseWithNoReferencesDiscardsDisplaylist() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer = graphicsContext.createGraphicsLayer()
                layer.record {
                    // Intentionally cause an exception to be thrown during recording
                    drawRect(Color.Red)
                }

                graphicsContext.releaseGraphicsLayer(layer)
                // View layers don't have a hasDisplayList method to verify and by default
                // returns true all the time. So if we have a RenderNode backed layer verify that
                // the displaylist is discarded after it has been released
                if (layer.impl !is GraphicsViewLayer) {
                    assertFalse(layer.impl.hasDisplayList)
                }
            },
            verify = { /* NO-OP */ },
        )
    }

    @Test
    fun testEndRecordingAlwaysCalled() {
        graphicsLayerTest(
            block = { graphicsContext ->
                val layer = graphicsContext.createGraphicsLayer()
                try {
                    layer.record {
                        // Intentionally cause an exception to be thrown during recording
                        throw Exception()
                    }
                } catch (_: Throwable) {
                    // NO-OP
                }

                // Attempts to record after an exception is thrown should still succeed
                layer.record { drawRect(Color.Red) }
                drawLayer(layer)
            },
            verify = { it.verifyQuadrants(Color.Red, Color.Red, Color.Red, Color.Red) },
        )
    }

    @Test
    fun testReleasingLayerDuringPersistenceLogicIsNotCrashing() {
        lateinit var layer1: GraphicsLayer
        lateinit var layer2: GraphicsLayer
        graphicsLayerTest(
            block = { context ->
                // creating new layers will also schedule a persistence pass in Handler
                layer1 = context.createGraphicsLayer()
                layer2 = context.createGraphicsLayer()
                layer2.record(Density(1f), Ltr, IntSize(10, 10)) { drawRect(Color.Red) }
                layer1.record(Density(1f), Ltr, IntSize(10, 10)) { drawLayer(layer2) }
                // we release layer2, but as it is drawn into layer1 its content is not discarded.
                context.releaseGraphicsLayer(layer2)
                // layer1 loses its content without us updating the dependency tracking
                layer1.emulateTrimMemory()
            },
            verify = {
                // just verifying there is no crash in layer persistence logic
                // there was an issue where the next persistence logic will re-draw layer1 content
                // and during this draw we fully release layer2. this was removing an item from
                // a set which is currently being iterated on.
            },
        )
    }

    @Test
    fun testChildLayerHasReferenceToParentLayer() {
        lateinit var layer1: GraphicsLayer
        lateinit var layer2: GraphicsLayer
        graphicsLayerTest(
            block = { context ->
                // creating new layers will also schedule a persistence pass in Handler
                layer1 = context.createGraphicsLayer()
                layer2 = context.createGraphicsLayer()
                layer2.record(Density(1f), Ltr, IntSize(10, 10)) {
                    assertEquals(layer2, drawContext.graphicsLayer)
                    drawRect(Color.Red)
                }
                layer1.record(Density(1f), Ltr, IntSize(10, 10)) {
                    assertEquals(layer1, drawContext.graphicsLayer)
                    drawLayer(layer2)
                }
                drawLayer(layer1)
            },
            verify = {
                // just verifying there is no crash
            },
        )
    }

    @Test
    fun testCanvasTransformStateRestore() {
        val bg = Color.White
        val layerColor1 = Color.Red
        val layerColor2 = Color.Green
        val layerColor3 = Color.Blue
        val layerColor4 = Color.Black
        var layerSize = IntSize.Zero
        graphicsLayerTest(
            block = { graphicsContext ->
                val layerWidth = size.width / 4
                val layerHeight = size.height / 4
                layerSize = IntSize(layerWidth.toInt(), layerHeight.toInt())
                val layer1 =
                    graphicsContext.createGraphicsLayer().apply {
                        record(size = layerSize) { drawRect(layerColor1) }
                    }
                val layer2 =
                    graphicsContext.createGraphicsLayer().apply {
                        topLeft = IntOffset(layerWidth.toInt(), layerHeight.toInt())
                        record(size = layerSize) { drawRect(layerColor2) }
                    }
                val layer3 =
                    graphicsContext.createGraphicsLayer().apply {
                        topLeft = IntOffset((layerWidth * 2).toInt(), (layerHeight * 2).toInt())
                        record(size = layerSize) { drawRect(layerColor3) }
                    }
                val layer4 =
                    graphicsContext.createGraphicsLayer().apply {
                        record(size = layerSize) { drawRect(layerColor4) }
                    }
                drawRect(bg)
                translate(layerWidth / 2, layerHeight / 2) {
                    translate(layerWidth / 2, layerHeight / 2) {
                        drawLayer(layer1)
                        translate(layerWidth / 2, layerHeight / 2) { drawLayer(layer2) }
                        drawLayer(layer3)
                    }
                }

                drawLayer(layer4)
            },
            verify = {
                val row1centerX = layerSize.width + layerSize.width / 2
                val row1centerY = layerSize.height + layerSize.height / 2

                val row2centerX = layerSize.width + row1centerX
                val row2centerY = layerSize.height + row1centerY

                val row3centerX = layerSize.width + row2centerX
                val row3centerY = layerSize.height + row2centerY

                val row4centerX = layerSize.width + row3centerX

                it.assertPixelColor(layerColor1, row1centerX, row1centerY)
                it.assertPixelColor(bg, row2centerX, row1centerY)

                it.assertPixelColor(bg, row1centerX, row2centerY)
                it.assertPixelColor(layerColor2, row2centerX, row2centerY)
                it.assertPixelColor(bg, row3centerX, row2centerY)

                it.assertPixelColor(bg, row2centerX, row3centerY)
                it.assertPixelColor(layerColor3, row3centerX, row3centerY)
                it.assertPixelColor(bg, row4centerX, row3centerY)

                it.assertPixelColor(layerColor4, layerSize.width / 2, layerSize.height / 2)
            },
        )
    }

    private fun PixelMap.verifyQuadrants(
        topLeft: Color,
        topRight: Color,
        bottomLeft: Color,
        bottomRight: Color,
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
        verify: (suspend (PixelMap) -> Unit)? = null,
        entireScene: Boolean = false,
        usePixelCopy: Boolean = false,
        verifySoftwareRender: Boolean = true,
    ) {
        var scenario: ActivityScenario<TestActivity>? = null
        var container: ViewGroup? = null
        try {
            var contentView: View? = null
            var rootGraphicsLayer: GraphicsLayer? = null
            var density = Density(1f)
            scenario =
                ActivityScenario.launch(TestActivity::class.java)
                    .moveToState(Lifecycle.State.CREATED)
                    .onActivity {
                        // See b/167533582 In the API 30 platform release, there was a StrictMode
                        // violation with SurfaceControl#readFromParcel that would cause the tests
                        // to
                        // crash on an issue not related to this test suite. So skip StrictMode
                        // tests
                        // for this platform version.
                        // See ag/283838 as Surface also violated StrictMode policies
                        val sdk = Build.VERSION.SDK_INT
                        val supportsStrictMode =
                            sdk != Build.VERSION_CODES.R && sdk >= Build.VERSION_CODES.M
                        if (supportsStrictMode) {
                            StrictMode.setVmPolicy(
                                StrictMode.VmPolicy.Builder()
                                    .detectLeakedClosableObjects()
                                    .penaltyLog()
                                    .penaltyDeath()
                                    .build()
                            )
                        }

                        container =
                            FrameLayout(it).apply {
                                setBackgroundColor(Color.White.toArgb())
                                clipToPadding = false
                                clipChildren = false
                            }
                        val graphicsContext = GraphicsContext(container!!)
                        rootGraphicsLayer = graphicsContext.createGraphicsLayer()
                        density = Density(it)
                        val content =
                            FrameLayout(it).apply {
                                setLayoutParams(FrameLayout.LayoutParams(TEST_WIDTH, TEST_HEIGHT))
                                setBackgroundColor(Color.Black.toArgb())
                                foreground = GraphicsContextHostDrawable(graphicsContext, block)
                            }
                        container!!.addView(content)
                        contentView = content
                        it.setContentView(container)
                    }
            val resumed = CountDownLatch(1)
            var testActivity: TestActivity? = null
            scenario.moveToState(Lifecycle.State.RESUMED).onActivity { activity ->
                testActivity = activity
                activity.runOnUiThread { resumed.countDown() }
            }
            assertTrue(resumed.await(3000, TimeUnit.MILLISECONDS))

            if (verify != null) {
                val target =
                    if (entireScene) {
                        container!!
                    } else {
                        contentView!!
                    }
                val pixelMap =
                    if (usePixelCopy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        target.captureToImage().toPixelMap()
                    } else {
                        val recordLatch = CountDownLatch(1)
                        testActivity!!.runOnUiThread {
                            rootGraphicsLayer!!.record(
                                density,
                                Ltr,
                                IntSize(target.width, target.height),
                            ) {
                                drawIntoCanvas { canvas -> target.draw(canvas.nativeCanvas) }
                            }
                            recordLatch.countDown()
                        }
                        assertTrue(recordLatch.await(3000, TimeUnit.MILLISECONDS))
                        val bitmap = runBlocking { rootGraphicsLayer!!.toImageBitmap() }
                        bitmap.toPixelMap()
                    }
                runBlocking { verify(pixelMap) }
                if (verifySoftwareRender) {
                    val softwareRenderLatch = CountDownLatch(1)
                    var softwareBitmap: Bitmap? = null
                    testActivity!!.runOnUiThread {
                        softwareBitmap = doSoftwareRender(target)
                        softwareRenderLatch.countDown()
                    }
                    assertTrue(softwareRenderLatch.await(300, TimeUnit.MILLISECONDS))
                    runBlocking { verify(softwareBitmap!!.asImageBitmap().toPixelMap()) }
                }
            }
        } finally {
            val detachLatch = CountDownLatch(1)
            scenario?.onActivity {
                it.runOnUiThread {
                    // Force removal of View first to ensure layers are destroyed on
                    // window detachment
                    (container!!.parent as ViewGroup).removeView(container!!)
                    detachLatch.countDown()
                }
            }
            assertTrue(detachLatch.await(3000, TimeUnit.MILLISECONDS))
            scenario?.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testSwitchingFromConvexPathToRect() {
        val bgColor = Color.Black
        val targetColor = Color.Red
        graphicsLayerTest(
            block = { graphicsContext ->
                val halfSize = size.toIntSize() / 2
                val center = size.toIntSize().center
                val layer =
                    graphicsContext.createGraphicsLayer().apply {
                        clip = true
                        setPathOutline(
                            // random not convex shape
                            Path().apply {
                                addRect(Rect(0f, 0f, 1f, 1f))
                                addRect(Rect(2f, 2f, 3f, 3f))
                            }
                        )
                        setRectOutline()
                        record(size = halfSize) { drawRect(targetColor) }
                        topLeft = center
                    }
                drawRect(bgColor)
                drawLayer(layer)
            },
            verify = { pixmap ->
                with(pixmap) {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val expected =
                                if (x < width / 2 || y < height / 2) {
                                    bgColor
                                } else {
                                    targetColor
                                }
                            assertEquals(this[x, y], expected)
                        }
                    }
                }
            },
        )
    }

    private fun doSoftwareRender(target: View): Bitmap {
        val bitmap = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        val softwareCanvas = Canvas(bitmap)
        target.draw(softwareCanvas)
        return bitmap
    }

    private class GraphicsContextHostDrawable(
        val graphicsContext: GraphicsContext,
        val block: DrawScope.(GraphicsContext) -> Unit,
    ) : Drawable() {

        var rootGraphicsLayer: GraphicsLayer? = null

        override fun draw(canvas: Canvas) {
            val bounds = getBounds()
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()
            var root = rootGraphicsLayer
            if (root == null) {
                root = graphicsContext.createGraphicsLayer()
                root.record(Density(1f, 1f), Ltr, IntSize(width.toInt(), height.toInt())) {
                    block(graphicsContext)
                }
                rootGraphicsLayer = root
            }
            root.draw(androidx.compose.ui.graphics.Canvas(canvas), null)
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
