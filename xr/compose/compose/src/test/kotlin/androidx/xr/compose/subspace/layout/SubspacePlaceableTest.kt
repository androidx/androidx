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
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspacePlaceableTest {

    private class TestSubspacePlaceable : SubspacePlaceable() {
        var placedPose: Pose? = null

        override fun placeAt(pose: Pose) {
            placedPose = pose
        }

        // Helper to set protected property 'measuredSize'
        fun setMeasuredSizeForTest(size: IntVolumeSize) {
            measuredWidth = size.width
            measuredHeight = size.height
            measuredDepth = size.depth
        }

        // Helper to set protected property 'measurementConstraints'
        fun setMeasurementConstraintsForTest(constraints: VolumeConstraints) {
            measurementConstraints = constraints
        }
    }

    private class TestSubspacePlacementScope(override val parentLayoutDirection: LayoutDirection) :
        SubspacePlaceable.SubspacePlacementScope()

    @Test
    fun subspacePlaceable_defaultInitialization_hasZeroDimensions() {
        val placeable = TestSubspacePlaceable()

        assertThat(placeable.width).isEqualTo(0)
        assertThat(placeable.height).isEqualTo(0)
        assertThat(placeable.depth).isEqualTo(0)
        assertThat(placeable.measuredWidth).isEqualTo(0)
        assertThat(placeable.measuredHeight).isEqualTo(0)
        assertThat(placeable.measuredDepth).isEqualTo(0)
    }

    @Test
    fun dimensions_measuredBelowMin_coercedToMin() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(5, 25, 45))

        assertThat(placeable.width).isEqualTo(10)
        assertThat(placeable.height).isEqualTo(30)
        assertThat(placeable.depth).isEqualTo(50)
        assertThat(placeable.measuredWidth).isEqualTo(5)
        assertThat(placeable.measuredHeight).isEqualTo(25)
        assertThat(placeable.measuredDepth).isEqualTo(45)
    }

    @Test
    fun dimensions_measuredAboveMax_coercedToMax() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(25, 45, 65))

        assertThat(placeable.width).isEqualTo(20)
        assertThat(placeable.height).isEqualTo(40)
        assertThat(placeable.depth).isEqualTo(60)
        assertThat(placeable.measuredWidth).isEqualTo(25)
        assertThat(placeable.measuredHeight).isEqualTo(45)
        assertThat(placeable.measuredDepth).isEqualTo(65)
    }

    @Test
    fun dimensions_measuredAtMin_isMin() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(10, 30, 50))

        assertThat(placeable.width).isEqualTo(10)
        assertThat(placeable.height).isEqualTo(30)
        assertThat(placeable.depth).isEqualTo(50)
    }

    @Test
    fun dimensions_measuredAtMax_isMax() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(20, 40, 60))

        assertThat(placeable.width).isEqualTo(20)
        assertThat(placeable.height).isEqualTo(40)
        assertThat(placeable.depth).isEqualTo(60)
    }

    @Test
    fun dimensions_measuredWithinBounds_isMeasured() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(15, 35, 55))

        assertThat(placeable.width).isEqualTo(15)
        assertThat(placeable.height).isEqualTo(35)
        assertThat(placeable.depth).isEqualTo(55)
    }

    @Test
    fun dimensions_measuredMixedOutOfBounds_coercedCorrectly() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        // W < min, H > max, D within
        placeable.setMeasuredSizeForTest(IntVolumeSize(5, 45, 55))

        assertThat(placeable.width).isEqualTo(10)
        assertThat(placeable.height).isEqualTo(40)
        assertThat(placeable.depth).isEqualTo(55)
    }

    @Test
    fun measurementConstraints_updateAfterMeasuredSizeSet_triggersReCoercion() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasuredSizeForTest(IntVolumeSize(5, 15, 25))
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 10,
                maxHeight = 20,
                minDepth = 10,
                maxDepth = 20,
            )
        )
        // Change the constraints.
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 0,
                maxWidth = 8,
                minHeight = 12,
                maxHeight = 18,
                minDepth = 22,
                maxDepth = 30,
            )
        )

        assertThat(placeable.width).isEqualTo(5)
        assertThat(placeable.height).isEqualTo(15)
        assertThat(placeable.depth).isEqualTo(25)
    }

    @Test
    fun measuredSize_updateAfterConstraintsSet_triggersReCoercion() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 10,
                maxHeight = 20,
                minDepth = 10,
                maxDepth = 20,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(5, 15, 25))
        // Change measured size.
        placeable.setMeasuredSizeForTest(IntVolumeSize(12, 22, 8))

        assertThat(placeable.width).isEqualTo(12)
        assertThat(placeable.height).isEqualTo(20)
        assertThat(placeable.depth).isEqualTo(10)
    }

    @Test
    fun subspacePlaceable_setConstraintsAfterMeasuredSize_coercesCorrectly() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasuredSizeForTest(IntVolumeSize(5, 15, 25))
        // Set constraints after measured size
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 10,
                maxHeight = 20,
                minDepth = 10,
                maxDepth = 20,
            )
        )

        assertThat(placeable.width).isEqualTo(10)
        assertThat(placeable.height).isEqualTo(15)
        assertThat(placeable.depth).isEqualTo(20)
    }

    @Test
    fun dimensions_zeroMinMaxConstraints_coercedToZero() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 0,
                maxWidth = 0,
                minHeight = 0,
                maxHeight = 0,
                minDepth = 0,
                maxDepth = 0,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(10, 10, 10))

        assertThat(placeable.width).isEqualTo(0)
        assertThat(placeable.height).isEqualTo(0)
        assertThat(placeable.depth).isEqualTo(0)
    }

    @Test
    fun dimensions_zeroMeasuredSize_coercedToMinConstraints() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 10,
                maxWidth = 20,
                minHeight = 30,
                maxHeight = 40,
                minDepth = 50,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize.Zero)

        assertThat(placeable.width).isEqualTo(10)
        assertThat(placeable.height).isEqualTo(30)
        assertThat(placeable.depth).isEqualTo(50)
    }

    @Test
    fun dimensions_unboundedConstraints_isMeasured() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(VolumeConstraints())
        placeable.setMeasuredSizeForTest(IntVolumeSize(100, 200, 300))

        assertThat(placeable.width).isEqualTo(100)
        assertThat(placeable.height).isEqualTo(200)
        assertThat(placeable.depth).isEqualTo(300)
    }

    @Test
    fun dimensions_negativeMeasuredSize_coercedToMinConstraintOrZero() {
        val placeable = TestSubspacePlaceable()
        placeable.setMeasurementConstraintsForTest(
            VolumeConstraints(
                minWidth = 0,
                maxWidth = 20,
                minHeight = 0,
                maxHeight = 40,
                minDepth = 0,
                maxDepth = 60,
            )
        )
        placeable.setMeasuredSizeForTest(IntVolumeSize(-10, -20, -30))

        assertThat(placeable.width).isEqualTo(0)
        assertThat(placeable.height).isEqualTo(0)
        assertThat(placeable.depth).isEqualTo(0)
    }

    @Test
    fun place_callsPlaceAtWithCorrectPose() {
        val placeable = TestSubspacePlaceable()
        val scope = TestSubspacePlacementScope(LayoutDirection.Ltr)
        val pose = Pose()

        with(scope) { placeable.place(pose) }

        assertThat(placeable.placedPose).isEqualTo(pose)
    }

    @Test
    fun placeRelative_ltr_callsPlaceAtWithOriginalPose() {
        val placeable = TestSubspacePlaceable()
        val scope = TestSubspacePlacementScope(LayoutDirection.Ltr)
        val pose = Pose(translation = Vector3(1f, 2f, 3f))

        with(scope) { placeable.placeRelative(pose) }

        assertThat(placeable.placedPose).isEqualTo(pose)
    }

    @Test
    fun placeRelative_rtl_callsPlaceAtWithMirroredPose() {
        val placeable = TestSubspacePlaceable()
        val scope = TestSubspacePlacementScope(LayoutDirection.Rtl)
        val originalPose =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.1f, 0.2f, 0.3f, 0.9f))

        with(scope) { placeable.placeRelative(originalPose) }

        val placedPose = assertNotNull(placeable.placedPose)

        // Translation x is negated
        assertThat(placedPose.translation.x).isEqualTo(-originalPose.translation.x)
        assertThat(placedPose.translation.y).isEqualTo(originalPose.translation.y)
        assertThat(placedPose.translation.z).isEqualTo(originalPose.translation.z)

        // Rotation y and z components are negated.
        assertThat(placedPose.rotation.x).isWithin(1e-6f).of(originalPose.rotation.x)
        assertThat(placedPose.rotation.y).isWithin(1e-6f).of(-originalPose.rotation.y)
        assertThat(placedPose.rotation.z).isWithin(1e-6f).of(-originalPose.rotation.z)
        assertThat(placedPose.rotation.w).isWithin(1e-6f).of(originalPose.rotation.w)
    }
}
