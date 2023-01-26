/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.processing.SurfaceProcessorInternal
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.ExecutionException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val TEST_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

/**
 * Unit tests for [Preview].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
class PreviewTest {

    var cameraUseCaseAdapter: CameraUseCaseAdapter? = null

    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var backCamera: FakeCamera
    private lateinit var frontCamera: FakeCamera
    private lateinit var cameraXConfig: CameraXConfig
    private lateinit var context: Context
    private lateinit var previewToDetach: Preview

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        backCamera = FakeCamera("back")
        frontCamera = FakeCamera("front", null, FakeCameraInfoInternal(0, LENS_FACING_FRONT))

        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig?, _: CameraSelector? ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(
                    backCamera.cameraInfoInternal.cameraId
                ) { backCamera }
                cameraFactory.insertDefaultFrontCamera(
                    frontCamera.cameraInfoInternal.cameraId
                ) { frontCamera }
                cameraFactory
            }
        cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).setCameraFactoryProvider(cameraFactoryProvider).build()
        context = ApplicationProvider.getApplicationContext<Context>()
        CameraXUtil.initialize(context, cameraXConfig).get()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        appSurfaceTexture.release()
        appSurface.release()
        with(cameraUseCaseAdapter) {
            this?.removeUseCases(useCases)
        }
        cameraUseCaseAdapter = null
        if (::previewToDetach.isInitialized) {
            previewToDetach.onUnbind()
        }
        CameraXUtil.shutdown().get()
    }

    @Test
    fun viewPortSet_cropRectIsBasedOnViewPort() {
        val transformationInfo = bindToLifecycleAndGetTransformationInfo(
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )
        // The expected value is based on fitting the 1:1 view port into a rect with the size of
        // FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.
        val expectedPadding = (
            FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width -
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            ) / 2
        assertThat(transformationInfo.cropRect).isEqualTo(
            Rect(
                expectedPadding,
                0,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width - expectedPadding,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun viewPortNotSet_cropRectIsFullSurface() {
        val transformationInfo = bindToLifecycleAndGetTransformationInfo(
            null
        )

        assertThat(transformationInfo.cropRect).isEqualTo(
            Rect(
                0,
                0,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun surfaceRequestSize_isSurfaceSize() {
        assertThat(bindToLifecycleAndGetSurfaceRequest().resolution).isEqualTo(
            Size(
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun setTargetRotation_rotationIsChanged() {
        // Arrange.
        val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_0).build()

        // Act: set target rotation.
        preview.targetRotation = Surface.ROTATION_180

        // Assert: target rotation is updated.
        assertThat(preview.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun attachUseCase_transformationInfoUpdates() {
        // Arrange: attach Preview without a SurfaceProvider.
        // Build and bind use case.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        val rational1 = Rational(1, 1)
        cameraUseCaseAdapter!!.setViewPort(ViewPort.Builder(rational1, Surface.ROTATION_0).build())
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))

        // Set SurfaceProvider
        var receivedTransformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
        }
        shadowOf(getMainLooper()).idle()
        assertThat(receivedTransformationInfo!!.cropRect.getAspectRatio()).isEqualTo(rational1)

        // Act: bind another use case with a different viewport.
        val fakeUseCase = FakeUseCase()
        val rational2 = Rational(2, 1)
        cameraUseCaseAdapter!!.setViewPort(ViewPort.Builder(rational2, Surface.ROTATION_0).build())
        cameraUseCaseAdapter!!.addUseCases(listOf(preview, fakeUseCase))
        shadowOf(getMainLooper()).idle()

        // Assert: received viewport's aspect ratio is the latest one.
        assertThat(receivedTransformationInfo!!.cropRect.getAspectRatio()).isEqualTo(rational2)
    }

    private fun Rect.getAspectRatio(): Rational {
        return Rational(width(), height())
    }

    @Test
    fun createSurfaceRequestWithProcessor_noCameraTransform() {
        // Arrange: attach Preview without a SurfaceProvider.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        var transformationInfo: TransformationInfo? = null

        // Act: create pipeline in Preview and provide Surface.
        val preview = createPreview(processor)
        preview.mCurrentSurfaceRequest!!.setTransformationInfoListener(mainThreadExecutor()) {
            transformationInfo = it
        }
        shadowOf(getMainLooper()).idle()

        // Get pending SurfaceRequest created by pipeline.
        assertThat(transformationInfo!!.hasCameraTransform()).isFalse()
    }

    @Test
    fun createSurfaceRequestWithoutProcessor_hasCameraTransform() {
        // Arrange: attach Preview without a SurfaceProvider.
        var transformationInfo: TransformationInfo? = null

        // Act: create pipeline in Preview and provide Surface.
        val preview = createPreview()
        preview.mCurrentSurfaceRequest!!.setTransformationInfoListener(mainThreadExecutor()) {
            transformationInfo = it
        }
        shadowOf(getMainLooper()).idle()

        // Get pending SurfaceRequest created by pipeline.
        assertThat(transformationInfo!!.hasCameraTransform()).isTrue()
    }

    @Test
    fun backCameraWithProcessor_notMirrored() {
        // Arrange.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        // Act: create pipeline
        val preview = createPreview(processor, backCamera)
        // Assert
        assertThat(preview.cameraEdge.mirroring).isFalse()
    }

    @Test
    fun frontCameraWithProcessor_mirrored() {
        // Arrange.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        // Act: create pipeline
        val preview = createPreview(processor, frontCamera)
        // Assert
        assertThat(preview.cameraEdge.mirroring).isTrue()
    }

    @Test
    fun setTargetRotationWithProcessor_rotationChangesOnSurfaceEdge() {
        // Arrange.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())

        // Act: create pipeline
        val preview = createPreview(processor)
        // Act: update target rotation
        preview.targetRotation = Surface.ROTATION_0
        shadowOf(getMainLooper()).idle()
        // Assert that the rotation of the SettableFuture is updated based on ROTATION_0.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(0)

        // Act: update target rotation again.
        preview.targetRotation = Surface.ROTATION_180
        shadowOf(getMainLooper()).idle()
        // Assert: the rotation of the SettableFuture is updated based on ROTATION_90.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun invalidateAppSurfaceRequestWithProcessing_cameraNotReset() {
        // Arrange: create Preview with processing.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor(), false)
        val surfaceRequest = createPreview(processor).mCurrentSurfaceRequest
        // Act: invalidate.
        surfaceRequest!!.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: preview is not reset.
        assertThat(backCamera.useCaseResetHistory).isEmpty()
    }

    @Test
    fun invalidateNodeSurfaceRequest_cameraReset() {
        // Arrange: create Preview with processing.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor(), false)
        val preview = createPreview(processor)
        // Act: invalidate.
        processor.surfaceRequest!!.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: preview is reset.
        assertThat(backCamera.useCaseResetHistory).containsExactly(preview)
    }

    @Test
    fun invalidateAppSurfaceRequestWithoutProcessing_cameraReset() {
        // Arrange: create Preview without processing.
        val preview = createPreview()
        val surfaceRequest = preview.mCurrentSurfaceRequest
        // Act: invalidate
        surfaceRequest!!.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: preview is reset.
        assertThat(backCamera.useCaseResetHistory).containsExactly(preview)
    }

    @Test
    fun invalidateWhenDetached_deferrableSurfaceClosed() {
        // Arrange: create Preview with processing then detach.
        val processor = FakeSurfaceProcessorInternal(mainThreadExecutor(), false)
        val preview = createPreview(processor)
        val surfaceRequest = processor.surfaceRequest!!
        preview.unbindFromCamera(backCamera)
        // Act: invalidate.
        surfaceRequest.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: preview is not reset and the DeferrableSurface is closed.
        assertThat(backCamera.useCaseResetHistory).isEmpty()
        assertThat(surfaceRequest.deferrableSurface.isClosed).isTrue()
    }

    @Test
    fun bindAndUnbindPreview_surfacesPropagated() {
        // Arrange.
        val processor = FakeSurfaceProcessorInternal(
            mainThreadExecutor(),
            false
        )

        // Act: create pipeline in Preview and provide Surface.
        val preview = createPreview(processor)
        val surfaceRequest = preview.mCurrentSurfaceRequest!!
        var appSurfaceReadyToRelease = false
        surfaceRequest.provideSurface(appSurface, mainThreadExecutor()) {
            appSurfaceReadyToRelease = true
        }
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput received.
        assertThat(processor.surfaceOutputs).hasSize(1)
        assertThat(processor.isReleased).isFalse()
        assertThat(processor.isOutputSurfaceRequestedToClose[PREVIEW]).isNull()
        assertThat(processor.isInputSurfaceReleased).isFalse()
        assertThat(appSurfaceReadyToRelease).isFalse()
        // processor surface is provided to camera.
        assertThat(preview.sessionConfig.surfaces[0].surface.get())
            .isEqualTo(processor.inputSurface)

        // Act: unbind Preview.
        preview.onUnbind()
        shadowOf(getMainLooper()).idle()

        // Assert: processor and processor surface is released.
        assertThat(processor.isReleased).isTrue()
        assertThat(processor.isOutputSurfaceRequestedToClose[PREVIEW]).isTrue()
        assertThat(processor.isInputSurfaceReleased).isTrue()
        assertThat(appSurfaceReadyToRelease).isFalse()

        // Act: close SurfaceOutput
        processor.surfaceOutputs[CameraEffect.PREVIEW]!!.close()
        shadowOf(getMainLooper()).idle()
        assertThat(appSurfaceReadyToRelease).isTrue()
    }

    @Test
    fun invokedErrorListener_recreatePipeline() {
        // Arrange: create pipeline and get a reference of the SessionConfig.
        val processor = FakeSurfaceProcessorInternal(
            mainThreadExecutor()
        )
        val preview = createPreview(processor)
        val originalSessionConfig = preview.sessionConfig

        // Act: invoke the error listener.
        preview.sessionConfig.errorListeners[0].onError(
            preview.sessionConfig, SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
        )
        shadowOf(getMainLooper()).idle()

        // Assert: the SessionConfig changed.
        assertThat(preview.sessionConfig).isNotEqualTo(originalSessionConfig)
    }

    @Test
    fun setTargetRotation_transformationInfoUpdated() {
        // Arrange: set up preview and verify target rotation in TransformationInfo.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        var receivedTransformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
        }
        shadowOf(getMainLooper()).idle()
        assertThat(receivedTransformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_0)

        // Act: set target rotation to a different value.
        preview.targetRotation = Surface.ROTATION_180
        shadowOf(getMainLooper()).idle()

        // Assert: target rotation changed.
        assertThat(receivedTransformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun setSurfaceProviderAfterAttachment_receivesSurfaceProviderCallbacks() {
        // Arrange: attach Preview without a SurfaceProvider.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        val cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider
                .getApplicationContext(),
            TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(preview))

        // Get pending SurfaceRequest created by pipeline.
        val pendingSurfaceRequest = preview.mCurrentSurfaceRequest
        var receivedSurfaceRequest: SurfaceRequest? = null
        var receivedTransformationInfo: TransformationInfo? = null

        // Act: set a SurfaceProvider after attachment.
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
            receivedSurfaceRequest = request
        }
        shadowOf(getMainLooper()).idle()

        // Assert: received SurfaceRequest is not the pending SurfaceRequest.
        assertThat(receivedSurfaceRequest).isNotSameInstanceAs(pendingSurfaceRequest)
        assertThat(receivedTransformationInfo).isNotNull()

        // Act: set a different SurfaceProvider.
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
            receivedSurfaceRequest = request
        }
        shadowOf(getMainLooper()).idle()

        // Assert: received a different SurfaceRequest.
        assertThat(receivedSurfaceRequest).isNotSameInstanceAs(pendingSurfaceRequest)
    }

    @Test
    fun setSurfaceProviderAfterDetach_receivesSurfaceRequestAfterAttach() {
        // Arrange: attach Preview without a SurfaceProvider.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider
                .getApplicationContext(),
            TEST_CAMERA_SELECTOR
        )
        // Attach
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        // Detach
        cameraUseCaseAdapter!!.removeUseCases(Collections.singleton<UseCase>(preview))

        // Act: set a SurfaceProvider after detaching
        var receivedSurfaceRequest = false
        preview.setSurfaceProvider { receivedSurfaceRequest = true }
        shadowOf(getMainLooper()).idle()

        val receivedWhileDetached = receivedSurfaceRequest

        // Attach
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        shadowOf(getMainLooper()).idle()

        val receivedAfterAttach = receivedSurfaceRequest

        // Assert: received a SurfaceRequest.
        assertThat(receivedWhileDetached).isFalse()
        assertThat(receivedAfterAttach).isTrue()
    }

    @Test
    fun throwException_whenSetBothTargetResolutionAndAspectRatio() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        }
    }

    @Test
    fun throwException_whenSetTargetResolutionWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Test
    fun throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    private fun bindToLifecycleAndGetSurfaceRequest(): SurfaceRequest {
        return bindToLifecycleAndGetResult(null).first
    }

    private fun bindToLifecycleAndGetTransformationInfo(viewPort: ViewPort?): TransformationInfo {
        return bindToLifecycleAndGetResult(viewPort).second
    }

    private fun bindToLifecycleAndGetResult(viewPort: ViewPort?): Pair<SurfaceRequest,
        TransformationInfo> {
        // Arrange.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        var surfaceRequest: SurfaceRequest? = null
        var transformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    transformationInfo = it
                }
            )
            surfaceRequest = request
        }

        // Act.
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter!!.setViewPort(viewPort)
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        shadowOf(getMainLooper()).idle()
        return Pair(surfaceRequest!!, transformationInfo!!)
    }

    private fun createPreview(
        surfaceProcessor: SurfaceProcessorInternal? = null,
        camera: FakeCamera = backCamera
    ): Preview {
        previewToDetach = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        previewToDetach.processor = surfaceProcessor
        previewToDetach.setSurfaceProvider(CameraXExecutors.directExecutor()) {}
        val previewConfig = PreviewConfig(
            cameraXConfig.getUseCaseConfigFactoryProvider(null)!!.newInstance(context).getConfig(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            )!! as OptionsBundle
        )
        previewToDetach.bindToCamera(camera, null, previewConfig)

        previewToDetach.onSuggestedResolutionUpdated(Size(640, 480))
        return previewToDetach
    }
}