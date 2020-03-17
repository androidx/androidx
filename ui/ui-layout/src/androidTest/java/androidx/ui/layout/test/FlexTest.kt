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
import androidx.ui.core.FirstBaseline
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.WithConstraints
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.layout.Align
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Arrangement
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutDirectionModifier
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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

@SmallTest
@RunWith(JUnit4::class)
class FlexTest : LayoutTest() {
    // region Size and position tests for Row and Column
    @Test
    fun testRow() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).round(), (sizeDp.toPx() * 2).round()),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(size.toPx(), 0.px), childPosition[1])
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
                    Container(LayoutWeight(1f) + onPositioned { coordinates ->
                        childSize[0] = coordinates.size
                        childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        drawLatch.countDown()
                    }, width = width, height = height) {
                    }

                    Container(LayoutWeight(2f) + onPositioned { coordinates ->
                        childSize[1] = coordinates.size
                        childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        drawLatch.countDown()
                    }, width = width, height = height) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition((rootWidth / 3).round().toPx(), 0.px), childPosition[1])
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
                        LayoutWeight(1f, fill = false) + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height
                        ) {
                    }

                    Container(
                        LayoutWeight(2f, fill = false) + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height * 2
                        ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(IntPxSize(childrenWidth, childrenHeight * 2), childSize[1])
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(childrenWidth.toPx(), 0.px), childPosition[1])
    }

    @Test
    fun testColumn() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).round(), (sizeDp.toPx() * 2).round()),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
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
                        LayoutWeight(1f) + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        LayoutWeight(2f) + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootHeight = root.height.px

        assertEquals(
            IntPxSize(childrenWidth, (rootHeight / 3).round()), childSize[0]
        )
        assertEquals(
            IntPxSize(childrenWidth, (rootHeight * 2 / 3).round()), childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, (rootHeight / 3).round().toPx()), childPosition[1])
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
                        LayoutWeight(1f, fill = false) + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height
                    ) {
                    }
                    Container(
                        LayoutWeight(2f, fill = false) + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(
            IntPxSize(childrenWidth, childrenHeight), childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, childrenHeight.toPx()), childPosition[1])
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
                LayoutAlign.TopStart + LayoutPadding(start = leftPadding.toDp()) +
                        LayoutWidth.Max(expectedRowWidth.toDp()) +
                        onPositioned { coordinates -> rowWidth = coordinates.size.width }
            ) {
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            width[0] = coordinates.size.width
                            x[0] = coordinates.globalPosition.x
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            width[1] = coordinates.size.width
                            x[1] = coordinates.globalPosition.x
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
                LayoutAlign.TopStart + LayoutPadding(start = leftPadding.toDp()) +
                        LayoutWidth.Max(expectedRowWidth.toDp()) +
                        onPositioned { coordinates -> rowWidth = coordinates.size.width }
            ) {
                Container(LayoutWeight(2f) +
                        onPositioned { coordinates ->
                            width[0] = coordinates.size.width
                            x[0] = coordinates.globalPosition.x
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(2f) +
                        onPositioned { coordinates ->
                            width[1] = coordinates.size.width
                            x[1] = coordinates.globalPosition.x
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(3f) +
                        onPositioned { coordinates ->
                            width[2] = coordinates.size.width
                            x[2] = coordinates.globalPosition.x
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
                LayoutAlign.TopStart + LayoutPadding(top = topPadding.toDp()) +
                        LayoutHeight.Max(expectedColumnHeight.toDp()) +
                        onPositioned { coordinates -> columnHeight = coordinates.size.height }
            ) {
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            height[0] = coordinates.size.height
                            y[0] = coordinates.globalPosition.y
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            height[1] = coordinates.size.height
                            y[1] = coordinates.globalPosition.y
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            height[2] = coordinates.size.height
                            y[2] = coordinates.globalPosition.y
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
                LayoutAlign.TopStart + LayoutPadding(top = topPadding.toDp()) +
                        LayoutHeight.Max(expectedColumnHeight.toDp()) +
                        onPositioned { coordinates -> columnHeight = coordinates.size.height }
            ) {
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            height[0] = coordinates.size.height
                            y[0] = coordinates.globalPosition.y
                            latch.countDown()
                        }
                ) {
                }
                Container(LayoutWeight(1f) +
                        onPositioned { coordinates ->
                            height[1] = coordinates.size.height
                            y[1] = coordinates.globalPosition.y
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
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Align(Alignment.CenterStart) {
                Row {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutHeight.Fill + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = LayoutHeight.Fill + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(size, root.height.ipx), childSize[0])
        assertEquals(
            IntPxSize((sizeDp.toPx() * 2).round(), root.height.ipx),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(size.toPx(), 0.px), childPosition[1])
    }

    @Test
    fun testRow_withGravityModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Align(Alignment.TopStart) {
                Row(LayoutHeight.Fill) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.Top + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.Center + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.Bottom + onPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootHeight = root.height.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(size.toPx(), ((rootHeight - size.toPx()) / 2).round().toPx()),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(PxPosition(size.toPx() * 2, rootHeight - size.toPx()), childPosition[2])
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
            Align(Alignment.TopStart) {
                Row(LayoutHeight.Fill) {
                    BaselineTestLayout(
                        baseline = baseline1Dp,
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings(TestHorizontalLine) +
                                onPositioned { coordinates ->
                                    childSize[0] = coordinates.size
                                    childPosition[0] = coordinates.globalPosition
                                    drawLatch.countDown()
                                }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.height * 0.5 } +
                                onPositioned { coordinates ->
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
                        modifier = LayoutGravity.RelativeToSiblings(TestHorizontalLine) +
                                onPositioned { coordinates ->
                                    childSize[2] = coordinates.size
                                    childPosition[2] = coordinates.globalPosition
                                    drawLatch.countDown()
                                }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.height * 0.75 } +
                                onPositioned { coordinates ->
                                    childSize[3] = coordinates.size
                                    childPosition[3] = coordinates.globalPosition
                                    drawLatch.countDown()
                        }
                        ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(size.toPx(), (baseline1 - (size.toPx() / 2).round()).toPx()),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition(size.toPx() * 2, (baseline1 - baseline2).toPx()),
            childPosition[2]
        )

        assertEquals(IntPxSize(size, size), childSize[3])
        assertEquals(
            PxPosition(size.toPx() * 3, 0.px),
            childPosition[3]
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
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Align(Alignment.TopCenter) {
                Column {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutWidth.Fill + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = LayoutWidth.Fill + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(root.width.ipx, size), childSize[0])
        assertEquals(
            IntPxSize(root.width.ipx, (sizeDp * 2).toIntPx()),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
    }

    @Test
    fun testColumn_withGravityModifier() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Align(Alignment.TopCenter) {
                Column(LayoutWidth.Fill) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.Start + onPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.Center + onPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.End + onPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        }
                        ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(
            PxPosition(((rootWidth - size.toPx()) / 2).round().toPx(), size.toPx()),
            childPosition[1]
        )

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(PxPosition(rootWidth - size.toPx(), size.toPx() * 2), childPosition[2])
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
            Align(Alignment.TopStart) {
                Column(LayoutWidth.Fill) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.width } +
                                onPositioned { coordinates ->
                                    childSize[0] = coordinates.size
                                    childPosition[0] = coordinates.globalPosition
                                    drawLatch.countDown()
                                }
                    ) {
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { 0.ipx } +
                                onPositioned { coordinates ->
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
                        modifier = LayoutGravity.RelativeToSiblings(TestVerticalLine) +
                                onPositioned { coordinates ->
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
                        modifier = LayoutGravity.RelativeToSiblings(TestVerticalLine) +
                                onPositioned { coordinates ->
                                    childSize[3] = coordinates.size
                                    childPosition[3] = coordinates.globalPosition
                                    drawLatch.countDown()
                                }
                    ) {
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), childSize[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])

        assertEquals(IntPxSize(size, size), childSize[1])
        assertEquals(PxPosition(size.toPx(), size.toPx()), childPosition[1])

        assertEquals(IntPxSize(size, size), childSize[2])
        assertEquals(
            PxPosition((size - firstBaseline1Dp.toIntPx()).toPx(), size.toPx() * 2),
            childPosition[2]
        )

        assertEquals(IntPxSize(size, size), childSize[3])
        assertEquals(
            PxPosition((size - firstBaseline2Dp.toIntPx()).toPx(), size.toPx() * 3),
            childPosition[3]
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
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Row(onPositioned { coordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Row(onPositioned { coordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Container(
                        LayoutWeight(1f),
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
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Row(LayoutHeight.Fill + onPositioned { coordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Row(onPositioned { coordinates ->
                    rowSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(LayoutWidth.Fill + onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            LayoutWeight(1f),
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
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(LayoutHeight.Fill + onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Row(onPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            modifier = LayoutWeight(1f) + onPositioned { coordinates ->
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
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                        WithConstraints { constraints, _ ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        WithConstraints { constraints, _ ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        Layout({}, LayoutWeight(1f)) { _, constraints, _ ->
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
                        WithConstraints { constraints, _ ->
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints { constraints, _ ->
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
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Column(onPositioned { coordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Column(onPositioned { coordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Container(
                        LayoutWeight(1f),
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
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Column(LayoutWidth.Fill + onPositioned { coordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                Column(onPositioned { coordinates ->
                    columnSize = coordinates.size
                    drawLatch.countDown()
                }) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(LayoutHeight.Fill + onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(
                            LayoutWeight(1f),
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
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(LayoutWidth.Fill + onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                    Column(LayoutHeight.Max(Dp.Infinity) + onPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }) {
                        Container(LayoutWeight(1f) + onPositioned { coordinates ->
                            expandedChildSize = coordinates.size
                            drawLatch.countDown()
                        }, width = sizeDp, height = sizeDp) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
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
                            WithConstraints { constraints, _ ->
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(0.ipx, noWeightChildHeight.toIntPx(), mapOf())
                            }
                            WithConstraints { constraints, _ ->
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(0.ipx, noWeightChildHeight.toIntPx(), mapOf())
                            }
                            Layout(emptyContent(), LayoutWeight(1f)) { _, constraints, _ ->
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
                        WithConstraints { constraints, _ ->
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints { constraints, _ ->
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
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition(size.toPx() * 2, 0.px), childPosition[2])
    }

    @Test
    fun testRow_withEndArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.End) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(root.width.px - size.toPx() * 3, 0.px), childPosition[0])
        assertEquals(PxPosition(root.width.px - size.toPx() * 2, 0.px), childPosition[1])
        assertEquals(PxPosition(root.width.px - size.toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withCenterArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val extraSpace = root.width.px.round() - size * 3
        assertEquals(PxPosition((extraSpace / 2).toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((extraSpace / 2).toPx() + size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((extraSpace / 2).toPx() + size.toPx() * 2, 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(gap.round().toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((size.toPx() + gap * 2).round().toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((size.toPx() * 2 + gap * 3).round().toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition((gap + size.toPx()).round().toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((gap * 2 + size.toPx() * 2).round().toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    LayoutWidth.Fill + onPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    arrangement = Arrangement.SpaceAround
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px.round() - size * 3) / 3
        assertEquals(PxPosition((gap / 2).toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((gap * 3 / 2).toPx() + size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((gap * 5 / 2).toPx() + size.toPx() * 2, 0.px), childPosition[2])
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
                Row(LayoutWidth.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = customHorizontalArrangement) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val step = (root.width.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition((step + size.toPx()).round().toPx(), 0.px), childPosition[1])
        assertEquals(
            PxPosition((step * 3 + size.toPx() * 2).round().toPx(), 0.px),
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
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, size.toPx() * 2), childPosition[2])
    }

    @Test
    fun testColumn_withEndArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.Bottom) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, root.height.px - size.toPx() * 3), childPosition[0])
        assertEquals(PxPosition(0.px, root.height.px - size.toPx() * 2), childPosition[1])
        assertEquals(PxPosition(0.px, root.height.px - size.toPx()), childPosition[2])
    }

    @Test
    fun testColumn_withCenterArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val extraSpace = root.height.px.round() - size * 3
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx()), childPosition[0])
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx() + size.toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx() + size.toPx() * 2), childPosition[2])
    }

    @Test
    fun testColumn_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(0.px, gap.round().toPx()), childPosition[0])
        assertEquals(PxPosition(0.px, (size.toPx() + gap * 2).round().toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (size.toPx() * 2 + gap * 3).round().toPx()), childPosition[2])
    }

    private fun calculateChildPositions(
        childPosition: Array<PxPosition>,
        parentLayoutCoordinates: LayoutCoordinates?,
        childLayoutCoordinates: Array<LayoutCoordinates?>
    ) {
        for (i in childPosition.indices) {
            childPosition[i] = parentLayoutCoordinates!!
                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
        }
    }

    @Test
    fun testColumn_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, (gap + size.toPx()).round().toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (gap * 2 + size.toPx() * 2).round().toPx()), childPosition[2])
    }

    @Test
    fun testColumn_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = Arrangement.SpaceAround) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0.px, (gap / 2).round().toPx()), childPosition[0])
        assertEquals(
            PxPosition(0.px, ((gap * 3 / 2) + size.toPx()).round().toPx()),
            childPosition[1]
        )
        assertEquals(
            PxPosition(0.px, ((gap * 5 / 2) + size.toPx() * 2).round().toPx()),
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
                Column(LayoutHeight.Fill + onPositioned { coordinates ->
                    parentLayoutCoordinates = coordinates
                    drawLatch.countDown()
                }, arrangement = customVerticalArrangement) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = onPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val step = (root.height.px - size.toPx() * 3) / 3
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, (step + size.toPx()).round().toPx()), childPosition[1])
        assertEquals(
            PxPosition(0.px, (step * 3 + size.toPx() * 2).round().toPx()),
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
                        Spacer(LayoutSize(width = childSizeDp, height = childSizeDp) +
                                onPositioned { coordinates ->
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
                        Spacer(LayoutSize(width = childSizeDp, height = childSizeDp) +
                                onPositioned { coordinates ->
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
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(LayoutWidth.Fill) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row {
                Container(LayoutAspectRatio(2f) + LayoutGravity.Top, children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Center,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row {
                Container(
                    LayoutAspectRatio(2f) + LayoutGravity.RelativeToSiblings(FirstBaseline),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.RelativeToSiblings { it.width },
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.Start) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.Center) {
                Container(LayoutGravity.Center + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Center,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.End) {
                Container(LayoutGravity.Bottom + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Bottom,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceAround) {
                Container(LayoutHeight.Fill + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutHeight.Fill,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceBetween) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
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
                    LayoutWeight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutWeight(2f),
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(2f) + LayoutWeight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutWeight(3f) + LayoutGravity.Top,
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutWeight(2f) + LayoutGravity.Center,
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(2f) + LayoutWeight(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutGravity.Bottom,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(arrangement = Arrangement.Start) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutWeight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutWeight(2f),
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(2f) + LayoutWeight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row(arrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutWeight(3f) + LayoutGravity.Center,
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutWeight(2f) + LayoutGravity.Center,
                    children = emptyContent()
                )
                Container(
                    LayoutAspectRatio(2f) + LayoutWeight(2f) + LayoutGravity.Center,
                    children =
                    emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutGravity.Center,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(arrangement = Arrangement.End) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutWeight(3f) + LayoutGravity.Bottom,
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutWeight(2f) + LayoutGravity.Bottom,
                    children = emptyContent()
                )
                Container(
                    LayoutAspectRatio(2f) + LayoutWeight(2f) + LayoutGravity.Bottom,
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutGravity.Bottom,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceAround) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutWeight(3f) + LayoutHeight.Fill,
                    children = emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutWeight(2f) + LayoutHeight.Fill,
                    children = emptyContent()
                )
                Container(
                    LayoutAspectRatio(2f) + LayoutWeight(2f) + LayoutHeight.Fill,
                    children =
                    emptyContent()
                )
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutHeight.Fill,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutWeight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutWeight(2f),
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(2f) + LayoutWeight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), children = emptyContent())
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutWeight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutWeight(2f),
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(2f) + LayoutWeight(2f), children = emptyContent())
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
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column {
                Container(LayoutAspectRatio(2f) + LayoutGravity.Start, children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.End,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column {
                Container(
                    LayoutAspectRatio(2f) + LayoutGravity.RelativeToSiblings { 0.ipx },
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.RelativeToSiblings(TestVerticalLine),
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(LayoutHeight.Fill) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.Top) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.Center) {
                Container(LayoutGravity.Center + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.Bottom) {
                Container(LayoutGravity.End + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.End,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceAround) {
                Container(LayoutWidth.Fill + LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutWidth.Fill,
                    children = emptyContent()
                )
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceBetween) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), children = emptyContent())
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceEvenly) {
                Container(LayoutAspectRatio(2f), children = emptyContent())
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
                    LayoutWeight(3f),
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(40.dp, 30.dp),
                    LayoutWeight(2f),
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(0.5f) + LayoutWeight(2f), children = emptyContent())
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), children = emptyContent())
            }
        }, @Composable {
            Column {
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 20.dp),
                    LayoutWeight(3f) + LayoutGravity.Start,
                    children = emptyContent()
                )
                ConstrainedBox(
                    DpConstraints.fixed(40.dp, 30.dp),
                    LayoutWeight(2f) + LayoutGravity.Center,
                    children = emptyContent()
                )
                Container(LayoutAspectRatio(0.5f) + LayoutWeight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutGravity.End) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.Top) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutWeight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutWeight(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutWeight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutWeight(3f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutWeight(2f) + LayoutGravity.Center
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutWeight(2f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutGravity.Center
                ) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.Bottom) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutWeight(3f) + LayoutGravity.End
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutWeight(2f) + LayoutGravity.End
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutWeight(2f) + LayoutGravity.End
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutGravity.End
                ) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.SpaceAround) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutWeight(3f) + LayoutWidth.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutWeight(2f) + LayoutWidth.Fill
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutWeight(2f) + LayoutWidth.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutWidth.Fill
                ) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutWeight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutWeight(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutWeight(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutWeight(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutWeight(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutWeight(2f)) { }
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
            Align(Alignment.TopStart) {
                Column(LayoutHeight(columnHeight.toDp())) {
                    Container(
                        LayoutWeight(2f) + LayoutWeight(1f) +
                                onPositioned { coordinates ->
                                    containerHeight.value = coordinates.size.height
                                    positionedLatch.countDown()
                                },
                        children = emptyContent()
                    )
                    Container(LayoutWeight(1f), children = emptyContent())
                }
            }
        }

        positionedLatch.await(1, TimeUnit.SECONDS)

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
                    modifier = LayoutGravity.RelativeToSiblings { it.height },
                    width = size,
                    height = size,
                    children = emptyContent()
                )
                Container(
                    modifier = LayoutGravity.RelativeToSiblings { 0.ipx } +
                            LayoutGravity.RelativeToSiblings { it.height * 0.5 } +
                            onPositioned { coordinates ->
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

        positionedLatch.await(1, TimeUnit.SECONDS)

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
            Row(LayoutWidth.Fill + LayoutDirectionModifier.Rtl) {
                Container(LayoutSize(sizeDp, sizeDp) + onPositioned { coordinates ->
                    childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }

                Container(
                    LayoutSize(sizeDp * 2, sizeDp * 2) + onPositioned { coordinates ->
                        childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        drawLatch.countDown()
                    }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition(rootWidth - size.toPx(), 0.px), childPosition[0])
        assertEquals(
            PxPosition(rootWidth - (sizeDp.toPx() * 3).round().toPx(), 0.px),
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
                LayoutWidth.Fill + LayoutDirectionModifier.Rtl,
                arrangement = Arrangement.End
            ) {
                Container(LayoutSize(sizeDp, sizeDp) + onPositioned { coordinates ->
                    childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }

                Container(LayoutSize(sizeDp * 2, sizeDp * 2) + onPositioned { coordinates ->
                    childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(PxPosition((sizeDp.toPx() * 2).round().toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[1])
    }

    @Test
    fun testRow_Rtl_customArrangement() = with(density) {
        val sizeDp = 35.dp

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Row(
                LayoutWidth.Fill + LayoutDirectionModifier.Rtl,
                arrangement = customHorizontalArrangement
            ) {
                Container(width = sizeDp, height = sizeDp, modifier = onPositioned { coordinates ->
                    childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }

                Container(
                    width = (sizeDp * 2),
                    height = (sizeDp * 2),
                    modifier = onPositioned { coordinates ->
                        childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        drawLatch.countDown()
                    }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition((sizeDp.toPx() * 2).round().toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, 0.px), childPosition[1])
    }

    @Test
    fun testColumn_Rtl_gravityStart() = with(density) {
        val sizeDp = 35.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Column(LayoutWidth.Fill + LayoutDirectionModifier.Rtl) {
                Container(LayoutSize(sizeDp, sizeDp) + onPositioned { coordinates ->
                    childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }

                Container(LayoutSize(sizeDp * 2, sizeDp * 2) + onPositioned { coordinates ->
                    childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                    drawLatch.countDown()
                }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition(rootWidth - size.toPx(), 0.px), childPosition[0])
        assertEquals(
            PxPosition((rootWidth - (sizeDp * 2).toPx()).round().toPx(), size.toPx()),
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
            Column(LayoutWidth.Fill + LayoutDirectionModifier.Rtl) {
                Container(LayoutSize(sizeDp, sizeDp) + LayoutGravity.End +
                        onPositioned { coordinates ->
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(
                    LayoutSize(sizeDp * 2, sizeDp * 2) + LayoutGravity.End +
                    onPositioned { coordinates ->
                        childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                        drawLatch.countDown()
                    }
                    ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
    }

    @Test
    fun testColumn_Rtl_gravityRelativeToSiblings() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(PxPosition.Origin, PxPosition.Origin)
        show {
            Column(LayoutWidth.Fill + LayoutDirectionModifier.Rtl) {
                Container(LayoutSize(sizeDp, sizeDp) +
                        LayoutGravity.RelativeToSiblings { it.width } +
                        onPositioned { coordinates ->
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(LayoutSize(sizeDp, sizeDp) +
                        LayoutGravity.RelativeToSiblings { it.width / 2 } +
                        onPositioned { coordinates ->
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.px

        assertEquals(PxPosition(rootWidth - size.toPx(), 0.px), childPosition[0])
        assertEquals(
            PxPosition((rootWidth - size.toPx() * 1.5f).round().toPx(), size.toPx()),
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
    children: @Composable() () -> Unit
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