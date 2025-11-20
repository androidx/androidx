/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialAlignment]. */
@RunWith(AndroidJUnit4::class)
class SpatialAlignmentTest {
    private val contentDimension = 10
    private val spaceDimension = 50
    private val contentSize = IntVolumeSize(contentDimension, contentDimension, contentDimension)
    private val spaceSize = IntVolumeSize(spaceDimension, spaceDimension, spaceDimension)

    // Expected offsets for bias values -1, 0, 1 with content=10, space=50
    private val offsetBiasNegativeOne = -20
    private val offsetBiasZero = 0
    private val offsetBiasOne = 20

    @Test
    fun spatialAlignment_TopStart() {
        val alignment = SpatialAlignment.TopStart // Bias H:-1, V:1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
    }

    @Test
    fun spatialAlignment_TopCenter() {
        val alignment = SpatialAlignment.TopCenter // Bias H:0, V:1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasZero)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasZero)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasZero.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasZero.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
    }

    @Test
    fun spatialAlignment_TopEnd() {
        val alignment = SpatialAlignment.TopEnd // Bias H:1, V:1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_CenterStart() {
        val alignment = SpatialAlignment.CenterStart // Bias H:-1, V:0, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            )
    }

    @Test
    fun spatialAlignment_Center() {
        val alignment = SpatialAlignment.Center // Bias H:0, V:0, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasZero)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasZero)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_CenterEnd() {
        val alignment = SpatialAlignment.CenterEnd // Bias H:1, V:0, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_BottomStart() {
        val alignment = SpatialAlignment.BottomStart // Bias H:-1, V:-1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_BottomCenter() {
        val alignment = SpatialAlignment.BottomCenter // Bias H:0, V:-1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasZero)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasZero)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasZero.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasZero.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_BottomEnd() {
        val alignment = SpatialAlignment.BottomEnd // Bias H:1, V:-1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne)
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
    }

