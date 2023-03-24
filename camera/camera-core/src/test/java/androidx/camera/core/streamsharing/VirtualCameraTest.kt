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
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.defaultEmptySessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.processing.SurfaceEdge
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeDeferrableSurface
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

    private val surfaceEdgesToClose = mutableListOf<SurfaceEdge>()
    private val parentCamera = FakeCamera()
    private val child1 = FakeUseCase()
    private val child2 = FakeUseCaseConfig.Builder()
        .setMirrorMode(MIRROR_MODE_ON)
        .build()
    private val childrenEdges = mapOf(
        Pair(child1 as UseCase, createSurfaceEdge()),
        Pair(child2 as UseCase, createSurfaceEdge())
    )
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private lateinit var virtualCamera: VirtualCamera
    private var snapshotTriggered = false

    @Before
    fun setUp() {
        virtualCamera = VirtualCamera(
            parentCamera, setOf(child1, child2), useCaseConfigFactory
        ) {
            snapshotTriggered = true
            Futures.immediateFuture(null)
        }
    }

    @After
    fun tearDown() {
        for (surfaceEdge in surfaceEdgesToClose) {
            surfaceEdge.close()
        }
    }

    @Test
    fun submitStillCaptureRequests_triggersSnapshot() {
        // Arrange.
        virtualCamera.bindChildren()

        // Act: submit a still capture request from a child.
        val cameraControl = child1.camera!!.cameraControl as CameraControlInternal
        cameraControl.submitStillCaptureRequests(
            listOf(CaptureConfig.Builder().build()),
            CAPTURE_MODE_MINIMIZE_LATENCY,
            FLASH_MODE_AUTO
        )
        shadowOf(getMainLooper()).idle()

        // The StreamSharing.Control is called to take a snapshot.
        assertThat(snapshotTriggered).isTrue()
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
    }

    @Test
    fun getChildrenOutConfigs() {
        // Arrange.
        val cropRect = Rect(10, 10, 410, 310)

        // Act.
        val outConfigs = virtualCamera.getChildrenOutConfigs(
            createSurfaceEdge(cropRect = cropRect)
        )

        // Assert: child1
        val outConfig1 = outConfigs[child1]!!
        assertThat(outConfig1.cropRect).isEqualTo(cropRect)
        assertThat(outConfig1.size).isEqualTo(Size(400, 300))
        assertThat(outConfig1.mirroring).isFalse()
        // Assert: child2
        val outConfig2 = outConfigs[child2]!!
        assertThat(outConfig2.cropRect).isEqualTo(cropRect)
        assertThat(outConfig2.size).isEqualTo(Size(400, 300))
        assertThat(outConfig2.mirroring).isTrue()
    }

    @Test
    fun updateChildrenSpec_updateAndNotifyChildren() {
        // Act: update children with the map.
        virtualCamera.setChildrenEdges(childrenEdges)
        // Assert: surface size propagated to children
        assertThat(child1.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
        assertThat(child2.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
    }

    private fun createSurfaceEdge(
        target: Int = PREVIEW,
        format: Int = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
        streamSpec: StreamSpec = StreamSpec.builder(INPUT_SIZE).build(),
        matrix: Matrix = Matrix(),
        hasCameraTransform: Boolean = true,
        cropRect: Rect = Rect(),
        rotationDegrees: Int = 0,
        mirroring: Boolean = false
    ): SurfaceEdge {
        return SurfaceEdge(
            target,
            format,
            streamSpec,
            matrix,
            hasCameraTransform,
            cropRect,
            rotationDegrees,
            mirroring
        ).also { surfaceEdgesToClose.add(it) }
    }

    private fun verifyEdge(child: UseCase, isClosed: Boolean, hasProvider: Boolean) {
        assertThat(childrenEdges[child]!!.deferrableSurfaceForTesting.isClosed).isEqualTo(isClosed)
        assertThat(childrenEdges[child]!!.hasProvider()).isEqualTo(hasProvider)
    }
}