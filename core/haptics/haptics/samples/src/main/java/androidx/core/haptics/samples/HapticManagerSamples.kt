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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.content.Context
import androidx.annotation.Sampled
import androidx.core.haptics.HapticAttributes
import androidx.core.haptics.HapticAttributes.Companion.USAGE_MEDIA
import androidx.core.haptics.HapticManager
import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.off
import androidx.core.haptics.signal.CompositionSignal.Companion.quickFall
import androidx.core.haptics.signal.CompositionSignal.Companion.slowRise
import androidx.core.haptics.signal.CompositionSignal.Companion.thud
import androidx.core.haptics.signal.FallbackChainSignal.Companion.fallbackChainOf
import androidx.core.haptics.signal.InfiniteSignal
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedDoubleClick
import kotlin.time.Duration.Companion.seconds

/**
 * Sample showing how to play a standard click haptic effect on the system vibrator.
 */
@Sampled
fun PlaySystemStandardClick(context: Context) {
    val hapticManager = HapticManager.create(context)
    hapticManager?.play(predefinedClick(), HapticAttributes(HapticAttributes.USAGE_TOUCH))
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
        ),
        HapticAttributes(HapticAttributes.USAGE_TOUCH),
    )
}

/**
 * Sample showing how to play a resolvable haptic signal on a vibrator.
 */
@Sampled
fun PlayResolvableHapticSignal(hapticManager: HapticManager) {
    hapticManager.play(
        fallbackChainOf(
            compositionOf(
                click(amplitudeScale = 0.7f),
                off(durationMillis = 50),
                thud(amplitudeScale = 0.5f),
            ),
            predefinedDoubleClick(),
        ),
        HapticAttributes(HapticAttributes.USAGE_TOUCH),
    )
}

/**
 * Sample showing how to play a haptic signal and then cancel.
 */
@Sampled
fun PlayThenCancel(
    hapticManager: HapticManager,
    repeatingHapticSignal: InfiniteSignal,
) {
    ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3.seconds.inWholeMilliseconds
        repeatMode = INFINITE
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                hapticManager.play(repeatingHapticSignal, HapticAttributes(USAGE_MEDIA))
            }
            override fun onAnimationCancel(animation: Animator) {
                hapticManager.cancel()
            }
        })
    }
}
