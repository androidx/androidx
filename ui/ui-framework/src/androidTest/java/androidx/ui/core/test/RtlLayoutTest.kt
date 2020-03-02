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

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.OnPositioned
import androidx.ui.core.Ref
import androidx.ui.core.setContent
import androidx.ui.unit.Density
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import org.junit.Assert
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

    @Composable
    private fun CustomLayout(
        absolutePositioning: Boolean,
        testLayoutDirection: LayoutDirection
    ) {
        val modifier = object : LayoutModifier {
            override fun Density.modifyLayoutDirection(layoutDirection: LayoutDirection) =
                testLayoutDirection
        }
        Layout(
            children = @Composable {
                FixedSize(size) {
                    SaveLayoutInfo(position[0], countDownLatch)
                }
                FixedSize(size) {
                    SaveLayoutInfo(position[1], countDownLatch)
                }
                FixedSize(size) {
                    SaveLayoutInfo(position[2], countDownLatch)
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
    private fun SaveLayoutInfo(position: Ref<PxPosition>, countDownLatch: CountDownLatch) {
        OnPositioned {
            position.value = it.localToGlobal(PxPosition(0.ipx, 0.ipx))
            countDownLatch.countDown()
        }
    }
}