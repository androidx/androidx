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

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.internal.Camera2UseCaseConfigFactory
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CompositionSettings
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_CLASS
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.core.processing.SurfaceProcessorWithExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [StreamSharing]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamSharingTest {

    companion object {
        private const val SENSOR_ROTATION = 270
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val child1 =
        FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(1).useCaseConfig)
    private val child2 =
        FakeUseCase(FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig)
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private val camera = FakeCamera()
    private val secondaryCamera = FakeCamera()
    private val frontCamera =
        FakeCamera(null, FakeCameraInfoInternal(SENSOR_ROTATION, LENS_FACING_FRONT))
    private lateinit var streamSharing: StreamSharing
    private val size = Size(800, 600)
    private val cropRect = Rect(150, 100, 750, 500)
    private lateinit var defaultConfig: UseCaseConfig<*>
    private lateinit var effectProcessor: FakeSurfaceProcessorInternal
    private lateinit var sharingProcessor: FakeSurfaceProcessorInternal
    private lateinit var effect: CameraEffect
    private val testImplementationOption: androidx.camera.core.impl.Config.Option<Int> =
        androidx.camera.core.impl.Config.Option.create(
            "test.testOption",
            Int::class.javaPrimitiveType!!
        )
    private val testImplementationOptionValue = 5

    @Before
    fun setUp() {
        sharingProcessor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        DefaultSurfaceProcessor.Factory.setSupplier { sharingProcessor }
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1, child2),
                useCaseConfigFactory
            )
        defaultConfig = streamSharing.getDefaultConfig(true, useCaseConfigFactory)!!
        effectProcessor = FakeSurfaceProcessorInternal(mainThreadExecutor())
        effect = FakeSurfaceEffect(PREVIEW or VIDEO_CAPTURE, effectProcessor)
    }

    @After
    fun tearDown() {
        if (streamSharing.camera != null) {
            streamSharing.unbindFromCamera(streamSharing.camera!!)
        }
        effectProcessor.release()
        sharingProcessor.cleanUp()
        effectProcessor.cleanUp()
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun effectHandleSharing_effectUsedAsSharingNode() {
        // Arrange: create an effect that handles sharing.
        effect =
            FakeSurfaceEffect(
                PREVIEW or VIDEO_CAPTURE,
                CameraEffect.TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION,
                CameraEffect.OUTPUT_OPTION_ONE_FOR_EACH_TARGET,
                effectProcessor
            )
        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder(Recorder.Builder().build()).build()
        streamSharing =
            StreamSharing(
                frontCamera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, videoCapture),
                useCaseConfigFactory
            )
        streamSharing.setViewPortCropRect(cropRect)
        streamSharing.effect = effect

        // Act: Bind effect and get sharing input edge.
        streamSharing.bindToCamera(frontCamera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Assert: the sharing node is built with the effect's processor
        val sharingProcessor =
            (streamSharing.sharingNode!!.surfaceProcessor as SurfaceProcessorWithExecutor).processor
        assertThat(sharingProcessor).isEqualTo(effectProcessor)
        assertThat(streamSharing.sharingInputEdge).isEqualTo(streamSharing.cameraEdge)
        assertThat(streamSharing.virtualCameraAdapter.mChildrenEdges[preview]!!.targets)
            .isEqualTo(PREVIEW)
        assertThat(streamSharing.virtualCameraAdapter.mChildrenEdges[videoCapture]!!.targets)
            .isEqualTo(VIDEO_CAPTURE)
    }

    @Test
    fun effectHandleRotationAndMirroring_remainingTransformationIsEmpty() {
        // Arrange: create an effect that handles rotation.
        effect =
            FakeSurfaceEffect(
                PREVIEW or VIDEO_CAPTURE,
                CameraEffect.TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION,
                effectProcessor
            )
        streamSharing =
            StreamSharing(
                frontCamera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1),
                useCaseConfigFactory
            )
        streamSharing.setViewPortCropRect(cropRect)
        streamSharing.effect = effect
        // Act: Bind effect and get sharing input edge.
        streamSharing.bindToCamera(frontCamera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        // Assert: no remaining rotation because it's handled by the effect.
        assertThat(streamSharing.sharingInputEdge!!.rotationDegrees).isEqualTo(0)
        assertThat(streamSharing.sharingInputEdge!!.cropRect).isEqualTo(Rect(100, 50, 500, 650))
        assertThat(streamSharing.sharingInputEdge!!.isMirroring).isEqualTo(false)
    }

    @Test
    fun effectDoNotHandleRotationAndMirroring_remainingTransformationIsNotEmpty() {
        // Arrange: create an effect that does not handle rotation.
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1),
                useCaseConfigFactory
            )
        streamSharing.setViewPortCropRect(cropRect)
        streamSharing.effect = effect
        // Act: bind effect.
        streamSharing.bindToCamera(frontCamera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        // Assert: the remaining rotation still exists because the effect doesn't handle it. It will
        // be handled by downstream pipeline.
        assertThat(streamSharing.sharingInputEdge!!.rotationDegrees).isEqualTo(SENSOR_ROTATION)
        assertThat(streamSharing.sharingInputEdge!!.cropRect).isEqualTo(Rect(0, 0, 600, 400))
        assertThat(streamSharing.sharingInputEdge!!.isMirroring).isEqualTo(true)
    }

    @Test
    fun effectWithTransformationPassthrough_surfaceProcessorIsNotApplied() {
        // Arrange: create an effect with passthrough transformation.
        effect =
            FakeSurfaceEffect(
                PREVIEW or VIDEO_CAPTURE,
                CameraEffect.TRANSFORMATION_PASSTHROUGH,
                effectProcessor
            )
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1),
                useCaseConfigFactory
            )
        streamSharing.effect = effect
        // Act: bind effect.
        streamSharing.bindToCamera(frontCamera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        // Assert: surface processor is not applied, the sharing input edge is the camera edge.
        assertThat(streamSharing.sharingInputEdge).isEqualTo(streamSharing.cameraEdge)
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun invokeParentSessionCaptureCallbacks_receivedByChildren() {
        // Arrange.
        val streamUseCaseIntDef = 3L
        val sessionConfig = extendChildAndReturnParentSessionConfig {
            it.setStreamUseCase(streamUseCaseIntDef)
        }

        // Assert: the repeating callback has size 2 (VirtualCamera callback and the child callback)
        assertThat(
                sessionConfig.implementationOptions.retrieveOption(
                    Camera2ImplConfig.STREAM_USE_CASE_OPTION
                )
            )
            .isEqualTo(streamUseCaseIntDef)
    }

    @Test
    fun configureChildWithSessionCaptureCallback_verifyParentSessionCaptureCallbacksCounts() {
        // Arrange.
        val childSessionCaptureCallback = FakeSessionCaptureCallback()
        val sessionConfig = extendChildAndReturnParentSessionConfig {
            it.setSessionCaptureCallback(childSessionCaptureCallback)
        }

        // Assert: the repeating callback has size 2 (VirtualCamera callback and the child callback)
        assertThat(sessionConfig.repeatingCameraCaptureCallbacks).hasSize(2)
        // Assert: the single callback has size of 1 (the child callback)
        assertThat(sessionConfig.singleCameraCaptureCallbacks).hasSize(1)
    }

    @Test
    fun invokeParentSessionStateCallbacks_receivedByChildren() {
        // Arrange.
        val childSessionStateCallback = FakeSessionStateCallback()
        val sessionConfig = extendChildAndReturnParentSessionConfig {
            it.setSessionStateCallback(childSessionStateCallback)
        }

        // Act: invoke the parent camera's callbacks.
        val parentCallback = sessionConfig.sessionStateCallbacks.single()
        parentCallback.onActive(mock(CameraCaptureSession::class.java))
        parentCallback.onClosed(mock(CameraCaptureSession::class.java))
        parentCallback.onConfigureFailed(mock(CameraCaptureSession::class.java))
        parentCallback.onConfigured(mock(CameraCaptureSession::class.java))
        parentCallback.onReady(mock(CameraCaptureSession::class.java))

        // Assert: the child receives the callbacks.
        assertThat(childSessionStateCallback.onActiveCalled).isTrue()
        assertThat(childSessionStateCallback.onClosedCalled).isTrue()
        assertThat(childSessionStateCallback.onConfigureFailedCalled).isTrue()
        assertThat(childSessionStateCallback.onConfiguredCalled).isTrue()
        assertThat(childSessionStateCallback.onReadyCalled).isTrue()
    }

    @Test
    fun invokeParentCameraStateCallbacks_receivedByChildren() {
        // Arrange: create child with DeviceStateCallback
        val childCameraStateCallback = FakeCameraStateCallback()
        val sessionConfig = extendChildAndReturnParentSessionConfig {
            it.setDeviceStateCallback(childCameraStateCallback)
        }

        // Act: invoke the parent camera's callbacks.
        val parentCallback = sessionConfig.deviceStateCallbacks.single()
        parentCallback.onOpened(Mockito.mock(CameraDevice::class.java))
        parentCallback.onError(Mockito.mock(CameraDevice::class.java), 0)
        parentCallback.onDisconnected(Mockito.mock(CameraDevice::class.java))

        // Assert: the child receives the callbacks.
        assertThat(childCameraStateCallback.onOpenedCalled).isTrue()
        assertThat(childCameraStateCallback.onDisconnectedCalled).isTrue()
        assertThat(childCameraStateCallback.onErrorCalled).isTrue()
    }

    @Test
    fun childTakingPicture_getJpegQuality() {
        // Arrange: set up StreamSharing with min latency ImageCapture as child
        val imageCapture =
            ImageCapture.Builder().setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY).build()
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1, imageCapture),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        imageCapture.targetRotation = Surface.ROTATION_90

        // Act: the child takes a picture.
        imageCapture.takePicture(
            directExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {}
            }
        )
        shadowOf(getMainLooper()).idle()

        // Assert: the jpeg quality of min latency capture is 95.
        assertThat(sharingProcessor.jpegQuality).isEqualTo(95)
        assertThat(sharingProcessor.rotationDegrees).isEqualTo(270)
    }

    @Test
    fun getParentSurfacePriority_isHighestChildrenPriority() {
        assertThat(
                streamSharing
                    .mergeConfigs(
                        camera.cameraInfoInternal, /*extendedConfig*/
                        null, /*cameraDefaultConfig*/
                        null
                    )
                    .surfaceOccupancyPriority
            )
            .isEqualTo(2)
    }

    @Test
    fun getParentDynamicRange_isIntersectionOfChildrenDynamicRanges() {
        val unspecifiedChild =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(1)
                    .setDynamicRange(DynamicRange.UNSPECIFIED)
                    .useCaseConfig
            )
        val hdrChild =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(2)
                    .setDynamicRange(HLG_10_BIT)
                    .useCaseConfig
            )
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(unspecifiedChild, hdrChild),
                useCaseConfigFactory
            )
        assertThat(
                streamSharing
                    .mergeConfigs(
                        camera.cameraInfoInternal, /*extendedConfig*/
                        null, /*cameraDefaultConfig*/
                        null
                    )
                    .dynamicRange
            )
            .isEqualTo(HLG_10_BIT)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getParentDynamicRange_exception_whenChildrenDynamicRangesConflict() {
        val sdrChild =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(1)
                    .setDynamicRange(SDR)
                    .useCaseConfig
            )
        val hdrChild =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(2)
                    .setDynamicRange(HLG_10_BIT)
                    .useCaseConfig
            )
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(sdrChild, hdrChild),
                useCaseConfigFactory
            )
        streamSharing.mergeConfigs(
            camera.cameraInfoInternal, /*extendedConfig*/
            null, /*cameraDefaultConfig*/
            null
        )
    }

    @Test
    fun verifySupportedEffects() {
        assertThat(streamSharing.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isTrue()
        assertThat(
                streamSharing.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)
            )
            .isTrue()
        assertThat(streamSharing.isEffectTargetsSupported(IMAGE_CAPTURE)).isFalse()
        assertThat(streamSharing.isEffectTargetsSupported(PREVIEW)).isFalse()
        assertThat(streamSharing.isEffectTargetsSupported(VIDEO_CAPTURE)).isFalse()
    }

    @Test
    fun hasEffect_createEffectNode() {
        // Arrange: set an effect on StreamSharing.
        streamSharing.bindToCamera(frontCamera, null, null, defaultConfig)
        streamSharing.effect = effect
        // Act: create pipeline
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        shadowOf(getMainLooper()).idle()
        // Assert: processors received input and output Surfaces.
        assertThat(effectProcessor.surfaceRequest).isNotNull()
        assertThat(effectProcessor.surfaceOutputs).isNotEmpty()
        assertThat(sharingProcessor.surfaceRequest).isNotNull()
        // Assert: effect implementation receives correct transformation.
        var transformationInfo: SurfaceRequest.TransformationInfo? = null
        effectProcessor.surfaceRequest!!.setTransformationInfoListener(mainThreadExecutor()) {
            transformationInfo = it
        }
        shadowOf(getMainLooper()).idle()
        assertThat(transformationInfo).isNotNull()
        assertThat(transformationInfo!!.rotationDegrees).isEqualTo(SENSOR_ROTATION)
        assertThat(transformationInfo!!.isMirroring).isTrue()
        // Act: unbind StreamSharing.
        streamSharing.unbindFromCamera(frontCamera)
        shadowOf(getMainLooper()).idle()
        // Assert: the processors received signals to release the Surfaces.
        assertThat(effectProcessor.isInputSurfaceReleased).isTrue()
        assertThat(effectProcessor.isOutputSurfaceRequestedToClose.values.single()).isTrue()
        assertThat(sharingProcessor.isInputSurfaceReleased).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun feedMetadataToParent_propagatesToChildren() {
        // Arrange: set tag bundle in children SessionConfig.
        val key = "key"
        val value = "value"
        val result1 = child1.setTagBundleOnSessionConfigAsync(key, value)
        val result2 = child2.setTagBundleOnSessionConfigAsync(key, value)
        streamSharing.bindToCamera(camera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Act: feed metadata to the parent.
        streamSharing.sessionConfig.repeatingCameraCaptureCallbacks
            .single()
            .onCaptureCompleted(CaptureConfig.DEFAULT_ID, FakeCameraCaptureResult())

        // Assert: children receives the metadata with the tag bundle overridden.
        assertThat(result1.getCompleted().tagBundle.getTag(key)).isEqualTo(value)
        assertThat(result2.getCompleted().tagBundle.getTag(key)).isEqualTo(value)
    }

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenCreatePipeline() {
        // Arrange: set up StreamSharing with ImageCapture as child
        val imageCapture = ImageCapture.Builder().build()
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1, imageCapture),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update stream specification.
        val streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, testImplementationOptionValue)
        streamSharing.onSuggestedStreamSpecUpdated(
            StreamSpec.builder(size).setImplementationOptions(streamSpecOptions).build(),
            null
        )

        // Assert: the session config gets the correct implementation options from stream
        // specification.
        assertThat(
                streamSharing.sessionConfig.implementationOptions.retrieveOption(
                    testImplementationOption
                )
            )
            .isEqualTo(testImplementationOptionValue)
    }

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenUpdateStreamSpecImplOptions() {
        // Arrange: set up StreamSharing with ImageCapture as child with initial stream
        // specification implementation options.
        val imageCapture = ImageCapture.Builder().build()
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1, imageCapture),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)
        var streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, testImplementationOptionValue)
        streamSharing.updateSuggestedStreamSpec(
            StreamSpec.builder(size).setImplementationOptions(streamSpecOptions).build(),
            null
        )

        // Act: update stream specification implementation options.
        val newImplementationOptionValue = 6
        streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, newImplementationOptionValue)
        streamSharing.updateSuggestedStreamSpecImplementationOptions(streamSpecOptions)

        // Assert: the session config gets the correct implementation options from stream
        // specification.
        assertThat(
                streamSharing.sessionConfig.implementationOptions.retrieveOption(
                    testImplementationOption
                )
            )
            .isEqualTo(newImplementationOptionValue)
    }

    @Test
    fun sessionConfigIsSdr_whenUpdateStreamSpecWithDefaultDynamicRangeSettings() {
        // Arrange.
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update stream specification.
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Assert: the session config gets the correct dynamic range.
        val outputConfigs = streamSharing.sessionConfig.outputConfigs
        assertThat(outputConfigs).hasSize(1)
        assertThat(outputConfigs[0].dynamicRange).isEqualTo(SDR)
    }

    @Test
    fun sessionConfigIsHdr_whenUpdateStreamSpecWithHdr() {
        // Arrange.
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update stream specification.
        streamSharing.onSuggestedStreamSpecUpdated(
            StreamSpec.builder(size).setDynamicRange(HLG_10_BIT).build(),
            null
        )

        // Assert: the session config gets the correct dynamic range.
        val outputConfigs = streamSharing.sessionConfig.outputConfigs
        assertThat(outputConfigs).hasSize(1)
        assertThat(outputConfigs[0].dynamicRange).isEqualTo(HLG_10_BIT)
    }

    private fun extendChildAndReturnParentSessionConfig(
        extender: (Camera2Interop.Extender<Preview>) -> Unit
    ): SessionConfig {
        val previewBuilder = Preview.Builder().apply { extender(Camera2Interop.Extender(this)) }
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(previewBuilder.build()),
                Camera2UseCaseConfigFactory(context)
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        return streamSharing.sessionConfig
    }

    private fun FakeUseCase.setTagBundleOnSessionConfigAsync(
        key: String,
        value: String
    ): Deferred<CameraCaptureResult> {
        val deferredResult = CompletableDeferred<CameraCaptureResult>()
        this.setSessionConfigSupplier {
            val builder = SessionConfig.Builder()
            builder.addTag(key, value)
            builder.addRepeatingCameraCaptureCallback(
                object : CameraCaptureCallback() {
                    override fun onCaptureCompleted(
                        captureConfig: Int,
                        cameraCaptureResult: CameraCaptureResult
                    ) {
                        deferredResult.complete(cameraCaptureResult)
                    }
                }
            )
            builder.build()
        }
        return deferredResult
    }

    private fun FakeUseCase.setSurfaceOnSessionConfig(surface: Surface) {
        this.setSessionConfigSupplier {
            SessionConfig.Builder()
                .addSurface(
                    object : DeferrableSurface(size, ImageFormat.PRIVATE) {
                        override fun provideSurface(): ListenableFuture<Surface> {
                            return Futures.immediateFuture(surface)
                        }
                    }
                )
                .build()
        }
    }

    @Test
    fun updateStreamSpec_propagatesToChildren() {
        // Arrange: bind StreamSharing to the camera.
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update suggested specs.
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Assert: StreamSharing pipeline created.
        val node = streamSharing.sharingNode!!
        val cameraEdge = streamSharing.cameraEdge!!
        assertThat(streamSharing.cameraEdge).isNotNull()
        assertThat(streamSharing.sharingNode).isNotNull()
        assertThat(streamSharing.sessionConfig.repeatingCameraCaptureCallbacks).isNotEmpty()
        // Assert: specs propagated to children.
        assertThat(child1.attachedStreamSpec).isNotNull()
        assertThat(child2.attachedStreamSpec).isNotNull()

        // Act: unbind StreamSharing.
        streamSharing.unbindFromCamera(camera)

        // Assert: pipeline is cleared.
        assertThat(streamSharing.cameraEdge).isNull()
        assertThat(streamSharing.sharingNode).isNull()
        assertThat((node.surfaceProcessor as FakeSurfaceProcessorInternal).isReleased).isTrue()
        assertThat(cameraEdge.isClosed).isTrue()
        assertThat(child1.attachedStreamSpec).isNull()
        assertThat(child2.attachedStreamSpec).isNull()
    }

    @Test
    fun onError_restartsPipeline() {
        // Arrange: bind stream sharing and update specs.
        streamSharing.bindToCamera(camera, null, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)
        val cameraEdge = streamSharing.cameraEdge
        val node = streamSharing.sharingNode
        // Arrange: given children new Surfaces.
        val surfaceTexture1 = SurfaceTexture(0)
        val surface1 = Surface(surfaceTexture1)
        child1.notifyActiveForTesting()
        child1.setSurfaceOnSessionConfig(surface1)
        val surfaceTexture2 = SurfaceTexture(0)
        val surface2 = Surface(surfaceTexture2)
        child2.notifyActiveForTesting()
        child2.setSurfaceOnSessionConfig(surface2)

        // Act: send error to StreamSharing
        val sessionConfig = streamSharing.sessionConfig
        sessionConfig.errorListener!!.onError(
            sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )
        shadowOf(getMainLooper()).idle()

        // Assert: StreamSharing and children pipeline are recreated.
        assertThat(streamSharing.cameraEdge).isNotSameInstanceAs(cameraEdge)
        assertThat(streamSharing.sharingNode).isNotSameInstanceAs(node)
        assertThat(child1.pipelineCreationCount).isEqualTo(2)
        assertThat(child2.pipelineCreationCount).isEqualTo(2)
        shadowOf(getMainLooper()).idle()
        // Assert: child Surface are propagated to StreamSharing.
        val child1Surface =
            streamSharing.virtualCameraAdapter.mChildrenEdges[child1]!!
                .deferrableSurfaceForTesting
                .surface
        assertThat(child1Surface.isDone).isTrue()
        assertThat(child1Surface.get()).isEqualTo(surface1)
        val child2Surface =
            streamSharing.virtualCameraAdapter.mChildrenEdges[child2]!!
                .deferrableSurfaceForTesting
                .surface
        assertThat(child2Surface.isDone).isTrue()
        assertThat(child2Surface.get()).isEqualTo(surface2)

        // Cleanup.
        surfaceTexture1.release()
        surface1.release()
        surfaceTexture2.release()
        surface2.release()
    }

    @Test
    fun bindChildToCamera_virtualCameraHasNoTransform() {
        // Act.
        streamSharing.bindToCamera(camera, null, null, null)
        // Assert.
        assertThat(child1.camera!!.hasTransform).isFalse()
        assertThat(child2.camera!!.hasTransform).isFalse()
    }

    @Test
    fun bindChildToCamera_virtualCameraHasNoRotationDegrees() {
        // Act.
        streamSharing.bindToCamera(frontCamera, null, null, null)
        // Assert.
        assertThat(child1.camera!!.cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
            .isEqualTo(0)
        assertThat(child2.camera!!.cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
            .isEqualTo(0)
    }

    @Test
    fun bindAndUnbindParent_propagatesToChildren() {
        // Assert: children not bound to camera by default.
        assertThat(child1.camera).isNull()
        assertThat(child2.camera).isNull()
        // Act: bind to camera.
        streamSharing.bindToCamera(camera, null, null, null)
        // Assert: children bound to the virtual camera.
        assertThat(child1.camera).isInstanceOf(VirtualCamera::class.java)
        assertThat(child1.mergedConfigRetrieved).isTrue()
        assertThat(child2.camera).isInstanceOf(VirtualCamera::class.java)
        assertThat(child2.mergedConfigRetrieved).isTrue()
        // Act: unbind.
        streamSharing.unbindFromCamera(camera)
        // Assert: children not bound.
        assertThat(child1.camera).isNull()
        assertThat(child2.camera).isNull()
    }

    @Test
    fun attachAndDetachParent_propagatesToChildren() {
        // Assert: children not attached by default.
        assertThat(child1.stateAttachedCount).isEqualTo(0)
        assertThat(child2.stateAttachedCount).isEqualTo(0)
        // Act: attach.
        streamSharing.onStateAttached()
        // Assert: children attached.
        assertThat(child1.stateAttachedCount).isEqualTo(1)
        assertThat(child2.stateAttachedCount).isEqualTo(1)
        // Act: detach.
        streamSharing.onStateDetached()
        // Assert: children not attached.
        assertThat(child1.stateAttachedCount).isEqualTo(0)
        assertThat(child2.stateAttachedCount).isEqualTo(0)
    }

    @Test
    fun getDefaultConfig_usesVideoCaptureType() {
        val config = streamSharing.getDefaultConfig(true, useCaseConfigFactory)!!

        assertThat(useCaseConfigFactory.lastRequestedCaptureType)
            .isEqualTo(UseCaseConfigFactory.CaptureType.STREAM_SHARING)
        assertThat(config.retrieveOption(OPTION_TARGET_CLASS, null))
            .isEqualTo(StreamSharing::class.java)
        assertThat(config.retrieveOption(OPTION_TARGET_NAME, null))
            .startsWith("androidx.camera.core.streamsharing.StreamSharing-")
    }

    @Test
    fun getDefaultConfig_getCaptureTypes() {
        val config: StreamSharingConfig =
            (streamSharing.getDefaultConfig(true, useCaseConfigFactory) as StreamSharingConfig?)!!
        assertThat(config.captureTypes.size).isEqualTo(2)
        assertThat(config.captureTypes[0]).isEqualTo(CaptureType.PREVIEW)
        assertThat(config.captureTypes[1]).isEqualTo(CaptureType.PREVIEW)
    }

    @Test
    fun getParentPreviewStabilizationMode_isPreviewChildMode() {
        val preview = Preview.Builder().setPreviewStabilizationEnabled(true).build()
        val videoCapture =
            VideoCapture.Builder(Recorder.Builder().build())
                .setVideoStabilizationEnabled(false)
                .build()

        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, videoCapture),
                useCaseConfigFactory
            )
        assertThat(
                streamSharing
                    .mergeConfigs(
                        camera.cameraInfoInternal, /*extendedConfig*/
                        null, /*cameraDefaultConfig*/
                        null
                    )
                    .previewStabilizationMode
            )
            .isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun getParentVideoStabilizationMode_isVideoCaptureChildMode() {
        val preview = Preview.Builder().setPreviewStabilizationEnabled(false).build()
        val videoCapture =
            VideoCapture.Builder(Recorder.Builder().build())
                .setVideoStabilizationEnabled(true)
                .build()

        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, videoCapture),
                useCaseConfigFactory
            )
        assertThat(
                streamSharing
                    .mergeConfigs(
                        camera.cameraInfoInternal, /*extendedConfig*/
                        null, /*cameraDefaultConfig*/
                        null
                    )
                    .videoStabilizationMode
            )
            .isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun propagateChildrenTemplate_noVideoCapture() {
        // Arrange.
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, imageCapture),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update stream specification.
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Assert:
        assertThat(streamSharing.sessionConfig.templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun propagateChildrenTemplate_hasVideoCapture() {
        // Arrange.
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, imageCapture, videoCapture),
                useCaseConfigFactory
            )
        streamSharing.bindToCamera(camera, null, null, defaultConfig)

        // Act: update stream specification.
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build(), null)

        // Assert:
        assertThat(streamSharing.sessionConfig.templateType).isEqualTo(TEMPLATE_RECORD)
    }

    @Test
    fun getParentTargetFrameRate_whenBothChildrenTargetFrameRateNotSpecified() =
        getParentTargetFrameRate_fromChildrenTargetFrameRates(
            null,
            null,
            StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
        )

    @Test
    fun getParentTargetFrameRate_whenFirstChildTargetFrameRateNotSpecified() =
        getParentTargetFrameRate_fromChildrenTargetFrameRates(
            null,
            Range.create(15, 30),
            Range.create(15, 30)
        )

    @Test
    fun getParentTargetFrameRate_whenSecondChildTargetFrameRateNotSpecified() =
        getParentTargetFrameRate_fromChildrenTargetFrameRates(
            Range.create(15, 30),
            null,
            Range.create(15, 30)
        )

    @Test
    fun getParentTargetFrameRate_isIntersectionOfChildrenTargetFrameRates() =
        getParentTargetFrameRate_fromChildrenTargetFrameRates(
            Range.create(15, 30),
            Range.create(25, 40),
            Range.create(25, 30)
        )

    @Test
    fun getParentTargetFrameRate_isExtendedRangeOfChildrenTargetFrameRates() =
        getParentTargetFrameRate_fromChildrenTargetFrameRates(
            Range.create(15, 30),
            Range.create(60, 60),
            Range.create(15, 60)
        )

    private fun getParentTargetFrameRate_fromChildrenTargetFrameRates(
        targetFrameRate1: Range<Int>?,
        targetFrameRate2: Range<Int>?,
        expectedFrameRate: Range<Int>
    ) {
        val child1 =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(1)
                    .apply { targetFrameRate1?.let { setTargetFrameRate(it) } }
                    .useCaseConfig
            )
        val child2 =
            FakeUseCase(
                FakeUseCaseConfig.Builder()
                    .setSurfaceOccupancyPriority(2)
                    .apply { targetFrameRate2?.let { setTargetFrameRate(it) } }
                    .useCaseConfig
            )
        streamSharing =
            StreamSharing(
                camera,
                secondaryCamera,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(child1, child2),
                useCaseConfigFactory
            )
        assertThat(
                streamSharing
                    .mergeConfigs(
                        camera.cameraInfoInternal, /*extendedConfig*/
                        null, /*cameraDefaultConfig*/
                        null
                    )
                    .targetFrameRate
            )
            .isEqualTo(expectedFrameRate)
    }
}
