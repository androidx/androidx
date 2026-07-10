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

import android.util.Log
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.lerp
import com.android.mechanics.MotionValue.Companion.TAG
import com.android.mechanics.haptics.BreakpointHaptics
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.SegmentData
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spring.SpringState
import com.android.mechanics.spring.calculateUpdatedState

internal abstract class Computations : CurrentFrameInput, LastFrameState, StaticConfig {
    internal class ComputedValues(
        val segment: SegmentData,
        val guarantee: GuaranteeState,
        val animation: DiscontinuityAnimation,
        val breakpointHaptics: BreakpointHaptics?,
    )

    // currentComputedValues input
    private var memoizedSpec: MotionSpec = MotionSpec.InitiallyUndefined
    private var memoizedInput: Float = Float.MIN_VALUE
    private var memoizedAnimationTimeNanos: Long = Long.MIN_VALUE
    private var memoizedDirection: InputDirection = InputDirection.Min

    // currentComputedValues output
    private var memoizedComputedValues: ComputedValues =
        ComputedValues(
            MotionSpec.InitiallyUndefined.segmentAtInput(memoizedInput, memoizedDirection),
            GuaranteeState.Inactive,
            DiscontinuityAnimation.None,
            BreakpointHaptics.None,
        )

    internal val currentComputedValues: ComputedValues
        get() {
            val currentSpec: MotionSpec = spec
            if (currentSpec == MotionSpec.InitiallyUndefined) {
                requireNoMotionSpecSet()
                return memoizedComputedValues
            }

            val currentInput: Float = currentInput
            val currentAnimationTimeNanos: Long = currentAnimationTimeNanos
            val currentDirection: InputDirection = currentDirection

            if (
                memoizedSpec == currentSpec &&
                    memoizedInput == currentInput &&
                    memoizedAnimationTimeNanos == currentAnimationTimeNanos &&
                    memoizedDirection == currentDirection
            ) {
                return memoizedComputedValues
            }

            val isInitialComputation = memoizedSpec == MotionSpec.InitiallyUndefined

            memoizedSpec = currentSpec
            memoizedInput = currentInput
            memoizedAnimationTimeNanos = currentAnimationTimeNanos
            memoizedDirection = currentDirection

            memoizedComputedValues =
                if (isInitialComputation) {
                    ComputedValues(
                        currentSpec.segmentAtInput(currentInput, currentDirection),
                        GuaranteeState.Inactive,
                        DiscontinuityAnimation.None,
                        BreakpointHaptics.None,
                    )
                } else {
                    val segment: SegmentData =
                        computeSegmentData(
                            spec = currentSpec,
                            input = currentInput,
                            direction = currentDirection,
                        )

                    val segmentChange: SegmentChangeType =
                        getSegmentChangeType(
                            segment = segment,
                            input = currentInput,
                            direction = currentDirection,
                        )

                    val guarantee: GuaranteeState =
                        computeGuaranteeState(
                            segment = segment,
                            segmentChange = segmentChange,
                            input = currentInput,
                        )

                    val animation: DiscontinuityAnimation =
                        computeAnimation(
                            segment = segment,
                            guarantee = guarantee,
                            segmentChange = segmentChange,
                            spec = currentSpec,
                            input = currentInput,
                            animationTimeNanos = currentAnimationTimeNanos,
                        )

                    val breakpointHaptics = computeBreakpointHaptics(segment, segmentChange)

                    ComputedValues(segment, guarantee, animation, breakpointHaptics)
                }
            return memoizedComputedValues
        }

    // currentSpringState input
    private var memoizedAnimation: DiscontinuityAnimation? = null
    private var memoizedTimeNanos: Long = Long.MIN_VALUE

    // currentSpringState output
    private var memoizedSpringState: SpringState = SpringState.AtRest

    val currentSpringState: SpringState
        get() {
            val animation = currentComputedValues.animation
            val timeNanos = currentAnimationTimeNanos
            if (memoizedAnimation == animation && memoizedTimeNanos == timeNanos) {
                return memoizedSpringState
            }
            memoizedAnimation = animation
            memoizedTimeNanos = timeNanos
            return computeSpringState(animation, timeNanos).also { memoizedSpringState = it }
        }

    val isSameSegmentAndAtRest: Boolean
        get() =
            lastSpringState == SpringState.AtRest &&
                lastSegment.spec == spec &&
                lastSegment.isValidForInput(currentInput, currentDirection)

    val computedOutput: Float
        get() =
            if (isSameSegmentAndAtRest) {
                lastSegment.mapping.map(currentInput)
            } else {
                computedOutputTarget + currentSpringState.displacement
            }

