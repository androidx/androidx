/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.haptics.signal

import androidx.core.haptics.device.HapticDeviceProfile

/**
 * A function that provides a [HapticSignal] to be played by a device vibrator.
 *
 * Resolvable haptic signals allow for extension of [HapticSignal] by providing an opaque
 * representation that can be resolved for a specific device vibrator base on its capabilities
 * described by the [HapticDeviceProfile].
 *
 * This can be used to implement custom fallback behavior for lower SDK versions and/or less capable
 * vibrator hardware.
 */
public fun interface ResolvableSignal {

    /**
     * Returns a concrete [HapticSignal] to be played in the device with given profile.
     *
     * This method can return a different signal based on the capabilities of the given device
     * vibrator, including null if no haptic should be played by this device.
     *
     * @param deviceProfile The device profile to be used for haptic capability checks.
     * @return The [HapticSignal] to be played on given device, not null if no signal should be
     *   played in this device.
     */
    public fun resolve(deviceProfile: HapticDeviceProfile): HapticSignal?
}
