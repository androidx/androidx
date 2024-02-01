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

package androidx.camera.video

import android.content.Context
import android.os.Build
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class SupportedQualitiesVerificationTest(
    private val lensFacing: Int,
    private var cameraSelector: CameraSelector,
    private var quality: Quality,
    private val cameraConfig: CameraXConfig,
    private val implName: String,
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L

        @JvmStatic
        private val cameraSelectors =
            arrayOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)

        @JvmStatic
        private val quality = arrayOf(
            Quality.SD,
            Quality.HD,
            Quality.FHD,
            Quality.UHD,
            Quality.LOWEST,
            Quality.HIGHEST,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, quality={2}, config={4}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            cameraSelectors.forEach { cameraSelector ->
                quality.forEach { quality ->
                    add(
                        arrayOf(
                            cameraSelector.lensFacing,
                            cameraSelector,
                            quality,
                            Camera2Config.defaultConfig(),
                            Camera2Config::class.simpleName
                        )
                    )
                    add(
                        arrayOf(
                            cameraSelector.lensFacing,
                            cameraSelector,
                            quality,
                            CameraPipeConfig.defaultConfig(),
                            CameraPipeConfig::class.simpleName
                        )
                    )
                }
            }
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    // TODO(b/278168212): Only SDR is checked by now. Need to extend to HDR dynamic ranges.
    private val dynamicRange = DynamicRange.SDR
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraInfo: CameraInfo
    private lateinit var camera: Camera

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        instrumentation.runOnMainSync {

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }

        // Ignore the unsupported Quality options
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        Assume.assumeTrue(
            "Camera ${cameraSelector.lensFacing} not support $quality, skip this test item.",
            videoCapabilities.isQualitySupported(quality, dynamicRange)
        )
    }

    @After
    fun tearDown() {
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun qualityOptionCanRecordVideo() {
        testQualityOptionRecordVideo()
    }

    @Test
    fun qualityOptionCanRecordVideo_enableSurfaceProcessing() {
        assumeSuccessfulSurfaceProcessing()

        testQualityOptionRecordVideo(enableSurfaceProcessing = true)
    }

    private fun testQualityOptionRecordVideo(enableSurfaceProcessing: Boolean = false) {
        // Arrange.
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        val videoProfile =
            videoCapabilities.getProfiles(quality, dynamicRange)!!.defaultVideoProfile
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
        val videoCapture = VideoCapture.Builder(recorder).apply {
            if (enableSurfaceProcessing) {
                setSurfaceProcessingForceEnabled()
            }
        }.build()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val latchForRecordingStatus = CountDownLatch(5)
        val latchForRecordingFinalized = CountDownLatch(1)
        var finalizedEvent: VideoRecordEvent.Finalize? = null
        val eventListener = Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Status -> {
                    // Make sure the recording proceed for a while.
                    latchForRecordingStatus.countDown()
                }

                is VideoRecordEvent.Finalize -> {
                    finalizedEvent = event
                    latchForRecordingFinalized.countDown()
                }

                else -> {
                    // Ignore other events.
                }
            }
        }

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                videoCapture,
            )
        }

        if (enableSurfaceProcessing) {
            // Ensure the surface processing is enabled.
            assertThat(isSurfaceProcessingEnabled(videoCapture)).isTrue()
        }

        // Act.
        videoCapture.startVideoRecording(file, eventListener).use {
            // Verify the recording proceed for a while.
            assertThat(latchForRecordingStatus.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        }

        // Verify the recording is finalized without error.
        assertThat(latchForRecordingFinalized.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        assertThat(finalizedEvent!!.error).isEqualTo(VideoRecordEvent.Finalize.ERROR_NONE)

        // Verify resolution.
        if (!hasExtraCroppingQuirk(implName)) {
            verifyVideoResolution(
                context,
                file,
                rotateSize(
                    Size(videoProfile.width, videoProfile.height),
                    getRotationNeeded(videoCapture, cameraInfo)
                )
            )
        }

        // Clean up
        file.delete()
    }

    private fun VideoCapture<Recorder>.startVideoRecording(
        file: File,
        eventListener: Consumer<VideoRecordEvent>
    ): Recording =
        output.prepareRecording(
            context, FileOutputOptions.Builder(file).build()
        ).start(
            CameraXExecutors.directExecutor(), eventListener
        )
}
