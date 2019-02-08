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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraCaptureMetaData.AeState;
import androidx.camera.core.CameraCaptureMetaData.AfMode;
import androidx.camera.core.CameraCaptureMetaData.AfState;
import androidx.camera.core.CameraCaptureMetaData.AwbState;
import androidx.camera.core.CameraCaptureMetaData.FlashState;

/**
 * The result of a single image capture.
 *
 * @hide
 */
public interface CameraCaptureResult {

  @NonNull
  AfMode getAfMode();

  @NonNull
  AfState getAfState();

  @NonNull
  AeState getAeState();

  @NonNull
  AwbState getAwbState();

  @NonNull
  FlashState getFlashState();

  /**
   * An implementation of CameraCaptureResult which always return default results.
   */
  final class EmptyCameraCaptureResult implements CameraCaptureResult {

    public static CameraCaptureResult create() {
      return new EmptyCameraCaptureResult();
    }

    @NonNull
    @Override
    public AfMode getAfMode() {
      return AfMode.UNKNOWN;
    }

    @NonNull
    @Override
    public AfState getAfState() {
      return AfState.UNKNOWN;
    }

    @NonNull
    @Override
    public AeState getAeState() {
      return AeState.UNKNOWN;
    }

    @NonNull
    @Override
    public AwbState getAwbState() {
      return AwbState.UNKNOWN;
    }

    @NonNull
    @Override
    public FlashState getFlashState() {
      return FlashState.UNKNOWN;
    }
  }
}
