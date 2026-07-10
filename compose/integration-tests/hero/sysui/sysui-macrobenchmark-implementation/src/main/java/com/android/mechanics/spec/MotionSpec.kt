/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics.spec

import androidx.compose.ui.util.fastFirstOrNull
import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spring.SpringParameters

/**
 * Specification for the mapping of input values to output values.
 *
 * The spec consists of two independent directional spec's, while only one the one matching
 * `MotionInput`'s `direction` is used at any given time.
 *
 * @param maxDirection spec used when the MotionInput's direction is [InputDirection.Max]
 * @param minDirection spec used when the MotionInput's direction is [InputDirection.Min]
 * @param resetSpring spring parameters to animate a difference in output, if the difference is
 *   caused by setting this new spec.
 * @param segmentHandlers allow for custom segment-change logic, when the `MotionValue` runtime
 *   would leave the [SegmentKey].
 * @param semantics semantics applied to the complete [MotionSpec]
 */
data class MotionSpec(
    val maxDirection: DirectionalMotionSpec,
    val minDirection: DirectionalMotionSpec = maxDirection,
    val resetSpring: SpringParameters = DefaultResetSpring,
    val segmentHandlers: Map<SegmentKey, OnChangeSegmentHandler> = emptyMap(),
    val semantics: List<SemanticValue<*>> = emptyList(),
) {

    /** The [DirectionalMotionSpec] for the specified [direction]. */
    operator fun get(direction: InputDirection): DirectionalMotionSpec {
        return when (direction) {
            InputDirection.Min -> minDirection
            InputDirection.Max -> maxDirection
        }
    }

    /** Whether this spec contains a segment with the specified [segmentKey]. */
    fun containsSegment(segmentKey: SegmentKey): Boolean {
        return get(segmentKey.direction).findSegmentIndex(segmentKey) != -1
    }

    /**
     * The semantic state for [key], as defined for the [MotionSpec].
     *
     * Returns `null` if no semantic value with [key] is defined.
     */
    fun <T> semanticState(key: SemanticKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return semantics.fastFirstOrNull { it.key == key }?.value as T?
    }

    /**
     * The semantic state for [key] at segment with [segmentKey].
     *
     * Returns `null` if no semantic value with [key] is defined. Throws [NoSuchElementException] if
     * [segmentKey] does not exist in this [MotionSpec].
     */
    fun <T> semanticState(key: SemanticKey<T>, segmentKey: SegmentKey): T? {
        with(get(segmentKey.direction)) {
            val semanticValues =
                semantics.fastFirstOrNull { it.key == key } ?: return semanticState(key)
            val segmentIndex = findSegmentIndex(segmentKey)
            if (segmentIndex < 0) throw NoSuchElementException()

            @Suppress("UNCHECKED_CAST")
            return semanticValues.values[segmentIndex] as T
        }
    }

    /**
     * All [SemanticValue]s associated with the segment identified with [segmentKey].
     *
     * Throws [NoSuchElementException] if [segmentKey] does not exist in this [MotionSpec].
     */
    fun semantics(segmentKey: SegmentKey): List<SemanticValue<*>> {
        with(get(segmentKey.direction)) {
            val segmentIndex = findSegmentIndex(segmentKey)
            if (segmentIndex < 0) throw NoSuchElementException()

            return semantics.map { it[segmentIndex] }
        }
    }

    /**
     * The [SegmentData] for an input with the specified [position] and [direction].
     *
     * The returned [SegmentData] will be cached while [SegmentData.isValidForInput] returns `true`.
     */
    fun segmentAtInput(position: Float, direction: InputDirection): SegmentData {
        require(position.isFinite())

        return with(get(direction)) {
            var idx = findBreakpointIndex(position)
            if (direction == InputDirection.Min && breakpoints[idx].position == position) {
                // The segment starts at `position`. Since the breakpoints are sorted ascending, no
                // matter the spec's direction, need to return the previous segment in the min
                // direction.
                idx--
            }

            SegmentData(
                this@MotionSpec,
                breakpoints[idx],
                breakpoints[idx + 1],
                direction,
                mappings[idx],
                haptics[idx],
            )
        }
    }

    /**
     * Looks up the new [SegmentData] once the [currentSegment] is not valid for an input with
     * [newPosition] and [newDirection].
     *
     * This will delegate to the [segmentHandlers], if registered for the [currentSegment]'s key.
     */
    internal fun onChangeSegment(
        currentSegment: SegmentData,
        newPosition: Float,
        newDirection: InputDirection,
    ): SegmentData {
        val segmentChangeHandler = segmentHandlers[currentSegment.key]
        return segmentChangeHandler?.invoke(this, currentSegment, newPosition, newDirection)
            ?: segmentAtInput(newPosition, newDirection)
    }

    override fun toString() = toDebugString()

    companion object {
        /**
         * Default spring parameters for the reset spring. Matches the Fast Spatial spring of the
         * standard motion scheme.
         */
        private val DefaultResetSpring = SpringParameters(stiffness = 1400f, dampingRatio = 1f)

        /* Identity motion spec, the output is the same as the input. */
        val Identity = MotionSpec(DirectionalMotionSpec.Identity)

        /**
         * Placeholder to indicate that a [MotionSpec] cannot be supplied yet.
         *
         * As long as this spec is set, the MotionValue output is NaN. When the MotionValue is first
         * supplied with an actual spec, the output value will be set immediately, without an
         * animation.
         *
         * This must only ever be supplied as a spec for new `MotionValue`s, which never were
         * supplied any other spec. Supplying this [InitiallyUndefined] spec to a MotionValue that
         * has already been supplied a spec will throw an exception.
         */
        val InitiallyUndefined = MotionSpec(DirectionalMotionSpec.InitiallyUndefined)
    }
}

