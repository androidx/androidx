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
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.enforce
import androidx.ui.core.onPositioned
import androidx.ui.layout.Stack
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.rtl
import androidx.ui.layout.wrapContentHeight
import androidx.ui.layout.wrapContentSize
import androidx.ui.layout.wrapContentWidth
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class LayoutAlignTest : LayoutTest() {
    @Test
    fun test2DAlignedModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntPxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Container(Modifier.saveLayoutInfo(alignSize, alignPosition, positionedLatch)) {
                Container(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.BottomEnd)
                        .preferredSize(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, root.height.ipx), alignSize.value)
        assertEquals(PxPosition(0f, 0f), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(
            PxPosition((root.width.px - size).value, (root.height.px - size).value),
            childPosition.value
        )
    }

    @Test
    fun test1DAlignedModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntPxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Container(
                Modifier.saveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
            ) {
                Container(
                    Modifier.fillMaxSize()
                        .wrapContentWidth(Alignment.End)
                        .preferredWidth(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, root.height.ipx), alignSize.value)
        assertEquals(PxPosition(0f, 0f), alignPosition.value)
        assertEquals(IntPxSize(size, root.height.ipx), childSize.value)
        assertEquals(PxPosition((root.width.px - size).value, 0f), childPosition.value)
    }

    @Test
    fun testAlignedModifier_rtl() = with(density) {
        val sizeDp = 200.ipx.toDp()
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val childSize = Array(3) { Ref<IntPxSize>() }
        val childPosition = Array(3) { Ref<PxPosition>() }
        show {
            Stack(Modifier.rtl) {
                Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                    Stack(
                        Modifier.preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    ) {
                    }
                }
                Stack(Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)) {
                    Stack(
                        Modifier.preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    ) {
                    }
                }
                Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                    Stack(
                        Modifier.preferredSize(sizeDp)
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            PxPosition(root.width.ipx - size, 0.ipx),
            childPosition[0].value
        )
        assertEquals(
            PxPosition(root.width.ipx - size, (root.height.ipx - size) / 2),
            childPosition[1].value
        )
        assertEquals(
            PxPosition(0.ipx, root.height.ipx - size),
            childPosition[2].value
        )
    }

    @Test
    fun testModifier_wrapsContent() = with(density) {
        val contentSize = 50.dp
        val size = Ref<IntPxSize>()
        val latch = CountDownLatch(1)
        show {
            Container {
                Container(Modifier.saveLayoutInfo(size, Ref(), latch)) {
                    Container(
                        Modifier.wrapContentSize(Alignment.TopStart)
                            .preferredSize(contentSize)
                    ) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntPxSize(contentSize.toIntPx(), contentSize.toIntPx()), size.value)
    }

    @Test
    fun testAlignedModifier_wrapsContent_whenMeasuredWithInfiniteConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntPxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Layout(
                children = {
                    Container(
                        Modifier.saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.wrapContentSize(Alignment.BottomEnd)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                },
                measureBlock = { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), alignSize.value)
        assertEquals(PxPosition(0f, 0f), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(0f, 0f), childPosition.value)
    }

    @Test
    fun testLayoutAlignModifier_respectsMinConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Container(Modifier.wrapContentSize(Alignment.TopStart)) {
                Layout(
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        wrapSize.value = coordinates.size
                        positionedLatch.countDown()
                    },
                    children = {
                        Container(
                            Modifier.wrapContentSize(Alignment.Center)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    },
                    measureBlock = { measurables, incomingConstraints, _ ->
                        val measurable = measurables.first()
                        val constraints = Constraints(
                            minWidth = doubleSizeDp.toIntPx(),
                            minHeight = doubleSizeDp.toIntPx()
                        ).enforce(incomingConstraints)
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(PxPosition.Origin)
                        }
                    }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(
            PxPosition((doubleSize - size) / 2, (doubleSize - size) / 2),
            childPosition.value
        )
    }

    // TODO(popam): this should be unit test instead
    @Test
    fun testAlignmentCoordinates_evenSize() {
        val size = IntPxSize(2.ipx, 2.ipx)
        assertEquals(IntPxPosition(0.ipx, 0.ipx), Alignment.TopStart.align(size))
        assertEquals(IntPxPosition(1.ipx, 0.ipx), Alignment.TopCenter.align(size))
        assertEquals(IntPxPosition(2.ipx, 0.ipx), Alignment.TopEnd.align(size))
        assertEquals(IntPxPosition(0.ipx, 1.ipx), Alignment.CenterStart.align(size))
        assertEquals(IntPxPosition(1.ipx, 1.ipx), Alignment.Center.align(size))
        assertEquals(IntPxPosition(2.ipx, 1.ipx), Alignment.CenterEnd.align(size))
        assertEquals(IntPxPosition(0.ipx, 2.ipx), Alignment.BottomStart.align(size))
        assertEquals(IntPxPosition(1.ipx, 2.ipx), Alignment.BottomCenter.align(size))
        assertEquals(IntPxPosition(2.ipx, 2.ipx), Alignment.BottomEnd.align(size))
    }

    // TODO(popam): this should be unit test instead
    @Test
    fun testAlignmentCoordinates_oddSize() {
        val size = IntPxSize(3.ipx, 3.ipx)
        assertEquals(IntPxPosition(0.ipx, 0.ipx), Alignment.TopStart.align(size))
        assertEquals(IntPxPosition(2.ipx, 0.ipx), Alignment.TopCenter.align(size))
        assertEquals(IntPxPosition(3.ipx, 0.ipx), Alignment.TopEnd.align(size))
        assertEquals(IntPxPosition(0.ipx, 2.ipx), Alignment.CenterStart.align(size))
        assertEquals(IntPxPosition(2.ipx, 2.ipx), Alignment.Center.align(size))
        assertEquals(IntPxPosition(3.ipx, 2.ipx), Alignment.CenterEnd.align(size))
        assertEquals(IntPxPosition(0.ipx, 3.ipx), Alignment.BottomStart.align(size))
        assertEquals(IntPxPosition(2.ipx, 3.ipx), Alignment.BottomCenter.align(size))
        assertEquals(IntPxPosition(3.ipx, 3.ipx), Alignment.BottomEnd.align(size))
    }

    @Test
    fun test2DAlignedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(Modifier.wrapContentSize(Alignment.TopStart).aspectRatio(2f)) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))

            // Min height.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(50.dp.toIntPx() / 2, minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))

            // Max width.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))

            // Max height.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(50.dp.toIntPx() / 2, maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun test1DAlignedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics({
            Container(Modifier.wrapContentHeight(Alignment.CenterVertically)
                .aspectRatio(2f)
            ) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->

            // Min width.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))

            // Min height.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(50.dp.toIntPx() / 2, minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))

            // Max width.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))

            // Max height.
            assertEquals(0.ipx, minIntrinsicWidth(0.ipx))
            assertEquals(50.dp.toIntPx() / 2, maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testAlignedModifier_alignsCorrectly_whenOddDimensions_endAligned() = with(density) {
        // Given a 100 x 100 pixel container, we want to make sure that when aligning a 1 x 1 pixel
        // child to both ends (bottom, and right) we correctly position children at the last
        // possible pixel, and avoid rounding issues. Previously we first centered the coordinates,
        // and then aligned after, so the maths would actually be (99 / 2) * 2, which incorrectly
        // ends up at 100 (IntPx rounds up) - so the last pixels in both directions just wouldn't
        // be visible.
        val parentSize = 100.ipx.toDp()
        val childSizeDp = 1.ipx.toDp()
        val childSizeIpx = childSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntPxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Layout(
                children = {
                    Container(
                        Modifier.preferredSize(parentSize)
                            .saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.fillMaxSize()
                                .wrapContentSize(Alignment.BottomEnd)
                                .preferredSize(childSizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                }, measureBlock = { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(childSizeIpx, childSizeIpx), childSize.value)
        assertEquals(
            PxPosition(
                alignSize.value!!.width - childSizeIpx,
                alignSize.value!!.height - childSizeIpx
            ),
            childPosition.value
        )
    }
}
