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

package androidx.camera.extensions.internal.compat.workaround;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;
import androidx.camera.extensions.internal.compat.quirk.GetAvailableKeysNeedsOnInit;

import java.util.List;

/**
 * A workaround for getting the available CaptureRequest keys safely.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AvailableKeysRetriever {
    boolean mShouldInvokeOnInit;

    /**
     * Default constructor.
     */
    public AvailableKeysRetriever() {
        mShouldInvokeOnInit = DeviceQuirks.get(GetAvailableKeysNeedsOnInit.class) != null;
    }

    /**
     * Get available CaptureRequest keys from the given {@link ImageCaptureExtenderImpl}. The
     * cameraId, cameraCharacteristics and the context is needed for invoking onInit whenever
     * necessary.
     */
    @NonNull
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys(
            @NonNull ImageCaptureExtenderImpl imageCaptureExtender,
            @NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
        if (mShouldInvokeOnInit) {
            imageCaptureExtender.onInit(cameraId, cameraCharacteristics, context);
        }

        try {
            return imageCaptureExtender.getAvailableCaptureRequestKeys();
        } finally {
            if (mShouldInvokeOnInit) {
                imageCaptureExtender.onDeInit();
            }
        }
    }
}
