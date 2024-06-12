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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraDevice.TEMPLATE_MANUAL
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
import android.hardware.camera2.CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureIntentPreviewQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailedForVideoSnapshotQuirk
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.Quirk
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

/** Unit test for [TemplateParamsOverride]. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
class TemplateParamsOverrideTest(
    private val testName: String,
    private val quirk: Quirk?,
    private val expectedParamsMap: Map<Int, Map<CaptureRequest.Key<*>, Any>>,
) {
    companion object {
        private val emptyParamsMap = emptyMap<CaptureRequest.Key<*>, Any>()
        private val paramsMapForCaptureIntentPreviewQuirk =
            mapOf(
                TEMPLATE_RECORD to mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_PREVIEW)
            )
        private val paramsMapForImageCaptureFailedForVideoSnapshot =
            mapOf(
                TEMPLATE_VIDEO_SNAPSHOT to
                    mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "testName={0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                add(
                    arrayOf(
                        "no quirk",
                        null,
                        emptyParamsMap,
                    )
                )
                add(
                    arrayOf(
                        "CaptureIntentPreviewQuirk with false workaround flag",
                        object : CaptureIntentPreviewQuirk {
                            override fun workaroundByCaptureIntentPreview() = false
                        },
                        emptyParamsMap,
                    )
                )
                add(
                    arrayOf(
                        "CaptureIntentPreviewQuirk with true workaround flag",
                        object : CaptureIntentPreviewQuirk {
                            override fun workaroundByCaptureIntentPreview() = true
                        },
                        paramsMapForCaptureIntentPreviewQuirk,
                    )
                )
                add(
                    arrayOf(
                        "ImageCaptureFailedForVideoSnapshot",
                        ImageCaptureFailedForVideoSnapshotQuirk(),
                        paramsMapForImageCaptureFailedForVideoSnapshot,
                    )
                )
            }
    }

    private lateinit var cameraQuirks: CameraQuirks

    @Before
    fun setup() {
        cameraQuirks =
            CameraQuirks(
                FakeCameraMetadata(),
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        FakeCameraMetadata(),
                        StreamConfigurationMapBuilder.newBuilder().build()
                    )
                )
            )
        if (quirk != null) {
            cameraQuirks.quirks.addQuirkForTesting(quirk)
        }
    }

    @Test
    fun getOverrideParams() {
        for (template in
            listOf(
                TEMPLATE_PREVIEW,
                TEMPLATE_RECORD,
                TEMPLATE_STILL_CAPTURE,
                TEMPLATE_MANUAL,
                TEMPLATE_VIDEO_SNAPSHOT,
                TEMPLATE_ZERO_SHUTTER_LAG
            )) {
            val templateParamsOverride =
                TemplateParamsOverride_Bindings_Companion_ProvideTemplateParamsOverrideFactory
                    .provideTemplateParamsOverride(cameraQuirks)
            val params = templateParamsOverride.getOverrideParams(RequestTemplate(template))
            val expectedParams = expectedParamsMap[template] ?: emptyParamsMap
            assertWithMessage("getOverrideParams with template: $template")
                .that(params)
                .isEqualTo(expectedParams)
        }
    }
}
