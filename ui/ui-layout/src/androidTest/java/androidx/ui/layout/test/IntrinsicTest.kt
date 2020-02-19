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
import androidx.ui.core.Layout
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Ref
import androidx.ui.layout.Align
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.MinIntrinsicHeight
import androidx.ui.layout.MinIntrinsicWidth
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class IntrinsicTest : LayoutTest() {
    @Test
    fun testMinIntrinsicWidth() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    minIntrinsicWidthSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    MinIntrinsicWidth {
                        FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(10.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(IntPxSize(10.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    minIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    MinIntrinsicHeight {
                        FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 40.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 40.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicWidthSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    MaxIntrinsicWidth {
                        FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(30.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(IntPxSize(30.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    MaxIntrinsicHeight {
                        FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 60.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 60.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_respectsIncomingMaxConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                ConstrainedBox(DpConstraints(maxWidth = 5.dp)) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        minIntrinsicWidthSize.value = coordinates.size
                        positionedLatch.countDown()
                    }) {
                        MinIntrinsicWidth {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(5.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(IntPxSize(5.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_respectsIncomingMinConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                ConstrainedBox(DpConstraints(minWidth = 15.dp)) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        minIntrinsicWidthSize.value = coordinates.size
                        positionedLatch.countDown()
                    }) {
                        MinIntrinsicWidth {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(15.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(IntPxSize(15.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight_respectsMaxIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    minIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(maxHeight = 35.dp)) {
                        MinIntrinsicHeight {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 35.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 35.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight_respectsMinIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    minIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(minHeight = 45.dp)) {
                        MinIntrinsicHeight {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 45.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 45.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth_respectsMaxIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicWidthSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(maxWidth = 25.dp)) {
                        MaxIntrinsicWidth {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(25.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(IntPxSize(25.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth_respectsMinIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicWidthSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(minWidth = 35.dp)) {
                        MaxIntrinsicWidth {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(35.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(IntPxSize(35.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight_respectsMaxIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(maxHeight = 55.dp)) {
                        MaxIntrinsicHeight {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 55.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 55.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight_respectsMinIncomingConstraints() = with(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopStart) {
                OnChildPositioned(onPositioned = { coordinates ->
                    maxIntrinsicHeightSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    ConstrainedBox(DpConstraints(minHeight = 65.dp)) {
                        MaxIntrinsicHeight {
                            FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) {
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

        assertEquals(IntPxSize(20.dp.toIntPx(), 65.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(IntPxSize(20.dp.toIntPx(), 65.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_intrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            MinIntrinsicWidth {
                FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.ipx))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.ipx))
            assertEquals(10.dp.toIntPx(), maxIntrinsicWidth(0.ipx))
            assertEquals(60.dp.toIntPx(), maxIntrinsicHeight(0.ipx))
        }
    }

    @Test
    fun testMinIntrinsicHeight_intrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            MinIntrinsicHeight {
                FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.ipx))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.ipx))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(0.ipx))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(0.ipx))
        }
    }

    @Test
    fun testMaxIntrinsicWidth_intrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            MaxIntrinsicWidth {
                FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(30.dp.toIntPx(), minIntrinsicWidth(0.ipx))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.ipx))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(0.ipx))
            assertEquals(60.dp.toIntPx(), maxIntrinsicHeight(0.ipx))
        }
    }

    @Test
    fun testMaxIntrinsicHeight_intrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            MaxIntrinsicHeight {
                FixedIntrinsicsBox(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(10.dp.toIntPx(), minIntrinsicWidth(0.ipx))
            assertEquals(60.dp.toIntPx(), minIntrinsicHeight(0.ipx))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(0.ipx))
            assertEquals(60.dp.toIntPx(), maxIntrinsicHeight(0.ipx))
        }
    }
}

@Composable
private fun FixedIntrinsicsBox(
    minIntrinsicWidth: Dp,
    width: Dp,
    maxIntrinsicWidth: Dp,
    minIntrinsicHeight: Dp,
    height: Dp,
    maxIntrinsicHeight: Dp,
    children: @Composable() () -> Unit
) {
    Layout(
        children,
        minIntrinsicWidthMeasureBlock = { _, _, _ -> minIntrinsicWidth.toIntPx() },
        minIntrinsicHeightMeasureBlock = { _, _, _ -> minIntrinsicHeight.toIntPx() },
        maxIntrinsicWidthMeasureBlock = { _, _, _ -> maxIntrinsicWidth.toIntPx() },
        maxIntrinsicHeightMeasureBlock = { _, _, _ -> maxIntrinsicHeight.toIntPx() }
    ) { _, constraints, _ ->
        layout(
            width.toIntPx().coerceIn(constraints.minWidth, constraints.maxWidth),
            height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {}
    }
}