/**
 * Defines the [breakpoints], as well as the [mappings] in-between adjacent [Breakpoint] pairs.
 *
 * This [DirectionalMotionSpec] is applied in the direction defined by the containing [MotionSpec]:
 * especially the direction in which the `breakpoint` [Guarantee] are applied depend on how this is
 * used; this type does not have an inherit direction.
 *
 * All [breakpoints] are sorted in ascending order by their `position`, with the first and last
 * breakpoints are guaranteed to be sentinel values for negative and positive infinity respectively.
 *
 * @param breakpoints All breakpoints in the spec, must contain [Breakpoint.minLimit] as the first
 *   element, and [Breakpoint.maxLimit] as the last element.
 * @param mappings All mappings in between the breakpoints, thus must always contain
 *   `breakpoints.size - 1` elements.
 * @param haptics All segment haptics in between the breakpoints, thus must always contain
 *   `breakpoints.size - 1` elements.
 * @param semantics Semantics that apply to the [MotionSpec].
 */
data class DirectionalMotionSpec(
    val breakpoints: List<Breakpoint>,
    val mappings: List<Mapping>,
    val haptics: List<SegmentHaptics> = List(mappings.size) { SegmentHaptics.None },
    val semantics: List<SegmentSemanticValues<*>> = emptyList(),
) {
    /** Maps all [BreakpointKey]s used in this spec to its index in [breakpoints]. */
    private val breakpointIndexByKey: Map<BreakpointKey, Int>

    init {
        require(breakpoints.size >= 2)
        require(breakpoints.first() == Breakpoint.minLimit)
        require(breakpoints.last() == Breakpoint.maxLimit)
        require(breakpoints.zipWithNext { a, b -> a <= b }.all { it }) {
            "Breakpoints are not sorted ascending ${breakpoints.map { "${it.key}@${it.position}" }}"
        }
        require(mappings.size == breakpoints.size - 1)
        require(haptics.size == breakpoints.size - 1) {
            "${haptics.size} segment haptics were provided but ${breakpoints.size - 1} are " +
                "required"
        }

        breakpointIndexByKey =
            breakpoints.mapIndexed { index, breakpoint -> breakpoint.key to index }.toMap()

        semantics.forEach {
            require(it.values.size == mappings.size) {
                "Semantics ${it.key} contains ${it.values.size} values vs ${mappings.size} expected"
            }
        }
    }

    /**
     * Returns the index of the closest breakpoint where `Breakpoint.position <= position`.
     *
     * Guaranteed to be a valid index into [breakpoints], and guaranteed to be neither the first nor
     * the last element.
     *
     * @param position the position in the input domain.
     * @return Index into [breakpoints], guaranteed to be in range `1..breakpoints.size - 2`
     */
    fun findBreakpointIndex(position: Float): Int {
        require(position.isFinite())
        val breakpointPosition = breakpoints.binarySearchBy(position) { it.position }

        val result =
            when {
                // position is between two anchors, return the min one.
                breakpointPosition < 0 -> -breakpointPosition - 2
                else -> breakpointPosition
            }

        check(result >= 0)
        check(result < breakpoints.size - 1)

        return result
    }

    /**
     * The index of the breakpoint with the specified [breakpointKey], or `-1` if no such breakpoint
     * exists.
     */
    fun findBreakpointIndex(breakpointKey: BreakpointKey): Int {
        return breakpointIndexByKey[breakpointKey] ?: -1
    }

    /** Index into [mappings] for the specified [segmentKey], or `-1` if no such segment exists. */
    fun findSegmentIndex(segmentKey: SegmentKey): Int {
        val result = breakpointIndexByKey[segmentKey.minBreakpoint] ?: return -1
        if (breakpoints[result + 1].key != segmentKey.maxBreakpoint) return -1

        return result
    }

    override fun toString() = toDebugString()

    companion object {
        /* Identity spec, the full input domain is mapped to output using [Mapping.identity]. */
        val Identity =
            DirectionalMotionSpec(
                listOf(Breakpoint.minLimit, Breakpoint.maxLimit),
                listOf(Mapping.Identity),
                listOf(SegmentHaptics.None),
            )

        /** Internal marker for [MotionSpec.InitiallyUndefined]. */
        internal val InitiallyUndefined =
            DirectionalMotionSpec(
                listOf(Breakpoint.minLimit, Breakpoint.maxLimit),
                listOf(
                    object : Mapping {
                        override fun map(input: Float): Float {
                            return Float.NaN
                        }

                        override fun toString(): String {
                            return "InitiallyUndefined"
                        }
                    }
                ),
                listOf<SegmentHaptics>(SegmentHaptics.None),
            )
    }
}
