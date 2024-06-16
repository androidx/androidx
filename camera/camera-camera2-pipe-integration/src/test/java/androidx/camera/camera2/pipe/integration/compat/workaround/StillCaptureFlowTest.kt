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

package androidx.camera.camera2.pipe.integration.compat.workaround

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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class StillCaptureFlowTest(
    private val brand: String,
    private val model: String,
    private val aeMode: Int,
    private val template: Int,
    private val stopRepeatingExpected: Boolean,
) {
    @Test
    fun shouldStopRepeating() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)

        assertThat(
                listOf(
                        Request(
                            streams = emptyList(),
                            parameters = mapOf(CaptureRequest.CONTROL_AE_MODE to aeMode),
                            template = RequestTemplate(template),
                        )
                    )
                    .shouldStopRepeatingBeforeCapture()
            )
            .isEqualTo(stopRepeatingExpected)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "brand={0}, model={1}, aeMode={2}, template={3}"
        )
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716B",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_PREVIEW,
                        false
                    )
                )

                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        CONTROL_AE_MODE_ON,
                        TEMPLATE_STILL_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A716U",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_PREVIEW,
                        false
                    )
                )

                add(
                    arrayOf(
                        "Google",
                        "Pixel 2",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "Moto",
                        "G3",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "Samsung",
                        "SM-A722",
                        CONTROL_AE_MODE_ON_AUTO_FLASH,
                        TEMPLATE_STILL_CAPTURE,
                        false
                    )
                )
            }
    }
}