    val computedOutputTarget: Float
        get() =
            if (isSameSegmentAndAtRest) {
                lastSegment.mapping.map(currentInput)
            } else {
                currentComputedValues.segment.mapping.map(currentInput)
            }

    val computedIsStable: Boolean
        get() =
            if (isSameSegmentAndAtRest) {
                true
            } else {
                currentSpringState == SpringState.AtRest
            }

    /**
     * Determines if the output value is fixed.
     *
     * The output is considered fixed if the animation has settled and the input falls into a
     * segment with a [Mapping.Fixed], and that mapping's value has not changed from the previous
     * frame.
     */
    val computedIsOutputFixed: Boolean
        get() {
            if (lastSpringState != SpringState.AtRest) {
                // The spring is still settling.
                return false
            }

            val lastMapping = lastSegment.mapping
            if (lastMapping !is Mapping.Fixed) {
                // We need to compute a new output value.
                return false
            }

            val isSameSegment =
                lastSegment.spec == spec &&
                    lastSegment.isValidForInput(currentInput, currentDirection)

            return if (isSameSegment) {
                // We are in the same fixed-value segment as the last frame.
                true
            } else {
                val currentMapping = currentComputedValues.segment.mapping
                if (currentMapping is Mapping.Fixed) {
                    // Both old and new mappings are fixed. The output is only considered fixed if
                    // their target values are identical.
                    lastMapping.value == currentMapping.value
                } else {
                    // The new mapping isn't a fixed value.
                    false
                }
            }
        }

    fun <T> computedSemanticState(semanticKey: SemanticKey<T>): T? {
        return with(if (isSameSegmentAndAtRest) lastSegment else currentComputedValues.segment) {
            spec.semanticState(semanticKey, key)
        }
    }

    fun computeDirectMappedVelocity(frameDurationNanos: Long): Float {
        val directMappedDelta =
            if (
                lastSegment.spec == spec &&
                    lastSegment.isValidForInput(currentInput, currentDirection)
            ) {
                lastSegment.mapping.map(currentInput) - lastSegment.mapping.map(lastInput)
            } else {
                val springChange = currentSpringState.displacement - lastSpringState.displacement

                currentComputedValues.segment.mapping.map(currentInput) -
                    lastSegment.mapping.map(lastInput) + springChange
            }

        val frameDuration = frameDurationNanos / 1_000_000_000.0
        return (directMappedDelta / frameDuration).toFloat()
    }

    /**
     * The current segment, which defines the [Mapping] function used to transform the input to the
     * output.
     *
     * While both [spec] and [direction] remain the same, and [input] is within the segment (see
     * [SegmentData.isValidForInput]), this is [LastFrameState.lastSegment].
     *
     * Otherwise, [MotionSpec.onChangeSegment] is queried for an up-dated segment.
     */
    private fun computeSegmentData(
        spec: MotionSpec,
        input: Float,
        direction: InputDirection,
    ): SegmentData {
        val specChanged = lastSegment.spec != spec
        return if (specChanged || !lastSegment.isValidForInput(input, direction)) {
            spec.onChangeSegment(lastSegment, input, direction)
        } else {
            lastSegment
        }
    }

    /** Computes the [SegmentChangeType] between [LastFrameState.lastSegment] and [segment]. */
    private fun getSegmentChangeType(
        segment: SegmentData,
        input: Float,
        direction: InputDirection,
    ): SegmentChangeType {
        if (segment.key == lastSegment.key) {
            return SegmentChangeType.Same
        }

        if (
            segment.key.minBreakpoint == lastSegment.key.minBreakpoint &&
                segment.key.maxBreakpoint == lastSegment.key.maxBreakpoint
        ) {
            return SegmentChangeType.SameOppositeDirection
        }

        val currentSpec = segment.spec
        val lastSpec = lastSegment.spec
        if (currentSpec !== lastSpec) {
            // Determine/guess whether the segment change was due to the changed spec, or
            // whether lastSpec would return the same segment key for the update input.
            val lastSpecSegmentForSameInput = lastSpec.segmentAtInput(input, direction).key
            if (segment.key != lastSpecSegmentForSameInput) {
                // Note: this might not be correct if the new [MotionSpec.segmentHandlers] were
                // involved.
                return SegmentChangeType.Spec
            }
        }

        return if (segment.direction == lastSegment.direction) {
            SegmentChangeType.Traverse
        } else {
            SegmentChangeType.Direction
        }
    }

