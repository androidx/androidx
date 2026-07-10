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
import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.OnChangeSegmentHandler
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters

/**
 * Defines the contract for applying [Effect]s within a [MotionSpecBuilder]
 *
 * Provides methods to define breakpoints, mappings and haptics for the motion specification.
 *
 * Breakpoints for [minLimit] and [maxLimit] will be created, with the specified key and parameters.
 */
interface EffectApplyScope : MotionBuilderContext {
    /** Default spring in use when not otherwise specified. */
    val defaultSpring: SpringParameters

    /** Mapping used outside of the defined effects. */
    val baseMapping: Mapping

    /**
     * Defines spec simultaneously for both, the min and max direction.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit]. Specifying breakpoints
     * outside of this range will throw.
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param initialMapping [Mapping] for the first segment after [minLimit].
     * @param semantics Initial semantics for the effect.
     * @param init Configures the effect's spec using [DirectionalBuilderScope].
     * @see com.android.mechanics.spec.directionalMotionSpec for in-depth documentation.
     */
    fun unidirectional(
        initialMapping: Mapping,
        semantics: List<SemanticValue<*>> = emptyList(),
        init: DirectionalEffectBuilderScope.() -> Unit,
    )

    /**
     * Defines spec simultaneously for both, the min and max direction, using a single segment only.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit].
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param mapping [Mapping] to be used between [minLimit] and [maxLimit].
     * @param semantics Initial semantics for the effect.
     * @see com.android.mechanics.spec.directionalMotionSpec for in depth documentation.
     */
    fun unidirectional(mapping: Mapping, semantics: List<SemanticValue<*>> = emptyList())

    /**
     * Defines the spec for max direction.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit]. Specifying breakpoints
     * outside of this range will throw.
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param initialMapping [Mapping] for the first segment after [minLimit].
     * @param initialSegmentHaptics [SegmentHaptics] for the first segment after [minLimit]
     * @param semantics Initial semantics for the effect.
     * @param init Configures the effect's spec using [DirectionalBuilderScope].
     * @see com.android.mechanics.spec.directionalMotionSpec for in-depth documentation.
     */
    fun forward(
        initialMapping: Mapping,
        initialSegmentHaptics: SegmentHaptics = SegmentHaptics.None,
        semantics: List<SemanticValue<*>> = emptyList(),
        init: DirectionalEffectBuilderScope.() -> Unit,
    )

    /**
     * Defines the spec for max direction, using a single segment only.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit].
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param mapping [Mapping] to be used between [minLimit] and [maxLimit].
     * @param semantics Initial semantics for the effect.
     * @see com.android.mechanics.spec.directionalMotionSpec for in depth documentation.
     */
    fun forward(mapping: Mapping, semantics: List<SemanticValue<*>> = emptyList())

    /**
     * Defines the spec for min direction.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit]. Specifying breakpoints
     * outside of this range will throw.
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param initialMapping [Mapping] for the first segment after [minLimit].
     * @param semantics Initial semantics for the effect.
     * @param init Configures the effect's spec using [DirectionalBuilderScope].
     * @see com.android.mechanics.spec.directionalMotionSpec for in-depth documentation.
     */
    fun backward(
        initialMapping: Mapping,
        semantics: List<SemanticValue<*>> = emptyList(),
        init: DirectionalEffectBuilderScope.() -> Unit,
    )

    /**
     * Defines the spec for min direction, using a single segment only.
     *
     * The behavior is the same as for `directionalMotionSpec`, with the notable exception that the
     * spec to be defined is confined within [minLimit] and [maxLimit].
     *
     * Will throw if [forward] or [unidirectional] has been called in this scope before.
     *
     * The first / last semantic value will implicitly extend to the start / end of the resulting
     * spec, unless redefined in another spec.
     *
     * @param mapping [Mapping] to be used between [minLimit] and [maxLimit].
     * @param semantics Initial semantics for the effect.
     * @see com.android.mechanics.spec.directionalMotionSpec for in depth documentation.
     */
    fun backward(mapping: Mapping, semantics: List<SemanticValue<*>> = emptyList())

    /** Adds a segment handler to the resulting [MotionSpec]. */
    fun addSegmentHandler(key: SegmentKey, handler: OnChangeSegmentHandler)

    /** Returns the value of [baseValue] at [position]. */
    fun baseValue(position: Float): Float
}

interface DirectionalEffectBuilderScope : DirectionalBuilderScope {

    fun before(
        spring: SpringParameters? = null,
        guarantee: Guarantee? = null,
        semantics: List<SemanticValue<*>>? = null,
        mapping: Mapping? = null,
        breakpointHaptics: BreakpointHaptics? = null,
    )

    fun after(
        spring: SpringParameters? = null,
        guarantee: Guarantee? = null,
        semantics: List<SemanticValue<*>>? = null,
        mapping: Mapping? = null,
        breakpointHaptics: BreakpointHaptics? = null,
    )
}
