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

package androidx.ui.core.test

import android.os.Build
import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Constraints
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureScope
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.offset
import androidx.ui.core.onPositioned
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.ltr
import androidx.ui.layout.padding
import androidx.ui.layout.rtl
import androidx.ui.layout.size
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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
class RtlLayoutTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<androidx.ui.framework.test.TestActivity>(
        androidx.ui.framework.test.TestActivity::class.java
    )
    private lateinit var activity: androidx.ui.framework.test.TestActivity
    internal lateinit var density: Density
    internal lateinit var countDownLatch: CountDownLatch
    internal lateinit var position: Array<Ref<PxPosition>>
    private val size = 100.ipx

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        position = Array(3) { Ref<PxPosition>() }
        countDownLatch = CountDownLatch(3)
    }

    @Test
    fun customLayout_absolutePositioning() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(true, LayoutDirection.Ltr)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(PxPosition(0.ipx, 0.ipx), position[0].value)
        Assert.assertEquals(PxPosition(size, size), position[1].value)
        Assert.assertEquals(
            PxPosition(size * 2, size * 2),
            position[2].value
        )
    }

    @Test
    fun customLayout_absolutePositioning_rtl() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(true, LayoutDirection.Rtl)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(PxPosition(0.ipx, 0.ipx), position[0].value)
        Assert.assertEquals(PxPosition(size, size), position[1].value)
        Assert.assertEquals(
            PxPosition(size * 2, size * 2),
            position[2].value
        )
    }

    @Test
    fun customLayout_positioning() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(false, LayoutDirection.Ltr)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(PxPosition(0.ipx, 0.ipx), position[0].value)
        Assert.assertEquals(PxPosition(size, size), position[1].value)
        Assert.assertEquals(
            PxPosition(size * 2, size * 2),
            position[2].value
        )
    }

    @Test
    fun customLayout_positioning_rtl() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(false, LayoutDirection.Rtl)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(PxPosition(size * 2, 0.ipx), position[0].value)
        Assert.assertEquals(PxPosition(size, size), position[1].value)
        Assert.assertEquals(PxPosition(0.ipx, size * 2), position[2].value)
    }

    @Test
    fun customLayout_updatingDirectionCausesRemeasure() {
        val direction = mutableStateOf(LayoutDirection.Rtl)
        var latch = CountDownLatch(1)
        var actualDirection: LayoutDirection? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                val children = @Composable {
                    Layout({}) { _, _, layoutDirection ->
                        actualDirection = layoutDirection
                        latch.countDown()
                        layout(100.ipx, 100.ipx) {}
                    }
                }
                Layout(children) { measurables, constraints, _ ->
                    layout(100.ipx, 100.ipx) {
                        measurables.first().measure(constraints, direction.value)
                            .place(0.ipx, 0.ipx)
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, actualDirection)

        latch = CountDownLatch(1)
        activityTestRule.runOnUiThread { direction.value = LayoutDirection.Ltr }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, actualDirection)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measurement_subsequentChanges() = with(density) {
        // The layout is a 100.dp white square, wrapped by 10.dp blue padding, wrapped by
        // 10.dp green padding, wrapped by 10.dp gray padding, wrapped by 10.dp magenta padding.
        // The test is asserting layout direction changes using modifiers and Layouts, and also
        // the propagation of layout direction across modifiers and layouts that are not changing
        // it. Padding is also added to the start, but the obtained padding is visually symmetrical
        // due to the layout direction changes.
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Stack(
                    Modifier
                        // White space padding.
                        .padding(10.dp)
                        // Magenta 10.dp padding.
                        .rtl
                        .background(Color.Magenta)
                        .padding(top = 10.dp, bottom = 10.dp)
                        .padding(start = 10.dp)
                        .ltr
                        .padding(start = 10.dp)
                ) {
                    Stack(Modifier.rtl) {
                        Stack(
                            Modifier
                                // Gray 10.dp padding.
                                .background(Color.Gray)
                                .padding(top = 10.dp, bottom = 10.dp)
                                .padding(start = 10.dp)
                                .ltr
                                .padding(start = 10.dp)
                        ) {
                            UpdateLayoutDirection(LayoutDirection.Rtl) {
                                // Green 10.dp padding.
                                Stack(
                                    Modifier
                                        .background(Color.Green)
                                        .padding(top = 10.dp, bottom = 10.dp)
                                        .padding(start = 10.dp)
                                        .ltr
                                        .padding(start = 10.dp)
                                        .rtl
                                ) {
                                    // Blue 10.dp padding.
                                    Stack(Modifier.background(Color.Blue)) {
                                        Padding(
                                            start = 10.dp,
                                            top = 10.dp,
                                            end = 0.dp,
                                            bottom = 10.dp
                                        ) {
                                            UpdateLayoutDirection(LayoutDirection.Ltr) {
                                                Padding(
                                                    start = 10.dp,
                                                    top = 0.dp,
                                                    end = 0.dp,
                                                    bottom = 0.dp
                                                ) {
                                                    Stack(Modifier
                                                        .background(Color.White)
                                                        .size(100.dp)) {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        activityTestRule.waitAndScreenShot().apply {
            val center = 200.dp.toIntPx().value / 2
            assertRect(
                Color.Magenta, 161.dp.toIntPx().value, 179.dp.toIntPx().value, center, center
            )
            assertRect(Color.Gray, 141.dp.toIntPx().value, 159.dp.toIntPx().value, center, center)
            assertRect(Color.Green, 121.dp.toIntPx().value, 139.dp.toIntPx().value, center, center)
            assertRect(Color.Blue, 101.dp.toIntPx().value, 119.dp.toIntPx().value, center, center)
        }
        Unit
    }

    @Test
    fun intrinsics_subsequentChanges() {
        val latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Stack(Modifier
                    .queryIntrinsics()
                    .rtl
                    .assertLayoutDirection(LayoutDirection.Rtl)
                    .ltr
                    .assertLayoutDirection(LayoutDirection.Ltr)
                    .rtl
                ) {
                    Layout(
                        children = {},
                        minIntrinsicWidthMeasureBlock = { _, _, layoutDirection ->
                            assertEquals(LayoutDirection.Rtl, layoutDirection)
                            0.ipx
                        },
                        minIntrinsicHeightMeasureBlock = { _, _, layoutDirection ->
                            assertEquals(LayoutDirection.Rtl, layoutDirection)
                            0.ipx
                        },
                        maxIntrinsicWidthMeasureBlock = { _, _, layoutDirection ->
                            assertEquals(LayoutDirection.Rtl, layoutDirection)
                            0.ipx
                        },
                        maxIntrinsicHeightMeasureBlock = { _, _, layoutDirection ->
                            assertEquals(LayoutDirection.Rtl, layoutDirection)
                            0.ipx
                        }
                    ) { _, _, layoutDirection ->
                        assertEquals(LayoutDirection.Rtl, layoutDirection)
                        latch.countDown()
                        layout(0.ipx, 0.ipx) {}
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun CustomLayout(
        absolutePositioning: Boolean,
        testLayoutDirection: LayoutDirection
    ) {
        val modifier = when (testLayoutDirection) {
            LayoutDirection.Ltr -> Modifier.ltr
            LayoutDirection.Rtl -> Modifier.rtl
        }
        Layout(
            children = @Composable {
                FixedSize(size, modifier = saveLayoutInfo(position[0], countDownLatch)) {
                }
                FixedSize(size, modifier = saveLayoutInfo(position[1], countDownLatch)) {
                }
                FixedSize(size, modifier = saveLayoutInfo(position[2], countDownLatch)) {
                }
            },
            modifier = modifier
        ) { measurables, constraints, _ ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.fold(0.ipx) { sum, p -> sum + p.width }
            val height = placeables.fold(0.ipx) { sum, p -> sum + p.height }
            layout(width, height) {
                var x = 0.ipx
                var y = 0.ipx
                for (placeable in placeables) {
                    if (absolutePositioning) {
                        placeable.placeAbsolute(PxPosition(x, y))
                    } else {
                        placeable.place(PxPosition(x, y))
                    }
                    x += placeable.width
                    y += placeable.height
                }
            }
        }
    }

    @Composable
    private fun UpdateLayoutDirection(ld: LayoutDirection, children: @Composable () -> Unit) {
        Layout(children) { measurables, constraints, _ ->
            val placeable = measurables[0].measure(constraints, ld)
            layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }

    @Composable
    private fun Padding(
        start: Dp,
        top: Dp,
        end: Dp,
        bottom: Dp,
        children: @Composable () -> Unit
    ) {
        Layout(children) { measurables, constraints, _ ->
            val childConstraints = constraints.offset(
                -start.toIntPx() - end.toIntPx(),
                -top.toIntPx() - bottom.toIntPx()
            )
            val placeable = measurables[0].measure(childConstraints)
            layout(
                placeable.width + start.toIntPx() + end.toIntPx(),
                placeable.height + top.toIntPx() + bottom.toIntPx()
            ) {
                placeable.place(start.toIntPx(), top.toIntPx())
            }
        }
    }

    private fun Modifier.queryIntrinsics() = this + object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            measurable.minIntrinsicWidth(0.ipx, layoutDirection)
            measurable.minIntrinsicHeight(0.ipx, layoutDirection)
            measurable.maxIntrinsicWidth(0.ipx, layoutDirection)
            measurable.maxIntrinsicHeight(0.ipx, layoutDirection)
            measurable.minIntrinsicWidth(0.ipx)
            measurable.minIntrinsicHeight(0.ipx)
            measurable.maxIntrinsicWidth(0.ipx)
            measurable.maxIntrinsicHeight(0.ipx)
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }

    @Composable
    private fun saveLayoutInfo(
        position: Ref<PxPosition>,
        countDownLatch: CountDownLatch
    ): Modifier = Modifier.onPositioned {
        position.value = it.localToGlobal(PxPosition(0.ipx, 0.ipx))
        countDownLatch.countDown()
    }
}

private fun Modifier.assertLayoutDirection(expectedLayoutDirection: LayoutDirection): Modifier =
    this + object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            assertEquals(expectedLayoutDirection, layoutDirection)
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurable: IntrinsicMeasurable,
            height: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx {
            assertEquals(expectedLayoutDirection, layoutDirection)
            measurable.minIntrinsicWidth(height)
            return measurable.minIntrinsicWidth(height, layoutDirection)
        }

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurable: IntrinsicMeasurable,
            width: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx {
            assertEquals(expectedLayoutDirection, layoutDirection)
            measurable.minIntrinsicHeight(width)
            return measurable.minIntrinsicHeight(width, layoutDirection)
        }

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurable: IntrinsicMeasurable,
            height: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx {
            assertEquals(expectedLayoutDirection, layoutDirection)
            measurable.maxIntrinsicWidth(height)
            return measurable.maxIntrinsicWidth(height, layoutDirection)
        }

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurable: IntrinsicMeasurable,
            width: IntPx,
            layoutDirection: LayoutDirection
        ): IntPx {
            assertEquals(expectedLayoutDirection, layoutDirection)
            measurable.maxIntrinsicHeight(width)
            return measurable.maxIntrinsicHeight(width, layoutDirection)
        }
    }
