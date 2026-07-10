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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.effects

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.mechanics.haptics.BreakpointHaptics
import com.android.mechanics.haptics.HapticsExperimentalApi
import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.ChangeSegmentHandlers.DirectionChangePreservesCurrentValue
import com.android.mechanics.spec.ChangeSegmentHandlers.PreventDirectionChangeWithinCurrentSegment
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.builder.Effect
import com.android.mechanics.spec.builder.EffectApplyScope
import com.android.mechanics.spec.builder.EffectPlacemenType
import com.android.mechanics.spec.builder.EffectPlacement
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spec.with
import com.android.mechanics.spring.SpringParameters

/** Default values for the original MagneticDetach implementation. */
object MagneticDetach {
    object Defaults {
        val AttachDetachState = SemanticKey<State>(debugLabel = "AttachDetachState")
        val AttachedValue = SemanticKey<Float?>(debugLabel = "AttachedValue")
        val AttachDetachScale = .3f
        val DetachPosition = 80.dp
        val AttachPosition = 40.dp
        val Spring = SpringParameters(stiffness = 800f, dampingRatio = 0.95f)
    }

    enum class State {
        Attached,
        Detached,
    }
}

/**
 * Gesture effect that emulates effort to detach an element from its resting position.
 *
 * @param semanticState semantic state used to check the state of this effect.
 * @param detachPosition distance from the origin to detach
 * @param attachPosition distance from the origin to re-attach
 * @param detachScale fraction of input changes propagated during detach.
 * @param attachScale fraction of input changes propagated after re-attach.
 * @param detachSpring spring used during detach
 * @param attachSpring spring used during attach
 *
 * TODO: b/448605986 - align this factory functions and the above defaults with the API best
 *   practices. This indirection is here to allow extracting a generic MagneticDetachEffect, while
 *   not having to change all call-sites in one CL.
 */
fun MagneticDetach(
    semanticState: SemanticKey<MagneticDetach.State> = MagneticDetach.Defaults.AttachDetachState,
    semanticAttachedValue: SemanticKey<Float?> = MagneticDetach.Defaults.AttachedValue,
    detachPosition: Dp = MagneticDetach.Defaults.DetachPosition,
    attachPosition: Dp = MagneticDetach.Defaults.AttachPosition,
    detachScale: Float = MagneticDetach.Defaults.AttachDetachScale,
    attachScale: Float =
        MagneticDetach.Defaults.AttachDetachScale * (attachPosition / detachPosition),
    detachSpring: SpringParameters = MagneticDetach.Defaults.Spring,
    attachSpring: SpringParameters = MagneticDetach.Defaults.Spring,
    enableHaptics: Boolean = false,
): MagneticDetachEffect<MagneticDetach.State> =
    MagneticDetachEffect(
        semanticState,
        MagneticDetach.State.Attached,
        MagneticDetach.State.Detached,
        semanticAttachedValue,
        detachPosition,
        attachPosition,
        detachScale,
        attachScale,
        detachSpring,
        attachSpring,
        enableHaptics,
    )

/**
 * Gesture effect that emulates effort to detach an element from its resting position. *
 *
 * @param attachedStateKey semantic state key on whether the gesture is past the detach threshold.
 * @param attachedState value for [attachedStateKey] when attached
 * @param detachedState value for [attachedStateKey] when detached
 * @param restingValueKey semantic state for the input value the gesture would want to go back to.
 * @param detachPosition distance from the origin to detach
 * @param attachPosition distance from the origin to re-attach
 * @param detachScale fraction of input changes propagated during detach.
 * @param attachScale fraction of input changes propagated after re-attach.
 * @param detachSpring spring used during detach
 * @param attachSpring spring used during attach
 */
