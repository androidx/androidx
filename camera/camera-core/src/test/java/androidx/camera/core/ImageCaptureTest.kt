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
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.MirrorMode.MIRROR_MODE_UNSPECIFIED
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.ScreenFlashWrapper
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.camera.testing.impl.mocks.MockScreenFlashListener
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

/** Unit tests for [ImageCapture]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImageCaptureTest {
    private val resolution = Size(640, 480)

    private lateinit var callbackHandler: Handler
    private lateinit var callbackThread: HandlerThread
    private lateinit var executor: Executor
    private lateinit var camera: FakeCamera
    private lateinit var cameraFront: FakeCamera
    private var fakeImageReaderProxy: FakeImageReaderProxy? = null
    private var capturedImage: ImageProxy? = null
    private var captureError: Exception? = null
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private val onImageCapturedCallback =
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                capturedImage = image
            }

            override fun onError(exception: ImageCaptureException) {
                captureError = exception
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
        val cameraInfo = FakeCameraInfoInternal().apply { isPrivateReprocessingSupported = true }

        camera = FakeCamera("0", null, cameraInfo)
        cameraFront =
            FakeCamera("1", null, FakeCameraInfoInternal("1", CameraSelector.LENS_FACING_FRONT))

        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(camera.cameraInfoInternal.cameraId) { camera }
                cameraFactory.insertDefaultFrontCamera(cameraFront.cameraInfoInternal.cameraId) {
                    cameraFront
                }
                cameraFactory
            }
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(FakeAppConfig.create())
                .setCameraFactoryProvider(cameraFactoryProvider)
                .build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        CameraXUtil.initialize(context, cameraXConfig).get()
        callbackThread = HandlerThread("Callback")
        callbackThread.start()
        // Explicitly pause callback thread since we will control execution manually in tests
        shadowOf(callbackThread.looper).pause()
        callbackHandler = Handler(callbackThread.looper)
        executor = CameraXExecutors.newHandlerExecutor(callbackHandler)

        capturedImage = null
        captureError = null
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        capturedImage?.close()
        CameraXUtil.shutdown().get()
        fakeImageReaderProxy = null
        callbackThread.quitSafely()
    }

    @Test
    fun virtualCamera_canRecreatePipeline() {
        // Arrange
        camera.hasTransform = false
        val imageCapture =
            bindImageCapture(
                bufferFormat = ImageFormat.JPEG,
            )
        // Act: pipeline can be recreated without crashing.
        imageCapture.updateSuggestedStreamSpec(StreamSpec.builder(resolution).build(), null)
    }

    @Test
    fun virtualCamera_imagePipelineExpectsNoMetadata() {
        // Arrange.
        camera.hasTransform = false

        // Act.
        val imageCapture =
            bindImageCapture(
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
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE))
            .isTrue()
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW)).isFalse()
        assertThat(imageCapture.isEffectTargetsSupported(VIDEO_CAPTURE)).isFalse()
        assertThat(imageCapture.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isFalse()
    }

    @Test
    fun defaultMirrorModeIsOff() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.mirrorModeInternal).isEqualTo(MIRROR_MODE_UNSPECIFIED)
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
        val imageCapture =
            bindImageCapture(
                bufferFormat = ImageFormat.JPEG,
            )
        val oldSurface = imageCapture.sessionConfig.surfaces.single().surface.get()
        assertTakePictureManagerHasTheSameSurface(imageCapture)

        // Act: invoke onError callback.
        imageCapture.sessionConfig.errorListener!!.onError(
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
            imageCapture.takePictureManager.imagePipeline
                .createSessionConfigBuilder(resolution)
                .build()
                .surfaces
                .single()
                .surface
                .get()
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
        val imageCapture =
            bindImageCapture(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build(),
                imageReaderProxyProvider = getFakeImageReaderProxyProvider()
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
        assertThat(capturedImage!!.cropRect)
            .isEqualTo(
                Rect(
                    expectedPadding,
                    0,
                    fakeImageReaderProxy!!.width - expectedPadding,
                    fakeImageReaderProxy!!.height
                )
            )
    }

    @Test
    fun capturedImageValidAfterRemoved() {
        // Arrange
        val imageCapture =
            bindImageCapture(
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
        val imageCapture =
            bindImageCapture(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider()
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
        val imageCapture =
            bindImageCapture(
                bufferFormat = ImageFormat.YUV_420_888,
                imageReaderProxyProvider = { width, height, _, queueDepth, usage ->
                    // Create a JPEG ImageReader that is of different format from buffer/input
                    // format.
                    fakeImageReaderProxy =
                        FakeImageReaderProxy.newInstance(
                            width,
                            height,
                            ImageFormat.JPEG,
                            queueDepth,
                            usage
                        )
                    fakeImageReaderProxy!!
                }
            )

        // Verify.
        assertThat(imageCapture.sessionConfig.surfaces[0].prescribedStreamFormat)
            .isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun canGetSupportedOutputFormats_whenCameraDoNotSupportUltraHdr() {
        val cameraInfo = FakeCameraInfoInternal()
        cameraInfo.setSupportedResolutions(ImageFormat.JPEG, listOf())

        // Verify.
        val capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)
        assertThat(capabilities.supportedOutputFormats)
            .containsExactlyElementsIn(listOf(OUTPUT_FORMAT_JPEG))
    }

    @Config(minSdk = 34)
    @Test
    fun canGetSupportedOutputFormats_whenCameraSupportsUltraHdr() {
        val cameraInfo = FakeCameraInfoInternal()
        cameraInfo.setSupportedResolutions(ImageFormat.JPEG, listOf())
        cameraInfo.setSupportedResolutions(ImageFormat.JPEG_R, listOf())

        // Verify.
        val capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)
        assertThat(capabilities.supportedOutputFormats)
            .containsExactlyElementsIn(listOf(OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_JPEG_ULTRA_HDR))
    }

    @Test
    fun outputFormat_isDefaultAsJpeg_whenNotSet() {
        val imageCapture = ImageCapture.Builder().build()

        // Verify.
        assertThat(imageCapture.outputFormat).isEqualTo(OUTPUT_FORMAT_JPEG)
    }

    @Test
    fun canSetOutputFormatAsJpeg() {
        val imageCapture = ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG).build()

        // Verify.
        assertThat(imageCapture.outputFormat).isEqualTo(OUTPUT_FORMAT_JPEG)
    }

    @Config(minSdk = 34)
    @Test
    fun canSetOutputFormatAsUltraHdr() {
        val imageCapture =
            ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR).build()

        // Verify.
        assertThat(imageCapture.outputFormat).isEqualTo(OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    @Config(minSdk = 34)
    @Test
    fun sessionConfigSurfaceFormat_isJpegR_whenOutputFormatIsSetAsUltraHdr() {
        // Arrange.
        val imageCapture =
            ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR).build()

        // Act.
        val cameraId = "fakeCameraId"
        val fakeManager = FakeCameraDeviceSurfaceManager()
        fakeManager.setValidSurfaceCombos(
            setOf(listOf(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, ImageFormat.JPEG_R))
        )
        val adapter =
            CameraUseCaseAdapter(
                FakeCamera(cameraId),
                FakeCameraCoordinator(),
                fakeManager,
                FakeUseCaseConfigFactory()
            )
        adapter.addUseCases(listOf(imageCapture))

        // Verify.
        assertThat(imageCapture.sessionConfig.surfaces[0].prescribedStreamFormat)
            .isEqualTo(ImageFormat.JPEG_R)
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
        val imageCapture =
            bindImageCapture(
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
            )
            .isEqualTo(newImplementationOptionValue)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetBothTargetResolutionAndAspectRatio() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder()
                .setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetResolutionWithResolutionSelector() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder()
                .setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Test
    fun throwExceptionWhileBuilding_whenFlashModeScreenSetWithoutScreenFlashInstanceSet() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_SCREEN).build()
        }
    }

    @Test
    fun throwException_whenFlashModeScreenSetWithoutScreenFlashInstanceSet() {
        val imageCapture = ImageCapture.Builder().build()

        assertThrows(IllegalArgumentException::class.java) {
            imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN
        }
    }

    @Test
    fun throwException_whenTakePictureWithFlashModeScreenButNoScreenFlashInstance() {
        val imageCapture = ImageCapture.Builder().build()
        imageCapture.screenFlash = MockScreenFlash()
        imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN
        imageCapture.screenFlash = null

        assertThrows(IllegalArgumentException::class.java) {
            imageCapture.takePicture(executor, onImageCapturedCallback)
        }
    }

    @Test
    fun throwException_whenFlashModeScreenSetToBackCamera() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)
        imageCapture.screenFlash = MockScreenFlash()

        assertThrows(IllegalArgumentException::class.java) {
            imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN
        }
    }

    @Test
    fun canSetFlashModeScreen_whenFrontCameraAndScreenFlashSet() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)

        imageCapture.screenFlash = MockScreenFlash()
        imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN
    }

    @Test
    fun throwException_whenSwitchToBackCameraAfterFlashModeScreenSetToFrontCamera() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        imageCapture.screenFlash = MockScreenFlash()
        imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN

        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            val cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(
                    ApplicationProvider.getApplicationContext(),
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(imageCapture))
        }
    }

    @Test
    fun screenFlashSetToCameraControl_whenSetInImageCapture() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        imageCapture.screenFlash = MockScreenFlash()

        assertThat((cameraFront.cameraControl as FakeCameraControl).screenFlash).isNotNull()
    }

    @Test
    fun screenFlashClearedFromCameraControl_whenImageCaptureUnbound() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        imageCapture.screenFlash = MockScreenFlash()

        cameraUseCaseAdapter.removeUseCases(listOf(imageCapture))

        assertThat((cameraFront.cameraControl as FakeCameraControl).screenFlash).isNull()
    }

    @Test
    fun screenFlashSetToCameraControl_whenUnboundAndBoundAgain() {
        val imageCapture = bindImageCapture(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        imageCapture.screenFlash = MockScreenFlash()

        cameraUseCaseAdapter.removeUseCases(listOf(imageCapture))
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        assertThat((cameraFront.cameraControl as FakeCameraControl).screenFlash).isNotNull()
    }

    @Test
    fun canCaptureScreenFlashImage() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )

        imageCapture.screenFlash = MockScreenFlash()
        imageCapture.flashMode = ImageCapture.FLASH_MODE_SCREEN

        // TODO: check why mainThreadExecutor() is needed here, instead of just any executor
        imageCapture.takePicture(mainThreadExecutor(), onImageCapturedCallback)

        provideFakeImageData()
        assertThat(capturedImage).isNotNull()
    }

    @Test
    fun screenFlashSetToImageCapture_cameraControlGetsWrapperScreenFlash() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )

        imageCapture.screenFlash = MockScreenFlash()

        val fakeCameraControl = cameraFront.cameraControl as FakeCameraControl

        assertThat(fakeCameraControl.screenFlash).isInstanceOf(ScreenFlashWrapper::class.java)
    }

    @Test
    fun cameraControlWrapperScreenFlash_hasImageCaptureScreenFlashInternally() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )

        imageCapture.screenFlash = MockScreenFlash()

        val fakeCameraControl = cameraFront.cameraControl as FakeCameraControl
        val screenFlashWrapper = fakeCameraControl.screenFlash as ScreenFlashWrapper

        assertThat(screenFlashWrapper.getBaseScreenFlash()).isEqualTo(imageCapture.screenFlash)
    }

    @Test
    fun imageCaptureUnbound_screenFlashClearNotInvoked_whenApplyWasNotInvokedBefore() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash()

        cameraUseCaseAdapter.removeUseCases(listOf(imageCapture))

        assertThat((imageCapture.screenFlash as MockScreenFlash).screenFlashEvents).isEmpty()
    }

    @Test
    fun imageCaptureUnbound_screenFlashClearInvoked_whenApplyWasInvokedBefore() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash()

        (cameraFront.cameraControl as FakeCameraControl)
            .screenFlash
            ?.apply(0L, MockScreenFlashListener())
        imageCapture.unbindFromCamera(cameraFront)

        assertThat((imageCapture.screenFlash as MockScreenFlash).screenFlashEvents)
            .contains(MockScreenFlash.CLEAR)
    }

    @Test
    fun imageCaptureUnbound_noScreenFlashEventIsDuplicate_whenApplyAndClearWasInvokedBefore() {
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash()

        (cameraFront.cameraControl as FakeCameraControl)
            .screenFlash
            ?.apply(0L, MockScreenFlashListener())
        (cameraFront.cameraControl as FakeCameraControl).screenFlash?.clear()
        imageCapture.unbindFromCamera(cameraFront)

        assertThat((imageCapture.screenFlash as MockScreenFlash).screenFlashEvents)
            .containsNoDuplicates()
    }

    @Test
    fun cameraControlScreenFlashListenerCompleted_whenImageCaptureListenerIsCompleted() {
        val listener = MockScreenFlashListener()
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash().apply { setApplyCompletedInstantly(false) }

        (cameraFront.cameraControl as FakeCameraControl).screenFlash?.apply(0L, listener)
        (imageCapture.screenFlash as MockScreenFlash).lastApplyListener?.onCompleted()

        listener.awaitComplete(3000)
        assertThat(listener.getCompleteCount()).isEqualTo(1)
    }

    @Test
    fun imageCaptureUnboundWithoutCompletion_cameraControlScreenFlashListenerCompleted() {
        val listener = MockScreenFlashListener()
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash().apply { setApplyCompletedInstantly(false) }

        (cameraFront.cameraControl as FakeCameraControl).screenFlash?.apply(0L, listener)
        imageCapture.unbindFromCamera(cameraFront)

        listener.awaitComplete(3000)
        assertThat(listener.getCompleteCount()).isEqualTo(1)
    }

    @Test
    fun imageCaptureUnboundAndListenerCompleted_cameraControlListenerCompletedOnlyOnce() {
        val listener = MockScreenFlashListener()
        val imageCapture =
            bindImageCapture(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                imageReaderProxyProvider = getFakeImageReaderProxyProvider(),
            )
        imageCapture.screenFlash = MockScreenFlash().apply { setApplyCompletedInstantly(false) }

        (cameraFront.cameraControl as FakeCameraControl).screenFlash?.apply(0L, listener)
        imageCapture.unbindFromCamera(cameraFront)
        (imageCapture.screenFlash as MockScreenFlash).lastApplyListener?.onCompleted()

        listener.awaitComplete(3000)
        assertThat(listener.getCompleteCount()).isEqualTo(1)
    }

    @Test
    fun canSetPostviewEnabled() {
        val imageCapture = ImageCapture.Builder().setPostviewEnabled(true).build()

        assertThat((imageCapture.currentConfig as ImageCaptureConfig).isPostviewEnabled).isTrue()
    }

    @Test
    fun canSetPostviewResolutionSelector() {
        val resolutionSelector = ResolutionSelector.Builder().build()
        val imageCapture =
            ImageCapture.Builder().setPostviewResolutionSelector(resolutionSelector).build()

        assertThat((imageCapture.currentConfig as ImageCaptureConfig).postviewResolutionSelector)
            .isSameInstanceAs(resolutionSelector)
    }

    @RequiresApi(23)
    @Test
    fun useMaximumSize_whenNotSettingPostviewResolutioSelector() {
        val imageCapture = ImageCapture.Builder().setPostviewEnabled(true).build()

        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(
                ApplicationProvider.getApplicationContext(),
                CameraSelector.DEFAULT_BACK_CAMERA,
                FakeCameraConfig(
                    sessionProcessor =
                        FakeSessionProcessor(
                            postviewSupportedSizes =
                                mapOf(ImageFormat.JPEG to listOf(Size(1920, 1080), Size(640, 480)))
                        )
                )
            )
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))
        assertThat(imageCapture.imagePipeline!!.postviewSize).isEqualTo(Size(1920, 1080))
    }

    @RequiresApi(23)
    @Test
    fun postviewResolutioSelectorCanWork() {
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()

        val imageCapture =
            ImageCapture.Builder()
                .setPostviewEnabled(true)
                .setPostviewResolutionSelector(resolutionSelector)
                .build()

        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(
                ApplicationProvider.getApplicationContext(),
                CameraSelector.DEFAULT_BACK_CAMERA,
                FakeCameraConfig(
                    sessionProcessor =
                        FakeSessionProcessor(
                            postviewSupportedSizes =
                                mapOf(
                                    ImageFormat.JPEG to listOf(Size(4000, 3000), Size(1920, 1080))
                                )
                        )
                )
            )

        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))
        assertThat(imageCapture.imagePipeline!!.postviewSize).isEqualTo(Size(1920, 1080))
    }

    @RequiresApi(23)
    @Test
    fun throwException_whenPostviewResolutionSelectorCannotSelectSize() {
        val resolutionSelector =
            ResolutionSelector.Builder().setResolutionFilter({ _, _ -> emptyList() }).build()
        val imageCapture =
            ImageCapture.Builder()
                .setPostviewEnabled(true)
                .setPostviewResolutionSelector(resolutionSelector)
                .build()

        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(
                ApplicationProvider.getApplicationContext(),
                CameraSelector.DEFAULT_BACK_CAMERA,
                FakeCameraConfig(
                    sessionProcessor =
                        FakeSessionProcessor(
                            postviewSupportedSizes =
                                mapOf(ImageFormat.JPEG to listOf(Size(1920, 1080)))
                        )
                )
            )

        // the CameraException will be converted to IllegalArgumentException in camera-lifecycle.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            cameraUseCaseAdapter.addUseCases(listOf(imageCapture))
        }
    }

    private fun bindImageCapture(
        captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        viewPort: ViewPort? = null,
        // Set non jpg format so it doesn't trigger the exif code path.
        bufferFormat: Int = ImageFormat.YUV_420_888,
        imageReaderProxyProvider: ImageReaderProxyProvider? = null,
        sessionProcessor: SessionProcessor? = null,
        cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    ): ImageCapture {
        // Arrange.
        val imageCapture =
            createImageCapture(
                captureMode,
                bufferFormat,
                imageReaderProxyProvider,
            )

        val cameraConfig = FakeCameraConfig(sessionProcessor = sessionProcessor)
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(
                ApplicationProvider.getApplicationContext(),
                cameraSelector,
                cameraConfig
            )

        cameraUseCaseAdapter.setViewPort(viewPort)
        cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(imageCapture))
        return imageCapture
    }

    private fun createImageCapture(
        captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        // Set non jpg format by default so it doesn't trigger the exif code path.
        bufferFormat: Int = ImageFormat.YUV_420_888,
        imageReaderProxyProvider: ImageReaderProxyProvider? = null
    ): ImageCapture {
        val builder =
            ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setCaptureMode(captureMode)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setIoExecutor(mainThreadExecutor())
                .setCaptureOptionUnpacker { _: UseCaseConfig<*>?, _: CaptureConfig.Builder? -> }
                .setSessionOptionUnpacker { _: Size, _: UseCaseConfig<*>?, _: SessionConfig.Builder?
                    ->
                }

        builder.setBufferFormat(bufferFormat)
        if (imageReaderProxyProvider != null) {
            builder.setImageReaderProxyProvider(imageReaderProxyProvider)
        }
        return builder.build()
    }

    private fun getFakeImageReaderProxyProvider(): ImageReaderProxyProvider {
        return ImageReaderProxyProvider { width, height, imageFormat, queueDepth, usage ->
            fakeImageReaderProxy =
                FakeImageReaderProxy.newInstance(width, height, imageFormat, queueDepth, usage)
            fakeImageReaderProxy!!
        }
    }

    private fun flushHandler(handler: Handler?) {
        (Shadow.extract<Any>(handler!!.looper) as ShadowLooper).idle()
    }

    private fun provideFakeImageData() {
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable(TagBundle.create(Pair("TagBundleKey", 0)), 0)
        flushAll()
    }

    private fun flushAll() {
        flushHandler(callbackHandler)
        shadowOf(getMainLooper()).idle()
    }
}
