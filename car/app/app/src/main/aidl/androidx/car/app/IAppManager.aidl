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

import androidx.car.app.serialization.Bundleable;
import androidx.car.app.IOnDoneCallback;

/** @hide */
oneway interface IAppManager {
  /** Requests for a callback to update the template. */
  void getTemplate(IOnDoneCallback callback) = 1;

  /** Notifies that the host received a back button press. */
  void onBackPressed(IOnDoneCallback callback) = 2;

  /** Requests the app to send location updates. */
  void startLocationUpdates(IOnDoneCallback callback) = 3;

  /** Requests the app to stop sending location updates. */
  void stopLocationUpdates(IOnDoneCallback callback) = 4;
}
