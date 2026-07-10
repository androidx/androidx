/*
 * Copyright 2026 The Android Open Source Project
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
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_UNSPECIFIED
import androidx.camera.video.internal.config.DynamicRangeFormatComboRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 23)
class SupportedFormatsVerificationTest(
    private val dynamicRange: DynamicRange,
    private val dynamicRangeLabel: String,
) {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val skipRule: TestRule = IgnoreVideoRecordingProblematicDeviceRule()

    companion object {
        private const val TAG = "SupportedFormatsTest"

        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(SDR, "SDR"),
                arrayOf(HLG_10_BIT, "HLG_10_BIT"),
                arrayOf(HDR10_10_BIT, "HDR10_10_BIT"),
                arrayOf(HDR10_PLUS_10_BIT, "HDR10_PLUS_10_BIT"),
                arrayOf(DOLBY_VISION_8_BIT, "DOLBY_VISION_8_BIT"),
                arrayOf(DOLBY_VISION_10_BIT, "DOLBY_VISION_10_BIT"),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var recordingSession: RecordingSession
    private lateinit var cameraSelector: CameraSelector
    private lateinit var camera: Camera
    private lateinit var cameraInfo: CameraInfo
    private var audioStreamAvailable = false

    @Before
    fun setUp() {
        assumeFalse(
            "Test fails on cuttlefish b/467136521",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )

        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig())
        cameraProvider = ProcessCameraProvider.getInstance(context).get()

        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }

        assumeTrue(
            "DynamicRange $dynamicRangeLabel not supported by device.",
            cameraInfo
                .querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED))
                .contains(dynamicRange),
        )

        preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
        }

        audioStreamAvailable = AudioChecker.canAudioStreamBeStarted()

        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
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

    @SuppressLint("BanThreadSleep")
    @Test
    fun verifySupportedFormatCombinations() {
        val registry =
            DynamicRangeFormatComboRegistry.getRegistry(dynamicRange)
                ?: throw AssumptionViolatedException(
                    "No registry found for DynamicRange: $dynamicRangeLabel"
                )

        // Get supported MIME types from Recorder
        val supportedVideoMimes = Recorder.getSupportedVideoMimeTypes()
        val supportedAudioMimes = Recorder.getSupportedAudioMimeTypes()

        Log.d(TAG, "Supported video mimes: $supportedVideoMimes")
        Log.d(TAG, "Supported audio mimes: $supportedAudioMimes")

        val testResultCollector = TestResultCollector()

        // Iterate over all supported video and audio MIME types
        supportedVideoMimes
            // Filter out unsupported Video Mimes for dynamic range
            .filter { videoMime ->
                val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo, videoMime)
                val isSupported = dynamicRange in videoCapabilities!!.supportedDynamicRanges
                if (!isSupported) {
                    Log.d(
                        TAG,
                        "DynamicRange $dynamicRangeLabel not supported for video mime: $videoMime",
                    )
                }
                isSupported
            }
            // Combine Video Mimes with Audio Mimes
            .flatMap { videoMime ->
                supportedAudioMimes.map { audioMime -> videoMime to audioMime }
            }
            // Filter out unknown Video Audio Mimes combinations
            .filter { (videoMime, audioMime) ->
                val combos = registry.getCombos(OUTPUT_FORMAT_UNSPECIFIED, videoMime, audioMime)
                if (combos.isEmpty()) {
                    Log.d(
                        TAG,
                        "No combos found for DynamicRange: $dynamicRangeLabel, Video: $videoMime, Audio: $audioMime",
                    )
                }
                combos.isNotEmpty()
            }
            .forEach { (videoMime, audioMime) ->
                val config = "DynamicRange=$dynamicRangeLabel, Video=$videoMime, Audio=$audioMime"
                Log.d(TAG, "Testing combo: $config")

                val recorder =
                    Recorder.Builder()
                        .apply {
                            setVideoMimeType(videoMime)
                            setAudioMimeType(audioMime)
                        }
                        .build()

                val videoCapture =
                    VideoCapture.Builder(recorder).setDynamicRange(dynamicRange).build()

                assumeTrue(
                    "The UseCase combination is not supported.",
                    camera.isUseCasesCombinationSupported(preview, videoCapture),
                )

                instrumentation.runOnMainSync {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture,
                    )
                }

                try {
                    recordingSession.createRecording(recorder = recorder).recordAndVerify()

                    testResultCollector.addPassed(config)
                } catch (e: Throwable) {
                    testResultCollector.addFailed(config, e)
                }

                instrumentation.runOnMainSync { cameraProvider.unbindAll() }

                // Sleep a while to avoid conflict with next test.
                Thread.sleep(1000L)
            }

        testResultCollector.reportPassed()
        testResultCollector.assertNoFailures()
    }

    private data class TestResult(val config: String, val cause: Throwable? = null)

    private class TestResultCollector {
        private val passed = mutableListOf<TestResult>()
        private val failed = mutableListOf<TestResult>()

        fun addPassed(config: String) {
            passed.add(TestResult(config))
        }

        fun addFailed(config: String, error: Throwable) {
            failed.add(TestResult(config, error))
        }

        /** Logs all successful combinations to Logcat. */
        fun reportPassed() {
            if (passed.isEmpty()) {
                Log.d(TAG, "No combinations passed verification.")
                return
            }

            val report = buildString {
                appendLine("Passed format verification (${passed.size} total combinations)")
                passed.forEachIndexed { index, result ->
                    appendLine("[#${index + 1}] Passed config: ${result.config}")
                }
            }
            Log.d(TAG, report)
        }

        /** Throws an AssertionError if any combinations failed. */
        fun assertNoFailures() {
            if (failed.isEmpty()) return

            val report = buildString {
                appendLine("Failed format verification (${failed.size} total failures)")

                failed.forEachIndexed { index, failure ->
                    val reason = failure.cause!!.let { it.message ?: it.toString() }
                    appendLine("[#${index + 1}] ${failure.config}")
                    appendLine("    Reason: $reason")
                }
            }
            throw AssertionError(report)
        }
    }
}
