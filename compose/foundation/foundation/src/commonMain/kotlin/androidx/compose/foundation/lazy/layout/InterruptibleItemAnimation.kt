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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter

internal abstract class InterruptibleItemAnimation<T, V : AnimationVector>(
    private val animatable: Animatable<T, V>,
    private val onValueChange: (T) -> Unit,
) {
    private enum class State {
        New,
        Idle,
        Entering,
        Exiting,
        Finished,
    }

    private var state = State.New

    private val isAnimating
        get() = state == State.Entering || state == State.Exiting

    protected suspend fun animateEnter(
        initialValue: T?,
        targetValue: T,
        animationSpec: AnimationSpec<T>?,
    ) {
        if (initialValue == null || animationSpec == null) {
            if (state == State.Exiting || state == State.Finished) {
                state = State.Idle
                onValueChange(targetValue)
                animatable.snapTo(targetValue)
            }
            return
        }
        val isInterrupting = isAnimating
        state = State.Entering
        if (!isInterrupting) {
            onValueChange(initialValue)
            animatable.snapTo(initialValue)
        }
        animatable.animateTo(targetValue, animationSpec) { onValueChange(value) }
        state = State.Idle
    }

    protected suspend fun animateExit(targetValue: T?, animationSpec: AnimationSpec<T>?) {
        if (targetValue == null || animationSpec == null) {
            state = State.Finished
            return
        }
        state = State.Exiting
        animatable.animateTo(targetValue, animationSpec) { onValueChange(value) }
        state = State.Finished
    }

    fun release() {
        state = State.New
    }
}

internal class EnterExitFadeAnimation(onAlphaChange: (Float) -> Unit) :
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
