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
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.layout.aspectRatio
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class LayoutAspectRatioTest : LayoutTest() {
    @Test
    fun testAspectRatioModifier_intrinsicDimensions() = with(density) {
        testIntrinsics(@Composable {
            Container(modifier = Modifier.aspectRatio(2f), width = 30.dp, height = 40.dp) { }
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
        Modifier.aspectRatio(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAspectRatioModifier_negativeRatio() {
        Modifier.aspectRatio(-2f)
    }

    @Test
    fun testAspectRatio_sizesCorrectly() {
        assertEquals(IntPxSize(30.ipx, 30.ipx), getSize(1f, Constraints(maxWidth = 30.ipx)))
        assertEquals(IntPxSize(30.ipx, 15.ipx), getSize(2f, Constraints(maxWidth = 30.ipx)))
        assertEquals(
            IntPxSize(10.ipx, 10.ipx),
            getSize(1f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            IntPxSize(20.ipx, 10.ipx),
            getSize(2f, Constraints(maxWidth = 30.ipx, maxHeight = 10.ipx))
        )
        assertEquals(
            IntPxSize(10.ipx, 5.ipx),
            getSize(2f, Constraints(minWidth = 10.ipx, minHeight = 5.ipx))
        )
        assertEquals(
            IntPxSize(20.ipx, 10.ipx),
            getSize(2f, Constraints(minWidth = 5.ipx, minHeight = 10.ipx))
        )
    }

    private fun getSize(aspectRatio: Float, childContraints: Constraints): IntPxSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<IntPxSize>()
        val position = Ref<PxPosition>()
        show {
            Layout(@Composable {
                Container(
                    Modifier.aspectRatio(aspectRatio) +
                        Modifier.saveLayoutInfo(size, position, positionedLatch)
                ) {
                }
            }) { measurables, incomingConstraints, _ ->
                require(measurables.isNotEmpty())
                val placeable = measurables.first().measure(childContraints)
                layout(incomingConstraints.maxWidth, incomingConstraints.maxHeight) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        return size.value!!
    }
}