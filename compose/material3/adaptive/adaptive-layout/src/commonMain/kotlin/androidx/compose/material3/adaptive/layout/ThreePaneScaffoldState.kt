/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.MutatorMutex
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable

@ExperimentalMaterial3AdaptiveApi
internal class ThreePaneScaffoldState
internal constructor(
    private val transitionState: SeekableTransitionState<ThreePaneScaffoldValue>,
) {
    val currentState
        get() = transitionState.currentState

    val targetState
        get() = transitionState.targetState

    val fraction
        get() = transitionState.fraction

    private val mutatorMutex = MutatorMutex()

    @Composable
    fun rememberTransition(label: String? = null): Transition<ThreePaneScaffoldValue> =
        rememberTransition(transitionState, label)

    suspend fun snapTo(targetState: ThreePaneScaffoldValue) {
        mutatorMutex.mutate { transitionState.snapTo(targetState) }
    }

    suspend fun seekTo(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
        targetState: ThreePaneScaffoldValue = this.targetState,
    ) {
        mutatorMutex.mutate { transitionState.seekTo(fraction, targetState) }
    }

    suspend fun animateTo(
        targetState: ThreePaneScaffoldValue = this.targetState,
        animationSpec: FiniteAnimationSpec<Float>? = null,
    ) {
        mutatorMutex.mutate { transitionState.animateTo(targetState, animationSpec) }
    }
}
