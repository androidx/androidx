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

package com.android.mechanics.effects

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.builder.Effect
import com.android.mechanics.spec.builder.EffectApplyScope
import com.android.mechanics.spec.builder.EffectPlacement

/**
 * An effect that reveals a component in multiple phases as available space increases past specific
 * thresholds.
 *
 * This effect is designed for a tactile surface reveal, which is structured into distinct phases
 * based on the drag distance (input).
 *
 * Phase 0: Hidden
 * - Below the [phase1HeightMin] threshold, the component is not visible.
 *
 * Phase 1: Horizontal Expansion
 * - Triggers when the input reaches the [phase1HeightMin] threshold.
 * - Spans the input range from [phase1HeightMin] up to the threshold defined by
 *   [phase2HeightPercentStart].
 * - During this phase, the component becomes visible (e.g., fades in), expands horizontally
 *   (controlled by [phase1MarginX]), and has input resistance (defined by [phase1FractionalInput],
 *   default 0.5f).
 *
 * Phase 2: Vertical Expansion
 * - Begins when the input reaches the threshold defined by [phase2HeightPercentStart].
 * - Spans the remaining input range up to the `maxLimit`.
 * - The component's width is at its maximum, and this phase controls the vertical expansion.
 * - Input tracking is typically direct: [phase2FractionalInput] (default 1.0f) provides a 1:1
 *   mapping ("follows the finger").
 *
 * @param maxCornerSize Defines the maximum corner size.
 * @param guaranteeDistance A distance to ensure the spring displacement completes.
 * @param phase1HeightMin The *input threshold* (as a distance) at which Phase 1 (Horizontal
 *   Expansion) begins. Before this threshold, the component is hidden.
 * @param phase1MarginX The horizontal margin to apply during Phase 1. This is typically used by the
 *   layout to control the component's width during horizontal expansion.
 * @param phase1FractionalInput The input-to-output progress ratio for Phase 1 (e.g., 0.5f for
 *   resistance).
 * @param phase2HeightPercentStart A percentage (0.0f to 1.0f) of the total effect height
 *   (`maxHeight`) that defines the *input threshold* at which Phase 2 (Vertical Expansion) begins.
 *   This also marks the end of Phase 1.
 * @param phase2FractionalInput The input-to-output progress ratio for Phase 2 (e.g., 1.0f for 1:1
 *   tracking).
 */
data class VerticalTactileSurfaceRevealEffect(
    // Shared configurations
    val maxCornerSize: () -> Dp = { Defaults.MaxCornerSize },
    val guaranteeDistance: Dp = Defaults.GuaranteeDistance,

    // Phase 1: Horizontal expansion
    val phase1HeightMin: Dp = Defaults.Phase1HeightMin,
    val phase1MarginX: Dp = Defaults.Phase1MarginX,
    val phase1FractionalInput: Float = Defaults.Phase1FractionalInput,

    // Phase 2: Vertical expansion
    val phase2HeightPercentStart: Float = Defaults.Phase2HeightPercentStart,
    val phase2FractionalInput: Float = Defaults.Phase2FractionalInput,
) : Effect.PlaceableBetween {
    init {
        require(phase1HeightMin >= 0.dp)
        require(phase2HeightPercentStart in 0f..1f)
    }

    override fun EffectApplyScope.createSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
        placement: EffectPlacement,
    ) {
        val maxHeight = maxLimit - minLimit
        val guaranteeInput = Guarantee.InputDelta(guaranteeDistance.toPx())

        val phase1HeightMin = phase1HeightMin.toPx().fastCoerceAtMost(maxHeight)
        val phase2HeightMin =
            (maxHeight * phase2HeightPercentStart).fastCoerceIn(phase1HeightMin, maxHeight)

        unidirectional(initialMapping = Mapping.Zero) {
            before(mapping = Mapping.Zero, guarantee = guaranteeInput)

            fractionalInput(
                breakpoint = minLimit + phase1HeightMin,
                from = phase1HeightMin,
                fraction = phase1FractionalInput,
            )

            fractionalInput(
                breakpoint = minLimit + phase2HeightMin,
                from = phase2HeightMin,
                fraction = phase2FractionalInput,
                guarantee = guaranteeInput,
            )

            after(mapping = Mapping.Fixed(maxHeight), guarantee = guaranteeInput)
        }
    }

    object Defaults {
        val MaxCornerSize: Dp = 32.dp
        val GuaranteeDistance: Dp = 8.dp
        val Phase1HeightMin: Dp = 8.dp
        val Phase1MarginX: Dp = 32.dp
        const val Phase1FractionalInput: Float = 0.5f
        const val Phase2HeightPercentStart: Float = 0.5f
        const val Phase2FractionalInput: Float = 1f
    }
}
