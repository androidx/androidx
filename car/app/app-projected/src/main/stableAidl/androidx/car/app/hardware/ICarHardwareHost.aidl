/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.car.app.hardware;

import androidx.car.app.serialization.Bundleable;
import androidx.car.app.hardware.ICarHardwareResult;

/** @hide */
interface ICarHardwareHost {
  /**
   * Indicates to the host that the app is interested in a car hardware result with a single value.
   */
  void getCarHardwareResult(in int resultType, in @nullable Bundleable params,
          in ICarHardwareResult callback) = 1;

  /**
   * Indicates to the host that the app wants to subscribe to a car hardware result that changes
   * over time.
   */
  void subscribeCarHardwareResult(in int resultType, in @nullable Bundleable params,
          in ICarHardwareResult callback) = 2;

  /**
   * Indicates to the host that the app wants to unsubscribe from a vehicle result that changes
   * over time.
   */
  void unsubscribeCarHardwareResult(in int resultType, in @nullable Bundleable params) = 3;

}
