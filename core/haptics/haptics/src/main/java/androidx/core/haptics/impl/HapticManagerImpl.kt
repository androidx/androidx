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

package androidx.core.haptics.impl

import android.content.Context
import android.os.Build.VERSION
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.haptics.HapticManager
import androidx.core.haptics.signal.PredefinedEffect
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedDoubleClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedHeavyClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedTick

/**
 * [HapticManager] implementation for the [Vibrator] service.
 */
internal class HapticManagerImpl internal constructor(
    private val vibrator: Vibrator
) : HapticManager {

    internal constructor(context: Context) : this(
        requireNotNull(ContextCompat.getSystemService(context, Vibrator::class.java)) {
            "Vibrator service not found"
        }
    )

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun play(effect: PredefinedEffect) {
        if (VERSION.SDK_INT >= 29) {
            Api29Impl.play(vibrator, effect)
        } else {
            ApiImpl.play(vibrator, effect)
        }
    }

    /** Version-specific static inner class. */
    @RequiresApi(29)
    private object Api29Impl {

        @JvmStatic
        @DoNotInline
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun play(vibrator: Vibrator, effect: PredefinedEffect) {
            vibrator.vibrate(VibrationEffect.createPredefined(effect.effectId))
        }
    }

    /** Version-specific static inner class. */
    private object ApiImpl {

        private val predefinedEffectFallbackPatterns = mapOf(
            PredefinedTick to longArrayOf(0, 10),
            PredefinedClick to longArrayOf(0, 20),
            PredefinedHeavyClick to longArrayOf(0, 30),
            PredefinedDoubleClick to longArrayOf(0, 30, 100, 30)
        )

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun play(vibrator: Vibrator, effect: PredefinedEffect) {
            predefinedEffectFallbackPatterns[effect]?.let {
                vibrator.vibrate(/* pattern= */ it, /* repeat= */ -1)
            }
        }
    }
}
