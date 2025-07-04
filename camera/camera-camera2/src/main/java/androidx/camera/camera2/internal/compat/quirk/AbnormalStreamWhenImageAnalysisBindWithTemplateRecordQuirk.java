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

package androidx.camera.camera2.internal.compat.quirk;

import android.os.Build;

/**
 * <p>QuirkSummary
 *     Bug Id: b/395822788, b/409478042
 *     Description: Quirk denotes that the camera produces only the first few frames when using
 *                  ImageAnalysis with another stream using TEMPLATE_RECORD (usually a VideoCapture
 *                  stream or a stream shared between Preview and VideoCapture). As a result, when
 *                  binding "Preview + VideoCapture + ImageAnalysis" and enabling StreamSharing, the
 *                  preview freezes after rendering a few frames.
 *     Device(s): Samsung Galaxy F55, M55
 */
public class AbnormalStreamWhenImageAnalysisBindWithTemplateRecordQuirk implements
        CaptureIntentPreviewQuirk {

    static boolean load() {
        return isSamsungM55();
    }

    public static boolean isSamsungM55() {
        // Samsung Galaxy F55 and M55 share the same device name.
        return "samsung".equalsIgnoreCase(Build.BRAND) && Build.DEVICE.equalsIgnoreCase("m55xq");
    }

    @Override
    public boolean workaroundByCaptureIntentPreview() {
        return isSamsungM55();
    }
}
