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
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.min
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.layout.Align
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutExpanded
import androidx.ui.layout.LayoutExpandedHeight
import androidx.ui.layout.LayoutExpandedWidth
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
    fun testExpandedModifier_correctSize() = withDensity(density) {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        Assert.assertEquals(
            Rect(0f, 0f, Width.toIntPx().value.toFloat(), Height.toIntPx().value.toFloat()),
            getSize().toRect()
        )
        Assert.assertEquals(
            Rect(0f, 0f, screenWidth, Height.toIntPx().value.toFloat()),
            getSize(LayoutExpandedWidth).toRect()
        )
        Assert.assertEquals(
            Rect(0f, 0f, Width.toIntPx().value.toFloat(), screenHeight),
            getSize(LayoutExpandedHeight).toRect()
        )
        Assert.assertEquals(
            Rect(0f, 0f, screenWidth, screenHeight),
            getSize(LayoutExpanded).toRect()
        )
    }

    @Test
    fun testExpandedModifier_noChangeIntrinsicMeasurements() = withDensity(density) {
        verifyIntrinsicMeasurements(LayoutExpandedWidth)
        verifyIntrinsicMeasurements(LayoutExpandedHeight)
        verifyIntrinsicMeasurements(LayoutExpanded)
    }

    private fun getSize(modifier: Modifier = Modifier.None): PxSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<PxSize>()
        val position = Ref<PxPosition>()
        show {
            Layout(@Composable {
                Align(alignment = Alignment.TopLeft) {
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

    private fun verifyIntrinsicMeasurements(expandedModifier: Modifier) = withDensity(density) {
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