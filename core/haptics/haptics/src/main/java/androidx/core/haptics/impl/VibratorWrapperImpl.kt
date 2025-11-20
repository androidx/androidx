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

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.haptics.AttributesWrapper
import androidx.core.haptics.AudioAttributesWrapper
import androidx.core.haptics.PatternVibrationWrapper
import androidx.core.haptics.VibrationAttributesWrapper
import androidx.core.haptics.VibrationEffectWrapper
import androidx.core.haptics.VibrationWrapper
import androidx.core.haptics.VibratorWrapper

/** [VibratorWrapper] implementation backed by a real [Vibrator] service. */
internal class VibratorWrapperImpl(private val vibrator: Vibrator) : VibratorWrapper {

    override fun hasVibrator(): Boolean = ApiImpl.hasVibrator(vibrator)

    override fun hasAmplitudeControl(): Boolean =
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.hasAmplitudeControl(vibrator)
        } else {
            false
        }

    override fun areEffectsSupported(effects: IntArray): Array<VibratorWrapper.EffectSupport>? =
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.areEffectsSupported(vibrator, effects)
        } else {
            null
        }

    override fun arePrimitivesSupported(primitives: IntArray): BooleanArray? =
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.arePrimitivesSupported(vibrator, primitives)
        } else {
            null
        }

    override fun getPrimitivesDurations(primitives: IntArray): IntArray? =
        if (Build.VERSION.SDK_INT >= 31) {
            Api31Impl.getPrimitivesDurations(vibrator, primitives)
        } else {
            null
        }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun vibrate(vibration: VibrationWrapper, attrs: AttributesWrapper?) {
        when (vibration) {
            is VibrationEffectWrapper -> {
                check(Build.VERSION.SDK_INT >= 26) {
                    "Attempting to vibrate with VibrationEffect before" +
                        " Android O (API 26+), not supported"
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    check(attrs is VibrationAttributesWrapper) {
                        "Attempting to vibrate without VibrationAttributes after" +
                            " Android T (API 33+), not allowed"
                    }
                    Api33Impl.vibrate(vibrator, vibration, attrs)
                } else if (Build.VERSION.SDK_INT >= 26) {
                    check(attrs is AudioAttributesWrapper) {
                        "Attempting to vibrate without AudioAttributes after" +
                            " Android O (API 26+), not allowed"
                    }
                    Api26Impl.vibrate(vibrator, vibration, attrs)
                }
            }
            is PatternVibrationWrapper -> {
                check(attrs is AudioAttributesWrapper) {
                    "Attempting to vibrate without AudioAttributes after" +
                        " Android Lollipop (API 21+), not allowed"
                }
                Api21Impl.vibrate(vibrator, vibration, attrs)
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun cancel() {
        ApiImpl.cancel(vibrator)
    }

    /** Version-specific static inner class. */
    @RequiresApi(33)
    private object Api33Impl {

        @JvmStatic
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun vibrate(
            vibrator: Vibrator,
            effect: VibrationEffectWrapper,
            attrs: VibrationAttributesWrapper,
        ) {
            check(effect.vibrationEffect is VibrationEffect) {
                "Attempting to vibrate with unexpected VibrationEffect ${effect.vibrationEffect}"
            }
            check(attrs.vibrationAttributes is VibrationAttributes) {
                "Attempting to vibrate with unexpected VibrationAttributes" +
                    " ${attrs.vibrationAttributes}"
            }
            vibrator.vibrate(effect.vibrationEffect, attrs.vibrationAttributes)
        }
    }

    /** Version-specific static inner class. */
    @RequiresApi(31)
    private object Api31Impl {

        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun getPrimitivesDurations(vibrator: Vibrator, primitives: IntArray): IntArray? {
            return vibrator.getPrimitiveDurations(*primitives)
        }
    }

    /** Version-specific static inner class. */
    @RequiresApi(30)
    private object Api30Impl {

        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun areEffectsSupported(
            vibrator: Vibrator,
            effects: IntArray,
        ): Array<VibratorWrapper.EffectSupport> {
            return vibrator
                .areEffectsSupported(*effects)
                .map {
                    when (it) {
                        Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> VibratorWrapper.EffectSupport.YES
                        Vibrator.VIBRATION_EFFECT_SUPPORT_NO -> VibratorWrapper.EffectSupport.NO
                        else -> VibratorWrapper.EffectSupport.UNKNOWN
                    }
                }
                .toTypedArray()
        }

        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun arePrimitivesSupported(vibrator: Vibrator, primitives: IntArray): BooleanArray {
            return vibrator.arePrimitivesSupported(*primitives).toTypedArray().toBooleanArray()
        }
    }

    /** Version-specific static inner class. */
    @RequiresApi(26)
    private object Api26Impl {

        @JvmStatic fun hasAmplitudeControl(vibrator: Vibrator) = vibrator.hasAmplitudeControl()

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun vibrate(
            vibrator: Vibrator,
            effect: VibrationEffectWrapper,
            attrs: AudioAttributesWrapper,
        ) {
            check(effect.vibrationEffect is VibrationEffect) {
                "Attempting to vibrate with unexpected VibrationEffect ${effect.vibrationEffect}"
            }
            check(attrs.audioAttributes is AudioAttributes) {
                "Attempting to vibrate with unexpected AudioAttributes ${attrs.audioAttributes}"
            }
            vibrator.vibrate(effect.vibrationEffect, attrs.audioAttributes)
        }
    }

    /** Version-specific static inner class. */
    private object Api21Impl {

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun vibrate(
            vibrator: Vibrator,
            pattern: PatternVibrationWrapper,
            attrs: AudioAttributesWrapper,
        ) {
            check(attrs.audioAttributes is AudioAttributes) {
                "Attempting to vibrate with unexpected AudioAttributes ${attrs.audioAttributes}"
            }
            vibrator.vibrate(pattern.timings, pattern.repeatIndex, attrs.audioAttributes)
        }
    }

    /** Version-specific static inner class. */
    private object ApiImpl {

        @JvmStatic fun hasVibrator(vibrator: Vibrator) = vibrator.hasVibrator()

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun vibrate(vibrator: Vibrator, pattern: PatternVibrationWrapper) =
            vibrator.vibrate(pattern.timings, pattern.repeatIndex)

        @JvmStatic
        @RequiresPermission(android.Manifest.permission.VIBRATE)
        fun cancel(vibrator: Vibrator) = vibrator.cancel()
    }
}
