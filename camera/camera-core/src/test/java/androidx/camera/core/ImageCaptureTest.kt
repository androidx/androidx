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
import android.graphics.ImageFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper.getMainLooper
import android.util.Pair
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.ImageCapture.ImageCaptureRequest
import androidx.camera.core.ImageCapture.ImageCaptureRequestProcessor
import androidx.camera.core.ImageCapture.ImageCaptureRequestProcessor.ImageCaptor
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import androidx.camera.testing.fakes.FakeImageReaderProxy
import androidx.camera.testing.fakes.FakeSessionProcessor
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.ArrayDeque
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

private const val MAX_IMAGES = 3

/**
 * Unit tests for [ImageCapture].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImageCaptureTest {

    private val resolution = Size(640, 480)

    private lateinit var callbackHandler: Handler
    private lateinit var callbackThread: HandlerThread
    private lateinit var executor: Executor
    private lateinit var camera: FakeCamera
    private var fakeImageReaderProxy: FakeImageReaderProxy? = null
    private var capturedImage: ImageProxy? = null
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private val onImageCapturedCallback = object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            capturedImage = image
        }

        override fun onError(exception: ImageCaptureException) {
        }
    }

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        val cameraInfo = FakeCameraInfoInternal().apply {
            isPrivateReprocessingSupported = true
        }

        camera = FakeCamera(null, cameraInfo)

        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(camera.cameraInfoInternal.cameraId) {
                    camera
                }
                cameraFactory
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).setCameraFactoryProvider(cameraFactoryProvider).build()
        val context =
            ApplicationProvider.getApplicationContext<Context>()
        CameraXUtil.initialize(context, cameraXConfig).get()
        callbackThread = HandlerThread("Callback")
        callbackThread.start()
        // Explicitly pause callback thread since we will control execution manually in tests
        shadowOf(callbackThread.looper).pause()
        callbackHandler = Handler(callbackThread.looper)
        executor = CameraXExecutors.newHandlerExecutor(callbackHandler)
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        CameraXUtil.shutdown().get()
        fakeImageReaderProxy = null
        callbackThread.quitSafely()
    }

    @Test
    fun virtualCamera_imagePipelineExpectsNoMetadata() {
        // Arrange.
        camera.hasTransform = false

        // Act.
        val imageCapture = bindImageCapture(
            bufferFormat = ImageFormat.JPEG,
        )

        // Assert.
        assertThat(imageCapture.imagePipeline!!.expectsMetadata()).isFalse()
    }

    @Test
    fun verifySupportedEffects() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.isEffectTargetsSupported(IMAGE_CAPTURE)).isTrue()
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW or IMAGE_CAPTURE)).isTrue()
        assertThat(
            imageCapture.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)
        ).isTrue()
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW)).isFalse()
        assertThat(imageCapture.isEffectTargetsSupported(VIDEO_CAPTURE)).isFalse()
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isFalse()
    }

    @Test
    fun setTargetRotationDegrees() {
        val imageCapture = ImageCapture.Builder().build()
        imageCapture.setTargetRotationDegrees(45)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_270)
        imageCapture.setTargetRotationDegrees(135)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
        imageCapture.setTargetRotationDegrees(225)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_90)
        imageCapture.setTargetRotationDegrees(315)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_0)
        imageCapture.setTargetRotationDegrees(405)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_270)
        imageCapture.setTargetRotationDegrees(-45)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_0)
    }

    @Test
    fun defaultMirrorModeIsOff() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.mirrorModeInternal).isEqualTo(MIRROR_MODE_OFF)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun setMirrorMode_throwException() {
        ImageCapture.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)
    }

    @Test
    fun metadataNotSet_createsNewMetadataInstance() {
        val options = ImageCapture.OutputFileOptions.Builder(File("fake_path")).build()
        options.metadata.isReversedHorizontal = true

        val anotherOption = ImageCapture.OutputFileOptions.Builder(File("fake_path")).build()

        assertThat(anotherOption.metadata.isReversedHorizontal).isFalse()
    }

    @Test
    fun reverseHorizontalIsSet_flagReturnsTrue() {
        val metadata = ImageCapture.Metadata()
        assertThat(metadata.isReversedHorizontalSet).isFalse()

        metadata.isReversedHorizontal = false
        assertThat(metadata.isReversedHorizontalSet).isTrue()
    }

    @Test
    fun takePictureToImageWithoutBinding_receiveOnError() {
        // Arrange.
        val imageCapture = createImageCapture()
        val onImageCapturedCallback = mock(ImageCapture.OnImageCapturedCallback::class.java)

        // Act.
        imageCapture.takePicture(executor, onImageCapturedCallback)
        shadowOf(getMainLooper()).idle()
        flushHandler(callbackHandler)

        // Assert.
        verify(onImageCapturedCallback).onError(any())
    }

    @Test
    fun takePictureToFileWithoutBinding_receiveOnError() {
        // Arrange.
        val imageCapture = createImageCapture()
        val options = ImageCapture.OutputFileOptions.Builder(File("fake_path")).build()
        val onImageSavedCallback = mock(ImageCapture.OnImageSavedCallback::class.java)

        // Act.
        imageCapture.takePicture(options, executor, onImageSavedCallback)
        shadowOf(getMainLooper()).idle()
        flushHandler(callbackHandler)

        // Assert.
        verify(onImageSavedCallback).onError(any())
    }

    @Test
    fun onError_surfaceIsRecreated() {
        // Arrange: create ImageCapture and get the Surface
        val imageCapture = bindImageCapture(
            bufferFormat = ImageFormat.JPEG,
        )
        val oldSurface = imageCapture.sessionConfig.surfaces.single().surface.get()
        assertTakePictureManagerHasTheSameSurface(imageCapture)

        // Act: invoke onError callback.
        imageCapture.sessionConfig.errorListeners.single().onError(
            imageCapture.sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )

        // Assert: the surface has been recreated.
        val newSurface = imageCapture.sessionConfig.surfaces.single().surface.get()
        assertThat(newSurface).isNotEqualTo(oldSurface)
        assertTakePictureManagerHasTheSameSurface(imageCapture)
    }

    private fun assertTakePictureManagerHasTheSameSurface(imageCapture: ImageCapture) {
        val takePictureManagerSurface =
            imageCapture.takePictureManager.imagePipeline.createSessionConfigBuilder(
                resolution
            ).build().surfaces.single().surface.get()
        val useCaseSurface = imageCapture.sessionConfig.surfaces.single().surface.get()
        assertThat(takePictureManagerSurface).isEqualTo(useCaseSurface)
    }

    @Test
    fun processingPipelineOn_pipelineEnabled() {
        assertThat(
            bindImageCapture(
                useProcessingPipeline = true,
                bufferFormat = ImageFormat.JPEG,
            ).isProcessingPipelineEnabled
        ).isTrue()
    }

    @Test
    fun detachWithoutAttach_doesNotCrash() {
        ImageCapture.Builder().build().onUnbind()
    }

    @Test
    fun useImageReaderProvider_pipelineDisabled() {
        assertThat(
            bindImageCapture(
                useProcessingPipeline = true,
                bufferFormat = ImageFormat.JPEG,
                imageReaderProxyProvider = getImageReaderProxyProvider(),
            ).isProcessingPipelineEnabled
        ).isFalse()
    }

    @Test
    fun yuvFormat_pipelineDisabled() {
        assertThat(
            bindImageCapture(
                useProcessingPipeline = true,
                bufferFormat = ImageFormat.YUV_420_888,
            ).isProcessingPipelineEnabled
        ).isFalse()
    }

    @Config(minSdk = 28)
    @Test
    fun extensionIsOn_pipelineEnabled() {
        val imageCapture = bindImageCapture(
            useProcessingPipeline = true,
            bufferFormat = ImageFormat.JPEG,
            sessionProcessor = FakeSessionProcessor(null, null)
        )
        assertThat(imageCapture.isProcessingPipelineEnabled).isTrue()
        assertThat(imageCapture.imagePipeline!!.expectsMetadata()).isFalse()
    }

    @Test
    fun captureImageWithViewPort_isSet() {
        // Arrange
        val imageCapture = bindImageCapture(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build(),
            imageReaderProxyProvider = getImageReaderProxyProvider()
        )

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        shadowOf(getMainLooper()).idle()
        flushHandler(callbackHandler)

        // Assert.
        // The expected value is based on fitting the 1:1 view port into a rect with the size of
        // the ImageReader.
        val expectedPadding = (fakeImageReaderProxy!!.width - fakeImageReaderProxy!!.height) / 2
        assertThat(capturedImage!!.cropRect).isEqualTo(
            Rect(
                expectedPadding, 0, fakeImageReaderProxy!!.width - expectedPadding,
                fakeImageReaderProxy!!.height
            )
        )
    }

    @Test
    fun capturedImageValidAfterRemoved() {
        // Arrange
        val imageCapture = bindImageCapture(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        flushHandler(callbackHandler)
        cameraUseCaseAdapter.removeUseCases(
            Collections.singleton(imageCapture) as Collection<UseCase>
        )

        // Assert.
        // The captured image should still be valid even if the ImageCapture has been unbound. It
        // is the consumer of the ImageProxy who determines when the ImageProxy will be closed.
        capturedImage?.format
    }

    @Test
    fun capturedImageSize_isEqualToSurfaceSize() {
        // Act/arrange.
        val imageCapture = bindImageCapture(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            imageReaderProxyProvider = getImageReaderProxyProvider()
        )

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        shadowOf(getMainLooper()).idle()
        flushHandler(callbackHandler)

        // Assert.
        assertThat(capturedImage!!.width).isEqualTo(fakeImageReaderProxy?.width)
        assertThat(capturedImage!!.height).isEqualTo(fakeImageReaderProxy?.height)
    }

    @Test
    fun imageCaptureRequestProcessor_canSendRequest() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())
        val request = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request)

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        verify(request).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_canSendMultipleRequests() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())
        for (i in 0 until MAX_IMAGES) {
            val request = createImageCaptureRequest()

            // Act.
            requestProcessor.sendRequest(request)

            // Ensure tasks are posted to the processing executor
            shadowOf(getMainLooper()).idle()

            // Assert.
            verify(request).dispatchImage(any())
        }
    }

    @Test
    fun imageCaptureRequestProcessor_onlyAllowOneRequestProcessing() {
        // Arrange.
        // Create an ImageCaptor that won't complete the future.
        val captorFutureRef = AtomicReference<ResolvableFuture<ImageProxy>?>()
        val imageCaptor = createHoldImageCaptor(captorFutureRef)
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, imageCaptor)
        val request0 = createImageCaptureRequest()
        val request1 = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request0)
        requestProcessor.sendRequest(request1)

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        // Has processing request but not complete.
        assertThat(captorFutureRef.get()).isNotNull()
        verify(request0, never()).dispatchImage(any())
        verify(request1, never()).dispatchImage(any())

        // Act.
        // Complete request0.
        captorFutureRef.getAndSet(null)!!.set(mock(ImageProxy::class.java))

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        // request0 is complete and request1 is in processing.
        verify(request0).dispatchImage(any())
        verify(request1, never()).dispatchImage(any())
        assertThat(captorFutureRef.get()).isNotNull()

        // Act.
        // Complete request1.
        captorFutureRef.getAndSet(null)!!.set(mock(ImageProxy::class.java))

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        verify(request1).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_unableToProcessNextWhenOverMaxImages() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())

        // Exhaust outstanding image quota.
        val images = ArrayDeque<ImageProxy>()
        for (i in 0 until MAX_IMAGES) {
            val request = createImageCaptureRequest()
            requestProcessor.sendRequest(request)

            // Ensure tasks are posted to the processing executor
            shadowOf(getMainLooper()).idle()

            // Save the dispatched images.
            val captor = ArgumentCaptor.forClass(ImageProxy::class.java)
            verify(request).dispatchImage(captor.capture())
            images.offer(captor.value)
        }
        assertThat(images.size).isEqualTo(MAX_IMAGES)

        // Act.
        // Send one more request.
        val request = createImageCaptureRequest()
        requestProcessor.sendRequest(request)

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        verify(request, never()).dispatchImage(any())

        // Act.
        // Close one image to trigger next processing.
        images.poll()!!.close()

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        // It should trigger next processing.
        verify(request).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_canCancelRequests() {
        // Arrange.
        // Create an ImageCaptor that won't complete the future.
        val captorFutureRef = AtomicReference<ResolvableFuture<ImageProxy>?>()
        val imageCaptor = createHoldImageCaptor(captorFutureRef)
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, imageCaptor)

        // Send multiple requests and save these requests.
        val requestList = ArrayList<ImageCaptureRequest>()
        for (i in 0 until 5) {
            val request = createImageCaptureRequest()
            requestList.add(request)
            requestProcessor.sendRequest(request)

            // Ensure tasks are posted to the processing executor
            shadowOf(getMainLooper()).idle()
        }

        // Act.
        val errorMsg = "Cancel request."
        val throwable = RuntimeException(errorMsg)
        requestProcessor.cancelRequests(throwable)

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Assert.
        for (request in requestList) {
            verify(request).notifyCallbackError(anyInt(), eq(errorMsg), eq(throwable))
        }
        // Capture future is cancelled.
        assertThat(captorFutureRef.get()!!.isCancelled).isTrue()
    }

    @Test
    fun imageCaptureRequestProcessor_requestFail() {
        // Arrange.
        val errorMsg = "Capture failed."
        val throwable = RuntimeException(errorMsg)
        val requestProcessor =
            ImageCaptureRequestProcessor(MAX_IMAGES, createFailedImageCaptor(throwable))
        val request = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request)

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle()

        // Verify.
        verify(request).notifyCallbackError(anyInt(), eq(errorMsg), eq(throwable))
    }

    @Test
    fun sessionConfigSurfaceFormat_isInputFormat() {
        // Act/arrange.
        val imageCapture = bindImageCapture(bufferFormat = ImageFormat.YUV_420_888,
            imageReaderProxyProvider = { width, height, _, queueDepth, usage ->
                // Create a JPEG ImageReader that is of different format from buffer/input format.
                fakeImageReaderProxy = FakeImageReaderProxy.newInstance(
                    width, height, ImageFormat.JPEG, queueDepth, usage
                )
                fakeImageReaderProxy!!
            })

        // Verify.
        assertThat(imageCapture.sessionConfig.surfaces[0].prescribedStreamFormat)
            .isEqualTo(ImageFormat.YUV_420_888)
    }

    @Config(maxSdk = 22)
    @Test
    fun bindImageCaptureWithZslUnsupportedSdkVersion_notAddZslConfig() {
        bindImageCapture(
            ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )

        assertThat(camera.cameraControlInternal).isInstanceOf(FakeCameraControl::class.java)
        val cameraControl = camera.cameraControlInternal as FakeCameraControl
        assertThat(cameraControl.isZslConfigAdded).isFalse()
    }

    @Config(minSdk = 23)
    @Test
    fun bindImageCaptureInRegularCaptureModeWithZslSupportedSdkVersion_notAddZslConfig() {
        bindImageCapture(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )

        assertThat(camera.cameraControlInternal).isInstanceOf(FakeCameraControl::class.java)
        val cameraControl = camera.cameraControlInternal as FakeCameraControl
        assertThat(cameraControl.isZslConfigAdded).isFalse()
    }

    @Config(minSdk = 23)
    @Test
    fun bindImageCaptureInZslCaptureModeWithZslSupportedSdkVersion_addZslConfig() {
        bindImageCapture(
            ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )

        assertThat(camera.cameraControlInternal).isInstanceOf(FakeCameraControl::class.java)
        val cameraControl = camera.cameraControlInternal as FakeCameraControl
        assertThat(cameraControl.isZslConfigAdded).isTrue()
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetBothTargetResolutionAndAspectRatio() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetResolutionWithResolutionSelector() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    private fun bindImageCapture(
        captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        viewPort: ViewPort? = null,
        // Set non jpg format so it doesn't trigger the exif code path.
        bufferFormat: Int = ImageFormat.YUV_420_888,
        imageReaderProxyProvider: ImageReaderProxyProvider? = null,
        useProcessingPipeline: Boolean? = null,
        sessionProcessor: SessionProcessor? = null
    ): ImageCapture {
        // Arrange.
        val imageCapture = createImageCapture(
            captureMode,
            bufferFormat,
            imageReaderProxyProvider,
        )
        if (useProcessingPipeline != null) {
            imageCapture.mUseProcessingPipeline = useProcessingPipeline
        }

        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(),
            CameraSelector.DEFAULT_BACK_CAMERA
        )

        cameraUseCaseAdapter.setViewPort(viewPort)
        if (sessionProcessor != null) {
            cameraUseCaseAdapter.setExtendedConfig(object : CameraConfig {
                override fun getConfig(): androidx.camera.core.impl.Config {
                    return OptionsBundle.emptyBundle()
                }

                override fun getSessionProcessor(
                    valueIfMissing: SessionProcessor?
                ): SessionProcessor? {
                    return sessionProcessor
                }

                override fun getSessionProcessor(): SessionProcessor {
                    return sessionProcessor
                }

                override fun getCompatibilityId(): Identifier {
                    return Identifier.create(Any())
                }
            })
        }

        cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(imageCapture))
        return imageCapture
    }

    private fun createImageCapture(
        captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        // Set non jpg format by default so it doesn't trigger the exif code path.
        bufferFormat: Int = ImageFormat.YUV_420_888,
        imageReaderProxyProvider: ImageReaderProxyProvider? = null
    ): ImageCapture {
        val builder = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setCaptureMode(captureMode)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setCaptureOptionUnpacker { _: UseCaseConfig<*>?, _: CaptureConfig.Builder? -> }
            .setSessionOptionUnpacker { _: Size, _: UseCaseConfig<*>?,
                _: SessionConfig.Builder? ->
            }

        builder.setBufferFormat(bufferFormat)
        if (imageReaderProxyProvider != null) {
            builder.setImageReaderProxyProvider(imageReaderProxyProvider)
        }
        return builder.build()
    }

    private fun getImageReaderProxyProvider(): ImageReaderProxyProvider {
        return ImageReaderProxyProvider { width, height, imageFormat, queueDepth, usage ->
            fakeImageReaderProxy = FakeImageReaderProxy.newInstance(
                width, height, imageFormat, queueDepth, usage
            )
            fakeImageReaderProxy!!
        }
    }

    private fun flushHandler(handler: Handler?) {
        (Shadow.extract<Any>(handler!!.looper) as ShadowLooper).idle()
    }

    private fun createImageCaptureRequest(): ImageCaptureRequest {
        return mock(ImageCaptureRequest::class.java)
    }

    private fun createSuccessImageCaptor(): ImageCaptor {
        return ImageCaptor {
            Futures.immediateFuture(FakeImageProxy(FakeImageInfo()))
        }
    }

    private fun createHoldImageCaptor(
        futureHolder: AtomicReference<ResolvableFuture<ImageProxy>?>
    ): ImageCaptor {
        return ImageCaptor {
            ResolvableFuture.create<ImageProxy>().apply {
                futureHolder.set(this)
            }
        }
    }

    private fun createFailedImageCaptor(throwable: Throwable): ImageCaptor {
        return ImageCaptor {
            Futures.immediateFailedFuture(throwable)
        }
    }
}