    /**
     * Computes the fraction of [position] between [lastInput] and [currentInput].
     *
     * Essentially, this determines fractionally when [position] was crossed, between the current
     * frame and the last frame.
     *
     * Since frames are updated periodically, not continuously, crossing a breakpoint happened
     * sometime between the last frame's start and this frame's start.
     *
     * This fraction is used to estimate the time when a breakpoint was crossed since last frame,
     * and simplifies the logic of crossing multiple breakpoints in one frame, as it offers the
     * springs and guarantees time to be updated correctly.
     *
     * Of course, this is a simplification that assumes the input velocity was uniform during the
     * last frame, but that is likely good enough.
     */
    private fun lastFrameFractionOfPosition(
        position: Float,
        lastInput: Float,
        input: Float,
    ): Float {
        return ((position - lastInput) / (input - lastInput)).fastCoerceIn(0f, 1f)
    }

    /**
     * The [GuaranteeState] for [segment].
     *
     * Without a segment change, this carries forward [lastGuaranteeState], adjusted to the new
     * input if needed.
     *
     * If a segment change happened, this is a new [GuaranteeState] for the [segment]. Any remaining
     * [LastFrameState.lastGuaranteeState] will be consumed in [currentAnimation].
     */
    private fun computeGuaranteeState(
        segment: SegmentData,
        segmentChange: SegmentChangeType,
        input: Float,
    ): GuaranteeState {
        val entryBreakpoint = segment.entryBreakpoint

        // First, determine the origin of the guarantee computations
        val guaranteeOriginState =
            when (segmentChange) {
                // Still in the segment, the origin is carried over from the last frame
                SegmentChangeType.Same -> lastGuaranteeState
                // The direction changed within the same segment, no guarantee to enforce.
                SegmentChangeType.SameOppositeDirection -> return GuaranteeState.Inactive
                // The spec changes, there is no guarantee associated with the animation.
                SegmentChangeType.Spec -> return GuaranteeState.Inactive
                SegmentChangeType.Direction -> {
                    // Direction changed over a segment boundary. To make up for the
                    // directionChangeSlop, the guarantee starts at the current input.
                    GuaranteeState.withStartValue(
                        when (entryBreakpoint.guarantee) {
                            is Guarantee.InputDelta -> input
                            is Guarantee.GestureDragDelta -> currentGestureDragOffset
                            is Guarantee.None -> return GuaranteeState.Inactive
                        }
                    )
                }

                SegmentChangeType.Traverse -> {
                    // Traversed over a segment boundary, the guarantee going forward is determined
                    // by the [entryBreakpoint].
                    GuaranteeState.withStartValue(
                        when (entryBreakpoint.guarantee) {
                            is Guarantee.InputDelta -> entryBreakpoint.position
                            is Guarantee.GestureDragDelta -> {
                                // Guess the GestureDragDelta origin - since the gesture dragOffset
                                // is sampled, interpolate it according to when the breakpoint was
                                // crossed in the last frame.
                                val fractionalBreakpointPos =
                                    lastFrameFractionOfPosition(
                                        entryBreakpoint.position,
                                        lastInput,
                                        input,
                                    )

                                lerp(
                                    lastGestureDragOffset,
                                    currentGestureDragOffset,
                                    fractionalBreakpointPos,
                                )
                            }

                            // No guarantee to enforce.
                            is Guarantee.None -> return GuaranteeState.Inactive
                        }
                    )
                }
            }

        // Finally, update the origin state with the current guarantee value.
        return guaranteeOriginState.withCurrentValue(
            when (entryBreakpoint.guarantee) {
                is Guarantee.InputDelta -> input
                is Guarantee.GestureDragDelta -> currentGestureDragOffset
                is Guarantee.None -> return GuaranteeState.Inactive
            },
            segment.direction,
        )
    }

