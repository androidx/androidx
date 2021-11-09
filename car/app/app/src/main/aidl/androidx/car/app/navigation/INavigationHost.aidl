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

package androidx.car.app.navigation;

import androidx.car.app.serialization.Bundleable;

/** @hide */
interface INavigationHost {
 /**
  * Update the host when active navigation in the app has started.
  */
  void navigationStarted() = 1;

 /**
  * Update the host when active navigation in the app has ended.
  */
  void navigationEnded() = 2;

  /**
   * Sends the navigation state to the host which can be rendered at different
   * places in the car such as the navigation templates, cluster screens, etc.
   */
  void updateTrip(in Bundleable trip) = 3;
}
