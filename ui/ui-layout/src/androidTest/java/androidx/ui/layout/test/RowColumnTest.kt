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
import androidx.compose.emptyContent
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.text.FirstBaseline
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.WithConstraints
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredHeightIn
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidthIn
import androidx.ui.layout.rtl
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max
import androidx.ui.unit.min
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class RowColumnTest : LayoutTest() {
    // region Size and position tests for Row and Column
    @Test
    fun testRow() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1f, -1f), PxPosition(-1f, -1f))
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).roundToInt().ipx, (sizeDp.toPx() * 2).roundToInt().ipx),
            childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(size.toPx().value, 0f), childPosition[1])
    }

    @Test
    fun testRow_withChildrenWithWeight() = with(density) {
        val width = 50.dp
        val height = 80.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        Modifier.weight(1f)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(
            IntPxSize((rootWidth / 3).round(), childrenHeight),
            childSize[0]
        )
        assertEquals(
            IntPxSize((rootWidth * 2 / 3).round(), childrenHeight),
            childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition((rootWidth / 3).round().toPx().value, 0f), childPosition[1])
    }

    @Test
    fun testRow_withChildrenWithWeightNonFilling() = with(density) {
        val width = 50.dp
        val childrenWidth = width.toIntPx()
        val height = 80.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        Modifier.weight(1f, fill = false)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f, fill = false)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height * 2
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(IntPxSize(childrenWidth, childrenHeight * 2), childSize[1])
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(childrenWidth.toPx().value, 0f), childPosition[1])
    }

    @Test
    fun testColumn() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1f, -1f), PxPosition(-1f, -1f))
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).roundToInt().ipx, (sizeDp.toPx() * 2).roundToInt().ipx),
            childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, size.toPx().value), childPosition[1])
    }

    @Test
    fun testColumn_withChildrenWithWeight() = with(density) {
        val width = 80.dp
        val childrenWidth = width.toIntPx()
        val height = 50.dp

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        Modifier.weight(1f)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootHeight = root.height.px

        assertEquals(
            IntPxSize(childrenWidth, (rootHeight / 3).round()), childSize[0]
        )
        assertEquals(
            IntPxSize(childrenWidth, (rootHeight * 2 / 3).round()), childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, (rootHeight / 3).round().toPx().value), childPosition[1])
    }

    @Test
    fun testColumn_withChildrenWithWeightNonFilling() = with(density) {
        val width = 80.dp
        val childrenWidth = width.toIntPx()
        val height = 50.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        Modifier.weight(1f, fill = false)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                    Container(
                        Modifier.weight(2f, fill = false)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(
            IntPxSize(childrenWidth, childrenHeight), childSize[1]
        )
        assertEquals(PxPosition(0.0f, 0.0f), childPosition[0])
        assertEquals(PxPosition(0.0f, childrenHeight.toPx().value), childPosition[1])
    }

    @Test
    fun testRow_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = with(density) {
        val expectedRowWidth = 11.ipx
        val leftPadding = 1.px
        var rowWidth = 0.ipx
        val width = Array(2) { 0.ipx }
        val x = Array(2) { 0.px }
        val latch = CountDownLatch(2)
        show {
            Row(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(start = leftPadding.toDp())
                    .preferredWidthIn(maxWidth = expectedRowWidth.toDp())
                    .onPositioned { coordinates: LayoutCoordinates ->
                        rowWidth = coordinates.size.width
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            width[0] = coordinates.size.width
                            x[0] = coordinates.globalPosition.x.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            width[1] = coordinates.size.width
                            x[1] = coordinates.globalPosition.x.px
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedRowWidth, rowWidth)
        assertEquals(leftPadding, x[0])
        assertEquals(leftPadding + width[0], x[1])
        assertEquals(rowWidth, width[0] + width[1])
    }

    @Test
    fun testRow_isNotLargerThanItsChildren_becauseOfRoundings() = with(density) {
        val expectedRowWidth = 8.ipx
        val leftPadding = 1.px
        var rowWidth = 0.ipx
        val width = Array(3) { 0.ipx }
        val x = Array(3) { 0.px }
        val latch = CountDownLatch(3)
        show {
            Row(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(start = leftPadding.toDp())
                    .preferredWidthIn(maxWidth = expectedRowWidth.toDp())
                    .onPositioned { coordinates: LayoutCoordinates ->
                        rowWidth = coordinates.size.width
                    }
            ) {
                Container(
                    Modifier.weight(2f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            width[0] = coordinates.size.width
                            x[0] = coordinates.globalPosition.x.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(2f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            width[1] = coordinates.size.width
                            x[1] = coordinates.globalPosition.x.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(3f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            width[2] = coordinates.size.width
                            x[2] = coordinates.globalPosition.x.px
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedRowWidth, rowWidth)
        assertEquals(leftPadding, x[0])
        assertEquals(leftPadding + width[0], x[1])
        assertEquals(leftPadding + width[0] + width[1], x[2])
        assertEquals(rowWidth, width[0] + width[1] + width[2])
    }

    @Test
    fun testColumn_isNotLargetThanItsChildren_becauseOfRoundings() = with(density) {
        val expectedColumnHeight = 8.ipx
        val topPadding = 1.px
        var columnHeight = 0.ipx
        val height = Array(3) { 0.ipx }
        val y = Array(3) { 0.px }
        val latch = CountDownLatch(3)
        show {
            Column(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(top = topPadding.toDp())
                    .preferredHeightIn(maxHeight = expectedColumnHeight.toDp())
                    .onPositioned { coordinates: LayoutCoordinates ->
                        columnHeight = coordinates.size.height
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            height[0] = coordinates.size.height
                            y[0] = coordinates.globalPosition.y.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            height[1] = coordinates.size.height
                            y[1] = coordinates.globalPosition.y.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            height[2] = coordinates.size.height
                            y[2] = coordinates.globalPosition.y.px
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedColumnHeight, columnHeight)
        assertEquals(topPadding, y[0])
        assertEquals(topPadding + height[0], y[1])
        assertEquals(topPadding + height[0] + height[1], y[2])
        assertEquals(columnHeight, height[0] + height[1] + height[2])
    }

    @Test
    fun testColumn_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = with(density) {
        val expectedColumnHeight = 11.ipx
        val topPadding = 1.px
        var columnHeight = 0.ipx
        val height = Array(2) { 0.ipx }
        val y = Array(2) { 0.px }
        val latch = CountDownLatch(2)
        show {
            Column(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(top = topPadding.toDp())
                    .preferredHeightIn(maxHeight = expectedColumnHeight.toDp())
                    .onPositioned { coordinates: LayoutCoordinates ->
                        columnHeight = coordinates.size.height
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            height[0] = coordinates.size.height
                            y[0] = coordinates.globalPosition.y.px
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            height[1] = coordinates.size.height
                            y[1] = coordinates.globalPosition.y.px
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedColumnHeight, columnHeight)
        assertEquals(topPadding, y[0])
        assertEquals(topPadding + height[0], y[1])
        assertEquals(columnHeight, height[0] + height[1])
    }

    // endregion

    // region Cross axis alignment tests in Row
    @Test
    fun testRow_withStretchCrossAxisAlignment() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1f, -1f), PxPosition(-1f, -1f))
        show {
            Row {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.fillMaxHeight()
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.fillMaxHeight()
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, root.height.ipx), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).roundToInt().ipx, root.height.ipx),
            childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(size.toPx().value, 0f), childPosition[1])
    }

    @Test
    fun testRow_withGravityModifier_andGravityParameter() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Row(Modifier.fillMaxHeight(), verticalGravity = Alignment.CenterVertically) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.gravity(Alignment.Top)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.gravity(Alignment.Bottom)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[2] = coordinates.size
                                childPosition[2] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootHeight = root.height.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(size.toPx().value, ((rootHeight - size.toPx()) / 2).round().toPx().value),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(PxPosition((size.toPx() * 2).value,
            (rootHeight - size.toPx()).value),
            childPosition[2])
    }

    @Test
    fun testRow_withGravityModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Row(Modifier.fillMaxHeight()) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.gravity(Alignment.Top)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.gravity(Alignment.CenterVertically)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.gravity(Alignment.Bottom)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootHeight = root.height.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(size.toPx().value, ((rootHeight - size.toPx()) / 2).round().toPx().value),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition((size.toPx() * 2).value, (rootHeight - size.toPx()).value),
            childPosition[2])
    }

    @Test
    fun testRow_withRelativeToSiblingsModifier() = with(density) {
        val baseline1Dp = 30.dp
        val baseline1 = baseline1Dp.toIntPx()
        val baseline2Dp = 25.dp
        val baseline2 = baseline2Dp.toIntPx()
        val sizeDp = 40.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childSize = arrayOfNulls<IntPxSize>(4)
        val childPosition = arrayOfNulls<PxPosition>(4)
        show {
            Row(Modifier.fillMaxHeight()) {
                    BaselineTestLayout(
                        baseline = baseline1Dp,
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings(TestHorizontalLine)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings { it.height * 0.5 }
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    BaselineTestLayout(
                        baseline = baseline2Dp,
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings(TestHorizontalLine)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[2] = coordinates.size
                                childPosition[2] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings { it.height * 0.75 }
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[3] = coordinates.size
                                childPosition[3] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(size.toPx().value, (baseline1 - (size.toPx() / 2).round()).toPx().value),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition((size.toPx() * 2).value, (baseline1 - baseline2).toPx().value),
            childPosition[2]
        )

        assertEquals(IntPxSize(size, size), childSize[3])
        assertEquals(
            PxPosition((size.toPx() * 3).value, 0f),
            childPosition[3]
        )
    }

    @Test
    fun testRow_withRelativeToSiblingsModifier_andWeight() = with(density) {
        val baselineDp = 30.dp
        val baseline = baselineDp.toIntPx()
        val sizeDp = 40.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Row(Modifier.fillMaxHeight()) {
                BaselineTestLayout(
                    baseline = baselineDp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignWithSiblings(TestHorizontalLine)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    height = sizeDp,
                    modifier = Modifier.alignWithSiblings { it.height * 0.5 }
                        .weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(size, childSize[1]!!.height)
        assertEquals(
            PxPosition(size.toPx().value, (baseline - size / 2).toPx().value),
            childPosition[1]
        )
    }
    // endregion

    // region Cross axis alignment tests in Column
    @Test
    fun testColumn_withStretchCrossAxisAlignment() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1f, -1f), PxPosition(-1f, -1f))
        show {
            Column {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.fillMaxWidth()
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.fillMaxWidth()
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, size), childSize[0])
        assertEquals(
            IntPxSize(root.width.ipx, (sizeDp * 2).toIntPx()),
            childSize[1]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, size.toPx().value), childPosition[1])
    }

    @Test
    fun testColumn_withGravityModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Column(Modifier.fillMaxWidth()) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.gravity(Alignment.Start)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.gravity(Alignment.CenterHorizontally)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.gravity(Alignment.End)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[2] = coordinates.size
                                childPosition[2] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(
                ((rootWidth - size.toPx()) / 2).round().toPx().value,
                size.toPx().value
            ),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition((rootWidth - size.toPx()).value, size.toPx().value * 2),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withGravityModifier_andGravityParameter() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Column(Modifier.fillMaxWidth(), horizontalGravity = Alignment.CenterHorizontally) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.gravity(Alignment.Start)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        childSize[1] = coordinates.size
                        childPosition[1] = coordinates.globalPosition
                        drawLatch.countDown()
                    }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.gravity(Alignment.End)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(
                ((rootWidth - size.toPx()) / 2).round().toPx().value,
                size.toPx().value
            ),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition((rootWidth - size.toPx()).value, size.toPx().value * 2),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withRelativeToSiblingsModifier() = with(density) {
        val sizeDp = 40.dp
        val size = sizeDp.toIntPx()
        val firstBaseline1Dp = 20.dp
        val firstBaseline2Dp = 30.dp

        val drawLatch = CountDownLatch(4)
        val childSize = arrayOfNulls<IntPxSize>(4)
        val childPosition = arrayOfNulls<PxPosition>(4)
        show {
            Column(Modifier.fillMaxWidth()) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings { it.width }
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.alignWithSiblings { 0.ipx }
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    BaselineTestLayout(
                        width = sizeDp,
                        height = sizeDp,
                        baseline = firstBaseline1Dp,
                        modifier = Modifier.alignWithSiblings(TestVerticalLine)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[2] = coordinates.size
                                childPosition[2] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    BaselineTestLayout(
                        width = sizeDp,
                        height = sizeDp,
                        baseline = firstBaseline2Dp,
                        modifier = Modifier.alignWithSiblings(TestVerticalLine)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                childSize[3] = coordinates.size
                                childPosition[3] = coordinates.globalPosition
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(PxPosition(size.toPx().value, size.toPx().value), childPosition[1])

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition(
                (size - firstBaseline1Dp.toIntPx()).toPx().value,
                size.toPx().value * 2
            ),
            childPosition[2]
        )

        assertEquals(IntPxSize(size, size), childSize[3])
        assertEquals(
            PxPosition(
                (size - firstBaseline2Dp.toIntPx()).toPx().value,
                size.toPx().value * 3
            ),
            childPosition[3]
        )
    }

    @Test
    fun testColumn_withRelativeToSiblingsModifier_andWeight() = with(density) {
        val baselineDp = 30.dp
        val baseline = baselineDp.toIntPx()
        val sizeDp = 40.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Column(Modifier.fillMaxWidth()) {
                BaselineTestLayout(
                    baseline = baselineDp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignWithSiblings(TestVerticalLine)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    modifier = Modifier.alignWithSiblings { it.width * 0.5 }
                        .weight(1f)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0f, 0f), childPosition[0])

        assertEquals(size, childSize[1]!!.width)
        assertEquals(
            PxPosition((baseline - (size / 2)).toPx().value, size.toPx().value),
            childPosition[1]
        )
    }
    // endregion

    // region Size tests in Row
    @Test
    fun testRow_expandedWidth_withExpandedModifier() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.width.ipx,
            rowSize.width
        )
    }

    @Test
    fun testRow_wrappedWidth_withNoWeightChildren() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 3).toIntPx(),
            rowSize.width
        )
    }

    @Test
    fun testRow_expandedWidth_withWeightChildren() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Container(
                        Modifier.weight(1f),
                        width = sizeDp,
                        height = sizeDp,
                        children = emptyContent()
                    )
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        children = emptyContent()
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.width.ipx,
            rowSize.width
        )
    }

    @Test
    fun testRow_withMaxCrossAxisSize() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.height.ipx,
            rowSize.height
        )
    }

    @Test
    fun testRow_withMinCrossAxisSize() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 2).toIntPx(),
            rowSize.height
        )
    }

    @Test
    fun testRow_withExpandedModifier_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.width.ipx, rowWidthDp.toIntPx()),
            rowSize.width
        )
    }

    @Test
    fun testRow_withChildrenWithWeight_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            Modifier.weight(1f),
                            width = sizeDp,
                            height = sizeDp,
                            children = emptyContent()
                        )
                        Container(
                            width = sizeDp * 2,
                            height = sizeDp * 2,
                            children = emptyContent()
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.width.ipx, rowWidthDp.toIntPx()),
            rowSize.width
        )
    }

    @Test
    fun testRow_withNoWeightChildren_respectsMinWidthConstraint() = with(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = rowWidthDp)) {
                    Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            rowWidthDp.toIntPx(),
            rowSize.width
        )
    }

    @Test
    fun testRow_withMaxCrossAxisSize_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.dp
        val rowHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = rowHeightDp)) {
                    Row(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.height.ipx, rowHeightDp.toIntPx()),
            rowSize.height
        )
    }

    @Test
    fun testRow_withMinCrossAxisSize_respectsMinHeightConstraint() = with(density) {
        val sizeDp = 50.dp
        val rowHeightDp = 150.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = rowHeightDp)) {
                    Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            rowHeightDp.toIntPx(),
            rowSize.height
        )
    }

    @Test
    @Ignore(
        "Wrap is not supported when there are children with weight. " +
                "Should use maxWidth(.Infinity) modifier when it is available"
    )
    fun testRow_withMinMainAxisSize() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val rowWidthDp = 250.dp
        val rowWidth = rowWidthDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        lateinit var rowSize: IntPxSize
        lateinit var expandedChildSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = rowWidthDp)) {
                    // TODO: add maxWidth(IntPx.Infinity) modifier
                    Row(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            modifier = Modifier.weight(1f)
                                .onPositioned { coordinates: LayoutCoordinates ->
                                    expandedChildSize = coordinates.size
                                    drawLatch.countDown()
                                },
                            width = sizeDp,
                            height = sizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            IntPxSize(rowWidth, size),
            rowSize
        )
        assertEquals(
            IntPxSize(rowWidth, size),
            expandedChildSize
        )
    }

    @Test
    fun testRow_measuresChildrenCorrectly_whenMeasuredWithInfiniteWidth() = with(density) {
        val rowMinWidth = 100.dp
        val noWeightChildWidth = 30.dp
        val latch = CountDownLatch(1)
        show {
            WithInfiniteConstraints {
                ConstrainedBox(DpConstraints(minWidth = rowMinWidth)) {
                    Row {
                        WithConstraints {
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        WithConstraints {
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        Layout({}, Modifier.weight(1f)) { _, constraints, _ ->
                            assertEquals(
                                rowMinWidth.toIntPx() - noWeightChildWidth.toIntPx() * 2,
                                constraints.minWidth
                            )
                            assertEquals(
                                rowMinWidth.toIntPx() - noWeightChildWidth.toIntPx() * 2,
                                constraints.maxWidth
                            )
                            latch.countDown()
                            layout(0.ipx, 0.ipx) { }
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_measuresNoWeightChildrenCorrectly() = with(density) {
        val availableWidth = 100.dp
        val childWidth = 50.dp
        val availableHeight = 200.dp
        val childHeight = 100.dp
        val latch = CountDownLatch(1)
        show {
            Stack {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Row {
                        WithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx() - childWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                            latch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region Size tests in Column
    @Test
    fun testColumn_expandedHeight_withExpandedModifier() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.height.ipx,
            columnSize.height
        )
    }

    @Test
    fun testColumn_wrappedHeight_withNoChildrenWithWeight() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 3).toIntPx(),
            columnSize.height
        )
    }

    @Test
    fun testColumn_expandedHeight_withWeightChildren() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Container(
                        Modifier.weight(1f),
                        width = sizeDp,
                        height = sizeDp,
                        children = emptyContent()
                    )
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        children = emptyContent()
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.height.ipx,
            columnSize.height
        )
    }

    @Test
    fun testColumn_withMaxCrossAxisSize() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            root.width.ipx,
            columnSize.width
        )
    }

    @Test
    fun testColumn_withMinCrossAxisSize() = with(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.preferredSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 2).toIntPx(),
            columnSize.width
        )
    }

    @Test
    fun testColumn_withExpandedModifier_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.height.ipx, columnHeightDp.toIntPx()),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withWeightChildren_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            Modifier.weight(1f),
                            width = sizeDp,
                            height = sizeDp,
                            children = emptyContent()
                        )
                        Container(
                            width = sizeDp * 2,
                            height = sizeDp * 2,
                            children = emptyContent()
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.height.ipx, columnHeightDp.toIntPx()),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withChildren_respectsMinHeightConstraint() = with(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = columnHeightDp)) {
                    Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            columnHeightDp.toIntPx(),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withMaxCrossAxisSize_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.dp
        val columnWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = columnWidthDp)) {
                    Column(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            min(root.width.ipx, columnWidthDp.toIntPx()),
            columnSize.width
        )
    }

    @Test
    fun testColumn_withMinCrossAxisSize_respectsMinWidthConstraint() = with(density) {
        val sizeDp = 50.dp
        val columnWidthDp = 150.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = columnWidthDp)) {
                    Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(Modifier.preferredSize(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.preferredSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            columnWidthDp.toIntPx(),
            columnSize.width
        )
    }

    @Test
    @Ignore(
        "Wrap is not supported when there are weight children. " +
                "Should use maxHeight(IntPx.Infinity) modifier when it is available"
    )
    fun testColumn_withMinMainAxisSize() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val columnHeightDp = 250.dp
        val columnHeight = columnHeightDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        lateinit var columnSize: IntPxSize
        lateinit var expandedChildSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = columnHeightDp)) {
                    // TODO: add maxHeight(IntPx.Infinity) modifier
                    Column(Modifier.preferredHeightIn(maxHeight = Dp.Infinity)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Container(Modifier.weight(1f)
                            .onPositioned { coordinates: LayoutCoordinates ->
                                expandedChildSize = coordinates.size
                                drawLatch.countDown()
                            },
                            width = sizeDp,
                            height = sizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            IntPxSize(size, columnHeight),
            columnSize
        )
        assertEquals(
            IntPxSize(size, columnHeight),
            expandedChildSize
        )
    }

    @Test
    fun testColumn_measuresChildrenCorrectly_whenMeasuredWithInfiniteHeight() =
        with(density) {
            val columnMinHeight = 100.dp
            val noWeightChildHeight = 30.dp
            val latch = CountDownLatch(1)
            show {
                WithInfiniteConstraints {
                    ConstrainedBox(DpConstraints(minHeight = columnMinHeight)) {
                        Column {
                            WithConstraints {
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(0.ipx, noWeightChildHeight.toIntPx(), mapOf())
                            }
                            WithConstraints {
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(0.ipx, noWeightChildHeight.toIntPx(), mapOf())
                            }
                            Layout(emptyContent(), Modifier.weight(1f)) { _, constraints, _ ->
                                assertEquals(
                                    columnMinHeight.toIntPx() - noWeightChildHeight.toIntPx() * 2,
                                    constraints.minHeight
                                )
                                assertEquals(
                                    columnMinHeight.toIntPx() - noWeightChildHeight.toIntPx() * 2,
                                    constraints.maxHeight
                                )
                                latch.countDown()
                                layout(0.ipx, 0.ipx) { }
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun testColumn_measuresNoWeightChildrenCorrectly() = with(density) {
        val availableWidth = 100.dp
        val childWidth = 50.dp
        val availableHeight = 200.dp
        val childHeight = 100.dp
        val latch = CountDownLatch(1)
        show {
            Stack {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Column {
                        WithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx() - childHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                            latch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region Main axis alignment tests in Row
    @Test
    fun testRow_withBeginArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth()
                    .onPositioned { coordinates: LayoutCoordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    }
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(size.toPx().value, 0f), childPosition[1])
        assertEquals(PxPosition(size.toPx().value * 2, 0f), childPosition[2])
    }

    @Test
    fun testRow_withEndArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, horizontalArrangement = Arrangement.End) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(PxPosition((root.width.px - size.toPx() * 3).value, 0f), childPosition[0])
        assertEquals(PxPosition((root.width.px - size.toPx() * 2).value, 0f), childPosition[1])
        assertEquals(PxPosition((root.width.px - size.toPx()).value, 0f), childPosition[2])
    }

    @Test
    fun testRow_withCenterArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, horizontalArrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val extraSpace = root.width.px.round() - size * 3
        assertEquals(PxPosition((extraSpace / 2).toPx().value, 0f), childPosition[0])
        assertEquals(
            PxPosition(((extraSpace / 2).toPx() + size.toPx()).value, 0f),
            childPosition[1]
        )
        assertEquals(
            PxPosition(
                ((extraSpace / 2).toPx() + size.toPx() * 2).value,
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(gap.round().toPx().value, 0f), childPosition[0])
        assertEquals(
            PxPosition((size.toPx() + gap * 2).round().toPx().value, 0f),
            childPosition[1]
        )
        assertEquals(
            PxPosition((size.toPx() * 2 + gap * 3).round().toPx().value, 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition((gap + size.toPx()).round().toPx().value, 0f), childPosition[1])
        assertEquals(
            PxPosition((gap * 2 + size.toPx() * 2).round().toPx().value, 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.width.px.round() - size * 3) / 3
        assertEquals(PxPosition((gap / 2).toPx().value, 0f), childPosition[0])
        assertEquals(
            PxPosition(((gap * 3 / 2).toPx() + size.toPx()).value, 0f),
            childPosition[1]
        )
        assertEquals(
            PxPosition(((gap * 5 / 2).toPx() + size.toPx() * 2).value, 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withCustomArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition.Origin, PxPosition.Origin, PxPosition.Origin
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(Modifier.fillMaxWidth().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, horizontalArrangement = customHorizontalArrangement) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val step = (root.width.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(
            PxPosition((step + size.toPx()).round().toPx().value, 0f),
            childPosition[1]
        )
        assertEquals(
            PxPosition((step * 3 + size.toPx() * 2).round().toPx().value, 0f),
            childPosition[2]
        )
    }
    // endregion

    // region Main axis alignment tests in Column
    @Test
    fun testColumn_withStartArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, size.toPx().value), childPosition[1])
        assertEquals(PxPosition(0f, size.toPx().value * 2), childPosition[2])
    }

    @Test
    fun testColumn_withEndArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = Arrangement.Bottom) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(PxPosition(0f, (root.height.px - size.toPx() * 3).value), childPosition[0])
        assertEquals(PxPosition(0f, (root.height.px - size.toPx() * 2).value), childPosition[1])
        assertEquals(PxPosition(0f, (root.height.px - size.toPx()).value), childPosition[2])
    }

    @Test
    fun testColumn_withCenterArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val extraSpace = root.height.px.round() - size * 3
        assertEquals(
            PxPosition(0f, (extraSpace / 2).toPx().value),
            childPosition[0]
        )
        assertEquals(
            PxPosition(0f, ((extraSpace / 2).toPx() + size.toPx()).value),
            childPosition[1]
        )
        assertEquals(
            PxPosition(
                0f,
                ((extraSpace / 2).toPx() + size.toPx() * 2).value
            ),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(0f, gap.round().toPx().value), childPosition[0])
        assertEquals(PxPosition(0f, (size.toPx() + gap * 2).round().toPx().value), childPosition[1])
        assertEquals(
            PxPosition(0f, (size.toPx() * 2 + gap * 3).round().toPx().value),
            childPosition[2]
        )
    }

    private fun calculateChildPositions(
        childPosition: Array<PxPosition>,
        parentLayoutCoordinates: LayoutCoordinates?,
        childLayoutCoordinates: Array<LayoutCoordinates?>
    ) {
        for (i in childPosition.indices) {
            childPosition[i] = parentLayoutCoordinates!!
                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0f, 0f))
        }
    }

    @Test
    fun testColumn_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, (gap + size.toPx()).round().toPx().value), childPosition[1])
        assertEquals(
            PxPosition(0f, (gap * 2 + size.toPx() * 2).round().toPx().value),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1f, -1f), PxPosition(-1f, -1f), PxPosition(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = Arrangement.SpaceAround) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0f, (gap / 2).round().toPx().value), childPosition[0])
        assertEquals(
            PxPosition(0f, ((gap * 3 / 2) + size.toPx()).round().toPx().value),
            childPosition[1]
        )
        assertEquals(
            PxPosition(0f, ((gap * 5 / 2) + size.toPx() * 2).round().toPx().value),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withCustomArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition.Origin, PxPosition.Origin, PxPosition.Origin
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(Modifier.fillMaxHeight().onPositioned { coordinates: LayoutCoordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, verticalArrangement = customVerticalArrangement) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findOwnerView()
        waitForDraw(root)

        val step = (root.height.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, (step + size.toPx()).round().toPx().value), childPosition[1])
        assertEquals(
            PxPosition(0f, (step * 3 + size.toPx() * 2).round().toPx().value),
            childPosition[2]
        )
    }

    @Test
    fun testRow_doesNotUseMinConstraintsOnChildren() = with(density) {
        val sizeDp = 50.dp
        val childSizeDp = 30.dp
        val childSize = childSizeDp.toIntPx()

        val layoutLatch = CountDownLatch(1)
        val containerSize = Ref<IntPxSize>()
        show {
            Center {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp)
                ) {
                    Row {
                        Spacer(
                            Modifier.preferredSize(width = childSizeDp, height = childSizeDp)
                                .onPositioned { coordinates: LayoutCoordinates ->
                                    containerSize.value = coordinates.size
                                    layoutLatch.countDown()
                                })
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(childSize, childSize), containerSize.value)
    }

    @Test
    fun testColumn_doesNotUseMinConstraintsOnChildren() = with(density) {
        val sizeDp = 50.dp
        val childSizeDp = 30.dp
        val childSize = childSizeDp.toIntPx()

        val layoutLatch = CountDownLatch(1)
        val containerSize = Ref<IntPxSize>()
        show {
            Center {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp)
                ) {
                    Column {
                        Spacer(
                            Modifier.preferredSize(width = childSizeDp, height = childSizeDp) +
                                Modifier.onPositioned { coordinates: LayoutCoordinates ->
                                    containerSize.value = coordinates.size
                                    layoutLatch.countDown()
                                })
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(childSize, childSize), containerSize.value)
    }
    // endregion

    // region Intrinsic measurement tests
    @Test
    fun testRow_withNoWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Row {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth()) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row {
                Container(
                    Modifier.aspectRatio(2f)
                        .gravity(Alignment.Top),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row {
                Container(
                    Modifier.aspectRatio(2f).alignWithSiblings(FirstBaseline),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.alignWithSiblings { it.width },
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Container(Modifier.gravity(Alignment.CenterVertically).aspectRatio(2f),
                    children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Container(Modifier.gravity(Alignment.Bottom).aspectRatio(2f),
                    children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.gravity(Alignment.Bottom),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Container(Modifier.fillMaxHeight().aspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.fillMaxHeight(),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(25.dp.toIntPx() * 2 + 50.dp.toIntPx(), minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(25.dp.toIntPx() * 2 + 50.dp.toIntPx(), maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(70.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testRow_withWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Row {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.weight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    Modifier.weight(2f),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(2f).weight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.weight(3f).gravity(Alignment.Top),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    Modifier.weight(2f).gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(2f).weight(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.gravity(Alignment.Bottom),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.Start) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.weight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    Modifier.weight(2f),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(2f).weight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.weight(3f).gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = Modifier.weight(2f).gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
                Container(
                    Modifier.aspectRatio(2f).weight(2f).gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.gravity(Alignment.CenterVertically),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.End) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.weight(3f).gravity(Alignment.Bottom),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = Modifier.weight(2f).gravity(Alignment.Bottom),
                    children = emptyContent()
                )
                Container(
                    Modifier.aspectRatio(2f).weight(2f).gravity(Alignment.Bottom),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.gravity(Alignment.Bottom),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.weight(3f).fillMaxHeight(),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    children = emptyContent()
                )
                Container(
                    Modifier.aspectRatio(2f).weight(2f).fillMaxHeight(),
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = Modifier.fillMaxHeight(),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.weight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    Modifier.weight(2f),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(2f).weight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    Modifier.weight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    Modifier.weight(2f),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(2f).weight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicWidth(0.ipx)
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicWidth(10.dp.toIntPx())
            )
            assertEquals(
                25.dp.toIntPx() * 2 / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicWidth(25.dp.toIntPx())
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicWidth(IntPx.Infinity)
            )
            // Min height.
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(125.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicHeight(370.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicWidth(0.ipx)
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicWidth(10.dp.toIntPx())
            )
            assertEquals(
                25.dp.toIntPx() * 2 / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicWidth(25.dp.toIntPx())
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicWidth(IntPx.Infinity)
            )
            // Max height.
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(125.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicHeight(370.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testColumn_withNoWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Column {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column {
                Container(Modifier.aspectRatio(2f).gravity(Alignment.Start),
                    children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.gravity(Alignment.End),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column {
                Container(
                    Modifier.aspectRatio(2f).alignWithSiblings { 0.ipx },
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.alignWithSiblings(TestVerticalLine),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight()) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Container(Modifier.gravity(Alignment.CenterHorizontally).aspectRatio(2f),
                    children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Container(Modifier.gravity(Alignment.End).aspectRatio(2f),
                    children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.gravity(Alignment.End),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceAround) {
                Container(Modifier.fillMaxWidth().aspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    Modifier.fillMaxWidth(),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Container(Modifier.aspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx() / 2 + 40.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(50.dp.toIntPx() / 2 + 40.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testColumn_withWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(@Composable {
            Column {
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 20.dp),
                    Modifier.weight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(40.dp, 30.dp),
                    Modifier.weight(2f),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(0.5f).weight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), children = emptyContent())
            }
        }, @Composable {
            Column {
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 20.dp),
                    Modifier.weight(3f).gravity(Alignment.Start),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(40.dp, 30.dp),
                    Modifier.weight(2f).gravity(Alignment.CenterHorizontally),
                    children = emptyContent()
                )
                Container(Modifier.aspectRatio(0.5f).weight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp),
                    Modifier.gravity(Alignment.End)) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.Top) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), Modifier.weight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), Modifier.weight(2f)) { }
                Container(Modifier.aspectRatio(0.5f).weight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.weight(3f).gravity(Alignment.CenterHorizontally)
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = Modifier.weight(2f).gravity(Alignment.CenterHorizontally)
                ) { }
                Container(
                    Modifier.aspectRatio(0.5f).weight(2f).gravity(Alignment.CenterHorizontally)
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.gravity(Alignment.CenterHorizontally)
                ) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.Bottom) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.weight(3f).gravity(Alignment.End)
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = Modifier.weight(2f).gravity(Alignment.End)
                ) { }
                Container(
                    Modifier.aspectRatio(0.5f).weight(2f).gravity(Alignment.End)
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.gravity(Alignment.End)
                ) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.SpaceAround) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.weight(3f).fillMaxWidth()
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = Modifier.weight(2f).fillMaxWidth()
                ) { }
                Container(
                    Modifier.aspectRatio(0.5f).weight(2f).fillMaxWidth()
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), Modifier.weight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), Modifier.weight(2f)) { }
                Container(Modifier.aspectRatio(0.5f) + Modifier.weight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(verticalArrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), Modifier.weight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), Modifier.weight(2f)) { }
                Container(Modifier.aspectRatio(0.5f) + Modifier.weight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(125.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), minIntrinsicWidth(370.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicHeight(0.ipx)
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicHeight(10.dp.toIntPx())
            )
            assertEquals(
                25.dp.toIntPx() * 2 / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicHeight(25.dp.toIntPx())
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                minIntrinsicHeight(IntPx.Infinity)
            )
            // Max width.
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(125.dp.toIntPx()))
            assertEquals(50.dp.toIntPx(), maxIntrinsicWidth(370.dp.toIntPx()))
            assertEquals(40.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicHeight(0.ipx)
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicHeight(10.dp.toIntPx())
            )
            assertEquals(
                25.dp.toIntPx() * 2 / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicHeight(25.dp.toIntPx())
            )
            assertEquals(
                30.dp.toIntPx() / 2 * 7 + 20.dp.toIntPx(),
                maxIntrinsicHeight(IntPx.Infinity)
            )
        }
    }
    // endregion

    // region Modifiers specific tests
    @Test
    fun testRowColumnModifiersChain_leftMostWins() = with(density) {
        val positionedLatch = CountDownLatch(1)
        val containerHeight = Ref<IntPx>()
        val columnHeight = 24.ipx

        show {
            Stack {
                Column(Modifier.preferredHeight(columnHeight.toDp())) {
                    Container(
                        Modifier.weight(2f)
                            .weight(1f)
                            .onPositioned { coordinates ->
                                containerHeight.value = coordinates.size.height
                                positionedLatch.countDown()
                            },
                        children = emptyContent()
                    )
                    Container(Modifier.weight(1f), children = emptyContent())
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertNotNull(containerHeight.value)
        assertEquals(columnHeight * 2 / 3, containerHeight.value)
    }

    @Test
    fun testRelativeToSiblingsModifiersChain_leftMostWins() = with(density) {
        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<IntPxSize>()
        val containerPosition = Ref<PxPosition>()
        val size = 40.dp

        show {
            Row {
                Container(
                    modifier = Modifier.alignWithSiblings { it.height },
                    width = size,
                    height = size,
                    children = emptyContent()
                )
                Container(
                    modifier = Modifier.alignWithSiblings { 0.ipx }
                        .alignWithSiblings { it.height * 0.5 }
                        .onPositioned { coordinates: LayoutCoordinates ->
                            containerSize.value = coordinates.size
                            containerPosition.value = coordinates.globalPosition
                            positionedLatch.countDown()
                        },
                    width = size,
                    height = size,
                    children = emptyContent()
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertNotNull(containerSize)
        assertEquals(PxPosition(size.toPx(), size.toPx()), containerPosition.value)
    }
    // endregion

    // region Rtl tests
    @Test
    fun testRow_Rtl_arrangementStart() = with(density) {
        val sizeDp = 35.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Row(Modifier.fillMaxWidth().rtl) {
                Container(
                    Modifier.preferredSize(sizeDp).onPositioned { coordinates: LayoutCoordinates ->
                        childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                        drawLatch.countDown()
                    }
                ) {
                }

                Container(
                    Modifier.preferredSize(sizeDp * 2)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition((rootWidth - size.toPx()).value, 0f), childPosition[0])
        assertEquals(
            PxPosition((rootWidth - (sizeDp.toPx() * 3).roundToInt().px).value, 0f),
            childPosition[1]
        )
    }

    @Test
    fun testRow_Rtl_arrangementEnd() = with(density) {
        val sizeDp = 35.dp

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Row(
                Modifier.fillMaxWidth().rtl,
                horizontalArrangement = Arrangement.End
            ) {
                Container(
                    Modifier.preferredSize(sizeDp).onPositioned { coordinates: LayoutCoordinates ->
                        childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                        drawLatch.countDown()
                    }
                ) {
                }

                Container(
                    Modifier.preferredSize(sizeDp * 2)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            PxPosition(
                (sizeDp.toPx() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[0]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[1])
    }

    @Test
    fun testRow_Rtl_customArrangement() = with(density) {
        val sizeDp = 35.dp

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Row(
                Modifier.fillMaxWidth().rtl,
                horizontalArrangement = customHorizontalArrangement
            ) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                        drawLatch.countDown()
                    }
                ) {
                }

                Container(
                    width = (sizeDp * 2),
                    height = (sizeDp * 2),
                    modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                        childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                        drawLatch.countDown()
                    }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            PxPosition((sizeDp.toPx() * 2).roundToInt().toFloat(), 0f),
            childPosition[0]
        )
        assertEquals(PxPosition(0f, 0f), childPosition[1])
    }

    @Test
    fun testColumn_Rtl_gravityStart() = with(density) {
        val sizeDp = 35.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Column(Modifier.fillMaxWidth().rtl) {
                Container(
                    Modifier.preferredSize(sizeDp).onPositioned { coordinates: LayoutCoordinates ->
                        childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                        drawLatch.countDown()
                    }
                ) {
                }

                Container(
                    Modifier.preferredSize(sizeDp * 2)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition((rootWidth - size.toPx()).value, 0f), childPosition[0])
        assertEquals(
            PxPosition(
                (rootWidth.value - (sizeDp * 2).toPx()).roundToInt().toFloat(),
                size.toPx().value
            ),
            childPosition[1]
        )
    }

    @Test
    fun testColumn_Rtl_gravityEnd() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Column(Modifier.fillMaxWidth().rtl) {
                Container(
                    Modifier.preferredSize(sizeDp)
                        .gravity(Alignment.End)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(
                    Modifier.preferredSize(sizeDp * 2)
                        .gravity(Alignment.End)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(PxPosition(0f, 0f), childPosition[0])
        assertEquals(PxPosition(0f, size.toPx().value), childPosition[1])
    }

    @Test
    fun testColumn_Rtl_gravityRelativeToSiblings() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Column(Modifier.fillMaxWidth().rtl) {
                Container(
                    Modifier.preferredSize(sizeDp)
                        .alignWithSiblings { it.width }
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(
                    Modifier.preferredSize(sizeDp)
                        .alignWithSiblings { it.width / 2 }
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0f, 0f))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition((rootWidth - size.toPx()).value, 0f), childPosition[0])
        assertEquals(
            PxPosition((rootWidth - size.toPx() * 1.5f).round().toPx().value, size.toPx().value),
            childPosition[1]
        )
    }

    // endregion
}

private val TestHorizontalLine = HorizontalAlignmentLine(::min)
private val TestVerticalLine = VerticalAlignmentLine(::min)

@Composable
private fun BaselineTestLayout(
    width: Dp,
    height: Dp,
    baseline: Dp,
    modifier: Modifier,
    children: @Composable () -> Unit
) {
    Layout(children = children, modifier = modifier, measureBlock = { _, constraints, _ ->
        val widthPx = max(width.toIntPx(), constraints.minWidth)
        val heightPx = max(height.toIntPx(), constraints.minHeight)
        layout(
            widthPx, heightPx,
            mapOf(TestHorizontalLine to baseline.toIntPx(), TestVerticalLine to baseline.toIntPx())
        ) {}
    })
}

// Center composable function is deprected whereas FlexTest tests heavily depend on it.
@Composable
private fun Center(children: @Composable () -> Unit) {
    Layout(children) { measurables, constraints, _ ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx))

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.maxWidth.isFinite()) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.maxHeight.isFinite()) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = Alignment.Center.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}