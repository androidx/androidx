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

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntList
import androidx.collection.MutableIntLongMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableLongList
import androidx.collection.ObjectList
import androidx.collection.mutableObjectListOf
import com.android.mechanics.haptics.BreakpointHaptics
import com.android.mechanics.haptics.SegmentHaptics
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.OnChangeSegmentHandler
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticValue
import com.android.mechanics.spring.SpringParameters
import kotlin.jvm.JvmInline

internal class MotionSpecBuilderImpl(
    override val baseMapping: Mapping,
    override val defaultSpring: SpringParameters,
    private val resetSpring: SpringParameters,
    private val baseSemantics: List<SemanticValue<*>>,
    motionBuilderContext: MotionBuilderContext,
) : MotionSpecBuilderScope, MotionBuilderContext by motionBuilderContext, EffectApplyScope {

    private val placedEffects = MutableIntObjectMap<Effect>()
    private val absoluteEffectPlacements = MutableIntLongMap()
    private val relativeEffectPlacements = MutableIntIntMap()

    private lateinit var builders: ObjectList<DirectionalEffectBuilderScopeImpl>
    private val forwardBuilder: DirectionalEffectBuilderScopeImpl
        get() = builders[0]

    private val reverseBuilder: DirectionalEffectBuilderScopeImpl
        get() = builders[1]

    private lateinit var segmentHandlers: MutableMap<SegmentKey, OnChangeSegmentHandler>

    fun build(): MotionSpec {
        if (placedEffects.isEmpty()) {
            return MotionSpec(
                directionalMotionSpec(baseMapping),
                resetSpring = resetSpring,
                semantics = baseSemantics,
            )
        }

        builders =
            mutableObjectListOf(
                DirectionalEffectBuilderScopeImpl(defaultSpring),
                DirectionalEffectBuilderScopeImpl(defaultSpring),
            )
        segmentHandlers = mutableMapOf()

        val capacity = placedEffects.size * 2 + 1
        val sortedEffects = MutableIntList(capacity)
        val specifiedPlacements = MutablePlacementList(MutableLongList(capacity))
        val actualPlacements = MutablePlacementList(MutableLongList(capacity))

        placeEffects(sortedEffects, specifiedPlacements, actualPlacements)
        check(sortedEffects.size >= 2)

        var minLimitKey = BreakpointKey.MinLimit
        lateinit var maxLimitKey: BreakpointKey

        for (i in 0 until sortedEffects.lastIndex) {
            maxLimitKey = BreakpointKey()
            applyEffect(
                sortedEffects[i],
                specifiedPlacements[i],
                actualPlacements[i],
                minLimitKey,
                maxLimitKey,
            )
            minLimitKey = maxLimitKey
        }

        maxLimitKey = BreakpointKey.MaxLimit

        applyEffect(
            sortedEffects.last(),
            specifiedPlacements.last(),
            actualPlacements.last(),
            minLimitKey,
            maxLimitKey,
        )

        return MotionSpec(
            builders[0].build(),
            builders[1].build(),
            resetSpring,
            segmentHandlers.toMap(),
            semantics = baseSemantics,
        )
    }

    private fun placeEffects(
        sortedEffects: MutableIntList,
        specifiedPlacements: MutablePlacementList,
        actualPlacements: MutablePlacementList,
    ) {

        // To place the effects, do the following
        // - sort all `absoluteEffectPlacements` in ascending order
        // - use the sorted absolutely placed effects as seeds. For each of them, do the following:
        //   - measure the effect
        //   - recursively walk the relatively effects placed before, tracking the min boundary
        //     (this requires effects that have a defined extend to the min side)
        //   - upon reaching the beginning, start placing the effects in the forward direction.
        //     continue up to the seed effects, t
        //   - recursively continue placing effects relatively placed afterwards.

        fun appendEffect(
            effectId: Int,
            specifiedPlacement: EffectPlacement,
            measuredPlacement: EffectPlacement,
        ) {
            var actualPlacement = measuredPlacement
            var prependNoPlaceholderEffect = false

            if (actualPlacements.isEmpty()) {
                // placing first effect.
                if (measuredPlacement.min.isFinite()) {
                    prependNoPlaceholderEffect = true
                }
            } else {

                val previousPlacement = actualPlacements.last()
                if (previousPlacement.max.isFinite()) {
                    // The previous effect has a defined end-point.

                    if (measuredPlacement.min == Float.NEGATIVE_INFINITY) {
                        // The current effect wants to extend to the end of the previous effect.
                        require(measuredPlacement.max.isFinite())
                        actualPlacement =
                            EffectPlacement.between(previousPlacement.max, measuredPlacement.max)
                    } else if (measuredPlacement.min > previousPlacement.max) {
                        // There's a gap between the last and the current effect, will need to
                        // insert a placeholder
                        require(measuredPlacement.min.isFinite())
                        prependNoPlaceholderEffect = true
                    } else {
                        // In all other cases, the previous end has to match the current start.
                        // In all other cases, effects are overlapping, which is not supported.
                        require(measuredPlacement.min == previousPlacement.max) {
                            "Effects are overlapping"
                        }
                    }
                } else {
                    // The previous effect wants to extend to the beginning of the next effect
                    check(previousPlacement.max == Float.POSITIVE_INFINITY)

                    // Therefore the current effect is required to have a defined start-point
                    require(measuredPlacement.min.isFinite()) {
                        "Only one of the effects can extend to the  boundary, not both:\n" +
                            "  this:  $actualPlacement (${placedEffects[effectId]})\n" +
                            "  previous:  $previousPlacement (${placedEffects[effectId]}])\n"
                    }

                    actualPlacements[actualPlacements.lastIndex] =
                        EffectPlacement.between(previousPlacement.min, measuredPlacement.min)
                }
            }

            if (prependNoPlaceholderEffect) {
                check(actualPlacement.min.isFinite())
                // Adding a placeholder that will be skipped, but simplifies the algorithm by
                // ensuring all effects are back-to-back. The NoEffectPlaceholderId is used to

                sortedEffects.add(NoEffectPlaceholderId)
                val placeholderPlacement = EffectPlacement.before(actualPlacement.min)
                specifiedPlacements.add(placeholderPlacement)
                actualPlacements.add(placeholderPlacement)
            }

            sortedEffects.add(effectId)
            specifiedPlacements.add(specifiedPlacement)

            actualPlacements.add(actualPlacement)
        }

        fun processEffectsPlacedBefore(
            anchorEffectId: Int,
            anchorEffectPlacement: EffectPlacement,
        ) {
            val beforeEffectKey = -anchorEffectId
            if (relativeEffectPlacements.containsKey(beforeEffectKey)) {
                val effectId = relativeEffectPlacements[beforeEffectKey]
                val effect = checkNotNull(placedEffects[effectId])

                require(anchorEffectPlacement.min.isFinite())
                val specifiedPlacement = EffectPlacement.before(anchorEffectPlacement.min)

                val measuredPlacement = measureEffect(effect, specifiedPlacement)
                processEffectsPlacedBefore(effectId, measuredPlacement)
                appendEffect(effectId, specifiedPlacement, measuredPlacement)
            }
        }

        fun processEffectsPlacedAfter(anchorEffectId: Int, anchorEffectPlacement: EffectPlacement) {
            val afterEffectKey = anchorEffectId
            if (relativeEffectPlacements.containsKey(afterEffectKey)) {
                val effectId = relativeEffectPlacements[afterEffectKey]
                val effect = checkNotNull(placedEffects[effectId])

                require(anchorEffectPlacement.max.isFinite())
                val specifiedPlacement = EffectPlacement.after(anchorEffectPlacement.max)

                val measuredPlacement = measureEffect(effect, specifiedPlacement)
                appendEffect(effectId, specifiedPlacement, measuredPlacement)
                processEffectsPlacedAfter(effectId, measuredPlacement)
            }
        }

        check(absoluteEffectPlacements.isNotEmpty())
        // Implementation note: sortedAbsolutePlacedEffects should be an IntArray, but that cannot
        // be sorted with a custom comparator, hence using a typed array.
        val sortedAbsolutePlacedEffects =
            Array(absoluteEffectPlacements.size) { 0 }
                .also { array ->
                    var index = 0
                    absoluteEffectPlacements.forEachKey { array[index++] = it }
                    array.sortBy { EffectPlacement(absoluteEffectPlacements[it]).sortOrder }
                }

        sortedAbsolutePlacedEffects.forEach { effectId ->
            val effect = checkNotNull(placedEffects[effectId])
            val specifiedPlacement = EffectPlacement(absoluteEffectPlacements[effectId])
            val measuredPlacement = measureEffect(effect, specifiedPlacement)
            processEffectsPlacedBefore(effectId, measuredPlacement)
            appendEffect(effectId, specifiedPlacement, measuredPlacement)
            processEffectsPlacedAfter(effectId, measuredPlacement)
        }

        if (actualPlacements.last().max != Float.POSITIVE_INFINITY) {
            sortedEffects.add(NoEffectPlaceholderId)
            val placeholderPlacement = EffectPlacement.after(actualPlacements.last().max)
            specifiedPlacements.add(placeholderPlacement)
            actualPlacements.add(placeholderPlacement)
        }
    }

    // ---- MotionSpecBuilderScope implementation --------------------------------------------------

    override fun at(position: Float, effect: Effect.PlaceableAt): PlacedEffect {
        return addEffect(effect).also {
            absoluteEffectPlacements[it.id] = EffectPlacement.after(position).value
        }
    }

    override fun between(start: Float, end: Float, effect: Effect.PlaceableBetween): PlacedEffect {
        return addEffect(effect).also {
            absoluteEffectPlacements[it.id] = EffectPlacement.between(start, end).value
        }
    }

    override fun before(position: Float, effect: Effect.PlaceableBefore): PlacedEffect {
        return addEffect(effect).also {
            absoluteEffectPlacements[it.id] = EffectPlacement.before(position).value
        }
    }

    override fun before(otherEffect: PlacedEffect, effect: Effect.PlaceableBefore): PlacedEffect {
        require(placedEffects.containsKey(otherEffect.id))
        require(!relativeEffectPlacements.containsKey(-otherEffect.id))
        return addEffect(effect).also { relativeEffectPlacements[-otherEffect.id] = it.id }
    }

    override fun after(position: Float, effect: Effect.PlaceableAfter): PlacedEffect {
        return addEffect(effect).also {
            absoluteEffectPlacements[it.id] = EffectPlacement.after(position).value
        }
    }

    override fun after(otherEffect: PlacedEffect, effect: Effect.PlaceableAfter): PlacedEffect {
        require(placedEffects.containsKey(otherEffect.id))
        require(!relativeEffectPlacements.containsKey(otherEffect.id))

        relativeEffectPlacements.forEach { key, value ->
            if (value == otherEffect.id) {
                require(key > 0) {
                    val other = placedEffects[otherEffect.id]
                    "Cannot place effect [$effect] *after* [$other], since the latter was placed" +
                        "*before* an effect"
                }
            }
        }

        require(!relativeEffectPlacements.containsKey(otherEffect.id))
        return addEffect(effect).also { relativeEffectPlacements[otherEffect.id] = it.id }
    }

    private fun addEffect(effect: Effect): PlacedEffect {
        return PlacedEffect(placedEffects.size + 1).also { placedEffects[it.id] = effect }
    }

    // ----- EffectApplyScope implementation -------------------------------------------------------

    override fun addSegmentHandler(key: SegmentKey, handler: OnChangeSegmentHandler) {
        require(!segmentHandlers.containsKey(key))
        segmentHandlers[key] = handler
    }

    override fun baseValue(position: Float): Float {
        return baseMapping.map(position)
    }

    override fun unidirectional(
        initialMapping: Mapping,
        semantics: List<SemanticValue<*>>,
        init: DirectionalEffectBuilderScope.() -> Unit,
    ) {
        forward(initialMapping, SegmentHaptics.None, semantics, init)
        backward(initialMapping, semantics, init)
    }

    override fun unidirectional(mapping: Mapping, semantics: List<SemanticValue<*>>) {
        forward(mapping, semantics)
        backward(mapping, semantics)
    }

    override fun forward(
        initialMapping: Mapping,
        initialSegmentHaptics: SegmentHaptics,
        semantics: List<SemanticValue<*>>,
        init: DirectionalEffectBuilderScope.() -> Unit,
    ) {
        check(!forwardInvoked) { "Cannot define forward spec more than once" }
        forwardInvoked = true

        forwardBuilder.prepareBuilderFn(initialMapping, initialSegmentHaptics, semantics)
        forwardBuilder.init()
    }

    override fun forward(mapping: Mapping, semantics: List<SemanticValue<*>>) {
        check(!forwardInvoked) { "Cannot define forward spec more than once" }
        forwardInvoked = true

        forwardBuilder.prepareBuilderFn(mapping, SegmentHaptics.None, semantics)
    }

    override fun backward(
        initialMapping: Mapping,
        semantics: List<SemanticValue<*>>,
        init: DirectionalEffectBuilderScope.() -> Unit,
    ) {
        check(!backwardInvoked) { "Cannot define backward spec more than once" }
        backwardInvoked = true

        reverseBuilder.prepareBuilderFn(initialMapping, SegmentHaptics.None, semantics)
        reverseBuilder.init()
    }

    override fun backward(mapping: Mapping, semantics: List<SemanticValue<*>>) {
        check(!backwardInvoked) { "Cannot define backward spec more than once" }
        backwardInvoked = true

        reverseBuilder.prepareBuilderFn(mapping, SegmentHaptics.None, semantics)
    }

    private var forwardInvoked = false
    private var backwardInvoked = false

    private fun applyEffect(
        effectId: Int,
        specifiedPlacement: EffectPlacement,
        actualPlacement: EffectPlacement,
        minLimitKey: BreakpointKey,
        maxLimitKey: BreakpointKey,
    ) {
        require(minLimitKey != maxLimitKey)

        if (effectId == NoEffectPlaceholderId) {
            val maxBreakpoint =
                Breakpoint.create(
                    maxLimitKey,
                    actualPlacement.max,
                    defaultSpring,
                    Guarantee.None,
                    BreakpointHaptics.None,
                )
            builders.forEach { builder ->
                builder.mappings += builder.afterMapping ?: baseMapping
                builder.segmentHaptics += SegmentHaptics.None
                builder.breakpoints += maxBreakpoint
            }
            return
        }

        val initialForwardSize = forwardBuilder.breakpoints.size
        val initialReverseSize = reverseBuilder.breakpoints.size

        val effect = checkNotNull(placedEffects[effectId])

        forwardInvoked = false
        backwardInvoked = false

        builders.forEach { it.resetBeforeAfter() }
        with(effect) {
            createSpec(
                actualPlacement.min,
                minLimitKey,
                actualPlacement.max,
                maxLimitKey,
                specifiedPlacement,
            )
        }

        check(forwardInvoked) { "forward() spec not defined during createSpec()" }
        check(backwardInvoked) { "backward() spec not defined during createSpec()" }

        builders.forEachIndexed { index, builder ->
            val initialSize = if (index == 0) initialForwardSize else initialReverseSize

            require(builder.breakpoints[initialSize - 1].key == minLimitKey)

            builder.finalizeBuilderFn(
                actualPlacement.max,
                maxLimitKey,
                builder.afterBreakpointHaptics ?: BreakpointHaptics.None,
                builder.afterSpring ?: defaultSpring,
                builder.afterGuarantee ?: Guarantee.None,
                builder.afterSemantics ?: emptyList(),
            )
            check(builder.breakpoints.size > initialSize)

            if (builder.beforeSpring != null || builder.beforeGuarantee != null) {
                val oldMinBreakpoint = builder.breakpoints[initialSize - 1]
                builder.breakpoints[initialSize - 1] =
                    oldMinBreakpoint.copy(
                        spring = builder.beforeSpring ?: oldMinBreakpoint.spring,
                        guarantee = builder.beforeGuarantee ?: oldMinBreakpoint.guarantee,
                    )
            }

            builder.beforeMapping
                ?.takeIf { initialSize >= 2 && builder.mappings[initialSize - 2] === baseMapping }
                ?.also { builder.mappings[initialSize - 2] = it }

            builder.beforeSemantics?.forEach {
                builder.getSemantics(it.key).updateBefore(initialSize - 2, it.value)
            }
        }
    }

    companion object {
        private val NoEffectPlaceholderId = -1
    }
}

