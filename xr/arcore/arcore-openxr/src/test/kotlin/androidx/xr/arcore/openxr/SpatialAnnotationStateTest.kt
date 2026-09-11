/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialAnnotationStateTest {

    @Test
    fun constructor_noArguments_returnsDefaultState() {
        val underTest = SpatialAnnotationState()

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        assertThat(underTest.pose).isNull()
        assertThat(underTest.upperLeft).isNull()
        assertThat(underTest.upperRight).isNull()
        assertThat(underTest.lowerRight).isNull()
        assertThat(underTest.lowerLeft).isNull()
    }

    @Test
    fun constructor_allArguments_returnsExpectedState() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f))
        val ul = Vector2(-1f, 1f)
        val ur = Vector2(1f, 1f)
        val lr = Vector2(1f, -1f)
        val ll = Vector2(-1f, -1f)
        val underTest =
            SpatialAnnotationState(
                trackingState = TrackingState.TRACKING,
                pose = pose,
                upperLeft = ul,
                upperRight = ur,
                lowerRight = lr,
                lowerLeft = ll,
            )

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.pose).isEqualTo(pose)
        assertThat(underTest.upperLeft).isEqualTo(ul)
        assertThat(underTest.upperRight).isEqualTo(ur)
        assertThat(underTest.lowerRight).isEqualTo(lr)
        assertThat(underTest.lowerLeft).isEqualTo(ll)
    }
}
