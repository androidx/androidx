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
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutMaxHeight
import androidx.ui.layout.LayoutMaxSize
import androidx.ui.layout.LayoutMaxWidth
import androidx.ui.layout.LayoutMinHeight
import androidx.ui.layout.LayoutMinSize
import androidx.ui.layout.LayoutMinWidth
import androidx.ui.layout.Row
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class SizeModifiersTest : LayoutTest() {

    @Test
    fun testSize_withWidthSizeModifiers() = withDensity(density) {
        val sizeDp = 50.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<PxSize>() }
        val position = MutableList(6) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopLeft) {
                Column {
                    Container(
                        LayoutMaxWidth(sizeDp * 2) wraps LayoutMinWidth(sizeDp) wraps
                                LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(LayoutMaxWidth(sizeDp * 2) wraps LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(LayoutMinWidth(sizeDp) wraps LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(
                        LayoutMaxWidth(sizeDp) wraps LayoutMinWidth(sizeDp * 2) wraps
                                LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(
                        LayoutMinWidth(sizeDp * 2) wraps LayoutMaxWidth(sizeDp) wraps
                                LayoutHeight(sizeDp)
                    ) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                    Container(LayoutWidth(sizeDp) wraps LayoutHeight(sizeDp)) {
                        SaveLayoutInfo(size[5], position[5], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(PxSize(0.ipx, sizeIpx), size[1].value)
        assertEquals(PxPosition(0.ipx, sizeIpx), position[1].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 2), position[2].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 3), position[3].value)

        assertEquals(PxSize((sizeDp * 2).toIntPx(), sizeIpx), size[4].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 4), position[4].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(PxPosition(0.ipx, sizeIpx * 5), position[5].value)
    }

    @Test
    fun testSize_withHeightSizeModifiers() = withDensity(density) {
        val sizeDp = 10.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<PxSize>() }
        val position = MutableList(6) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopLeft) {
                Row {
                    Container(
                        LayoutMaxHeight(sizeDp * 2) wraps LayoutMinHeight(sizeDp) wraps
                                LayoutWidth(sizeDp)
                    ) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(LayoutMaxHeight(sizeDp * 2) wraps LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(LayoutMinHeight(sizeDp) wraps LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(
                        LayoutMaxHeight(sizeDp) wraps LayoutMinHeight(sizeDp * 2) wraps
                                LayoutWidth(sizeDp)
                    ) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(
                        LayoutMinHeight(sizeDp * 2) wraps LayoutMaxHeight(sizeDp) wraps
                                LayoutWidth(sizeDp)
                    ) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                    Container(LayoutHeight(sizeDp) wraps LayoutWidth(sizeDp)) {
                        SaveLayoutInfo(size[5], position[5], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(PxSize(sizeIpx, 0.ipx), size[1].value)
        assertEquals(PxPosition(sizeIpx, 0.ipx), position[1].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(sizeIpx * 2, 0.ipx), position[2].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(PxPosition(sizeIpx * 3, 0.ipx), position[3].value)

        assertEquals(PxSize(sizeIpx, (sizeDp * 2).toIntPx()), size[4].value)
        assertEquals(PxPosition(sizeIpx * 4, 0.ipx), position[4].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(PxPosition(sizeIpx * 5, 0.ipx), position[5].value)
    }

    @Test
    fun testSize_withSizeModifiers() = withDensity(density) {
        val sizeDp = 50.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(5)
        val size = MutableList(5) { Ref<PxSize>() }
        val position = MutableList(5) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopLeft) {
                Row {
                    Container(
                        LayoutMaxSize(sizeDp * 2, sizeDp * 2)
                                wraps LayoutMinSize(sizeDp, sizeDp)
                    ) {
                        SaveLayoutInfo(size[0], position[0], positionedLatch)
                    }
                    Container(
                        LayoutMaxSize(sizeDp, sizeDp) wraps LayoutMinSize(sizeDp * 2, sizeDp)
                    ) {
                        SaveLayoutInfo(size[1], position[1], positionedLatch)
                    }
                    Container(
                        LayoutMinSize(sizeDp, sizeDp) wraps LayoutMaxSize(sizeDp * 2, sizeDp * 2)
                    ) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(
                        LayoutMinSize(sizeDp * 2, sizeDp * 2)
                                wraps LayoutMaxSize(sizeDp, sizeDp)
                    ) {
                        SaveLayoutInfo(size[3], position[3], positionedLatch)
                    }
                    Container(LayoutSize(sizeDp, sizeDp)) {
                        SaveLayoutInfo(size[4], position[4], positionedLatch)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxPosition.Origin, position[0].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(PxPosition(sizeIpx, 0.ipx), position[1].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxPosition(sizeIpx * 2, 0.ipx), position[2].value)

        assertEquals(PxSize((sizeDp * 2).toIntPx(), (sizeDp * 2).toIntPx()), size[3].value)
        assertEquals(PxPosition(sizeIpx * 3, 0.ipx), position[3].value)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[4].value)
        assertEquals(PxPosition((sizeDp * 5).toIntPx(), 0.ipx), position[4].value)
    }

    @Test
    fun testSizeModifiers_respectMaxConstraint() = withDensity(density) {
        val sizeDp = 100.dp
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
                        Container(LayoutWidth(sizeDp * 2) wraps LayoutHeight(sizeDp * 3)) {
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
        assertEquals(PxPosition.Origin, childPosition.value)
    }

    @Test
    fun testMaxModifiers_withInfiniteValue() = withDensity(density) {
        val sizeDp = 20.dp
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(4)
        val size = MutableList(4) { Ref<PxSize>() }
        val position = MutableList(4) { Ref<PxPosition>() }
        show {
            Align(alignment = Alignment.TopLeft) {
                Row {
                    Container(LayoutMaxWidth(Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[0], position[0], positionedLatch)
                        }
                    }
                    Container(LayoutMaxHeight(Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[1], position[1], positionedLatch)
                        }
                    }
                    Container(LayoutWidth(sizeDp) wraps LayoutHeight(sizeDp) wraps
                            LayoutMaxWidth(Dp.Infinity)
                            wraps LayoutMaxHeight(Dp.Infinity)) {
                        SaveLayoutInfo(size[2], position[2], positionedLatch)
                    }
                    Container(LayoutMaxSize(Dp.Infinity, Dp.Infinity)) {
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(size[3], position[3], positionedLatch)
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(PxSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(PxSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(PxSize(sizeIpx, sizeIpx), size[3].value)
    }

    @Test
    fun testMinWidthModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMinWidth(10.dp)) {
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
    fun testMaxWidthModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMaxWidth(20.dp)) {
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
    fun testMinHeightModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMinHeight(30.dp)) {
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
    fun testMaxHeightModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMaxHeight(40.dp)) {
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
    fun testWidthModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
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
    fun testHeightModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
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
    fun testWidthHeightModifiers_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMinWidth(10.dp) wraps LayoutMaxWidth(20.dp) wraps
                    LayoutMinHeight(30.dp) wraps LayoutMaxHeight(40.dp)) {
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
    fun testMinSizeModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMinSize(20.dp, 30.dp)) {
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
    fun testMaxSizeModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(LayoutMaxSize(40.dp, 50.dp)) {
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
    fun testSizeModifier_hasCorrectIntrinsicMeasurements() = withDensity(density) {
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