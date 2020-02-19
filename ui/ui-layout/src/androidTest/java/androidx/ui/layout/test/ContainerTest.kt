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
import androidx.compose.Model
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.Ref
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Wrap
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ContainerTest : LayoutTest() {
    @Test
    fun testContainer_wrapsChild() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<IntPxSize>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container {
                        EmptyBox(width = sizeDp, height = sizeDp)
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_appliesPaddingToChild() = with(density) {
        val paddingDp = 20.dp
        val padding = paddingDp.toIntPx()
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container(padding = EdgeInsets(paddingDp)) {
                        OnChildPositioned(onPositioned = { coordinates ->
                            childPosition.value = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            positionedLatch.countDown()
                        }) {
                            EmptyBox(width = sizeDp, height = sizeDp)
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val totalPadding = paddingDp.toIntPx() * 2
        assertEquals(
            IntPxSize(size + totalPadding, size + totalPadding),
            containerSize.value
        )
        assertEquals(PxPosition(padding, padding), childPosition.value)
    }

    @Test
    fun testContainer_passesConstraintsToChild() = with(density) {
        val sizeDp = 100.dp
        val childWidthDp = 20.dp
        val childWidth = childWidthDp.toIntPx()
        val childHeightDp = 30.dp
        val childHeight = childHeightDp.toIntPx()
        val childConstraints = DpConstraints.fixed(childWidthDp, childHeightDp)

        val positionedLatch = CountDownLatch(4)
        val containerSize = Ref<IntPxSize>()
        val childSize = Array(3) { IntPxSize(0.ipx, 0.ipx) }
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Row {
                        Container(width = childWidthDp, height = childHeightDp) {
                            OnChildPositioned(onPositioned = { coordinates ->
                                childSize[0] = coordinates.size
                                positionedLatch.countDown()
                            }) {
                                EmptyBox(width = sizeDp, height = sizeDp)
                            }
                        }
                        Container(constraints = childConstraints) {
                            OnChildPositioned(onPositioned = { coordinates ->
                                childSize[1] = coordinates.size
                                positionedLatch.countDown()
                            }) {
                                EmptyBox(width = sizeDp, height = sizeDp)
                            }
                        }
                        Container(
                            constraints = (childConstraints),
                            // These should have priority.
                            width = (childWidthDp * 2),
                            height = (childHeightDp * 2)
                        ) {
                            OnChildPositioned(onPositioned = { coordinates ->
                                childSize[2] = coordinates.size
                                positionedLatch.countDown()
                            }) {
                                EmptyBox(width = sizeDp, height = sizeDp)
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(IntPxSize(childWidth, childHeight), childSize[0])
        assertEquals(IntPxSize(childWidth, childHeight), childSize[1])
        assertEquals(
            IntPxSize((childWidthDp * 2).toIntPx(), (childHeightDp * 2).toIntPx()),
            childSize[2]
        )
    }

    @Test
    fun testContainer_fillsAvailableSpace_whenSizeIsMax() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val alignSize = Ref<IntPxSize>()
        val containerSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnPositioned(onPositioned = { coordinates ->
                    alignSize.value = coordinates.size
                    positionedLatch.countDown()
                })
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    Container(expanded = true) {
                        OnChildPositioned(onPositioned = { coordinates ->
                            childSize.value = coordinates.size
                            childPosition.value = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            positionedLatch.countDown()
                        }) {
                            EmptyBox(width = sizeDp, height = sizeDp)
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(alignSize.value, containerSize.value)
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(
            PxPosition(
                (containerSize.value!!.width.toPx() / 2 - size.toPx() / 2).round(),
                (containerSize.value!!.height.toPx() / 2 - size.toPx() / 2).round()
            ),
            childPosition.value
        )
    }

    @Test
    fun testContainer_respectsIncomingMinConstraints() = with(density) {
        // Start with an even number of IntPx to avoid rounding issues due to different DPI
        // I.e, if we fix Dp instead, it's possible that when we convert to Px, sizeDp can round
        // down but sizeDp * 2 can round up, causing a 1 pixel test error.
        val size = 200.ipx
        val sizeDp = size.toDp()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<IntPxSize>()
        val childSize = Ref<IntPxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.TopLeft) {
                OnChildPositioned(onPositioned = { coordinates ->
                    containerSize.value = coordinates.size
                    positionedLatch.countDown()
                }) {
                    val constraints = DpConstraints(minWidth = sizeDp * 2, minHeight = sizeDp * 2)
                    ConstrainedBox(constraints = constraints) {
                        Container(alignment = Alignment.BottomRight) {
                            OnChildPositioned(onPositioned = { coordinates ->
                                childSize.value = coordinates.size
                                childPosition.value =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                positionedLatch.countDown()
                            }) {
                                EmptyBox(width = sizeDp, height = sizeDp)
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(
            IntPxSize((sizeDp * 2).toIntPx(), (sizeDp * 2).toIntPx()),
            containerSize.value
        )
        assertEquals(IntPxSize(size, size), childSize.value)
        assertEquals(PxPosition(size, size), childPosition.value)
    }

    @Test
    fun testContainer_hasTheRightSize_withPaddingAndNoChildren() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val containerSize = Ref<IntPxSize>()
        val latch = CountDownLatch(1)
        show {
            Align(alignment = Alignment.TopLeft) {
                Container(width = sizeDp, height = sizeDp, padding = EdgeInsets(10.dp)) {
                    OnPositioned(onPositioned = { coordinates ->
                        containerSize.value = coordinates.size
                        latch.countDown()
                    })
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(IntPxSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_correctlyAppliesNonSymmetricPadding() = with(density) {
        val childSizeDp = 50.dp
        val paddingLeft = 8.dp
        val paddingTop = 7.dp
        val paddingRight = 5.dp
        val paddingBottom = 10.dp
        val edgeInsets = EdgeInsets(
            left = paddingLeft, top = paddingTop,
            right = paddingRight, bottom = paddingBottom
        )
        val expectedSize = IntPxSize(
            childSizeDp.toIntPx() + paddingLeft.toIntPx() + paddingRight.toIntPx(),
            childSizeDp.toIntPx() + paddingTop.toIntPx() + paddingBottom.toIntPx()
        )

        var containerSize: IntPxSize? = null
        val latch = CountDownLatch(1)
        show {
            Wrap {
                Container(padding = edgeInsets) {
                    Spacer(LayoutSize(width = childSizeDp, height = childSizeDp))
                    OnPositioned(onPositioned = { coordinates ->
                        containerSize = coordinates.size
                        latch.countDown()
                    })
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedSize, containerSize)
    }

    @Test
    fun testContainer_contentSmallerThanPaddingIsCentered() = with(density) {
        val containerSize = 50.dp
        val padding = 10.dp
        val childSize = 5.dp
        val edgeInsets = EdgeInsets(padding)

        var childCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        show {
            Wrap {
                Container(width = containerSize, height = containerSize, padding = edgeInsets) {
                    OnChildPositioned(onPositioned = { coordinates ->
                        childCoordinates = coordinates
                        latch.countDown()
                    }) {
                        Spacer(LayoutSize(width = childSize, height = childSize))
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val centeringOffset = padding.toIntPx() +
                (containerSize.toIntPx() - padding.toIntPx() * 2 - childSize.toIntPx()) / 2
        val childPosition = childCoordinates!!.parentCoordinates!!.childToLocal(
            childCoordinates!!,
            PxPosition.Origin
        )
        assertEquals(PxPosition(centeringOffset, centeringOffset), childPosition)
        assertEquals(IntPxSize(childSize.toIntPx(), childSize.toIntPx()), childCoordinates!!.size)
    }

    @Test
    fun testContainer_childAffectsContainerSize() {
        var layoutLatch = CountDownLatch(2)
        val model = SizeModel(10.dp)
        var measure = 0
        var layout = 0
        show {
            Align(alignment = Alignment.TopLeft) {
                Layout(children = {
                    Container {
                        OnChildPositioned(onPositioned = {
                            layoutLatch.countDown()
                        }) {
                            EmptyBox(width = model.size, height = 10.dp)
                        }
                    }
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.place(IntPx.Zero, IntPx.Zero)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(2)
        activityTestRule.runOnUiThread { model.size = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, measure)
        assertEquals(2, layout)
    }

    @Test
    fun testContainer_childDoesNotAffectContainerSize_whenSizeIsMax() {
        var layoutLatch = CountDownLatch(2)
        val model = SizeModel(10.dp)
        var measure = 0
        var layout = 0
        show {
            Align(alignment = Alignment.TopLeft) {
                Layout(children = {
                    Container(expanded = true) {
                        OnChildPositioned(onPositioned = {
                            layoutLatch.countDown()
                        }) {
                            EmptyBox(width = model.size, height = 10.dp)
                        }
                    }
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.place(IntPx.Zero, IntPx.Zero)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { model.size = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
    }

    @Test
    fun testContainer_childDoesNotAffectContainerSize_whenFixedWidthAndHeight() {
        var layoutLatch = CountDownLatch(2)
        val model = SizeModel(10.dp)
        var measure = 0
        var layout = 0
        show {
            Align(alignment = Alignment.TopLeft) {
                Layout(children = {
                    Container(width = 20.dp, height = 20.dp) {
                        OnChildPositioned(onPositioned = {
                            layoutLatch.countDown()
                        }) {
                            EmptyBox(width = model.size, height = 10.dp)
                        }
                    }
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.place(IntPx.Zero, IntPx.Zero)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { model.size = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
    }

    @Composable
    fun EmptyBox(width: Dp, height: Dp) {
        Layout(children = { }) { _, constraints ->
            layout(
                width.toIntPx().coerceIn(constraints.minWidth, constraints.maxWidth),
                height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            ) {}
        }
    }
}

@Model
data class SizeModel(var size: Dp)
