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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureIntentPreviewQuirk.Companion.workaroundByCaptureIntentPreview
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailedWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewDelayWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewStretchWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.TemporalNoiseQuirk
import dagger.Module
import dagger.Provides

/**
 * Workaround to get those capture parameters used to override the template default parameters.
 * - This workaround should only be applied on repeating request but not on single request.
 *
 * @see PreviewStretchWhenVideoCaptureIsBoundQuirk
 * @see PreviewDelayWhenVideoCaptureIsBoundQuirk
 * @see ImageCaptureFailedWhenVideoCaptureIsBoundQuirk
 * @see TemporalNoiseQuirk
 */
interface TemplateParamsOverride {
    /** Returns capture parameters used to override the default parameters of the input template. */
    fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any>

    @Module
    abstract class Bindings {
        companion object {
            @Provides
            fun provideTemplateParamsOverride(quirks: CameraQuirks): TemplateParamsOverride {
                return if (workaroundByCaptureIntentPreview(quirks.quirks))
                    TemplateParamsQuirkOverride
                else NoOpTemplateParamsOverride
            }
        }
    }
}

object TemplateParamsQuirkOverride : TemplateParamsOverride {
    override fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any> {
        if (template?.value == CameraDevice.TEMPLATE_RECORD) {
            return mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_PREVIEW)
        }
        return emptyMap()
    }
}

object NoOpTemplateParamsOverride : TemplateParamsOverride {
    override fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any> {
        return emptyMap()
    }
}
