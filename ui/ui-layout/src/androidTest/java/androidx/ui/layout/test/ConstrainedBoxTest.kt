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

package androidx.ui.layout.test

import androidx.test.filters.SmallTest
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.IntPx
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.AspectRatio
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ConstrainedBoxTest : LayoutTest() {
    @Test
    fun testConstrainedBox() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val constrainedBoxSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    constrainedBoxSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(constraints = DpConstraints.tightConstraints(sizeDp, sizeDp)) {
                        Container(expanded = true) {
                            SaveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), constrainedBoxSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testConstrainedBox_respectsMaxIncomingConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val constrainedBoxSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                Container(width = sizeDp, height = sizeDp) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        constrainedBoxSize.value = coordinates.size
                        positionedLatch.countDown()
                    }) {
                        ConstrainedBox(
                            constraints = DpConstraints.tightConstraints(sizeDp * 2, sizeDp * 2)
                        ) {
                            Container(expanded = true) {
                                SaveLayoutInfo(
                                    size = childSize,
                                    position = childPosition,
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), constrainedBoxSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testConstrainedBox_withNoChildren_sizesToChildMinConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(1)
        val constrainedBoxSize = Ref<PxSize>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    constrainedBoxSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    val constraints = DpConstraints(sizeDp, sizeDp * 2, sizeDp, sizeDp * 2)
                    ConstrainedBox(constraints = constraints) {
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), constrainedBoxSize.value)
    }

    @Test
    fun testConstrainedBox_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            ConstrainedBox(constraints = DpConstraints(10.dp, 20.dp, 30.dp, 40.dp)) {
                AspectRatio(1f) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }
}