private class DirectionalEffectBuilderScopeImpl(defaultSpring: SpringParameters) :
    DirectionalBuilderImpl(defaultSpring, baseSemantics = emptyList()),
    DirectionalEffectBuilderScope {

    var beforeGuarantee: Guarantee? = null
    var beforeSpring: SpringParameters? = null
    var beforeSemantics: List<SemanticValue<*>>? = null
    var beforeMapping: Mapping? = null
    var beforeBreakpointHaptics: BreakpointHaptics? = null

    override fun before(
        spring: SpringParameters?,
        guarantee: Guarantee?,
        semantics: List<SemanticValue<*>>?,
        mapping: Mapping?,
        breakpointHaptics: BreakpointHaptics?,
    ) {
        beforeGuarantee = guarantee
        beforeSpring = spring
        beforeSemantics = semantics
        beforeMapping = mapping
        beforeBreakpointHaptics = breakpointHaptics
    }

    var afterGuarantee: Guarantee? = null
    var afterSpring: SpringParameters? = null
    var afterSemantics: List<SemanticValue<*>>? = null
    var afterMapping: Mapping? = null
    var afterBreakpointHaptics: BreakpointHaptics? = null

    override fun after(
        spring: SpringParameters?,
        guarantee: Guarantee?,
        semantics: List<SemanticValue<*>>?,
        mapping: Mapping?,
        breakpointHaptics: BreakpointHaptics?,
    ) {
        afterGuarantee = guarantee
        afterSpring = spring
        afterSemantics = semantics
        afterMapping = mapping
        afterBreakpointHaptics = breakpointHaptics
    }

    fun resetBeforeAfter() {
        beforeGuarantee = null
        beforeSpring = null
        beforeSemantics = null
        beforeMapping = null
        afterGuarantee = null
        afterSpring = null
        afterSemantics = null
        afterMapping = null
        afterBreakpointHaptics = null
        beforeBreakpointHaptics = null
    }
}

