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

package androidx.camera.camera2.internal.compat.workaround;

import static android.hardware.camera2.CameraDevice.TEMPLATE_RECORD;
import static android.hardware.camera2.CameraDevice.TEMPLATE_VIDEO_SNAPSHOT;
import static android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW;
import static android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE;
import static android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT;

import static androidx.camera.camera2.internal.compat.quirk.CaptureIntentPreviewQuirk.workaroundByCaptureIntentPreview;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFailedForVideoSnapshotQuirk;
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFailedWhenVideoCaptureIsBoundQuirk;
import androidx.camera.camera2.internal.compat.quirk.PreviewDelayWhenVideoCaptureIsBoundQuirk;
import androidx.camera.camera2.internal.compat.quirk.PreviewStretchWhenVideoCaptureIsBoundQuirk;
import androidx.camera.camera2.internal.compat.quirk.TemporalNoiseQuirk;
import androidx.camera.core.impl.Quirks;

import java.util.HashMap;
import java.util.Map;

/**
 * Workaround to get those capture parameters used to override the template default parameters.
 *
 * <p>This workaround should only be applied on repeating request but not on single request.
 *
 * @see PreviewStretchWhenVideoCaptureIsBoundQuirk
 * @see PreviewDelayWhenVideoCaptureIsBoundQuirk
 * @see ImageCaptureFailedWhenVideoCaptureIsBoundQuirk
 * @see TemporalNoiseQuirk
 * @see ImageCaptureFailedForVideoSnapshotQuirk
 */
public class TemplateParamsOverride {
    private final boolean mWorkaroundByCaptureIntentPreview;
    private final boolean mWorkaroundByCaptureIntentStillCapture;

    public TemplateParamsOverride(@NonNull Quirks quirks) {
        mWorkaroundByCaptureIntentPreview = workaroundByCaptureIntentPreview(quirks);
        mWorkaroundByCaptureIntentStillCapture = quirks.contains(
                ImageCaptureFailedForVideoSnapshotQuirk.class);
    }

    /**
     * Returns capture parameters used to override the default parameters of the input template.
     */
    @NonNull
    public Map<CaptureRequest.Key<?>, Object> getOverrideParams(int template) {
        if (template == TEMPLATE_RECORD && mWorkaroundByCaptureIntentPreview) {
            Map<CaptureRequest.Key<?>, Object> params = new HashMap<>();
            params.put(CONTROL_CAPTURE_INTENT, CONTROL_CAPTURE_INTENT_PREVIEW);
            return unmodifiableMap(params);
        } else if (template == TEMPLATE_VIDEO_SNAPSHOT && mWorkaroundByCaptureIntentStillCapture) {
            Map<CaptureRequest.Key<?>, Object> params = new HashMap<>();
            params.put(CONTROL_CAPTURE_INTENT, CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            return unmodifiableMap(params);
        }
        return emptyMap();
    }
}
