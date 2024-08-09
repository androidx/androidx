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

import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureIntentPreviewQuirk.Companion.workaroundByCaptureIntentPreview
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailedForVideoSnapshotQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailedWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewDelayWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewStretchWhenVideoCaptureIsBoundQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.TemporalNoiseQuirk
import androidx.camera.core.impl.Quirks
import dagger.Module
import dagger.Provides

/**
 * Workaround to get those capture parameters used to override the template default parameters.
 *
 * @see PreviewStretchWhenVideoCaptureIsBoundQuirk
 * @see PreviewDelayWhenVideoCaptureIsBoundQuirk
 * @see ImageCaptureFailedWhenVideoCaptureIsBoundQuirk
 * @see TemporalNoiseQuirk
 * @see ImageCaptureFailedForVideoSnapshotQuirk
 */
public interface TemplateParamsOverride {
    /** Returns capture parameters used to override the default parameters of the input template. */
    public fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any>

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideTemplateParamsOverride(
                cameraQuirks: CameraQuirks
            ): TemplateParamsOverride {
                val quirks = cameraQuirks.quirks
                return if (
                    workaroundByCaptureIntentPreview(quirks) ||
                        quirks.contains(ImageCaptureFailedForVideoSnapshotQuirk::class.java)
                )
                    TemplateParamsQuirkOverride(quirks)
                else NoOpTemplateParamsOverride
            }
        }
    }
}

public class TemplateParamsQuirkOverride(quirks: Quirks) : TemplateParamsOverride {
    private val workaroundByCaptureIntentPreview = workaroundByCaptureIntentPreview(quirks)
    private val workaroundByCaptureIntentStillCapture =
        quirks.contains(ImageCaptureFailedForVideoSnapshotQuirk::class.java)

    override fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any> {
        if (template?.value == TEMPLATE_RECORD && workaroundByCaptureIntentPreview) {
            return mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_PREVIEW)
        }
        if (template?.value == TEMPLATE_VIDEO_SNAPSHOT && workaroundByCaptureIntentStillCapture) {
            return mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
        }
        return emptyMap()
    }
}

public object NoOpTemplateParamsOverride : TemplateParamsOverride {
    override fun getOverrideParams(template: RequestTemplate?): Map<CaptureRequest.Key<*>, Any> {
        return emptyMap()
    }
}
