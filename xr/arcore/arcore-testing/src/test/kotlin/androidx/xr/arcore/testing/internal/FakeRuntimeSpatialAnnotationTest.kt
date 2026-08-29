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

package androidx.xr.arcore.testing.internal

import androidx.kruth.assertThat
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeSpatialAnnotationTest {

    private val TEST_ANNOTATION_ID = SpatialAnnotationId.fromString("test_annotation")

    @Test
    fun constructor_withDefaultArguments_isTracked() {
        val underTest = FakeRuntimeSpatialAnnotation(id = TEST_ANNOTATION_ID)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.id).isEqualTo(TEST_ANNOTATION_ID)
    }

    @Test
    fun constructor_withDefaultArguments_hasDefaultPoses() {
        val underTest = FakeRuntimeSpatialAnnotation(id = TEST_ANNOTATION_ID)

        assertThat(underTest.centerPose).isEqualTo(Pose())
        assertThat(underTest.alignment).isNull()
        assertThat(underTest.quad).isNotNull()
        assertThat(underTest.quad!!.upperLeft).isEqualTo(Vector2(0f, 0f))
        assertThat(underTest.quad!!.upperRight).isEqualTo(Vector2(1f, 0f))
        assertThat(underTest.quad!!.lowerRight).isEqualTo(Vector2(1f, 1f))
        assertThat(underTest.quad!!.lowerLeft).isEqualTo(Vector2(0f, 1f))
    }
}
