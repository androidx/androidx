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
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.defaultEmptySessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.processing.SurfaceEdge
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.FakeCameraCapturePipeline
import androidx.camera.testing.impl.FakeCameraCapturePipeline.InvocationType.POST_CAPTURE
import androidx.camera.testing.impl.FakeCameraCapturePipeline.InvocationType.PRE_CAPTURE
import androidx.camera.testing.impl.fakes.FakeDeferrableSurface
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [VirtualCameraAdapter]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VirtualCameraAdapterTest {

    companion object {
        private const val CLOSED = true
        private const val OPEN = false
        private const val HAS_PROVIDER = true
        private const val NO_PROVIDER = false
        private val INPUT_SIZE = Size(800, 600)
        private val CROP_RECT = Rect(0, 0, 800, 600)

        // Arbitrary transform to test that the transform is propagated.
        private val SENSOR_TO_BUFFER = Matrix().apply { setRotate(90F) }
        private var receivedSessionConfigError: SessionConfig.SessionError? = null
        private val SESSION_CONFIG_WITH_SURFACE =
            SessionConfig.Builder()
                .addSurface(FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE))
                .setErrorListener { _, error -> receivedSessionConfigError = error }
                .build()
    }

    private val surfaceEdgesToClose = mutableListOf<SurfaceEdge>()
    private val parentCamera = FakeCamera()
    private val child1 = FakeUseCaseConfig.Builder().setTargetRotation(Surface.ROTATION_0).build()
    private val child2 = FakeUseCaseConfig.Builder().setMirrorMode(MIRROR_MODE_ON).build()

    private val childrenEdges =
        mapOf(
            Pair(child1 as UseCase, createSurfaceEdge()),
            Pair(child2 as UseCase, createSurfaceEdge())
        )
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private lateinit var adapter: VirtualCameraAdapter
    private var snapshotTriggered = false

    private enum class Event {
        PRE_CAPTURE,
        SNAPSHOT,
        POST_CAPTURE,
    }

    private val events = CopyOnWriteArrayList<Event>()

    @Before
    fun setUp() {
        adapter =
            VirtualCameraAdapter(parentCamera, null, setOf(child1, child2), useCaseConfigFactory) {
                _,
                _ ->
                snapshotTriggered = true
                events.add(Event.SNAPSHOT)
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
        adapter.bindChildren()

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
    fun submitStillCaptureRequests_invokesPreAndPostCapturesInCorrectSequence() {
        // Arrange.
        adapter.bindChildren()

        val cameraControl = child1.camera!!.cameraControl as CameraControlInternal

        val fakeCameraCapturePipeline =
            cameraControl
                .getCameraCapturePipelineAsync(CAPTURE_MODE_MINIMIZE_LATENCY, FLASH_MODE_AUTO)
                .get(100, TimeUnit.MILLISECONDS) as FakeCameraCapturePipeline

        fakeCameraCapturePipeline.addInvocationListener(
            object : FakeCameraCapturePipeline.InvocationListener {
                override fun onInvoked(invocationType: FakeCameraCapturePipeline.InvocationType) {
                    when (invocationType) {
                        PRE_CAPTURE -> events.add(Event.PRE_CAPTURE)
                        POST_CAPTURE -> events.add(Event.POST_CAPTURE)
                    }
                }
            }
        )

        // Act: submit a still capture request from a child.

        cameraControl.submitStillCaptureRequests(
            listOf(CaptureConfig.Builder().build()),
            CAPTURE_MODE_MINIMIZE_LATENCY,
            FLASH_MODE_AUTO
        )
        shadowOf(getMainLooper()).idle()

        assertThat(events).isEqualTo(listOf(Event.PRE_CAPTURE, Event.SNAPSHOT, Event.POST_CAPTURE))
    }

    @Test
    fun getImageCaptureSurface_returnsNonRepeatingSurface() {
        assertThat(getUseCaseSurface(ImageCapture.Builder().build())).isNotNull()
    }

    @Test
    fun getChildSurface_returnsRepeatingSurface() {
        // Arrange.
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val preview =
            Preview.Builder().build().apply {
                this.setSurfaceProvider {
                    it.provideSurface(surface, directExecutor()) {
                        surfaceTexture.release()
                        surface.release()
                    }
                }
            }
        // Act & Assert.
        assertThat(getUseCaseSurface(preview)).isNotNull()
        // Cleanup.
        preview.unbindFromCamera(parentCamera)
        surfaceTexture.release()
        surface.release()
    }

    private fun getUseCaseSurface(useCase: UseCase): DeferrableSurface? {
        useCase.bindToCamera(
            parentCamera,
            null,
            null,
            useCase.getDefaultConfig(true, useCaseConfigFactory)
        )
        useCase.updateSuggestedStreamSpec(StreamSpec.builder(INPUT_SIZE).build(), null)
        return VirtualCameraAdapter.getChildSurface(useCase)
    }

    @Test
    fun setUseCaseActiveAndInactive_surfaceConnectsAndDisconnects() {
        // Arrange.
        adapter.bindChildren()
        adapter.setChildrenEdges(childrenEdges)
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
    fun resetWithClosedChildSurface_invokesErrorListener() {
        // Arrange.
        adapter.bindChildren()
        adapter.setChildrenEdges(childrenEdges)
        child1.updateSessionConfigForTesting(SESSION_CONFIG_WITH_SURFACE)
        child1.notifyActiveForTesting()

        // Act: close the child surface.
        SESSION_CONFIG_WITH_SURFACE.surfaces[0].close()
        adapter.onUseCaseReset(child1)
        shadowOf(getMainLooper()).idle()

        // Assert: error listener is invoked.
        assertThat(receivedSessionConfigError)
            .isEqualTo(SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
    }

    @Test
    fun resetUseCase_edgeInvalidated() {
        // Arrange: setup and get the old DeferrableSurface.
        adapter.bindChildren()
        adapter.setChildrenEdges(childrenEdges)
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
        adapter.bindChildren()
        adapter.setChildrenEdges(childrenEdges)
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
    fun parentHasMirroring_clientDoNotApplyMirroring() {
        // Arrange: create an edge that has mirrored the input in the past.
        val inputEdge = createSurfaceEdge(matrix = Matrix().apply { setScale(-1f, 1f) })
        // Act: get the children's out configs.
        val outConfigs = adapter.getChildrenOutConfigs(inputEdge, Surface.ROTATION_90, true)
        // Assert: child1 needs additional mirroring because the parent mirrors the input while the
        // child doesn't mirror.
        assertThat(outConfigs[child1]!!.isMirroring).isTrue()
        // Assert: child2 does not need additional mirroring because both the parent and the child
        // mirrors the input.
        assertThat(outConfigs[child2]!!.isMirroring).isFalse()
    }

    @Test
    fun getChildrenOutConfigs() {
        // Arrange.
        val cropRect = Rect(10, 10, 410, 310)
        val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_90).build()
        val imageCapture = ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build()
        adapter =
            VirtualCameraAdapter(
                parentCamera,
                null,
                setOf(preview, child2, imageCapture),
                useCaseConfigFactory
            ) { _, _ ->
                Futures.immediateFuture(null)
            }

        // Act.
        val outConfigs =
            adapter.getChildrenOutConfigs(
                createSurfaceEdge(cropRect = cropRect, rotationDegrees = 90),
                Surface.ROTATION_90,
                true
            )

        // Assert: preview config
        val previewOutConfig = outConfigs[preview]!!
        assertThat(previewOutConfig.format).isEqualTo(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
        assertThat(previewOutConfig.targets).isEqualTo(PREVIEW)
        assertThat(previewOutConfig.cropRect).isEqualTo(cropRect)
        // Preview's target rotation matches the parent's, so it only applies the 90° rotation.
        assertThat(previewOutConfig.size).isEqualTo(Size(300, 400))
        assertThat(previewOutConfig.rotationDegrees).isEqualTo(90)
        assertThat(previewOutConfig.isMirroring).isFalse()
        // Assert: ImageCapture config
        val imageOutConfig = outConfigs[imageCapture]!!
        assertThat(imageOutConfig.format).isEqualTo(ImageFormat.JPEG)
        assertThat(imageOutConfig.targets).isEqualTo(IMAGE_CAPTURE)
        // ImageCapture's target rotation does not match the parent's, so it applies the delta on
        // top of the 90° rotation.
        assertThat(imageOutConfig.size).isEqualTo(Size(400, 300))
        assertThat(imageOutConfig.rotationDegrees).isEqualTo(180)
        // Assert: child2
        val outConfig2 = outConfigs[child2]!!
        assertThat(outConfig2.format).isEqualTo(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
        assertThat(outConfig2.targets).isEqualTo(VIDEO_CAPTURE)
        assertThat(outConfig2.cropRect).isEqualTo(cropRect)
        assertThat(outConfig2.isMirroring).isTrue()
    }

    @Test
    fun updateChildrenSpec_updateAndNotifyChildren() {
        // Act: update children with the map.
        adapter.setChildrenEdges(childrenEdges)
        // Assert: surface size, crop rect and transformation propagated to children
        assertThat(child1.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
        assertThat(child2.attachedStreamSpec!!.resolution).isEqualTo(INPUT_SIZE)
        assertThat(child1.viewPortCropRect).isEqualTo(CROP_RECT)
        assertThat(child2.viewPortCropRect).isEqualTo(CROP_RECT)
        assertThat(child1.sensorToBufferTransformMatrix).isEqualTo(SENSOR_TO_BUFFER)
        assertThat(child2.sensorToBufferTransformMatrix).isEqualTo(SENSOR_TO_BUFFER)
    }

    private fun createSurfaceEdge(
        target: Int = PREVIEW,
        format: Int = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
        streamSpec: StreamSpec = StreamSpec.builder(INPUT_SIZE).build(),
        matrix: Matrix = SENSOR_TO_BUFFER,
        hasCameraTransform: Boolean = true,
        cropRect: Rect = CROP_RECT,
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
                ROTATION_NOT_SPECIFIED,
                mirroring
            )
            .also { surfaceEdgesToClose.add(it) }
    }

    private fun verifyEdge(child: UseCase, isClosed: Boolean, hasProvider: Boolean) {
        assertThat(childrenEdges[child]!!.deferrableSurfaceForTesting.isClosed).isEqualTo(isClosed)
        assertThat(childrenEdges[child]!!.hasProvider()).isEqualTo(hasProvider)
    }
}
