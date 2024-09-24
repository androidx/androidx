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
import androidx.compose.runtime.Stable

/**
 * A read-only state of a three pane scaffold. It provides information about the [Transition]
 * between [ThreePaneScaffoldValue]s.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed class ThreePaneScaffoldState {
    /**
     * Current [ThreePaneScaffoldValue] state of the transition. If there is an active transition,
     * [currentState] and [targetState] are different.
     */
    abstract val currentState: ThreePaneScaffoldValue

    /**
     * Target [ThreePaneScaffoldValue] state of the transition. If this is the same as
     * [currentState], no transition is active.
     */
    abstract val targetState: ThreePaneScaffoldValue

    /**
     * The progress of the transition from [currentState] to [targetState] as a fraction of the
     * entire duration.
     *
     * If [targetState] and [currentState] are the same, [progressFraction] will be 0.
     */
    @get:FloatRange(from = 0.0, to = 1.0) abstract val progressFraction: Float

    @Composable internal abstract fun rememberTransition(): Transition<ThreePaneScaffoldValue>
}

/**
 * The seekable state of a three pane scaffold. It serves as the [SeekableTransitionState] to
 * manipulate the [Transition] between [ThreePaneScaffoldValue]s.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
class MutableThreePaneScaffoldState(
    initialScaffoldValue: ThreePaneScaffoldValue
) : ThreePaneScaffoldState() {
    private val transitionState = SeekableTransitionState(initialScaffoldValue)

    override val currentState
        get() = transitionState.currentState

    override val targetState
        get() = transitionState.targetState

    @get:FloatRange(from = 0.0, to = 1.0)
    override val progressFraction
        get() = transitionState.fraction

    private val mutatorMutex = MutatorMutex()

    /**
     * Creates a [Transition] and puts it in the [currentState] of this
     * [MutableThreePaneScaffoldState]. If [targetState] changes, the [Transition] will change where
     * it will animate to.
     */
    @Composable
    override fun rememberTransition(): Transition<ThreePaneScaffoldValue> =
        rememberTransition(transitionState, label = "ThreePaneScaffoldState")

    /**
     * Sets [currentState] and [targetState][MutableThreePaneScaffoldState.targetState] to
     * [targetState] and snaps all values to those at that state. The transition will not have any
     * animations running after running [snapTo].
     *
     * @param targetState The [ThreePaneScaffoldValue] state to snap to.
     * @see SeekableTransitionState.snapTo
     */
    suspend fun snapTo(targetState: ThreePaneScaffoldValue) {
        mutatorMutex.mutate { transitionState.snapTo(targetState) }
    }

    /**
     * Seeks the transition to [targetState] with [fraction] used to indicate the progress towards
     * [targetState].
     *
     * @param fraction The fractional progress of the transition.
     * @param targetState The [ThreePaneScaffoldValue] state to seek to.
     * @see SeekableTransitionState.seekTo
     */
    suspend fun seekTo(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
        targetState: ThreePaneScaffoldValue = this.targetState,
    ) {
        mutatorMutex.mutate { transitionState.seekTo(fraction, targetState) }
    }

    /**
     * Updates the current [targetState][MutableThreePaneScaffoldState.targetState] to [targetState]
     * with an animation to the new state.
     *
     * @param targetState The [ThreePaneScaffoldValue] state to animate towards.
     * @param animationSpec If provided, is used to animate the animation fraction. If `null`, the
     *   transition is linearly traversed based on the duration of the transition.
     * @see SeekableTransitionState.animateTo
     */
    suspend fun animateTo(
        targetState: ThreePaneScaffoldValue = this.targetState,
        animationSpec: FiniteAnimationSpec<Float>? = null,
    ) {
        mutatorMutex.mutate { transitionState.animateTo(targetState, animationSpec) }
    }
}
