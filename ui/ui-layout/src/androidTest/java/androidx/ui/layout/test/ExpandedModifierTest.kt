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

import android.content.res.Resources
import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ExpandedModifierTest : LayoutTest() {
    private val Width = 100.dp
    private val Height = 100.dp

    @Test
    fun testExpandedModifier_correctSize() = with(density) {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        Assert.assertEquals(
            IntPxSize(Width.toIntPx(), Height.toIntPx()),
            getSize()
        )
        Assert.assertEquals(
            IntPxSize(screenWidth.ipx, Height.toIntPx()),
            getSize(LayoutWidth.Fill)
        )
        Assert.assertEquals(
            IntPxSize(Width.toIntPx(), screenHeight.ipx),
            getSize(LayoutHeight.Fill)
        )
        Assert.assertEquals(
            IntPxSize(screenWidth.ipx, screenHeight.ipx),
            getSize(LayoutSize.Fill)
        )
    }

    @Test
    fun testExpandedModifier_noChangeIntrinsicMeasurements() = with(density) {
        verifyIntrinsicMeasurements(LayoutWidth.Fill)
        verifyIntrinsicMeasurements(LayoutHeight.Fill)
        verifyIntrinsicMeasurements(LayoutSize.Fill)
    }

    private fun getSize(modifier: Modifier = Modifier.None): IntPxSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<IntPxSize>()
        val position = Ref<PxPosition>()
        show {
            Layout(@Composable {
                Align(alignment = Alignment.TopStart) {
                    Container(modifier = modifier) {
                        Container(width = Width, height = Height) { }
                        SaveLayoutInfo(size, position, positionedLatch)
                    }
                }
            }) { measurables, incomingConstraints ->
                require(measurables.isNotEmpty())
                val placeable = measurables.first().measure(incomingConstraints)
                layout(
                    min(placeable.width, incomingConstraints.maxWidth),
                    min(placeable.height, incomingConstraints.maxHeight)
                ) {
                    placeable.place(IntPx.Zero, IntPx.Zero)
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)
        return size.value!!
    }

    private fun verifyIntrinsicMeasurements(expandedModifier: Modifier) = with(density) {
        // intrinsic measurements do not change with the ExpandedModifier
        testIntrinsics(@Composable {
            Container(
                expandedModifier + LayoutAspectRatio(2f),
                width = 30.dp, height = 40.dp) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Width
            assertEquals(40.ipx, minIntrinsicWidth(20.ipx))
            assertEquals(30.dp.toIntPx(), minIntrinsicWidth(IntPx.Infinity))

            assertEquals(40.ipx, maxIntrinsicWidth(20.ipx))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(IntPx.Infinity))

            // Height
            assertEquals(20.ipx, minIntrinsicHeight(40.ipx))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(IntPx.Infinity))

            assertEquals(20.ipx, maxIntrinsicHeight(40.ipx))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(IntPx.Infinity))
        }
    }
}