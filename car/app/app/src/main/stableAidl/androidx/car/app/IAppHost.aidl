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

package androidx.car.app;

import android.location.Location;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.serialization.Bundleable;

/** @hide */
interface IAppHost {
  /** Requests the current template to be invalidated. */
  void invalidate() = 1;

  /** Shows a toast on the car screen. */
  void showToast(CharSequence text, int duration) = 2;

  /** Registers the callback to get surface events. */
  void setSurfaceCallback(@nullable ISurfaceCallback callback) = 3;

  /** Sends the last known location to the host. */
  void sendLocation(in Location location) = 4;

  /** Shows an alert to the car screen. */
  void showAlert(in Bundleable alert) = 5;

  /** Dismisses the alert if active. */
  void dismissAlert(int alertId) = 6;

  /**
   * Requests microphone input bytes.
   */
  Bundleable openMicrophone(in Bundleable openMicrophoneRequest) = 7;
}
