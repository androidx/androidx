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

package androidx.camera.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.RestrictedCameraControl;
import androidx.camera.core.impl.SessionProcessor;
import androidx.core.util.Preconditions;

/**
 * Utility methods for operating on {@link CameraExtensionsControl} instances.
 */
class CameraExtensionsControls {

    /**
     * Returns a {@link CameraExtensionsControl} instance converted from a {@link CameraControl}
     * object when the {@link CameraControl} is retrieved from a extensions-enabled camera.
     * Otherwise, returns {@code null}.
     */
    @Nullable
    static CameraExtensionsControl from(@NonNull CameraControl cameraControl) {
        Preconditions.checkArgument(cameraControl instanceof RestrictedCameraControl, "The input "
                + "camera control must be an instance retrieved from the camera that is returned "
                + "by invoking CameraProvider#bindToLifecycle() with an extension enabled camera "
                + "selector.");

        SessionProcessor sessionProcessor =
                ((RestrictedCameraControl) cameraControl).getSessionProcessor();
        if (sessionProcessor instanceof CameraExtensionsControl) {
            return (CameraExtensionsControl) sessionProcessor;
        } else {
            return null;
        }
    }

    private CameraExtensionsControls() {
    }
}
