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
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.RestrictedCameraInfo;
import androidx.camera.core.impl.SessionProcessor;
import androidx.core.util.Preconditions;

/**
 * Utility methods for operating on {@link CameraExtensionsInfo} instances.
 */
class CameraExtensionsInfos {
    private static final CameraExtensionsInfo NORMAL_MODE_CAMERA_EXTENSIONS_INFO =
            new CameraExtensionsInfo() {
            };

    /**
     * Returns a {@link CameraExtensionsInfo} instance converted from a {@link CameraInfo} object.
     */
    @NonNull
    static CameraExtensionsInfo from(@NonNull CameraInfo cameraInfo) {
        Preconditions.checkArgument(cameraInfo instanceof RestrictedCameraInfo, "The input camera"
                + " info must be an instance retrieved from the camera that is returned "
                + "by invoking CameraProvider#bindToLifecycle() with an extension enabled camera "
                + "selector.");
        SessionProcessor sessionProcessor =
                ((RestrictedCameraInfo) cameraInfo).getSessionProcessor();
        if (sessionProcessor instanceof CameraExtensionsInfo) {
            return (CameraExtensionsInfo) sessionProcessor;
        } else {
            return NORMAL_MODE_CAMERA_EXTENSIONS_INFO;
        }
    }

    private CameraExtensionsInfos() {
    }
}