class MagneticDetachEffect<T>(
    private val attachedStateKey: SemanticKey<T>,
    private val attachedState: T,
    private val detachedState: T,
    private val restingValueKey: SemanticKey<Float?> = CommonSemantics.RestingValueKey,
    private val detachPosition: Dp = MagneticDetach.Defaults.DetachPosition,
    private val attachPosition: Dp = MagneticDetach.Defaults.AttachPosition,
    private val detachScale: Float = MagneticDetach.Defaults.AttachDetachScale,
    private val attachScale: Float =
        MagneticDetach.Defaults.AttachDetachScale * (attachPosition / detachPosition),
    private val detachSpring: SpringParameters = MagneticDetach.Defaults.Spring,
    private val attachSpring: SpringParameters = MagneticDetach.Defaults.Spring,
    private val enableHaptics: Boolean = false,
) : Effect.PlaceableAfter, Effect.PlaceableBefore {

    init {
        require(attachPosition <= detachPosition)
    }

    override fun MotionBuilderContext.intrinsicSize(): Float {
        return detachPosition.toPx()
    }

    override fun EffectApplyScope.createSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
        placement: EffectPlacement,
    ) {
        if (placement.type == EffectPlacemenType.Before) {
            createPlacedBeforeSpec(minLimit, minLimitKey, maxLimit, maxLimitKey)
        } else {
            check(placement.type == EffectPlacemenType.After)
            createPlacedAfterSpec(minLimit, minLimitKey, maxLimit, maxLimitKey)
        }
    }

    /* Effect is attached at minLimit, and detaches at maxLimit. */
    @OptIn(HapticsExperimentalApi::class)
    private fun EffectApplyScope.createPlacedAfterSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
    ) {
        val attachedValue = baseValue(minLimit)
        val detachedValue = baseValue(maxLimit)
        val reattachPos = minLimit + attachPosition.toPx()
        val reattachValue = baseValue(reattachPos)

        val attachedSemantics =
            listOf(attachedStateKey with attachedState, restingValueKey with attachedValue)
        val detachedSemantics =
            listOf(attachedStateKey with detachedState, restingValueKey with null)

        val scaledDetachValue = attachedValue + (detachedValue - attachedValue) * detachScale
        val scaledReattachValue = attachedValue + (reattachValue - attachedValue) * attachScale

        // Haptic specs
        val tensionHaptics =
            if (enableHaptics) {
                SegmentHaptics.SpringTension(anchorPointPx = minLimit)
            } else {
                SegmentHaptics.None
            }
        val thresholdHaptics =
            if (enableHaptics) {
                BreakpointHaptics.GenericThreshold
            } else {
                BreakpointHaptics.None
            }

        val attachKey = BreakpointKey("attach")

        forward(
            initialMapping = Mapping.Linear(minLimit, attachedValue, maxLimit, scaledDetachValue),
            initialSegmentHaptics = tensionHaptics,
            semantics = attachedSemantics,
        ) {
            after(
                spring = detachSpring,
                semantics = detachedSemantics,
                breakpointHaptics = thresholdHaptics,
            )
            before(semantics = listOf(restingValueKey with null))
        }

        backward(
            initialMapping =
                Mapping.Linear(minLimit, attachedValue, reattachPos, scaledReattachValue),
            semantics = attachedSemantics,
        ) {
            mapping(
                breakpoint = reattachPos,
                key = attachKey,
                spring = attachSpring,
                semantics = detachedSemantics,
                mapping = baseMapping,
                breakpointHaptics = thresholdHaptics,
            )
            before(semantics = listOf(restingValueKey with null))
            after(semantics = listOf(restingValueKey with null))
        }

        addSegmentHandlers(
            beforeDetachSegment = SegmentKey(minLimitKey, maxLimitKey, InputDirection.Max),
            beforeAttachSegment = SegmentKey(attachKey, maxLimitKey, InputDirection.Min),
            afterAttachSegment = SegmentKey(minLimitKey, attachKey, InputDirection.Min),
        )
    }

    /* Effect is attached at maxLimit, and detaches at minLimit. */
    private fun EffectApplyScope.createPlacedBeforeSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
    ) {
        val attachedValue = baseValue(maxLimit)
        val detachedValue = baseValue(minLimit)
        val reattachPos = maxLimit - attachPosition.toPx()
        val reattachValue = baseValue(reattachPos)

        val attachedSemantics =
            listOf(attachedStateKey with attachedState, restingValueKey with attachedValue)
        val detachedSemantics =
            listOf(attachedStateKey with detachedState, restingValueKey with null)

        val scaledDetachValue = attachedValue + (detachedValue - attachedValue) * detachScale
        val scaledReattachValue = attachedValue + (reattachValue - attachedValue) * attachScale

        val attachKey = BreakpointKey("attach")

        backward(
            initialMapping = Mapping.Linear(minLimit, scaledDetachValue, maxLimit, attachedValue),
            semantics = attachedSemantics,
        ) {
            before(spring = detachSpring, semantics = detachedSemantics)
            after(semantics = listOf(restingValueKey with null))
        }

        forward(initialMapping = baseMapping, semantics = detachedSemantics) {
            target(
                breakpoint = reattachPos,
                key = attachKey,
                from = scaledReattachValue,
                to = attachedValue,
                spring = attachSpring,
                semantics = attachedSemantics,
            )
            after(semantics = listOf(restingValueKey with null))
        }

        addSegmentHandlers(
            beforeDetachSegment = SegmentKey(minLimitKey, maxLimitKey, InputDirection.Min),
            beforeAttachSegment = SegmentKey(minLimitKey, attachKey, InputDirection.Max),
            afterAttachSegment = SegmentKey(attachKey, maxLimitKey, InputDirection.Max),
        )
    }

    private fun EffectApplyScope.addSegmentHandlers(
        beforeDetachSegment: SegmentKey,
        beforeAttachSegment: SegmentKey,
        afterAttachSegment: SegmentKey,
    ) {
        // Suppress direction change during detach. This prevents snapping to the origin when
        // changing the direction while detaching.
        addSegmentHandler(beforeDetachSegment, PreventDirectionChangeWithinCurrentSegment)
        // Suppress direction when approaching attach. This prevents the detach effect when changing
        // direction just before reattaching.
        addSegmentHandler(beforeAttachSegment, PreventDirectionChangeWithinCurrentSegment)

        // When changing direction after re-attaching, the pre-detach ratio is tweaked to
        // interpolate between the direction change-position and the detach point.
        addSegmentHandler(afterAttachSegment, DirectionChangePreservesCurrentValue)
    }
}
