/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.animation.FadeConfig
import androidx.compose.animation.SlideConfig
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Runs an enter or exit animation for a single property of a lazy layout item, handling
 * interruptions between them.
 *
 * @param animatable holds the animated value.
 * @param onValueChange invoked every time the applied value changes.
 */
internal abstract class InterruptibleItemAnimation<T, V : AnimationVector>(
    private val animatable: Animatable<T, V>,
    private val onValueChange: () -> Unit,
) {
    private enum class State {
        New,
        Idle,
        Entering,
        Exiting,
        Finished,
    }

    private var appliedValue: T = animatable.value

    private fun applyValue(value: T) {
        appliedValue = value
        onValueChange()
    }

    private var state = State.New

    private val isAnimating
        get() = state == State.Entering || state == State.Exiting

    private suspend fun snapTo(targetValue: T) {
        applyValue(targetValue)
        animatable.snapTo(targetValue)
    }

    private suspend fun animateTo(targetValue: T, animationSpec: AnimationSpec<T>) {
        if (appliedValue == targetValue) {
            withFrameNanos {}
            snapTo(targetValue)
            return
        }
        animatable.animateTo(targetValue, animationSpec) { applyValue(value) }
    }

    protected suspend fun animateEnter(
        initialValue: T?,
        targetValue: T,
        animationSpec: AnimationSpec<T>?,
    ) {
        if (initialValue == null || animationSpec == null) {
            if (state == State.Exiting || state == State.Finished) {
                state = State.Idle
                snapTo(targetValue)
            }
            return
        }
        val wasAnimating = isAnimating
        state = State.Entering
        if (!wasAnimating) {
            snapTo(initialValue)
        }
        animateTo(targetValue, animationSpec)
        state = State.Idle
    }

    protected suspend fun animateExit(
        targetValue: T?,
        animationSpec: AnimationSpec<T>?,
        exitOnlyInitialValue: (() -> T)? = null,
    ) {
        if (targetValue == null || animationSpec == null) {
            if ((state == State.New).also { state = State.Finished }) {
                snapTo(exitOnlyInitialValue?.invoke() ?: appliedValue)
            }
            return
        }
        val wasAnimating = isAnimating
        state = State.Exiting
        if (!wasAnimating) {
            snapTo(exitOnlyInitialValue?.invoke() ?: appliedValue)
        }
        animateTo(targetValue, animationSpec)
        state = State.Finished
    }

    fun release() {
        state = State.New
    }

    val value: T?
        get() = if (state == State.New) null else appliedValue
}

internal class EnterExitFadeAnimation(onAlphaChange: () -> Unit) :
    InterruptibleItemAnimation<Float, AnimationVector1D>(
        animatable = Animatable(1f, Float.VectorConverter),
        onValueChange = onAlphaChange,
    ) {
    suspend fun animateFadeIn(fadeInConfig: FadeConfig?) =
        animateEnter(
            initialValue = fadeInConfig?.alpha,
            targetValue = 1f,
            animationSpec = fadeInConfig?.animationSpec,
        )

    suspend fun animateFadeOut(fadeOutConfig: FadeConfig?) =
        animateExit(
            targetValue = fadeOutConfig?.alpha,
            animationSpec = fadeOutConfig?.animationSpec,
        )
}

internal class EnterExitSlideAnimation(onOffsetChange: () -> Unit) :
    InterruptibleItemAnimation<IntOffset, AnimationVector2D>(
        animatable = Animatable(IntOffset.Zero, IntOffset.VectorConverter),
        onValueChange = onOffsetChange,
    ) {
    suspend fun animateSlideIn(slideInConfig: SlideConfig?, itemSize: IntSize) =
        animateEnter(
            initialValue = slideInConfig?.slideOffset?.invoke(itemSize),
            targetValue = IntOffset.Zero,
            animationSpec = slideInConfig?.animationSpec,
        )

    suspend fun animateSlideOut(
        slideOutConfig: SlideConfig?,
        itemSize: IntSize,
        exitOnlyInitialOffset: () -> IntOffset,
    ) =
        animateExit(
            targetValue = slideOutConfig?.slideOffset(itemSize),
            animationSpec = slideOutConfig?.animationSpec,
            exitOnlyInitialValue = exitOnlyInitialOffset,
        )
}
