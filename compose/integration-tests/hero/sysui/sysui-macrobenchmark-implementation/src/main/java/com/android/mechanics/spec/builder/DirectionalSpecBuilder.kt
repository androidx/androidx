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

import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SegmentSemanticValues
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters

/**
 * Builds a [DirectionalMotionSpec] for spatial values by defining a sequence of ([Breakpoint],
 * [Mapping]) pairs
 *
 * The [initialMapping] is [Mapping.Identity], and the Material spatial.default spring is used,
 * unless otherwise specified.
 *
 * @see directionalMotionSpec
 */
fun MotionBuilderContext.spatialDirectionalMotionSpec(
    initialMapping: Mapping = Mapping.Identity,
    semantics: List<SemanticValue<*>> = emptyList(),
    defaultSpring: SpringParameters = this.spatial.default,
    init: DirectionalBuilderFn,
) = directionalMotionSpec(defaultSpring, initialMapping, semantics, init)

/**
 * Builds a [DirectionalMotionSpec] for effects values by defining a sequence of ([Breakpoint],
 * [Mapping]) pairs
 *
 * The [initialMapping] is [Mapping.Zero], and the Material effects.default spring is used, unless
 * otherwise specified.
 *
 * @see directionalMotionSpec
 */
fun MotionBuilderContext.effectsDirectionalMotionSpec(
    initialMapping: Mapping = Mapping.Zero,
    semantics: List<SemanticValue<*>> = emptyList(),
    defaultSpring: SpringParameters = this.effects.default,
    init: DirectionalBuilderFn,
) = directionalMotionSpec(defaultSpring, initialMapping, semantics, init)

/**
 * Builds a [DirectionalMotionSpec] by defining a sequence of ([Breakpoint], [Mapping]) pairs.
 *
 * This function simplifies the creation of complex motion specifications. It allows you to define a
 * series of motion segments, each with its own behavior, separated by breakpoints. The breakpoints
 * and their corresponding segments will always be ordered from min to max value, regardless of how
 * the `DirectionalMotionSpec` is applied.
 *
 * Example Usage:
 * ```kotlin
 * val motionSpec = directionalMotionSpec(
 *     defaultSpring = materialSpatial,
 *
 *     // Start as a constant transition, always 0.
 *     initialMapping = Mapping.Zero
 * ) {
 *     // At breakpoint 10: Linear transition from 0 to 50.
 *     target(breakpoint = 10f, from = 0f, to = 50f)
 *
 *     // At breakpoint 20: Jump +5, and constant value 55.
 *     fixedValueFromCurrent(breakpoint = 20f, delta = 5f)
 *
 *     // At breakpoint 30: Jump to 40. Linear mapping using: progress_since_breakpoint * fraction.
 *     fractionalInput(breakpoint = 30f, from = 40f, fraction = 2f)
 * }
 * ```
 *
 * @param defaultSpring The default [SpringParameters] to use for all breakpoints.
 * @param initialMapping The initial [Mapping] for the first segment (defaults to
 *   [Mapping.Identity]).
 * @param init A lambda function that configures the spec using the [DirectionalBuilderScope]. The
 *   lambda should return a [CanBeLastSegment] to indicate the end of the spec.
 * @param semantics Semantics specified in this spec, including the initial value applied for
 *   [initialMapping].
 *     @return The constructed [DirectionalMotionSpec].
 */
fun directionalMotionSpec(
    defaultSpring: SpringParameters,
    initialMapping: Mapping = Mapping.Identity,
    semantics: List<SemanticValue<*>> = emptyList(),
    init: DirectionalBuilderFn,
): DirectionalMotionSpec {
    return DirectionalBuilderImpl(defaultSpring, semantics)
        .apply {
            prepareBuilderFn(initialMapping)
            init()
            finalizeBuilderFn(Breakpoint.maxLimit)
        }
        .build()
}

/**
 * Builds a simple [DirectionalMotionSpec] with a single segment.
 *
 * @param mapping The [Mapping] to apply to the segment. Defaults to [Mapping.Identity].
 * @param semantics Semantics values for this spec.
 * @return A new [DirectionalMotionSpec] instance configured with the provided parameters.
 */
fun directionalMotionSpec(
    mapping: Mapping = Mapping.Identity,
    segmentHaptics: SegmentHaptics = SegmentHaptics.None,
    semantics: List<SemanticValue<*>> = emptyList(),
): DirectionalMotionSpec {
    fun <T> toSegmentSemanticValues(semanticValue: SemanticValue<T>) =
        SegmentSemanticValues(semanticValue.key, listOf(semanticValue.value))

    return DirectionalMotionSpec(
        listOf(Breakpoint.minLimit, Breakpoint.maxLimit),
        listOf(mapping),
        listOf(segmentHaptics),
        semantics.map { toSegmentSemanticValues(it) },
    )
}
