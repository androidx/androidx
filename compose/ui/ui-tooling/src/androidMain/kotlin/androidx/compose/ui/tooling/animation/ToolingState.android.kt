/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tooling can override [mutableStateOf] in [Composable] with [ToolingState]. [Composable] should
 * declare a state, which could be private:
 *
 *      val toolingOverride = remember { mutableStateOf<State<T>?>(null) }
 *
 * Tooling overrides `toolingOverride` with
 *
 *      toolingOverride.value = ToolingState(default)
 *
 * @param default default value
 */
class ToolingState<T>(default: T) : State<T> {
    override var value by mutableStateOf(default)
}

/**
 * @param override [MutableState] that is part of compose animation and let tooling override
 *   animation behavior. [override] value should be overridden with [state]. [override] is found as
 *   part of the slotTree.
 * @param state value to override [override] with.
 */
internal class ToolingOverride<T>(
    val override: MutableState<State<T>?>,
    val state: ToolingState<T>,
) {

    /**
     * Override animation value with tooling value. This allows animation to be controlled from
     * tooling.
     */
    fun overrideState() {
        override.value = state
    }

    /** Clear [override] value. This allows animation to play without intervention from tooling. */
    fun clearOverride() {
        override.value = null
    }
}
