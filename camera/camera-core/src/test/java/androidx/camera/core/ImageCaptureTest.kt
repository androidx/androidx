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
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

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
    private val testImplementationOption: androidx.camera.core.impl.Config.Option<Int> =
        androidx.camera.core.impl.Config.Option.create(
            "test.testOption",
            Int::class.javaPrimitiveType!!
        )
    private val testImplementationOptionValue = 5

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        val cameraInfo = FakeCameraInfoInternal().apply {
            isPrivateReprocessingSupported = true
        }

        camera = FakeCamera(null, cameraInfo)

        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _, _ ->
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
    fun virtualCamera_canRecreatePipeline() {
        // Arrange
        camera.hasTransform = false
        val imageCapture = bindImageCapture(
            bufferFormat = ImageFormat.JPEG,
        )
        // Act: pipeline can be recreated without crashing.
        imageCapture.updateSuggestedStreamSpec(StreamSpec.builder(resolution).build())
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
    fun detachWithoutAttach_doesNotCrash() {
        ImageCapture.Builder().build().onUnbind()
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
        imageCapture.takePicture(mainThreadExecutor(), onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        shadowOf(getMainLooper()).idle()

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
        imageCapture.takePicture(mainThreadExecutor(), onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(capturedImage!!.width).isEqualTo(fakeImageReaderProxy?.width)
        assertThat(capturedImage!!.height).isEqualTo(fakeImageReaderProxy?.height)
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

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenUpdateStreamSpecImplOptions() {
        val imageCapture = bindImageCapture(
            bufferFormat = ImageFormat.JPEG,
        )
        val newImplementationOptionValue = 6
        val streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, newImplementationOptionValue)
        imageCapture.updateSuggestedStreamSpecImplementationOptions(streamSpecOptions)
        assertThat(
            imageCapture.sessionConfig.implementationOptions.retrieveOption(
                testImplementationOption
            )
        ).isEqualTo(newImplementationOptionValue)
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
        sessionProcessor: SessionProcessor? = null
    ): ImageCapture {
        // Arrange.
        val imageCapture = createImageCapture(
            captureMode,
            bufferFormat,
            imageReaderProxyProvider,
        )

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
                ): SessionProcessor {
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
            .setIoExecutor(mainThreadExecutor())
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
}
