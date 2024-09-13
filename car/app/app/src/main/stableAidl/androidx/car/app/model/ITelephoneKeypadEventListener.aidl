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

package androidx.car.app.model;

import androidx.car.app.IOnDoneCallback;

/** Binder for Keypad key events. Keys are defined in the Keypad class. */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ITelephoneKeypadEventListener {
  /** Triggered when a key is long pressed. */
  void onKeyLongPress(int key, IOnDoneCallback callback) = 1;

  /** Triggered when a key is pushed. */
  void onKeyDown(int key, IOnDoneCallback callback) = 2;

  /** Triggered when a key is released. */
  void onKeyUp(int key, IOnDoneCallback callback) = 3;
}
