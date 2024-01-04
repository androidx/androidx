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
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
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
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCaptureResult
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [StreamSharing].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamSharingTest {

    companion object {
        private const val SENSOR_ROTATION = 270
    }

    private val child1 = FakeUseCase(
        FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(1).useCaseConfig
    )
    private val child2 = FakeUseCase(
        FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2).useCaseConfig
    )
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private val camera = FakeCamera()
    private val frontCamera =
        FakeCamera(null, FakeCameraInfoInternal(SENSOR_ROTATION, LENS_FACING_FRONT))
    private lateinit var streamSharing: StreamSharing
    private val size = Size(800, 600)
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
        streamSharing = StreamSharing(camera, setOf(child1, child2), useCaseConfigFactory)
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
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun childTakingPicture_getJpegQuality() {
        // Arrange: set up StreamSharing with min latency ImageCapture as child
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_90)
            .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        streamSharing = StreamSharing(camera, setOf(child1, imageCapture), useCaseConfigFactory)
        streamSharing.bindToCamera(camera, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build())

        // Act: the child takes a picture.
        imageCapture.takePicture(directExecutor(), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {}
        })
        shadowOf(getMainLooper()).idle()

        // Assert: the jpeg quality of min latency capture is 95.
        assertThat(sharingProcessor.jpegQuality).isEqualTo(95)
        assertThat(sharingProcessor.rotationDegrees).isEqualTo(270)
    }

    @Test
    fun getParentSurfacePriority_isHighestChildrenPriority() {
        assertThat(
            streamSharing.mergeConfigs(
                camera.cameraInfoInternal, /*extendedConfig*/null, /*cameraDefaultConfig*/null
            ).surfaceOccupancyPriority
        ).isEqualTo(2)
    }

    @Test
    fun getParentDynamicRange_isIntersectionOfChildrenDynamicRanges() {
        val unspecifiedChild = FakeUseCase(
            FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(1)
                .setDynamicRange(DynamicRange.UNSPECIFIED).useCaseConfig
        )
        val hdrChild = FakeUseCase(
            FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2)
                .setDynamicRange(DynamicRange.HLG_10_BIT).useCaseConfig
        )
        streamSharing =
            StreamSharing(camera, setOf(unspecifiedChild, hdrChild), useCaseConfigFactory)
        assertThat(
            streamSharing.mergeConfigs(
                camera.cameraInfoInternal, /*extendedConfig*/null, /*cameraDefaultConfig*/null
            ).dynamicRange
        ).isEqualTo(DynamicRange.HLG_10_BIT)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getParentDynamicRange_exception_whenChildrenDynamicRangesConflict() {
        val sdrChild = FakeUseCase(
            FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(1)
                .setDynamicRange(DynamicRange.SDR).useCaseConfig
        )
        val hdrChild = FakeUseCase(
            FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(2)
                .setDynamicRange(DynamicRange.HLG_10_BIT).useCaseConfig
        )
        streamSharing = StreamSharing(camera, setOf(sdrChild, hdrChild), useCaseConfigFactory)
        streamSharing.mergeConfigs(
            camera.cameraInfoInternal, /*extendedConfig*/null, /*cameraDefaultConfig*/null
        )
    }

    @Test
    fun verifySupportedEffects() {
        assertThat(streamSharing.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isTrue()
        assertThat(
            streamSharing.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)
        ).isTrue()
        assertThat(streamSharing.isEffectTargetsSupported(IMAGE_CAPTURE)).isFalse()
        assertThat(streamSharing.isEffectTargetsSupported(PREVIEW)).isFalse()
        assertThat(streamSharing.isEffectTargetsSupported(VIDEO_CAPTURE)).isFalse()
    }

    @Test
    fun hasEffect_createEffectNode() {
        // Arrange: set an effect on StreamSharing.
        streamSharing.bindToCamera(frontCamera, null, defaultConfig)
        streamSharing.effect = effect
        // Act: create pipeline
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build())
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
        assertThat(transformationInfo!!.mirroring).isTrue()
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
        streamSharing.bindToCamera(camera, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build())

        // Act: feed metadata to the parent.
        streamSharing.sessionConfig.repeatingCameraCaptureCallbacks.single()
            .onCaptureCompleted(FakeCameraCaptureResult())

        // Assert: children receives the metadata with the tag bundle overridden.
        assertThat(result1.getCompleted().tagBundle.getTag(key)).isEqualTo(value)
        assertThat(result2.getCompleted().tagBundle.getTag(key)).isEqualTo(value)
    }

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenCreatePipeline() {
        // Arrange: set up StreamSharing with ImageCapture as child
        val imageCapture = ImageCapture.Builder().build()
        streamSharing = StreamSharing(camera, setOf(child1, imageCapture), useCaseConfigFactory)
        streamSharing.bindToCamera(camera, null, defaultConfig)

        // Act: update stream specification.
        val streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, testImplementationOptionValue)
        streamSharing.onSuggestedStreamSpecUpdated(
            StreamSpec.builder(size).setImplementationOptions(streamSpecOptions).build()
        )

        // Assert: the session config gets the correct implementation options from stream
        // specification.
        assertThat(
            streamSharing.sessionConfig.implementationOptions.retrieveOption(
                testImplementationOption
            )
        ).isEqualTo(testImplementationOptionValue)
    }

    @Test
    fun sessionConfigHasStreamSpecImplementationOptions_whenUpdateStreamSpecImplOptions() {
        // Arrange: set up StreamSharing with ImageCapture as child with initial stream
        // specification implementation options.
        val imageCapture = ImageCapture.Builder().build()
        streamSharing = StreamSharing(camera, setOf(child1, imageCapture), useCaseConfigFactory)
        streamSharing.bindToCamera(camera, null, defaultConfig)
        var streamSpecOptions = MutableOptionsBundle.create()
        streamSpecOptions.insertOption(testImplementationOption, testImplementationOptionValue)
        streamSharing.updateSuggestedStreamSpec(
            StreamSpec.builder(size).setImplementationOptions(streamSpecOptions).build()
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
        ).isEqualTo(newImplementationOptionValue)
    }

    private fun FakeUseCase.setTagBundleOnSessionConfigAsync(
        key: String,
        value: String
    ): Deferred<CameraCaptureResult> {
        val deferredResult = CompletableDeferred<CameraCaptureResult>()
        this.setSessionConfigSupplier {
            val builder = SessionConfig.Builder()
            builder.addTag(key, value)
            builder.addRepeatingCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCompleted(cameraCaptureResult: CameraCaptureResult) {
                    deferredResult.complete(cameraCaptureResult)
                }
            })
            builder.build()
        }
        return deferredResult
    }

    private fun FakeUseCase.setSurfaceOnSessionConfig(surface: Surface) {
        this.setSessionConfigSupplier {
            SessionConfig.Builder()
                .addSurface(object : DeferrableSurface(size, ImageFormat.PRIVATE) {
                    override fun provideSurface(): ListenableFuture<Surface> {
                        return Futures.immediateFuture(surface)
                    }
                }).build()
        }
    }

    @Test
    fun updateStreamSpec_propagatesToChildren() {
        // Arrange: bind StreamSharing to the camera.
        streamSharing.bindToCamera(camera, null, defaultConfig)

        // Act: update suggested specs.
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build())

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
        streamSharing.bindToCamera(camera, null, defaultConfig)
        streamSharing.onSuggestedStreamSpecUpdated(StreamSpec.builder(size).build())
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
        sessionConfig.errorListeners.single()
            .onError(sessionConfig, SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
        shadowOf(getMainLooper()).idle()

        // Assert: StreamSharing and children pipeline are recreated.
        assertThat(streamSharing.cameraEdge).isNotSameInstanceAs(cameraEdge)
        assertThat(streamSharing.sharingNode).isNotSameInstanceAs(node)
        assertThat(child1.pipelineCreationCount).isEqualTo(2)
        assertThat(child2.pipelineCreationCount).isEqualTo(2)
        shadowOf(getMainLooper()).idle()
        // Assert: child Surface are propagated to StreamSharing.
        val child1Surface =
            streamSharing.virtualCamera.mChildrenEdges[child1]!!.deferrableSurfaceForTesting.surface
        assertThat(child1Surface.isDone).isTrue()
        assertThat(child1Surface.get()).isEqualTo(surface1)
        val child2Surface =
            streamSharing.virtualCamera.mChildrenEdges[child2]!!.deferrableSurfaceForTesting.surface
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
        streamSharing.bindToCamera(camera, null, null)
        // Assert.
        assertThat(child1.camera!!.hasTransform).isFalse()
        assertThat(child2.camera!!.hasTransform).isFalse()
    }

    @Test
    fun bindAndUnbindParent_propagatesToChildren() {
        // Assert: children not bound to camera by default.
        assertThat(child1.camera).isNull()
        assertThat(child2.camera).isNull()
        // Act: bind to camera.
        streamSharing.bindToCamera(camera, null, null)
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
        assertThat(
            config.retrieveOption(
                OPTION_TARGET_CLASS,
                null
            )
        ).isEqualTo(StreamSharing::class.java)
        assertThat(
            config.retrieveOption(
                OPTION_TARGET_NAME,
                null
            )
        ).startsWith("androidx.camera.core.streamsharing.StreamSharing-")
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
        val videoCapture = VideoCapture.Builder(Recorder.Builder().build())
            .setVideoStabilizationEnabled(false).build()

        streamSharing =
            StreamSharing(camera, setOf(preview, videoCapture), useCaseConfigFactory)
        assertThat(
            streamSharing.mergeConfigs(
                camera.cameraInfoInternal, /*extendedConfig*/null, /*cameraDefaultConfig*/null
            ).previewStabilizationMode
        ).isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun getParentVideoStabilizationMode_isVideoCaptureChildMode() {
        val preview = Preview.Builder().setPreviewStabilizationEnabled(false).build()
        val videoCapture = VideoCapture.Builder(Recorder.Builder().build())
            .setVideoStabilizationEnabled(true).build()

        streamSharing =
            StreamSharing(camera, setOf(preview, videoCapture), useCaseConfigFactory)
        assertThat(
            streamSharing.mergeConfigs(
                camera.cameraInfoInternal, /*extendedConfig*/null, /*cameraDefaultConfig*/null
            ).videoStabilizationMode
        ).isEqualTo(StabilizationMode.ON)
    }
}
