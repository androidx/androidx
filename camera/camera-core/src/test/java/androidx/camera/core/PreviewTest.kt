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
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper.getMainLooper
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.Surface.ROTATION_90
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RequiresApi(21)
private val TEST_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

/**
 * Unit tests for [Preview].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
// Option Declarations:
// *********************************************************************************************
class PreviewTest {

    companion object {
        val FRAME_RATE_RANGE = Range(30, 60)
    }

    private var cameraUseCaseAdapter: CameraUseCaseAdapter? = null

    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var backCamera: FakeCamera
    private lateinit var frontCamera: FakeCamera
    private lateinit var cameraXConfig: CameraXConfig
    private lateinit var context: Context
    private lateinit var previewToDetach: Preview
    private lateinit var processor: FakeSurfaceProcessorInternal
    private lateinit var effect: CameraEffect

    private val handlersToRelease = mutableListOf<Handler>()

    private val sensorToBufferTransform = Matrix().apply {
        setScale(1f, 2f)
    }

    private val testImplementationOption: androidx.camera.core.impl.Config.Option<Int> =
        androidx.camera.core.impl.Config.Option.create(
            "test.testOption",
            Int::class.javaPrimitiveType!!
        )
    private val testImplementationOptionValue = 5

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        backCamera = FakeCamera("back")
        frontCamera = FakeCamera("front", null, FakeCameraInfoInternal(0, LENS_FACING_FRONT))

        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig?,
                _: CameraSelector?, _: Long? ->
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
        processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        effect = FakeSurfaceEffect(processor)
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
        processor.release()
        CameraXUtil.shutdown().get()
        for (handler in handlersToRelease) {
            handler.looper.quitSafely()
        }
    }

    @Test
    fun attachPreview_receiveTransformationInfoOnlyOnce() {
        // Arrange.
        val semaphore = Semaphore(0)

        // Act: create preview and listen to transformation info.
        createPreview(surfaceProvider = {
            it.setTransformationInfoListener(directExecutor()) {
                semaphore.release()
            }
        })

        // Assert: only receive transformation info once.
        assertThat(semaphore.tryAcquire(1, 1, TimeUnit.SECONDS)).isTrue()
        assertThat(semaphore.tryAcquire(2, 1, TimeUnit.SECONDS)).isFalse()
    }

    @Test
    fun createPreview_sessionConfigMatchesStreamSpec() {
        // Act: Create a preview use case.
        val preview = createPreview()
        // Assert: The session config matches the stream spec.
        val sessionConfig = preview.sessionConfig
        assertThat(sessionConfig.expectedFrameRateRange).isEqualTo(FRAME_RATE_RANGE)
        assertThat(sessionConfig.implementationOptions.retrieveOption(testImplementationOption))
            .isEqualTo(testImplementationOptionValue)
    }

    @Test
    fun createPreview_deferrableSurfaceIsTheSurfaceRequestSurface() {
        // Act: Create a preview use case.
        val preview = createPreview()
        // Assert: The preview's deferrable surface is the surface request surface.
        assertThat(preview.sessionConfig.surfaces.single())
            .isEqualTo(preview.mCurrentSurfaceRequest!!.deferrableSurface)
    }

    @Test
    fun verifySupportedEffects() {
        val preview = Preview.Builder().build()
        assertThat(preview.isEffectTargetsSupported(PREVIEW)).isTrue()
        assertThat(preview.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isTrue()
        assertThat(
            preview.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)
        ).isTrue()
        assertThat(preview.isEffectTargetsSupported(VIDEO_CAPTURE)).isFalse()
        assertThat(preview.isEffectTargetsSupported(IMAGE_CAPTURE)).isFalse()
        assertThat(preview.isEffectTargetsSupported(IMAGE_CAPTURE or VIDEO_CAPTURE)).isFalse()
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
    fun surfaceRequestFrameRateRange_isUnspecified() {
        // Target frame rate range isn't specified, so SurfaceRequest
        // expected frame rate range should be unspecified.
        assertThat(bindToLifecycleAndGetSurfaceRequest().expectedFrameRate).isEqualTo(
            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
        )
    }

    @Test
    fun defaultMirrorModeIsOnFrontOnly() {
        val preview = Preview.Builder().build()
        assertThat(preview.mirrorModeInternal).isEqualTo(MIRROR_MODE_ON_FRONT_ONLY)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun setMirrorMode_throwException() {
        Preview.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)
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
    fun setTargetRotation_rotationIsPropagated() {
        // Arrange: create preview and wait for transformation info.
        val preview = createPreview()
        var transformationInfo: TransformationInfo? = null
        preview.mCurrentSurfaceRequest!!.setTransformationInfoListener(
            mainThreadExecutor()
        ) { newValue: TransformationInfo -> transformationInfo = newValue }

        // Act: set target rotation.
        preview.targetRotation = Surface.ROTATION_180
        shadowOf(getMainLooper()).idle()

        // Assert: target rotation is updated.
        assertThat(transformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    // @Test TODO re-enable once b/284336967 is done.
    fun attachUseCase_transformationInfoUpdates() {
        // Arrange: attach Preview without a SurfaceProvider.
        // Build and bind use case.
        val sessionOptionUnpacker =
            { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
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
        var transformationInfo: TransformationInfo? = null

        // Act: create pipeline in Preview and provide Surface.
        val preview = createPreview(effect)
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
        assertThat(transformationInfo!!.sensorToBufferTransform).isEqualTo(sensorToBufferTransform)
    }

    @Test
    fun backCameraWithProcessor_notMirrored() {
        // Act: create pipeline
        val preview = createPreview(effect, backCamera)
        // Assert
        assertThat(preview.cameraEdge.mirroring).isFalse()
    }

    @Test
    fun frontCameraWithProcessor_mirrored() {
        // Act: create pipeline
        val preview = createPreview(effect, frontCamera)
        // Assert
        assertThat(preview.cameraEdge.mirroring).isTrue()
    }

    @Test
    fun setTargetRotationWithProcessor_rotationChangesOnSurfaceEdge() {
        // Act: create pipeline
        val preview = createPreview(effect)
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
    fun setTargetRotationWithProcessorOnBackground_rotationChangesOnSurfaceEdge() {
        // Act: create pipeline
        val preview = createPreview(effect)
        // Act: update target rotation
        preview.targetRotation = Surface.ROTATION_0
        shadowOf(getMainLooper()).idle()
        // Assert that the rotation of the SettableFuture is updated based on ROTATION_0.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(0)

        // Act: update target rotation again.
        val backgroundHandler = createBackgroundHandler()
        backgroundHandler.post { preview.targetRotation = Surface.ROTATION_180 }
        shadowOf(backgroundHandler.looper).idle()
        shadowOf(getMainLooper()).idle()
        // Assert: the rotation of the SettableFuture is updated based on ROTATION_90.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun invalidateAppSurfaceRequestWithProcessing_cameraNotReset() {
        // Arrange: create Preview with processing.
        val surfaceRequest = createPreview(effect).mCurrentSurfaceRequest
        // Act: invalidate.
        surfaceRequest!!.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: preview is not reset.
        assertThat(backCamera.useCaseResetHistory).isEmpty()
    }

    @Test
    fun invalidateNodeSurfaceRequest_cameraReset() {
        // Arrange: create Preview with processing.
        val preview = createPreview(effect)
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
        val preview = createPreview(effect)
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
    fun hasCameraTransform_rotationDegreesNotFlipped() {
        // Act: create preview with hasCameraTransform == true
        val preview = createPreview(
            effect,
            frontCamera,
            targetRotation = ROTATION_90
        )
        assertThat(preview.cameraEdge.hasCameraTransform()).isTrue()
        // Assert: rotationDegrees is not flipped.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(90)
    }

    @Test
    fun noCameraTransform_rotationDegreesIsZero() {
        // Act: create preview with hasCameraTransform == false
        frontCamera.hasTransform = false
        val preview = createPreview(
            effect,
            frontCamera,
            targetRotation = ROTATION_90
        )
        // Assert: rotationDegrees is 0.
        assertThat(preview.cameraEdge.rotationDegrees).isEqualTo(0)
    }

    @Test
    fun setNoCameraTransform_propagatesToCameraEdge() {
        // Act: create preview with hasCameraTransform == false
        frontCamera.hasTransform = false
        val preview = createPreview(
            effect,
            frontCamera,
            targetRotation = ROTATION_90
        )
        // Assert
        assertThat(preview.cameraEdge.hasCameraTransform()).isFalse()
        assertThat(preview.cameraEdge.mirroring).isFalse()
    }

    @Test
    fun frontCameraWithoutCameraTransform_noMirroring() {
        // Act: create preview with hasCameraTransform == false
        frontCamera.hasTransform = false
        val preview = createPreview(
            effect,
            frontCamera,
            targetRotation = ROTATION_90
        )
        // Assert
        assertThat(preview.cameraEdge.mirroring).isFalse()
    }

    @Test
    fun cameraEdgeHasTransformByDefault() {
        assertThat(createPreview(effect).cameraEdge.hasCameraTransform()).isTrue()
    }

    @Test
    fun bindAndUnbindPreview_surfacesPropagated() {
        // Act: create pipeline in Preview and provide Surface.
        val preview = createPreview(effect)
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
        assertThat(appSurfaceReadyToRelease).isTrue()
    }

    @Test
    fun invokedErrorListener_recreatePipeline() {
        // Arrange: create pipeline and get a reference of the SessionConfig.
        val preview = createPreview(effect)
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
            { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
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
            { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
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
            { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
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
    fun sessionConfigHasStreamSpecImplementationOptions_whenCreatePipeline() {
        val preview = createPreview(effect)
        assertThat(
            preview.sessionConfig.implementationOptions.retrieveOption(
                testImplementationOption
            )
        ).isEqualTo(testImplementationOptionValue)
    }

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenUpdateStreamSpecImplOptions() {
        val preview = createPreview(effect)
        val newImplementationOptionValue = 6
        val streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, newImplementationOptionValue)
        preview.updateSuggestedStreamSpecImplementationOptions(streamSpecOptions)
        assertThat(
            preview.sessionConfig.implementationOptions.retrieveOption(
                testImplementationOption
            )
        ).isEqualTo(newImplementationOptionValue)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetBothTargetResolutionAndAspectRatio() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetResolutionWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Test
    fun canSetTargetFrameRate() {
        val preview = Preview.Builder().setTargetFrameRate(Range(15, 30))
            .build()
        assertThat(preview.targetFrameRate).isEqualTo(Range(15, 30))
    }

    @Test
    fun canSetPreviewStabilization() {
        val preview = Preview.Builder().setPreviewStabilizationEnabled(true)
            .build()
        assertThat(preview.isPreviewStabilizationEnabled).isTrue()
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
            { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
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
        effect: CameraEffect? = null,
        camera: FakeCamera = backCamera,
        targetRotation: Int = ROTATION_90,
        surfaceProvider: SurfaceProvider = SurfaceProvider {
        }
    ): Preview {
        previewToDetach = Preview.Builder()
            .setTargetRotation(targetRotation)
            .build()
        previewToDetach.effect = effect
        previewToDetach.setSurfaceProvider(directExecutor(), surfaceProvider)
        previewToDetach.bindToCamera(
            camera, null, previewToDetach.getDefaultConfig(
                true,
                cameraXConfig.getUseCaseConfigFactoryProvider(null)!!.newInstance(context)
            )
        )

        val streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, testImplementationOptionValue)
        val streamSpec = StreamSpec.builder(Size(640, 480))
            .setExpectedFrameRateRange(FRAME_RATE_RANGE)
            .setImplementationOptions(streamSpecOptions).build()
        previewToDetach.sensorToBufferTransformMatrix = sensorToBufferTransform
        previewToDetach.updateSuggestedStreamSpec(streamSpec)
        return previewToDetach
    }

    private fun createBackgroundHandler(): Handler {
        val handler = Handler(HandlerThread("PreviewTest").run {
            start()
            looper
        })
        handlersToRelease.add(handler)
        return handler
    }
}
