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

package androidx.ui.graphics.vector

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.core.setContent
import androidx.ui.core.test.AtLeastSize
import androidx.ui.core.test.runOnUiThreadIR
import androidx.ui.core.test.waitAndScreenShot
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.toArgb
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.toPx
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class VectorTest {

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTint() {
        rule.runOnUiThreadIR {
            activity.setContent {
                VectorTint()
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorAlignment() {
        rule.runOnUiThreadIR {
            activity.setContent {
                VectorTint(minimumSize = 500.ipx, alignment = Alignment.BottomEnd)
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(500).apply {
            assertEquals(getPixel(480, 480), Color.Cyan.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorInvalidation() {
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        val testCase = VectorInvalidationTestCase(latch1)
        rule.runOnUiThreadIR {
            activity.setContent {
                testCase.createTestVector()
            }
        }

        latch1.await(5, TimeUnit.SECONDS)
        val size = testCase.vectorSize
        takeScreenShot(size).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.White.toArgb(), getPixel(size - 5, 5))
        }

        testCase.latch = latch2
        rule.runOnUiThreadIR {
            testCase.toggle()
        }

        latch2.await(5, TimeUnit.SECONDS)
        takeScreenShot(size).apply {
            assertEquals(Color.White.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.Red.toArgb(), getPixel(size - 5, 5))
        }
    }

    @Composable
    private fun VectorTint(
        size: IntPx = 200.ipx,
        minimumSize: IntPx = size,
        alignment: Alignment = Alignment.Center
    ) {
        val sizePx = size.toPx()
        val sizeDp = (size.value / DensityAmbient.current.density).dp
        val background = Modifier.paint(
            VectorPainter(
                defaultWidth = sizeDp,
                defaultHeight = sizeDp) { _, _ ->
                Path(
                    pathData = PathData {
                        lineTo(sizePx.value, 0.0f)
                        lineTo(sizePx.value, sizePx.value)
                        lineTo(0.0f, sizePx.value)
                        close()
                    },
                    fill = SolidColor(Color.Black)
                )

                drawLatch.countDown()
            },
            colorFilter = ColorFilter.tint(Color.Cyan),
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }
}