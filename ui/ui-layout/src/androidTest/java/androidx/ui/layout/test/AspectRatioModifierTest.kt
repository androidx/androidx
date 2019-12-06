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
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutAspectRatio
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalArgumentException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class AspectRatioModifierTest : LayoutTest() {
    @Test
    fun testAspectRatioModifier_intrinsicDimensions() = withDensity(density) {
        testIntrinsics(@Composable {
            Container(modifier = LayoutAspectRatio(2f), width = 30.dp, height = 40.dp) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(40.ipx, minIntrinsicWidth(20.ipx))
            assertEquals(40.ipx, maxIntrinsicWidth(20.ipx))
            assertEquals(20.ipx, minIntrinsicHeight(40.ipx))
            assertEquals(20.ipx, maxIntrinsicHeight(40.ipx))

            assertEquals(30.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAspectRatioModifier_zeroRatio() {
        show {
            Container(LayoutAspectRatio(0f)) { }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAspectRatioModifier_negativeRatio() {
        show {
            Container(LayoutAspectRatio(-2f)) { }
        }
    }

    @Test
    fun testAspectRatio_sizesCorrectly() {
        assertEquals(PxSize(30.px, 30.px), getSize(1f, Constraints(maxWidth = 30.ipx)))
        assertEquals(PxSize(30.px, 15.px), getSize(2f, Constraints(maxWidth = 30.ipx)))
        assertEquals(
            PxSize(10.px, 10.px),
            getSize(1f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            PxSize(20.px, 10.px),
            getSize(2f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            PxSize(10.px, 5.px),
            getSize(2f, Constraints(minWidth = 10.ipx, minHeight = 5.ipx))
        )
        assertEquals(
            PxSize(20.px, 10.px),
            getSize(2f, Constraints(minWidth = 5.ipx, minHeight = 10.ipx))
        )
    }

    private fun getSize(aspectRatio: Float, childContraints: Constraints): PxSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<PxSize>()
        val position = Ref<PxPosition>()
        show {
            Layout(@Composable {
                Container(LayoutAspectRatio(aspectRatio)) {
                    SaveLayoutInfo(size, position, positionedLatch)
                }
            }) { measurables, incomingConstraints ->
                require(measurables.isNotEmpty())
                val placeable = measurables.first().measure(childContraints)
                layout(incomingConstraints.maxWidth, incomingConstraints.maxHeight) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)
        return size.value!!
    }
}