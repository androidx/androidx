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

package androidx.ui.foundation

import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findRoot
import androidx.ui.test.findByTag
import androidx.ui.test.setContentAndCollectSizes
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class CanvasTest {

    val contentTag = "CanvasTest"
    val boxWidth = 100
    val boxHeight = 100
    val containerSize = boxWidth

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCanvas() {
        val strokeWidth = 5.0f
        composeTestRule.setContent {
            val density = DensityAmbient.current.density
            val containerSize = (containerSize * 2 / density).dp
            val minWidth = (boxWidth / density).dp
            val minHeight = (boxHeight / density).dp
            Box(modifier = Modifier.preferredSize(containerSize)
                .drawBackground(Color.White)
                .wrapContentSize(Alignment.Center)) {
                    Canvas(modifier = Modifier.preferredSize(minWidth, minHeight)) {
                        drawLine(
                            p1 = Offset.zero,
                            p2 = Offset(size.width, size.height),
                            color = Color.Red,
                            stroke = Stroke(width = strokeWidth)
                        )
                    }
            }
        }

        val paintBoxColor = Color.Red.toArgb()
        val containerBgColor = Color.White.toArgb()
        val strokeOffset = (strokeWidth / 2).toInt() + 3
        findRoot().captureToBitmap().apply {
            val imageStartX = width / 2 - boxWidth / 2
            val imageStartY = height / 2 - boxHeight / 2

            // Top left
            Assert.assertEquals(paintBoxColor, getPixel(imageStartX, imageStartY))

            // Top Left, to the left of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX - strokeOffset, imageStartY))

            // Top Left, to the right of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX + strokeOffset, imageStartY))

            // Bottom right
            Assert.assertEquals(paintBoxColor, getPixel(imageStartX + boxWidth - 1,
                imageStartY + boxHeight - 1))

            // Bottom right to the right of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX + boxWidth + strokeOffset,
                    imageStartY + boxHeight))

            // Bottom right to the left of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX + boxWidth - strokeOffset,
                    imageStartY + boxHeight))

            // Middle
            Assert.assertEquals(paintBoxColor, getPixel(imageStartX + boxWidth / 2,
                imageStartY + boxHeight / 2))

            // Middle to the left of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX + boxWidth / 2 - strokeOffset,
                    imageStartY + boxHeight / 2))

            // Middle to the right of the line
            Assert.assertEquals(containerBgColor,
                getPixel(imageStartX + boxWidth / 2 + strokeOffset,
                    imageStartY + boxHeight / 2))
        }
    }

    @Test
    fun canvas_noSize_emptyCanvas() {
        composeTestRule.setContentAndCollectSizes {
            Canvas(modifier = Modifier) {
                drawRect(Color.Black)
            }
        }
            .assertHeightEqualsTo(0.dp)
            .assertWidthEqualsTo(0.dp)
    }

    @Test
    fun canvas_exactSizes() {
        var canvasSize: IntPxSize? = null
        val latch = CountDownLatch(1)
        composeTestRule.setContentAndCollectSizes {
            SemanticParent {
                Canvas(
                    Modifier.preferredSize(100.dp)
                        .onPositioned { position -> canvasSize = position.size }
                ) {
                    drawRect(Color.Red)

                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))

        with(composeTestRule.density) {
            Truth.assertThat(canvasSize!!.width.value).isEqualTo(100.dp.toIntPx().value)
            Truth.assertThat(canvasSize!!.height.value).isEqualTo(100.dp.toIntPx().value)
        }

        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shapeColor = Color.Red,
            shape = RectangleShape
        )
    }

    @Test
    fun canvas_exactSizes_drawCircle() {
        var canvasSize: IntPxSize? = null
        val latch = CountDownLatch(1)
        composeTestRule.setContentAndCollectSizes {
            SemanticParent {
                Canvas(
                    Modifier.preferredSize(100.dp)
                        .onPositioned { position -> canvasSize = position.size }
                ) {
                    drawRect(Color.Red)
                    drawCircle(
                        Color.Blue,
                        radius = 10.0f
                    )
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))

        with(composeTestRule.density) {
            Truth.assertThat(canvasSize!!.width.value).isEqualTo(100.dp.toIntPx().value)
            Truth.assertThat(canvasSize!!.height.value).isEqualTo(100.dp.toIntPx().value)
        }

        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shapeColor = Color.Blue,
            shape = CircleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Composable
    fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack(Modifier.testTag(contentTag)) {
            Box {
                DensityAmbient.current.children()
            }
        }
    }
}