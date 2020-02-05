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
import androidx.ui.core.OnPositioned
import androidx.ui.core.Ref
import androidx.ui.core.enforce
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWrapped
import androidx.ui.layout.Wrap
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class WrapTest : LayoutTest() {
    @Test
    fun testWrap() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Wrap {
                        Container(width = sizeDp, height = sizeDp) {
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

        assertEquals(IntPxSize(size, size), wrapSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrappedModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntPxSize>()
        show {
            Container(LayoutWrapped) {
                OnPositioned(onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                })
                Container(LayoutSize(sizeDp, sizeDp)) { }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), wrapSize.value)
    }

    @Test
    fun testWrap_respectsMinConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    val constraints =
                        DpConstraints(minWidth = doubleSizeDp, minHeight = doubleSizeDp)
                    ConstrainedBox(constraints = constraints) {
                        Wrap {
                            Container(width = sizeDp, height = sizeDp) {
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

        assertEquals(IntPxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrappedModifier_respectsMinConstraints() = with(density) {
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
                            Container(LayoutWrapped + LayoutSize(sizeDp, sizeDp)) {
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

    @Test
    fun testWrap_respectsMinConstraintsAndAlignment() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    val constraints = DpConstraints(
                        minWidth = doubleSizeDp,
                        minHeight = doubleSizeDp
                    )
                    ConstrainedBox(constraints = constraints) {
                        Wrap(alignment = Alignment.Center) {
                            Container(width = sizeDp, height = sizeDp) {
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

        assertEquals(IntPxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(size / 2, size / 2), childPosition.value)
    }

    @Test
    fun testWrap_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Wrap {
                Container(modifier = LayoutAspectRatio(2f)) { }
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
    fun testWrappedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Container(modifier = LayoutAspectRatio(2f) + LayoutWrapped) { }
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
    fun testWrap_hasCorrectIntrinsicMeasurements_whenNoChildren() = with(density) {
        testIntrinsics(@Composable {
            Wrap { }
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
}