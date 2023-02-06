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

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.UseCase
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.defaultEmptySessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.processing.SurfaceEdge
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeDeferrableSurface
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
        private const val CLOSED = true
        private const val OPEN = false
        private const val HAS_PROVIDER = true
        private const val NO_PROVIDER = false
        private val INPUT_SIZE = Size(800, 600)
        private val SESSION_CONFIG_WITH_SURFACE = SessionConfig.Builder()
            .addSurface(FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE)).build()
    }

    private val parentCamera = FakeCamera()
    private val child1 = FakeUseCase()
    private val child2 = FakeUseCase()
    private val childrenEdges = mapOf(
        Pair(child1 as UseCase, createSurfaceEdge()),
        Pair(child2 as UseCase, createSurfaceEdge())
    )
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private lateinit var virtualCamera: VirtualCamera

    @Before
    fun setUp() {
        virtualCamera = VirtualCamera(parentCamera, setOf(child1, child2), useCaseConfigFactory)
    }

    @Test
    fun setUseCaseActiveAndInactive_surfaceConnectsAndDisconnects() {
        // Arrange.
        virtualCamera.bindChildren()
        virtualCamera.setChildrenEdges(childrenEdges)
        child1.updateSessionConfigForTesting(SESSION_CONFIG_WITH_SURFACE)
        // Assert: edge open by default.
        verifyEdge(child1, OPEN, NO_PROVIDER)
        // Set UseCase to active, verify it has provider.
        child1.notifyActiveForTesting()
        verifyEdge(child1, OPEN, HAS_PROVIDER)
        // Set UseCase to inactive, verify it's closed.
        child1.notifyInactiveForTesting()
        verifyEdge(child1, CLOSED, HAS_PROVIDER)
        // Set UseCase to active, verify it becomes open again.
        child1.notifyActiveForTesting()
        verifyEdge(child1, OPEN, HAS_PROVIDER)
    }

    @Test
    fun resetUseCase_edgeInvalidated() {
        // Arrange: setup and get the old DeferrableSurface.
        virtualCamera.bindChildren()
        virtualCamera.setChildrenEdges(childrenEdges)
        child1.updateSessionConfigForTesting(SESSION_CONFIG_WITH_SURFACE)
        child1.notifyActiveForTesting()
        val oldSurface = childrenEdges[child1]!!.deferrableSurfaceForTesting
        // Act: notify reset.
        child1.notifyResetForTesting()
        // Assert: DeferrableSurface is recreated. The old one is closed.
        assertThat(oldSurface.isClosed).isTrue()
        assertThat(childrenEdges[child1]!!.deferrableSurfaceForTesting)
            .isNotSameInstanceAs(oldSurface)
        verifyEdge(child1, OPEN, HAS_PROVIDER)
    }

    @Test
    fun updateUseCaseWithAndWithoutSurface_surfaceConnectsAndDisconnects() {
        // Arrange
        virtualCamera.bindChildren()
        virtualCamera.setChildrenEdges(childrenEdges)
        child1.notifyActiveForTesting()
        verifyEdge(child1, OPEN, NO_PROVIDER)

        // Act: set Surface and update
        child1.updateSessionConfigForTesting(SESSION_CONFIG_WITH_SURFACE)
        child1.notifyUpdatedForTesting()
        // Assert: edge is connected.
        verifyEdge(child1, OPEN, HAS_PROVIDER)
        // Act: remove Surface and update.
        child1.updateSessionConfigForTesting(defaultEmptySessionConfig())
        child1.notifyUpdatedForTesting()
        // Assert: edge is disconnected.
        verifyEdge(child1, CLOSED, HAS_PROVIDER)
        // Act: set Surface and update.
        child1.updateSessionConfigForTesting(SESSION_CONFIG_WITH_SURFACE)
        child1.notifyUpdatedForTesting()
        // Assert: edge is connected again.
        verifyEdge(child1, OPEN, HAS_PROVIDER)
    }

    @Test
    fun virtualCameraInheritsParentProperties() {
        assertThat(virtualCamera.cameraState).isEqualTo(parentCamera.cameraState)
        assertThat(virtualCamera.cameraInfo).isEqualTo(parentCamera.cameraInfo)
        assertThat(virtualCamera.cameraControl).isEqualTo(parentCamera.cameraControl)
    }

    @Test
    fun updateChildrenSpec_updateAndNotifyChildren() {
        // Act: update children with the map.
        virtualCamera.setChildrenEdges(childrenEdges)
        // Assert: surface size propagated to children
        assertThat(child1.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
        assertThat(child2.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
    }

    private fun createSurfaceEdge(): SurfaceEdge {
        return SurfaceEdge(
            PREVIEW, StreamSpec.builder(INPUT_SIZE).build(), Matrix(), true, Rect(), 0, false
        )
    }

    private fun verifyEdge(child: UseCase, isClosed: Boolean, hasProvider: Boolean) {
        assertThat(childrenEdges[child]!!.deferrableSurfaceForTesting.isClosed).isEqualTo(isClosed)
        assertThat(childrenEdges[child]!!.hasProvider()).isEqualTo(hasProvider)
    }
}