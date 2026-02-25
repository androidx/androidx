/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.projected.platform;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)")
@Backing(type="int")
enum ProjectedDeviceState {
  // Device is currently not usable. Commonly done because the device is not worn.
  INACTIVE = 0,
  // Device is currently useable. Apps can project content to the device.
  ACTIVE = 1,
  // A virtual device representing the Projected device was destroyed.
  DESTROYED = 2,
}
