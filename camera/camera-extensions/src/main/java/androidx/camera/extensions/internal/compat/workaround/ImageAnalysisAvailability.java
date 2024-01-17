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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;
import androidx.camera.extensions.internal.compat.quirk.ImageAnalysisUnavailableQuirk;

/**
 * Workaround to check whether {@link ImageAnalysis} can be bound together with {@link Preview} and
 * {@link ImageCapture} when enabling extensions.
 *
 * @see ImageAnalysisUnavailableQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageAnalysisAvailability {
    ImageAnalysisUnavailableQuirk mImageAnalysisUnavailableQuirk =
            DeviceQuirks.get(ImageAnalysisUnavailableQuirk.class);

    /**
     * Returns whether {@link ImageAnalysis} is available to be bound together with
     * {@link Preview} and {@link ImageCapture} for the specified camera id and extensions mode.
     *
     * @param cameraId the camera id to query
     * @param mode the extensions mode to query
     * @return {@code true} if {@link ImageAnalysis} is available. Otherwise, returns {@code
     * false}.
     */
    public boolean isAvailable(@NonNull String cameraId, @ExtensionMode.Mode int mode) {
        if (mImageAnalysisUnavailableQuirk != null
                && mImageAnalysisUnavailableQuirk.isUnavailable(cameraId, mode)) {
            return false;
        }
        return true;
    }
}
