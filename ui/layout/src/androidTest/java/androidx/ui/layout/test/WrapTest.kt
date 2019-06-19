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
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.AspectRatio
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.ComplexLayout
import androidx.ui.core.IntPx
import androidx.ui.core.ipx
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
    fun testWrap() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
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

        assertEquals(PxSize(size, size), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrap_respectsMinConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
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

        assertEquals(PxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrap_respectsMinConstraintsAndAlignment() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
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

        assertEquals(PxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(size / 2, size / 2), childPosition.value)
    }

    @Test
    fun testWrap_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        val layoutLatch = CountDownLatch(1)
        show {
            Center {
                val wrappedChild = @Composable {
                    Wrap {
                        AspectRatio(2f) { }
                    }
                }
                ComplexLayout(children = wrappedChild) {
                    layout { measurables, _ ->
                        val wrapMeasurable = measurables.first()
                        // Min width.
                        assertEquals(
                            25.dp.toIntPx() * 2,
                            wrapMeasurable.minIntrinsicWidth(25.dp.toIntPx())
                        )
                        assertEquals(
                            0.dp.toIntPx(),
                            wrapMeasurable.minIntrinsicWidth(IntPx.Infinity)
                        )
                        // Min height.
                        assertEquals(
                            50.dp.toIntPx() / 2,
                            wrapMeasurable.minIntrinsicHeight(50.dp.toIntPx())
                        )
                        assertEquals(
                            0.dp.toIntPx(),
                            wrapMeasurable.minIntrinsicHeight(IntPx.Infinity)
                        )
                        // Max width.
                        assertEquals(
                            25.dp.toIntPx() * 2,
                            wrapMeasurable.maxIntrinsicWidth(25.dp.toIntPx())
                        )
                        assertEquals(
                            0.dp.toIntPx(),
                            wrapMeasurable.maxIntrinsicWidth(IntPx.Infinity)
                        )
                        // Max height.
                        assertEquals(
                            50.dp.toIntPx() / 2,
                            wrapMeasurable.maxIntrinsicHeight(50.dp.toIntPx())
                        )
                        assertEquals(
                            0.dp.toIntPx(),
                            wrapMeasurable.maxIntrinsicHeight(IntPx.Infinity)
                        )
                        layoutLatch.countDown()
                    }
                    minIntrinsicWidth { _, _ -> 0.ipx }
                    maxIntrinsicWidth { _, _ -> 0.ipx }
                    minIntrinsicHeight { _, _ -> 0.ipx }
                    maxIntrinsicHeight { _, _ -> 0.ipx }
                }
            }
        }
        layoutLatch.await(1, TimeUnit.SECONDS)
        Unit
    }

    @Test
    fun testWrap_hasCorrectIntrinsicMeasurements_whenNoChildren() = withDensity(density) {
        val layoutLatch = CountDownLatch(1)
        show {
            Center {
                val wrappedChild = @Composable {
                    Wrap { }
                }
                ComplexLayout(children = wrappedChild) {
                    layout { measurables, _ ->
                        val wrappedMeasurable = measurables.first()
                        // Min width.
                        assertEquals(
                            0.dp.toIntPx(),
                            wrappedMeasurable.minIntrinsicWidth(25.dp.toIntPx())
                        )
                        // Min height.
                        assertEquals(
                            0.dp.toIntPx(),
                            wrappedMeasurable.minIntrinsicHeight(25.dp.toIntPx())
                        )
                        // Max width.
                        assertEquals(
                            0.dp.toIntPx(),
                            wrappedMeasurable.maxIntrinsicWidth(25.dp.toIntPx())
                        )
                        // Max height.
                        assertEquals(
                            0.dp.toIntPx(),
                            wrappedMeasurable.maxIntrinsicHeight(25.dp.toIntPx())
                        )
                        layoutLatch.countDown()
                    }
                    minIntrinsicWidth { _, _ -> 0.ipx }
                    maxIntrinsicWidth { _, _ -> 0.ipx }
                    minIntrinsicHeight { _, _ -> 0.ipx }
                    maxIntrinsicHeight { _, _ -> 0.ipx }
                }
            }
        }
        layoutLatch.await(1, TimeUnit.SECONDS)
        Unit
    }
}
