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

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.integration.core.util.doTempRecording
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Frame drops sometimes happen for reasons beyond our control. This test will always be flaky.
// The main purpose is to catch cases where frame drops happen consistently.
@FlakyTest
@LargeTest
@RunWith(Parameterized::class)
class VideoRecordingFrameDropTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
    private val perSelectorTestData: PerSelectorTestData,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName.contains(CameraPipeConfig::class.simpleName!!))

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    // Due to the flaky nature of this test, it should only be run in the lab
    @get:Rule val labTestRule = LabTestRule()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    data class PerSelectorTestData(
        var hasResult: Boolean = false,
        var routineError: Exception? = null,
        var numDroppedFrames: Int = 0,
    )

    companion object {
        private const val TAG = "RecordingFrameDropTest"
        lateinit var cameraProvider: ProcessCameraProvider
        var needsShutdown = false

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                            PerSelectorTestData(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                            PerSelectorTestData(),
                        )
                    )
                }
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29,
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
            cameraProvider.shutdownAsync().await()
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedNoFrames() {
        verifyFrameDropForCamera2Config(0)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedLessThanFiveFrames() {
        verifyFrameDropForCamera2Config(5)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedLessThanTenFrames() {
        verifyFrameDropForCamera2Config(10)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun droppedLessThanFifteenFrames() {
        assertThat(perSelectorTestData.numDroppedFrames).isLessThan(15)
    }

    private fun verifyFrameDropForCamera2Config(numberOfDroppedFrames: Int) {
        // Run this test only for Camera2 configuration to continue tracking framedrops
        // for Camera2 Configuration
        Assume.assumeTrue(implName.endsWith(Camera2Config::class.simpleName!!))
        if (numberOfDroppedFrames == 0) {
            assertThat(perSelectorTestData.numDroppedFrames).isEqualTo(numberOfDroppedFrames)
        } else {
            assertThat(perSelectorTestData.numDroppedFrames).isLessThan(numberOfDroppedFrames)
        }
    }

    @Suppress("DEPRECATION") // legacy resolution API
    private suspend fun runRecordingRoutineAndReturnNumDroppedFrames(): Int = coroutineScope {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        needsShutdown = true

        val droppedFrameFlow = MutableSharedFlow<Long>(replay = Channel.UNLIMITED)
        val captureCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureBufferLost(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    target: Surface,
                    frameNumber: Long,
                ) {
                    Logger.e(
                        TAG,
                        "Frame drop detected! [Frame number: $frameNumber, Target: $target]",
                    )
                    droppedFrameFlow.tryEmit(frameNumber)
                }
            }

        val droppedFrames = mutableSetOf<Long>()
        val droppedFrameJob = launch {
            droppedFrameFlow.asSharedFlow().collect { droppedFrames.add(it) }
        }

        val aspectRatio = AspectRatio.RATIO_16_9

        // Create video capture with a recorder
        val videoCapture =
            VideoCapture.withOutput(
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            )

        // Add Preview to ensure the preview stream does not drop frames during/after recordings
        val preview =
            Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .apply { Camera2Interop.Extender(this).setSessionCaptureCallback(captureCallback) }
                .build()

        val imageCapture =
            ImageCapture.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            // Sets surface provider to preview
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)

            val isImageCaptureSupportedAs3rdUseCase =
                camera.isUseCasesCombinationSupported(preview, videoCapture, imageCapture)
            val useCaseGroup =
                UseCaseGroup.Builder()
                    .addUseCase(videoCapture)
                    .addUseCase(preview)
                    .apply {
                        if (isImageCaptureSupportedAs3rdUseCase) {
                            addUseCase(imageCapture)
                        } else {
                            Logger.d(
                                TAG,
                                "Skipping ImageCapture use case, because this device" +
                                    " doesn't support 3 use case combination" +
                                    " (Preview, Video, ImageCapture).",
                            )
                        }
                    }
                    .build()

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)

            val files = mutableListOf<File>()
            lifecycleOwner.startAndResume()
            try {
                // Record for at least 5 seconds
                files.add(doTempRecording(context, videoCapture, 5000L))

                // Wait 5 seconds after recording has stopped to see if any frame drops occur
                delay(5000)

                // Record again for at least 5 seconds
                files.add(doTempRecording(context, videoCapture, 5000L))

                // Wait 5 seconds more to see if any frame drops occur while VideoCapture
                // is still bound.
                delay(5000)
            } finally {
                lifecycleOwner.pauseAndStop()
                lifecycleOwner.destroy()
                withContext(Dispatchers.IO) { files.forEach { it.delete() } }
            }
        }

        droppedFrameJob.cancelAndJoin()

        return@coroutineScope droppedFrames.size
    }
}
