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
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.WithConstraints
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSizeIn
import androidx.ui.layout.relativePaddingFrom
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
    fun testRelativePaddingFrom_vertical() = with(density) {
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
            Stack(
                Modifier.onPositioned {
                    parentSize.value = it.size
                    layoutLatch.countDown()
                }
            ) {
                Layout({},
                    Modifier
                        .onPositioned {
                            childSize.value = it.size
                            childPosition.value = it.globalPosition
                            layoutLatch.countDown()
                        }
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                ) { _, _, _ ->
                    layout(childDp.toIntPx(), 0.ipx, mapOf(testLine to lineDp.toIntPx())) {}
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
            (beforeDp.toIntPx().toPx() - lineDp.toIntPx().toPx()).value,
            childPosition.value!!.x
        )
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal() = with(density) {
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
            Stack(
                modifier = Modifier.onPositioned {
                    parentSize.value = it.size
                    layoutLatch.countDown()
                }
            ) {
                Layout(
                    modifier = Modifier
                        .onPositioned {
                            childSize.value = it.size
                            childPosition.value = it.globalPosition
                            layoutLatch.countDown()
                        }
                        .relativePaddingFrom(testLine, beforeDp, afterDp),
                    children = {}
                ) { _, _, _ ->
                    layout(0.ipx, childDp.toIntPx(), mapOf(testLine to lineDp.toIntPx())) {}
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(beforeDp.toIntPx() + afterDp.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(
            (beforeDp.toIntPx().toPx() - lineDp.toIntPx().toPx()).value,
            childPosition.value!!.y
        )
    }

    @Test
    fun testRelativePaddingFrom_vertical_withSmallOffsets() = with(density) {
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
            Stack(modifier = Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                Layout(
                    modifier = Modifier
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp),
                    children = {}
                ) { _, _, _ ->
                    layout(childDp.toIntPx(), 0.ipx, mapOf(testLine to lineDp.toIntPx())) { }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal_withSmallOffsets() = with(density) {
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
            Stack(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                Layout(
                    { },
                    Modifier
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                ) { _, _, _ ->
                    layout(0.ipx, childDp.toIntPx(), mapOf(testLine to lineDp.toIntPx())) { }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_vertical_withInsufficientSpace() = with(density) {
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
            Stack(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                Layout(
                    children = { },
                    modifier = Modifier
                        .preferredSizeIn(maxWidth = maxWidth)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                ) { _, _, _ ->
                    layout(
                        childDp.toIntPx(),
                        0.ipx,
                        mapOf(testLine to lineDp.toIntPx())
                    ) { }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxWidth.toIntPx(), parentSize.value!!.width)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(5f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal_withInsufficientSpace() = with(density) {
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
            Stack(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                Layout(
                    children = { },
                    modifier = Modifier
                        .preferredSizeIn(maxHeight = maxHeight)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                ) { _, _, _ ->
                    layout(
                        0.ipx,
                        childDp.toIntPx(),
                        mapOf(testLine to lineDp.toIntPx())
                    ) { }
                }
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxHeight.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(5f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_vertical_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minHeight = 10.dp
        show {
            Stack {
                WithConstraints(
                    Modifier
                        .preferredSizeIn(minHeight = minHeight)
                        .relativePaddingFrom(testLine)
                ) {
                    Assert.assertEquals(minHeight.toIntPx(), constraints.minHeight)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRelativePaddingFrom_horizontal_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minWidth = 10.dp
        show {
            Stack {
                WithConstraints(
                    Modifier
                        .preferredSizeIn(minWidth = minWidth)
                        .relativePaddingFrom(testLine)
                ) {
                    Assert.assertEquals(minWidth.toIntPx(), constraints.minWidth)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
}
