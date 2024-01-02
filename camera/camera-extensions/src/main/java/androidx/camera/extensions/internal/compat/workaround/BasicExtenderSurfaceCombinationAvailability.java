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

import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;
import androidx.camera.extensions.internal.compat.quirk.ExtraSupportedSurfaceCombinationsQuirk;

/**
 * Workaround to check whether {@link ImageAnalysis} can be bound together with {@link Preview} and
 * {@link ImageCapture} when enabling extensions.
 *
 * <p>This is used by the BasicVendorExtender to check whether the device can support to bind the
 * additional ImageAnalysis UseCase.
 *
 * @see ExtraSupportedSurfaceCombinationsQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class BasicExtenderSurfaceCombinationAvailability {
    ExtraSupportedSurfaceCombinationsQuirk mExtraSupportedSurfaceCombinationsQuirk =
            DeviceQuirks.get(ExtraSupportedSurfaceCombinationsQuirk.class);

    /**
     * Returns whether {@link ImageAnalysis} is available to be bound together with
     * {@link Preview} and {@link ImageCapture} for the specified camera id and extensions mode.
     *
     * @param hardwareLevel the camera device hardware level
     * @param hasPreviewProcessor whether PreviewExtenderImpl has processor
     * @param hasImageCaptureProcessor whether ImageCaptureExtenderImpl has processor
     * @return {@code true} if {@link ImageAnalysis} is available. Otherwise, returns {@code
     * false}.
     */
    public boolean isImageAnalysisAvailable(int hardwareLevel,
            boolean hasPreviewProcessor,
            boolean hasImageCaptureProcessor) {
        // No matter what the device hardware is and no matter the Preview and the ImageCapture
        // have processor or not, once ExtraSupportedSurfaceCombinationsQuirk can be loaded, the
        // device can support to bind ImageAnalysis when enabling the extensions mode.
        if (mExtraSupportedSurfaceCombinationsQuirk != null) {
            return true;
        }

        if (!hasPreviewProcessor && !hasImageCaptureProcessor) {
            // Required configuration: PRIV + JPEG + YUV
            // Required HW level: any
            return true;
        } else if (hasPreviewProcessor && !hasImageCaptureProcessor) {
            // Required configuration: YUV + JPEG + YUV
            // Required HW level: LIMITED level or above
            return hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
                    || hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                    || hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_3;
        } else {
            // Required configuration: PRIV + YUV + YUV or YUV + YUV + YUV
            // Required HW level: FULL level or above
            return hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                    || hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_3;
        }
    }
}