private fun MotionBuilderContext.measureEffect(
    effect: Effect,
    specifiedPlacement: EffectPlacement,
): EffectPlacement {
    return when (specifiedPlacement.type) {
        EffectPlacemenType.At -> {
            require(effect is Effect.PlaceableAt)
            with(effect) {
                val minExtend = minExtent()
                require(minExtend.isFinite() && minExtend >= 0)
                val maxExtend = maxExtent()
                require(maxExtend.isFinite() && maxExtend >= 0)

                EffectPlacement.between(
                    specifiedPlacement.start - minExtend,
                    specifiedPlacement.start + maxExtend,
                )
            }
        }

        EffectPlacemenType.Before -> {
            require(effect is Effect.PlaceableBefore)
            with(effect) {
                val intrinsicSize = intrinsicSize()
                if (intrinsicSize.isFinite()) {
                    require(intrinsicSize >= 0)

                    EffectPlacement.between(
                        specifiedPlacement.start,
                        specifiedPlacement.start - intrinsicSize,
                    )
                } else {
                    specifiedPlacement
                }
            }
        }

        EffectPlacemenType.After -> {
            require(effect is Effect.PlaceableAfter)
            with(effect) {
                val intrinsicSize = intrinsicSize()
                if (intrinsicSize.isFinite()) {

                    require(intrinsicSize >= 0)

                    EffectPlacement.between(
                        specifiedPlacement.start,
                        specifiedPlacement.start + intrinsicSize,
                    )
                } else {
                    specifiedPlacement
                }
            }
        }

        EffectPlacemenType.Between -> specifiedPlacement
    }
}

@JvmInline
value class MutablePlacementList(val storage: MutableLongList) {

    val size: Int
        get() = storage.size

    val lastIndex: Int
        get() = storage.lastIndex

    val indices: IntRange
        get() = storage.indices

    fun isEmpty() = storage.isEmpty()

    fun isNotEmpty() = storage.isNotEmpty()

    operator fun get(index: Int) = EffectPlacement(storage.get(index))

    fun last() = EffectPlacement(storage.last())

    fun add(element: EffectPlacement) = storage.add(element.value)

    operator fun set(index: Int, element: EffectPlacement) =
        EffectPlacement(storage.set(index, element.value))
}
