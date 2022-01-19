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
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

// Frame drops sometimes happen for reasons beyond our control. This test will always be flaky.
// The main purpose is to catch cases where frame drops happen consistently.
@FlakyTest
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 23) // Requires CaptureCallback.onCaptureBufferLost
class VideoRecordingFrameDropTest(
    private val cameraName: String,
    private val cameraSelector: CameraSelector,
    private val perSelectorTestData: PerSelectorTestData
) {
    @get:Rule
    var cameraRule: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    // Due to the flaky nature of this test, it should only be run in the lab
    @get:Rule
    val labTestRule = LabTestRule()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    data class PerSelectorTestData(
        var hasResult: Boolean = false,
        var routineError: Exception? = null,
        var numDroppedFrames: Int = 0
    )

    companion object {
        private const val TAG = "RecordingFrameDropTest"
        lateinit var cameraProvider: ProcessCameraProvider
        var needsShutdown = false

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Back", CameraSelector.DEFAULT_BACK_CAMERA, PerSelectorTestData()),
                arrayOf("Front", CameraSelector.DEFAULT_FRONT_CAMERA, PerSelectorTestData()),
            )
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        if (!perSelectorTestData.hasResult) {
            perSelectorTestData.hasResult = true
            try {
                perSelectorTestData.numDroppedFrames =
                    runRecordingRoutineAndReturnNumDroppedFrames()
            } catch (ex: Exception) {
                perSelectorTestData.routineError = ex
            }
        }

        // Ensure all tests fail if the routine failed to complete
        if (perSelectorTestData.routineError != null) {
            throw perSelectorTestData.routineError!!
        }
    }

    @After
    fun tearDown() = runBlocking {
        if (needsShutdown) {
            needsShutdown = false
            cameraProvider.shutdown().await()
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedNoFrames() {
        assertThat(perSelectorTestData.numDroppedFrames).isEqualTo(0)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedLessThanFiveFrames() {
        assertThat(perSelectorTestData.numDroppedFrames).isLessThan(5)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedLessThanTenFrames() {
        assertThat(perSelectorTestData.numDroppedFrames).isLessThan(10)
    }

    private suspend fun runRecordingRoutineAndReturnNumDroppedFrames(): Int = coroutineScope {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        needsShutdown = true
        val lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        val droppedFrameFlow = MutableSharedFlow<Long>(replay = Channel.UNLIMITED)
        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureBufferLost(
                session: CameraCaptureSession,
                request: CaptureRequest,
                target: Surface,
                frameNumber: Long
            ) {
                Logger.e(TAG, "Frame drop detected! [Frame number: $frameNumber, Target: $target]")
                droppedFrameFlow.tryEmit(frameNumber)
            }
        }

        val droppedFrames = mutableSetOf<Long>()
        val droppedFrameJob = launch {
            droppedFrameFlow.asSharedFlow().collect { droppedFrames.add(it) }
        }

        val camInfo = cameraSelector.filter(cameraProvider.availableCameraInfos).first()
            .let { Camera2CameraInfo.from(it) }
        val aspectRatio = AspectRatio.RATIO_16_9

        // Create video capture with a recorder
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().setQualitySelector(
            QualitySelector.from(Quality.HIGHEST)).build())

        // Add Preview to ensure the preview stream does not drop frames during/after recordings
        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .apply { Camera2Interop.Extender(this).setSessionCaptureCallback(captureCallback) }
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(videoCapture)
            .addUseCase(preview)
            .apply {
                val hardwareLevel =
                    camInfo.getCameraCharacteristic(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

                if (hardwareLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    val imageCapture = ImageCapture.Builder()
                        .setTargetAspectRatio(aspectRatio)
                        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    addUseCase(imageCapture)
                }
            }.build()

        withContext(Dispatchers.Main) {
            // Sets surface provider to preview
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider())
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
        }

        // Record for at least 5 seconds
        val file1 = doTempRecording(videoCapture, 5000L)

        // Wait 5 seconds after recording has stopped to see if any frame drops occur
        delay(5000)

        // Record again for at least 5 seconds
        val file2 = doTempRecording(videoCapture, 5000L)

        // Wait 5 seconds more to see if any frame drops occur while VideoCapture is still bound
        delay(5000)

        file1.delete()
        file2.delete()

        droppedFrameJob.cancelAndJoin()

        return@coroutineScope droppedFrames.size
    }

    private suspend fun doTempRecording(
        videoCapture: VideoCapture<Recorder>,
        minDurationMillis: Long
    ): File {
        val tmpFile = createTempFileForRecording().apply { deleteOnExit() }

        videoCapture.output.prepareRecording(context,
            FileOutputOptions.Builder(tmpFile).build())
            .withAudioEnabled()
            .startWithRecording { eventFlow ->
                // Wait for our first status event to ensure recording is started
                eventFlow.waitForEvent<VideoRecordEvent.Status>(timeoutMs = 5000L)
                // Record for half the minimum duration now that we have a status
                delay(minDurationMillis / 2)

                // Pause in the middle of the recording
                pause()

                // Wait for the pause event
                eventFlow.waitForEvent<VideoRecordEvent.Pause>(timeoutMs = 5000L)

                // Stay paused for 1 second
                delay(1000L)

                // Resume the recording
                resume()

                // Wait for the resume event
                eventFlow.waitForEvent<VideoRecordEvent.Resume>(timeoutMs = 5000L)
                // Wait for a status event to ensure we are recording
                eventFlow.waitForEvent<VideoRecordEvent.Status>(timeoutMs = 5000L)

                // Record for the remaining half of the min duration time
                delay(minDurationMillis / 2)

                // Stop the recording
                stop()

                // Wait for the recording to finalize
                eventFlow.waitForEvent<VideoRecordEvent.Finalize>(timeoutMs = 5000L)
            }

        return tmpFile
    }

    @Suppress("BlockingMethodInNonBlockingContext") // See b/177458751
    private suspend fun createTempFileForRecording() = withContext(Dispatchers.IO) {
        File.createTempFile("CameraX", ".tmp")
    }

    private suspend inline fun <reified T : VideoRecordEvent>
        SharedFlow<VideoRecordEvent>.waitForEvent(timeoutMs: Long) =
        withTimeout(timeoutMs) { takeWhile { it !is T } }

    /**
     * Executes the given block in the scope of a recording [Recording] with a [SharedFlow]
     * containing the [VideoRecordEvent]s for that recording.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend inline fun PendingRecording.startWithRecording(
        crossinline block: suspend Recording.(SharedFlow<VideoRecordEvent>) -> Unit
    ) {
        val eventFlow = MutableSharedFlow<VideoRecordEvent>(replay = 1)
        val eventListener = Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Pause,
                is VideoRecordEvent.Resume,
                is VideoRecordEvent.Finalize -> {
                    // For all of these events, we need to reset the replay cache since we want
                    // them to be the first event received by new subscribers. The same is true for
                    // Start, but since no events should exist before start, we don't need to reset
                    // in that case.
                    eventFlow.resetReplayCache()
                }
            }
            // We still try to emit every event. This should cause the replay cache to contain one
            // of Start, Pause, Resume or Finalize. Status events will always only be sent after
            // Start or Resume, so they will only be sent to subscribers that have received one of
            // those events already.
            eventFlow.tryEmit(event)
        }

        val recording = start(CameraXExecutors.directExecutor(), eventListener)
        recording.use { it.apply { block(eventFlow) } }
    }
}