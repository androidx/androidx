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
import androidx.ui.core.FirstBaseline
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.WithConstraints
import androidx.ui.core.globalPosition
import androidx.ui.layout.Align
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Wrap
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
import androidx.ui.unit.withDensity
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
    fun testRow() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Container(alignment = Alignment.TopLeft) {
                Row {
                    Container(width = sizeDp, height = sizeDp) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(width = (sizeDp * 2), height = (sizeDp * 2)) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testRow_withFlexibleChildren() = withDensity(density) {
        val width = 50.dp
        val height = 80.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopLeft) {
                Row {
                    Container(LayoutFlexible(1f), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(LayoutFlexible(2f), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testRow_withLooselyFlexibleChildren() = withDensity(density) {
        val width = 50.dp
        val childrenWidth = width.toIntPx()
        val height = 80.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopLeft) {
                Row {
                    Container(
                        LayoutFlexible(1f, tight = false),
                        width = width,
                        height = height
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(
                        LayoutFlexible(2f, tight = false),
                        width = width,
                        height = height * 2
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testColumn() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Container(alignment = Alignment.TopLeft) {
                Column {
                    Container(width = sizeDp, height = sizeDp) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }
                    Container(width = (sizeDp * 2), height = (sizeDp * 2)) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testColumn_withFlexibleChildren() = withDensity(density) {
        val width = 80.dp
        val childrenWidth = width.toIntPx()
        val height = 50.dp

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopLeft) {
                Column {
                    Container(LayoutFlexible(1f), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(LayoutFlexible(2f), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testColumn_withLooselyFlexibleChildren() = withDensity(density) {
        val width = 80.dp
        val childrenWidth = width.toIntPx()
        val height = 50.dp
        val childrenHeight = height.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntPxSize>(2)
        val childPosition = arrayOfNulls<PxPosition>(2)
        show {
            Container(alignment = Alignment.TopLeft) {
                Column {
                    Container(LayoutFlexible(1f, tight = false), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }
                    Container(LayoutFlexible(2f, tight = false), width = width, height = height) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testRow_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = withDensity(density) {
        val expectedRowWidth = 11.ipx
        val leftPadding = 1.px
        var rowWidth = 0.ipx
        val width = Array(2) { 0.ipx }
        val x = Array(2) { 0.px }
        val latch = CountDownLatch(2)
        show {
            Row(
                LayoutAlign.TopLeft + LayoutPadding(left = leftPadding.toDp()) +
                        LayoutWidth.Max(expectedRowWidth.toDp())
            ) {
                OnPositioned { coordinates -> rowWidth = coordinates.size.width }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        width[0] = coordinates.size.width
                        x[0] = coordinates.globalPosition.x
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        width[1] = coordinates.size.width
                        x[1] = coordinates.globalPosition.x
                        latch.countDown()
                    }
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
    fun testRow_isNotLargerThanItsChildren_becauseOfRoundings() = withDensity(density) {
        val expectedRowWidth = 8.ipx
        val leftPadding = 1.px
        var rowWidth = 0.ipx
        val width = Array(3) { 0.ipx }
        val x = Array(3) { 0.px }
        val latch = CountDownLatch(3)
        show {
            Row(
                LayoutAlign.TopLeft + LayoutPadding(left = leftPadding.toDp()) +
                        LayoutWidth.Max(expectedRowWidth.toDp())
            ) {
                OnPositioned { coordinates -> rowWidth = coordinates.size.width }
                Container(LayoutFlexible(2f)) {
                    OnPositioned { coordinates ->
                        width[0] = coordinates.size.width
                        x[0] = coordinates.globalPosition.x
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(2f)) {
                    OnPositioned { coordinates ->
                        width[1] = coordinates.size.width
                        x[1] = coordinates.globalPosition.x
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(3f)) {
                    OnPositioned { coordinates ->
                        width[2] = coordinates.size.width
                        x[2] = coordinates.globalPosition.x
                        latch.countDown()
                    }
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
    fun testColumn_isNotLargetThanItsChildren_becauseOfRoundings() = withDensity(density) {
        val expectedColumnHeight = 8.ipx
        val topPadding = 1.px
        var columnHeight = 0.ipx
        val height = Array(3) { 0.ipx }
        val y = Array(3) { 0.px }
        val latch = CountDownLatch(3)
        show {
            Column(
                LayoutAlign.TopLeft + LayoutPadding(top = topPadding.toDp()) +
                        LayoutHeight.Max(expectedColumnHeight.toDp())
            ) {
                OnPositioned { coordinates -> columnHeight = coordinates.size.height }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        height[0] = coordinates.size.height
                        y[0] = coordinates.globalPosition.y
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        height[1] = coordinates.size.height
                        y[1] = coordinates.globalPosition.y
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        height[2] = coordinates.size.height
                        y[2] = coordinates.globalPosition.y
                        latch.countDown()
                    }
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
    fun testColumn_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = withDensity(density) {
        val expectedColumnHeight = 11.ipx
        val topPadding = 1.px
        var columnHeight = 0.ipx
        val height = Array(2) { 0.ipx }
        val y = Array(2) { 0.px }
        val latch = CountDownLatch(2)
        show {
            Column(
                LayoutAlign.TopLeft + LayoutPadding(top = topPadding.toDp()) +
                        LayoutHeight.Max(expectedColumnHeight.toDp())
            ) {
                OnPositioned { coordinates -> columnHeight = coordinates.size.height }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        height[0] = coordinates.size.height
                        y[0] = coordinates.globalPosition.y
                        latch.countDown()
                    }
                }
                Container(LayoutFlexible(1f)) {
                    OnPositioned { coordinates ->
                        height[1] = coordinates.size.height
                        y[1] = coordinates.globalPosition.y
                        latch.countDown()
                    }
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
    fun testRow_withStretchCrossAxisAlignment() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Align(Alignment.CenterLeft) {
                Row {
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutHeight.Fill) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = LayoutHeight.Fill
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testRow_withGravityModifier() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Align(Alignment.TopLeft) {
                Row(LayoutHeight.Fill) {
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.Top) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.Center) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.Bottom) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
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
    fun testRow_withRelativeToSiblingsModifier() = withDensity(density) {
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
            Align(Alignment.TopLeft) {
                Row(LayoutHeight.Fill) {
                    BaselineTestLayout(
                        baseline = baseline1Dp,
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings(TestHorizontalLine)
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.height * 0.5 }
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    BaselineTestLayout(
                        baseline = baseline2Dp,
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings(TestHorizontalLine)
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.height * 0.75 }
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[3] = coordinates.size
                            childPosition[3] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
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
    fun testColumn_withStretchCrossAxisAlignment() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntPxSize(-1.ipx, -1.ipx), IntPxSize(-1.ipx, -1.ipx))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show {
            Align(Alignment.TopCenter) {
                Column {
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutWidth.Fill) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
                    }

                    Container(
                        width = (sizeDp * 2), height = (sizeDp * 2), modifier = LayoutWidth.Fill
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        })
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
    fun testColumn_withGravityModifier() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntPxSize>(3)
        val childPosition = arrayOfNulls<PxPosition>(3)
        show {
            Align(Alignment.TopCenter) {
                Column(LayoutWidth.Fill) {
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.Start) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.Center) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(width = sizeDp, height = sizeDp, modifier = LayoutGravity.End) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
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
    fun testColumn_withRelativeToSiblingsModifier() = withDensity(density) {
        val sizeDp = 40.dp
        val size = sizeDp.toIntPx()
        val firstBaseline1Dp = 20.dp
        val firstBaseline2Dp = 30.dp

        val drawLatch = CountDownLatch(4)
        val childSize = arrayOfNulls<IntPxSize>(4)
        val childPosition = arrayOfNulls<PxPosition>(4)
        show {
            Align(Alignment.TopLeft) {
                Column(LayoutWidth.Fill) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { it.width }
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = LayoutGravity.RelativeToSiblings { 0.ipx }
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    BaselineTestLayout(
                        width = sizeDp,
                        height = sizeDp,
                        baseline = firstBaseline1Dp,
                        modifier = LayoutGravity.RelativeToSiblings(TestVerticalLine)
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
                    }
                    BaselineTestLayout(
                        width = sizeDp,
                        height = sizeDp,
                        baseline = firstBaseline2Dp,
                        modifier = LayoutGravity.RelativeToSiblings(TestVerticalLine)
                    ) {
                        OnPositioned(onPositioned = { coordinates ->
                            childSize[3] = coordinates.size
                            childPosition[3] = coordinates.globalPosition
                            drawLatch.countDown()
                        })
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
    fun testRow_expandedWidth_withExpandedModifier() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(LayoutWidth.Fill) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testRow_wrappedWidth_withNoFlexibleChildren() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testRow_expandedWidth_withFlexibleChildren() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row {
                    Container(LayoutFlexible(1f), width = sizeDp, height = sizeDp) {}
                    Container(width = (sizeDp * 2), height = (sizeDp * 2)) {}

                    OnPositioned(onPositioned = { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testRow_withMaxCrossAxisSize() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row(LayoutHeight.Fill) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testRow_withMinCrossAxisSize() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                Row {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testRow_withExpandedModifier_respectsMaxWidthConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row(LayoutWidth.Fill) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testRow_withFlexibleChildren_respectsMaxWidthConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row {
                        Container(LayoutFlexible(1f), width = sizeDp, height = sizeDp) {}
                        Container(width = sizeDp * 2, height = sizeDp * 2) {}

                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testRow_withInflexibleChildren_respectsMinWidthConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val rowWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = rowWidthDp)) {
                    Row {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testRow_withMaxCrossAxisSize_respectsMaxHeightConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val rowHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = rowHeightDp)) {
                    Row(LayoutHeight.Fill) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testRow_withMinCrossAxisSize_respectsMinHeightConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val rowHeightDp = 150.dp

        val drawLatch = CountDownLatch(1)
        lateinit var rowSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = rowHeightDp)) {
                    Row {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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

    @Test @Ignore("Wrap is not supported when there are flexible children. " +
            "Should use maxWidth(.Infinity) modifier when it is available")
    fun testRow_withMinMainAxisSize() = withDensity(density) {
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
                    Row {
                        Container(modifier = LayoutFlexible(1f), width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                expandedChildSize = coordinates.size
                                drawLatch.countDown()
                            })
                        }
                        OnPositioned(onPositioned = { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testRow_measuresChildrenCorrectly_whenMeasuredWithInfiniteWidth() = withDensity(density) {
        val rowMinWidth = 100.dp
        val inflexibleChildWidth = 30.dp
        val latch = CountDownLatch(1)
        show {
            WithInfiniteConstraints {
                ConstrainedBox(DpConstraints(minWidth = rowMinWidth)) {
                    Row {
                        WithConstraints { constraints ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(inflexibleChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        WithConstraints { constraints ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(inflexibleChildWidth.toIntPx(), 0.ipx, mapOf())
                        }
                        Layout({}, LayoutFlexible(1f)) { _, constraints ->
                            assertEquals(
                                rowMinWidth.toIntPx() - inflexibleChildWidth.toIntPx() * 2,
                                constraints.minWidth
                            )
                            assertEquals(
                                rowMinWidth.toIntPx() - inflexibleChildWidth.toIntPx() * 2,
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
    fun testRow_measuresInflexibleChildrenCorrectly() = withDensity(density) {
        val availableWidth = 100.dp
        val childWidth = 50.dp
        val availableHeight = 200.dp
        val childHeight = 100.dp
        val latch = CountDownLatch(1)
        show {
            Wrap {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Row {
                        WithConstraints { constraints ->
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints { constraints ->
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
    fun testColumn_expandedHeight_withExpandedModifier() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(LayoutHeight.Fill) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testColumn_wrappedHeight_withNoFlexibleChildren() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testColumn_expandedHeight_withFlexibleChildren() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column {
                    Container(LayoutFlexible(1f), width = sizeDp, height = sizeDp) {}
                    Container(width = (sizeDp * 2), height = (sizeDp * 2)) {}

                    OnPositioned(onPositioned = { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testColumn_withMaxCrossAxisSize() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column(LayoutWidth.Fill) {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testColumn_withMinCrossAxisSize() = withDensity(density) {
        val sizeDp = 50.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                Column {
                    Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                    Spacer(LayoutSize(width = (sizeDp * 2), height = (sizeDp * 2)))

                    OnPositioned(onPositioned = { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    })
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
    fun testColumn_withExpandedModifier_respectsMaxHeightConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column(LayoutHeight.Fill) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testColumn_withFlexibleChildren_respectsMaxHeightConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column {
                        Container(LayoutFlexible(1f), width = sizeDp, height = sizeDp) {}
                        Container(width = sizeDp * 2, height = sizeDp * 2) {}

                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testColumn_withInflexibleChildren_respectsMinHeightConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val columnHeightDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = columnHeightDp)) {
                    Column {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testColumn_withMaxCrossAxisSize_respectsMaxWidthConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val columnWidthDp = 250.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = columnWidthDp)) {
                    Column(LayoutWidth.Fill) {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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
    fun testColumn_withMinCrossAxisSize_respectsMinWidthConstraint() = withDensity(density) {
        val sizeDp = 50.dp
        val columnWidthDp = 150.dp

        val drawLatch = CountDownLatch(1)
        lateinit var columnSize: IntPxSize
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = columnWidthDp)) {
                    Column {
                        Spacer(LayoutSize(width = sizeDp, height = sizeDp))
                        Spacer(LayoutSize(width = sizeDp * 2, height = sizeDp * 2))

                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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

    @Test @Ignore("Wrap is not supported when there are flexible children. " +
            "Should use maxHeight(IntPx.Infinity) modifier when it is available")
    fun testColumn_withMinMainAxisSize() = withDensity(density) {
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
                    Column(LayoutHeight.Max(Dp.Infinity)) {
                        Container(LayoutFlexible(1f), width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                expandedChildSize = coordinates.size
                                drawLatch.countDown()
                            })
                        }
                        OnPositioned(onPositioned = { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        })
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
        withDensity(density) {
        val columnMinHeight = 100.dp
        val inflexibleChildHeight = 30.dp
        val latch = CountDownLatch(1)
        show {
            WithInfiniteConstraints {
                ConstrainedBox(DpConstraints(minHeight = columnMinHeight)) {
                    Column {
                        WithConstraints { constraints ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(0.ipx, inflexibleChildHeight.toIntPx(), mapOf())
                        }
                        WithConstraints { constraints ->
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(0.ipx, inflexibleChildHeight.toIntPx(), mapOf())
                        }
                        Layout({}, LayoutFlexible(1f)) { _, constraints ->
                            assertEquals(
                                columnMinHeight.toIntPx() - inflexibleChildHeight.toIntPx() * 2,
                                constraints.minHeight
                            )
                            assertEquals(
                                columnMinHeight.toIntPx() - inflexibleChildHeight.toIntPx() * 2,
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
    fun testColumn_measuresInflexibleChildrenCorrectly() = withDensity(density) {
        val availableWidth = 100.dp
        val childWidth = 50.dp
        val availableHeight = 200.dp
        val childHeight = 100.dp
        val latch = CountDownLatch(1)
        show {
            Wrap {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Column {
                        WithConstraints { constraints ->
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.toIntPx(),
                                    maxHeight = availableHeight.toIntPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(childWidth.toIntPx(), childHeight.toIntPx(), mapOf())
                        }
                        WithConstraints { constraints ->
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
    fun testRow_withBeginArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition(size.toPx() * 2, 0.px), childPosition[2])
    }

    @Test
    fun testRow_withEndArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement.End) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(root.width.px - size.toPx() * 3, 0.px), childPosition[0])
        assertEquals(PxPosition(root.width.px - size.toPx() * 2, 0.px), childPosition[1])
        assertEquals(PxPosition(root.width.px - size.toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withCenterArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val extraSpace = root.width.px.round() - size * 3
        assertEquals(PxPosition((extraSpace / 2).toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((extraSpace / 2).toPx() + size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((extraSpace / 2).toPx() + size.toPx() * 2, 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceEvenlyArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(gap.round().toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((size.toPx() + gap * 2).round().toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((size.toPx() * 2 + gap * 3).round().toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceBetweenArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition((gap + size.toPx()).round().toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((gap * 2 + size.toPx() * 2).round().toPx(), 0.px), childPosition[2])
    }

    @Test
    fun testRow_withSpaceAroundArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceAround) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.width.px.round() - size * 3) / 3
        assertEquals(PxPosition((gap / 2).toPx(), 0.px), childPosition[0])
        assertEquals(PxPosition((gap * 3 / 2).toPx() + size.toPx(), 0.px), childPosition[1])
        assertEquals(PxPosition((gap * 5 / 2).toPx() + size.toPx() * 2, 0.px), childPosition[2])
    }

    @Test
    fun testRow_withCustomArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childPosition = arrayOf(
            PxPosition.Origin, PxPosition.Origin, PxPosition.Origin
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Row(LayoutWidth.Fill, arrangement = Arrangement(customArrangement)) {
                    for (i in childPosition.indices) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in childPosition.indices) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

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
    fun testColumn_withStartArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, size.toPx() * 2), childPosition[2])
    }

    @Test
    fun testColumn_withEndArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement.End) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(PxPosition(0.px, root.height.px - size.toPx() * 3), childPosition[0])
        assertEquals(PxPosition(0.px, root.height.px - size.toPx() * 2), childPosition[1])
        assertEquals(PxPosition(0.px, root.height.px - size.toPx()), childPosition[2])
    }

    @Test
    fun testColumn_withCenterArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement.Center) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val extraSpace = root.height.px.round() - size * 3
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx()), childPosition[0])
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx() + size.toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (extraSpace / 2).toPx() + size.toPx() * 2), childPosition[2])
    }

    @Test
    fun testColumn_withSpaceEvenlyArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 4
        assertEquals(PxPosition(0.px, gap.round().toPx()), childPosition[0])
        assertEquals(PxPosition(0.px, (size.toPx() + gap * 2).round().toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (size.toPx() * 2 + gap * 3).round().toPx()), childPosition[2])
    }

    @Test
    fun testColumn_withSpaceBetweenArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val gap = (root.height.px - size.toPx() * 3) / 2
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, (gap + size.toPx()).round().toPx()), childPosition[1])
        assertEquals(PxPosition(0.px, (gap * 2 + size.toPx() * 2).round().toPx()), childPosition[2])
    }

    @Test
    fun testColumn_withSpaceAroundArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceAround) {
                    for (i in 0 until childPosition.size) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in 0 until childPosition.size) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

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
    fun testColumn_withCustomArrangement() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childPosition = arrayOf(
            PxPosition.Origin, PxPosition.Origin, PxPosition.Origin
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        show {
            Center {
                Column(LayoutHeight.Fill, arrangement = Arrangement(customArrangement)) {
                    for (i in childPosition.indices) {
                        Container(width = sizeDp, height = sizeDp) {
                            OnPositioned(onPositioned = { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            })
                        }
                    }
                    OnPositioned(onPositioned = { coordinates ->
                        for (i in childPosition.indices) {
                            childPosition[i] = coordinates
                                .childToLocal(childLayoutCoordinates[i]!!, PxPosition(0.px, 0.px))
                        }
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

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
    fun testRow_doesNotUseMinConstraintsOnChildren() = withDensity(density) {
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
                        OnChildPositioned(onPositioned = { coordinates ->
                            containerSize.value = coordinates.size
                            layoutLatch.countDown()
                        }) {
                            Spacer(LayoutSize(width = childSizeDp, height = childSizeDp))
                        }
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(childSize, childSize), containerSize.value)
    }

    @Test
    fun testColumn_doesNotUseMinConstraintsOnChildren() = withDensity(density) {
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
                        OnChildPositioned(onPositioned = { coordinates ->
                            containerSize.value = coordinates.size
                            layoutLatch.countDown()
                        }) {
                            Spacer(LayoutSize(width = childSizeDp, height = childSizeDp))
                        }
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
    fun testRow_withInflexibleChildren_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Row {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Row {
                Container(LayoutAspectRatio(2f) + LayoutGravity.Top) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Center) { }
            }
        }, @Composable {
            Row {
                Container(
                    LayoutAspectRatio(2f) + LayoutGravity.RelativeToSiblings(FirstBaseline)
                ) { }
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.RelativeToSiblings { it.width }
                ) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.Begin) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.Center) {
                Container(LayoutGravity.Center + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Center) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.End) {
                Container(LayoutGravity.Bottom + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.Bottom) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceAround) {
                Container(LayoutHeight.Fill + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp),
                    LayoutHeight.Fill
                ) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceBetween) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
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
    fun testRow_withFlexibleChildren_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Row {
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 40.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(2f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp)) { }
            }
        }, @Composable {
            Row {
                ConstrainedBox(
                    DpConstraints.fixed(20.dp, 30.dp),
                    LayoutFlexible(3f) + LayoutGravity.Top
                ) { }
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 40.dp),
                    LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                Container(LayoutAspectRatio(2f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp),
                    LayoutGravity.Bottom) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.Begin) {
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 40.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(2f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp)) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutFlexible(3f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                Container(
                    LayoutAspectRatio(2f) + LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutGravity.Center
                ) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.End) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutFlexible(3f) + LayoutGravity.Bottom
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutFlexible(2f) + LayoutGravity.Bottom
                ) { }
                Container(
                    LayoutAspectRatio(2f) + LayoutFlexible(2f) + LayoutGravity.Bottom
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutGravity.Bottom
                ) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceAround) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutFlexible(3f) + LayoutHeight.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 40.dp),
                    modifier = LayoutFlexible(2f) + LayoutHeight.Fill
                ) { }
                Container(
                    LayoutAspectRatio(2f) + LayoutFlexible(2f) + LayoutHeight.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(20.dp, 30.dp),
                    modifier = LayoutHeight.Fill
                ) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 40.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(2f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp)) { }
            }
        }, @Composable {
            Row(arrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 40.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(2f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(20.dp, 30.dp)) { }
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
    fun testColumn_withInflexibleChildren_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Column {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Column {
                Container(LayoutAspectRatio(2f) + LayoutGravity.Start) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), LayoutGravity.End) { }
            }
        }, @Composable {
            Column {
                Container(LayoutAspectRatio(2f) + LayoutGravity.RelativeToSiblings { 0.ipx }) { }
                ConstrainedBox(
                    DpConstraints.fixed(50.dp, 40.dp),
                    LayoutGravity.RelativeToSiblings(TestVerticalLine)
                ) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.Begin) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.Center) {
                Container(LayoutGravity.Center + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.End) {
                Container(LayoutGravity.End + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp), LayoutGravity.End) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceAround) {
                Container(LayoutWidth.Fill + LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp),
                    LayoutWidth.Fill
                ) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceBetween) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
            }
        }, @Composable {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceEvenly) {
                Container(LayoutAspectRatio(2f)) { }
                ConstrainedBox(DpConstraints.fixed(50.dp, 40.dp)) { }
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
    fun testColumn_withFlexibleChildren_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Column {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column {
                ConstrainedBox(
                    DpConstraints.fixed(30.dp, 20.dp),
                    LayoutFlexible(3f) + LayoutGravity.Start
                ) { }
                ConstrainedBox(
                    DpConstraints.fixed(40.dp, 30.dp),
                    LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                Container(LayoutAspectRatio(0.5f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutGravity.End) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.Begin) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.Center) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutFlexible(3f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutFlexible(2f) + LayoutGravity.Center
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutGravity.Center
                ) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.End) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutFlexible(3f) + LayoutGravity.End
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutFlexible(2f) + LayoutGravity.End
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutFlexible(2f) + LayoutGravity.End
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
                    modifier = LayoutFlexible(3f) + LayoutWidth.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(40.dp, 30.dp),
                    modifier = LayoutFlexible(2f) + LayoutWidth.Fill
                ) { }
                Container(
                    LayoutAspectRatio(0.5f) + LayoutFlexible(2f) + LayoutWidth.Fill
                ) { }
                ConstrainedBox(
                    constraints = DpConstraints.fixed(30.dp, 20.dp),
                    modifier = LayoutWidth.Fill
                ) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.SpaceBetween) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutFlexible(2f)) { }
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp)) { }
            }
        }, @Composable {
            Column(arrangement = Arrangement.SpaceEvenly) {
                ConstrainedBox(DpConstraints.fixed(30.dp, 20.dp), LayoutFlexible(3f)) { }
                ConstrainedBox(DpConstraints.fixed(40.dp, 30.dp), LayoutFlexible(2f)) { }
                Container(LayoutAspectRatio(0.5f) + LayoutFlexible(2f)) { }
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
    fun testFlexModifiersChain_leftMostWins() = withDensity(density) {
        val positionedLatch = CountDownLatch(1)
        val containerHeight = Ref<IntPx>()
        val columnHeight = 24.ipx

        show {
            Align(Alignment.TopLeft) {
                Column(LayoutHeight(columnHeight.toDp())) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        containerHeight.value = coordinates.size.height
                        positionedLatch.countDown()
                    }) {
                        Container(
                            LayoutFlexible(2f) + LayoutFlexible(1f)
                        ) {}
                    }
                    Container(LayoutFlexible(1f)) {}
                }
            }
        }

        positionedLatch.await(1, TimeUnit.SECONDS)

        assertNotNull(containerHeight.value)
        assertEquals(columnHeight * 2 / 3, containerHeight.value)
    }

    @Test
    fun testRelativeToSiblingsModifiersChain_leftMostWins() = withDensity(density) {
        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<IntPxSize>()
        val containerPosition = Ref<PxPosition>()
        val size = 40.dp

        show {
            Row {
                Container(
                    modifier = LayoutGravity.RelativeToSiblings { it.height },
                    width = size,
                    height = size
                ) {}
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    containerPosition.value = coordinates.globalPosition
                    positionedLatch.countDown()
                }) {
                    Container(
                        modifier = LayoutGravity.RelativeToSiblings { 0.ipx } +
                                LayoutGravity.RelativeToSiblings { it.height * 0.5 },
                        width = size,
                        height = size
                    ) {}
                }
            }
        }

        positionedLatch.await(1, TimeUnit.SECONDS)

        assertNotNull(containerSize)
        assertEquals(PxPosition(size.toPx(), size.toPx()), containerPosition.value)
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
    Layout(children = children, modifier = modifier, measureBlock = { _, constraints ->
        val widthPx = max(width.toIntPx(), constraints.minWidth)
        val heightPx = max(height.toIntPx(), constraints.minHeight)
        layout(widthPx, heightPx,
            mapOf(TestHorizontalLine to baseline.toIntPx(), TestVerticalLine to baseline.toIntPx())
        ) {}
    })
}