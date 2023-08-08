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
import androidx.core.haptics.impl.HapticManagerImpl
import androidx.core.haptics.signal.PredefinedEffect

/**
 * Manager for the vibrators of a device.
 *
 * <p>If your process exits, any vibration you started will stop.
 */
interface HapticManager {

    companion object {

        /**
         * Creates haptic manager for the system vibrators.
         *
         * Sample code:
         * @sample androidx.core.haptics.samples.PlaySystemStandardClick
         *
         * @param context Context to load the device vibrators.
         * @return a new instance of HapticManager for the system vibrators.
         */
        @JvmStatic
        fun create(context: Context): HapticManager {
            return HapticManagerImpl(context)
        }

        /** Creates haptic manager for given vibrator. */
        internal fun createForVibrator(vibrator: Vibrator): HapticManager {
            return HapticManagerImpl(vibrator)
        }
    }

    /**
     * Play a [PredefinedEffect].
     *
     * The app should be in the foreground for the vibration to happen.
     *
     * Sample code:
     * @sample androidx.core.haptics.samples.PlaySystemStandardClick
     *
     * @param effect The predefined haptic effect to be played.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun play(effect: PredefinedEffect)
}
