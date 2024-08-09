/*
 * Copyright 2021 The Android Open Source Project
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

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider.createSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Contains tests for [CameraX] which varies use case combinations to run. */
@LargeTest
@RunWith(Parameterized::class)
class UseCaseCombinationTest(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName.contains(CameraPipeConfig::class.simpleName!!),
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(
                    "back+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var previewMonitor: PreviewMonitor
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysisMonitor: AnalysisMonitor
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recordingSession: RecordingSession
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities

    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(videoCapabilities, Recorder.DEFAULT_QUALITY_SELECTOR)
    }

    @Before
    fun initializeCameraX() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()

            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }

        previewMonitor = PreviewMonitor()
        preview = initPreview(previewMonitor)
        imageCapture = initImageCapture()
        imageAnalysisMonitor = AnalysisMonitor()
        imageAnalysis = initImageAnalysis(imageAnalysisMonitor)
    }

    @After
    fun shutdownCameraX() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(timeoutMs = 5000)
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    /** Test Combination: Preview + ImageCapture */
    @Test
    fun previewCombinesImageCapture() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
    }

    /** Test Combination: Preview (no surface provider) + ImageCapture */
    @Test
    fun previewCombinesImageCapture_withNoSurfaceProvider() {
        // Arrange.
        preview = initPreview(previewMonitor, /* setSurfaceProvider= */ false)
        checkAndBindUseCases(preview, imageCapture)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.waitForStreamIdle()
    }

    /** Test Combination: Preview + ImageAnalysis */
    @Test
    fun previewCombinesImageAnalysis() {
        // Arrange.
        checkAndBindUseCases(preview, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    /** Test Combination: Preview (no surface provider) + ImageAnalysis */
    @Test
    fun previewCombinesImageAnalysis_withNoSurfaceProvider() {
        // Arrange.
        preview = initPreview(previewMonitor, /* setSurfaceProvider= */ false)
        checkAndBindUseCases(preview, imageAnalysis)

        // Assert.
        previewMonitor.waitForStreamIdle()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    /** Test Combination: Preview + ImageAnalysis + ImageCapture */
    @Test
    fun previewCombinesImageAnalysisAndImageCapture() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun sequentialBindPreviewImageCaptureAndImageAnalysis() {
        // Arrange.
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))

        // Bind Preview and verify
        bindUseCases(preview)
        previewMonitor.waitForStream()

        // Bind additional ImageCapture and verify
        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Bind additional ImageAnalysis and verify
        bindUseCases(preview, imageCapture, imageAnalysis)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun unbindImageAnalysis_captureAndPreviewStillWorking() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(imageAnalysis)

        // Assert
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
    }

    @Test
    fun unbindPreview_captureAndAnalysisStillWorking(): Unit = runBlocking {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(preview)
        delay(1000) // Unbind and stop the output stream should be done within 1 sec.
        previewMonitor.waitForStreamIdle(count = 1, timeMillis = TimeUnit.SECONDS.toMillis(2))

        // Assert
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun unbindImageCapture_previewAndAnalysisStillWorking() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(imageCapture)

        // Assert
        imageAnalysisMonitor.waitForImageAnalysis()
        previewMonitor.waitForStream()
    }

    @Test
    fun previewCombinesVideoCapture() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun previewCombinesVideoCaptureAndImageCapture() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
    }

    @Test
    fun previewCombinesVideoCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun previewCombinesVideoCaptureImageCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun sequentialBindPreviewVideoCaptureImageCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(
            camera.isUseCasesCombinationSupported(
                preview,
                imageCapture,
                imageAnalysis,
                videoCapture
            )
        )

        // Bind Preview and verify
        bindUseCases(preview)
        previewMonitor.waitForStream()

        // Bind additional VideoCapture and Verify
        bindUseCases(preview, videoCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()

        // Bind additional VideoCapture and Verify
        bindUseCases(preview, videoCapture, imageCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()

        // Bind additional ImageAnalysis and Verify
        bindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    // Preview + ImageCapture -> Preview + VideoCapture -> Preview + ImageCapture
    @Test
    fun switchImageCaptureVideoCaptureWithTwoUseCasesBound() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))

        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Unbind ImageCapture and switches to VideoCapture
        unbindUseCases(imageCapture)
        bindUseCases(preview, videoCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()

        // Unbind VideoCapture and switches back to ImageCapture
        unbindUseCases(videoCapture)
        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
    }

    // Preview + ImageCapture -> Preview + ImageCapture + VideoCapture -> Preview + ImageCapture
    @Test
    fun addVideoCaptureToPreviewAndImageCapture_thenRemove() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))

        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Bind additional VideoCapture and verify
        bindUseCases(preview, imageCapture, videoCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        recordingSession.createRecording().recordAndVerify()

        // Unbind VideoCapture and verify
        unbindUseCases(videoCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
    }

    // Possible for QR code scanning use case.
    @Test
    fun sequentialBindPreviewAndImageAnalysis() {
        bindUseCases(preview)
        previewMonitor.waitForStream()

        bindUseCases(preview, imageAnalysis)
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    private fun initPreview(monitor: PreviewMonitor?, setSurfaceProvider: Boolean = true): Preview {
        return Preview.Builder()
            .setTargetName("Preview")
            .also {
                monitor?.let { monitor ->
                    CameraPipeUtil.setCameraCaptureSessionCallback(implName, it, monitor)
                }
            }
            .build()
            .apply {
                if (setSurfaceProvider) {
                    instrumentation.runOnMainSync {
                        surfaceProvider = createSurfaceTextureProvider()
                    }
                }
            }
    }

    private fun initImageAnalysis(analyzer: ImageAnalysis.Analyzer?): ImageAnalysis {
        return ImageAnalysis.Builder().setTargetName("ImageAnalysis").build().apply {
            analyzer?.let { analyzer -> setAnalyzer(Dispatchers.IO.asExecutor(), analyzer) }
        }
    }

    private fun initImageCapture(): ImageCapture {
        return ImageCapture.Builder().build()
    }

    private fun ImageCapture.waitForCapturing(timeMillis: Long = 5000) {
        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                val latch = CountDownLatch(1)
                val errors = mutableListOf<ImageCaptureException>()

                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    latch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    errors.add(exception)
                    latch.countDown()
                }
            }

        takePicture(Dispatchers.Main.asExecutor(), callback)

        assertThat(
                callback.latch.await(timeMillis, TimeUnit.MILLISECONDS) && callback.errors.isEmpty()
            )
            .isTrue()
    }

    class PreviewMonitor : CameraCaptureSession.CaptureCallback() {
        private var countDown: CountDownLatch? = null

        fun waitForStream(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(5)) {
            Truth.assertWithMessage("Preview doesn't start")
                .that(
                    synchronized(this) {
                            countDown = CountDownLatch(count)
                            countDown
                        }!!
                        .await(timeMillis, TimeUnit.MILLISECONDS)
                )
                .isTrue()
        }

        fun waitForStreamIdle(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(5)) {
            Truth.assertWithMessage("Preview doesn't become idle")
                .that(
                    synchronized(this) {
                            countDown = CountDownLatch(count)
                            countDown
                        }!!
                        .await(timeMillis, TimeUnit.MILLISECONDS)
                )
                .isFalse()
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            synchronized(this) { countDown?.countDown() }
        }
    }

    class AnalysisMonitor : ImageAnalysis.Analyzer {
        private var countDown: CountDownLatch? = null

        fun waitForImageAnalysis(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(5)) {
            Truth.assertWithMessage("Preview doesn't start")
                .that(
                    synchronized(this) {
                            countDown = CountDownLatch(count)
                            countDown
                        }!!
                        .await(timeMillis, TimeUnit.MILLISECONDS)
                )
                .isTrue()
        }

        override fun analyze(image: ImageProxy) {
            image.close()
            synchronized(this) { countDown?.countDown() }
        }
    }

    private fun checkAndBindUseCases(vararg useCases: UseCase) {
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases))
        bindUseCases(*useCases)
    }

    private fun bindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, *useCases)
        }
    }

    private fun unbindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync { cameraProvider.unbind(*useCases) }
    }

    private fun checkAndPrepareVideoCaptureSources() {
        skipVideoRecordingTestIfNotSupportedByEmulator()
        videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    recorder = videoCapture.output,
                    outputOptionsProvider = {
                        FileOutputOptions.Builder(temporaryFolder.newFile()).build()
                    },
                    withAudio = audioStreamAvailable,
                )
            )
    }
}
