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

package androidx.camera.integration.core.util

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES
import android.hardware.camera2.CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES
import android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_OFF
import android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
import android.hardware.camera2.CameraMetadata.CONTROL_MODE_OFF
import android.hardware.camera2.CameraMetadata.DISTORTION_CORRECTION_MODE_OFF
import android.hardware.camera2.CameraMetadata.EDGE_MODE_OFF
import android.hardware.camera2.CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
import android.hardware.camera2.CameraMetadata.SHADING_MODE_OFF
import android.hardware.camera2.CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_GAINS
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_MODE
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_TRANSFORM
import android.hardware.camera2.CaptureRequest.CONTROL_MODE
import android.hardware.camera2.CaptureRequest.DISTORTION_CORRECTION_MODE
import android.hardware.camera2.CaptureRequest.EDGE_MODE
import android.hardware.camera2.CaptureRequest.SENSOR_TEST_PATTERN_DATA
import android.hardware.camera2.CaptureRequest.SENSOR_TEST_PATTERN_MODE
import android.hardware.camera2.CaptureRequest.SHADING_MODE
import android.hardware.camera2.CaptureRequest.TONEMAP_CURVE
import android.hardware.camera2.CaptureRequest.TONEMAP_MODE
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.camera.testing.impl.util.Camera2InteropUtil.builder
import androidx.camera.testing.impl.util.Camera2InteropUtil.from
import androidx.palette.graphics.Palette
import org.junit.Assume.assumeFalse
import org.junit.AssumptionViolatedException

/** Utility functions for testing with the camera sensor's test pattern. */
object SensorPatternUtil {

