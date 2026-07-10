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
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SegmentSemanticValues
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters

/**
 * Internal, reusable implementation of the [DirectionalBuilderScope].
 *
 * Clients must use [directionalMotionSpec] instead.
 */
internal open class DirectionalBuilderImpl(
    override val defaultSpring: SpringParameters,
    baseSemantics: List<SemanticValue<*>>,
) : DirectionalBuilderScope {
    internal val breakpoints = mutableListOf(Breakpoint.minLimit)
    internal val semantics = mutableListOf<SegmentSemanticValuesBuilder<*>>()
    internal val mappings = mutableListOf<Mapping>()
    internal val segmentHaptics = mutableListOf<SegmentHaptics>()
    private var currentSegmentHaptics: SegmentHaptics = SegmentHaptics.None
    private var sourceValue: Float = Float.NaN
    private var targetValue: Float = Float.NaN
    private var fractionalMapping: Float = Float.NaN
    private var breakpointPosition: Float = Float.NaN
    private var breakpointKey: BreakpointKey? = null

    init {
        baseSemantics.forEach { getSemantics(it.key).apply { set(0, it.value) } }
    }

    /** Prepares the builder for invoking the [DirectionalBuilderFn] on it. */
    fun prepareBuilderFn(
        initialMapping: Mapping = Mapping.Identity,
        initialSegmentHaptics: SegmentHaptics = SegmentHaptics.None,
        initialSemantics: List<SemanticValue<*>> = emptyList(),
    ) {
        check(mappings.size == breakpoints.size - 1)
        check(segmentHaptics.size == breakpoints.size - 1)

        mappings.add(initialMapping)
        segmentHaptics.add(initialSegmentHaptics)
        val semanticIndex = mappings.size - 1
        initialSemantics.forEach { semantic ->
            getSemantics(semantic.key).apply { set(semanticIndex, semantic.value) }
        }
    }

    internal fun <T> getSemantics(key: SemanticKey<T>): SegmentSemanticValuesBuilder<T> {
        @Suppress("UNCHECKED_CAST")
        var builder = semantics.firstOrNull { it.key == key } as SegmentSemanticValuesBuilder<T>?
        if (builder == null) {
            builder = SegmentSemanticValuesBuilder(key).also { semantics.add(it) }
        }
        return builder
    }

    /**
     * Finalizes open segments, after invoking a [DirectionalBuilderFn].
     *
     * Afterwards, either [build] or another pair of {[prepareBuilderFn], [finalizeBuilderFn]} calls
     * can be done.
     */
    fun finalizeBuilderFn(
        atPosition: Float,
        key: BreakpointKey,
        breakpointHaptics: BreakpointHaptics,
        springSpec: SpringParameters,
        guarantee: Guarantee,
        semantics: List<SemanticValue<*>>,
    ) {
        if (!(targetValue.isNaN() && fractionalMapping.isNaN())) {
            // Finalizing will produce the mapping and breakpoint
            check(mappings.size == breakpoints.size - 1)
            check(segmentHaptics.size == breakpoints.size - 1)
        } else {
            // Mapping is already added, this will add the breakpoint
            check(mappings.size == breakpoints.size)
            check(segmentHaptics.size == breakpoints.size) {
                "Total segment haptics: ${segmentHaptics.size}. A total of ${breakpoints.size} was expected"
            }
        }

        if (key == BreakpointKey.MaxLimit) {
            check(targetValue.isNaN()) { "cant specify target value for last segment" }
            check(semantics.isEmpty()) { "cant specify semantics for last breakpoint" }
        } else {
            check(atPosition.isFinite())
            check(atPosition > breakpoints.last().position) {
                "Breakpoint ${breakpoints.last()} placed after partial sequence (end=$atPosition)"
            }
        }

        toBreakpointImpl(atPosition, key, semantics)
        doAddBreakpointImpl(springSpec, guarantee, breakpointHaptics)
    }

    fun finalizeBuilderFn(breakpoint: Breakpoint) =
        finalizeBuilderFn(
            breakpoint.position,
            breakpoint.key,
            breakpoint.breakpointHaptics,
            breakpoint.spring,
            breakpoint.guarantee,
            emptyList(),
        )

    /* Creates the [DirectionalMotionSpec] from the current builder state. */
    fun build(): DirectionalMotionSpec {
        require(mappings.size == breakpoints.size - 1)
        require(segmentHaptics.size == breakpoints.size - 1)
        check(breakpoints.last() == Breakpoint.maxLimit)

        val segmentCount = mappings.size

        val semantics = semantics.map { builder -> with(builder) { build(segmentCount) } }

        return DirectionalMotionSpec(
            breakpoints.toList(),
            mappings.toList(),
            segmentHaptics.toList(),
            semantics,
        )
    }

    override fun target(
        breakpoint: Float,
        from: Float,
        to: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ) {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpToImpl(from, spring, guarantee, breakpointHaptics)
        continueWithTargetValueImpl(to)
    }

    override fun targetFromCurrent(
        breakpoint: Float,
        to: Float,
        delta: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ) {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpByImpl(delta, spring, guarantee, breakpointHaptics)
        continueWithTargetValueImpl(to)
    }

    override fun fractionalInput(
        breakpoint: Float,
        from: Float,
        fraction: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ): CanBeLastSegment {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpToImpl(from, spring, guarantee, breakpointHaptics)
        continueWithFractionalInputImpl(fraction)
        return CanBeLastSegmentImpl
    }

    override fun fractionalInputFromCurrent(
        breakpoint: Float,
        fraction: Float,
        delta: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ): CanBeLastSegment {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpByImpl(delta, spring, guarantee, breakpointHaptics)
        continueWithFractionalInputImpl(fraction)
        return CanBeLastSegmentImpl
    }

    override fun fixedValue(
        breakpoint: Float,
        value: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ): CanBeLastSegment {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpToImpl(value, spring, guarantee, breakpointHaptics)
        continueWithFixedValueImpl()
        return CanBeLastSegmentImpl
    }

    override fun fixedValueFromCurrent(
        breakpoint: Float,
        delta: Float,
        breakpointHaptics: BreakpointHaptics,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ): CanBeLastSegment {
        toBreakpointImpl(breakpoint, key, semantics)
        jumpByImpl(delta, spring, guarantee, breakpointHaptics)
        continueWithFixedValueImpl()
        return CanBeLastSegmentImpl
    }

    override fun mapping(
        breakpoint: Float,
        spring: SpringParameters,
        guarantee: Guarantee,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
        breakpointHaptics: BreakpointHaptics,
        mapping: Mapping,
    ): CanBeLastSegment {
        toBreakpointImpl(breakpoint, key, semantics)
        continueWithImpl(mapping, spring, guarantee, breakpointHaptics)
        return CanBeLastSegmentImpl
    }

    private fun continueWithTargetValueImpl(target: Float) {
        check(sourceValue.isFinite())

        targetValue = target
    }

    private fun continueWithFractionalInputImpl(fraction: Float) {
        check(sourceValue.isFinite())

        fractionalMapping = fraction
    }

    private fun continueWithFixedValueImpl() {
        check(sourceValue.isFinite())

        mappings.add(Mapping.Fixed(sourceValue))
        segmentHaptics.add(currentSegmentHaptics)
        sourceValue = Float.NaN
    }

    private fun jumpToImpl(
        value: Float,
        spring: SpringParameters,
        guarantee: Guarantee,
        breakpointHaptics: BreakpointHaptics,
    ) {
        check(sourceValue.isNaN())

        doAddBreakpointImpl(spring, guarantee, breakpointHaptics)
        sourceValue = value
    }

    private fun jumpByImpl(
        delta: Float,
        spring: SpringParameters,
        guarantee: Guarantee,
        breakpointHaptics: BreakpointHaptics,
    ) {
        check(sourceValue.isNaN())

        val breakpoint = doAddBreakpointImpl(spring, guarantee, breakpointHaptics)
        sourceValue = mappings.last().map(breakpoint.position) + delta
    }

    private fun continueWithImpl(
        mapping: Mapping,
        spring: SpringParameters,
        guarantee: Guarantee,
        breakpointHaptics: BreakpointHaptics,
    ) {
        check(sourceValue.isNaN())

        doAddBreakpointImpl(spring, guarantee, breakpointHaptics)
        mappings.add(mapping)
        segmentHaptics.add(currentSegmentHaptics)
    }

    private fun toBreakpointImpl(
        atPosition: Float,
        key: BreakpointKey,
        semantics: List<SemanticValue<*>>,
    ) {
        check(breakpointPosition.isNaN())
        check(breakpointKey == null)

        check(atPosition >= breakpoints.last().position) {
            "Breakpoint position specified is before last breakpoint"
        }

        if (!targetValue.isNaN() || !fractionalMapping.isNaN()) {
            check(!sourceValue.isNaN())

            val sourcePosition = breakpoints.last().position
            val breakpointDistance = atPosition - sourcePosition
            val mapping =
                if (breakpointDistance == 0f) {
                    Mapping.Fixed(sourceValue)
                } else {

                    if (fractionalMapping.isNaN()) {
                        val delta = targetValue - sourceValue
                        fractionalMapping = delta / (atPosition - sourcePosition)
                    } else {
                        val delta = (atPosition - sourcePosition) * fractionalMapping
                        targetValue = sourceValue + delta
                    }

                    val offset = sourceValue - (sourcePosition * fractionalMapping)
                    Mapping.Linear(fractionalMapping, offset)
                }

            mappings.add(mapping)
            segmentHaptics.add(currentSegmentHaptics)
            targetValue = Float.NaN
            sourceValue = Float.NaN
            fractionalMapping = Float.NaN
        }

        breakpointPosition = atPosition
        breakpointKey = key

        semantics.forEach { (key, value) ->
            getSemantics(key).apply {
                // Last segment is guaranteed to be completed
                set(mappings.size, value)
            }
        }
    }

    private fun doAddBreakpointImpl(
        springSpec: SpringParameters,
        guarantee: Guarantee,
        breakpointHaptics: BreakpointHaptics,
    ): Breakpoint {
        val breakpoint =
            Breakpoint.create(
                checkNotNull(breakpointKey),
                breakpointPosition,
                springSpec,
                guarantee,
                breakpointHaptics,
            )

        breakpoints.add(breakpoint)
        breakpointPosition = Float.NaN
        breakpointKey = null

        return breakpoint
    }

    private fun beginHaptics(segmentHaptics: SegmentHaptics) {
        currentSegmentHaptics = segmentHaptics
    }

    private fun endHaptics() {
        currentSegmentHaptics = SegmentHaptics.None
    }

    @HapticsExperimentalApi
    override fun <T> haptics(
        segmentHaptics: SegmentHaptics,
        block: DirectionalBuilderScope.() -> T,
    ) {
        beginHaptics(segmentHaptics)
        try {
            block()
        } finally {
            endHaptics()
        }
    }
}

