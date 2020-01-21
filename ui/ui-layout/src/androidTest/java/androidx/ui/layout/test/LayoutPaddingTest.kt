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
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.OnPositioned
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutPadding
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import androidx.ui.unit.withDensity
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class LayoutPaddingTest : LayoutTest() {

    /**
     * Tests that the [LayoutPadding]-all and [LayoutPadding] factories return equivalent modifiers.
     */
    @Test
    fun allEqualToAbsoluteWithExplicitSides() {
        Assert.assertEquals(
            LayoutPadding(10.dp, 10.dp, 10.dp, 10.dp),
            LayoutPadding(10.dp)
        )
    }

    /**
     * Tests the top-level [LayoutPadding] modifier factory with a single "all sides" argument,
     * checking that a uniform padding of all sides is applied to a child when plenty of space is
     * available for both content and padding.
     */
    @Test
    fun paddingAllAppliedToChild() = withDensity(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable() () -> Unit ->
            TestBox(modifier = LayoutPadding(padding), body = child)
        }
    }

    /**
     * Tests the top-level [LayoutPadding] modifier factory with different values for left, top,
     * right and bottom paddings, checking that this padding is applied as expected when plenty of
     * space is available for both the content and padding.
     */
    @Test
    fun absolutePaddingAppliedToChild() {
        val paddingLeft = 10.dp
        val paddingTop = 15.dp
        val paddingRight = 20.dp
        val paddingBottom = 30.dp
        val padding = LayoutPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        testPaddingWithDifferentInsetsImplementation(
            paddingLeft,
            paddingTop,
            paddingRight,
            paddingBottom
        ) { child: @Composable() () -> Unit ->
            TestBox(modifier = padding, body = child)
        }
    }

    /**
     * Tests the result of the [LayoutPadding] modifier factory when not enough space is
     * available to accommodate both the padding and the content. In this case, the padding
     * should still be applied, modifying the final position of the content by its left and top
     * paddings even if it would result in constraints that the child content is unable or
     * unwilling to satisfy.
     */
    @Test
    fun insufficientSpaceAvailable() = withDensity(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable() () -> Unit ->
            TestBox(modifier = LayoutPadding(padding), body = child)
        }
    }

    @Test
    fun intrinsicMeasurements() = withDensity(density) {
        val padding = 100.ipx.toDp()

        val latch = CountDownLatch(1)
        var error: Throwable? = null
        testIntrinsics(@Composable {
            TestBox(modifier = LayoutPadding(padding)) {
                Container(LayoutAspectRatio(2f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Spacing is applied on both sides of an axis
            val totalAxisSpacing = (padding * 2).toIntPx()

            // When the width/height is measured as 3 x the padding
            val testDimension = (padding * 3).toIntPx()
            // The actual dimension for the AspectRatio will be: test dimension - total padding
            val actualAspectRatioDimension = testDimension - totalAxisSpacing

            // When we measure the width first, the height will be half
            val expectedAspectRatioHeight = actualAspectRatioDimension / 2f
            // When we measure the height first, the width will be double
            val expectedAspectRatioWidth = actualAspectRatioDimension * 2

            // Add back the padding on both sides to get the total expected height
            val expectedTotalHeight = expectedAspectRatioHeight + totalAxisSpacing
            // Add back the padding on both sides to get the total expected height
            val expectedTotalWidth = expectedAspectRatioWidth + totalAxisSpacing

            try {
                // Min width.
                assertEquals(totalAxisSpacing, minIntrinsicWidth(0.dp.toIntPx()))
                assertEquals(expectedTotalWidth, minIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicWidth(IntPx.Infinity))
                // Min height.
                assertEquals(totalAxisSpacing, minIntrinsicHeight(0.dp.toIntPx()))
                assertEquals(expectedTotalHeight, minIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicHeight(IntPx.Infinity))
                // Max width.
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(0.dp.toIntPx()))
                assertEquals(expectedTotalWidth, maxIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(IntPx.Infinity))
                // Max height.
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(0.dp.toIntPx()))
                assertEquals(expectedTotalHeight, maxIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(IntPx.Infinity))
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        latch.await(1, TimeUnit.SECONDS)
        error?.let { throw it }

        Unit
    }

    private fun testPaddingIsAppliedImplementation(
        padding: Dp,
        paddingContainer: @Composable() (@Composable() () -> Unit) -> Unit
    ) = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntPxSize(-1.ipx, -1.ipx)
        var childPosition = PxPosition(-1.px, -1.px)
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints.fixed(sizeDp, sizeDp)) {
                    val children = @Composable {
                        Container {
                            OnPositioned(onPositioned = { coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            })
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val innerSize = (size - paddingPx * 2)
        assertEquals(IntPxSize(innerSize, innerSize), childSize)
        val left = ((root.width.ipx - size) / 2) + paddingPx
        val top = ((root.height.ipx - size) / 2) + paddingPx
        assertEquals(
            PxPosition(left.toPx(), top.toPx()),
            childPosition
        )
    }

    private fun testPaddingWithDifferentInsetsImplementation(
        left: Dp,
        top: Dp,
        right: Dp,
        bottom: Dp,
        paddingContainer: @Composable() ((@Composable() () -> Unit) -> Unit)
    ) = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntPxSize(-1.ipx, -1.ipx)
        var childPosition = PxPosition(-1.px, -1.px)
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints.fixed(sizeDp, sizeDp)) {
                    val children = @Composable {
                        Container {
                            OnPositioned(onPositioned = { coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            })
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        val paddingLeft = left.toIntPx()
        val paddingRight = right.toIntPx()
        val paddingTop = top.toIntPx()
        val paddingBottom = bottom.toIntPx()
        assertEquals(
            IntPxSize(
                size - paddingLeft - paddingRight,
                size - paddingTop - paddingBottom
            ),
            childSize
        )
        val viewLeft = ((root.width.ipx - size) / 2) + paddingLeft
        val viewTop = ((root.height.ipx - size) / 2) + paddingTop
        assertEquals(
            PxPosition(viewLeft.toPx(), viewTop.toPx()),
            childPosition
        )
    }

    private fun testPaddingWithInsufficientSpaceImplementation(
        padding: Dp,
        paddingContainer: @Composable() (@Composable() () -> Unit) -> Unit
    ) = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntPxSize(-1.ipx, -1.ipx)
        var childPosition = PxPosition(-1.px, -1.px)
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints.fixed(sizeDp, sizeDp)) {
                    paddingContainer {
                        Container {
                            OnPositioned(onPositioned = { coordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            })
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidComposeView()
        waitForDraw(root)

        assertEquals(IntPxSize(0.ipx, 0.ipx), childSize)
        val left = ((root.width.ipx - size) / 2) + paddingPx
        val top = ((root.height.ipx - size) / 2) + paddingPx
        assertEquals(PxPosition(left.toPx(), top.toPx()), childPosition)
    }

    /**
     * A trivial layout that applies a [Modifier] and measures/lays out a single child
     * with the same constraints it received.
     */
    @Composable
    private fun TestBox(modifier: Modifier = Modifier.None, body: @Composable() () -> Unit) {
        Layout(children = body, modifier = modifier) { measurables, constraints ->
            require(measurables.size == 1) {
                "TestBox received ${measurables.size} children; must have exactly 1"
            }
            val placeable = measurables.first().measure(constraints)
            layout(
                min(placeable.width, constraints.maxWidth),
                min(placeable.height, constraints.maxHeight)
            ) {
                placeable.place(IntPx.Zero, IntPx.Zero)
            }
        }
    }
}
