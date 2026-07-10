/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

enum class ArrangementTestDirection {
    Horizontal,
    Vertical,
    AxisIndependent,
}

/** Tests for [SpatialArrangement]. */
@RunWith(Parameterized::class)
class SpatialArrangementTest(
    private val name: String,
    private val testDirection: ArrangementTestDirection,
    private val horizontalTestParams: HorizontalTestParams,
    private val verticalTestParams: VerticalTestParams,
) {
    private val testDensity = Density(density = 1f, fontScale = 1f)

    @Suppress("ArrayInDataClass")
    data class HorizontalTestParams(
        val actualArrangement: SpatialArrangement.Horizontal = SpatialArrangement.Start,
        val actualTotalSize: Int = 0,
        val actualSizes: IntArray = intArrayOf(),
        val actualLayoutDirection: LayoutDirection = LayoutDirection.Ltr,
        val actualOutPositions: IntArray = intArrayOf(),
        val expectedOutPositions: IntArray = intArrayOf(),
    )

    @Suppress("ArrayInDataClass")
    data class VerticalTestParams(
        val actualArrangement: SpatialArrangement.Vertical = SpatialArrangement.Top,
        val actualTotalSize: Int = 0,
        val actualSizes: IntArray = intArrayOf(),
        val actualOutPositions: IntArray = intArrayOf(),
        val expectedOutPositions: IntArray = intArrayOf(),
    )

    @Test
    fun test_arrangement() {
        when (testDirection) {
            ArrangementTestDirection.Horizontal -> {
                horizontal_test_arrangement(horizontalTestParams)
            }
            ArrangementTestDirection.Vertical -> {
                vertical_test_arrangement(verticalTestParams)
            }
            ArrangementTestDirection.AxisIndependent -> {
                horizontal_test_arrangement(horizontalTestParams)
                vertical_test_arrangement(verticalTestParams)
            }
        }
    }

    fun horizontal_test_arrangement(horizontalTestParams: HorizontalTestParams) {
        with(horizontalTestParams) {
            with(actualArrangement) {
                testDensity.arrange(
                    actualTotalSize,
                    actualSizes,
                    actualLayoutDirection,
                    actualOutPositions,
                )
                assertThat(actualOutPositions).isEqualTo(expectedOutPositions)
            }
        }
    }

    fun vertical_test_arrangement(verticalTestParams: VerticalTestParams) {
        with(verticalTestParams) {
            with(actualArrangement) {
                testDensity.arrange(actualTotalSize, actualSizes, actualOutPositions)
                assertThat(actualOutPositions).isEqualTo(expectedOutPositions)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data() =
            listOf(
                // Tests for SpatialArrangement.Start
                arrayOf(
                    "start_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 20, 45),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(95, 80, 55),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(80, 45, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 70, 105),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "start_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Start,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(75, 30, -5),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.End
                arrayOf(
                    "end_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(45, 60, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(55, 40, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(80, 45, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 50, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "end_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.End,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(95, 50, 15),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Top
                arrayOf(
                    "top_noChildren",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Top,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "top_oneChild",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Top,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                ),
                arrayOf(
                    "top_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Top,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 20, 45),
                    ),
                ),
                arrayOf(
                    "top_multipleChildren_exactSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Top,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "top_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Top,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 70, 105),
                    ),
                ),
                // Tests for SpatialArrangement.Bottom
                arrayOf(
                    "bottom_noChildren",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Bottom,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "bottom_oneChild",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Bottom,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                ),
                arrayOf(
                    "bottom_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Bottom,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(45, 60, 85),
                    ),
                ),
                arrayOf(
                    "bottom_multipleChildren_exactSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Bottom,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "bottom_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Vertical,
                    HorizontalTestParams(),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Bottom,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 50, 85),
                    ),
                ),
                // Tests for SpatialArrangement.Center
                arrayOf(
                    "center_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "center_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "center_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50),
                    ),
                ),
                arrayOf(
                    "center_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "center_multipleChildren_ampleSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 40, 65),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 40, 65),
                    ),
                ),
                arrayOf(
                    "center_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(75, 60, 35),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "center_multipleChildren_exactSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "center_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(80, 45, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "center_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 60, 95),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 60, 95),
                    ),
                ),
                arrayOf(
                    "center_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(85, 40, 5),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.SpaceBetween
                arrayOf(
                    "spaceBetween_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "spaceBetween_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceBetween_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10), // Placed at the Start
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10), // Placed at the Top
                    ),
                ),
                arrayOf(
                    "spaceBetween_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceBetween_multipleChildren_ampleSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 40, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 40, 85),
                    ),
                ),
                arrayOf(
                    "spaceBetween_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(95, 60, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceBetween_multipleChildren_exactSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "spaceBetween_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(80, 45, 15),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceBetween_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 60, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 60, 85),
                    ),
                ),
                arrayOf(
                    "spaceBetween_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(75, 40, 15),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.SpaceAround
                arrayOf(
                    "spaceAround_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "spaceAround_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceAround_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                ),
                arrayOf(
                    "spaceAround_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceAround_multipleChildren_ampleSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 120, // Child size=60, free=60, N=3, gap=20
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 50, 95),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 120, // Child size=60, free=60, N=3, gap=20
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 50, 95),
                    ),
                ),
                arrayOf(
                    "spaceAround_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 120, // Child size=60, free=60, N=3, gap=20
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(105, 70, 25),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceAround_multipleChildren_exactSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100, // Gap is 0, behaves like Start
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100, // Gap is 0, behaves like Top
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "spaceAround_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions =
                            intArrayOf(80, 45, 15), // Gap is 0, behaves like Start (RTL)
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceAround_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 90, // Child size=120, free=-30, N=3, gap=-10
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 80),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 90, // Child size=120, free=-30, N=3, gap=-10
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 80),
                    ),
                ),
                arrayOf(
                    "spaceAround_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceAround,
                        actualTotalSize = 90, // Child size=120, free=-30, N=3, gap=-10
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(70, 35, 10),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.SpaceEvenly
                arrayOf(
                    "spaceEvenly_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "spaceEvenly_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceEvenly_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                ),
                arrayOf(
                    "spaceEvenly_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceEvenly_multipleChildren_ampleSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 150, // Child size=60, free=90, N+1=4, gap=22.5~23
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(28, 65, 113),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 150, // Child size=60, free=90, N+1=4, gap=22.5~23
                        actualSizes = intArrayOf(10, 20, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(28, 65, 113),
                    ),
                ),
                arrayOf(
                    "spaceEvenly_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 150, // Child size=60, free=90, N+1=4, gap=22.5~23
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(123, 85, 38),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceEvenly_multipleChildren_exactSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100, // Gap is 0, behaves like Start
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100, // Gap is 0, behaves like Top
                        actualSizes = intArrayOf(40, 30, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                ),
                arrayOf(
                    "spaceEvenly_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions =
                            intArrayOf(80, 45, 15), // Gap is 0, behaves like Start (RTL)
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spaceEvenly_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100, // Child size=120, free=-20, N+1=4, gap=-5
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 60, 90),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100, // Child size=120, free=-20, N+1=4, gap=-5
                        actualSizes = intArrayOf(50, 40, 30),
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 60, 90),
                    ),
                ),
                arrayOf(
                    "spaceEvenly_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.SpaceEvenly,
                        actualTotalSize = 100, // Child size=120, free=-20, N+1=4, gap=-5
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(80, 40, 10),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.spacedBy
                arrayOf(
                    "spacedBy_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spacedBy_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Centered
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Centered
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Centered
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spacedBy_multipleChildren",
                    ArrangementTestDirection.AxisIndependent,
                    // Group size = 20 + 10(space) + 30 = 60. Free space = 100 - 60 = 40.
                    // Group is centered, so offset is 40 / 2 = 20.
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65), // [30, 65]
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65), // [30, 65]
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_multipleChildren",
                    ArrangementTestDirection.Horizontal,
                    // Group size is 60. Free space 40. Offset 20.
                    // Children are reversed. Child 2 (30) then Child 1 (20).
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.spacedBy(10.dp),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        // relative positions: child2 at 15, child1 at 30+10+10=50
                        // final positions: [20+50, 20+15] -> [70, 35]
                        expectedOutPositions = intArrayOf(70, 35),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.spacedBy with Alignment
                arrayOf(
                    "spacedBy_horizontal_start_and_vertical_top",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 45),
                    ),
                    VerticalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.Top),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 45),
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_horizontal_start",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(90, 55),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spacedBy_horizontal_and_vertical_center",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.CenterHorizontally),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65),
                    ),
                    VerticalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.CenterVertically),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65),
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.CenterHorizontally),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(70, 35),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "spacedBy_horizontal_end_and_vertical_bottom",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.End),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(50, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.Bottom),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(50, 85),
                    ),
                ),
                arrayOf(
                    "spacedBy_rtl_horizontal_end",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.spacedBy(10.dp, SpatialAlignment.End),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(50, 15),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.aligned
                arrayOf(
                    "aligned_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Top),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "aligned_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Top),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "aligned_multipleChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Top),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_multipleChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(90, 65),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.aligned with Alignment
                arrayOf(
                    "aligned_horizontal_start_and_vertical_top",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Top),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_horizontal_start",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Start),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(90, 65),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "aligned_horizontal_and_vertical_center",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.aligned(SpatialAlignment.CenterHorizontally),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(35, 60),
                    ),
                    VerticalTestParams(
                        actualArrangement =
                            SpatialArrangement.aligned(SpatialAlignment.CenterVertically),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(35, 60),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.aligned(SpatialAlignment.CenterHorizontally),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(65, 40),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "aligned_horizontal_end_and_vertical_bottom",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.End),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(60, 85),
                    ),
                    VerticalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.Bottom),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(60, 85),
                    ),
                ),
                arrayOf(
                    "aligned_rtl_horizontal_end",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.aligned(SpatialAlignment.End),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(40, 15),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.Left
                arrayOf(
                    "absoluteLeft_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 20, 45),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 20, 45),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 70, 105),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteLeft_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Left,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 70, 105),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.Right
                arrayOf(
                    "absoluteRight_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(45, 60, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(45, 60, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 50, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteRight_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Right,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 50, 85),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.Center
                arrayOf(
                    "absoluteCenter_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 40, 65),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 40, 65),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 60, 95),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteCenter_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.Center,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 60, 95),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.SpaceBetween
                arrayOf(
                    "absoluteSpaceBetween_noChildren",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_oneChild",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10), // Placed at the Start
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(10),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_multipleChildren_ampleSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 40, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(5, 40, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_multipleChildren_exactSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.AxisIndependent,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 60, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceBetween_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceBetween,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(25, 60, 85),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.SpaceAround
                arrayOf(
                    "absoluteSpaceAround_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 120, // Child size=60, free=60, N=3, gap=20
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 50, 95),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 120, // Child size=60, free=60, N=3, gap=20
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(15, 50, 95),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100, // Gap is 0, behaves like Start
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 100, // Gap is 0, behaves like Start (RTL)
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 90, // Child size=120, free=-30, N=3, gap=-10
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 80),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceAround_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceAround,
                        actualTotalSize = 90, // Child size=120, free=-30, N=3, gap=-10
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 80),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.SpaceEvenly
                arrayOf(
                    "absoluteSpaceEvenly_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_rtl_noChildren",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(0),
                        expectedOutPositions = intArrayOf(),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_rtl_oneChild",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(1),
                        expectedOutPositions = intArrayOf(50), // Behaves like Center
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 150, // Child size=60, free=90, N+1=4, gap=22.5~23
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(28, 65, 113),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_rtl_multipleChildren_ampleSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 150, // Child size=60, free=90, N+1=4, gap=22.5~23
                        actualSizes = intArrayOf(10, 20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(28, 65, 113),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100, // Gap is 0, behaves like Start
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_rtl_multipleChildren_exactSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100, // Gap is 0, behaves like Start (RTL)
                        actualSizes = intArrayOf(40, 30, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 55, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100, // Child size=120, free=-20, N+1=4, gap=-5
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 60, 90),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absoluteSpaceEvenly_rtl_multipleChildren_constrainedSpace",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement = SpatialArrangement.Absolute.SpaceEvenly,
                        actualTotalSize = 100, // Child size=120, free=-20, N+1=4, gap=-5
                        actualSizes = intArrayOf(50, 40, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(3),
                        expectedOutPositions = intArrayOf(20, 60, 90),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.spacedBy
                arrayOf(
                    "absolute_spacedBy_horizontal_left",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAbsoluteAlignment.Left,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 45),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_spacedBy_rtl_horizontal_left",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAbsoluteAlignment.Left,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 45),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_spacedBy_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAlignment.CenterHorizontally,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_spacedBy_rtl_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAlignment.CenterHorizontally,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(30, 65),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_spacedBy_horizontal_right",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAbsoluteAlignment.Right,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(50, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_spacedBy_rtl_horizontal_right",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.spacedBy(
                                10.dp,
                                SpatialAbsoluteAlignment.Right,
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(50, 85),
                    ),
                    VerticalTestParams(),
                ),
                // Tests for SpatialArrangement.Absolute.aligned
                arrayOf(
                    "absolute_aligned_horizontal_left",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(SpatialAbsoluteAlignment.Left),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_aligned_rtl_horizontal_left",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(SpatialAbsoluteAlignment.Left),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(10, 35),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_aligned_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(
                                SpatialAlignment.CenterHorizontally
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(35, 60),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_aligned_rtl_horizontal_center",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(
                                SpatialAlignment.CenterHorizontally
                            ),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(35, 60),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_aligned_horizontal_right",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(SpatialAbsoluteAlignment.Right),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Ltr,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(60, 85),
                    ),
                    VerticalTestParams(),
                ),
                arrayOf(
                    "absolute_aligned_rtl_horizontal_right",
                    ArrangementTestDirection.Horizontal,
                    HorizontalTestParams(
                        actualArrangement =
                            SpatialArrangement.Absolute.aligned(SpatialAbsoluteAlignment.Right),
                        actualTotalSize = 100,
                        actualSizes = intArrayOf(20, 30),
                        actualLayoutDirection = LayoutDirection.Rtl,
                        actualOutPositions = IntArray(2),
                        expectedOutPositions = intArrayOf(60, 85),
                    ),
                    VerticalTestParams(),
                ),
            )
    }
}
