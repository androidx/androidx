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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Ref
import androidx.ui.core.enforce
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
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
class AlignTest : LayoutTest() {
    @Test
    fun testAlign() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntPxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.BottomEnd) {
                SaveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
                Container(width = sizeDp, height = sizeDp) {
                    SaveLayoutInfo(
                        size = childSize,
                        position = childPosition,
                        positionedLatch = positionedLatch
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, root.height.ipx), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(
            PxPosition(root.width.px - size, root.height.px - size),
            childPosition.value
        )
    }

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
            Container {
                SaveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
                Container(LayoutSize.Fill + LayoutAlign.BottomRight + LayoutSize(sizeDp, sizeDp)) {
                    SaveLayoutInfo(
                        size = childSize,
                        position = childPosition,
                        positionedLatch = positionedLatch
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, root.height.ipx), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(
            PxPosition(root.width.px - size, root.height.px - size),
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
            Container {
                SaveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
                Container(LayoutSize.Fill + LayoutAlign.End + LayoutWidth(sizeDp)) {
                    SaveLayoutInfo(
                        size = childSize,
                        position = childPosition,
                        positionedLatch = positionedLatch
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, root.height.ipx), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(IntPxSize(size, root.height.ipx), childSize.value)
        assertEquals(PxPosition(root.width.px - size, 0.px), childPosition.value)
    }

    @Test
    fun testModifier_wrapsContent() = with(density) {
        val contentSize = 50.dp
        val size = Ref<IntPxSize>()
        val latch = CountDownLatch(1)
        show {
            Container {
                Container {
                    SaveLayoutInfo(size, Ref(), latch)
                    Container(LayoutAlign.TopLeft + LayoutSize(contentSize)) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntPxSize(contentSize.toIntPx(), contentSize.toIntPx()), size.value)
    }

    @Test
    fun testAlign_wrapsContent_whenMeasuredWithInfiniteConstraints() = with(density) {
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
                    Align(alignment = Alignment.BottomEnd) {
                        SaveLayoutInfo(
                            size = alignSize,
                            position = alignPosition,
                            positionedLatch = positionedLatch
                        )
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                },
                measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testAlignedModifier_wrapsContent_whenMeasuredWithInfiniteConstraints() = with(
        density
    ) {
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
                    Container {
                        SaveLayoutInfo(
                            size = alignSize,
                            position = alignPosition,
                            positionedLatch = positionedLatch
                        )
                        Container(LayoutAlign.BottomRight + LayoutSize(sizeDp, sizeDp)) {
                            SaveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                },
                measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
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
            Container(LayoutAlign.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Layout(
                        children = {
                            Container(LayoutAlign.Center + LayoutSize(sizeDp, sizeDp)) {
                                SaveLayoutInfo(
                                    size = childSize,
                                    position = childPosition,
                                    positionedLatch = positionedLatch
                                )
                            }
                        },
                        measureBlock = { measurables, incomingConstraints ->
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
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

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
    fun testAlign_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Align(alignment = Alignment.TopStart) {
                Container(LayoutAspectRatio(2f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(50.dp.toIntPx() / 2, minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(50.dp.toIntPx() / 2, maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun test2DAlignedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutAlign.TopLeft + LayoutAspectRatio(2f)) { }
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
        testIntrinsics(@Composable {
            Container(LayoutAlign.CenterVertically + LayoutAspectRatio(2f)) { }
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
    fun testAlign_hasCorrectIntrinsicMeasurements_whenNoChildren() = with(density) {
        testIntrinsics(@Composable {
            Align(alignment = Alignment.TopStart) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(25.dp.toIntPx()))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(25.dp.toIntPx()))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(25.dp.toIntPx()))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(25.dp.toIntPx()))
        }
    }

    @Test
    fun testAlign_alignsCorrectly_whenOddDimensions_endAligned() = with(density) {
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
                    Container(width = parentSize, height = parentSize) {
                        Align(alignment = Alignment.BottomEnd) {
                            SaveLayoutInfo(
                                size = alignSize,
                                position = alignPosition,
                                positionedLatch = positionedLatch
                            )
                            Container(width = childSizeDp, height = childSizeDp) {
                                SaveLayoutInfo(
                                    size = childSize,
                                    position = childPosition,
                                    positionedLatch = positionedLatch
                                )
                            }
                        }
                    }
                }, measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Container(LayoutSize(parentSize, parentSize)) {
                        SaveLayoutInfo(
                            size = alignSize,
                            position = alignPosition,
                            positionedLatch = positionedLatch
                        )
                        Container(LayoutSize.Fill + LayoutAlign.BottomRight +
                                LayoutSize(childSizeDp, childSizeDp)) {
                            SaveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                }, measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
