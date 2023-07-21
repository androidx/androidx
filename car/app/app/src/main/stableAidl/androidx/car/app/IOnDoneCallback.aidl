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

/**
 * A generic callback to allow the host to wait for the client to complete processing a request
 * before continuing.
 *
 * @hide
 */
interface IOnDoneCallback {
  /**
   * Notifies that the client has successfully processed the request, and provides its response.
   */
  void onSuccess(in @nullable Bundleable response) = 1;

  /**
   * Notifies that the client did not fulfill the request successfully.
   */
  void onFailure(in Bundleable failureResponse) = 2;
}