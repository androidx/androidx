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
import androidx.core.haptics.impl.HapticManagerImpl
import androidx.core.haptics.impl.VibratorWrapperImpl
import androidx.core.haptics.signal.HapticSignal

/**
 * Manager for interactions with a device vibrator.
 *
 * <p>If your process exits, any vibration you started will stop.
 */
interface HapticManager {

    companion object {

        /**
         * Creates a haptic manager for the system vibrator.
         *
         * @sample androidx.core.haptics.samples.PlaySystemStandardClick
         *
         * @param context Context to load the device vibrator.
         * @return a new instance of HapticManager for the system vibrator.
         */
        @JvmStatic
        fun create(context: Context): HapticManager {
            return HapticManagerImpl(
                VibratorWrapperImpl(
                    requireNotNull(ContextCompat.getSystemService(context, Vibrator::class.java)) {
                        "Vibrator service not found"
                    }
                )
            )
        }

        /** Creates a haptic manager for the given vibrator. */
        @VisibleForTesting
        internal fun createForVibrator(vibrator: VibratorWrapper): HapticManager {
            return HapticManagerImpl(vibrator)
        }
    }

    /**
     * Play a [HapticSignal].
     *
     * @sample androidx.core.haptics.samples.PlayHapticSignal
     *
     * @param signal The haptic signal to be played.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun play(signal: HapticSignal)
}
