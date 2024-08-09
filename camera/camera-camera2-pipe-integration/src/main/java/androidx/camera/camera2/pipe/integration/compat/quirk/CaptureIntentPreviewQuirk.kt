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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.hardware.camera2.CaptureRequest
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.Quirks

/**
 * A Quirk interface denotes devices have specific issue and can be workaround by using
 * [CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW] to replace
 * [CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD].
 * - Subclasses of this quirk may contain device specific information.
 */
public interface CaptureIntentPreviewQuirk : Quirk {
    /**
     * Returns if the device specific issue can be workaround by using
     * [CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW] to replace
     * [CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD].
     */
    public fun workaroundByCaptureIntentPreview(): Boolean = true

    public companion object {
        /**
         * Returns if input quirks contains at least one [CaptureIntentPreviewQuirk] which
         * [CaptureIntentPreviewQuirk.workaroundByCaptureIntentPreview] is true.
         */
        public fun workaroundByCaptureIntentPreview(quirks: Quirks): Boolean {
            for (quirk in quirks.getAll(CaptureIntentPreviewQuirk::class.java)) {
                if (quirk.workaroundByCaptureIntentPreview()) {
                    return true
                }
            }
            return false
        }
    }
}
