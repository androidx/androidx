/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule.Companion.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import androidx.media3.effect.RgbFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ConcurrentCameraTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule(order = 0) val labTest: LabTestRule = LabTestRule()

    @get:Rule(order = 1)
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    companion object {
        private const val PREVIEW_FRAMES = 5
        private const val PREVIEW_TIMEOUT_SECONDS = 10L
        private const val CAMERA_CLOSE_TIMEOUT_SECONDS = 10L
        private const val IMAGE_CAPTURE_TIMEOUT_SECONDS = 10L
        private const val VIDEO_DURATION_LIMIT_MS = 1_500L
        // The recording finalizes after the duration limit is reached by the encoded data, which
        // also includes the first key frame latency and the encoder stop timeouts. These can take
        // several seconds on slow devices such as emulators.
        private const val VIDEO_FINALIZE_TIMEOUT_MS = 15_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private val cameraParameters = mutableListOf<SingleCameraParameter>()

    @Before
    fun setUp(): Unit = runBlocking {
        assumeFalse(
            "Test fails on cuttlefish b/467708340",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
        assumeTrue(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT))

        ProcessCameraProvider.configureInstance(cameraConfig)

        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        try {
            cameraParameters.forEach { it.cleanup() }
        } finally {
            ProcessCameraProvider.shutdown()[20, TimeUnit.SECONDS]
        }
    }

    private fun assumeConcurrentCameraSelectors(): Pair<CameraSelector, CameraSelector> =
        cameraProvider.availableConcurrentCameraInfos.firstNotNullOfOrNull { combination ->
            val front = combination.firstOrNull {
                it.lensFacing == CameraSelector.LENS_FACING_FRONT
            }
            val back = combination.firstOrNull { it.lensFacing == CameraSelector.LENS_FACING_BACK }
            if (front != null && back != null) {
                front.cameraSelector to back.cameraSelector
            } else {
                null
            }
        }
            ?: throw AssumptionViolatedException(
                "No supported front+back concurrent camera combination available"
            )

    @Test
    fun testConcurrentCameraV1_preview_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val primary = SingleCameraParameter(cameraSelector = frontSelector)
        val secondary = SingleCameraParameter(cameraSelector = backSelector)

        // Act.
        val concurrentCamera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                )
            }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(2)
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
    }

    @Test
    fun testConcurrentCameraV1_preview_stopAndStart_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val lifecycleOwner = FakeLifecycleOwner()
        val primary =
            SingleCameraParameter(lifecycleOwner = lifecycleOwner, cameraSelector = frontSelector)
        val secondary =
            SingleCameraParameter(lifecycleOwner = lifecycleOwner, cameraSelector = backSelector)

        // Act.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
            )
        }
        // lifecycle starts after binding
        lifecycleOwner.startAndResume()
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()

        lifecycleOwner.pauseAndStop()
        assertBothCamerasClosed(primary, secondary)

        primary.reset()
        secondary.reset()
        lifecycleOwner.startAndResume()

        // Assert.
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
    }

    private suspend fun assertBothCamerasClosed(
        primary: SingleCameraParameter,
        secondary: SingleCameraParameter,
    ) {
        for (selector in listOf(primary.cameraSelector, secondary.cameraSelector)) {
            val info = cameraProvider.getCameraInfo(selector)
            val closedState =
                withContext(Dispatchers.Main) {
                    withTimeoutOrNull(CAMERA_CLOSE_TIMEOUT_SECONDS.seconds) {
                        info.cameraState.asFlow().first { it.type == CameraState.Type.CLOSED }
                    }
                }
            assertWithMessage(
                    "Camera (lensFacing=${info.lensFacing}) did not reach CLOSED within " +
                        "${CAMERA_CLOSE_TIMEOUT_SECONDS}s"
                )
                .that(closedState)
                .isNotNull()
        }
    }

    @Test
    fun testConcurrentCameraV1_previewImageCapture_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val primary =
            SingleCameraParameter(cameraSelector = frontSelector, enableImageCapture = true)
        val secondary =
            SingleCameraParameter(cameraSelector = backSelector, enableImageCapture = true)

        // Act.
        val concurrentCamera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                )
            }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(2)
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
        primary.assertCanCaptureImages()
        secondary.assertCanCaptureImages()
    }

    @Test
    fun testConcurrentCameraV1_previewVideoCapture_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val primary =
            SingleCameraParameter(cameraSelector = frontSelector, enableVideoCapture = true)
        val secondary =
            SingleCameraParameter(cameraSelector = backSelector, enableVideoCapture = true)

        // Act.
        val concurrentCamera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                )
            }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(2)
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
        primary.startRecordingVideos()
        secondary.startRecordingVideos()
        primary.assertVideoRecorded()
        secondary.assertVideoRecorded()
    }

    @Test
    fun testConcurrentCameraV1_previewVideoCapture_withOneForEachTargetEffect_canWork() =
        runBlocking {
            val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = frontSelector,
                    enableVideoCapture = true,
                    effect = createOneForEachTargetEffect(),
                )
            val secondary =
                SingleCameraParameter(
                    cameraSelector = backSelector,
                    enableVideoCapture = true,
                    effect = createOneForEachTargetEffect(),
                )

            // Act.
            val concurrentCamera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                    )
                }

            // Assert.
            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(2)
            primary.assertPreviewFramesReceived()
            secondary.assertPreviewFramesReceived()
            primary.startRecordingVideos()
            secondary.startRecordingVideos()
            primary.assertVideoRecorded()
            secondary.assertVideoRecorded()
        }

    @Test
    fun testConcurrentCameraV1_previewVideoCapture_withOneForAllTargetsEffect_canWork() =
        runBlocking {
            val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = frontSelector,
                    enableVideoCapture = true,
                    effect = createOneForAllTargetsEffect(),
                )
            val secondary =
                SingleCameraParameter(
                    cameraSelector = backSelector,
                    enableVideoCapture = true,
                    effect = createOneForAllTargetsEffect(),
                )

            // Act.
            val concurrentCamera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                    )
                }

            // Assert.
            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(2)
            primary.assertPreviewFramesReceived()
            secondary.assertPreviewFramesReceived()
            primary.startRecordingVideos()
            secondary.startRecordingVideos()
            primary.assertVideoRecorded()
            secondary.assertVideoRecorded()
        }

    @Test
    fun testConcurrentCameraV1_previewVideoCaptureImageCapture_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = frontSelector,
                enableImageCapture = true,
                enableVideoCapture = true,
            )
        val secondary =
            SingleCameraParameter(
                cameraSelector = backSelector,
                enableImageCapture = true,
                enableVideoCapture = true,
            )

        // Act.
        val concurrentCamera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                )
            }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(2)
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
        primary.assertCanCaptureImages()
        secondary.assertCanCaptureImages()
        primary.startRecordingVideos()
        secondary.startRecordingVideos()
        primary.assertVideoRecorded()
        secondary.assertVideoRecorded()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConcurrentCameraV1_onlyOneCamera_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        // Arrange.
        val primary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        // Act & Assert.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(listOf(primary.getSingleCameraConfig()))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConcurrentCameraV1_moreThanTwoCameras_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
        val secondary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)
        val tertiary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)

        // Act & Assert.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                listOf(
                    primary.getSingleCameraConfig(),
                    secondary.getSingleCameraConfig(),
                    tertiary.getSingleCameraConfig(),
                )
            )
        }
    }

    @Test
    fun testConcurrentCameraV2_previewVideoCapture_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = backSelector,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(0.0f, 0.0f)
                        .setScale(1.0f, 1.0f)
                        .build(),
            )

        val secondary =
            SingleCameraParameter(
                cameraSelector = frontSelector,
                useCaseGroup = primary.useCaseGroup,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(-0.3f, -0.4f)
                        .setScale(0.3f, 0.3f)
                        .build(),
            )

        // Act.
        val concurrentCamera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                )
            }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(1)
        primary.assertPreviewFramesReceived()
        primary.startRecordingVideos()
        primary.assertVideoRecorded()
    }

    @Test
    fun testConcurrentCameraV2_previewVideoCapture_stopAndStart_canWork() = runBlocking {
        val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

        // Arrange.
        val lifecycleOwner = FakeLifecycleOwner()
        val primary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = backSelector,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(0.0f, 0.0f)
                        .setScale(1.0f, 1.0f)
                        .build(),
            )

        val secondary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = frontSelector,
                useCaseGroup = primary.useCaseGroup,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(-0.3f, -0.4f)
                        .setScale(0.3f, 0.3f)
                        .build(),
            )

        // Act.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
            )
        }

        // lifecycle starts after binding
        lifecycleOwner.startAndResume()
        primary.assertPreviewFramesReceived()

        lifecycleOwner.pauseAndStop()
        assertBothCamerasClosed(primary, secondary)

        primary.reset()
        lifecycleOwner.startAndResume()

        // Assert.
        primary.assertPreviewFramesReceived()
        primary.startRecordingVideos()
        primary.assertVideoRecorded()
    }

    @Test
    fun testConcurrentCameraV2_previewVideoCapture_withOneForEachTargetEffect_canWork() =
        runBlocking {
            val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = backSelector,
                    enableVideoCapture = true,
                    effect = createOneForEachTargetEffect(),
                    compositionSettings =
                        CompositionSettings.Builder()
                            .setOffset(0.0f, 0.0f)
                            .setScale(1.0f, 1.0f)
                            .build(),
                )

            val secondary =
                SingleCameraParameter(
                    cameraSelector = frontSelector,
                    useCaseGroup = primary.useCaseGroup,
                    compositionSettings =
                        CompositionSettings.Builder()
                            .setOffset(-0.3f, -0.4f)
                            .setScale(0.3f, 0.3f)
                            .build(),
                )

            // Act.
            val concurrentCamera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                    )
                }

            // Assert.
            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(1)
            primary.assertPreviewFramesReceived()
            primary.startRecordingVideos()
            primary.assertVideoRecorded()
        }

    @Test
    fun testConcurrentCameraV2_previewVideoCapture_withOneForAllTargetsEffect_canWork() =
        runBlocking {
            val (frontSelector, backSelector) = assumeConcurrentCameraSelectors()

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = backSelector,
                    enableVideoCapture = true,
                    effect = createOneForAllTargetsEffect(),
                    compositionSettings =
                        CompositionSettings.Builder()
                            .setOffset(0.0f, 0.0f)
                            .setScale(1.0f, 1.0f)
                            .build(),
                )

            val secondary =
                SingleCameraParameter(
                    cameraSelector = frontSelector,
                    useCaseGroup = primary.useCaseGroup,
                    compositionSettings =
                        CompositionSettings.Builder()
                            .setOffset(-0.3f, -0.4f)
                            .setScale(0.3f, 0.3f)
                            .build(),
                )

            // Act.
            val concurrentCamera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        listOf(primary.getSingleCameraConfig(), secondary.getSingleCameraConfig())
                    )
                }

            // Assert.
            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(1)
            primary.assertPreviewFramesReceived()
            primary.startRecordingVideos()
            primary.assertVideoRecorded()
        }

    @Test(expected = IllegalArgumentException::class)
    fun testConcurrentCameraV2_moreThanTwoCamerasRecording_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(0.0f, 0.0f)
                        .setScale(1.0f, 1.0f)
                        .build(),
            )

        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup = primary.useCaseGroup,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(0.0f, 0.0f)
                        .setScale(1.0f, 1.0f)
                        .build(),
            )

        val tertiary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup = primary.useCaseGroup,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(-0.3f, -0.4f)
                        .setScale(0.3f, 0.3f)
                        .build(),
            )

        // Act & Assert.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                listOf(
                    primary.getSingleCameraConfig(),
                    secondary.getSingleCameraConfig(),
                    tertiary.getSingleCameraConfig(),
                )
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConcurrentCameraV2_oneCameraRecording_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder()
                        .setOffset(0.0f, 0.0f)
                        .setScale(1.0f, 1.0f)
                        .build(),
            )

        // Act & Assert.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(listOf(primary.getSingleCameraConfig()))
        }
    }

    private class DefaultEffect(targets: Int) :
        CameraEffect(
            targets,
            OUTPUT_OPTION_ONE_FOR_EACH_TARGET,
            TRANSFORMATION_ARBITRARY,
            CameraXExecutors.mainThreadExecutor(),
            DefaultSurfaceProcessor.Factory.newInstance(DynamicRange.SDR),
            {},
        ) {}

    private fun createOneForEachTargetEffect(): DefaultEffect {
        return DefaultEffect(CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE)
    }

    private suspend fun createOneForAllTargetsEffect(): Media3Effect {
        return withContext(Dispatchers.Main) {
            Media3Effect(
                    context,
                    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                    CameraXExecutors.mainThreadExecutor(),
                    Consumer {},
                )
                .also { it.setEffects(listOf(RgbFilter.createGrayscaleFilter())) }
        }
    }

    private inner class SingleCameraParameter(
        val lifecycleOwner: LifecycleOwner = fakeLifecycleOwner,
        val cameraSelector: CameraSelector,
        val enableImageCapture: Boolean = false,
        val enableVideoCapture: Boolean = false,
        val effect: CameraEffect? = null,
        var useCaseGroup: UseCaseGroup? = null,
        val compositionSettings: CompositionSettings? = null,
    ) {
        @Volatile var previewLatch = CountDownLatch(PREVIEW_FRAMES)
        val preview: Preview
        val imageCapture: ImageCapture?
        val videoCapture: VideoCapture<Recorder>?

        init {
            if (useCaseGroup == null) {
                preview = Preview.Builder().build()
                val useCaseGroupBuilder = UseCaseGroup.Builder().addUseCase(preview)
                if (enableImageCapture) {
                    imageCapture = ImageCapture.Builder().build()
                    useCaseGroupBuilder.addUseCase(imageCapture)
                } else {
                    imageCapture = null
                }

                if (enableVideoCapture) {
                    videoCapture = VideoCapture.withOutput<Recorder>(Recorder.Builder().build())
                    useCaseGroupBuilder.addUseCase(videoCapture)
                } else {
                    videoCapture = null
                }

                effect?.let { useCaseGroupBuilder.addEffect(it) }
                useCaseGroup = useCaseGroupBuilder.build()
            } else {
                preview = useCaseGroup!!.useCases.first { it is Preview } as Preview
                imageCapture =
                    useCaseGroup!!.useCases.firstOrNull { it is ImageCapture } as? ImageCapture
                @Suppress("UNCHECKED_CAST")
                videoCapture =
                    useCaseGroup!!.useCases.firstOrNull { it is VideoCapture<*> }
                        as? VideoCapture<Recorder>
            }
            cameraParameters.add(this)
        }

        suspend fun getSingleCameraConfig(): SingleCameraConfig {
            withContext(Dispatchers.Main) {
                if (preview.surfaceProvider == null) {
                    preview.setSurfaceProvider(
                        CameraXExecutors.mainThreadExecutor(),
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                            previewLatch.countDown()
                        },
                    )
                }
            }

            return if (compositionSettings == null) {
                SingleCameraConfig(cameraSelector, useCaseGroup!!, lifecycleOwner)
            } else {
                SingleCameraConfig(
                    cameraSelector,
                    useCaseGroup!!,
                    compositionSettings,
                    lifecycleOwner,
                )
            }
        }

        fun assertPreviewFramesReceived() {
            assertThat(previewLatch.await(PREVIEW_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        }

        fun reset() {
            previewLatch = CountDownLatch(PREVIEW_FRAMES)
        }

        suspend fun assertCanCaptureImages() {
            val captured = CompletableDeferred<Unit>()
            requireNotNull(imageCapture)
                .takePicture(
                    CameraXExecutors.ioExecutor(),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                image.use { it.toBitmap().recycle() }
                                captured.complete(Unit)
                            } catch (t: Throwable) {
                                captured.completeExceptionally(t)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            captured.completeExceptionally(exception)
                        }
                    },
                )
            val ok = withTimeoutOrNull(IMAGE_CAPTURE_TIMEOUT_SECONDS.seconds) { captured.await() }
            assertWithMessage("Image capture timed out after ${IMAGE_CAPTURE_TIMEOUT_SECONDS}s")
                .that(ok)
                .isNotNull()
        }

        var recordingFile: File? = null
        var activeRecording: Recording? = null
        var finalize: VideoRecordEvent.Finalize? = null
        val semaphoresForVideoSaved = Semaphore(0)

        fun cleanup() {
            activeRecording?.close()
            activeRecording = null
            recordingFile?.delete()
            recordingFile = null
        }

        fun assertVideoRecorded() {
            assertThat(recordingFile).isNotNull()
            assertThat(activeRecording).isNotNull()

            try {
                // Wait for finalize event to saved file.
                assertThat(
                        semaphoresForVideoSaved.tryAcquire(
                            VIDEO_FINALIZE_TIMEOUT_MS,
                            TimeUnit.MILLISECONDS,
                        )
                    )
                    .isTrue()
                // Verify.
                val uri = Uri.fromFile(recordingFile)
                checkFileHasVideo(uri)
                assertThat(finalize!!.outputResults.outputUri).isEqualTo(uri)
            } finally {
                cleanup()
            }
        }

        fun startRecordingVideos() {
            skipVideoRecordingTestIfNotSupportedByEmulator()
            recordingFile = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
            activeRecording =
                videoCapture!!
                    .output
                    .prepareRecording(
                        context,
                        FileOutputOptions.Builder(recordingFile!!)
                            .setDurationLimitMillis(VIDEO_DURATION_LIMIT_MS)
                            .build(),
                    )
                    .start(
                        CameraXExecutors.directExecutor(),
                        {
                            when (it) {
                                is VideoRecordEvent.Start -> {}
                                is VideoRecordEvent.Finalize -> {
                                    finalize = it
                                    semaphoresForVideoSaved.release()
                                }

                                is VideoRecordEvent.Status,
                                is VideoRecordEvent.Pause,
                                is VideoRecordEvent.Resume -> {
                                    // Do nothing.
                                }
                                else -> {
                                    throw IllegalStateException()
                                }
                            }
                        },
                    )
        }

        private fun checkFileHasVideo(uri: Uri) {
            val mediaRetriever = MediaMetadataRetriever()
            try {
                mediaRetriever.setDataSource(context, uri)
                val hasVideo =
                    mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                assertThat(hasVideo).isEqualTo("yes")
            } finally {
                mediaRetriever.release()
            }
        }
    }
}
