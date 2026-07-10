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

import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.ChangeSegmentHandlers.DirectionChangePreservesCurrentValue
import com.android.mechanics.spec.ChangeSegmentHandlers.PreventDirectionChangeWithinCurrentSegment
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.builder.Effect
import com.android.mechanics.spec.builder.EffectApplyScope
import com.android.mechanics.spec.builder.EffectPlacemenType
import com.android.mechanics.spec.builder.EffectPlacement
import com.android.mechanics.spec.with
import com.android.mechanics.spring.SpringParameters

/**
 * A gesture effect that toggles the output value between the placement's `start` and `end` values.
 *
 * The toggle action is triggered when the input changes by a specified fraction ([toggleFraction])
 * of the total input range, measured from the start of the effect.
 *
 * The logical state of the toggle is exposed via the SemanticKey [stateKey], and is either
 * [minState] or [maxState], based on the input gesture's progress.
 *
 * @param T The type of the state being toggled.
 * @property stateKey A [SemanticKey] used to identify the current state of the toggle (either
 *   [minState] or [maxState]).
 * @property minState The value representing the logical state when toggled to the `min` side.
 * @property minState The value representing the logical state when toggled to the `max` side.
 * @property restingValueKey A [SemanticKey] used to identify the resting value of the input.
 * @property toggleFraction The fraction of the input range (between `minLimit` and `maxLimit` of
 *   the effect placement) at which the toggle action occurs. For example, a value of 0.7 means the
 *   toggle happens when the input has covered 70% of the distance from `minLimit` towards
 *   `maxLimit`.
 * @property preToggleScale A scaling factor applied to the output value *before* the toggle point
 *   is reached. This controls how much the output changes leading up to the toggle.
 * @property postToggleScale A scaling factor applied to the output value *after* the toggle point
 *   is reached. This controls the initial change in output immediately after toggling.
 * @property spring The [SpringParameters] used for the animation when the toggle action occurs.
 *   This defines the physics of the transition between states.
 */
class Toggle<T>(
    private val stateKey: SemanticKey<T>,
    private val minState: T,
    private val maxState: T,
    private val restingValueKey: SemanticKey<Float?> = CommonSemantics.RestingValueKey,
    private val toggleFraction: Float = Defaults.ToggleFraction,
    private val preToggleScale: Float = Defaults.PreToggleScale,
    private val postToggleScale: Float = Defaults.PostToggleScale,
    private val spring: SpringParameters = Defaults.Spring,
) : Effect.PlaceableBetween {

    override fun EffectApplyScope.createSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
        placement: EffectPlacement,
    ) {
        check(placement.type == EffectPlacemenType.Between)
        val minValue = baseValue(minLimit)
        val maxValue = baseValue(maxLimit)
        val valueRange = maxValue - minValue

        val distance = maxLimit - minLimit

        val minTargetSemantics = listOf(restingValueKey with minValue, stateKey with minState)
        val maxTargetSemantics = listOf(restingValueKey with maxValue, stateKey with maxState)

        val toggleKey = BreakpointKey("toggle")

        val forwardTogglePos = minLimit + distance * toggleFraction
        forward(
            initialMapping =
                Mapping.Linear(
                    minLimit,
                    minValue,
                    forwardTogglePos,
                    minValue + valueRange * preToggleScale,
                ),
            semantics = minTargetSemantics,
        ) {
            target(
                forwardTogglePos,
                from = maxValue - valueRange * postToggleScale,
                to = maxValue,
                spring = spring,
                semantics = maxTargetSemantics,
                key = toggleKey,
                guarantee = Guarantee.GestureDragDelta(distance * 2),
            )
        }

        val reverseTogglePos = minLimit + distance * (1 - toggleFraction)
        backward(
            initialMapping =
                Mapping.Linear(
                    minLimit,
                    minValue,
                    reverseTogglePos,
                    minValue + valueRange * postToggleScale,
                ),
            semantics = minTargetSemantics,
        ) {
            target(
                reverseTogglePos,
                from = maxValue - valueRange * preToggleScale,
                to = maxValue,
                spring = spring,
                key = toggleKey,
                semantics = maxTargetSemantics,
                guarantee = Guarantee.GestureDragDelta(distance * 2),
            )
        }

        // Before toggling, suppress direction change
        addSegmentHandler(
            SegmentKey(minLimitKey, toggleKey, InputDirection.Max),
            PreventDirectionChangeWithinCurrentSegment,
        )
        addSegmentHandler(
            SegmentKey(toggleKey, maxLimitKey, InputDirection.Min),
            PreventDirectionChangeWithinCurrentSegment,
        )

        // after toggling, ensure a direction change does
        addSegmentHandler(
            SegmentKey(toggleKey, maxLimitKey, InputDirection.Max),
            DirectionChangePreservesCurrentValue,
        )

        addSegmentHandler(
            SegmentKey(minLimitKey, toggleKey, InputDirection.Min),
            DirectionChangePreservesCurrentValue,
        )
    }

    object Defaults {
        val ToggleFraction = 0.7f
        val PreToggleScale = 0.2f
        val PostToggleScale = 0.01f
        val Spring = SpringParameters(stiffness = 800f, dampingRatio = 0.95f)
    }
}

/**
 * Convenience implementation of a [Toggle] effect for an expanding / collapsing element.
 *
 * This object provides a pre-configured [Toggle] specifically designed for elements that can be
 * expanded or collapsed. It exposes the logical expansion state via the semantic [IsExpandedKey].
 */
object ExpansionToggle {
    /** Semantic key for a boolean flag indicating whether the element is expanded. */
    val IsExpandedKey: SemanticKey<Boolean> = SemanticKey("IsToggleExpanded")

    /** Toggle effect with default values. */
    val Default = Toggle(IsExpandedKey, minState = false, maxState = true)
}