internal class SegmentSemanticValuesBuilder<T>(val key: SemanticKey<T>) {
    private val values = mutableListOf<SemanticValueHolder<T>>()
    private val unspecified = SemanticValueHolder.Unspecified<T>()

    @Suppress("UNCHECKED_CAST")
    fun <V> set(segmentIndex: Int, value: V) {
        if (segmentIndex < values.size) {
            values[segmentIndex] = SemanticValueHolder.Specified(value as T)
        } else {
            backfill(segmentCount = segmentIndex)
            values.add(SemanticValueHolder.Specified(value as T))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> updateBefore(segmentIndex: Int, value: V) {
        require(segmentIndex < values.size)

        val specified = SemanticValueHolder.Specified(value as T)

        for (i in segmentIndex downTo 0) {
            if (values[i] is SemanticValueHolder.Specified) break
            values[i] = specified
        }
    }

    fun build(segmentCount: Int): SegmentSemanticValues<T> {
        backfill(segmentCount)
        val firstValue = values.firstNotNullOf { it as? SemanticValueHolder.Specified }.value
        return SegmentSemanticValues(
            key,
            values.drop(1).runningFold(firstValue) { lastValue, thisHolder ->
                if (thisHolder is SemanticValueHolder.Specified) thisHolder.value else lastValue
            },
        )
    }

    private fun backfill(segmentCount: Int) {
        repeat(segmentCount - values.size) { values.add(unspecified) }
    }
}

internal sealed interface SemanticValueHolder<T> {
    class Specified<T>(val value: T) : SemanticValueHolder<T>

    class Unspecified<T>() : SemanticValueHolder<T>
}

private data object CanBeLastSegmentImpl : CanBeLastSegment
