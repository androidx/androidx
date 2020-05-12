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

package androidx.camera.view.preview.transform;

import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;

final class RotationTransform {
    // Queries rotation value from view automatically.
    static final int ROTATION_AUTOMATIC = -1;

    private RotationTransform() {
    }

    /**
     * Computes the rotation in degrees from its natural orientation.
     *
     * <p>When a application is running in remote display, the correct rotation degrees should
     * be obtained from the device rotation. The rotation from the {@link View} may cause incorrect
     * transform calculation results.
     *
     * @param view The {@link View} used to show the preview.
     * @param deviceRotation The device rotation value provided by the developers.
     * @return The rotation in degrees from its natural orientation.
     */
    static float getRotationDegrees(@NonNull final View view, final int deviceRotation) {
        if (deviceRotation != ROTATION_AUTOMATIC) {
            return SurfaceRotation.rotationDegreesFromSurfaceRotation(deviceRotation);
        } else {
            return getRotationDegrees(view);
        }
    }

    /** Computes the rotation of a {@link View} in degrees from its natural orientation. */
    static float getRotationDegrees(@NonNull final View view) {
        final Display display = view.getDisplay();
        if (display == null) {
            return 0;
        }
        final int rotation = display.getRotation();
        return SurfaceRotation.rotationDegreesFromSurfaceRotation(rotation);
    }
}
