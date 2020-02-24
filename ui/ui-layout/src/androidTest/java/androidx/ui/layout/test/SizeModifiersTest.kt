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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Ref
import androidx.ui.layout.Align
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class SizeModifiersTest : LayoutTest() {

    @Test
    fun testSize_withWidthSizeModifiers() = with(density) {
        val sizeDp = 50.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntPxSize>() }
        val position = MutableList(6) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        LayoutWidth.Max(sizeDp * 2) + LayoutWidth.Min(sizeDp) + LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(LayoutWidth.Max(sizeDp * 2) + LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(LayoutWidth.Min(sizeDp) + LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(
                        LayoutWidth.Max(sizeDp) + LayoutWidth.Min(sizeDp * 2) + LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(
                        LayoutWidth.Min(sizeDp * 2) + LayoutWidth.Max(sizeDp) + LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                    Container(LayoutWidth(sizeDp) + LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[5], position[5], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(IntPxSize(0.ipx, sizeIpx), size[1].value)
        assertEquals(PxPosition(0.ipx, sizeIpx), position[1].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 2), position[2].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 3), position[3].value)

        assertEquals(IntPxSize((sizeDp * 2).toIntPx(), sizeIpx), size[4].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 4), position[4].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 5), position[5].value)
    }

    @Test
    fun testSize_withHeightSizeModifiers() = with(density) {
        val sizeDp = 10.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntPxSize>() }
        val position = MutableList(6) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopStart) {
                Row {
                    Container(LayoutHeight.Max(sizeDp * 2) + LayoutHeight.Min(sizeDp) +
                            LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(LayoutHeight.Max(sizeDp * 2) + LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(LayoutHeight.Min(sizeDp) + LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(LayoutHeight.Max(sizeDp) + LayoutHeight.Min(sizeDp * 2) +
                            LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(LayoutHeight.Min(sizeDp * 2) + LayoutHeight.Max(sizeDp) +
                            LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                    Container(LayoutHeight(sizeDp) + LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[5], position[5], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(IntPxSize(sizeIpx, 0.ipx), size[1].value)
        assertEquals(PxPosition(sizeIpx, 0.ipx), position[1].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(sizeIpx * 2, 0.ipx), position[2].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(PxPosition(sizeIpx * 3, 0.ipx), position[3].value)

        assertEquals(IntPxSize(sizeIpx, (sizeDp * 2).toIntPx()), size[4].value)
        assertEquals(PxPosition(sizeIpx * 4, 0.ipx), position[4].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(PxPosition(sizeIpx * 5, 0.ipx), position[5].value)
    }

    @Test
    fun testSize_withSizeModifiers() = with(density) {
        val sizeDp = 50.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(5)
        val size = MutableList(5) { Ref<IntPxSize>() }
        val position = MutableList(5) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopStart) {
                Row {
                    Container(LayoutSize.Max(sizeDp * 2) + LayoutSize.Min(sizeDp)) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(LayoutSize.Max(sizeDp) + LayoutSize.Min(sizeDp * 2, sizeDp)) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(LayoutSize.Min(sizeDp) + LayoutSize.Max(sizeDp * 2)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(LayoutSize.Min(sizeDp * 2) + LayoutSize.Max(sizeDp)) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(LayoutSize(sizeDp)) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(PxPosition(sizeIpx, 0.ipx), position[1].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(sizeIpx * 2, 0.ipx), position[2].value)

        assertEquals(IntPxSize((sizeDp * 2).toIntPx(), (sizeDp * 2).toIntPx()), size[3].value)
        assertEquals(PxPosition(sizeIpx * 3, 0.ipx), position[3].value)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[4].value)
        assertEquals(PxPosition((sizeDp * 5).toIntPx(), 0.ipx), position[4].value)
    }

    @Test
    fun testSizeModifiers_respectMaxConstraint() = with(density) {
        val sizeDp = 100.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val constrainedBoxSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                Container(width = sizeDp, height = sizeDp) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        constrainedBoxSize.value = coordinates.size
                        positionedLatch.countDown()
                    }) {
                        Container(LayoutWidth(sizeDp * 2) + LayoutHeight(sizeDp * 3)) {
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

        assertEquals(IntPxSize(size, size), constrainedBoxSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition.Origin, childPosition.value)
    }

    @Test
    fun testMaxModifiers_withInfiniteValue() = with(density) {
        val sizeDp = 20.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(4)
        val size = MutableList(4) { Ref<IntPxSize>() }
        val position = MutableList(4) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopStart) {
                Row {
                    Container(LayoutWidth.Max(Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[0], position[0], positionedLatch)
                        }
                    }
                    Container(LayoutHeight.Max(Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[1], position[1], positionedLatch)
                        }
                    }
                    Container(
                        LayoutWidth(sizeDp) + LayoutHeight(sizeDp) + LayoutWidth.Max(Dp.Infinity) +
                                LayoutHeight.Max(Dp.Infinity)
                    ) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(LayoutSize.Max(Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[3], position[3], positionedLatch)
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(IntPxSize(sizeIpx, sizeIpx), size[3].value)
    }

    @Test
    fun testMinWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutWidth.Min(10.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(5.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(5.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testMaxWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutWidth.Max(20.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testMinHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutHeight.Min(30.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testMaxHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutHeight.Max(40.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutWidth(10.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(10.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(70.dp.toIntPx(), minIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(70.dp.toIntPx(), maxIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutHeight(10.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(75.dp.toIntPx(), minIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(10.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(75.dp.toIntPx(), maxIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(10.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testWidthHeightModifiers_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(
                LayoutWidth.Min(10.dp) + LayoutWidth.Max(20.dp) + LayoutHeight.Min(30.dp) +
                        LayoutHeight.Max(40.dp)
            ) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(35.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testMinSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutSize.Min(20.dp, 30.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(10.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(10.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(10.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(20.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(10.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(30.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testMaxSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutSize.Max(40.dp, 50.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), minIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(75.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicWidth(15.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(15.dp.toIntPx(), maxIntrinsicHeight(15.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(75.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(LayoutSize(40.dp, 50.dp)) {
                Container(LayoutAspectRatio(1f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(35.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(75.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(35.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }
}