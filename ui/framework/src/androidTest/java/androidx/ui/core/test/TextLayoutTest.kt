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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.ComplexLayout
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.Text
import androidx.ui.core.ipx
import androidx.ui.core.looseMin
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.framework.test.TestActivity
import androidx.ui.text.TextStyle
import androidx.ui.text.font.Font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.asFontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.UnsupportedOperationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class TextLayoutTest {
    @get:Rule
    internal val activityTestRule = ActivityTestRule(TestActivity::class.java)
    private val fontFamily = Font(
        name = "sample_font.ttf",
        weight = FontWeight.normal,
        style = FontStyle.Normal
    ).asFontFamily()
    private lateinit var activity: TestActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
    }

    @Test
    fun testTextLayout() = withDensity(density) {
        val layoutLatch = CountDownLatch(2)
        val textSize = Ref<PxSize>()
        val doubleTextSize = Ref<PxSize>()
        show {
            OnChildPositioned({ coordinates ->
                textSize.value = coordinates.size
                layoutLatch.countDown()
            }) {
                Text("aa", style = TextStyle(fontFamily = fontFamily))
            }
            OnChildPositioned({ coordinates ->
                doubleTextSize.value = coordinates.size
                layoutLatch.countDown()
            }) {
                Text("aaaa", style = TextStyle(fontFamily = fontFamily))
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertNotNull(textSize.value)
        assertNotNull(doubleTextSize.value)
        assertTrue(textSize.value!!.width > 0.px)
        assertTrue(textSize.value!!.height > 0.px)
        assertEquals(textSize.value!!.width * 2, doubleTextSize.value!!.width)
        assertEquals(textSize.value!!.height, doubleTextSize.value!!.height)
    }

    @Test
    fun testTextLayout_intrinsicMeasurements() = withDensity(density) {
        val layoutLatch = CountDownLatch(2)
        val textSize = Ref<PxSize>()
        val doubleTextSize = Ref<PxSize>()
        show {
            OnChildPositioned({ coordinates ->
                textSize.value = coordinates.size
                layoutLatch.countDown()
            }) {
                Text("aa", style = TextStyle(fontFamily = fontFamily))
            }
            OnChildPositioned({ coordinates ->
                doubleTextSize.value = coordinates.size
                layoutLatch.countDown()
            }) {
                Text("aaaa", style = TextStyle(fontFamily = fontFamily))
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        val textWidth = textSize.value!!.width.round()
        val textHeight = textSize.value!!.height.round()
        val doubleTextWidth = doubleTextSize.value!!.width.round()

        val intrinsicsLatch = CountDownLatch(1)
        show {
            val text = @Composable {
                Text("aaaa", style = TextStyle(fontFamily = fontFamily))
            }
            ComplexLayout(text) {
                layout { measurables, _ ->
                    val textMeasurable = measurables.first()

                    // Min width.
                    var threw = false
                    try {
                        textMeasurable.minIntrinsicWidth(0.ipx)
                    } catch(e: UnsupportedOperationException) {
                        threw = true
                    }
                    assertTrue(threw)
                    // Min height.
                    assertTrue(textMeasurable.minIntrinsicHeight(textWidth) > textHeight)
                    assertEquals(textHeight, textMeasurable.minIntrinsicHeight(doubleTextWidth))
                    assertEquals(textHeight, textMeasurable.minIntrinsicHeight(IntPx.Infinity))
                    // Max width.
                    assertEquals(doubleTextWidth, textMeasurable.maxIntrinsicWidth(0.ipx))
                    // Max height.
                    assertTrue(textMeasurable.maxIntrinsicHeight(textWidth) > textHeight)
                    assertEquals(textHeight, textMeasurable.maxIntrinsicHeight(doubleTextWidth))
                    assertEquals(textHeight, textMeasurable.maxIntrinsicHeight(IntPx.Infinity))

                    intrinsicsLatch.countDown()
                }
                minIntrinsicWidth { _, _ -> 0.ipx }
                minIntrinsicHeight { _, _ -> 0.ipx }
                maxIntrinsicWidth { _, _ -> 0.ipx }
                maxIntrinsicHeight { _, _ -> 0.ipx }
            }
        }
        assertTrue(intrinsicsLatch.await(1, TimeUnit.SECONDS))
    }

    private fun show(@Children composable: @Composable() () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent {
                    CraneWrapper {
                        Layout(composable) { measurables, constraints ->
                            val placeables = measurables.map { it.measure(constraints.looseMin()) }
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                var top = 0.px
                                placeables.forEach {
                                    it.place(0.px, top)
                                    top += it.height
                                }
                            }
                        }
                    }
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}