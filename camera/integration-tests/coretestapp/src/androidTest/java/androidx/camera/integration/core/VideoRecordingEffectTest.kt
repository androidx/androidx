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
import android.hardware.camera2.CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult.SENSOR_TEST_PATTERN_MODE
import android.hardware.camera2.TotalCaptureResult
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.integration.core.util.SensorPatternUtil.ColorChannel
import androidx.camera.integration.core.util.SensorPatternUtil.assumeSolidColorPatternSupported
import androidx.camera.integration.core.util.SensorPatternUtil.setSolidColorPatternToCamera
import androidx.camera.integration.core.util.SensorPatternUtil.verifyColor
import androidx.camera.integration.core.util.doTempRecording
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil.setCameraCaptureSessionCallback
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoRecordingEffectTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {

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
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun recordSdrVideoRed_producesRedVideo_withEffect() =
        runSolidColorRecordingTest(colorChannel = ColorChannel.RED, dynamicRange = DynamicRange.SDR)

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun recordSdrVideoGreen_producesGreenVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.GREEN,
            dynamicRange = DynamicRange.SDR,
        )

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun recordSdrVideoBlue_producesBlueVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.BLUE,
            dynamicRange = DynamicRange.SDR,
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoRed_producesRedVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.RED,
            dynamicRange = DynamicRange.HLG_10_BIT,
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoGreen_producesGreenVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.GREEN,
            dynamicRange = DynamicRange.HLG_10_BIT,
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoBlue_producesBlueVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.BLUE,
            dynamicRange = DynamicRange.HLG_10_BIT,
        )

    @RequiresApi(29)
    private fun runSolidColorRecordingTest(
        colorChannel: ColorChannel,
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED,
    ) =
        runCameraTest(cameraConfig) { camProvider ->
            val camInfo = camProvider.getCameraInfo(cameraSelector)

            // Assume the device/camera supports requested dynamic range
            with(Recorder.getVideoCapabilities(camInfo)) {
                Assume.assumeTrue(getSupportedQualities(dynamicRange).isNotEmpty())
            }

            // Assume the device supports solid color test patterns
            assumeSolidColorPatternSupported(camInfo, implName)

            val testPatternModeFlow = MutableStateFlow<Int?>(null)
            val captureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        result.get(SENSOR_TEST_PATTERN_MODE)?.let { testPatternModeFlow.value = it }
                    }
                }

            // Create video capture with a recorder
            val videoCapture =
                VideoCapture.Builder(Recorder.Builder().build())
                    .setDynamicRange(dynamicRange)
                    .build()

            // Add Preview to ensure the preview stream does not drop frames during/after recordings
            val preview =
                Preview.Builder()
                    .apply { setCameraCaptureSessionCallback(implName, this, captureCallback) }
                    .build()

            var effectJob: Job? = null
            val useCaseGroup =
                UseCaseGroup.Builder()
                    .apply {
                        addUseCase(videoCapture)
                        addUseCase(preview)

                        setViewPort(ViewPort.Builder(Rational(9, 16), Surface.ROTATION_0).build())

                        effectJob = launchCopyEffect { addEffect(it) }
                    }
                    .build()

            withContext(Dispatchers.Main) {
                val lifecycleOwner = FakeLifecycleOwner()
                // Sets surface provider to preview
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                val camera =
                    camProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
                setSolidColorPatternToCamera(camera, colorChannel, implName)

                lifecycleOwner.startAndResume()
                try {
                    // Wait for a capture result with the test image
                    testPatternModeFlow.first { it == SENSOR_TEST_PATTERN_MODE_SOLID_COLOR }

                    with(
                        doTempRecording(
                            context = ApplicationProvider.getApplicationContext(),
                            videoCapture = videoCapture,
                            minDurationMillis = 1000,
                            pauseDurationMillis = 0,
                            withAudio = false,
                        )
                    ) {
                        try {
                            ThumbnailUtils.createVideoThumbnail(this, Size(90, 160), null).also {
                                bitmap ->
                                assertThat(verifyColor(bitmap, colorChannel)).isTrue()
                            }
                        } finally {
                            delete()
                        }
                    }
                } finally {
                    lifecycleOwner.pauseAndStop()
                    lifecycleOwner.destroy()
                    effectJob?.cancel()
                }
            }
        }

    private inline fun runCameraTest(
        cameraConfig: CameraXConfig,
        crossinline block: suspend CoroutineScope.(ProcessCameraProvider) -> Unit,
    ): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        val context: Context = ApplicationProvider.getApplicationContext()
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        try {
            block(cameraProvider)
        } finally {
            withContext(NonCancellable) { cameraProvider.shutdownAsync().await() }
        }
    }
}
