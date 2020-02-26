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
import androidx.compose.Providers
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutDirectionAmbient
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Ref
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import org.junit.Assert.assertEquals
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
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        Container(LayoutGravity.BottomEnd, width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(
                                size = alignedChildSize,
                                position = alignedChildPosition,
                                positionedLatch = positionedLatch
                            )
                        }

                        Container(LayoutGravity.Stretch + LayoutPadding(10.dp)) {
                            SaveLayoutInfo(
                                size = positionedChildSize,
                                position = positionedChildPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), alignedChildSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignedChildPosition.value)
        assertEquals(IntPxSize(30.dp.toIntPx(), 30.dp.toIntPx()), positionedChildSize.value)
        assertEquals(PxPosition(10.dp.toIntPx(), 10.dp.toIntPx()), positionedChildPosition.value)
    }

    @Test
    fun testStack_withMultipleAlignedChildren() = with(density) {
        val size = 250.ipx
        val sizeDp = size.toDp()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = (sizeDp * 2).toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntPxSize>()
        val childSize = arrayOf(Ref<IntPxSize>(), Ref<IntPxSize>())
        val childPosition = arrayOf(Ref<PxPosition>(), Ref<PxPosition>())
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        Container(
                            modifier = LayoutGravity.BottomEnd, width = sizeDp, height = sizeDp
                        ) {
                            SaveLayoutInfo(
                                size = childSize[0],
                                position = childPosition[0],
                                positionedLatch = positionedLatch
                            )
                        }
                        Container(
                            modifier = LayoutGravity.BottomEnd,
                            width = doubleSizeDp,
                            height = doubleSizeDp
                        ) {
                            SaveLayoutInfo(
                                size = childSize[1],
                                position = childPosition[1],
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(doubleSize, doubleSize), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(size, size), childPosition[0].value)
        assertEquals(IntPxSize(doubleSize, doubleSize), childSize[1].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[1].value)
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
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        Container(LayoutGravity.Center, width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(
                                size = childSize[0],
                                position = childPosition[0],
                                positionedLatch = positionedLatch
                            )
                        }
                        Container(
                            LayoutGravity.Stretch + LayoutPadding(
                                start = insetDp,
                                top = insetDp
                            ),
                            width = halfSizeDp,
                            height = halfSizeDp
                        ) {
                            SaveLayoutInfo(
                                size = childSize[1],
                                position = childPosition[1],
                                positionedLatch = positionedLatch
                            )
                        }
                        Container(
                            LayoutGravity.Stretch + LayoutPadding(
                                end = insetDp,
                                bottom = insetDp
                            ),
                            width = halfSizeDp,
                            height = halfSizeDp
                        ) {
                            SaveLayoutInfo(
                                size = childSize[2],
                                position = childPosition[2],
                                positionedLatch = positionedLatch
                            )
                        }
                        Container(
                            LayoutGravity.Stretch + LayoutPadding(
                                start = insetDp,
                                end = insetDp
                            ),
                            width = halfSizeDp,
                            height = halfSizeDp) {
                            SaveLayoutInfo(
                                size = childSize[3],
                                position = childPosition[3],
                                positionedLatch = positionedLatch
                            )
                        }
                        Container(
                            LayoutGravity.Stretch + LayoutPadding(
                                top = insetDp,
                                bottom = insetDp
                            ),
                            width = halfSizeDp,
                            height = halfSizeDp
                        ) {
                            SaveLayoutInfo(
                                size = childSize[4],
                                position = childPosition[4],
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[0].value)
        assertEquals(IntPxSize(size - inset, size - inset), childSize[1].value)
        assertEquals(PxPosition(inset, inset), childPosition[1].value)
        assertEquals(IntPxSize(size - inset, size - inset), childSize[2].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[2].value)
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
            Stack(LayoutAlign.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                        Stack(LayoutSize(tripleSizeDp, tripleSizeDp)) {
                            Stack(LayoutGravity.TopStart + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[0],
                                    position = childPosition[0],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.TopCenter + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[1],
                                    position = childPosition[1],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.TopEnd + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[2],
                                    position = childPosition[2],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.CenterStart + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[3],
                                    position = childPosition[3],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.Center + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[4],
                                    position = childPosition[4],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.CenterEnd + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[5],
                                    position = childPosition[5],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.BottomStart + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[6],
                                    position = childPosition[6],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.BottomCenter + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[7],
                                    position = childPosition[7],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Stack(LayoutGravity.BottomEnd + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize[8],
                                    position = childPosition[8],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(tripleSize, tripleSize), stackSize.value)
        assertEquals(PxPosition((size * 2).toPx(), 0.px), childPosition[0].value)
        assertEquals(PxPosition(size, 0.ipx), childPosition[1].value)
        assertEquals(PxPosition(0.ipx, 0.ipx), childPosition[2].value)
        assertEquals(PxPosition((size * 2).toPx(), size.toPx()), childPosition[3].value)
        assertEquals(PxPosition(size, size), childPosition[4].value)
        assertEquals(PxPosition(0.ipx, size), childPosition[5].value)
        assertEquals(PxPosition((size * 2).toPx(), (size * 2).toPx()), childPosition[6].value)
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
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container(LayoutSize(sizeDp, sizeDp)) {
                        Stack {
                            Container(LayoutSize.Fill) {
                                SaveLayoutInfo(
                                    size = childSize[0],
                                    position = childPosition[0],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Container(
                                LayoutGravity.BottomEnd,
                                width = halfSizeDp,
                                height = halfSizeDp
                            ) {
                                SaveLayoutInfo(
                                    size = childSize[1],
                                    position = childPosition[1],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), stackSize.value)
        assertEquals(IntPxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[0].value)
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
                Container(LayoutGravity.TopStart + LayoutAspectRatio(2f)) { }
                ConstrainedBox(
                    DpConstraints.fixed(testWidth, testHeight),
                    LayoutGravity.BottomCenter
                ) { }
                ConstrainedBox(
                    DpConstraints.fixed(200.dp, 200.dp),
                    LayoutGravity.Stretch + LayoutPadding(10.dp)
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
                    modifier = LayoutGravity.Stretch + LayoutPadding(10.dp),
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
