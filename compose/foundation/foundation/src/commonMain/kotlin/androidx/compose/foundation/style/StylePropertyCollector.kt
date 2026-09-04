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

import androidx.collection.MutableObjectIntMap
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Density
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalFoundationStyleApi::class)
private typealias SpecMap = MutableScatterMap<StyleProperty<*>, AnimationSpec<Float>>

// A private object used as a tracking instance for when a spec is not specified.
private object UnspecifiedAnimationSpec : FiniteAnimationSpec<Float> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedFiniteAnimationSpec<V> {
        error("Not implemented")
    }
}

@ExperimentalFoundationStyleApi
internal class StylePropertyCollector : CommonStyleScope {
    private var node: StyleResolverNode? = null
    private var _density: Float = 1f
    private var _fontScale: Float = 1f
    private var properties: StyleProperties? = null
    private var layers: MutableObjectIntMap<StyleProperty<*>>? = null
    private var previous: StyleProperties? = null
    private var animatingProperties: MutableScatterSet<StyleProperty<*>>? = null
    private var previousAnimatingProperties: MutableScatterSet<StyleProperty<*>>? = null
    private var changes: MutableScatterSet<StyleProperty<*>>? = null
    private var effectiveAnimations: MutableScatterSet<StyleProperty<*>>? = null
    private var toSpecs: SpecMap? = null
    private var fromSpecs: SpecMap? = null
    private var previousFromSpecs: SpecMap? = null
    private var nestedStyleKey: NestedStyleKey? = null
    private var nestedStateOverride: StyleState? = null
    private var nestedStyleKeys: MutableScatterSet<NestedStyleKey>? = null
    private var defaultToSpec: AnimationSpec<Float>? = UnspecifiedAnimationSpec
    private var defaultFromSpec: AnimationSpec<Float>? = UnspecifiedAnimationSpec
    private var layer: Int = 0

    override val density: Float
        get() = _density

    override val fontScale: Float
        get() = _fontScale

    override val state: StyleState
        get() = nestedStateOverride ?: node!!.state

    override val <T> CompositionLocal<T>.currentValue: T
        get() = node!!.currentValueOf(this)

    override fun <T> ProvidableStyleProperty<T>.provide(value: T) {
        if (nestedStyleKeys != null) return
        val layers = layers
        val effectiveLayer = layers?.getOrElse(this) { 0 } ?: 0
        if (layer >= effectiveLayer) {
            recordWrite(this, defaultToSpec, defaultFromSpec)
            properties!![this] = value
            if (layer > effectiveLayer) {
                val newLayers =
                    layers
                        ?: MutableObjectIntMap<StyleProperty<*>>().also {
                            this@StylePropertyCollector.layers = it
                        }
                newLayers[this] = layer
            }
        }
    }

    override fun <T> state(
        key: StyleStateKey<T>,
        block: () -> Unit,
        active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
    ) {
        if (active(key, state)) styleLayer(block)
    }

    override fun styleLayer(block: () -> Unit) {
        layer++
        try {
            block()
        } finally {
            layer--
        }
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

    override fun applyNestedStyle(key: NestedStyleKey) {
        val previousKey = nestedStyleKey
        try {
            nestedStyleKey = key
            node!!.styleResolverField.resolveNestedStyle(key) { style, state ->
                val previousStateOverride = nestedStateOverride
                nestedStateOverride = state
                try {
                    with(style) { applyStyle() }
                } finally {
                    nestedStateOverride = previousStateOverride
                }
            }
        } finally {
            nestedStyleKey = previousKey
        }
    }

    /**
     * A style with nested styles is applied in two modes. The first is for the main style. The
     * second mode occurs when [applyNestedStyle] is called in the nested composable. In the first
     * mode, the nested style is skipped (the key is recorded to ensure the nested style is
     * invalidated if a key is added). In the second, everything but the nested style being applied
     * is ignored.
     */
    override fun provideNestedStyle(key: NestedStyleKey, style: CommonStyle) {
        if (nestedStyleKey != key) {
            // Record that the key was provided, but otherwise ignore the style
            (nestedStyleKeys ?: mutableScatterSetOf<NestedStyleKey>().also { nestedStyleKeys = it })
                .add(key)
        } else {
            nestedStyleKey = null
            val previousStateOverride = nestedStateOverride
            nestedStateOverride = null
            try {
                with(style) { applyStyle() }
            } finally {
                nestedStyleKey = key
                nestedStateOverride = previousStateOverride
            }
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
                UnspecifiedAnimationSpec ->
                    if (animatingProperties != null && property in animatingProperties)
                        toSpecs?.get(property) ?: DefaultSpringSpec
                    else null
                else -> to
            }
        val effectiveFrom =
            when (from) {
                UnspecifiedAnimationSpec ->
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
        defaultToSpec = UnspecifiedAnimationSpec
        defaultFromSpec = UnspecifiedAnimationSpec
        fromSpecs = null
        toSpecs = null
        previousAnimatingProperties = animatingProperties
        animatingProperties = null
        layers?.clear()
    }

    internal fun doneCollect() {
        val node = node!!
        this.node = null
        layers?.clear()
        val resolver = node.styleResolverField
        resolver.updateProperties(properties!!, nestedStyleKeys)
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
    if (isNotNullOrEmpty()) {
        (newUnion ?: mutableScatterSetOf<T>().also { newUnion = it }).addAll(this)
    }
    if (other.isNotNullOrEmpty()) {
        (newUnion ?: mutableScatterSetOf<T>().also { newUnion = it }).addAll(other)
    }
    return newUnion
}

@OptIn(ExperimentalContracts::class)
private fun <T> ScatterSet<T>?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && isNotEmpty()
}

internal fun <T> MutableScatterSet<T>?.clearIfNotNull() = this?.let { if (isNotEmpty()) clear() }
