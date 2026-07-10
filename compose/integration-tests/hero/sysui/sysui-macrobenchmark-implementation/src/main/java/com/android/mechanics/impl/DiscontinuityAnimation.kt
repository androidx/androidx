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

package com.android.mechanics.impl

import com.android.mechanics.spring.SpringParameters
import com.android.mechanics.spring.SpringState

/**
 * Captures the start-state of a spring-animation to smooth over a discontinuity.
 *
 * Discontinuities are caused by segment changes, where the new and old segment produce different
 * output values for the same input.
 */
internal data class DiscontinuityAnimation(
    val springStartState: SpringState,
    val springParameters: SpringParameters,
    val springStartTimeNanos: Long,
) {
    val isAtRest: Boolean
        get() = springStartState == SpringState.AtRest

    companion object {
        val None =
            DiscontinuityAnimation(
                springStartState = SpringState.AtRest,
                springParameters = SpringParameters.Snap,
                springStartTimeNanos = 0L,
            )
    }
}
