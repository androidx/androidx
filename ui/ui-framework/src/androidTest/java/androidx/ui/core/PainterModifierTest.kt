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

package androidx.ui.core

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.test.AtLeastSize
import androidx.ui.core.test.background

import androidx.ui.core.test.runOnUiThreadIR
import androidx.ui.core.test.waitAndScreenShot
import androidx.ui.framework.test.TestActivity
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.Paint
import androidx.ui.graphics.compositeOver
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.toArgb
import androidx.ui.unit.Px
import androidx.ui.unit.ipx
import androidx.ui.unit.PxSize
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class PainterModifierTest {

    val containerWidth = 100.0f
    val containerHeight = 100.0f

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierColorFilter() {
        val paintLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                testPainter(
                    colorFilter = ColorFilter(Color.Cyan, BlendMode.srcIn),
                    latch = paintLatch
                )
            }
        }

        paintLatch.await()

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Cyan.toArgb(), getPixel(50, 50))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierAlpha() {
        val paintLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                testPainter(
                    alpha = 0.5f,
                    latch = paintLatch
                )
            }
        }

        paintLatch.await()

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            val expected = Color(
                alpha = 0.5f,
                red = Color.Red.red,
                green = Color.Red.green,
                blue = Color.Red.blue
            ).compositeOver(Color.White)

            val result = Color(getPixel(50, 50))
            assertEquals(expected.red, result.red, 0.01f)
            assertEquals(expected.green, result.green, 0.01f)
            assertEquals(expected.blue, result.blue, 0.01f)
            assertEquals(expected.alpha, result.alpha, 0.01f)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierRtl() {
        val paintLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                testPainter(
                    rtl = true,
                    latch = paintLatch
                )
            }
        }

        paintLatch.await()

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(50, 50))
        }
    }

    @Composable
    private fun testPainter(
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        rtl: Boolean = false,
        latch: CountDownLatch
    ) {
        with(DensityAmbient.current) {
            val p = object : Painter() {
                override val intrinsicSize: PxSize
                    get() = PxSize(
                        Px(containerWidth),
                        Px(containerHeight)
                    )

                override fun onDraw(canvas: Canvas, bounds: PxSize) {
                    val paint = Paint().apply {
                        this.color = if (rtl) Color.Blue else Color.Red
                    }
                    canvas.drawRect(
                        Rect.fromLTWH(0.0f, 0.0f, bounds.width.value, bounds.height.value),
                        paint
                    )
                    latch.countDown()
                }
            }

            AtLeastSize(
                modifier = background(Color.White) +
                        p.toModifier(alpha = alpha, colorFilter =
                colorFilter, rtl = rtl),
                size = containerWidth.roundToInt().ipx
            ) {
                // Intentionally empty
            }
        }
    }

    private fun obtainScreenshotBitmap(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }
}