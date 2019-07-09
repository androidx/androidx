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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.test.filters.SmallTest
import androidx.ui.core.ComplexLayout
import androidx.ui.core.Dp
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.MaxIntrinsicHeight
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.MinIntrinsicHeight
import androidx.ui.layout.MinIntrinsicWidth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class IntrinsicTest : LayoutTest() {
    @Test
    fun testMinIntrinsicWidth() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(10.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(PxSize(10.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 40.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 40.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(30.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(PxSize(30.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 60.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 60.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_respectsIncomingMaxConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(5.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(PxSize(5.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_respectsIncomingMinConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(15.dp.toIntPx(), 50.dp.toIntPx()), minIntrinsicWidthSize.value)
        assertEquals(PxSize(15.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight_respectsMaxIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 35.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 35.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicHeight_respectsMinIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val minIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 45.dp.toIntPx()), minIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 45.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth_respectsMaxIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(25.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(PxSize(25.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicWidth_respectsMinIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicWidthSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(35.dp.toIntPx(), 50.dp.toIntPx()), maxIntrinsicWidthSize.value)
        assertEquals(PxSize(35.dp.toIntPx(), 50.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight_respectsMaxIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 55.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 55.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMaxIntrinsicHeight_respectsMinIncomingConstraints() = withDensity(density) {
        val positionedLatch = CountDownLatch(2)
        val maxIntrinsicHeightSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
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

        assertEquals(PxSize(20.dp.toIntPx(), 65.dp.toIntPx()), maxIntrinsicHeightSize.value)
        assertEquals(PxSize(20.dp.toIntPx(), 65.dp.toIntPx()), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testMinIntrinsicWidth_intrinsicMeasurements() = withDensity(density) {
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
    fun testMinIntrinsicHeight_intrinsicMeasurements() = withDensity(density) {
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
    fun testMaxIntrinsicWidth_intrinsicMeasurements() = withDensity(density) {
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
    fun testMaxIntrinsicHeight_intrinsicMeasurements() = withDensity(density) {
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
    height: Dp, maxIntrinsicHeight: Dp,
    @Children children: @Composable() () -> Unit
) {
    ComplexLayout(children) {
        layout { _, constraints ->
            layoutResult(
                width.toIntPx().coerceIn(constraints.minWidth, constraints.maxWidth),
                height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            ) { }
        }
        minIntrinsicWidth { _, _ -> minIntrinsicWidth.toIntPx() }
        minIntrinsicHeight { _, _ -> minIntrinsicHeight.toIntPx() }
        maxIntrinsicWidth { _, _ -> maxIntrinsicWidth.toIntPx() }
        maxIntrinsicHeight { _, _ -> maxIntrinsicHeight.toIntPx() }
    }
}
