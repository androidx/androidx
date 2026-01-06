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

import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters

/**
 * Creates a [MotionSpec] for a spatial value.
 *
 * The [baseMapping] is [Mapping.Identity], and the Material spatial.default spring is used unless
 * otherwise specified.
 *
 * @see motionSpec
 */
fun MotionBuilderContext.spatialMotionSpec(
    baseMapping: Mapping = Mapping.Identity,
    defaultSpring: SpringParameters = this.spatial.default,
    resetSpring: SpringParameters = defaultSpring,
    semantics: List<SemanticValue<*>> = emptyList(),
    init: MotionSpecBuilderScope.() -> Unit,
) = motionSpec(baseMapping, defaultSpring, resetSpring, semantics, init)

/**
 * Creates a [MotionSpec] for an effects value.
 *
 * The [baseMapping] is [Mapping.Zero], and the Material effects.default spring is used unless
 * otherwise specified.
 *
 * @see motionSpec
 */
fun MotionBuilderContext.effectsMotionSpec(
    baseMapping: Mapping = Mapping.Zero,
    defaultSpring: SpringParameters = this.effects.default,
    resetSpring: SpringParameters = defaultSpring,
    semantics: List<SemanticValue<*>> = emptyList(),
    init: MotionSpecBuilderScope.() -> Unit,
) = motionSpec(baseMapping, defaultSpring, resetSpring, semantics, init)

/**
 * Creates a [MotionSpec], based on reusable effects.
 *
 * @param baseMapping The mapping in used for segments where no [Effect] is specified.
 * @param defaultSpring The [DirectionalBuilderScope.defaultSpring], used for all discontinuities
 *   unless otherwise specified.
 * @param resetSpring spring parameters to animate a difference in output, if the difference is
 *   caused by setting this new spec.
 * @param semantics initial semantics that apply before of effects override them.
 * @param init
 */
fun MotionBuilderContext.motionSpec(
    baseMapping: Mapping,
    defaultSpring: SpringParameters,
    resetSpring: SpringParameters = defaultSpring,
    semantics: List<SemanticValue<*>> = emptyList(),
    init: MotionSpecBuilderScope.() -> Unit,
): MotionSpec {
    return MotionSpecBuilderImpl(
            baseMapping,
            defaultSpring,
            resetSpring,
            semantics,
            motionBuilderContext = this,
        )
        .apply(init)
        .build()
}

/**
 * Creates a [MotionSpec] producing a fixed output value, no matter the [MotionValues]'s input.
 *
 * The Material spatial.default spring is used to animate to the fixed output value.
 *
 * @see fixedValueSpec
 */
fun MotionBuilderContext.fixedSpatialValueSpec(
    value: Float,
    resetSpring: SpringParameters = this.spatial.default,
    semantics: List<SemanticValue<*>> = emptyList(),
) = fixedValueSpec(value, resetSpring, semantics)

/**
 * Creates a [MotionSpec] producing a fixed output value, no matter the [MotionValues]'s input.
 *
 * The Material effects.default spring is used to animate to the fixed output value.
 *
 * @see fixedValueSpec
 */
fun MotionBuilderContext.fixedEffectsValueSpec(
    value: Float,
    resetSpring: SpringParameters = this.effects.default,
    semantics: List<SemanticValue<*>> = emptyList(),
) = fixedValueSpec(value, resetSpring, semantics)

/**
 * Creates a [MotionSpec] producing a fixed output value, no matter the [MotionValues]'s input.
 *
 * @param value The fixed output value.
 * @param resetSpring spring parameters to animate to the fixed output value.
 * @param semantics for this spec.
 */
fun MotionBuilderContext.fixedValueSpec(
    value: Float,
    resetSpring: SpringParameters,
    semantics: List<SemanticValue<*>> = emptyList(),
): MotionSpec {
    return MotionSpec(
        directionalMotionSpec(Mapping.Fixed(value)),
        resetSpring = resetSpring,
        semantics = semantics,
    )
}

/** Defines the contract placing [Effect]s within a [MotionSpecBuilder] */
interface MotionSpecBuilderScope : MotionBuilderContext {

    /**
     * Places [effect] between [start] and [end].
     *
     * If `start > end`, the effect will be reversed when applied. The [effect] can overrule the
     * `end` position with [Effect.measure].
     */
    fun between(start: Float, end: Float, effect: Effect.PlaceableBetween): PlacedEffect

    /**
     * Places [effect] at position, extending backwards.
     *
     * The effect will be reversed when applied.
     */
    fun before(position: Float, effect: Effect.PlaceableBefore): PlacedEffect

    /** Places [effect] at position, extending forward. */
    fun after(position: Float, effect: Effect.PlaceableAfter): PlacedEffect

    /**
     * Places [effect] at [otherEffect]'s min position, extending backwards.
     *
     * The effect will be reversed when applied.
     */
    fun before(otherEffect: PlacedEffect, effect: Effect.PlaceableBefore): PlacedEffect

    /** Places [effect] after the end of [otherEffect], extending forward. */
    fun after(otherEffect: PlacedEffect, effect: Effect.PlaceableAfter): PlacedEffect

    /** Places [effect] at position. */
    fun at(position: Float, effect: Effect.PlaceableAt): PlacedEffect
}
