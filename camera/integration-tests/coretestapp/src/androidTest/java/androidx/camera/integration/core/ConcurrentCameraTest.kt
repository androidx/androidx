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
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
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
import androidx.media3.effect.RgbFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ConcurrentCameraTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {
        const val PREVIEW_FRAMES = 5

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

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
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[20, TimeUnit.SECONDS]
        }
    }

    @Test
    fun testConcurrentCameraV1_preview_canWork() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)

        val secondary = SingleCameraParameter(cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)

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
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val lifecycleOwner = FakeLifecycleOwner()
        val primary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            )

        val secondary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            )

        // Act.
        val concurrentCamera =
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
        concurrentCamera.assertBothCamerasClosed()

        lifecycleOwner.startAndResume()
        primary.reset()
        secondary.reset()

        // Assert.
        primary.assertPreviewFramesReceived()
        secondary.assertPreviewFramesReceived()
    }

    suspend fun ConcurrentCamera.assertBothCamerasClosed() {
        cameras.forEach { camera ->
            val cameraCloseLatch = CountDownLatch(1)
            withContext(Dispatchers.Main) {
                camera.cameraInfo.cameraState.observeForever {
                    if (it.type == CameraState.Type.CLOSED) {
                        cameraCloseLatch.countDown()
                    }
                }
            }
            assertThat(cameraCloseLatch.await(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun testConcurrentCameraV1_previewImageCapture_canWork() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                enableImageCapture = true,
            )
        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                enableImageCapture = true,
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
    }

    @Test
    fun testConcurrentCameraV1_previewVideoCapture_canWork() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                enableVideoCapture = true,
            )
        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
        primary.startRecordingVideos()
        secondary.startRecordingVideos()
        primary.assertVideoRecorded()
        secondary.assertVideoRecorded()
    }

    @Test
    fun testConcurrentCameraV1_previewVideoCapture_withOneForEachTargetEffect_canWork() =
        runBlocking {
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                    enableVideoCapture = true,
                    effect = createOneForEachTargetEffect(),
                )
            val secondary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                    enableVideoCapture = true,
                    effect = createOneForAllTargetsEffect(),
                )
            val secondary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                enableImageCapture = true,
                enableVideoCapture = true,
            )
        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val primary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
            )

        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
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
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val lifecycleOwner = FakeLifecycleOwner()
        val primary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                enableVideoCapture = true,
                compositionSettings =
                    CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
            )

        val secondary =
            SingleCameraParameter(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
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

        // lifecycle starts after binding
        lifecycleOwner.startAndResume()
        primary.assertPreviewFramesReceived()

        lifecycleOwner.pauseAndStop()
        concurrentCamera.assertBothCamerasClosed()

        lifecycleOwner.startAndResume()
        primary.reset()

        // Assert.
        primary.assertPreviewFramesReceived()
        primary.startRecordingVideos()
        primary.assertVideoRecorded()
    }

    @Test
    fun testConcurrentCameraV2_previewVideoCapture_withOneForEachTargetEffect_canWork() =
        runBlocking {
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
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
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

            // Arrange.
            val primary =
                SingleCameraParameter(
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
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
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
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
                    CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
            )

        val secondary =
            SingleCameraParameter(
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup = primary.useCaseGroup,
                compositionSettings =
                    CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
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
                    CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
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
        var previewLatch = CountDownLatch(PREVIEW_FRAMES)
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
            assertThat(previewLatch.await(5, TimeUnit.SECONDS)).isTrue()
        }

        fun reset() {
            previewLatch = CountDownLatch(PREVIEW_FRAMES)
        }

        fun assertCanCaptureImages() {
            val imageSemaphore = Semaphore(0)
            var capturedImage: ImageProxy? = null
            imageCapture!!.takePicture(
                CameraXExecutors.ioExecutor(),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        capturedImage = image
                        imageSemaphore.release()
                    }
                },
            )

            assertThat(imageSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
            assertThat(capturedImage!!.toBitmap()).isNotNull()
            capturedImage.close()
        }

        var recordingFile: File? = null
        var activeRecording: Recording? = null
        var finalize: VideoRecordEvent.Finalize? = null
        val semaphoresForVideoSaved = Semaphore(0)

        fun assertVideoRecorded() {
            assertThat(recordingFile).isNotNull()
            assertThat(activeRecording).isNotNull()

            // Wait for finalize event to saved file.
            assertThat(semaphoresForVideoSaved.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
            // Verify.
            val uri = Uri.fromFile(recordingFile)
            checkFileHasVideo(uri)
            assertThat(finalize!!.outputResults.outputUri).isEqualTo(uri)

            // Cleanup.
            activeRecording?.close()
            recordingFile!!.delete()
        }

        fun startRecordingVideos() {
            recordingFile = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
            activeRecording =
                videoCapture!!
                    .output
                    .prepareRecording(
                        context,
                        FileOutputOptions.Builder(recordingFile!!)
                            .setDurationLimitMillis(1_500)
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
            mediaRetriever.apply {
                setDataSource(context, uri)
                val hasVideo = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                assertThat(hasVideo).isEqualTo("yes")
            }
        }
    }
}