    private val LIMITED = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
    private val FULL = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    private val LEVEL_3 = setOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)

    /** The color channel to be used for the solid color test pattern. */
    enum class ColorChannel {
        RED,
        GREEN,
        BLUE;

        /**
         * Converts the [ColorChannel] enum to an [android.graphics.Color] int value.
         *
         * @return The [android.graphics.Color] int value corresponding to the color channel.
         */
        fun toColor(): Int {
            return when (this) {
                RED -> Color.RED
                GREEN -> Color.GREEN
                BLUE -> Color.BLUE
            }
        }
    }

    /**
     * Assumes that the camera supports the solid color test pattern mode.
     *
     * This function checks if the camera specified by [cameraInfo] and [implName] supports the
     * `SENSOR_TEST_PATTERN_MODE_SOLID_COLOR` test pattern. If the camera does not support this test
     * pattern, an [AssumptionViolatedException] is thrown, causing the test to be skipped.
     *
     * @param cameraInfo The [CameraInfo] of the camera to check.
     * @param implName The implementation name of the camera.
     * @throws AssumptionViolatedException if the camera does not support the solid color test
     *   pattern mode.
     */
    fun assumeSolidColorPatternSupported(cameraInfo: CameraInfo, implName: String) {
        // Skip for b/342016557
        assumeFalse(
            "Emulator API 30 reports incorrect supported available test pattern modes",
            Build.VERSION.SDK_INT == 30 && AndroidUtil.isEmulator(),
        )
        // Skip for b/412262667
        assumeFalse(
            "Emulator API 33-36 can not correctly apply solid color pattern",
            (Build.VERSION.SDK_INT == 33 ||
                Build.VERSION.SDK_INT == 34 ||
                Build.VERSION.SDK_INT == 35 ||
                Build.VERSION.SDK_INT == 36) && AndroidUtil.isEmulator(),
        )

        with(Camera2InteropUtil.Camera2CameraInfoWrapper.from(implName, cameraInfo)) {
            val availableTestPatterns = getCameraCharacteristic(SENSOR_AVAILABLE_TEST_PATTERN_MODES)
            if (availableTestPatterns?.contains(SENSOR_TEST_PATTERN_MODE_SOLID_COLOR) == false) {
                throw AssumptionViolatedException(
                    "Camera does not support solid color test pattern."
                )
            }
        }
    }

    /**
     * Sets the camera sensor to output a solid color pattern.
     *
     * @param camera The [Camera] instance to apply the solid color pattern to.
     * @param colorChannel The [ColorChannel] representing the desired solid color.
     * @param implName The implementation name of the camera.
     */
    fun setSolidColorPatternToCamera(camera: Camera, colorChannel: ColorChannel, implName: String) {
        Camera2InteropUtil.Camera2CameraControlWrapper.from(implName, camera.cameraControl).apply {
            setCaptureRequestOptions(
                createMinimalProcessedSolidColorCaptureRequestOptions(
                    colorChannel,
                    camera.cameraInfo,
                    implName,
                )
            )
        }
    }

    /**
     * Verifies if the primary color of the bitmap matches the expected color channel.
     *
     * @param bitmap The bitmap to analyze for dominant color.
     * @param colorChannel The expected dominant color channel.
     * @return True if the dominant color of the bitmap matches the expected color channel, false
     *   otherwise.
     */
    fun verifyColor(bitmap: Bitmap, colorChannel: ColorChannel): Boolean {
        return verifyColor(bitmap.getPrimaryColor(), colorChannel)
    }

    /**
     * Verifies if the color to be verified matches the expected color channel.
     *
     * @param colorToBeVerified The color to be verified.
     * @param expectedColorChannel The expected dominant color channel.
     * @return True if the normalized value of the expected color channel is greater than or equal
     *   to certain threshold, false otherwise.
     */
    fun verifyColor(colorToBeVerified: Int?, expectedColorChannel: ColorChannel): Boolean {
        if (colorToBeVerified == null) {
            return false
        }

        val redValue = Color.red(colorToBeVerified).toFloat()
        val greenValue = Color.green(colorToBeVerified).toFloat()
        val blueValue = Color.blue(colorToBeVerified).toFloat()
        val normalizedDenominator = redValue + greenValue + blueValue

        // Check denominator is not zero
        if (normalizedDenominator == 0f) {
            return false
        }
        val expectedDominantColor =
            when (expectedColorChannel) {
                ColorChannel.RED -> redValue / normalizedDenominator
                ColorChannel.GREEN -> greenValue / normalizedDenominator
                ColorChannel.BLUE -> blueValue / normalizedDenominator
            }

        return expectedDominantColor >= 2f / 3
    }

    /**
     * Retrieves the primary color from a bitmap.
     *
     * @return The primary color as an ARGB integer. Returns 0x00000000 (transparent black) if no
     *   color is found.
     */
    fun Bitmap.getPrimaryColor(): Int {
        val colorPalette = Palette.Builder(this).generate()
        return colorPalette.getDominantColor(0x00000000)
    }

    @JvmStatic
    private fun createMinimalProcessedSolidColorCaptureRequestOptions(
        colorChannel: ColorChannel,
        cameraInfo: CameraInfo,
        implName: String,
    ): Camera2InteropUtil.CaptureRequestOptionsWrapper {
        val sensorData =
            when (colorChannel) {
                ColorChannel.RED -> {
                    // Create sensor data of R: 100%, G: 0%, B: 0%
                    intArrayOf(
                        /*r=*/ 0xFFFFFFFF.toInt(),
                        /*g_even=*/ 0,
                        /*g_odd=*/ 0,
                        /*b=*/ 0,
                    )
                }
                ColorChannel.GREEN -> {
                    // Create sensor data of R: 0%, G: 100%, B: 0%
                    intArrayOf(
                        /*r=*/ 0,
                        /*g_even=*/ 0xFFFFFFFF.toInt(),
                        /*g_odd=*/ 0xFFFFFFFF.toInt(),
                        /*b=*/ 0,
                    )
                }
                ColorChannel.BLUE -> {
                    // Create sensor data of R: 0%, G: 0%, B: 100%
                    intArrayOf(
                        /*r=*/ 0,
                        /*g_even=*/ 0,
                        /*g_odd=*/ 0,
                        /*b=*/ 0xFFFFFFFF.toInt(),
                    )
                }
            }

        return Camera2InteropUtil.CaptureRequestOptionsWrapper.builder(implName)
            .apply {
                setCaptureRequestOption(SENSOR_TEST_PATTERN_DATA, sensorData)

                setCaptureRequestOption(
                    SENSOR_TEST_PATTERN_MODE,
                    SENSOR_TEST_PATTERN_MODE_SOLID_COLOR,
                )

                with(Camera2InteropUtil.Camera2CameraInfoWrapper.from(implName, cameraInfo)) {
                    val availableAberrationModes =
                        getCameraCharacteristic(
                            CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES
                        )
                    if (COLOR_CORRECTION_ABERRATION_MODE_OFF isOneOf availableAberrationModes) {
                        setCaptureRequestOption(
                            COLOR_CORRECTION_ABERRATION_MODE,
                            COLOR_CORRECTION_ABERRATION_MODE_OFF,
                        )
                    }

                    val availableEdgeModes =
                        getCameraCharacteristic(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
                    if (EDGE_MODE_OFF isOneOf availableEdgeModes) {
                        setCaptureRequestOption(EDGE_MODE, EDGE_MODE_OFF)
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
                                DISTORTION_CORRECTION_MODE,
                                DISTORTION_CORRECTION_MODE_OFF,
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
                        setCaptureRequestOption(CONTROL_MODE, CONTROL_MODE_OFF)
                    }

                    if (
                        hardwareLevel isOneOf (FULL + LEVEL_3) ||
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                SHADING_MODE_OFF isOneOf
                                    getCameraCharacteristic(
                                        CameraCharacteristics.SHADING_AVAILABLE_MODES
                                    )
                    ) {
                        setCaptureRequestOption(SHADING_MODE, SHADING_MODE_OFF)
                    }

                    if (hardwareLevel isOneOf (FULL + LEVEL_3)) {
                        setCaptureRequestOption(
                            COLOR_CORRECTION_MODE,
                            COLOR_CORRECTION_MODE_TRANSFORM_MATRIX,
                        )

                        setCaptureRequestOption(
                            COLOR_CORRECTION_GAINS,
                            RggbChannelVector(1f, 1f, 1f, 1f),
                        )

                        setCaptureRequestOption(
                            COLOR_CORRECTION_TRANSFORM,
                            ColorSpaceTransform(
                                intArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1)
                            ),
                        )
                    }

                    val availableTonemapModes =
                        getCameraCharacteristic(TONEMAP_AVAILABLE_TONE_MAP_MODES)
                    if (TONEMAP_MODE_CONTRAST_CURVE isOneOf availableTonemapModes) {
                        setCaptureRequestOption(TONEMAP_MODE, TONEMAP_MODE_CONTRAST_CURVE)

                        setCaptureRequestOption(
                            TONEMAP_CURVE,
                            TonemapCurve(
                                floatArrayOf(0f, 0f, 1f, 1f),
                                floatArrayOf(0f, 0f, 1f, 1f),
                                floatArrayOf(0f, 0f, 1f, 1f),
                            ),
                        )
                    }
                }
            }
            .build()
    }
}

private infix fun <T> T.isOneOf(set: Set<T>?) = set?.contains(this) ?: false

private infix fun Int.isOneOf(array: IntArray?) = array?.contains(this) ?: false
