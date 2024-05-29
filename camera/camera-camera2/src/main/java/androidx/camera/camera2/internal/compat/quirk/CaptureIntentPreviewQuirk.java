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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;

/**
 * A Quirk interface denotes devices have specific issue and can be workaround by using
 * {@link CaptureRequest#CONTROL_CAPTURE_INTENT_PREVIEW} to replace
 * {@link CaptureRequest#CONTROL_CAPTURE_INTENT_VIDEO_RECORD}.
 *
 * <p>Subclasses of this quirk may contain device specific information.
 */
public interface CaptureIntentPreviewQuirk extends Quirk {
    /**
     * Returns if the device specific issue can be workaround by using
     * {@link CaptureRequest#CONTROL_CAPTURE_INTENT_PREVIEW} to replace
     * {@link CaptureRequest#CONTROL_CAPTURE_INTENT_VIDEO_RECORD}.
     */
    default boolean workaroundByCaptureIntentPreview() {
        return true;
    }

    /**
     * Returns if input quirks contains at least one {@link CaptureIntentPreviewQuirk} which
     * {@link CaptureIntentPreviewQuirk#workaroundByCaptureIntentPreview()} is true.
     */
    static boolean workaroundByCaptureIntentPreview(@NonNull Quirks quirks) {
        for (CaptureIntentPreviewQuirk quirk : quirks.getAll(CaptureIntentPreviewQuirk.class)) {
            if (quirk.workaroundByCaptureIntentPreview()) {
                return true;
            }
        }
        return false;
    }
}
