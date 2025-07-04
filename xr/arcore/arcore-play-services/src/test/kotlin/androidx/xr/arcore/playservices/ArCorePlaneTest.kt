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

package androidx.xr.arcore.playservices

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.math.Vector2
import com.google.ar.core.Plane as ARCore1xPlane
import com.google.ar.core.Plane.Type as ARCore1xPlaneType
import com.google.ar.core.Pose as ARCore1xPose
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.common.truth.Truth.assertThat
import java.nio.FloatBuffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCorePlaneTest {

    private lateinit var mockPlane: ARCore1xPlane
    lateinit private var underTest: ArCorePlane

    @Before
    fun setUp() {
        mockPlane = mock<ARCore1xPlane>()
        underTest = ArCorePlane(mockPlane, XrResources())
    }

    @Test
    fun centerPose_callsArCore1xPlaneCenterPose() {
        val pose =
            ARCore1xPose(floatArrayOf(1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))
        whenever(mockPlane.centerPose).thenReturn(pose)

        assertThat(underTest.centerPose).isEqualTo(pose.toRuntimePose())

        verify(mockPlane).centerPose
    }

    @Test
    fun extents_callsArCore1xPlaneExtent() {
        whenever(mockPlane.extentX).thenReturn(1.0f)
        whenever(mockPlane.extentZ).thenReturn(2.0f)

        assertThat(underTest.extents).isEqualTo(Vector2(1.0f, 2.0f))

        verify(mockPlane).extentX
        verify(mockPlane).extentZ
    }

    @Test
    fun label_returnsUnknown() {
        assertThat(underTest.label).isEqualTo(Plane.Label.UNKNOWN)
    }

    @Test
    fun subsumedPlane_callsArCore1xPlaneSubsumedBy() {
        assertThat(underTest.subsumedBy).isNull()

        verify(mockPlane).subsumedBy
    }

    @Test
    fun trackingState_callsArCore1xPlaneTrackingState() {
        whenever(mockPlane.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)

        verify(mockPlane).trackingState
    }

    @Test
    fun type_callsArCore1xPlaneType() {
        whenever(mockPlane.type).thenReturn(ARCore1xPlaneType.HORIZONTAL_DOWNWARD_FACING)

        assertThat(underTest.type).isEqualTo(Plane.Type.HORIZONTAL_DOWNWARD_FACING)

        verify(mockPlane).type
    }

    @Test
    fun vertices_callsArCore1xPlanePolygon() {
        whenever(mockPlane.polygon)
            .thenReturn(FloatBuffer.wrap(floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)))

        assertThat(underTest.vertices).isEqualTo(listOf(Vector2(1.0f, 2.0f), Vector2(3.0f, 4.0f)))

        verify(mockPlane).polygon
    }
}
