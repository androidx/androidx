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

package androidx.compose.ui.tooling.animation

import androidx.compose.runtime.MutableState
import androidx.compose.ui.tooling.AnimationDebugMutableState

/**
 * Information about a trigger for an animation - a property what can change the state of the
 * Composable.
 *
 * @property animationObject the state that triggers the animation.
 * @property label the label of the animation.
 * @property availableStates the set of possible states for the trigger, including null.
 */
internal class TriggerComposeAnimation<T>
private constructor(
    val animationObject: MutableState<T>,
    val label: String,
    availableStates: Set<T>,
) {

    /** The current state of the trigger. */
    var initialState: T = animationObject.value
        private set

    /** The target state of the trigger. */
    var targetState: T = availableStates.first { it != initialState }
        private set

    /**
     * The set of possible states for the trigger, excluding null.
     *
     * TODO(b/478807872) At the moment nullability is not fully supported.
     */
    val states =
        availableStates.filterNotNull().toMutableSet().also {
            it.addAll(setOfNotNull(animationObject.value))
        }

    /** Default state of the trigger. */
    private val defaultState = animationObject.value

    companion object {

        fun <T> AnimationDebugMutableState<T>.parse(): TriggerComposeAnimation<T>? {
            val availableStates = states()
            return TriggerComposeAnimation(this as MutableState<T>, label, availableStates)
        }
    }
}
