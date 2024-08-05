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

import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.device.HapticDeviceProfile

/**
 * A [HapticSignal] describes a generic vibration to be played by a vibrator.
 *
 * These signals may represent any number of things, from single shot vibrations to complex
 * waveforms, device-specific predefined effects or custom vibration patterns.
 *
 * A haptic signal can be defined as a [FiniteSignal] or [InfiniteSignal]. Infinite signals will be
 * played by the vibrator until canceled, while finite signals will stop playing once completed.
 *
 * Note: This is a library-restricted representation of a haptic signal that will be mapped to a
 * platform representation available, like [android.os.VibrationEffect]. This class cannot be
 * extended or supplemented outside the library, but they can be instantiated from custom extensions
 * via factory methods.
 */
public abstract class HapticSignal internal constructor() : ResolvableSignal {

    override fun resolve(deviceProfile: HapticDeviceProfile): HapticSignal? = this

    /**
     * Returns a [VibrationWrapper] representing this signal, or null if not supported in this SDK
     * level.
     */
    internal abstract fun toVibration(): VibrationWrapper?

    /** Returns true if the device vibrator can play this signal as intended, false otherwise. */
    internal abstract fun isSupportedBy(deviceProfile: HapticDeviceProfile): Boolean
}

/** A [FiniteSignal] describes a non-infinite haptic signal to be played by a vibrator. */
public abstract class FiniteSignal internal constructor() : HapticSignal()

/**
 * A [InfiniteSignal] describes a haptic signal that will be played by a vibrator until canceled.
 */
public abstract class InfiniteSignal internal constructor() : HapticSignal()
