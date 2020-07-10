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
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.core.test.AtLeastSize
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.toArgb
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findRoot
import androidx.ui.test.runOnUiThread
import androidx.ui.test.waitForIdle
import androidx.ui.unit.dp
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch

@SmallTest
@RunWith(JUnit4::class)
class VectorTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTint() {
        rule.setContent {
            VectorTint()
        }

        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorAlignment() {
        rule.setContent {
            VectorTint(minimumSize = 500, alignment = Alignment.BottomEnd)
        }

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
        rule.setContent {
            testCase.createTestVector()
        }

        val size = testCase.vectorSize
        takeScreenShot(size).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.White.toArgb(), getPixel(size - 5, 5))
        }

        testCase.latch = latch2
        runOnUiThread {
            testCase.toggle()
        }

        waitForIdle()

        takeScreenShot(size).apply {
            assertEquals(Color.White.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.Red.toArgb(), getPixel(size - 5, 5))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorClipPath() {
        rule.setContent {
            VectorClip()
        }

        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 50), Color.Cyan.toArgb())
            assertEquals(getPixel(100, 150), Color.Black.toArgb())
        }
    }

    @Test
    fun testVectorZeroSizeDoesNotCrash() {
        // Make sure that if we are given the size of zero we should not crash and instead
        // act as a no-op
        rule.setContent {
            Box(modifier = Modifier.preferredSize(0.dp).paint(createTestVectorPainter()))
        }
    }

    @Test
    fun testVectorZeroWidthDoesNotCrash() {
        rule.setContent {
            Box(modifier = Modifier.preferredWidth(0.dp).preferredHeight(100.dp).paint
                (createTestVectorPainter()))
        }
    }

    @Test
    fun testVectorZeroHeightDoesNotCrash() {
        rule.setContent {
            Box(modifier = Modifier.preferredWidth(50.dp).preferredHeight(0.dp).paint(
                createTestVectorPainter()
            ))
        }
    }

    @Composable
    private fun VectorTint(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center
    ) {

        val background = Modifier.paint(
            createTestVectorPainter(size),
            colorFilter = ColorFilter.tint(Color.Cyan),
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    @Composable
    private fun createTestVectorPainter(size: Int = 200): VectorPainter {
        val sizePx = size.toFloat()
        val sizeDp = (size / DensityAmbient.current.density).dp
        return VectorPainter(
            defaultWidth = sizeDp,
            defaultHeight = sizeDp) { _, _ ->
            Path(
                pathData = PathData {
                    lineTo(sizePx, 0.0f)
                    lineTo(sizePx, sizePx)
                    lineTo(0.0f, sizePx)
                    close()
                },
                fill = SolidColor(Color.Black)
            )
        }
    }

    @Composable
    private fun VectorClip(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / DensityAmbient.current.density).dp
        val background = Modifier.paint(
            VectorPainter(
                defaultWidth = sizeDp,
                defaultHeight = sizeDp
            ) { _, _ ->
                Path(
                    // Cyan background.
                    pathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx)
                        lineTo(0.0f, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Cyan)
                )
                Group(
                    // Only show the top half...
                    clipPathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx / 2)
                        lineTo(0.0f, sizePx / 2)
                        close()
                    },
                    // And rotate it, resulting in the bottom half being black.
                    pivotX = sizePx / 2,
                    pivotY = sizePx / 2,
                    rotation = 180f
                ) {
                    Path(
                        pathData = PathData {
                            lineTo(sizePx, 0.0f)
                            lineTo(sizePx, sizePx)
                            lineTo(0.0f, sizePx)
                            close()
                        },
                        fill = SolidColor(Color.Black)
                    )
                }
            },
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = findRoot().captureToBitmap()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }
}
