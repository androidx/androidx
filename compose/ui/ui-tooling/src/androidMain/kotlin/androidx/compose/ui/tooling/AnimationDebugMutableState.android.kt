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

package androidx.compose.ui.tooling

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.StateFactoryMarker

/**
 * Extend [mutableStateOf] with debug information required for animation tooling.
 *
 * @param value the initial value for the [MutableState]
 * @param states the set of possible states for the [MutableState]
 * @param label the label of the [MutableState] to display in tooling
 */
@StateFactoryMarker
internal fun <T> animationDebugMutableStateOf(
    value: T,
    createMutableState: (T) -> MutableState<T>,
    states: () -> Set<T>,
    label: String,
): MutableState<T> {
    return if (isAnimationPreviewEnabled)
        AnimationDebugMutableState(createMutableState(value), states, label)
    else createMutableState(value)
}

/**
 * A mutable value holder which extends [MutableState] with extra debug information required for
 * animation tooling.
 *
 * @param state the wrapped [MutableState]
 * @param states the set of possible states for the [MutableState]
 * @param label the label of the [MutableState] to display in tooling
 */
internal class AnimationDebugMutableState<T>(
    state: MutableState<T>,
    val states: () -> Set<T>,
    val label: String,
) : MutableState<T> by state
