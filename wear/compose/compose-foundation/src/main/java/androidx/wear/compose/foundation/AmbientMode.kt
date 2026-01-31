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

package androidx.wear.compose.foundation

/**
 * Represents the current ambient mode of the device.
 *
 * Example of using [AmbientMode] in a simple use case:
 *
 * @sample androidx.wear.compose.foundation.samples.AmbientModeBasicSample
 */
public abstract class AmbientMode private constructor() {
    /** Represents the mode when the user is actively interacting with the device. */
    public object Interactive : AmbientMode()

    /**
     * Represents that device is in the ambient mode. In this mode, the app is typically updated at
     * infrequent intervals (e.g., once per minute).
     *
     * @property isBurnInProtectionRequired Indicates whether the ambient layout must implement
     *   burn-in protection. When this property is set to true, composables must be shifted around
     *   periodically in ambient mode. To ensure that content isn't shifted off the screen, avoid
     *   placing content within 10 pixels of the edge of the screen and also avoid solid white areas
     *   to prevent pixel burn-in. Both of these requirements only apply in ambient mode, and only
     *   when this property is set to true.
     * @property isLowBitAmbientSupported Specifies whether this device has low-bit ambient mode.
     *   When this property is set to true, the screen supports fewer bits for each color in ambient
     *   mode. In this case, anti-aliasing should be disabled in ambient mode.
     */
    public class Ambient(
        public val isBurnInProtectionRequired: Boolean,
        public val isLowBitAmbientSupported: Boolean,
    ) : AmbientMode() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Ambient

            return isBurnInProtectionRequired == other.isBurnInProtectionRequired &&
                isLowBitAmbientSupported == other.isLowBitAmbientSupported
        }

        override fun hashCode(): Int {
            var result = isBurnInProtectionRequired.hashCode()
            result = 31 * result + isLowBitAmbientSupported.hashCode()
            return result
        }
    }
}
