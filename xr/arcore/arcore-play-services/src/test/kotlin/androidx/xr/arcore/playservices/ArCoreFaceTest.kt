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

package androidx.xr.arcore.playservices

import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.TrackingState
import com.google.ar.core.AugmentedFace as ArCore1xAugmentedFace
import com.google.ar.core.Pose as ArCore1xPose
import com.google.ar.core.TrackingState as ArCore1xTrackingState
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreFaceTest {

    private lateinit var mockFace: ArCore1xAugmentedFace
    private lateinit var underTest: ArCoreFace

    @Before
    fun setUp() {
        mockFace = mock<ArCore1xAugmentedFace>()
        underTest = ArCoreFace(mockFace)
    }

    @Test
    fun trackingState_returns_ArCoreAugmentedFace_TrackingState_PAUSED() {
        whenever(mockFace.trackingState).thenReturn(ArCore1xTrackingState.STOPPED)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.STOPPED)
        verify(mockFace).trackingState
    }

    @Test
    fun trackingState_returns_ArCoreAugmentedFace_TrackingState_STOPPED() {
        whenever(mockFace.trackingState).thenReturn(ArCore1xTrackingState.PAUSED)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.PAUSED)
        verify(mockFace).trackingState
    }

    @Test
    fun trackingState_returns_ArCoreAugmentedFace_TrackingState_TRACKING() {
        whenever(mockFace.trackingState).thenReturn(ArCore1xTrackingState.TRACKING)

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
        verify(mockFace).trackingState
    }

    @Test
    fun centerPose_returns_ArCoreAugmentedFace_centerPose() {
        val arCorePose = ArCore1xPose(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 1f, 1f, 1f))
        whenever(mockFace.centerPose).thenReturn(arCorePose)

        assertThat(underTest.centerPose).isEqualTo(arCorePose.toRuntimePose())
        verify(mockFace).centerPose
    }

    @Test
    fun meshTriangleIndices_returns_ArCoreAugmentedFace_meshTriangleIndices() {
        val indices = ShortBuffer.allocate(3)
        indices.put(8)
        indices.put(8)
        indices.put(8)
        whenever(mockFace.meshTriangleIndices).thenReturn(indices)

        assertThat(underTest.mesh.triangleIndices).isEqualTo(indices)
        verify(mockFace).meshTriangleIndices
    }

    @Test
    fun meshNormals_returns_ArCoreAugmentedFace_meshNormals() {
        val normals = FloatBuffer.allocate(3)
        normals.put(8f)
        normals.put(8f)
        normals.put(8f)
        whenever(mockFace.meshNormals).thenReturn(normals)

        assertThat(underTest.mesh.normals).isEqualTo(normals)
        verify(mockFace).meshNormals
    }

    @Test
    fun meshTextureCoordinates_returns_ArCoreAugmentedFace_meshTextureCoordinates() {
        val coords = FloatBuffer.allocate(2)
        coords.put(8f)
        coords.put(8f)
        whenever(mockFace.meshTextureCoordinates).thenReturn(coords)

        assertThat(underTest.mesh.textureCoordinates).isEqualTo(coords)
        verify(mockFace).meshTextureCoordinates
    }

    @Test
    fun meshVertices_returns_ArCoreAugmentedFace_meshVertices() {
        val vertices = FloatBuffer.allocate(3)
        vertices.put(8f)
        vertices.put(8f)
        vertices.put(8f)
        whenever(mockFace.meshVertices).thenReturn(vertices)

        assertThat(underTest.mesh.vertices).isEqualTo(vertices)
        verify(mockFace).meshVertices
    }

    @Test
    fun noseTipPose_returns_arcore_noseTip_Pose() {
        val arCorePose = ArCore1xPose(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 1f, 1f, 1f))
        whenever(mockFace.getRegionPose(any())).thenReturn(arCorePose)

        assertThat(underTest.noseTipPose).isEqualTo(arCorePose.toRuntimePose())
        verify(mockFace).getRegionPose(ArCore1xAugmentedFace.RegionType.NOSE_TIP)
    }

    @Test
    fun foreheadLeftPose_returns_arcore_foreheadLeft_Pose() {
        val arCorePose = ArCore1xPose(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 1f, 1f, 1f))
        whenever(mockFace.getRegionPose(any())).thenReturn(arCorePose)

        assertThat(underTest.foreheadLeftPose).isEqualTo(arCorePose.toRuntimePose())
        verify(mockFace).getRegionPose(ArCore1xAugmentedFace.RegionType.FOREHEAD_LEFT)
    }

    @Test
    fun foreheadRightPose_returns_arcore_foreheadRight_Pose() {
        val arCorePose = ArCore1xPose(floatArrayOf(1f, 2f, 3f), floatArrayOf(1f, 1f, 1f, 1f))
        whenever(mockFace.getRegionPose(any())).thenReturn(arCorePose)

        assertThat(underTest.foreheadRightPose).isEqualTo(arCorePose.toRuntimePose())
        verify(mockFace).getRegionPose(ArCore1xAugmentedFace.RegionType.FOREHEAD_RIGHT)
    }
}
