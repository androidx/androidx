/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.TrackingState
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

@Implements(OpenXrDevice::class)
public class ShadowOpenXrDevice {
    public companion object {
        internal var mockedDeviceState: DeviceState = DeviceState(TrackingState.PAUSED, null)

        @Resetter
        @JvmStatic
        public fun reset() {
            mockedDeviceState = DeviceState(TrackingState.PAUSED, null)
        }

        @Implementation
        @JvmStatic
        @JvmName("nativeGetDeviceState")
        internal fun nativeGetDeviceState(monotonicTimeNs: Long): DeviceState = mockedDeviceState
    }
}
