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
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.onPositioned
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.ExperimentalLayout
import androidx.ui.layout.FlowColumn
import androidx.ui.layout.FlowCrossAxisAlignment
import androidx.ui.layout.FlowMainAxisAlignment
import androidx.ui.layout.FlowRow
import androidx.ui.layout.SizeMode
import androidx.ui.layout.Stack
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalLayout::class)
class FlowTest : LayoutTest() {
    @Test
    fun testFlowRow() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisSize_wrap() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(mainAxisSize = SizeMode.Wrap) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisSize_expand() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(mainAxisSize = SizeMode.Expand) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.Center
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = (flowWidth - size * 5) / 2 + size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.Start
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.End
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = flowWidth - size * 5 + size * (i % 5), y = size * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_spaceEvenly() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = (flowWidth - size * 5) * (i % 5 + 1) / 6 + size * (i % 5),
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_spaceBetween() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = (flowWidth - size * 5) * (i % 5) / 4 + size * (i % 5),
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisAlignment_spaceAround() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceAround
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = flowWidth, height = size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = (flowWidth - size * 5) * (i % 5 + 0.5f) / 5 + size * (i % 5),
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withLastLineMainAxisAlignment_justify_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.Center
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(flowWidth, size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = if (i < 10) {
                        (flowWidth - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        (flowWidth - size * 5) / 2 + size * (i % 5)
                    },
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withLastLineMainAxisAlignment_justify_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.Start
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(flowWidth, size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = if (i < 10) {
                        (flowWidth - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        size * (i % 5)
                    },
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withLastLineMainAxisAlignment_justify_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.End
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(flowWidth, size * 3),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = if (i < 10) {
                        (flowWidth - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        (flowWidth - size * 5) + size * (i % 5)
                    },
                    y = size * (i / 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withMainAxisSpacing() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val spacing = 32.ipx
        val spacingDp = spacing.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(mainAxisSpacing = spacingDp) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3 + spacing * 2, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = (size + spacing) * (i % 3), y = size * (i / 3)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withCrossAxisAlignment_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(crossAxisAlignment = FlowCrossAxisAlignment.Center) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp,
                                height = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 6),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(
                    width = size,
                    height = if (i % 2 == 0) size else size * 2
                ),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i % 5),
                    y = size * 2 * (i / 5) + if (i % 2 == 0) size / 2 else IntPx.Zero
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withCrossAxisAlignment_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(crossAxisAlignment = FlowCrossAxisAlignment.Start) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp,
                                height = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 6),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(
                    width = size,
                    height = if (i % 2 == 0) size else size * 2
                ),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = size * 2 * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withCrossAxisAlignment_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(crossAxisAlignment = FlowCrossAxisAlignment.End) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp,
                                height = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 6),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(
                    width = size,
                    height = if (i % 2 == 0) size else size * 2
                ),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i % 5),
                    y = size * 2 * (i / 5) + if (i % 2 == 0) size else IntPx.Zero
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowRow_withCrossAxisSpacing() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val spacing = 32.ipx
        val spacingDp = spacing.toDp()
        val flowWidth = 256.ipx
        val flowWidthDp = flowWidth.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxWidth = flowWidthDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowRow(crossAxisSpacing = spacingDp) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 3 + spacing * 2),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i % 5), y = (size + spacing) * (i / 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisSize_wrap() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(mainAxisSize = SizeMode.Wrap) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisSize_expand() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(mainAxisSize = SizeMode.Expand) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.Center
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = (flowHeight - size * 5) / 2 + size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.Start
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(
                    constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.End
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 5), y = flowHeight - size * 5 + size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_spaceEvenly() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = (flowHeight - size * 5) * (i % 5 + 1) / 6 + size * (i % 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_spaceBetween() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = (flowHeight - size * 5) * (i % 5) / 4 + size * (i % 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisAlignment_spaceAround() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceAround
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3, height = flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = (flowHeight - size * 5) * (i % 5 + 0.5f) / 5 + size * (i % 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withLastLineMainAxisAlignment_justify_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.Center
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(size * 3, flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = if (i < 10) {
                        (flowHeight - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        (flowHeight - size * 5) / 2 + size * (i % 5)
                    }
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withLastLineMainAxisAlignment_justify_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.Start
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(size * 3, flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = if (i < 10) {
                        (flowHeight - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        size * (i % 5)
                    }
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withLastLineMainAxisAlignment_justify_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(
                        mainAxisSize = SizeMode.Expand,
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                        lastLineMainAxisAlignment = FlowMainAxisAlignment.End
                    ) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(size * 3, flowHeight),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * (i / 5),
                    y = if (i < 10) {
                        (flowHeight - size * 5) * (i % 5) / 4 + size * (i % 5)
                    } else {
                        (flowHeight - size * 5) + size * (i % 5)
                    }
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withMainAxisSpacing() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val spacing = 32.ipx
        val spacingDp = spacing.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(mainAxisSpacing = spacingDp) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 5, height = size * 3 + spacing * 2),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * (i / 3), y = (size + spacing) * (i % 3)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withCrossAxisAlignment_center() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(crossAxisAlignment = FlowCrossAxisAlignment.Center) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 6, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(
                    width = if (i % 2 == 0) size else size * 2,
                    height = size
                ),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * 2 * (i / 5) + if (i % 2 == 0) size / 2 else IntPx.Zero,
                    y = size * (i % 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withCrossAxisAlignment_start() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(crossAxisAlignment = FlowCrossAxisAlignment.Start) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 6, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = if (i % 2 == 0) size else size * 2, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = size * 2 * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withCrossAxisAlignment_end() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(crossAxisAlignment = FlowCrossAxisAlignment.End) {
                        for (i in 0 until numberOfSquares) {
                            Container(
                                width = if (i % 2 == 0) sizeDp else sizeDp * 2,
                                height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 6, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(
                    width = if (i % 2 == 0) size else size * 2,
                    height = size
                ),
                childSize[i].value
            )
            assertEquals(
                PxPosition(
                    x = size * 2 * (i / 5) + if (i % 2 == 0) size else IntPx.Zero,
                    y = size * (i % 5)
                ),
                childPosition[i].value
            )
        }
    }

    @Test
    fun testFlowColumn_withCrossAxisSpacing() = with(density) {
        val numberOfSquares = 15
        val size = 48.ipx
        val sizeDp = size.toDp()
        val spacing = 32.ipx
        val spacingDp = spacing.toDp()
        val flowHeight = 256.ipx
        val flowHeightDp = flowHeight.toDp()

        val flowSize = Ref<IntPxSize>()
        val childSize = Array(numberOfSquares) { Ref<IntPxSize>() }
        val childPosition = Array(numberOfSquares) { Ref<PxPosition>() }
        val positionedLatch = CountDownLatch(numberOfSquares + 1)

        show {
            Stack {
                ConstrainedBox(constraints = DpConstraints(maxHeight = flowHeightDp),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        flowSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    FlowColumn(crossAxisSpacing = spacingDp) {
                        for (i in 0 until numberOfSquares) {
                            Container(width = sizeDp, height = sizeDp,
                                modifier = Modifier.saveLayoutInfo(
                                    childSize[i],
                                    childPosition[i],
                                    positionedLatch
                                )
                            ) {
                            }
                        }
                    }
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntPxSize(width = size * 3 + spacing * 2, height = size * 5),
            flowSize.value
        )
        for (i in 0 until numberOfSquares) {
            assertEquals(
                IntPxSize(width = size, height = size),
                childSize[i].value
            )
            assertEquals(
                PxPosition(x = (size + spacing) * (i / 5), y = size * (i % 5)),
                childPosition[i].value
            )
        }
    }
}