    /**
     * The [DiscontinuityAnimation] in effect for the current frame.
     *
     * This describes the starting condition of the spring animation, and is only updated if the
     * spring animation must restarted: that is, if yet another discontinuity must be animated as a
     * result of a segment change, or if the [guarantee] requires the spring to be tightened.
     *
     * See [currentSpringState] for the continuously updated, animated spring values.
     */
    private fun computeAnimation(
        segment: SegmentData,
        guarantee: GuaranteeState,
        segmentChange: SegmentChangeType,
        spec: MotionSpec,
        input: Float,
        animationTimeNanos: Long,
    ): DiscontinuityAnimation {
        return when (segmentChange) {
            SegmentChangeType.Same -> {
                if (lastSpringState == SpringState.AtRest) {
                    // Nothing to update if no animation is ongoing
                    DiscontinuityAnimation.None
                } else if (lastGuaranteeState == guarantee) {
                    // Nothing to update if the spring must not be tightened.
                    lastAnimation
                } else {
                    // Compute the updated spring parameters
                    val tightenedSpringParameters =
                        guarantee.updatedSpringParameters(segment.entryBreakpoint)

                    lastAnimation.copy(
                        springStartState = lastSpringState,
                        springParameters = tightenedSpringParameters,
                        springStartTimeNanos = lastFrameTimeNanos,
                    )
                }
            }

            SegmentChangeType.SameOppositeDirection,
            SegmentChangeType.Direction,
            SegmentChangeType.Spec -> {
                // Determine the delta in the output, as produced by the old and new mapping.
                val currentMapping = segment.mapping.map(input)
                val lastMapping = lastSegment.mapping.map(input)
                val delta = currentMapping - lastMapping

                val deltaIsFinite = delta.fastIsFinite()
                if (!deltaIsFinite) {
                    Log.wtf(
                        TAG,
                        "Delta between mappings is undefined!\n" +
                            "  MotionValue: $label\n" +
                            "  input: $input\n" +
                            "  lastMapping: $lastMapping (lastSegment: $lastSegment)\n" +
                            "  currentMapping: $currentMapping (currentSegment: $segment)",
                    )
                }

                if (delta == 0f || !deltaIsFinite) {
                    // Nothing new to animate.
                    lastAnimation
                } else {
                    val springParameters =
                        if (segmentChange == SegmentChangeType.Direction) {
                            segment.entryBreakpoint.spring
                        } else {
                            spec.resetSpring
                        }

                    val newTarget = delta - lastSpringState.displacement
                    DiscontinuityAnimation(
                        SpringState(-newTarget, lastSpringState.velocity + directMappedVelocity),
                        springParameters,
                        lastFrameTimeNanos,
                    )
                }
            }

            SegmentChangeType.Traverse -> {
                // Process all breakpoints traversed, in order.
                // This is involved due to the guarantees - they have to be applied, one after the
                // other, before crossing the next breakpoint.
                val currentDirection = segment.direction

                with(spec[currentDirection]) {
                    val targetIndex = findSegmentIndex(segment.key)
                    val sourceIndex = findSegmentIndex(lastSegment.key)
                    check(targetIndex != sourceIndex)

                    val directionOffset = if (targetIndex > sourceIndex) 1 else -1

                    var lastBreakpoint = lastSegment.entryBreakpoint
                    var lastAnimationTime = lastFrameTimeNanos
                    var guaranteeState = lastGuaranteeState
                    var springState = lastSpringState
                    var springParameters = lastAnimation.springParameters
                    var initialSpringVelocity = directMappedVelocity

                    var segmentIndex = sourceIndex
                    while (segmentIndex != targetIndex) {
                        val nextBreakpoint =
                            breakpoints[segmentIndex + directionOffset.fastCoerceAtLeast(0)]

                        val nextBreakpointFrameFraction =
                            lastFrameFractionOfPosition(nextBreakpoint.position, lastInput, input)

                        val nextBreakpointCrossTime =
                            lerp(
                                lastFrameTimeNanos,
                                animationTimeNanos,
                                nextBreakpointFrameFraction,
                            )
                        if (
                            guaranteeState != GuaranteeState.Inactive &&
                                springState != SpringState.AtRest
                        ) {
                            val guaranteeValueAtNextBreakpoint =
                                when (lastBreakpoint.guarantee) {
                                    is Guarantee.InputDelta -> nextBreakpoint.position
                                    is Guarantee.GestureDragDelta ->
                                        lerp(
                                            lastGestureDragOffset,
                                            currentGestureDragOffset,
                                            nextBreakpointFrameFraction,
                                        )

                                    is Guarantee.None ->
                                        error(
                                            "guaranteeState ($guaranteeState) is not Inactive, guarantee is missing"
                                        )
                                }

                            guaranteeState =
                                guaranteeState.withCurrentValue(
                                    guaranteeValueAtNextBreakpoint,
                                    currentDirection,
                                )

                            springParameters =
                                guaranteeState.updatedSpringParameters(lastBreakpoint)
                        }

                        springState =
                            springState.calculateUpdatedState(
                                nextBreakpointCrossTime - lastAnimationTime,
                                springParameters,
                            )
                        lastAnimationTime = nextBreakpointCrossTime

                        val mappingBefore = mappings[segmentIndex]
                        val beforeBreakpoint = mappingBefore.map(nextBreakpoint.position)
                        val mappingAfter = mappings[segmentIndex + directionOffset]
                        val afterBreakpoint = mappingAfter.map(nextBreakpoint.position)

                        val delta = afterBreakpoint - beforeBreakpoint
                        val deltaIsFinite = delta.fastIsFinite()
                        if (deltaIsFinite) {
                            if (delta != 0f) {
                                // There is a discontinuity on this breakpoint, that needs to be
                                // animated. The delta is pushed to the spring, to consume the
                                // discontinuity over time.
                                springState =
                                    springState.nudge(
                                        displacementDelta = -delta,
                                        velocityDelta = initialSpringVelocity,
                                    )

                                // When *first* crossing a discontinuity in a given frame, the
                                // static mapped velocity observed during previous frame is added as
                                // initial velocity to the spring. This is done ot most once per
                                // frame, and only if there is an actual discontinuity.
                                initialSpringVelocity = 0f
                            }
                        } else {
                            // The before and / or after mapping produced an non-finite number,
                            // which is not allowed. This intentionally crashes eng-builds, since
                            // it's a bug in the Mapping implementation that must be fixed. On
                            // regular builds, it will likely cause a jumpcut.
                            Log.wtf(
                                TAG,
                                "Delta between breakpoints is undefined!\n" +
                                    "  MotionValue: ${label}\n" +
                                    "  position: ${nextBreakpoint.position}\n" +
                                    "  before: $beforeBreakpoint (mapping: $mappingBefore)\n" +
                                    "  after: $afterBreakpoint (mapping: $mappingAfter)",
                            )
                        }

                        segmentIndex += directionOffset
                        lastBreakpoint = nextBreakpoint
                        guaranteeState =
                            when (nextBreakpoint.guarantee) {
                                is Guarantee.InputDelta ->
                                    GuaranteeState.withStartValue(nextBreakpoint.position)

                                is Guarantee.GestureDragDelta ->
                                    GuaranteeState.withStartValue(
                                        lerp(
                                            lastGestureDragOffset,
                                            currentGestureDragOffset,
                                            nextBreakpointFrameFraction,
                                        )
                                    )

                                is Guarantee.None -> GuaranteeState.Inactive
                            }
                    }

                    val tightened = guarantee.updatedSpringParameters(segment.entryBreakpoint)

                    DiscontinuityAnimation(springState, tightened, lastAnimationTime)
                }
            }
        }
    }

