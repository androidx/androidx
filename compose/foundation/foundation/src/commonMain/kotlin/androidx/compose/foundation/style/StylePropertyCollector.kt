/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Density

@OptIn(ExperimentalFoundationStyleApi::class)
private typealias SpecMap = MutableScatterMap<StyleProperty<*>, AnimationSpec<Float>>

private val UnspecifiedSpec = spring<Float>()

@ExperimentalFoundationStyleApi
internal class StylePropertyCollector : CommonStyleScope {
    private var node: StyleResolverNode? = null
    private var _density: Float = 1f
    private var _fontScale: Float = 1f
    private var properties: StyleProperties? = null
    private var previous: StyleProperties? = null
    private var animatingProperties: MutableScatterSet<StyleProperty<*>>? = null
    private var previousAnimatingProperties: MutableScatterSet<StyleProperty<*>>? = null
    private var changes: MutableScatterSet<StyleProperty<*>>? = null
    private var effectiveAnimations: MutableScatterSet<StyleProperty<*>>? = null
    private var toSpecs: SpecMap? = null
    private var fromSpecs: SpecMap? = null
    private var previousFromSpecs: SpecMap? = null

    private var defaultToSpec: AnimationSpec<Float>? = UnspecifiedSpec
    private var defaultFromSpec: AnimationSpec<Float>? = UnspecifiedSpec

    override val density: Float
        get() = _density

    override val fontScale: Float
        get() = _fontScale

    override val state: StyleState
        get() = node!!.state

    override val <T> CompositionLocal<T>.currentValue: T
        get() = node!!.currentValueOf(this)

    override fun <T> ProvidableStyleProperty<T>.provide(value: T) {
        recordWrite(this, defaultToSpec, defaultFromSpec)
        properties!![this] = value
    }

    override fun <T> state(
        key: StyleStateKey<T>,
        block: () -> Unit,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    ) {
        if (active(key, state)) block()
    }

    override fun animate(
        toSpec: AnimationSpec<Float>,
        fromSpec: AnimationSpec<Float>,
        block: () -> Unit,
    ) {
        val previousToSpec = defaultToSpec
        val previousFromSpec = defaultFromSpec
        try {
            defaultToSpec = toSpec
            defaultFromSpec = fromSpec

            block()
        } finally {
            defaultToSpec = previousToSpec
            defaultFromSpec = previousFromSpec
        }
    }

    private fun recordWrite(
        property: StyleProperty<*>,
        to: AnimationSpec<Float>?,
        from: AnimationSpec<Float>?,
    ) {
        val animatingProperties = animatingProperties
        // Only update the value of the specification if it was specified, otherwise leave it the
        // same as it was set previously.
        val effectiveTo =
            when (to) {
                UnspecifiedSpec ->
                    if (animatingProperties != null && property in animatingProperties)
                        toSpecs?.get(property) ?: DefaultSpringSpec
                    else null
                else -> to
            }
        val effectiveFrom =
            when (from) {
                UnspecifiedSpec ->
                    if (animatingProperties != null && property in animatingProperties)
                        fromSpecs?.get(property) ?: DefaultSpringSpec
                    else null
                else -> from
            }

        if (effectiveTo == null || effectiveTo == DefaultSpringSpec) {
            toSpecs?.remove(property)
        } else {
            (toSpecs ?: SpecMap().also { toSpecs = it })[property] = effectiveTo
        }
        if (effectiveFrom == null || effectiveFrom == DefaultSpringSpec) {
            fromSpecs?.remove(property)
        } else {
            (fromSpecs ?: SpecMap().also { fromSpecs = it })[property] = effectiveFrom
        }
        val isAnimating = effectiveTo != null && effectiveFrom != null
        if (isAnimating) {
            (animatingProperties
                    ?: mutableScatterSetOf<StyleProperty<*>>().also {
                        this.animatingProperties = it
                    })
                .add(property)
        }
    }

    internal fun collect(style: CommonStyle, node: StyleResolverNode, density: Density) {
        startCollect(node, density)
        with(this) { with(style) { applyStyle() } }
        doneCollect()
    }

    internal fun startCollect(node: StyleResolverNode, density: Density) {
        this.node = node
        this._density = density.density
        val properties = properties
        val newProperties = previous?.also { it.clear() } ?: StyleProperties()
        this.properties = newProperties
        previous = properties
        previousFromSpecs = fromSpecs
        fromSpecs = null
        toSpecs = null
        previousAnimatingProperties = animatingProperties
        animatingProperties = null
    }

    internal fun doneCollect() {
        val node = node!!
        this.node = null
        val resolver = node.styleResolverField.actual()
        resolver.updateProperties(properties!!)
        val animations = resolver.animations
        val animatingProperties = animatingProperties
        val previousAnimatingProperties = previousAnimatingProperties
        val previousFromSpecs = previousFromSpecs
        val properties = properties
        val previous = previous
        if (properties != null && previous != null) {
            if (
                animations.isNotEmpty() ||
                    animatingProperties.isNotEmpty() ||
                    previousAnimatingProperties.isNotEmpty()
            ) {
                // Record the changes into a scatter set, allocating one if null and changes are
                // detected.
                changes = properties.diffInto(previous, changes)

                // We need to record all animations requested plus any changed properties that
                // also used to have a from animation.
                effectiveAnimations =
                    animatingProperties.unionInto(previousAnimatingProperties, effectiveAnimations)

                animations.recordAnimations(
                    effectiveAnimations ?: emptyScatterSet(),
                    changes ?: emptyScatterSet(),
                    toSpecs,
                    fromSpecs,
                    previousFromSpecs,
                    previous,
                    properties,
                    node,
                )

                // Clearing the set as we don't need the diff any longer. It is kept and reused to
                // reduce allocations.
                changes.clearIfNotNull()
            }
            resolver.discardUnneededProperties(properties, previous)
        }
    }
}

private fun <T> ScatterSet<T>?.isNotEmpty() = this != null && this.isNotEmpty()

private fun <T> ScatterSet<T>?.unionInto(
    other: ScatterSet<T>?,
    union: MutableScatterSet<T>?,
): MutableScatterSet<T>? {
    var newUnion = union
    newUnion.clearIfNotNull()

    when {
        this == null || isEmpty() -> {}
        other == null || other.isEmpty() ->
            (newUnion ?: mutableScatterSetOf<T>().also { newUnion = it }).addAll(this)
        else -> {
            (newUnion ?: mutableScatterSetOf<T>().also { newUnion = it }).let {
                it.addAll(this@unionInto)
                it.addAll(other)
            }
        }
    }
    return newUnion
}

internal fun <T> MutableScatterSet<T>?.clearIfNotNull() = this?.let { if (isNotEmpty()) clear() }
