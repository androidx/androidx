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

package androidx.camera.video

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraTaskTrackingExecutor
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.RecordingSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runners.Parameterized

abstract class PersistentRecordingTestBase(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig
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

    @get:Rule val labTestRule = LabTestRule()

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
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
    }

    protected abstract val testTag: String
    protected abstract val enableStreamSharing: Boolean

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProviderWrapper
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var camera: Camera
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recordingSession: RecordingSession
    private lateinit var cameraExecutor: CameraTaskTrackingExecutor

    private val oppositeCameraSelector: CameraSelector by lazy {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private val oppositeCamera: Camera by lazy {
        lateinit var camera: Camera
        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, oppositeCameraSelector)
        }
        camera
    }

    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(videoCapabilities, Recorder.DEFAULT_QUALITY_SELECTOR)
    }

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        skipVideoRecordingTestIfNotSupportedByEmulator()

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        cameraExecutor = CameraTaskTrackingExecutor()
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(cameraConfig).setCameraExecutor(cameraExecutor).build()

        ProcessCameraProvider.configureInstance(cameraXConfig)

        cameraProvider =
            ProcessCameraProviderWrapper(
                ProcessCameraProvider.getInstance(context).get(),
                enableStreamSharing
            )
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()
        videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
            videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        }

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

    @After
    fun tearDown() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(timeoutMs = 5000)
        }
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    // TODO(b/340406044): Remove LabTestOnly when issue is resolved.
    @LabTestRule.LabTestOnly
    @Test
    fun persistentRecording_canContinueRecordingAfterRebind() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        val recording =
            recordingSession.createRecording(asPersistentRecording = true).startAndVerify()

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        checkAndBindUseCases(preview, videoCapture)

        recording.clearEvents()
        recording.verifyStatus()

        recording.stopAndVerify()
    }

    // TODO(b/340406044): Remove LabTestOnly when issue is resolved.
    @LabTestRule.LabTestOnly
    @Test
    fun persistentRecording_canContinueRecordingPausedAfterRebind() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        val recording =
            recordingSession
                .createRecording(asPersistentRecording = true)
                .startAndVerify()
                .pauseAndVerify()

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        checkAndBindUseCases(preview, videoCapture)

        recording.resumeAndVerify().stopAndVerify()
    }

    // TODO(b/353113961): Remove LabTestOnly when issue is resolved.
    @LabTestRule.LabTestOnly
    @Test
    fun persistentRecording_canStopAfterUnbind() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        val recording =
            recordingSession.createRecording(asPersistentRecording = true).startAndVerify()

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }

        recording.stopAndVerify()
    }

    // TODO(b/340406044): Remove LabTestOnly when issue is resolved.
    @LabTestRule.LabTestOnly
    @Test
    fun updateVideoUsage_whenUseCaseUnboundAndReboundForPersistentRecording(): Unit = runBlocking {
        checkAndBindUseCases(preview, videoCapture)
        val recording =
            recordingSession.createRecording(asPersistentRecording = true).startAndVerify()

        // Act 1 - unbind VideoCapture before recording completes, isRecording should be false.
        instrumentation.runOnMainSync { cameraProvider.unbind(videoCapture) }

        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "VideoCapture unbound but camera still in video usage"
        )

        // Act 2 - rebind VideoCapture, isRecording should be true.
        checkAndBindUseCases(videoCapture)

        camera.cameraControl.verifyIfInVideoUsage(
            true,
            "VideoCapture re-bound but camera still not in video usage"
        )

        // TODO(b/382158668): Remove the check for the status events.
        recording.clearEvents()
        recording.verifyStatus()
        recording.stopAndVerify()
    }

    // TODO(b/340406044): Remove LabTestOnly when issue is resolved.
    @LabTestRule.LabTestOnly
    @Test
    fun updateVideoUsage_whenUseCaseBoundToNewCameraForPersistentRecording(): Unit = runBlocking {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        val recording =
            recordingSession.createRecording(asPersistentRecording = true).startAndVerify()

        // Act 1 - unbind before recording completes, isRecording should be false.
        instrumentation.runOnMainSync { cameraProvider.unbindAll() }

        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "VideoCapture unbound but camera still in video usage"
        )

        // Act 2 - rebind VideoCapture to opposite camera, isRecording should be true.
        checkAndBindUseCases(preview, videoCapture, useOppositeCamera = true)

        oppositeCamera.cameraControl.verifyIfInVideoUsage(
            true,
            "VideoCapture re-bound but camera still not in video usage"
        )

        // TODO(b/382158668): Remove the check for the status events.
        recording.clearEvents()
        recording.verifyStatus()
        recording.stopAndVerify()
    }

    private fun getCameraSelector(useOppositeCamera: Boolean): CameraSelector =
        if (!useOppositeCamera) cameraSelector else oppositeCameraSelector

    private fun getCamera(useOppositeCamera: Boolean): Camera =
        if (!useOppositeCamera) camera else oppositeCamera

    private fun isUseCasesCombinationSupported(
        vararg useCases: UseCase,
        withStreamSharing: Boolean,
        useOppositeCamera: Boolean = false,
    ) = getCamera(useOppositeCamera).isUseCasesCombinationSupported(withStreamSharing, *useCases)

    /** Checks use case combination with considering StreamSharing and then binds to lifecycle. */
    private fun checkAndBindUseCases(
        vararg useCases: UseCase,
        withStreamSharing: Boolean = enableStreamSharing,
        useOppositeCamera: Boolean = false,
    ) {
        assumeTrue(
            isUseCasesCombinationSupported(
                *useCases,
                withStreamSharing = withStreamSharing,
                useOppositeCamera = useOppositeCamera
            )
        )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                getCameraSelector(useOppositeCamera),
                *useCases
            )
        }
    }

    private suspend fun CameraControl.verifyIfInVideoUsage(
        expected: Boolean,
        message: String = ""
    ) {
        instrumentation.waitForIdleSync() // VideoCapture observes Recorder in main thread
        // VideoUsage is updated in camera thread. So, we should ensure all tasks already submitted
        // to camera thread are completed before checking isInVideoUsage
        cameraExecutor.awaitIdle()
        assertWithMessage(message).that((this as CameraControlInternal).isInVideoUsage).apply {
            if (expected) {
                isTrue()
            } else {
                isFalse()
            }
        }
    }
}
