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
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenXrSpatialAnnotationTest {

    private val handle = 42L
    private val id = SpatialAnnotationId.fromString("test_annotation")

    @Test
    fun constructor_defaultValues_initializesExpectedState() {
        val underTest = OpenXrSpatialAnnotation(handle, id)

        assertThat(underTest.nativeSpatialAnnotationId).isEqualTo(handle)
        assertThat(underTest.id).isEqualTo(id)
        assertThat(underTest.alignment).isNull()
        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        assertThat(underTest.centerPose).isEqualTo(Pose())
        assertThat(underTest.quad).isNull()
    }

    @Test
    fun constructor_withAlignment_setsAlignmentProperty() {
        val underTest = OpenXrSpatialAnnotation(handle, id, SpatialAnnotationQuadAlignment.SCREEN)

        assertThat(underTest.alignment).isEqualTo(SpatialAnnotationQuadAlignment.SCREEN)
    }

    @Test
    fun update_invokesNativeMethod() {
        val underTest = OpenXrSpatialAnnotation(handle, id)

        underTest.update(1000L)
    }
}
