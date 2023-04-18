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

import android.content.Intent;

/** @hide */
interface ICarHost {
  /**
   * Starts a car app on the car screen.
   */
  void startCarApp(in Intent intent) = 1;

  /**
   * Returns the binder for the specified type for the app to use.
   *
   * <p>type is a @CarContext#CarAppService.
   */
  IBinder getHost(in String type) = 2;

  /**
   * Exits the car app on the car screen.
   */
   void finish() = 3;
}
