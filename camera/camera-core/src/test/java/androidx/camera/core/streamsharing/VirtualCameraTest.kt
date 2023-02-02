/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.UseCase
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.processing.SurfaceEdge
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [VirtualCamera].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VirtualCameraTest {

    companion object {
        private val INPUT_SIZE = Size(800, 600)
    }

    private val parentCamera = FakeCamera()
    private val child1 = FakeUseCase()
    private val child2 = FakeUseCase()
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private lateinit var virtualCamera: VirtualCamera

    @Before
    fun setUp() {
        virtualCamera = VirtualCamera(parentCamera, setOf(child1, child2), useCaseConfigFactory)
    }

    @Test
    fun virtualCameraInheritsParentProperties() {
        assertThat(virtualCamera.cameraState).isEqualTo(parentCamera.cameraState)
        assertThat(virtualCamera.cameraInfo).isEqualTo(parentCamera.cameraInfo)
        assertThat(virtualCamera.cameraControl).isEqualTo(parentCamera.cameraControl)
    }

    @Test
    fun updateChildrenSpec_updateAndNotifyChildren() {
        // Arrange: create children spec map.
        val map = mutableMapOf<UseCase, SurfaceEdge>()
        map[child1] = createSurfaceEdge()
        map[child2] = createSurfaceEdge()
        // Act: update children with the map.
        virtualCamera.setChildrenEdges(map)
        // Assert: surface size propagated to children
        assertThat(child1.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
        assertThat(child2.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
    }

    private fun createSurfaceEdge(): SurfaceEdge {
        return SurfaceEdge(
            PREVIEW, StreamSpec.builder(INPUT_SIZE).build(), Matrix(), true, Rect(), 0, false
        )
    }
}