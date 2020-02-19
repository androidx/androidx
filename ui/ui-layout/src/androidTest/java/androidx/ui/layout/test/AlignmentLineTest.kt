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
import androidx.ui.core.Constraints
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.WithConstraints
import androidx.ui.layout.AlignmentLineOffset
import androidx.ui.layout.CenterAlignmentLine
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class AlignmentLineTest : LayoutTest() {
    @Test
    fun testAlignmentLineOffset_vertical() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val beforeDp = 20.px.toDp()
        val afterDp = 40.px.toDp()
        val childDp = 30.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                    SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                        layout(childDp.toIntPx(), 0.ipx, mapOf(testLine to lineDp.toIntPx())) { }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(
            beforeDp.toIntPx() + afterDp.toIntPx(),
            parentSize.value!!.width
        )
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(
            beforeDp.toIntPx().toPx() - lineDp.toIntPx().toPx(),
            childPosition.value!!.x
        )
        Assert.assertEquals(0.px, childPosition.value!!.y)
    }

    @Test
    fun testAlignmentLineOffset_horizontal() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val beforeDp = 20.px.toDp()
        val afterDp = 40.px.toDp()
        val childDp = 30.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                    SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                        layout(0.ipx, childDp.toIntPx(), mapOf(testLine to lineDp.toIntPx())) { }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(beforeDp.toIntPx() + afterDp.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0.px, childPosition.value!!.x)
        Assert.assertEquals(
            beforeDp.toIntPx().toPx() - lineDp.toIntPx().toPx(),
            childPosition.value!!.y
        )
    }

    @Test
    fun testAlignmentLineOffset_vertical_withSmallOffsets() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val beforeDp = 5.px.toDp()
        val afterDp = 5.px.toDp()
        val childDp = 30.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                    SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                        layout(childDp.toIntPx(), 0.ipx, mapOf(testLine to lineDp.toIntPx())) { }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0.px, childPosition.value!!.x)
        Assert.assertEquals(0.px, childPosition.value!!.y)
    }

    @Test
    fun testAlignmentLineOffset_horizontal_withSmallOffsets() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val beforeDp = 5.px.toDp()
        val afterDp = 5.px.toDp()
        val childDp = 30.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                    SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                        layout(0.ipx, childDp.toIntPx(), mapOf(testLine to lineDp.toIntPx())) { }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0.px, childPosition.value!!.x)
        Assert.assertEquals(0.px, childPosition.value!!.y)
    }

    @Test
    fun testAlignmentLineOffset_vertical_withInsufficientSpace() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val maxWidth = 30.px.toDp()
        val beforeDp = 20.px.toDp()
        val afterDp = 20.px.toDp()
        val childDp = 25.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                ConstrainedBox(DpConstraints(maxWidth = maxWidth)) {
                    AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                        SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                        Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                            layout(
                                childDp.toIntPx(),
                                0.ipx,
                                mapOf(testLine to lineDp.toIntPx())
                            ) { }
                        }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxWidth.toIntPx(), parentSize.value!!.width)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(5.px, childPosition.value!!.x)
        Assert.assertEquals(0.px, childPosition.value!!.y)
    }

    @Test
    fun testAlignmentLineOffset_horizontal_withInsufficientSpace() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val maxHeight = 30.px.toDp()
        val beforeDp = 20.px.toDp()
        val afterDp = 20.px.toDp()
        val childDp = 25.px.toDp()
        val lineDp = 10.px.toDp()

        val parentSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Wrap {
                ConstrainedBox(DpConstraints(maxHeight = maxHeight)) {
                    AlignmentLineOffset(testLine, before = beforeDp, after = afterDp) {
                        SaveLayoutInfo(parentSize, Ref(), layoutLatch)
                        Layout({ SaveLayoutInfo(childSize, childPosition, layoutLatch) }) { _, _ ->
                            layout(
                                0.ipx,
                                childDp.toIntPx(),
                                mapOf(testLine to lineDp.toIntPx())
                            ) { }
                        }
                    }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxHeight.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0.px, childPosition.value!!.x)
        Assert.assertEquals(5.px, childPosition.value!!.y)
    }

    @Test
    fun testAlignmentLineOffset_vertical_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minHeight = 10.dp
        show {
            Wrap {
                ConstrainedBox(DpConstraints(minHeight = minHeight)) {
                    AlignmentLineOffset(testLine) {
                        WithConstraints { constraints ->
                            Assert.assertEquals(minHeight.toIntPx(), constraints.minHeight)
                            latch.countDown()
                        }
                    }
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testAlignmentLineOffset_horizontal_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minWidth = 10.dp
        show {
            Wrap {
                ConstrainedBox(DpConstraints(minWidth = minWidth)) {
                    AlignmentLineOffset(testLine) {
                        WithConstraints { constraints ->
                            Assert.assertEquals(minWidth.toIntPx(), constraints.minWidth)
                            latch.countDown()
                        }
                    }
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testCenterAlignmentLine_vertical_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minHeight = 10.dp
        show {
            Wrap {
                ConstrainedBox(DpConstraints(minHeight = minHeight)) {
                    CenterAlignmentLine(testLine) {
                        WithConstraints { constraints ->
                            Assert.assertEquals(minHeight.toIntPx(), constraints.minHeight)
                            latch.countDown()
                        }
                    }
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testCenterAlignmentLine_horizontal_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minWidth = 10.dp
        show {
            Wrap {
                ConstrainedBox(DpConstraints(minWidth = minWidth)) {
                    CenterAlignmentLine(testLine) {
                        WithConstraints { constraints ->
                            Assert.assertEquals(minWidth.toIntPx(), constraints.minWidth)
                            latch.countDown()
                        }
                    }
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testCenterAlignmentLine_vertical_withInfiniteWidth() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val childWidth = 30.dp
        val childHeight = 40.dp
        val lineDp = 10.dp

        val centerSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Layout({
                CenterAlignmentLine(testLine) {
                    SaveLayoutInfo(centerSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(Ref(), childPosition, layoutLatch) }) { _, _ ->
                        layout(
                            childWidth.toIntPx(),
                            childHeight.toIntPx(),
                            mapOf(testLine to lineDp.toIntPx())
                        ) { }
                    }
                }
            }) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints()) // Infinite measuring
                layout(0.ipx, 0.ipx) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(centerSize.value)
        Assert.assertEquals(
            ((childWidth.toIntPx() - lineDp.toIntPx()) * 2),
            centerSize.value!!.width
        )
        Assert.assertEquals(childHeight.toIntPx(), centerSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(
            (childWidth.toIntPx() - lineDp.toIntPx() * 2).toPx(), childPosition.value!!.x
        )
        Assert.assertEquals(0.px, childPosition.value!!.y)
    }

    @Test
    fun testCenterAlignmentLine_horizontal_withInfiniteHeight() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val childWidth = 30.dp
        val childHeight = 40.dp
        val lineDp = 10.dp

        val centerSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Layout({
                CenterAlignmentLine(testLine) {
                    SaveLayoutInfo(centerSize, Ref(), layoutLatch)
                    Layout({ SaveLayoutInfo(Ref(), childPosition, layoutLatch) }) { _, _ ->
                        layout(
                            childWidth.toIntPx(),
                            childHeight.toIntPx(),
                            mapOf(testLine to lineDp.toIntPx())
                        ) { }
                    }
                }
            }) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints()) // Infinite measuring
                layout(0.ipx, 0.ipx) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(centerSize.value)
        Assert.assertEquals(childWidth.toIntPx(), centerSize.value!!.width)
        Assert.assertEquals(
            ((childHeight.toIntPx() - lineDp.toIntPx()) * 2),
            centerSize.value!!.height
        )
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0.px, childPosition.value!!.x)
        Assert.assertEquals(
            (childHeight.toIntPx() - lineDp.toIntPx() * 2).toPx(), childPosition.value!!.y
        )
    }
}
