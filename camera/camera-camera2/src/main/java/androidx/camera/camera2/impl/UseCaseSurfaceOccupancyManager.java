/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;

import java.util.List;

/**
 * Collect the use case surface occupancy customization rules in this class to make
 * Camera2DeviceSurfaceManager independent from use case type.
 */
final class UseCaseSurfaceOccupancyManager {
    private UseCaseSurfaceOccupancyManager() {
    }

    static void checkUseCaseLimitNotExceeded(
            List<UseCase> originalUseCases, List<UseCase> newUseCases) {
        int imageCaptureCount = 0;
        int videoCaptureCount = 0;

        if (newUseCases == null || newUseCases.isEmpty()) {
            throw new IllegalArgumentException("No new use cases to be bound.");
        }

        if (originalUseCases != null) {
            for (UseCase useCase : originalUseCases) {
                if (useCase instanceof ImageCapture) {
                    imageCaptureCount++;
                } else if (useCase instanceof VideoCapture) {
                    videoCaptureCount++;
                }
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
            throw new IllegalArgumentException(
                    "Exceeded max simultaneously bound image capture use cases.");
        }

        if (videoCaptureCount > 1) {
            throw new IllegalArgumentException(
                    "Exceeded max simultaneously bound video capture use cases.");
        }
    }
}