    @Test
    fun spatialAlignment_Start() {
        val alignment = SpatialAlignment.Start // Bias H:-1, V:0, D:0
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasOne)
    }

    @Test
    fun spatialAlignment_CenterHorizontally() {
        val alignment = SpatialAlignment.CenterHorizontally // Bias H:0, V:0, D:0
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasZero)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasZero)
    }

    @Test
    fun spatialAlignment_End() {
        val alignment = SpatialAlignment.End // Bias H:1, V:0, D:0
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasNegativeOne)
    }

    @Test
    fun spatialAlignment_Bottom() {
        val alignment = SpatialAlignment.Bottom // Bias H:0, V:-1, D:0
        // Vertical Offset
        assertThat(alignment.offset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
    }

    @Test
    fun spatialAlignment_CenterVertically() {
        val alignment = SpatialAlignment.CenterVertically // Bias H:0, V:0, D:0
        // Vertical Offset
        assertThat(alignment.offset(contentDimension, spaceDimension)).isEqualTo(offsetBiasZero)
    }

    @Test
    fun spatialAlignment_Top() {
        val alignment = SpatialAlignment.Top // Bias H:0, V:1, D:0
        // Vertical Offset
        assertThat(alignment.offset(contentDimension, spaceDimension)).isEqualTo(offsetBiasOne)
    }

    @Test
    fun spatialAlignment_Back() {
        val alignment = SpatialAlignment.Back // Bias H:0, V:0, D:-1
        // Depth Offset
        assertThat(alignment.offset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
    }

    @Test
    fun spatialAlignment_CenterDepthwise() {
        val alignment = SpatialAlignment.CenterDepthwise // Bias H:0, V:0, D:0
        // Depth Offset
        assertThat(alignment.offset(contentDimension, spaceDimension)).isEqualTo(offsetBiasZero)
    }

    @Test
    fun spatialAlignment_Front() {
        val alignment = SpatialAlignment.Front // Bias H:0, V:0, D:1
        // Depth Offset
        assertThat(alignment.offset(contentDimension, spaceDimension)).isEqualTo(offsetBiasOne)
    }

    @Test
    fun spatialBiasAlignment_copy() {
        val original = SpatialBiasAlignment(0.5f, 0.25f, -0.5f)

        // Copy with default parameters (should be equal to original)
        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        // Copy with new horizontalBias
        val copyHorizontal = original.copy(horizontalBias = -0.2f)
        assertThat(copyHorizontal).isNotEqualTo(original)
        assertThat(copyHorizontal.horizontalBias).isEqualTo(-0.2f)

        // Copy with new verticalBias
        val copyVertical = original.copy(verticalBias = -0.75f)
        assertThat(copyVertical).isNotEqualTo(original)
        assertThat(copyVertical.verticalBias).isEqualTo(-0.75f)

        // Copy with new depthBias
        val copyDepth = original.copy(depthBias = 0.1f)
        assertThat(copyDepth).isNotEqualTo(original)
        assertThat(copyDepth.depthBias).isEqualTo(0.1f)

        // Copy with all new biases
        val copyAll = original.copy(horizontalBias = 0.1f, verticalBias = 0.2f, depthBias = 0.3f)
        assertThat(copyAll).isNotEqualTo(original)
        assertThat(copyAll.horizontalBias).isEqualTo(0.1f)
        assertThat(copyAll.verticalBias).isEqualTo(0.2f)
        assertThat(copyAll.depthBias).isEqualTo(0.3f)
    }

    @Test
    fun spatialBiasAlignment_equals() {
        val alignment1 = SpatialBiasAlignment(0.5f, 0.25f, -0.5f)
        val alignment2 = SpatialBiasAlignment(0.5f, 0.25f, -0.5f) // Same as 1
        val alignment3 = SpatialBiasAlignment(0.1f, 0.25f, -0.5f) // Different H
        val alignment4 = SpatialBiasAlignment(0.5f, 0.1f, -0.5f) // Different V
        val alignment5 = SpatialBiasAlignment(0.5f, 0.25f, 0.1f) // Different D

        assertThat(alignment1).isEqualTo(alignment2)
        assertThat(alignment1).isNotEqualTo(alignment3)
        assertThat(alignment1).isNotEqualTo(alignment4)
        assertThat(alignment1).isNotEqualTo(alignment5)
        assertThat(alignment1).isNotEqualTo(null)
        assertThat(alignment1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAlignment_hashCode() {
        val alignment1 = SpatialBiasAlignment(0.5f, 0.25f, -0.5f)
        val alignment2 = SpatialBiasAlignment(0.5f, 0.25f, -0.5f) // Same as 1
        val alignment3 = SpatialBiasAlignment(0.1f, 0.25f, -0.5f) // Different H
        val alignment4 = SpatialBiasAlignment(0.5f, 0.1f, -0.5f) // Different V
        val alignment5 = SpatialBiasAlignment(0.5f, 0.25f, 0.1f) // Different D

        assertThat(alignment1.hashCode()).isEqualTo(alignment2.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment3.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment4.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment5.hashCode())
    }

    @Test
    fun spatialBiasAlignment_toString() {
        val alignment = SpatialBiasAlignment(0.5f, 0.25f, -0.5f)
        assertThat(alignment.toString())
            .isEqualTo(
                "SpatialBiasAlignment(horizontalBias=${0.5f}, verticalBias=${0.25f}, depthBias=${-0.5f})"
            )
    }

    @Test
    fun spatialBiasAlignment_Horizontal_equals() {
        val horizontal1 = SpatialBiasAlignment.Horizontal(0.5f)
        val horizontal2 = SpatialBiasAlignment.Horizontal(0.5f) // Same bias
        val horizontal3 = SpatialBiasAlignment.Horizontal(-0.5f) // Different bias

        assertThat(horizontal1).isEqualTo(horizontal2)
        assertThat(horizontal1).isNotEqualTo(horizontal3)
        assertThat(horizontal1).isNotEqualTo(null)
        assertThat(horizontal1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAlignment_Horizontal_hashCode() {
        val horizontal1 = SpatialBiasAlignment.Horizontal(0.5f)
        val horizontal2 = SpatialBiasAlignment.Horizontal(0.5f) // Same bias
        val horizontal3 = SpatialBiasAlignment.Horizontal(-0.5f) // Different bias

        assertThat(horizontal1.hashCode()).isEqualTo(horizontal2.hashCode())
        assertThat(horizontal1.hashCode()).isNotEqualTo(horizontal3.hashCode())
    }

    @Test
    fun spatialBiasAlignment_Horizontal_toString() {
        val original = SpatialBiasAlignment.Horizontal(0.75f)
        assertThat(original.toString()).isEqualTo("Horizontal(bias=${0.75f})")
    }

    @Test
    fun spatialBiasAlignment_Horizontal_copy() {
        val original = SpatialBiasAlignment.Horizontal(0.5f)

        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        val copyNewBias = original.copy(bias = -0.25f)
        assertThat(copyNewBias).isNotEqualTo(original)
        assertThat(copyNewBias.bias).isEqualTo(-0.25f)
    }

    @Test
    fun spatialBiasAlignment_Vertical_equals() {
        val vertical1 = SpatialBiasAlignment.Vertical(0.5f)
        val vertical2 = SpatialBiasAlignment.Vertical(0.5f) // Same bias
        val vertical3 = SpatialBiasAlignment.Vertical(-0.5f) // Different bias

        assertThat(vertical1).isEqualTo(vertical2)
        assertThat(vertical1).isNotEqualTo(vertical3)
        assertThat(vertical1).isNotEqualTo(null)
        assertThat(vertical1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAlignment_Vertical_hashCode() {
        val vertical1 = SpatialBiasAlignment.Vertical(0.5f)
        val vertical2 = SpatialBiasAlignment.Vertical(0.5f) // Same bias
        val vertical3 = SpatialBiasAlignment.Vertical(-0.5f) // Different bias

        assertThat(vertical1.hashCode()).isEqualTo(vertical2.hashCode())
        assertThat(vertical1.hashCode()).isNotEqualTo(vertical3.hashCode())
    }

    @Test
    fun spatialBiasAlignment_Vertical_toString() {
        val original = SpatialBiasAlignment.Vertical(0.75f)
        assertThat(original.toString()).isEqualTo("Vertical(bias=${0.75f})")
    }

    @Test
    fun spatialBiasAlignment_Vertical_copy() {
        val original = SpatialBiasAlignment.Vertical(0.5f)

        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        val copyNewBias = original.copy(bias = -0.25f)
        assertThat(copyNewBias).isNotEqualTo(original)
        assertThat(copyNewBias.bias).isEqualTo(-0.25f)
    }

    @Test
    fun spatialBiasAlignment_Depth_equals() {
        val depth1 = SpatialBiasAlignment.Depth(0.5f)
        val depth2 = SpatialBiasAlignment.Depth(0.5f) // Same bias
        val depth3 = SpatialBiasAlignment.Depth(-0.5f) // Different bias

        assertThat(depth1).isEqualTo(depth2)
        assertThat(depth1).isNotEqualTo(depth3)
        assertThat(depth1).isNotEqualTo(null)
        assertThat(depth1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAlignment_Depth_hashCode() {
        val depth1 = SpatialBiasAlignment.Depth(0.5f)
        val depth2 = SpatialBiasAlignment.Depth(0.5f) // Same bias
        val depth3 = SpatialBiasAlignment.Depth(-0.5f) // Different bias

        assertThat(depth1.hashCode()).isEqualTo(depth2.hashCode())
        assertThat(depth1.hashCode()).isNotEqualTo(depth3.hashCode())
    }

    @Test
    fun spatialBiasAlignment_Depth_toString() {
        val original = SpatialBiasAlignment.Depth(0.75f)
        assertThat(original.toString()).isEqualTo("Depth(bias=${0.75f})")
    }

    @Test
    fun spatialBiasAlignment_Depth_copy() {
        val original = SpatialBiasAlignment.Depth(0.5f)

        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        val copyNewBias = original.copy(bias = -0.25f)
        assertThat(copyNewBias).isNotEqualTo(original)
        assertThat(copyNewBias.bias).isEqualTo(-0.25f)
    }

    // --- Tests for SpatialAbsoluteAlignment APIs ---

    @Test
    fun spatialAbsoluteAlignment_TopLeft() {
        val alignment = SpatialAbsoluteAlignment.TopLeft // Bias H:-1, V:1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_TopRight() {
        val alignment = SpatialAbsoluteAlignment.TopRight // Bias H:1, V:1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_CenterLeft() {
        val alignment = SpatialAbsoluteAlignment.CenterLeft // Bias H:-1, V:0, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_CenterRight() {
        val alignment = SpatialAbsoluteAlignment.CenterRight // Bias H:1, V:0, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_BottomLeft() {
        val alignment = SpatialAbsoluteAlignment.BottomLeft // Bias H:-1, V:-1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_BottomRight() {
        val alignment = SpatialAbsoluteAlignment.BottomRight // Bias H:1, V:-1, D:0
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAbsoluteAlignment_Left() {
        val alignment = SpatialAbsoluteAlignment.Left // Bias H:-1, V:0, D:0
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasNegativeOne)
    }

    @Test
    fun spatialAbsoluteAlignment_Right() {
        val alignment = SpatialAbsoluteAlignment.Right // Bias H:1, V:0, D:0
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasOne)
    }

    @Test
    fun spatialBiasAbsoluteAlignment_copy() {
        val original = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f)

        // Copy with default parameters (should be equal to original)
        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        // Copy with new horizontalBias
        val copyHorizontal = original.copy(horizontalBias = -0.2f)
        assertThat(copyHorizontal).isNotEqualTo(original)
        assertThat(copyHorizontal.horizontalBias).isEqualTo(-0.2f)

        // Copy with new verticalBias
        val copyVertical = original.copy(verticalBias = -0.75f)
        assertThat(copyVertical).isNotEqualTo(original)
        assertThat(copyVertical.verticalBias).isEqualTo(-0.75f)

        // Copy with new depthBias
        val copyDepth = original.copy(depthBias = 0.1f)
        assertThat(copyDepth).isNotEqualTo(original)
        assertThat(copyDepth.depthBias).isEqualTo(0.1f)

        // Copy with all new biases
        val copyAll = original.copy(horizontalBias = 0.1f, verticalBias = 0.2f, depthBias = 0.3f)
        assertThat(copyAll).isNotEqualTo(original)
        assertThat(copyAll.horizontalBias).isEqualTo(0.1f)
        assertThat(copyAll.verticalBias).isEqualTo(0.2f)
        assertThat(copyAll.depthBias).isEqualTo(0.3f)
    }

    @Test
    fun spatialBiasAbsoluteAlignment_equals() {
        val alignment1 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f)
        val alignment2 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f) // Same as 1
        val alignment3 = SpatialBiasAbsoluteAlignment(0.1f, 0.25f, -0.5f) // Different H
        val alignment4 = SpatialBiasAbsoluteAlignment(0.5f, 0.1f, -0.5f) // Different V
        val alignment5 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, 0.1f) // Different D

        assertThat(alignment1).isEqualTo(alignment2)
        assertThat(alignment1).isNotEqualTo(alignment3)
        assertThat(alignment1).isNotEqualTo(alignment4)
        assertThat(alignment1).isNotEqualTo(alignment5)
        assertThat(alignment1).isNotEqualTo(null)
        assertThat(alignment1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAbsoluteAlignment_hashCode() {
        val alignment1 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f)
        val alignment2 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f) // Same as 1
        val alignment3 = SpatialBiasAbsoluteAlignment(0.1f, 0.25f, -0.5f) // Different H
        val alignment4 = SpatialBiasAbsoluteAlignment(0.5f, 0.1f, -0.5f) // Different V
        val alignment5 = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, 0.1f) // Different D

        assertThat(alignment1.hashCode()).isEqualTo(alignment2.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment3.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment4.hashCode())
        assertThat(alignment1.hashCode()).isNotEqualTo(alignment5.hashCode())
    }

    @Test
    fun spatialBiasAbsoluteAlignment_toString() {
        val alignment = SpatialBiasAbsoluteAlignment(0.5f, 0.25f, -0.5f)
        assertThat(alignment.toString())
            .isEqualTo(
                "SpatialBiasAbsoluteAlignment(horizontalBias=${0.5f}, verticalBias=${0.25f}, depthBias=${-0.5f})"
            )
    }

    @Test
    fun spatialBiasAbsoluteAlignment_Horizontal_equals() {
        val absHorizontal1 = SpatialBiasAbsoluteAlignment.Horizontal(0.5f)
        val absHorizontal2 = SpatialBiasAbsoluteAlignment.Horizontal(0.5f) // Same bias
        val absHorizontal3 = SpatialBiasAbsoluteAlignment.Horizontal(-0.5f) // Different bias

        assertThat(absHorizontal1).isEqualTo(absHorizontal2)
        assertThat(absHorizontal1).isNotEqualTo(absHorizontal3)
        assertThat(absHorizontal1).isNotEqualTo(null)
        assertThat(absHorizontal1).isNotEqualTo(Any())
    }

    @Test
    fun spatialBiasAbsoluteAlignment_Horizontal_hashCode() {
        val absHorizontal1 = SpatialBiasAbsoluteAlignment.Horizontal(0.5f)
        val absHorizontal2 = SpatialBiasAbsoluteAlignment.Horizontal(0.5f) // Same bias
        val absHorizontal3 = SpatialBiasAbsoluteAlignment.Horizontal(-0.5f) // Different bias

        assertThat(absHorizontal1.hashCode()).isEqualTo(absHorizontal2.hashCode())
        assertThat(absHorizontal1.hashCode()).isNotEqualTo(absHorizontal3.hashCode())
    }

    @Test
    fun spatialBiasAbsoluteAlignment_Horizontal_toString() {
        val original = SpatialBiasAbsoluteAlignment.Horizontal(0.75f)
        assertThat(original.toString())
            .isEqualTo("SpatialBiasAbsoluteAlignment#Horizontal(bias=0.75)")
    }

    @Test
    fun spatialBiasAbsoluteAlignment_Horizontal_copy() {
        val original = SpatialBiasAbsoluteAlignment.Horizontal(0.75f)

        val copyDefault = original.copy()
        assertThat(copyDefault).isEqualTo(original)

        val copyNewBias = original.copy(bias = -0.25f)
        assertThat(copyNewBias).isNotEqualTo(original)
    }

    // --- Tests for SpatialAlignment.Companion (Deprecated Absolute) ---

    @Test
    fun spatialAlignment_TopLeft_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.TopLeft // Bias H:-1, V:1, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAlignment_TopRight_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.TopRight // Bias H:1, V:1, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasOne.toFloat(), offsetBiasZero.toFloat())
            ) // Absolute
    }

    @Test
    fun spatialAlignment_CenterLeft_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.CenterLeft // Bias H:-1, V:0, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAlignment_CenterRight_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.CenterRight // Bias H:1, V:0, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(offsetBiasOne.toFloat(), offsetBiasZero.toFloat(), offsetBiasZero.toFloat())
            ) // Absolute
    }

    @Test
    fun spatialAlignment_BottomLeft_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.BottomLeft // Bias H:-1, V:-1, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasNegativeOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAlignment_BottomRight_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.BottomRight // Bias H:1, V:-1, D:0 (Absolute)
        // Horizontal Offset
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Ltr)
            )
            .isEqualTo(offsetBiasOne)
        assertThat(
                alignment.horizontalOffset(contentDimension, spaceDimension, LayoutDirection.Rtl)
            )
            .isEqualTo(offsetBiasOne) // Absolute
        // Vertical Offset
        assertThat(alignment.verticalOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasNegativeOne)
        // Depth Offset
        assertThat(alignment.depthOffset(contentDimension, spaceDimension))
            .isEqualTo(offsetBiasZero)
        // Position
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Ltr))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            )
        assertThat(alignment.position(contentSize, spaceSize, LayoutDirection.Rtl))
            .isEqualTo(
                Vector3(
                    offsetBiasOne.toFloat(),
                    offsetBiasNegativeOne.toFloat(),
                    offsetBiasZero.toFloat(),
                )
            ) // Absolute
    }

    @Test
    fun spatialAlignment_Left_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.Left // Bias H:-1, V:0, D:0 (Absolute)
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasNegativeOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasNegativeOne)
    }

    @Test
    fun spatialAlignment_Right_deprecated() {
        @Suppress("DEPRECATION")
        val alignment = SpatialAlignment.Right // Bias H:1, V:0, D:0 (Absolute)
        // Horizontal Offset
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Ltr))
            .isEqualTo(offsetBiasOne)
        assertThat(alignment.offset(contentDimension, spaceDimension, LayoutDirection.Rtl))
            .isEqualTo(offsetBiasOne)
    }
}
