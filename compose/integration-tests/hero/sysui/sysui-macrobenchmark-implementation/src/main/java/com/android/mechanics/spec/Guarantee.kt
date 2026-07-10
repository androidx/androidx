/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics.spec

/**
 * Describes the condition by which a discontinuity at a breakpoint must have finished animating.
 *
 * With a guarantee in effect, the spring parameters will be continuously adjusted, ensuring the
 * guarantee's target will be met.
 */
sealed class Guarantee {
    /**
     * No guarantee is provided.
     *
     * The spring animation will proceed at its natural pace, regardless of the input or gesture's
     * progress.
     */
    data object None : Guarantee()

    /**
     * Guarantees that the animation will be complete before the input value is [delta] away from
     * the [Breakpoint] position.
     */
    data class InputDelta(val delta: Float) : Guarantee()

    /**
     * Guarantees to complete the animation before the gesture is [delta] away from the gesture
     * position captured when the breakpoint was crossed.
     */
    data class GestureDragDelta(val delta: Float) : Guarantee()
}
