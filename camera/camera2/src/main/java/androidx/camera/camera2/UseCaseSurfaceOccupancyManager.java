/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import androidx.camera.core.BaseUseCase;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.VideoCaptureUseCase;
import java.util.List;

/**
 * Collect the use case surface occupancy customization rules in this class to make
 * Camera2DeviceSurfaceManager independent from use case type.
 */
final class UseCaseSurfaceOccupancyManager {
  static void checkUseCaseLimitNotExceeded(
      List<BaseUseCase> originalUseCases, List<BaseUseCase> newUseCases) {
    int imageCaptureUseCaseCount = 0;
    int videoCaptureUseCaseCount = 0;

    if (newUseCases == null || newUseCases.isEmpty()) {
      throw new IllegalArgumentException("No new use cases to be bound.");
    }

    if (originalUseCases != null) {
      for (BaseUseCase useCase : originalUseCases) {
        if (useCase instanceof ImageCaptureUseCase) {
          imageCaptureUseCaseCount++;
        } else if (useCase instanceof VideoCaptureUseCase) {
          videoCaptureUseCaseCount++;
        }
      }
    }

    for (BaseUseCase useCase : newUseCases) {
      if (useCase instanceof ImageCaptureUseCase) {
        imageCaptureUseCaseCount++;
      } else if (useCase instanceof VideoCaptureUseCase) {
        videoCaptureUseCaseCount++;
      }
    }

    if (imageCaptureUseCaseCount > 1) {
      throw new IllegalArgumentException(
          "Exceeded max simultaneously bound image capture use cases.");
    }

    if (videoCaptureUseCaseCount > 1) {
      throw new IllegalArgumentException(
          "Exceeded max simultaneously bound video capture use cases.");
    }
  }

  private UseCaseSurfaceOccupancyManager() {}
}
