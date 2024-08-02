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
 * A [FallbackChainSignal] is a resolvable haptic signal that returns the first supported
 * [HapticSignal] from a selection list, or null if no effect is supported by the device.
 *
 * A haptic fallback chain is composed of an ordered list of [ResolvableSignal], which allows
 * composing this with generic haptic signal extensions, including other fallback chains.
 *
 * @sample androidx.core.haptics.samples.HapticFallbackChainOfSignals
 * @see HapticDeviceProfile
 */
public class FallbackChainSignal(

    /** The ordered list of haptic signals that defines the fallback chain. */
    public val signals: List<ResolvableSignal>,
) : ResolvableSignal {

    public companion object {

        /**
         * Returns a [FallbackChainSignal] with given signals.
         *
         * @sample androidx.core.haptics.samples.HapticFallbackChainOfSignals
         */
        @JvmStatic
        public fun fallbackChainOf(vararg signals: ResolvableSignal): FallbackChainSignal =
            FallbackChainSignal(signals.toList())
    }

    override fun resolve(deviceProfile: HapticDeviceProfile): HapticSignal? =
        signals
            .asSequence()
            .mapNotNull { it.resolve(deviceProfile) }
            .firstOrNull { deviceProfile.supports(it) }
}
