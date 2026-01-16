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
    }

    private class TestSubspacePlacementScope(override val parentLayoutDirection: LayoutDirection) :
        SubspacePlaceable.SubspacePlacementScope()

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
