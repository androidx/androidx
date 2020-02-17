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
import androidx.compose.mutableStateOf
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.Opacity
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.toRect
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
                AtLeastSize(size = 10.ipx) {
                    FillColor(Color.White)
                    Opacity(opacity = 1f) {
                        FillColor(color)
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
                AtLeastSize(size = 10.ipx) {
                    FillColor(Color.White)
                    Opacity(opacity = 0f) {
                        FillColor(color)
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
                FillColor(Color.White)
                Row {
                    AtLeastSize(size = 10.ipx) {
                        Opacity(opacity = 0.5f) {
                            FillColor(color)
                        }
                    }
                    AtLeastSize(size = 10.ipx) {
                        FillColor(color.copy(alpha = 0.5f))
                    }
                }
            }
        }

        takeScreenShot(20, 10).apply {
            assertColorsEqual(color(5, 5), color(15, 5))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromHalfOpacityToFull() {
        val color = Color.Green
        val model = ValueModel(0.5f)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 10.ipx) {
                    FillColor(Color.White)
                    Opacity(opacity = model.value) {
                        FillColor(color)
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = 1f
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromZeroOpacityToFullWithNestedRepaintBoundaries() {
        val color = Color.Green
        var opacity by mutableStateOf(0f)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 10.ipx) {
                    FillColor(Color.White)
                    Opacity(1f) {
                        Opacity(opacity = opacity) {
                            FillColor(color)
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            opacity = 1f
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitDrawWithOpacityLater() {
        val model = ValueModel(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 10.ipx) {
                    FillColor(Color.White)
                    if (model.value) {
                        Opacity(opacity = 0f) {
                            FillColor(Color.Green)
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = true
        }

        takeScreenShot(10).apply {
            assertRect(Color.White)
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
