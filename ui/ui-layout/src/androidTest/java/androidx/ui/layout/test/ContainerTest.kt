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
import androidx.ui.core.Dp
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FixedSpacer
import androidx.ui.layout.Row
import androidx.ui.layout.Wrap
import androidx.compose.Composable
import androidx.compose.composer
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
    fun testContainer_wrapsChild() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<PxSize>()
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

        assertEquals(PxSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_appliesPaddingToChild() = withDensity(density) {
        val paddingDp = 20.dp
        val padding = paddingDp.toIntPx()
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<PxSize>()
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
            PxSize(size + totalPadding, size + totalPadding),
            containerSize.value
        )
        assertEquals(PxPosition(padding, padding), childPosition.value)
    }

    @Test
    fun testContainer_passesConstraintsToChild() = withDensity(density) {
        val sizeDp = 100.dp
        val childWidthDp = 20.dp
        val childWidth = childWidthDp.toIntPx()
        val childHeightDp = 30.dp
        val childHeight = childHeightDp.toIntPx()
        val childConstraints = DpConstraints.tightConstraints(childWidthDp, childHeightDp)

        val positionedLatch = CountDownLatch(4)
        val containerSize = Ref<PxSize>()
        val childSize = Array(3) { PxSize(0.px, 0.px) }
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

        assertEquals(PxSize(childWidth, childHeight), childSize[0])
        assertEquals(PxSize(childWidth, childHeight), childSize[1])
        assertEquals(
            PxSize((childWidthDp * 2).toIntPx(), (childHeightDp * 2).toIntPx()),
            childSize[2]
        )
    }

    @Test
    fun testContainer_fillsAvailableSpace_whenSizeIsMax() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val alignSize = Ref<PxSize>()
        val containerSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
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
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(
            PxPosition(
                (containerSize.value!!.width / 2 - size.toPx() / 2).round(),
                (containerSize.value!!.height / 2 - size.toPx() / 2).round()
            ),
            childPosition.value
        )
    }

    @Test
    fun testContainer_respectsIncomingMinConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
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
            PxSize((sizeDp * 2).toIntPx(), (sizeDp * 2).toIntPx()),
            containerSize.value
        )
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(size + 1.ipx, size + 1.ipx), childPosition.value)
    }

    @Test
    fun testContainer_hasTheRightSize_withPaddingAndNoChildren() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val containerSize = Ref<PxSize>()
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

        assertEquals(PxSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_correctlyAppliesNonSymmetricPadding() = withDensity(density) {
        val childSizeDp = 50.dp
        val paddingLeft = 8.dp
        val paddingTop = 7.dp
        val paddingRight = 5.dp
        val paddingBottom = 10.dp
        val edgeInsets = EdgeInsets(
            left = paddingLeft, top = paddingTop,
            right = paddingRight, bottom = paddingBottom
        )
        val expectedSize = PxSize(
            childSizeDp.toIntPx() + paddingLeft.toIntPx() + paddingRight.toIntPx(),
            childSizeDp.toIntPx() + paddingTop.toIntPx() + paddingBottom.toIntPx()
        )

        var containerSize: PxSize? = null
        val latch = CountDownLatch(1)
        show {
            Wrap {
                Container(padding = edgeInsets) {
                    FixedSpacer(width = childSizeDp, height = childSizeDp)
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
    fun testContainer_contentSmallerThanPaddingIsCentered() = withDensity(density) {
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
                        FixedSpacer(width = childSize, height = childSize)
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val centeringOffset = padding.toIntPx() +
                (containerSize.toIntPx() - padding.toIntPx() * 2 - childSize.toIntPx()) / 2
        assertEquals(PxPosition(centeringOffset, centeringOffset), childCoordinates!!.position)
        assertEquals(PxSize(childSize.toIntPx(), childSize.toIntPx()), childCoordinates!!.size)
    }

    @Composable
    fun EmptyBox(width: Dp, height: Dp) {
        Layout(layoutBlock = { _, constraints ->
            layout(
                width.toIntPx().coerceIn(constraints.minWidth, constraints.maxWidth),
                height.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            ) {}
        }, children = { })
    }
}
