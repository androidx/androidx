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

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.onPositioned
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Stack
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.layout.rtl
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class StackTest : LayoutTest() {
    @Test
    fun testStack() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntPxSize>()
        val alignedChildSize = Ref<IntPxSize>()
        val alignedChildPosition = Ref<PxPosition>()
        val positionedChildSize = Ref<IntPxSize>()
        val positionedChildPosition = Ref<PxPosition>()
        show {
            Container(alignment = Alignment.TopStart) {
                Stack {
                    Container(
                        Modifier.gravity(Alignment.BottomEnd)
                            .saveLayoutInfo(alignedChildSize, alignedChildPosition, positionedLatch)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                stackSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }

                    Container(
                        Modifier.matchParentSize()
                            .padding(10.dp)
                            .saveLayoutInfo(
                                positionedChildSize,
                                positionedChildPosition,
                                positionedLatch
                            )
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), alignedChildSize.value)
        assertEquals(PxPosition(0f, 0f), alignedChildPosition.value)
        assertEquals(IntPxSize(30.dp.toIntPx(), 30.dp.toIntPx()), positionedChildSize.value)
        assertEquals(PxPosition(10.dp.toIntPx(), 10.dp.toIntPx()), positionedChildPosition.value)
    }

    @Test
    fun testStack_withMultipleAlignedChildren() = with(density) {
        val size = 200.ipx
        val sizeDp = size.toDp()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = (sizeDp * 2).toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntPxSize>()
        val childSize = arrayOf(Ref<IntPxSize>(), Ref<IntPxSize>())
        val childPosition = arrayOf(Ref<PxPosition>(), Ref<PxPosition>())
        show {
            Container(alignment = Alignment.TopStart) {
                Stack(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container(
                        modifier = Modifier.gravity(Alignment.BottomEnd)
                            .saveLayoutInfo(
                                childSize[0],
                                childPosition[0],
                                positionedLatch
                            ),
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }
                    Container(
                        modifier = Modifier.gravity(Alignment.BottomEnd)
                            .saveLayoutInfo(
                                size = childSize[1],
                                position = childPosition[1],
                                positionedLatch = positionedLatch
                            ),
                        width = doubleSizeDp,
                        height = doubleSizeDp
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(doubleSize, doubleSize), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(size, size), childPosition[0].value)
        assertEquals(IntPxSize(doubleSize, doubleSize), childSize[1].value)
        assertEquals(PxPosition(0f, 0f), childPosition[1].value)
    }

    @Test
    fun testStack_withStretchChildren() = with(density) {
        val size = 250.ipx
        val sizeDp = size.toDp()
        val halfSizeDp = sizeDp / 2
        val inset = 50.ipx
        val insetDp = inset.toDp()

        val positionedLatch = CountDownLatch(6)
        val stackSize = Ref<IntPxSize>()
        val childSize = Array(5) { Ref<IntPxSize>() }
        val childPosition = Array(5) { Ref<PxPosition>() }
        show {
            Container(alignment = Alignment.TopStart) {
                Stack(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container(
                        Modifier.gravity(Alignment.Center)
                            .saveLayoutInfo(
                                childSize[0],
                                childPosition[0],
                                positionedLatch
                            ),
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(start = insetDp, top = insetDp)
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(end = insetDp, bottom = insetDp)
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(start = insetDp, end = insetDp)
                            .saveLayoutInfo(childSize[3], childPosition[3], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(top = insetDp, bottom = insetDp)
                            .saveLayoutInfo(childSize[4], childPosition[4], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(0f, 0f), childPosition[0].value)
        assertEquals(IntPxSize(size - inset, size - inset), childSize[1].value)
        assertEquals(PxPosition(inset, inset), childPosition[1].value)
        assertEquals(IntPxSize(size - inset, size - inset), childSize[2].value)
        assertEquals(PxPosition(0f, 0f), childPosition[2].value)
        assertEquals(IntPxSize(size - inset * 2, size), childSize[3].value)
        assertEquals(PxPosition(inset, 0.ipx), childPosition[3].value)
        assertEquals(IntPxSize(size, size - inset * 2), childSize[4].value)
        assertEquals(PxPosition(0.ipx, inset), childPosition[4].value)
    }

    @Test
    fun testStack_Rtl() = with(density) {
        val sizeDp = 48.ipx.toDp()
        val size = sizeDp.toIntPx()
        val tripleSizeDp = sizeDp * 3
        val tripleSize = (sizeDp * 3).toIntPx()

        val positionedLatch = CountDownLatch(10)
        val stackSize = Ref<IntPxSize>()
        val childSize = Array(9) { Ref<IntPxSize>() }
        val childPosition = Array(9) { Ref<PxPosition>() }
        show {
            Stack(Modifier.wrapContentSize(Alignment.TopStart)) {
                Stack(
                    Modifier.rtl
                        .preferredSize(tripleSizeDp)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            stackSize.value = coordinates.size
                            positionedLatch.countDown()
                        }
                ) {
                    Stack(
                        Modifier.gravity(Alignment.TopStart)
                            .preferredSize(sizeDp, sizeDp)
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.TopCenter)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.TopEnd)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.CenterStart)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[3], childPosition[3], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.Center)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[4], childPosition[4], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.CenterEnd)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[5], childPosition[5], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.BottomStart)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[6], childPosition[6], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.BottomCenter)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[7], childPosition[7], positionedLatch)
                    ) {
                    }
                    Stack(
                        Modifier.gravity(Alignment.BottomEnd)
                            .preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[8], childPosition[8], positionedLatch)
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(tripleSize, tripleSize), stackSize.value)
        assertEquals(PxPosition((size * 2).value.toFloat(), 0f), childPosition[0].value)
        assertEquals(PxPosition(size, 0.ipx), childPosition[1].value)
        assertEquals(PxPosition(0.ipx, 0.ipx), childPosition[2].value)
        assertEquals(PxPosition((size * 2).toPx().value, size.toPx().value), childPosition[3].value)
        assertEquals(PxPosition(size, size), childPosition[4].value)
        assertEquals(PxPosition(0.ipx, size), childPosition[5].value)
        assertEquals(
            PxPosition(
                (size * 2).toPx().value,
                (size * 2).toPx().value
            ),
            childPosition[6].value
        )
        assertEquals(PxPosition(size, size * 2), childPosition[7].value)
        assertEquals(PxPosition(0.ipx, size * 2), childPosition[8].value)
    }

    @Test
    fun testStack_expanded() = with(density) {
        val size = 250.ipx
        val sizeDp = size.toDp()
        val halfSize = 125.ipx
        val halfSizeDp = halfSize.toDp()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntPxSize>()
        val childSize = Array(2) { Ref<IntPxSize>() }
        val childPosition = Array(2) { Ref<PxPosition>() }
        show {
            Container(alignment = Alignment.TopStart) {
                Container(
                    Modifier.preferredSize(
                        sizeDp,
                        sizeDp
                    ) + Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        Container(
                            Modifier.fillMaxSize()
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                        Container(
                            Modifier.gravity(Alignment.BottomEnd)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch),
                            width = halfSizeDp,
                            height = halfSizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(0f, 0f), childPosition[0].value)
        assertEquals(IntPxSize(halfSize, halfSize), childSize[1].value)
        assertEquals(PxPosition(size - halfSize, size - halfSize), childPosition[1].value)
    }

    @Test
    fun testStack_hasCorrectIntrinsicMeasurements() = with(density) {
        val testWidth = 90.ipx.toDp()
        val testHeight = 80.ipx.toDp()

        val testDimension = 200.ipx
        // When measuring the height with testDimension, width should be double
        val expectedWidth = testDimension * 2
        // When measuring the width with testDimension, height should be half
        val expectedHeight = testDimension / 2

        testIntrinsics(@Composable {
            Stack {
                Container(Modifier.gravity(Alignment.TopStart).aspectRatio(2f)) { }
                ConstrainedBox(
                    DpConstraints.fixed(testWidth, testHeight),
                    Modifier.gravity(Alignment.BottomCenter)
                ) { }
                ConstrainedBox(
                    DpConstraints.fixed(200.dp, 200.dp),
                    Modifier.matchParentSize().padding(10.dp)
                ) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(testWidth.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(expectedWidth, minIntrinsicWidth(testDimension))
            assertEquals(testWidth.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(testHeight.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(expectedHeight, minIntrinsicHeight(testDimension))
            assertEquals(testHeight.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(testWidth.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(expectedWidth, maxIntrinsicWidth(testDimension))
            assertEquals(testWidth.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(testHeight.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(expectedHeight, maxIntrinsicHeight(testDimension))
            assertEquals(testHeight.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testStack_hasCorrectIntrinsicMeasurements_withNoAlignedChildren() = with(density) {
        testIntrinsics(@Composable {
            Stack {
                ConstrainedBox(
                    modifier = Modifier.matchParentSize().padding(10.dp),
                    constraints = DpConstraints.fixed(200.dp, 200.dp)
                ) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }
}
