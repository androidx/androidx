/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import androidx.car.app.IOnDoneCallback;

/** @hide */
oneway interface IAlertCallback {
  /** Will be triggered when the alert is cancelled. */
  void onAlertCancelled(int reason, IOnDoneCallback callback) = 1;
  /** Will be triggered when the alert is dismissed. */
  void onAlertDismissed(IOnDoneCallback callback) = 2;
}
