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

package androidx.core.haptics

import android.content.Context
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.impl.HapticManagerImpl
import androidx.core.haptics.impl.VibratorWrapperImpl
import androidx.core.haptics.signal.HapticSignal
import androidx.core.haptics.signal.ResolvableSignal

/**
 * Manager for interactions with a device vibrator.
 *
 * <p>If your process exits, any vibration you started will stop.
 */
public interface HapticManager {

    public companion object {

        /**
         * Creates a haptic manager for the system vibrator.
         *
         * This returns a manager instance only if the device has a vibrator motor, i.e. the system
         * vibrator check [Vibrator.hasVibrator] returns true, and returns null otherwise.
         *
         * @sample androidx.core.haptics.samples.PlaySystemStandardClick
         * @param context Context to load the device vibrator.
         * @return a new instance of HapticManager for the system vibrator, or null if the device
         *   does not have a vibrator motor.
         */
        @JvmStatic
        public fun create(context: Context): HapticManager? {
            return requireNotNull(ContextCompat.getSystemService(context, Vibrator::class.java)) {
                    "Vibrator service not found"
                }
                .let { systemVibrator ->
                    if (systemVibrator.hasVibrator()) {
                        HapticManagerImpl(VibratorWrapperImpl(systemVibrator))
                    } else {
                        null
                    }
                }
        }

        /** Creates a haptic manager for the given vibrator. */
        @VisibleForTesting
        internal fun createForVibrator(vibrator: VibratorWrapper): HapticManager? {
            return if (vibrator.hasVibrator()) {
                HapticManagerImpl(vibrator)
            } else {
                null
            }
        }
    }

    /** A [HapticDeviceProfile] describing the vibrator hardware capabilities for the device. */
    public val deviceProfile: HapticDeviceProfile

    /**
     * Play a [HapticSignal].
     *
     * @sample androidx.core.haptics.samples.PlayHapticSignal
     * @param signal The haptic signal to be played.
     * @param attrs The attributes corresponding to the haptic signal. For example, specify
     *   [HapticAttributes.USAGE_NOTIFICATION] for notification vibrations or
     *   [HapticAttributes.USAGE_TOUCH] for touch feedback haptics.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    public fun play(signal: HapticSignal, attrs: HapticAttributes)

    /**
     * Resolves and plays a given [ResolvableSignal].
     *
     * If the same signal will be played by this vibrator multiple times then consider resolving the
     * [HapticSignal] only once using this [deviceProfile] and then reusing it.
     *
     * @sample androidx.core.haptics.samples.PlayResolvableHapticSignal
     * @param signal The haptic signal to be resolved using this device profile and played.
     * @param attrs The attributes corresponding to the haptic signal. For example, specify
     *   [HapticAttributes.USAGE_NOTIFICATION] for notification vibrations or
     *   [HapticAttributes.USAGE_TOUCH] for touch feedback haptics.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    public fun play(signal: ResolvableSignal, attrs: HapticAttributes) {
        signal.resolve(deviceProfile)?.let { resolvedSignal -> play(resolvedSignal, attrs) }
    }

    /**
     * Cancel any [HapticSignal] currently playing.
     *
     * @sample androidx.core.haptics.samples.PlayThenCancel
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE) public fun cancel()
}
