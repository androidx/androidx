/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Stable
import com.android.mechanics.debug.DebugInspector
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticKey

/** State produces by a motion value. */
@Stable
sealed interface MotionValueState : FloatState {

    /**
     * Animated [output] value.
     *
     * Same as [floatValue].
     */
    val output: Float

    /**
     * [output] value, but without animations.
     *
     * This value always reports the target value, even before a animation is finished.
     *
     * While [isStable], [outputTarget] and [output] are the same value.
     */
    val outputTarget: Float

    /** Whether an animation is currently running. */
    val isStable: Boolean

    /**
     * The current value for the [SemanticKey].
     *
     * `null` if not defined in the spec.
     */
    operator fun <T> get(key: SemanticKey<T>): T?

    /** The current segment used to compute the output. */
    val segmentKey: SegmentKey

    /** Debug label of the motion value. */
    val label: String?

    /** Provides access to the current state for debugging.. */
    fun debugInspector(): DebugInspector
}
