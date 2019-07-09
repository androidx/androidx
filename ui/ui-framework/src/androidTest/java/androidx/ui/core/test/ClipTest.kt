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

package androidx.ui.core.test

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.setContent
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Clip
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.Draw
import androidx.ui.core.PxSize
import androidx.ui.core.ipx
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Shape
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@SmallTest
@RunWith(JUnit4::class)
class ClipTest {

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    private val rectShape = object : Shape {
        override fun createOutline(size: PxSize, density: Density): Outline =
            Outline.Rectangle(size.toRect())
    }
    private val triangleShape = object : Shape {
        override fun createOutline(size: PxSize, density: Density): Outline =
            Outline.Generic(
                Path().apply {
                    moveTo(size.width.value / 2f, 0f)
                    lineTo(size.width.value, size.height.value)
                    lineTo(0f, size.height.value)
                    close()
                }
            )
    }

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleRectClip() {
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Padding(size = 10.ipx) {
                        Clip(rectShape) {
                            AtLeastSize(size = 10.ipx) {
                                FillColor(Color.Cyan)
                            }
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 10)
            assertRect(Color.Green, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun roundedUniformRectClip() {
        val shape = object : Shape {
            override fun createOutline(size: PxSize, density: Density): Outline =
                    Outline.Rounded(RRect(size.toRect(), Radius.circular(12f)))
        }
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(shape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            // check corners
            assertColor(Color.Green, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
            // check inner rect
            assertRect(Color.Cyan, size = 18)
            // check centers of all sides
            assertColor(Color.Cyan, 0, 14)
            assertColor(Color.Cyan, 29, 14)
            assertColor(Color.Cyan, 14, 0)
            assertColor(Color.Cyan, 14, 29)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun roundedRectWithDiffCornersClip() {
        val shape = object : Shape {
            override fun createOutline(size: PxSize, density: Density): Outline =
                Outline.Rounded(
                    RRect(size.toRect(),
                        Radius.zero,
                        Radius.circular(12f),
                        Radius.circular(12f),
                        Radius.circular(12f))
                )
        }
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(shape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            // check corners
            assertColor(Color.Cyan, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
            // check inner rect
            assertRect(Color.Cyan, size = 18)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun triangleClip() {
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(triangleShape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromRectToRounded() {
        val model = ShapeModel(rectShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(model.shape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.shape = object : Shape {
                override fun createOutline(size: PxSize, density: Density): Outline =
                    Outline.Rounded(RRect(size.toRect(), Radius.circular(12f)))
            }
        }

        takeScreenShot(30).apply {
            assertColor(Color.Green, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromRectToPath() {
        val model = ShapeModel(rectShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(model.shape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.shape = triangleShape }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromPathToRect() {
        val model = ShapeModel(triangleShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.Green)
                    Clip(model.shape) {
                        AtLeastSize(size = 30.ipx) {
                            FillColor(Color.Cyan)
                        }
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.shape = rectShape }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }
    }

    @Composable
    private fun FillColor(color: Color) {
        Draw { canvas, parentSize ->
            canvas.drawRect(
                Rect(
                    -100f,
                    -100f,
                    parentSize.width.value + 100f,
                    parentSize.height.value + 100f
                ), Paint().apply {
                    this.color = color
                })
            drawLatch.countDown()
        }
    }

    private fun takeScreenShot(size: Int): Bitmap {
        Assert.assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        Assert.assertEquals(size, bitmap.width)
        Assert.assertEquals(size, bitmap.height)
        return bitmap
    }
}

@Model
private data class ShapeModel(var shape: Shape)

fun Bitmap.assertRect(color: Color, holeSize: Int = 0, size: Int = width) {
    Assert.assertEquals(width, height)
    Assert.assertTrue(width >= size)
    val center = width / 2
    val halfHoleSize = holeSize / 2
    val outerOffset = center - size / 2
    for (x in outerOffset until width - outerOffset) {
        for (y in outerOffset until width - outerOffset) {
            if (abs(x - center) > halfHoleSize &&
                abs(y - center) > halfHoleSize) {
                assertColor(color, x, y)
            }
        }
    }
}

fun Bitmap.assertTriangle(innerColor: Color, outerColor: Color) {
    Assert.assertEquals(width, height)
    val center = (width - 1) / 2
    val last = width - 1
    // check center
    assertColor(innerColor, center, center)
    // check top corners
    assertColor(outerColor, 4, 4)
    assertColor(outerColor, last - 4, 4)
    // check bottom corners
    assertColor(outerColor, 0, last - 4)
    assertColor(innerColor, 4, last - 4)
    assertColor(outerColor, last, last - 4)
    assertColor(innerColor, last - 4, last)
    // check top center
    assertColor(outerColor, center - 4, 0)
    assertColor(outerColor, center + 4, 0)
    assertColor(innerColor, center, 4)
}

fun Bitmap.assertColor(expectedColor: Color, x: Int, y: Int) {
    val pixel = getPixel(x, y)
    val pixelString = Color(pixel).toString()
    Assert.assertEquals(
        "Pixel [$x, $y] is expected to be $expectedColor, " +
                "but was $pixelString", expectedColor.toArgb(), pixel)
}