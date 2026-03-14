/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.view

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionSessionConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.ParameterizedTestConfigUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.video.AudioConfig
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Instrumentation tests for [CameraController]. */
@LargeTest
@RunWith(Parameterized::class)
class CameraControllerDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    companion object {
        const val TIMEOUT_SECONDS = 10L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            ParameterizedTestConfigUtil.generateCameraXConfigParameterizedTestConfigs(
                inLabTestRequired = true
            )
    }

    @get:Rule
    val requireForegroundRule = RequireForegroundRule { assumeTrue(CameraUtil.deviceHasCamera()) }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    private var controller: LifecycleCameraController? = null
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<FakeActivity>? = null
    private lateinit var context: Context
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var defaultCameraSelector: CameraSelector

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ProcessCameraProvider.configureInstance(cameraConfig)
        defaultCameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        activityScenario = ActivityScenario.launch(FakeActivity::class.java)
        controller = LifecycleCameraController(context)
        instrumentation.runOnMainSync { controller!!.cameraSelector = defaultCameraSelector }
        controller!!.initializationFuture.get()
        requireForegroundRule.deferCleanup {
            controller?.shutDownForTests()
            cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
            cameraProvider = null
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun setInvalidEffectsCombination_throwsException() {
        // Arrange: setup PreviewView and CameraController
        var previewView: PreviewView? = null
        activityScenario!!.onActivity {
            // Arrange.
            previewView = PreviewView(context)
            it.setContentView(previewView)
            previewView.controller = controller
            controller!!.bindToLifecycle(FakeLifecycleOwner())
            controller!!.initializationFuture.get()
        }
        val layoutLatch = CountDownLatch(1)
        waitUntilPreviewViewIsReady(previewView!!, layoutLatch)
        assertThat(layoutLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

        // Act: set the same effect twice, which is invalid.
        val previewEffect1 =
            FakeSurfaceEffect(mainThreadExecutor(), FakeSurfaceProcessor(mainThreadExecutor()))
        val previewEffect2 =
            FakeSurfaceEffect(mainThreadExecutor(), FakeSurfaceProcessor(mainThreadExecutor()))
        instrumentation.runOnMainSync {
            controller!!.setEffects(setOf(previewEffect1, previewEffect2))
        }
    }

    @Test
    fun setEffect_effectSetOnUseCase() {
        // Arrange: setup PreviewView and CameraController
        var previewView: PreviewView? = null
        activityScenario!!.onActivity {
            // Arrange.
            previewView = PreviewView(context)
            it.setContentView(previewView)
            previewView.controller = controller
            controller!!.bindToLifecycle(FakeLifecycleOwner())
            controller!!.initializationFuture.get()
        }
        val layoutLatch = CountDownLatch(1)
        waitUntilPreviewViewIsReady(previewView!!, layoutLatch)
        assertThat(layoutLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

        // Act: set an effect
        val effect =
            FakeSurfaceEffect(mainThreadExecutor(), FakeSurfaceProcessor(mainThreadExecutor()))
        instrumentation.runOnMainSync { controller!!.setEffects(setOf(effect)) }

        // Assert: preview has effect
        assertThat(controller!!.mPreview.effect).isNotNull()

        // Act: clear the effects
        instrumentation.runOnMainSync { controller!!.clearEffects() }

        // Assert: preview no longer has the effect.
        assertThat(controller!!.mPreview.effect).isNull()
    }

    @Test
    fun setSelectorAfterBound_selectorSet() {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)
        val cameraSelector0 = cameraSelectors[0]
        val cameraSelector1 = cameraSelectors[1]

        // Act
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = cameraSelector0

            assertThat(controller!!.cameraSelector.lensFacing).isEqualTo(cameraSelector0.lensFacing)
            controller!!.cameraSelector = cameraSelector1

            // Assert.
            assertThat(controller!!.cameraSelector.lensFacing).isEqualTo(cameraSelector1.lensFacing)
        }
    }

    @Test
    fun previewViewNotAttached_useCaseGroupIsNotBuilt() {
        assertThat(controller!!.createUseCaseGroup(/* checkPreviewViewAttached= */ true)).isNull()
    }

    @Test
    fun frontCameraFlipNotSet_imageIsMirrored() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))

        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val options = getOutputFileOptionsBuilder().build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isTrue()
    }

    @Test
    fun frontCameraFlipSetToFalse_imageIsNotMirrored() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))

        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = false
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isFalse()
    }

    @Test
    fun frontCameraFlipSetToTrue_imageIsMirrored() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))

        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = true
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isTrue()
    }

    private fun getOutputFileOptionsBuilder(): ImageCapture.OutputFileOptions.Builder {
        return ImageCapture.OutputFileOptions.Builder(
            instrumentation.context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues(),
        )
    }

    @Test
    fun analysisIsEnabledByDefault() {
        instrumentation.runOnMainSync { assertThat(controller!!.isImageAnalysisEnabled).isTrue() }
    }

    @Test
    fun captureIsEnabledByDefault() {
        instrumentation.runOnMainSync { assertThat(controller!!.isImageCaptureEnabled).isTrue() }
    }

    @Test
    fun disableAnalysisCaptureEnableVideo() {
        instrumentation.runOnMainSync {
            controller!!.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            assertThat(controller!!.isImageCaptureEnabled).isFalse()
            assertThat(controller!!.isImageAnalysisEnabled).isFalse()
            assertThat(controller!!.isVideoCaptureEnabled).isTrue()
        }
    }

    @Test
    fun clearPreviewSurface_wontUnbindOthersUseCases() {
        // Arrange.
        val cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]

        val imageCapture = ImageCapture.Builder().build()
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                FakeLifecycleOwner(),
                defaultCameraSelector,
                imageCapture,
            )
        }

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller!!.initializationFuture[10000, TimeUnit.MILLISECONDS]

        // Act.
        instrumentation.runOnMainSync { controller!!.clearPreviewSurface() }

        // Assert.
        assertThat(cameraProvider.isBound(imageCapture)).isTrue()
    }

    @Test
    fun setCameraSelector_wontUnbindOthersUseCases() {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        if (cameraSelectors.isNotEmpty()) {
            val cameraSelector0 = cameraSelectors[0]
            testCameraSelectorWontUnbindUseCases(cameraSelector0, cameraSelector0)
        }
        if (cameraSelectors.size > 1) {
            val cameraSelector0 = cameraSelectors[0]
            val cameraSelector1 = cameraSelectors[1]
            testCameraSelectorWontUnbindUseCases(cameraSelector1, cameraSelector1)
            testCameraSelectorWontUnbindUseCases(cameraSelector0, cameraSelector1)
            testCameraSelectorWontUnbindUseCases(cameraSelector1, cameraSelector0)
        }
    }

    private fun testCameraSelectorWontUnbindUseCases(
        firstCamera: CameraSelector,
        secondCamera: CameraSelector,
    ) {
        // Arrange.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(firstCamera.lensFacing!!))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(secondCamera.lensFacing!!))
        val cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]

        val imageCapture = ImageCapture.Builder().build()
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), firstCamera, imageCapture)
        }

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller!!.initializationFuture[10000, TimeUnit.MILLISECONDS]

        // Act.
        instrumentation.runOnMainSync { controller!!.cameraSelector = secondCamera }

        // Assert.
        assertThat(cameraProvider.isBound(imageCapture)).isTrue()
    }

    @Test
    fun lifecycleCameraController_canEnterStreamingStateAndTakePicture_withSetSessionConfig() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val sessionConfig = SessionConfig(preview, imageCapture)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: streaming and can take picture
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanTakePicture(controller, imageCapture)
    }

    @Test
    fun lifecycleCameraController_canSwitchSessionConfig_andAllUseCasesWork() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val sessionConfig1 = SessionConfig(preview, imageCapture)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act: set SessionConfig with Preview and ImageCapture
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller.setSessionConfig(sessionConfig1, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: streaming and can take picture
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanTakePicture(controller, imageCapture)

        // Act: set a new SessionConfig with VideoCapture
        val streamingStateLatch2 = getPreviewStreamingStateLatch(previewView)
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val sessionConfig2 = SessionConfig(preview, imageCapture, videoCapture)
        instrumentation.runOnMainSync {
            controller.setSessionConfig(sessionConfig2, defaultCameraSelector)
        }

        // Assert: streaming and can take picture and record video
        assertThat(streamingStateLatch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanTakePicture(controller, imageCapture)
        assertCanRecordVideo(controller, videoCapture)
    }

    @Test
    fun lifecycleCameraController_canEnterStreamingStateAndRecordVideo_withSetSessionConfig() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val sessionConfig = SessionConfig(preview, videoCapture)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: streaming and can record video
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanRecordVideo(controller, videoCapture)
    }

    @Test
    fun lifecycleCameraController_canEnterStreamingStateAndAnalyze_withSetSessionConfig() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val sessionConfig = SessionConfig(preview, imageAnalysis)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: streaming and can analyze received image
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanAnalyzeImage(imageAnalysis)
    }

    @Test
    fun lifecycleCameraController_canEnterStreamingStateAndTakePicture_withExtensionSessionConfig():
        Unit = runBlocking {
        assumeTrue(!Build.MODEL.contains("Cuttlefish", true))
        ExtensionsUtil.assumePcsSupportedForImageCapture(context)
        // 1. Get ExtensionsManager instance
        val extensionsManager = ExtensionsManager.getInstance(context, cameraProvider!!)

        // 2. Checks whether NIGHT extension mode is available
        assumeTrue(
            extensionsManager.isExtensionAvailable(defaultCameraSelector, ExtensionMode.NIGHT)
        )

        // 3. Create an ExtensionSessionConfig with Preview and ImageCapture
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val extensionSessionConfig =
            ExtensionSessionConfig(ExtensionMode.NIGHT, extensionsManager, preview, imageCapture)

        // 4. Bind the ExtensionSessionConfig and verify preview is streaming
        val fakeLifecycleOwner = FakeLifecycleOwner()
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller!!.setSessionConfig(extensionSessionConfig, defaultCameraSelector)
            controller!!.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: streaming and can take picture
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertCanTakePicture(controller!!, imageCapture)
    }

    @Test
    fun setCameraSelector_canSwitchesCamera_withSessionConfig() {
        // Arrange
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_BACK))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))

        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val sessionConfig = SessionConfig(preview)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act: set initial camera to back
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            previewView.get().controller = controller
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: preview is streaming with back camera
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            assertThat(controller.cameraSelector.lensFacing).isEqualTo(LENS_FACING_BACK)
        }

        // Act: switch to front camera
        val streamingStateLatch2 = getPreviewStreamingStateLatch(previewView)
        instrumentation.runOnMainSync {
            controller.setSessionConfig(sessionConfig, CameraSelector.DEFAULT_FRONT_CAMERA)
        }

        // Assert: preview is streaming with front camera
        assertThat(streamingStateLatch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            assertThat(controller.cameraSelector.lensFacing).isEqualTo(LENS_FACING_FRONT)
        }
    }

    @Test
    fun previewViewCanEnterStreamingState_whenSetPreviewViewControllerAfterSessionConfigIsSet() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        val preview = Preview.Builder().build()
        val sessionConfig = SessionConfig(preview)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            // Set SessionConfig on controller, but don't set controller on PreviewView yet
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: preview is not streaming yet.
        // Use a short timeout because we expect it to fail.
        assertThat(streamingStateLatch.await(2, TimeUnit.SECONDS)).isFalse()
        instrumentation.runOnMainSync {
            assertThat(previewView.get().previewStreamState.value)
                .isNotEqualTo(PreviewView.StreamState.STREAMING)
        }

        // Act: set the controller on the PreviewView
        val streamingStateLatch2 = getPreviewStreamingStateLatch(previewView)
        instrumentation.runOnMainSync { previewView.get().controller = controller }

        // Assert: preview is streaming
        assertThat(streamingStateLatch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun previewViewCanEnterStreamingState_whenSetPreviewViewControllerBeforeSessionConfigIsSet() {
        // Arrange
        val controller = LifecycleCameraController(context)
        val previewView = createPreview()
        val streamingStateLatch = getPreviewStreamingStateLatch(previewView)
        // Create a Preview without setting a SurfaceProvider.
        val preview = Preview.Builder().build()
        val sessionConfig = SessionConfig(preview)
        val fakeLifecycleOwner = FakeLifecycleOwner()

        // Act
        instrumentation.runOnMainSync {
            fakeLifecycleOwner.startAndResume()
            // Set controller on PreviewView first.
            previewView.get().controller = controller
            // Then set the SessionConfig. The controller should automatically connect the
            // Preview to the PreviewView's SurfaceProvider.
            controller.setSessionConfig(sessionConfig, defaultCameraSelector)
            controller.bindToLifecycle(fakeLifecycleOwner)
        }

        // Assert: preview is streaming.
        assertThat(streamingStateLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun createPreview(): AtomicReference<PreviewView> {
        val previewView = AtomicReference<PreviewView>()
        val layoutLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val view = PreviewView(context)
            previewView.set(view)
            waitUntilPreviewViewIsReady(view, layoutLatch)
            setContentView(view)
        }
        assertThat(layoutLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        return previewView
    }

    private fun getPreviewStreamingStateLatch(
        previewView: AtomicReference<PreviewView>
    ): CountDownLatch {
        val streamingLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            previewView.get().previewStreamState.observeForever { streamState ->
                if (streamState == PreviewView.StreamState.STREAMING) {
                    streamingLatch.countDown()
                }
            }
        }
        return streamingLatch
    }

    private fun waitUntilPreviewViewIsReady(
        previewView: PreviewView,
        countDownLatch: CountDownLatch,
    ) {
        previewView.addOnLayoutChangeListener(
            object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    if (v.width > 0 && v.height > 0) {
                        countDownLatch.countDown()
                        previewView.removeOnLayoutChangeListener(this)
                    }
                }
            }
        )
    }

    private fun setContentView(view: View?) {
        activityScenario!!.onActivity { activity: FakeActivity -> activity.setContentView(view) }
    }

    private fun assertCanTakePicture(
        controller: LifecycleCameraController,
        imageCapture: ImageCapture,
    ) {
        assertThat(controller.mImageCapture).isSameInstanceAs(imageCapture)

        // Act: take picture via controller and via ImageCapture directly.
        val captureSuccessLatch = CountDownLatch(1)
        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    captureSuccessLatch.countDown()
                }
            }

        instrumentation.runOnMainSync { controller.takePicture(mainThreadExecutor(), callback) }

        // Assert: camera controller capture succeed.
        assertThat(captureSuccessLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

        val captureSuccessLatch2 = CountDownLatch(1)
        val callback2 =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    captureSuccessLatch2.countDown()
                }
            }
        imageCapture.takePicture(mainThreadExecutor(), callback2)

        // Assert: ImageCapture capture succeed.
        assertThat(captureSuccessLatch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun assertCanRecordVideo(
        controller: LifecycleCameraController,
        videoCapture: VideoCapture<Recorder>,
    ) {
        assertThat(controller.mVideoCapture).isSameInstanceAs(videoCapture)

        // Act: record video via controller
        val recordingFinalizedLatch = CountDownLatch(1)
        val file = File.createTempFile("video", ".mp4")
        file.deleteOnExit()
        val outputFile = FileOutputOptions.Builder(file).build()

        var recording: Recording? = null
        instrumentation.runOnMainSync {
            recording =
                controller.startRecording(
                    outputFile,
                    AudioConfig.AUDIO_DISABLED,
                    mainThreadExecutor(),
                ) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> recordingFinalizedLatch.countDown()
                    }
                }
        }

        // Recording for 3 seconds
        runBlocking { delay(3000) }

        // Assert: recording started
        instrumentation.runOnMainSync { recording!!.stop() }
        assertThat(recordingFinalizedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

        // Act: record video via VideoCapture directly
        val recordingFinalizedLatch2 = CountDownLatch(1)
        val file2 = File.createTempFile("video2", ".mp4")
        file2.deleteOnExit()
        val outputFile2 = FileOutputOptions.Builder(file2).build()
        var recording2: Recording? = null
        instrumentation.runOnMainSync {
            recording2 =
                videoCapture.output.prepareRecording(context, outputFile2).start(
                    mainThreadExecutor()
                ) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> recordingFinalizedLatch2.countDown()
                    }
                }
        }

        // Recording for 3 seconds
        runBlocking { delay(3000) }

        // Assert: recording started
        instrumentation.runOnMainSync { recording2!!.stop() }
        assertThat(recordingFinalizedLatch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun assertCanAnalyzeImage(controller: CameraController) {
        val analysisLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            controller.setImageAnalysisAnalyzer(mainThreadExecutor()) { imageProxy ->
                imageProxy.close()
                analysisLatch.countDown()
            }
        }
        assertThat(analysisLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun assertCanAnalyzeImage(imageAnalysis: ImageAnalysis) {
        val analysisLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            imageAnalysis.setAnalyzer(mainThreadExecutor()) { imageProxy ->
                imageProxy.close()
                analysisLatch.countDown()
            }
        }
        assertThat(analysisLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }
}
