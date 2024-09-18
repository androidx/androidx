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

package androidx.camera.core.internal.compat.quirk;

import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.UseCaseConfigFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 359062845, 259335612
 *     Description: Quirk required to check whether still image capture can run failed when a
 *     specific combination of UseCases are bound together.
 *     Device(s): OnePlus 12, Pixel 4a, 5, 5a
 */
public final class ImageCaptureFailedForSpecificCombinationQuirk implements Quirk {
    private static final Set<String> PIXEL_MODELS = new HashSet<>(Arrays.asList(
            "pixel 4a", // Pixel 4a
            "pixel 4a (5g)", // Pixel 4a (5G)
            "pixel 5", // Pixel 5
            "pixel 5a" // Pixel 5a
    ));

    static boolean load() {
        return isOnePlus12() || isPixelProblematicDevice();
    }

    private static boolean isOnePlus12() {
        return "oneplus".equalsIgnoreCase(Build.BRAND) && "cph2583".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isPixelProblematicDevice() {
        return "google".equalsIgnoreCase(Build.BRAND) && PIXEL_MODELS.contains(
                Build.MODEL.toLowerCase());
    }

    /**
     *  Returns whether stream sharing should be forced enabled for specific camera and UseCase
     *  combination.
     */
    public boolean shouldForceEnableStreamSharing(@NonNull String cameraId,
            @NonNull Collection<UseCase> appUseCases) {
        if (isOnePlus12()) {
            return shouldForceEnableStreamSharingForOnePlus12(cameraId, appUseCases);
        } else if (isPixelProblematicDevice()) {
            return shouldForceEnableStreamSharingForPixelDevice(cameraId, appUseCases);
        }
        return false;
    }

    /**
     * On OnePlus 12, still image capture run failed on the front camera only when the UseCase
     * combination is exactly Preview + VideoCapture + ImageCapture.
     */
    private boolean shouldForceEnableStreamSharingForOnePlus12(@NonNull String cameraId,
            @NonNull Collection<UseCase> appUseCases) {
        return cameraId.equals("1") && isVideoCapturePreviewImageCaptureCombination(appUseCases);
    }

    /**
     * On Pixel 4a, 5 and 5a device, still image capture run failed on the front camera only when
     * the UseCase combination is exactly Preview + VideoCapture + ImageCapture.
     */
    private boolean shouldForceEnableStreamSharingForPixelDevice(@NonNull String cameraId,
            @NonNull Collection<UseCase> appUseCases) {
        return cameraId.equals("1") && isVideoCapturePreviewImageCaptureCombination(appUseCases);
    }

    private boolean isVideoCapturePreviewImageCaptureCombination(
            @NonNull Collection<UseCase> appUseCases) {
        if (appUseCases.size() != 3) {
            return false;
        }

        boolean hasPreview = false;
        boolean hasVideoCapture = false;
        boolean hasImageCapture = false;

        for (UseCase useCase : appUseCases) {
            if (useCase instanceof Preview) {
                hasPreview = true;
            } else if (useCase instanceof ImageCapture) {
                hasImageCapture = true;
            } else {
                if (useCase.getCurrentConfig().containsOption(OPTION_CAPTURE_TYPE)) {
                    hasVideoCapture = useCase.getCurrentConfig().getCaptureType()
                            == UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE;
                }
            }
        }

        return hasPreview && hasVideoCapture && hasImageCapture;
    }
}
