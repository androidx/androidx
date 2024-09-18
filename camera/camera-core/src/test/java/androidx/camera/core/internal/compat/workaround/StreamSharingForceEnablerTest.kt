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

package androidx.camera.core.internal.compat.workaround

import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

private const val PREVIEW = 0x1
private const val IMAGE_CAPTURE = 0x2
private const val VIDEO_CAPTURE = 0x4
private const val IMAGE_ANALYSIS = 0x8

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamSharingForceEnablerTest(
    private val brand: String,
    private val model: String,
    private val cameraId: String,
    private val useCaseCombination: Int,
    private val shouldEnableStreamSharing: Boolean
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "brand={0}, model={1}, cameraId={2}, useCases={3}, result={4}"
        )
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                add(
                    arrayOf(
                        "OnePlus",
                        "cph2583",
                        "0",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "OnePlus",
                        "cph2583",
                        "1",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "OnePlus",
                        "cph2583",
                        "1",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE or IMAGE_ANALYSIS,
                        false
                    )
                )
                add(arrayOf("", "", "1", PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE, false))
                add(arrayOf("Motorola", "Moto E20", "0", PREVIEW or VIDEO_CAPTURE, true))
                add(arrayOf("Motorola", "Moto E20", "1", PREVIEW or VIDEO_CAPTURE, false))
                add(
                    arrayOf(
                        "Motorola",
                        "Moto E20",
                        "0",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        false
                    )
                )
                add(
                    arrayOf(
                        "Google",
                        "Pixel 4a",
                        "1",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Google",
                        "Pixel 5",
                        "1",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        true
                    )
                )
                add(
                    arrayOf(
                        "Google",
                        "Pixel 4a",
                        "1",
                        PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                        true
                    )
                )
                add(arrayOf("Google", "Pixel 5a", "1", PREVIEW or IMAGE_CAPTURE, false))
            }
    }

    @Test
    fun shouldForceEnableStreamSharing() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)

        assertThat(
                StreamSharingForceEnabler()
                    .shouldForceEnableStreamSharing(cameraId, createUseCases(useCaseCombination))
            )
            .isEqualTo(shouldEnableStreamSharing)
    }

    private fun createUseCases(useCaseCombination: Int): Collection<UseCase> {
        val useCases = mutableListOf<UseCase>()

        if (useCaseCombination and PREVIEW != 0) {
            useCases.add(Preview.Builder().build())
        }
        if (useCaseCombination and IMAGE_CAPTURE != 0) {
            useCases.add(ImageCapture.Builder().build())
        }
        if (useCaseCombination and VIDEO_CAPTURE != 0) {
            useCases.add(
                FakeUseCaseConfig.Builder()
                    .setCaptureType(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
                    .build()
            )
        }
        if (useCaseCombination and IMAGE_ANALYSIS != 0) {
            useCases.add(ImageAnalysis.Builder().build())
        }

        return useCases
    }
}
