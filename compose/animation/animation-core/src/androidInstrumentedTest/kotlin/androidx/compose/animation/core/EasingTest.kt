/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

const val BitmapSize = 128
const val Margin = 16

@SmallTest
@RunWith(AndroidJUnit4::class)
class EasingTest {
    @Test
    fun easing() {
        val paint =
            Paint().apply {
                style = Paint.Style.STROKE
                color = Color.RED
                isAntiAlias = false
            }

        // Testing methodology:
        // - Go through a list of Cubic curves with fixed start/end points at 0.0,0.0 and 1.0,1.0
        // - Create a Path and draw it using Canvas into a Bitmap
        // - Create a second Bitmap of the same size
        // - Create CubicBezierEasing curve and for each X coordinate in the second bitmap,
        //   evaluate the curve to compute the Y coordinate and draw a line
        // - Compare the two bitmaps to validate that our cubic Bézier evaluation matches that
        //   of Skia/Canvas
        for (cubic in
            listOf(
                floatArrayOf(0.40f, 0.00f, 0.20f, 1.0f), // FastOutSlowInEasing
                floatArrayOf(0.00f, 0.00f, 0.20f, 1.0f), // LinearOutSlowInEasing
                floatArrayOf(0.40f, 0.00f, 1.00f, 1.0f), // FastOutLinearInEasing
                floatArrayOf(0.34f, 1.56f, 0.64f, 1.0f), // EaseOutBack (overshoot)
                floatArrayOf(0.00f, 1.00f, 0.00f, 1.0f),
            )) {
            val path =
                Path().apply {
                    cubicTo(
                        cubic[0] * BitmapSize,
                        cubic[1] * BitmapSize,
                        cubic[2] * BitmapSize,
                        cubic[3] * BitmapSize,
                        BitmapSize.toFloat(),
                        BitmapSize.toFloat()
                    )
                }

            val reference =
                createBitmap(BitmapSize + 2 * Margin, BitmapSize + 2 * Margin).applyCanvas {
                    withTranslation(Margin.toFloat(), Margin.toFloat()) {
                        drawPath(path.asAndroidPath(), paint)
                    }
                }

            val easing = CubicBezierEasing(cubic[0], cubic[1], cubic[2], cubic[3])
            val subject =
                createBitmap(BitmapSize + 2 * Margin, BitmapSize + 2 * Margin).applyCanvas {
                    var x0 = 0.0f
                    var y0 = 0.0f
                    for (x in 1 until BitmapSize) {
                        val x1 = x / BitmapSize.toFloat()
                        val y1 = easing.transform(x1)
                        drawLine(
                            Margin.toFloat() + x0 * BitmapSize,
                            Margin.toFloat() + y0 * BitmapSize,
                            Margin.toFloat() + x1 * BitmapSize,
                            Margin.toFloat() + y1 * BitmapSize,
                            paint
                        )
                        x0 = x1
                        y0 = y1
                    }
                }

            // Allow for up to 32 pixels to be different in a 128x128 image, to
            // account for rasterization differences between the two techniques.
            // Visual inspection of the bitmap shows that differences come from
            // slightly different rounding between drawPath() and our cubic
            // evaluation.
            compareBitmaps(reference, subject, 32)
        }
    }
}

/**
 * Compares two bitmaps and fails the test if they are different. The two bitmaps are considered
 * different if more than [errorCount] pixels differ by more than [threshold] in any of the RGB
 * channels.
 */
fun compareBitmaps(bitmap1: Bitmap, bitmap2: Bitmap, errorCount: Int, threshold: Int = 1) {
    assertEquals(bitmap1.width, bitmap2.width)
    assertEquals(bitmap1.height, bitmap2.height)

    val p1 = IntArray(bitmap1.width * bitmap1.height)
    bitmap1.getPixels(p1, 0, bitmap1.width, 0, 0, bitmap1.width, bitmap1.height)

    val p2 = IntArray(bitmap2.width * bitmap2.height)
    bitmap2.getPixels(p2, 0, bitmap2.width, 0, 0, bitmap2.width, bitmap2.height)

    var count = 0
    for (y in 0 until bitmap1.height) {
        for (x in 0 until bitmap1.width) {
            val index = y * bitmap1.width + x

            val (r1, g1, b1, _) = p1[index]
            val (r2, g2, b2, _) = p2[index]

            if (abs(r1 - r2) > threshold || abs(g1 - g2) > threshold || abs(b1 - b2) > threshold) {
                count++
            }
        }
    }

    if (count > errorCount) {
        fail("More than $errorCount different pixels ($count) with error threshold=$threshold")
    }
}
