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

package com.android.mechanics.spec.builder

import com.android.mechanics.haptics.BreakpointHaptics
import com.android.mechanics.haptics.HapticsExperimentalApi
import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters

/** Builder function signature. */
typealias DirectionalBuilderFn = DirectionalBuilderScope.() -> CanBeLastSegment

/**
 * Defines the contract for building a [DirectionalMotionSpec].
 *
 * Provides methods to define breakpoints and mappings for the motion specification.
 */
interface DirectionalBuilderScope {
    /** The default [SpringParameters] used for breakpoints. */
    val defaultSpring: SpringParameters

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to
     * linearly interpolate from a starting value ([from]) to the desired target value ([to]).
     *
     * Note: This segment cannot be used as the last segment in the specification, as it requires a
     * subsequent breakpoint to define the target value.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param from The output value at the previous breakpoint, explicitly setting the starting
     *   point for the linear mapping.
     * @param to The desired output value at the new breakpoint.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun target(
        breakpoint: Float,
        from: Float,
        to: Float,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    )

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to
     * linearly interpolate from the current output value (optionally with an offset of [delta]) to
     * the desired target value ([to]).
     *
     * Note: This segment cannot be used as the last segment in the specification, as it requires a
     * subsequent breakpoint to define the target value.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param to The desired output value at the new breakpoint.
     * @param delta An optional offset to apply to the calculated starting value. Defaults to 0f.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun targetFromCurrent(
        breakpoint: Float,
        to: Float,
        delta: Float = 0f,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    )

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to
     * linearly interpolate from a starting value ([from]) and then continue with a fractional input
     * ([fraction]).
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param from The output value at the previous breakpoint, explicitly setting the starting
     *   point for the linear mapping.
     * @param fraction The fractional multiplier applied to the input difference between
     *   breakpoints.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun fractionalInput(
        breakpoint: Float,
        from: Float,
        fraction: Float,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    ): CanBeLastSegment

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to
     * linearly interpolate from the current output value (optionally with an offset of [delta]) and
     * then continue with a fractional input ([fraction]).
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param fraction The fractional multiplier applied to the input difference between
     *   breakpoints.
     * @param delta An optional offset to apply to the calculated starting value. Defaults to 0f.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun fractionalInputFromCurrent(
        breakpoint: Float,
        fraction: Float,
        delta: Float = 0f,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    ): CanBeLastSegment

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to output
     * a fixed value ([value]).
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param value The constant output value for this segment.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun fixedValue(
        breakpoint: Float,
        value: Float,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    ): CanBeLastSegment

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to output
     * a constant value derived from the current output value (optionally with an offset of
     * [delta]).
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param delta An optional offset to apply to the mapped value to determine the fixed value.
     *   Defaults to 0f.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun fixedValueFromCurrent(
        breakpoint: Float,
        delta: Float = 0f,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    ): CanBeLastSegment

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment using the
     * provided [mapping].
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param spring The [SpringParameters] for the transition to this breakpoint. Defaults to
     *   [defaultSpring].
     * @param guarantee The animation guarantee for this transition. Defaults to [Guarantee.None].
     * @param key A unique [BreakpointKey] for this breakpoint. Defaults to a newly generated key.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param mapping The custom [Mapping] to use.
     */
    fun mapping(
        breakpoint: Float,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        mapping: Mapping,
    ): CanBeLastSegment

    /**
     * Ends the current segment at the [breakpoint] position and defines the next segment to produce
     * the input value as output (optionally with an offset of [delta]).
     *
     * Note: This segment can be used as the last segment in the specification.
     *
     * @param breakpoint The breakpoint defining the end of the current segment and the start of the
     *   next.
     * @param delta An optional offset to apply to the mapped value to determine the fixed value.
     * @param breakpointHaptics Haptics at the breakpoint that ends the current segment.
     * @param spring The [SpringParameters] for the transition to this breakpoint.
     * @param guarantee The animation guarantee for this transition.
     * @param key A unique [BreakpointKey] for this breakpoint.
     * @param semantics Updated semantics values to be applied. Must be a subset of the
     *   [SemanticKey]s used when first creating this builder.
     */
    fun identity(
        breakpoint: Float,
        delta: Float = 0f,
        breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
        spring: SpringParameters = defaultSpring,
        guarantee: Guarantee = Guarantee.None,
        key: BreakpointKey = BreakpointKey(),
        semantics: List<SemanticValue<*>> = emptyList(),
    ): CanBeLastSegment {
        return if (delta == 0f) {
            mapping(
                breakpoint,
                spring,
                guarantee,
                key,
                semantics,
                breakpointHaptics,
                Mapping.Identity,
            )
        } else {
            fractionalInput(
                breakpoint,
                fraction = 1f,
                breakpointHaptics = breakpointHaptics,
                from = breakpoint + delta,
                spring = spring,
                guarantee = guarantee,
                key = key,
                semantics = semantics,
            )
        }
    }

    /**
     * Builds the [DirectionalMotionSpec] according to the given [block] with the given
     * [SegmentHaptics].
     *
     * Within the block, one or more segments can be defined and the same type of haptics will be
     * delivered during interactions with the segments.
     */
    @HapticsExperimentalApi
    fun <T> haptics(segmentHaptics: SegmentHaptics, block: DirectionalBuilderScope.() -> T)
}

/** Marker interface to indicate that a segment can be the last one in a [DirectionalMotionSpec]. */
sealed interface CanBeLastSegment
