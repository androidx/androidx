/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal.utils;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.CameraOrientationUtil;

/**
 * Contains utility methods related to UseCaseConfig.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class UseCaseConfigUtil {
    private UseCaseConfigUtil() {}

    /**
     * Updates target rotation together with related orientation-dependent configs.
     *
     * @param builder The builder that target rotation needs to be updated.
     * @param newRotation The new target rotation of the output image, expressed as one of
     * {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180}, or
     * {@link Surface#ROTATION_270}.
     */
    public static void updateTargetRotationAndRelatedConfigs(
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder, int newRotation) {
        ImageOutputConfig config = (ImageOutputConfig) builder.getUseCaseConfig();
        int oldRotation = config.getTargetRotation(ImageOutputConfig.INVALID_ROTATION);

        if (oldRotation == ImageOutputConfig.INVALID_ROTATION || oldRotation != newRotation) {
            ((ImageOutputConfig.Builder<?>) builder).setTargetRotation(newRotation);
        }

        if (oldRotation == ImageOutputConfig.INVALID_ROTATION
                || newRotation == ImageOutputConfig.INVALID_ROTATION
                || oldRotation == newRotation) {
            return;
        }

        int oldRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(oldRotation);
        int newRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(newRotation);

        // When the target rotation is changed either from portrait to landscape or from
        // landscape, the target resolution or crop aspect ratio values need to be updated to
        // match the new target rotation value.
        //
        // For the target resolution, the width and height of original setting value will be
        // swapped then set back. The target resolution value is orientation-dependent that will
        // be used by auto-resolution mechanism to find the nearest boxing size if anyone exists.
        if ((Math.abs(newRotationDegrees - oldRotationDegrees) % 180) == 90) {
            Size targetResolution = config.getTargetResolution(null);

            if (targetResolution != null) {
                // If there is target resolution value set before, updating it and then crop aspect
                // ratio value will also be updated together.
                ((ImageOutputConfig.Builder<?>) builder).setTargetResolution(
                        new Size(/* width=*/targetResolution.getHeight(), /* height= */
                                targetResolution.getWidth()));
            }
        }
    }
}
