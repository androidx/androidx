/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.compat.workaround

import android.graphics.Rect
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class StillCaptureFlowTest(
    private val brand: String,
    private val model: String,
    private val device: String,
    private val aeMode: Int,
    private val template: Int,
    private val cameraId: String?,
    private val hasMultipleRepeatingStreams: Boolean,
    private val isZoomed: Boolean,
    private val stopRepeatingExpected: Boolean,
    private val zoomRatio: Float?,
) {
    @Test
    fun shouldStopRepeating() {
        assumeTrue(zoomRatio == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", device)

        val parameters =
            mutableMapOf<CaptureRequest.Key<*>, Any>(CaptureRequest.CONTROL_AE_MODE to aeMode)
        if (isZoomed) {
            parameters[CaptureRequest.SCALER_CROP_REGION] = Rect(1278, 958, 2370, 1778)
        }
        if (zoomRatio != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parameters[CaptureRequest.CONTROL_ZOOM_RATIO] = zoomRatio
        }

        assertThat(
                listOf(
                        Request(
                            streams = emptyList(),
                            parameters = parameters,
                            template = RequestTemplate(template),
                        )
                    )
                    .shouldStopRepeatingBeforeCapture(
                        cameraId = cameraId,
                        hasMultipleRepeatingStreams = hasMultipleRepeatingStreams,
                    )
            )
            .isEqualTo(stopRepeatingExpected)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name =
                "brand={0}, model={1}, device={2}, aeMode={3}, template={4}, cameraId={5}," +
                    " multiRepeating={6}, zoomed={7}, zoomRatio={9}"
        )
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        "a71xq",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        "a71xq",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        true,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        "a71xq",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        true,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        "a71xq",
                        CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        true,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        "a71xq",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_PREVIEW,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )

                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        "a71xq",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        "a71xq",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        true,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        "a71xq",
                        CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        true,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        "a71xq",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_PREVIEW,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )

                // Snapdragon Note10+ (d2q / SM-N975U1): only front camera ("1") with multiple
                // repeating streams and digital zoom stops repeating.
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        true,
                        true,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        false,
                        true,
                        2.0f,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        false,
                        false,
                        1.0f,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        false,
                        true,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "0",
                        true,
                        true,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        "0",
                        false,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975U1",
                        "d2q",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_PREVIEW,
                        "1",
                        true,
                        true,
                        false,
                        null,
                    )
                )
                // Exynos Note10+ (d2s / SM-N975F) does not use Qualcomm CamX and is unaffected.
                add(
                    arrayOf(
                        "Samsung",
                        "SM-N975F",
                        "d2s",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        "1",
                        true,
                        true,
                        false,
                        null,
                    )
                )

                add(
                    arrayOf(
                        "Google",
                        "Pixel 2",
                        "walleye",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Moto",
                        "G3",
                        "osprey",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A722",
                        "a72q",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        null,
                        false,
                        false,
                        false,
                        null,
                    )
                )
            }
    }
}
