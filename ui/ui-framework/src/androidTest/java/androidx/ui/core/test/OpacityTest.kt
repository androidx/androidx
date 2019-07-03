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
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.Opacity
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.ui.core.toRect
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
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
class OpacityTest {

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
    fun drawFullOpacity() {
        val color = Color.LightGray
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.White)
                    Opacity(opacity = 1f) {
                        AtLeastSize(size = 10.ipx) {
                            FillColor(color)
                        }
                    }
                }
            }
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawZeroOpacity() {
        val color = Color.LightGray
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.White)
                    Opacity(opacity = 0f) {
                        AtLeastSize(size = 10.ipx) {
                            FillColor(color)
                        }
                    }
                }
            }
        }

        takeScreenShot(10).apply {
            assertRect(Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawHalfOpacity() {
        val color = Color.Red
        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.White)
                    Row {
                        Opacity(opacity = 0.5f) {
                            AtLeastSize(size = 10.ipx) {
                                FillColor(color)
                            }
                        }
                        AtLeastSize(size = 10.ipx) {
                            FillColor(color.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        takeScreenShot(20, 10).apply {
            assertSimilar(color(5, 5), color(15, 5))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromHalfOpacityToFull() {
        val color = Color.Lime
        val model = OpacityModel(0.5f)

        rule.runOnUiThreadIR {
            activity.setContent {
                CraneWrapper {
                    FillColor(Color.White)
                    Opacity(opacity = model.opacity) {
                        AtLeastSize(size = 10.ipx) {
                            FillColor(color)
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.opacity = 1f
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @Composable
    private fun FillColor(color: Color) {
        Draw { canvas, parentSize ->
            canvas.drawRect(parentSize.toRect(), Paint().apply {
                    this.color = color
                })
            drawLatch.countDown()
        }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }
}

@Composable
fun Row(children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        var width = 0.ipx
        var height = 0.ipx
        placeables.forEach {
            width += it.width
            height = max(height, it.height)
        }
        layout(width, height) {
            var offset = 0.ipx
            placeables.forEach {
                it.place(offset, 0.ipx)
                offset += it.width
            }
        }
    }
}

fun Bitmap.color(x: Int, y: Int): Color = Color(getPixel(x, y))

fun assertSimilar(color1: Color, color2: Color) {
    val errorString = "$color1 and $color2 are not similar!"
    assertEquals(errorString, color1.red, color2.red, 0.01f)
    assertEquals(errorString, color1.green, color2.green, 0.01f)
    assertEquals(errorString, color1.blue, color2.blue, 0.01f)
    assertEquals(errorString, color1.alpha, color2.alpha, 0.01f)
}

@Model
private data class OpacityModel(var opacity: Float)
