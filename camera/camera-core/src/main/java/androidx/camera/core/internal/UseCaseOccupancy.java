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

package androidx.camera.core.internal;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;

import java.util.List;

/**
 * Checks to see if the number of specific {@link UseCase} exceeds the supported number.
 */
public final class UseCaseOccupancy {
    private static final String TAG = "UseCaseOccupancy";
    private UseCaseOccupancy() {
    }

    /**
     * Check to see if CameraX supports running the set of use cases.
     *
     * @param originalUseCases  the currently existing use cases
     * @param newUseCases       the use cases to be added
     * @return true if the set of use cases is supported, otherwise false
     */
    public static boolean checkUseCaseLimitNotExceeded(
            @NonNull List<UseCase> originalUseCases,
            @NonNull List<UseCase> newUseCases) {
        int imageCaptureCount = 0;
        int videoCaptureCount = 0;

        for (UseCase useCase : originalUseCases) {
            if (useCase instanceof ImageCapture) {
                imageCaptureCount++;
            } else if (useCase instanceof VideoCapture) {
                videoCaptureCount++;
            }
        }

        for (UseCase useCase : newUseCases) {
            if (useCase instanceof ImageCapture) {
                imageCaptureCount++;
            } else if (useCase instanceof VideoCapture) {
                videoCaptureCount++;
            }
        }

        if (imageCaptureCount > 1) {
            Log.e(TAG, "Exceeded max simultaneously bound image capture use cases.");
            return false;
        }

        if (videoCaptureCount > 1) {
            Log.e(TAG, "Exceeded max simultaneously bound video capture use cases.");
            return false;
        }

        return true;
    }
}
