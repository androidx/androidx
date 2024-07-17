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
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_OFF
import android.hardware.camera2.CameraCharacteristics.CONTROL_MODE_OFF
import android.hardware.camera2.CameraCharacteristics.DISTORTION_CORRECTION_MODE_OFF
import android.hardware.camera2.CameraCharacteristics.EDGE_MODE_OFF
import android.hardware.camera2.CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES
import android.hardware.camera2.CameraCharacteristics.SHADING_MODE_OFF
import android.hardware.camera2.CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.CaptureResult.SENSOR_TEST_PATTERN_MODE
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.integration.core.util.CameraPipeUtil.builder
import androidx.camera.integration.core.util.CameraPipeUtil.from
import androidx.camera.integration.core.util.CameraPipeUtil.setCameraCaptureSessionCallback
import androidx.camera.integration.core.util.doTempRecording
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.palette.graphics.Palette
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
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoRecordingEffectTest(
    private val implName: String,
    val selectorName: String,
    private val cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

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
        private val LIMITED = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        private val FULL = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        private val LEVEL_3 = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)

        @JvmStatic
        @Parameterized.Parameters(name = "{1}+{0}")
        fun data(): List<Array<Any?>> {
            return listOf(
                arrayOf(
                    Camera2Config::class.simpleName,
                    "back",
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig()
                ),
                arrayOf(
                    Camera2Config::class.simpleName,
                    "front",
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig()
                ),
                arrayOf(
                    CameraPipeConfig::class.simpleName,
                    "back",
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig()
                ),
                arrayOf(
                    CameraPipeConfig::class.simpleName,
                    "front",
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig()
                ),
            )
        }
    }

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
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
            dynamicRange = DynamicRange.SDR
        )

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun recordSdrVideoBlue_producesBlueVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.BLUE,
            dynamicRange = DynamicRange.SDR
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoRed_producesRedVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.RED,
            dynamicRange = DynamicRange.HLG_10_BIT
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoGreen_producesGreenVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.GREEN,
            dynamicRange = DynamicRange.HLG_10_BIT
        )

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun recordHlg10VideoBlue_producesBlueVideo_withEffect() =
        runSolidColorRecordingTest(
            colorChannel = ColorChannel.BLUE,
            dynamicRange = DynamicRange.HLG_10_BIT
        )

    private enum class ColorChannel {
        RED,
        GREEN,
        BLUE
    }

    @RequiresApi(29)
    private fun runSolidColorRecordingTest(
        colorChannel: ColorChannel,
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED
    ) =
        runCameraTest(cameraConfig) { camProvider ->
            val camInfo = camProvider.getCameraInfo(cameraSelector)

            // Assume the device/camera supports requested dynamic range
            with(Recorder.getVideoCapabilities(camInfo)) {
                Assume.assumeTrue(getSupportedQualities(dynamicRange).isNotEmpty())
            }

            // Assume the device supports solid color test patterns
            assumeSolidColorPatternSupported(camInfo)

            val testPatternModeFlow = MutableStateFlow<Int?>(null)
            val captureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
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

                CameraPipeUtil.Camera2CameraControlWrapper.from(implName, camera.cameraControl)
                    .apply {
                        setCaptureRequestOptions(
                            createMinimalProcessedSolidColorCaptureRequestOptions(
                                colorChannel,
                                camera.cameraInfo
                            )
                        )
                    }

                lifecycleOwner.startAndResume()
                try {
                    // Wait for a capture result with the test image
                    testPatternModeFlow.first {
                        it == CaptureResult.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
                    }

                    with(
                        doTempRecording(
                            context = ApplicationProvider.getApplicationContext(),
                            videoCapture = videoCapture,
                            minDurationMillis = 1000,
                            pauseDurationMillis = 0,
                            withAudio = false
                        )
                    ) {
                        try {
                            ThumbnailUtils.createVideoThumbnail(this, Size(90, 160), null).also {
                                bitmap ->
                                val colorPalette = Palette.Builder(bitmap).generate()
                                val primaryColor = colorPalette.getDominantColor(0x00000000)
                                val normalizedDenom =
                                    Color.red(primaryColor) +
                                        Color.green(primaryColor) +
                                        Color.blue(primaryColor)

                                val expectedDominantColor =
                                    when (colorChannel) {
                                        ColorChannel.RED ->
                                            Color.red(primaryColor).toFloat() / normalizedDenom
                                        ColorChannel.GREEN ->
                                            Color.green(primaryColor).toFloat() / normalizedDenom
                                        ColorChannel.BLUE ->
                                            Color.blue(primaryColor).toFloat() / normalizedDenom
                                    }

                                // Assert the selected channel is at least 2 / 3
                                // of the signal on from the sensor
                                assertThat(expectedDominantColor).isAtLeast(2f / 3)
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

    private fun assumeSolidColorPatternSupported(cameraInfo: CameraInfo) {
        // Skip for b/342016557
        Assume.assumeFalse(
            "Emulator API 30 reports incorrect supported available test pattern modes",
            Build.VERSION.SDK_INT == 30 && AndroidUtil.isEmulator()
        )

        with(CameraPipeUtil.Camera2CameraInfoWrapper.from(implName, cameraInfo)) {
            val availableTestPatterns = getCameraCharacteristic(SENSOR_AVAILABLE_TEST_PATTERN_MODES)
            if (availableTestPatterns?.contains(SENSOR_TEST_PATTERN_MODE_SOLID_COLOR) == false) {
                throw AssumptionViolatedException(
                    "Camera does not support solid color test pattern."
                )
            }
        }
    }

    private fun createMinimalProcessedSolidColorCaptureRequestOptions(
        colorChannel: ColorChannel,
        cameraInfo: CameraInfo
    ): CameraPipeUtil.CaptureRequestOptionsWrapper {
        val sensorData =
            when (colorChannel) {
                ColorChannel.RED -> {
                    // Create sensor data of R: 100%, G: 0%, B: 0%
                    intArrayOf(
                        /*r=*/ 0xFFFFFFFF.toInt(),
                        /*g_even=*/ 0,
                        /*g_odd=*/ 0,
                        /*b=*/ 0
                    )
                }
                ColorChannel.GREEN -> {
                    // Create sensor data of R: 0%, G: 100%, B: 0%
                    intArrayOf(
                        /*r=*/ 0,
                        /*g_even=*/ 0xFFFFFFFF.toInt(),
                        /*g_odd=*/ 0xFFFFFFFF.toInt(),
                        /*b=*/ 0
                    )
                }
                ColorChannel.BLUE -> {
                    // Create sensor data of R: 0%, G: 0%, B: 100%
                    intArrayOf(
                        /*r=*/ 0,
                        /*g_even=*/ 0,
                        /*g_odd=*/ 0,
                        /*b=*/ 0xFFFFFFFF.toInt()
                    )
                }
            }

        return CameraPipeUtil.CaptureRequestOptionsWrapper.builder(implName)
            .apply {
                setCaptureRequestOption(CaptureRequest.SENSOR_TEST_PATTERN_DATA, sensorData)

                setCaptureRequestOption(
                    CaptureRequest.SENSOR_TEST_PATTERN_MODE,
                    CaptureRequest.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
                )

                with(CameraPipeUtil.Camera2CameraInfoWrapper.from(implName, cameraInfo)) {
                    val availableAberrationModes =
                        getCameraCharacteristic(
                            CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES
                        )
                    if (COLOR_CORRECTION_ABERRATION_MODE_OFF isOneOf availableAberrationModes) {
                        setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF
                        )
                    }

                    val availableEdgeModes =
                        getCameraCharacteristic(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
                    if (EDGE_MODE_OFF isOneOf availableEdgeModes) {
                        setCaptureRequestOption(
                            CaptureRequest.EDGE_MODE,
                            CaptureRequest.EDGE_MODE_OFF
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val availableDistortionCorrectionModes =
                            getCameraCharacteristic(
                                CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES
                            )
                        if (
                            DISTORTION_CORRECTION_MODE_OFF isOneOf
                                availableDistortionCorrectionModes
                        ) {
                            setCaptureRequestOption(
                                CaptureRequest.DISTORTION_CORRECTION_MODE,
                                CaptureRequest.DISTORTION_CORRECTION_MODE_OFF
                            )
                        }
                    }

                    val hardwareLevel =
                        getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    if (
                        hardwareLevel isOneOf (LIMITED + FULL + LEVEL_3) ||
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                CONTROL_MODE_OFF isOneOf
                                    getCameraCharacteristic(
                                        CameraCharacteristics.CONTROL_AVAILABLE_MODES
                                    )
                    ) {
                        setCaptureRequestOption(
                            CaptureRequest.CONTROL_MODE,
                            CaptureRequest.CONTROL_MODE_OFF
                        )
                    }

                    if (
                        hardwareLevel isOneOf (FULL + LEVEL_3) ||
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                SHADING_MODE_OFF isOneOf
                                    getCameraCharacteristic(
                                        CameraCharacteristics.SHADING_AVAILABLE_MODES
                                    )
                    ) {
                        setCaptureRequestOption(
                            CaptureRequest.SHADING_MODE,
                            CaptureRequest.SHADING_MODE_OFF
                        )
                    }

                    if (hardwareLevel isOneOf (FULL + LEVEL_3)) {
                        setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_MODE,
                            CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                        )

                        setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_GAINS,
                            RggbChannelVector(1f, 1f, 1f, 1f)
                        )

                        setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_TRANSFORM,
                            ColorSpaceTransform(
                                intArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1)
                            )
                        )
                    }

                    val availableTonemapModes =
                        getCameraCharacteristic(
                            CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES
                        )
                    if (
                        CameraCharacteristics.TONEMAP_MODE_CONTRAST_CURVE isOneOf
                            availableTonemapModes
                    ) {
                        setCaptureRequestOption(
                            CaptureRequest.TONEMAP_MODE,
                            CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE
                        )

                        setCaptureRequestOption(
                            CaptureRequest.TONEMAP_CURVE,
                            TonemapCurve(
                                floatArrayOf(0f, 0f, 1f, 1f),
                                floatArrayOf(0f, 0f, 1f, 1f),
                                floatArrayOf(0f, 0f, 1f, 1f)
                            )
                        )
                    }
                }
            }
            .build()
    }

    private inline fun runCameraTest(
        cameraConfig: CameraXConfig,
        crossinline block: suspend CoroutineScope.(ProcessCameraProvider) -> Unit
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

private infix fun <T> T.isOneOf(set: Set<T>?) = set?.contains(this) ?: false

private infix fun Int.isOneOf(array: IntArray?) = array?.contains(this) ?: false
