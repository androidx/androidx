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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Container
import androidx.ui.layout.Stack
import androidx.compose.Composable
import androidx.compose.composer
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
    fun testStack() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<PxSize>()
        val alignedChildSize = Ref<PxSize>()
        val alignedChildPosition = Ref<PxPosition>()
        val positionedChildSize = Ref<PxSize>()
        val positionedChildPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        aligned(Alignment.BottomRight) {
                            Container(width = sizeDp, height = sizeDp) {
                                SaveLayoutInfo(
                                    size = alignedChildSize,
                                    position = alignedChildPosition,
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(
                            leftInset = 10.dp,
                            topInset = 10.dp,
                            rightInset = 10.dp,
                            bottomInset = 10.dp
                        ) {
                            Container {
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
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), stackSize.value)
        assertEquals(PxSize(size, size), alignedChildSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignedChildPosition.value)
        assertEquals(PxSize(30.dp.toIntPx(), 30.dp.toIntPx()), positionedChildSize.value)
        assertEquals(PxPosition(10.dp.toIntPx(), 10.dp.toIntPx()), positionedChildPosition.value)
    }

    @Test
    fun testStack_withMultipleAlignedChildren() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = (sizeDp * 2).toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<PxSize>()
        val childSize = arrayOf(Ref<PxSize>(), Ref<PxSize>())
        val childPosition = arrayOf(Ref<PxPosition>(), Ref<PxPosition>())
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack {
                        aligned(Alignment.BottomRight) {
                            Container(width = sizeDp, height = sizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[0],
                                    position = childPosition[0],
                                    positionedLatch = positionedLatch
                                )
                            }
                            Container(width = doubleSizeDp, height = doubleSizeDp) {
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

        assertEquals(PxSize(doubleSize, doubleSize), stackSize.value)
        assertEquals(PxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(size + 1.ipx, size + 1.ipx), childPosition[0].value)
        assertEquals(PxSize(doubleSize, doubleSize), childSize[1].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[1].value)
    }

    @Test
    fun testStack_withPositionedChildren() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val halfSizeDp = sizeDp / 2
        val halfSize = (sizeDp / 2).toIntPx()
        val insetDp = 10.dp
        val inset = insetDp.toIntPx()

        val positionedLatch = CountDownLatch(8)
        val stackSize = Ref<PxSize>()
        val childSize = Array(7) { Ref<PxSize>() }
        val childPosition = Array(7) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    stackSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Stack(defaultAlignment = Alignment.BottomRight) {
                        aligned(Alignment.Center) {
                            Container(width = sizeDp, height = sizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[0],
                                    position = childPosition[0],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(leftInset = insetDp, topInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[1],
                                    position = childPosition[1],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(rightInset = insetDp, bottomInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[2],
                                    position = childPosition[2],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(leftInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[3],
                                    position = childPosition[3],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(topInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[4],
                                    position = childPosition[4],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(rightInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[5],
                                    position = childPosition[5],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                        positioned(bottomInset = insetDp) {
                            Container(width = halfSizeDp, height = halfSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize[6],
                                    position = childPosition[6],
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), stackSize.value)
        assertEquals(PxSize(size, size), childSize[0].value)
        assertEquals(PxPosition(0.px, 0.px), childPosition[0].value)
        assertEquals(PxSize(halfSize, halfSize), childSize[1].value)
        assertEquals(PxPosition(inset, inset), childPosition[1].value)
        assertEquals(PxSize(halfSize, halfSize), childSize[2].value)
        assertEquals(
            PxPosition(size - inset - halfSize, size - inset - halfSize),
            childPosition[2].value
        )
        assertEquals(PxSize(halfSize, halfSize), childSize[3].value)
        assertEquals(PxPosition(inset, halfSize), childPosition[3].value)
        assertEquals(PxSize(halfSize, halfSize), childSize[4].value)
        assertEquals(PxPosition(halfSize, inset), childPosition[4].value)
        assertEquals(PxSize(halfSize, halfSize), childSize[5].value)
        assertEquals(PxPosition(size - inset - halfSize, halfSize), childPosition[5].value)
        assertEquals(PxSize(halfSize, halfSize), childSize[6].value)
        assertEquals(PxPosition(halfSize, size - inset - halfSize), childPosition[6].value)
    }
}
