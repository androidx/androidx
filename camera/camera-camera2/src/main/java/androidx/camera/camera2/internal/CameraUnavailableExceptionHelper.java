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

package androidx.camera.camera2.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.core.CameraUnavailableException;

/**
 * Helper class to create a {@link CameraUnavailableException}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraUnavailableExceptionHelper {

    private CameraUnavailableExceptionHelper() {}

    /**
     * Create {@link CameraUnavailableException} from {@link CameraAccessExceptionCompat}.
     *
     * @param e the {@link CameraAccessExceptionCompat}.
     * @return the {@link CameraUnavailableException}.
     */
    @NonNull
    public static CameraUnavailableException createFrom(@NonNull CameraAccessExceptionCompat e) {
        int errorCode;
        switch (e.getReason()) {
            case CameraAccessExceptionCompat.CAMERA_DISABLED:
                errorCode = CameraUnavailableException.CAMERA_DISABLED;
                break;
            case CameraAccessExceptionCompat.CAMERA_DISCONNECTED:
                errorCode = CameraUnavailableException.CAMERA_DISCONNECTED;
                break;
            case CameraAccessExceptionCompat.CAMERA_ERROR:
                errorCode = CameraUnavailableException.CAMERA_ERROR;
                break;
            case CameraAccessExceptionCompat.CAMERA_IN_USE:
                errorCode = CameraUnavailableException.CAMERA_IN_USE;
                break;
            case CameraAccessExceptionCompat.MAX_CAMERAS_IN_USE:
                errorCode = CameraUnavailableException.CAMERA_MAX_IN_USE;
                break;
            case CameraAccessExceptionCompat.CAMERA_UNAVAILABLE_DO_NOT_DISTURB:
                errorCode = CameraUnavailableException.CAMERA_UNAVAILABLE_DO_NOT_DISTURB;
                break;
            default:
                errorCode = CameraUnavailableException.CAMERA_UNKNOWN_ERROR;
        }
        return new CameraUnavailableException(errorCode, e);
    }
}
