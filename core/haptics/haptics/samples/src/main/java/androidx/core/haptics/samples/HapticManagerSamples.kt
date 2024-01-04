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

package androidx.core.haptics.samples

import android.content.Context
import androidx.annotation.Sampled
import androidx.core.haptics.HapticManager
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.off
import androidx.core.haptics.signal.CompositionSignal.Companion.quickFall
import androidx.core.haptics.signal.CompositionSignal.Companion.slowRise
import androidx.core.haptics.signal.CompositionSignal.Companion.thud
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick

/**
 * Sample showing how to play a standard click haptic effect on the system vibrator.
 */
@Sampled
fun PlaySystemStandardClick(context: Context) {
    val hapticManager = HapticManager.create(context)
    hapticManager.play(predefinedClick())
}

/**
 * Sample showing how to play a haptic signal on a vibrator.
 */
@Sampled
fun PlayHapticSignal(hapticManager: HapticManager) {
    hapticManager.play(
        compositionOf(
            slowRise(),
            quickFall(),
            off(durationMillis = 50),
            thud(),
        )
    )
}
