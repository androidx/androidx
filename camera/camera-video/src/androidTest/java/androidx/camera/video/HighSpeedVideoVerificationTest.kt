/*
 * Copyright 2025 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Range
import android.util.Rational
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_240_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_480_480
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.getDurationMs
import androidx.camera.testing.impl.getRotatedResolution
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalHighSpeedVideo::class)
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class HighSpeedVideoVerificationTest(
    private val cameraConfig: CameraXConfig,
    private val implName: String,
    private val cameraSelector: CameraSelector,
    private val lensFacing: Int,
    private val dynamicRange: DynamicRange,
    private val dynamicRangeName: String,
    private val quality: Quality,
    private val qualityName: String,
    private val captureFrameRate: Range<Int>,
) {
    companion object {
        private const val SLOW_MOTION_ENCODE_FRAME_RATE = 30
        private val cameraConfigs =
            arrayOf(
                Camera2Config::class.simpleName to Camera2Config.defaultConfig(),
                CameraPipeConfig::class.simpleName to CameraPipeConfig.defaultConfig(),
            )
        private val cameraSelectors = arrayOf(DEFAULT_BACK_CAMERA, DEFAULT_FRONT_CAMERA)
        private val dynamicRanges = arrayOf("SDR" to SDR, "HLG" to HLG_10_BIT)
        private val qualities = arrayOf(SD, HD, FHD, UHD)
        private val captureFrameRates = arrayOf(FPS_120_120, FPS_240_240, FPS_480_480)

        @JvmStatic
        @Parameterized.Parameters(
            name = "quality={7}, captureFrameRate={8}, config={1}, lensFacing={3}, dynamicRange={5}"
        )
        fun data(): List<Array<Any?>> =
            cameraConfigs.flatMap { (cameraConfigName, cameraConfig) ->
                cameraSelectors.flatMap { cameraSelector ->
                    dynamicRanges.flatMap { (dynamicRangeName, dynamicRange) ->
                        qualities.flatMap { quality ->
                            captureFrameRates.map { captureFrameRate ->
                                arrayOf(
                                    cameraConfig,
                                    cameraConfigName,
                                    cameraSelector,
                                    cameraSelector.lensFacing,
                                    dynamicRange,
                                    dynamicRangeName,
                                    quality,
                                    (quality as Quality.ConstantQuality).name,
                                    captureFrameRate,
                                )
                            }
                        }
                    }
                }
            }
    }

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val skipAndPreTestRule: TestRule =
        RuleChain.outerRule(IgnoreVideoRecordingProblematicDeviceRule())
            .around(
                CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)
            )
            .around(
                CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
                    CameraUtil.PreTestCameraIdList(cameraConfig)
                )
            )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(videoCapabilities, Recorder.DEFAULT_QUALITY_SELECTOR)
    }
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraInfo: CameraInfo
    private lateinit var camera: Camera
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var recordingSession: RecordingSession
    private lateinit var preview: Preview
    private lateinit var videoCapture: VideoCapture<Recorder>

    @OptIn(ExperimentalSessionConfig::class)
    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }

        // Check high-speed capability
        val videoCapabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)
        assumeTrue("$implName: High-speed video is not supported", videoCapabilities != null)

        // Check high-speed capability for quality and dynamic range
        assumeTrue(
            "$implName: High-speed video is not supported for $qualityName and $dynamicRangeName",
            videoCapabilities!!.isQualitySupported(quality, dynamicRange),
        )

        this.videoCapabilities = videoCapabilities

        // Create UseCases
        videoCapture =
            VideoCapture.Builder(
                    Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
                )
                .setDynamicRange(dynamicRange)
                .build()
        preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
        }

        // Check frame rate
        val highSpeedConfig = HighSpeedVideoSessionConfig(videoCapture, preview)
        val supportedFrameRateRanges = cameraInfo.getSupportedFrameRateRanges(highSpeedConfig)
        assumeTrue(
            "$captureFrameRate is not in supported set: $supportedFrameRateRanges",
            supportedFrameRateRanges.contains(captureFrameRate),
        )

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
            recordingSession.release(timeoutMs = 5000L)
        }
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun canRecordHighSpeedVideo() {
        testRecording()
    }

    @Test
    fun canRecordSlowMotionVideo() {
        testRecording(isSlowMotionEnabled = true)
    }

    @SuppressLint("BanThreadSleep")
    @OptIn(ExperimentalSessionConfig::class)
    private fun testRecording(isSlowMotionEnabled: Boolean = false) {
        // Arrange.
        val highSpeedVideoConfig =
            HighSpeedVideoSessionConfig(
                frameRateRange = captureFrameRate,
                videoCapture = videoCapture,
                preview = preview,
                isSlowMotionEnabled = isSlowMotionEnabled,
            )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, highSpeedVideoConfig)
        }

        // Act & Verify.
        val recordingDurationMs = 1000L
        val recording =
            recordingSession.createRecording(recorder = videoCapture.output).startAndVerify()
        sleep(recordingDurationMs)
        val finalize = recording.stopAndVerify()

        // Verify: verify video metadata.
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(finalize.file))

            // Verify video resolution.
            val videoProfile =
                videoCapabilities.getProfiles(quality, dynamicRange)!!.defaultVideoProfile
            val expectedResolution =
                rotateSize(videoProfile.resolution, getRotationNeeded(videoCapture, cameraInfo))

            assertThat(it.getRotatedResolution()).isEqualTo(expectedResolution)

            // Verify video duration for slow-motion recording.
            if (isSlowMotionEnabled) {
                // ex: For 1/4x slow-motion recording, i.e. 120 capture fps to 30 encoding fps,
                // and the recording duration is 1 second at least,
                // the recorded duration should be (1 / 1/4x) = 4 second at least.
                val captureEncodeRatio =
                    Rational(captureFrameRate.upper, SLOW_MOTION_ENCODE_FRAME_RATE)
                val atLeastVideoDurationMs =
                    (recordingDurationMs * captureEncodeRatio.toDouble()).toLong()

                assertThat(it.getDurationMs()).isAtLeast(atLeastVideoDurationMs)
            }
        }
    }
}