    private fun computeSpringState(
        animation: DiscontinuityAnimation,
        timeNanos: Long,
    ): SpringState {
        with(animation) {
            if (isAtRest) return SpringState.AtRest

            val nanosSinceAnimationStart = timeNanos - springStartTimeNanos
            val updatedSpringState =
                springStartState.calculateUpdatedState(nanosSinceAnimationStart, springParameters)

            return if (updatedSpringState.isStable(springParameters, stableThreshold)) {
                SpringState.AtRest
            } else {
                updatedSpringState
            }
        }
    }

    private fun computeBreakpointHaptics(
        segment: SegmentData,
        segmentChange: SegmentChangeType,
    ): BreakpointHaptics? =
        when (segmentChange) {
            SegmentChangeType.Traverse -> segment.entryBreakpoint.breakpointHaptics
            else -> null
        }

    /**
     * Precondition to ensure that this [Computations] has not yet been initialized with a
     * MotionSpec other than [MotionSpec.InitiallyUndefined].
     *
     * This precondition is added since the desired behavior of the MotionValue when toggling back
     * to a [MotionSpec.InitiallyUndefined] spec is unclear. If there is a compelling usecase, this
     * restriction could be lifted.
     */
    private fun requireNoMotionSpecSet() {
        // A MotionValue's spec can be MotionValue.Undefined initially. However, once a real spec
        // has been set, it cannot be changed back to MotionValue.Undefined.

        require(memoizedSpec == MotionSpec.InitiallyUndefined) {
            // memoizedSpec is only ever Undefined initially, before a motionSpec was set.
            //  This is used as a signal to detect if a user switches back to Undefined.
            "MotionSpec must not be changed back to undefined!\n" +
                " MotionValue: $label\n" +
                " last MotionSpec: $memoizedSpec"
        }

        // memoizedComputedValues must not have been reassigned either.
        require(
            with(memoizedComputedValues) {
                segment.spec == MotionSpec.InitiallyUndefined &&
                    guarantee == GuaranteeState.Inactive &&
                    animation == DiscontinuityAnimation.None
            }
        )
    }
}
