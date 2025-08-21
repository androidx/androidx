/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.vector

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalImageVectorCache
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class VectorTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTint() {
        rule.setContent { VectorTint() }

        takeScreenShot(200).apply { assertEquals(getPixel(100, 100), Color.Cyan.toArgb()) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorIntrinsicTint() {
        rule.setContent {
            val background =
                Modifier.paint(
                    createTestVectorPainter(200, Color.Magenta),
                    alignment = Alignment.Center,
                )
            AtLeastSize(size = 200, modifier = background) {}
        }
        takeScreenShot(200).apply { assertEquals(getPixel(100, 100), Color.Magenta.toArgb()) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorIntrinsicTintFirstFrame() {
        var vector: VectorPainter? = null
        rule.setContent {
            vector = createTestVectorPainter(200, Color.Magenta)

            val bitmap = remember {
                val bitmap = ImageBitmap(200, 200)
                val canvas = Canvas(bitmap)
                val bitmapSize = Size(200f, 200f)
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, bitmapSize) {
                    with(vector!!) { draw(bitmapSize) }
                }
                bitmap
            }

            val background = Modifier.paint(BitmapPainter(bitmap))

            AtLeastSize(size = 200, modifier = background) {}
        }
        takeScreenShot(200).apply { assertEquals(getPixel(100, 100), Color.Magenta.toArgb()) }
        assertEquals(ImageBitmapConfig.Alpha8, vector!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorAlignment() {
        rule.setContent { VectorTint(minimumSize = 450, alignment = Alignment.BottomEnd) }

        takeScreenShot(450).apply { assertEquals(getPixel(430, 430), Color.Cyan.toArgb()) }
    }

    @Test
    fun testVectorSkipsRecompositionOnNoChange() {
        val state = mutableIntStateOf(0)
        var composeCount = 0
        var vectorComposeCount = 0

        val composeVector: @Composable @VectorComposable (Float, Float) -> Unit =
            { viewportWidth, viewportHeight ->
                vectorComposeCount++
                Path(
                    fill = SolidColor(Color.Blue),
                    pathData =
                        PathData {
                            lineTo(viewportWidth, 0f)
                            lineTo(viewportWidth, viewportHeight)
                            lineTo(0f, viewportHeight)
                            close()
                        },
                )
            }

        rule.setContent {
            composeCount++
            // Arbitrary read to force composition here and verify the subcomposition below skips
            state.value
            val vectorPainter =
                rememberVectorPainter(
                    defaultWidth = 10.dp,
                    defaultHeight = 10.dp,
                    autoMirror = false,
                    content = composeVector,
                )
            Image(vectorPainter, null, modifier = Modifier.size(20.dp))
        }

        state.intValue = 1
        rule.waitForIdle()
        assertEquals(2, composeCount) // Arbitrary state read should compose twice
        assertEquals(1, vectorComposeCount) // Vector is identical so should compose once
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorInvalidation() {
        val testCase = VectorInvalidationTestCase()
        rule.setContent { testCase.TestVector() }

        rule.waitUntil { testCase.measured }
        val size = testCase.vectorSize
        takeScreenShot(size).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.White.toArgb(), getPixel(size - 5, 5))
        }

        testCase.measured = false
        rule.runOnUiThread { testCase.toggle() }

        rule.waitUntil { testCase.measured }

        takeScreenShot(size).apply {
            assertEquals(Color.White.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.Red.toArgb(), getPixel(size - 5, 5))
        }
    }

    @Test
    fun testVectorDisposal() {
        val composeVector = mutableStateOf(true)
        var initCount = 0
        var disposeCount = 0
        val disposeLatch = CountDownLatch(1)
        rule.setContent {
            if (composeVector.value) {
                rememberVectorPainter(
                    defaultWidth = 16.dp,
                    defaultHeight = 16.dp,
                    viewportWidth = 16f,
                    viewportHeight = 16f,
                    autoMirror = false,
                ) { _, _ ->
                    DisposableEffect(Unit) {
                        initCount++
                        onDispose {
                            disposeCount++
                            disposeLatch.countDown()
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        composeVector.value = false

        rule.waitForIdle()

        assertTrue(disposeLatch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(initCount, disposeCount)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorRendersOnceOnFirstFrame() {
        var drawCount = 0
        val testTag = "TestTag"
        rule.setContent {
            Box(
                modifier =
                    Modifier.wrapContentSize()
                        .drawBehind { drawCount++ }
                        .paint(painterResource(R.drawable.ic_triangle2))
                        .testTag(testTag)
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().toPixelMap().apply {
            assertEquals(1, drawCount)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorClipPath() {
        rule.setContent { VectorClip() }

        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 50), Color.Cyan.toArgb())
            assertEquals(getPixel(100, 150), Color.Black.toArgb())
        }
    }

    @Test
    fun testVectorZeroSizeDoesNotCrash() {
        // Make sure that if we are given the size of zero we should not crash and instead
        // act as a no-op
        rule.setContent { Box(modifier = Modifier.size(0.dp).paint(createTestVectorPainter())) }
    }

    @Test
    fun testVectorZeroWidthDoesNotCrash() {
        rule.setContent {
            Box(modifier = Modifier.width(0.dp).height(100.dp).paint(createTestVectorPainter()))
        }
    }

    @Test
    fun testVectorZeroHeightDoesNotCrash() {
        rule.setContent {
            Box(modifier = Modifier.width(50.dp).height(0.dp).paint(createTestVectorPainter()))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTrimPath() {
        rule.setContent { VectorTrim() }

        takeScreenShot(200).apply {
            fun colorToHexString(color: Int): String = String.format("#%08X", color)

            assertEquals("#FFFFFF00", colorToHexString(getPixel(25, 100)))
            assertEquals("#FF0000FF", colorToHexString(getPixel(100, 100)))
            assertEquals("#FFFFFF00", colorToHexString(getPixel(175, 100)))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testImageVectorChangeOnStateChange() {
        val defaultWidth = 48.dp
        val defaultHeight = 48.dp
        val viewportWidth = 24f
        val viewportHeight = 24f

        val icon1 =
            ImageVector.Builder(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                )
                .addPath(
                    fill = SolidColor(Color.Black),
                    pathData =
                        PathData {
                            lineTo(viewportWidth, 0f)
                            lineTo(viewportWidth, viewportHeight)
                            lineTo(0f, 0f)
                            close()
                        },
                )
                .build()

        val icon2 =
            ImageVector.Builder(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                )
                .addPath(
                    fill = SolidColor(Color.Black),
                    pathData =
                        PathData {
                            lineTo(0f, viewportHeight)
                            lineTo(viewportWidth, viewportHeight)
                            lineTo(0f, 0f)
                            close()
                        },
                )
                .build()

        val testTag = "iconClick"
        rule.setContent {
            val clickState = remember { mutableStateOf(false) }
            Image(
                imageVector = if (clickState.value) icon1 else icon2,
                contentDescription = null,
                modifier =
                    Modifier.testTag(testTag)
                        .size(icon1.defaultWidth, icon1.defaultHeight)
                        .background(Color.Red)
                        .clickable { clickState.value = !clickState.value },
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillHeight,
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToImage().asAndroidBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, 0))
                assertEquals(Color.Red.toArgb(), getPixel(2, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width - 1, height - 4))

                assertEquals(Color.Black.toArgb(), getPixel(0, 2))
                assertEquals(Color.Black.toArgb(), getPixel(0, height - 2))
                assertEquals(Color.Black.toArgb(), getPixel(width - 4, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Black.toArgb(), getPixel(width - 2, 0))
            assertEquals(Color.Black.toArgb(), getPixel(2, 0))
            assertEquals(Color.Black.toArgb(), getPixel(width - 1, height - 4))

            assertEquals(Color.Red.toArgb(), getPixel(0, 2))
            assertEquals(Color.Red.toArgb(), getPixel(0, height - 2))
            assertEquals(Color.Red.toArgb(), getPixel(width - 4, height - 2))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithoutColorFilterAfterPreviouslyConfigured() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null

        var tint: ColorFilter? by mutableStateOf(ColorFilter.tint(Color.Green))
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = SolidColor(Color.Blue),
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier = Modifier.testTag(testTag).background(Color.Red),
                contentScale = ContentScale.FillBounds,
                colorFilter = tint,
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Green }

        tint = null
        rule.waitForIdle()
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Alpha8, vectorPainter!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithColorFilterAfterNotPreviouslyConfigured() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null

        var tint: ColorFilter? by mutableStateOf(null)
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = SolidColor(Color.Blue),
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier = Modifier.testTag(testTag).background(Color.Red),
                contentScale = ContentScale.FillBounds,
                colorFilter = tint,
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }

        tint = ColorFilter.tint(Color.Green)
        rule.waitForIdle()
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Green }
        assertEquals(ImageBitmapConfig.Alpha8, vectorPainter!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWhenVectorHasOneColorWithAlpha() {
        val backgroundColor = Color.Red
        val brushColor = Color.Blue.copy(alpha = 0.5F)
        val compositeColor = brushColor.compositeOver(backgroundColor)
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = SolidColor(brushColor),
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter,
                contentDescription = null,
                modifier = Modifier.testTag(testTag).background(backgroundColor),
            )
        }

        val isBitmapConfigAlpha8 = vectorPainter?.bitmapConfig == ImageBitmapConfig.Alpha8
        assertTrue("Bitmap config was not Alpha8", isBitmapConfigAlpha8)

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { compositeColor }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicClearBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Clear)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSrcBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Src)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDstBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Dst)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSrcOverBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.SrcOver, expectedConfig = ImageBitmapConfig.Alpha8)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDstOverBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.DstOver)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSrcInBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.SrcIn, expectedConfig = ImageBitmapConfig.Alpha8)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDstInBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.DstIn)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSrcOutBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.SrcOut)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDstOutBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.DstOut)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSrcAtopBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.SrcAtop)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDstAtopBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.DstAtop)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicXorBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Xor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicPlusBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Plus)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicModulateBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Modulate)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicScreenBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Screen)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicOverlayBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Overlay)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDarkenBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Darken)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicLightenBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Lighten)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicColorDodgeBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.ColorDodge)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicColorBurnBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.ColorBurn)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicHardlightBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Hardlight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSoftLightBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Softlight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicDifferenceBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Difference)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicExclusionBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Exclusion)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicMultiplyBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Multiply)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicHueBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Hue)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicSaturationBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Saturation)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicColorBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Color)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithIntrinsicLuminosityBlendMode() {
        verifyAlphaMaskWithBlendModes(BlendMode.Luminosity)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawClearBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Clear))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSrcBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Src))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDstBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Dst))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSrcOverBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.SrcOver),
            expectedConfig = ImageBitmapConfig.Alpha8,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDstOverBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.DstOver)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSrcInBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.SrcIn),
            expectedConfig = ImageBitmapConfig.Alpha8,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDstInBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.DstIn))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSrcOutBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.SrcOut)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDstOutBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.DstOut)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSrcAtopBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.SrcAtop)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDstAtopBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.DstAtop)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawXorBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Xor))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawPlusBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Plus))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawModulateBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Modulate)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawScreenBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Screen)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawOverlayBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Overlay)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDarkenBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Darken)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawLightenBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Lighten)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawColorDodgeBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.ColorDodge)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawColorBurnBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.ColorBurn)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawHardlightBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Hardlight)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSoftLightBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Softlight)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawDifferenceBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Difference)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawExclusionBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Exclusion)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawMultiplyBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Multiply)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawHueBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Hue))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawSaturationBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Saturation)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawColorBlendMode() {
        verifyAlphaMaskWithBlendModes(colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Color))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAlphaMaskWithDrawLuminosityBlendMode() {
        verifyAlphaMaskWithBlendModes(
            colorFilter = ColorFilter.tint(Color.Yellow, BlendMode.Luminosity)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyAlphaMaskWithBlendModes(
        intrinsicBlendMode: BlendMode = BlendMode.SrcIn,
        colorFilter: ColorFilter? = null,
        expectedConfig: ImageBitmapConfig? = null,
    ) {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null

        // Create a gradient of the same color as a solid in order to verify behavior
        // of intrinsic color filter usage both with and without the optimization to tint
        // use a tinted alpha channel bitmap instead of a ARGB8888
        val solidBlueGradient = Brush.horizontalGradient(listOf(Color.Blue, Color.Blue))
        val solidBlueColor = SolidColor(Color.Blue)
        var targetBrush: Brush by mutableStateOf(solidBlueColor)
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    tintColor = Color.Cyan,
                    tintBlendMode = intrinsicBlendMode,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = targetBrush,
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier =
                    Modifier.testTag(testTag)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Yellow, Color.Transparent)
                            )
                        )
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
            )
        }

        rule.waitForIdle()

        val solidBrushImage = rule.onNodeWithTag(testTag).captureToImage()
        if (expectedConfig != null) {
            assertEquals(expectedConfig, vectorPainter!!.bitmapConfig)
        }

        targetBrush = solidBlueGradient
        rule.waitForIdle()

        val gradientBrushImage = rule.onNodeWithTag(testTag).captureToImage()

        assertArrayEquals(
            "Optimized vector does not match expected for $intrinsicBlendMode",
            gradientBrushImage.toPixelMap().buffer,
            solidBrushImage.toPixelMap().buffer,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPathColorChangeUpdatesBitmapConfig() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null
        var brush: Brush by mutableStateOf(SolidColor(Color.Blue))
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = brush,
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier =
                    Modifier.testTag(testTag)
                        .size(defaultWidth * 8, defaultHeight * 2)
                        .background(Color.Red),
                contentScale = ContentScale.FillBounds,
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Alpha8, vectorPainter!!.bitmapConfig)

        brush = Brush.horizontalGradient(listOf(Color.Blue, Color.Blue))
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Argb8888, vectorPainter!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testGroupPathColorChangeUpdatesBitmapConfig() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null
        var brush: Brush by mutableStateOf(SolidColor(Color.Blue))
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Group {
                        Path(
                            fill = brush,
                            pathData =
                                PathData {
                                    lineTo(viewportWidth, 0f)
                                    lineTo(viewportWidth, viewportHeight)
                                    lineTo(0f, viewportHeight)
                                    close()
                                },
                        )
                    }
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier =
                    Modifier.testTag(testTag)
                        .size(defaultWidth * 8, defaultHeight * 2)
                        .background(Color.Red),
                contentScale = ContentScale.FillBounds,
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Alpha8, vectorPainter!!.bitmapConfig)

        brush = Brush.horizontalGradient(listOf(Color.Blue, Color.Blue))
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Argb8888, vectorPainter!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorScaleNonUniformly() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        var vectorPainter: VectorPainter? = null
        rule.setContent {
            vectorPainter =
                rememberVectorPainter(
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    autoMirror = false,
                ) { viewportWidth, viewportHeight ->
                    Path(
                        fill = SolidColor(Color.Blue),
                        pathData =
                            PathData {
                                lineTo(viewportWidth, 0f)
                                lineTo(viewportWidth, viewportHeight)
                                lineTo(0f, viewportHeight)
                                close()
                            },
                    )
                }
            Image(
                painter = vectorPainter!!,
                contentDescription = null,
                modifier =
                    Modifier.testTag(testTag)
                        .size(defaultWidth * 8, defaultHeight * 2)
                        .background(Color.Red),
                contentScale = ContentScale.FillBounds,
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
        assertEquals(ImageBitmapConfig.Alpha8, vectorPainter!!.bitmapConfig)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorChangeSize() {
        val size = mutableStateOf(200)
        val color = mutableStateOf(Color.Magenta)

        rule.setContent {
            val background =
                Modifier.background(Color.Red)
                    .paint(
                        createTestVectorPainter(size.value, color.value),
                        alignment = Alignment.TopStart,
                    )
            AtLeastSize(size = 400, modifier = background) {}
        }

        takeScreenShot(400).apply {
            assertEquals(getPixel(100, 100), Color.Magenta.toArgb())
            assertEquals(getPixel(300, 300), Color.Red.toArgb())
        }

        size.value = 400
        color.value = Color.Cyan

        takeScreenShot(400).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
            assertEquals(getPixel(300, 300), Color.Cyan.toArgb())
        }

        size.value = 50
        color.value = Color.Yellow

        takeScreenShot(400).apply {
            assertEquals(getPixel(10, 10), Color.Yellow.toArgb())
            assertEquals(getPixel(100, 100), Color.Red.toArgb())
            assertEquals(getPixel(300, 300), Color.Red.toArgb())
        }
    }

    @Test
    fun testImageVectorCacheHit() {
        var vectorInCache = false
        rule.setContent {
            val theme = LocalContext.current.theme
            val imageVectorCache = LocalImageVectorCache.current
            imageVectorCache.clear()
            Image(painterResource(R.drawable.ic_triangle), contentDescription = null)

            vectorInCache =
                imageVectorCache[ImageVectorCache.Key(theme, R.drawable.ic_triangle)] != null
        }

        assertTrue(vectorInCache)
    }

    @Test
    fun testImageVectorCacheCleared() {
        var vectorInCache = false
        var application: Application? = null
        var theme: Resources.Theme? = null
        var vectorCache: ImageVectorCache? = null
        rule.setContent {
            application = LocalContext.current.applicationContext as Application
            theme = LocalContext.current.theme
            val imageVectorCache = LocalImageVectorCache.current
            imageVectorCache.clear()
            Image(painterResource(R.drawable.ic_triangle), contentDescription = null)

            vectorInCache =
                imageVectorCache[ImageVectorCache.Key(theme!!, R.drawable.ic_triangle)] != null

            vectorCache = imageVectorCache
        }

        application?.onTrimMemory(0)

        val cacheCleared =
            vectorCache?.let { it[ImageVectorCache.Key(theme!!, R.drawable.ic_triangle)] == null }
                ?: false

        assertTrue("Vector was not inserted in cache after initial creation", vectorInCache)
        assertTrue("Cache was not cleared after trim memory call", cacheCleared)
    }

    @Test
    fun testImageVectorCacheMissOnConfigChange() {
        val tag = "testTag"
        var vectorCache: ImageVectorCache? = null
        var vectorInCache = false
        var theme: Resources.Theme? = null
        try {
            rule.setContent {
                val imageVectorCache = LocalImageVectorCache.current
                theme = LocalContext.current.theme
                Image(
                    painter = painterResource(R.drawable.ic_triangle_config),
                    contentDescription = null,
                    modifier = Modifier.testTag(tag),
                )

                vectorInCache =
                    imageVectorCache[
                        ImageVectorCache.Key(theme!!, R.drawable.ic_triangle_config)] != null
                vectorCache = imageVectorCache
            }

            if (!rule.activity.rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
                Log.w(TAG, "device rotation unsuccessful")
                return
            }

            val cacheMiss =
                vectorCache?.let {
                    it[ImageVectorCache.Key(theme!!, R.drawable.ic_triangle_config)] == null
                } ?: false

            assertTrue("Vector was not inserted in cache after initial creation", vectorInCache)
            assertTrue("Vector object was not pruned on configuration change", cacheMiss)
        } catch (e: InterruptedException) {
            fail("Unable to verify the image vector cache on configuration (orientation) change")
        } finally {
            rule.activity.rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    private fun Activity.rotate(rotation: Int): Boolean {
        var rotationCount = 0
        var rotateSuccess = false
        var latch: CountDownLatch? = null
        val callbacks =
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(p0: Configuration) {
                    latch?.countDown()
                }

                @Deprecated("This callback is superseded by onTrimMemory")
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(p0: Int) {
                    // NO-OP
                }
            }
        application.registerComponentCallbacks(callbacks)
        try {
            while (rotationCount < 3 && !rotateSuccess) {
                latch = CountDownLatch(1)
                this.requestedOrientation = rotation
                rotateSuccess =
                    latch.await(3000, TimeUnit.MILLISECONDS) &&
                        this.requestedOrientation == rotation
                rotationCount++
            }
        } finally {
            application.unregisterComponentCallbacks(callbacks)
        }
        return rotateSuccess
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testImageVectorConfigChange() {
        if (!rule.activity.rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            Log.w(TAG, "device rotation unsuccessful")
            return
        }
        val tag = "testTag"
        try {
            rule.setContent {
                Image(
                    painterResource(R.drawable.ic_triangle_config),
                    contentDescription = null,
                    modifier = Modifier.testTag(tag),
                )
            }
            rule.onNodeWithTag(tag).captureToImage().apply {
                assertEquals(Color.Blue, toPixelMap()[width - 5, 5])
            }
        } catch (e: InterruptedException) {
            fail("Unable to verify vector asset in landscape orientation")
        } finally {
            rule.activity.rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorMirror() {
        val tag = "mirroredVector"
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Image(
                    painter = VectorMirror(20),
                    contentDescription = null,
                    modifier = Modifier.testTag(tag),
                )
            }
        }
        rule.onNodeWithTag(tag).captureToImage().toPixelMap().apply {
            assertEquals(Color.Blue, this[2, 2])
            assertEquals(Color.Blue, this[2, height - 3])
            assertEquals(Color.Blue, this[width / 2 - 3, 2])
            assertEquals(Color.Blue, this[width / 2 - 3, height - 3])

            assertEquals(Color.Red, this[width - 3, 2])
            assertEquals(Color.Red, this[width - 3, height - 3])
            assertEquals(Color.Red, this[width / 2 + 3, 2])
            assertEquals(Color.Red, this[width / 2 + 3, height - 3])
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorStrokeWidth() {
        val strokeWidth = mutableStateOf(100)
        rule.setContent { VectorStroke(strokeWidth = strokeWidth.value) }
        takeScreenShot(200).apply {
            assertEquals(Color.Yellow.toArgb(), getPixel(100, 25))
            assertEquals(Color.Blue.toArgb(), getPixel(100, 75))
        }
        rule.runOnUiThread { strokeWidth.value = 200 }
        rule.waitForIdle()
        takeScreenShot(200).apply {
            assertEquals(Color.Yellow.toArgb(), getPixel(100, 25))
            assertEquals(Color.Yellow.toArgb(), getPixel(100, 75))
        }
    }

    @Composable
    private fun VectorTint(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center,
    ) {
        val background =
            Modifier.paint(
                createTestVectorPainter(size),
                colorFilter = ColorFilter.tint(Color.Cyan),
                alignment = alignment,
            )
        AtLeastSize(size = minimumSize, modifier = background) {}
    }

    @Composable
    private fun createTestVectorPainter(
        size: Int = 200,
        tintColor: Color = Color.Unspecified,
    ): VectorPainter {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        return rememberVectorPainter(
            defaultWidth = sizeDp,
            defaultHeight = sizeDp,
            autoMirror = false,
            content = { _, _ ->
                Path(
                    pathData =
                        PathData {
                            lineTo(sizePx, 0.0f)
                            lineTo(sizePx, sizePx)
                            lineTo(0.0f, sizePx)
                            close()
                        },
                    fill = SolidColor(Color.Black),
                )
            },
            tintColor = tintColor,
        )
    }

    @Composable
    private fun VectorClip(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center,
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        val background =
            Modifier.paint(
                rememberVectorPainter(
                    defaultWidth = sizeDp,
                    defaultHeight = sizeDp,
                    autoMirror = false,
                ) { _, _ ->
                    Path(
                        // Cyan background.
                        pathData =
                            PathData {
                                lineTo(sizePx, 0.0f)
                                lineTo(sizePx, sizePx)
                                lineTo(0.0f, sizePx)
                                close()
                            },
                        fill = SolidColor(Color.Cyan),
                    )
                    Group(
                        // Only show the top half...
                        clipPathData =
                            PathData {
                                lineTo(sizePx, 0.0f)
                                lineTo(sizePx, sizePx / 2)
                                lineTo(0.0f, sizePx / 2)
                                close()
                            },
                        // And rotate it, resulting in the bottom half being black.
                        pivotX = sizePx / 2,
                        pivotY = sizePx / 2,
                        rotation = 180f,
                    ) {
                        Path(
                            pathData =
                                PathData {
                                    lineTo(sizePx, 0.0f)
                                    lineTo(sizePx, sizePx)
                                    lineTo(0.0f, sizePx)
                                    close()
                                },
                            fill = SolidColor(Color.Black),
                        )
                    }
                },
                alignment = alignment,
            )
        AtLeastSize(size = minimumSize, modifier = background) {}
    }

    @Composable
    private fun VectorTrim(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center,
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        val background =
            Modifier.paint(
                rememberVectorPainter(
                    defaultWidth = sizeDp,
                    defaultHeight = sizeDp,
                    autoMirror = false,
                ) { _, _ ->
                    Path(
                        pathData =
                            PathData {
                                lineTo(sizePx, 0.0f)
                                lineTo(sizePx, sizePx)
                                lineTo(0.0f, sizePx)
                                close()
                            },
                        fill = SolidColor(Color.Blue),
                    )
                    // A thick stroke
                    Path(
                        pathData =
                            PathData {
                                moveTo(0.0f, sizePx / 2)
                                lineTo(sizePx, sizePx / 2)
                            },
                        stroke = SolidColor(Color.Yellow),
                        strokeLineWidth = sizePx / 2,
                        trimPathStart = 0.25f,
                        trimPathEnd = 0.75f,
                        trimPathOffset = 0.5f,
                    )
                },
                alignment = alignment,
            )
        AtLeastSize(size = minimumSize, modifier = background) {}
    }

    @Composable
    private fun VectorStroke(
        size: Int = 200,
        strokeWidth: Int = 100,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center,
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        val strokeWidthPx = strokeWidth.toFloat()
        val background =
            Modifier.paint(
                rememberVectorPainter(
                    defaultWidth = sizeDp,
                    defaultHeight = sizeDp,
                    autoMirror = false,
                ) { _, _ ->
                    Path(
                        pathData =
                            PathData {
                                lineTo(sizePx, 0.0f)
                                lineTo(sizePx, sizePx)
                                lineTo(0.0f, sizePx)
                                close()
                            },
                        fill = SolidColor(Color.Blue),
                    )
                    // A thick stroke
                    Path(
                        pathData =
                            PathData {
                                moveTo(0.0f, 0.0f)
                                lineTo(sizePx, 0.0f)
                            },
                        stroke = SolidColor(Color.Yellow),
                        strokeLineWidth = strokeWidthPx,
                    )
                },
                alignment = alignment,
            )
        AtLeastSize(size = minimumSize, modifier = background) {}
    }

    @Composable
    private fun VectorMirror(size: Int): VectorPainter {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        return rememberVectorPainter(
            defaultWidth = sizeDp,
            defaultHeight = sizeDp,
            autoMirror = true,
        ) { _, _ ->
            Path(
                pathData =
                    PathData {
                        lineTo(sizePx / 2, 0f)
                        lineTo(sizePx / 2, sizePx)
                        lineTo(0f, sizePx)
                        close()
                    },
                fill = SolidColor(Color.Red),
            )

            Path(
                pathData =
                    PathData {
                        moveTo(sizePx / 2, 0f)
                        lineTo(sizePx, 0f)
                        lineTo(sizePx, sizePx)
                        lineTo(sizePx / 2, sizePx)
                        close()
                    },
                fill = SolidColor(Color.Blue),
            )
        }
    }

    // captureToImage() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }

    private val TAG = "VectorTest"
}
