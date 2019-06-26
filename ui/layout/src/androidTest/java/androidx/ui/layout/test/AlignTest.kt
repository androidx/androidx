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
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.AspectRatio
import androidx.ui.layout.Container
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class AlignTest : LayoutTest() {
    @Test
    fun testAlign() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<PxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Align(alignment = Alignment.BottomRight) {
                SaveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
                Container(width = sizeDp, height = sizeDp) {
                    SaveLayoutInfo(
                        size = childSize,
                        position = childPosition,
                        positionedLatch = positionedLatch
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(root.width.px, root.height.px), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(
            PxPosition(root.width.px - size + 1.px, root.height.px - size + 1.px),
            childPosition.value
        )
    }

    @Test
    fun testAlign_wrapsContent_whenMeasuredWithInfiniteConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<PxSize>()
        val alignPosition = Ref<PxPosition>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show {
            Layout(
                children = {
                    Align(alignment = Alignment.BottomRight) {
                        SaveLayoutInfo(
                            size = alignSize,
                            position = alignPosition,
                            positionedLatch = positionedLatch
                        )
                        Container(width = sizeDp, height = sizeDp) {
                            SaveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        }
                    }
                },
                layoutBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), alignSize.value)
        assertEquals(PxPosition(0.px, 0.px), alignPosition.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    // TODO(popam): this should be unit test instead
    @Test
    fun testAlignmentCoordinates() {
        val size = IntPxSize(2.ipx, 2.ipx)
        assertEquals(IntPxPosition(0.ipx, 0.ipx), Alignment.TopLeft.align(size))
        assertEquals(IntPxPosition(1.ipx, 0.ipx), Alignment.TopCenter.align(size))
        assertEquals(IntPxPosition(2.ipx, 0.ipx), Alignment.TopRight.align(size))
        assertEquals(IntPxPosition(0.ipx, 1.ipx), Alignment.CenterLeft.align(size))
        assertEquals(IntPxPosition(1.ipx, 1.ipx), Alignment.Center.align(size))
        assertEquals(IntPxPosition(2.ipx, 1.ipx), Alignment.CenterRight.align(size))
        assertEquals(IntPxPosition(0.ipx, 2.ipx), Alignment.BottomLeft.align(size))
        assertEquals(IntPxPosition(1.ipx, 2.ipx), Alignment.BottomCenter.align(size))
        assertEquals(IntPxPosition(2.ipx, 2.ipx), Alignment.BottomRight.align(size))
    }

    @Test
    fun testAlign_hasCorrectIntrinsicMeasurements() = withDensity(density) {
        testIntrinsics(@Composable {
            Align(alignment = Alignment.TopLeft) {
                AspectRatio(2f) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            // Min height.
            assertEquals(50.dp.toIntPx() / 2, minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            // Max width.
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            // Max height.
            assertEquals(50.dp.toIntPx() / 2, maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test
    fun testAlign_hasCorrectIntrinsicMeasurements_whenNoChildren() = withDensity(density) {
        testIntrinsics(@Composable {
            Align(alignment = Alignment.TopLeft) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(25.dp.toIntPx()))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(25.dp.toIntPx()))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(25.dp.toIntPx()))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(25.dp.toIntPx()))
        }
    }
}
