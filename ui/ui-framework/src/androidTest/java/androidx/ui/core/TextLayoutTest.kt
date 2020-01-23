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

package androidx.ui.core

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.framework.test.R
import androidx.ui.framework.test.TestActivity
import androidx.ui.text.TextStyle
import androidx.ui.text.font.font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.asFontFamily
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.withDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class TextLayoutTest {
    @get:Rule
    internal val activityTestRule = ActivityTestRule(TestActivity::class.java)
    private val fontFamily = font(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
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
                Text("aa ", style = TextStyle(fontFamily = fontFamily))
            }
            OnChildPositioned({ coordinates ->
                doubleTextSize.value = coordinates.size
                layoutLatch.countDown()
            }) {
                Text("aa aa ", style = TextStyle(fontFamily = fontFamily))
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        val textWidth = textSize.value!!.width.round()
        val textHeight = textSize.value!!.height.round()
        val doubleTextWidth = doubleTextSize.value!!.width.round()

        val intrinsicsLatch = CountDownLatch(1)
        show {
            val text = @Composable {
                Text("aa aa ", style = TextStyle(fontFamily = fontFamily))
            }
            Layout(
                text,
                minIntrinsicWidthMeasureBlock = { _, _ -> 0.ipx },
                minIntrinsicHeightMeasureBlock = { _, _ -> 0.ipx },
                maxIntrinsicWidthMeasureBlock = { _, _ -> 0.ipx },
                maxIntrinsicHeightMeasureBlock = { _, _ -> 0.ipx }
            ) { measurables, _ ->
                val textMeasurable = measurables.first()
                // Min width.
                assertEquals(textWidth, textMeasurable.minIntrinsicWidth(0.ipx))
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

                layout(0.ipx, 0.ipx) {}
            }
        }
        assertTrue(intrinsicsLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testTextLayout_providesBaselines() = withDensity(density) {
        val layoutLatch = CountDownLatch(2)
        show {
            val text = @Composable {
                Text("aa", style = TextStyle(fontFamily = fontFamily))
            }
            Layout(text) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints())
                assertNotNull(placeable[FirstBaseline])
                assertNotNull(placeable[LastBaseline])
                assertEquals(placeable[FirstBaseline], placeable[LastBaseline])
                layoutLatch.countDown()
                layout(0.ipx, 0.ipx) {}
            }
            Layout(text) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints(maxWidth = 0.ipx))
                assertNotNull(placeable[FirstBaseline])
                assertNotNull(placeable[LastBaseline])
                assertTrue(placeable[FirstBaseline]!!.value < placeable[LastBaseline]!!.value)
                layoutLatch.countDown()
                layout(0.ipx, 0.ipx) {}
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    private fun show(composable: @Composable() () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent {
                    Layout(composable) { measurables, constraints ->
                        val placeables = measurables.map {
                            it.measure(constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx))
                        }
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
        activityTestRule.runOnUiThread(runnable)
    }
}
