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

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A modifier that binds a [StyleResolver] to the location in the composition it should resolve
 * properties. A [StyleResolver] is required to be passed to one and only one [styleResolver]
 * modifier. An exception will be thrown if a [StyleResolver] instance is supplied to more than one
 * [styleResolver] modifiers.
 *
 * @param styleResolver the resolver to be bound to this modifier.
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleResolver(styleResolver: StyleResolver): Modifier =
    this then StyleResolverElement(styleResolver)

@ExperimentalFoundationStyleApi
internal class StyleResolverElement(val styleResolver: StyleResolver) :
    ModifierNodeElement<StyleResolverNode>() {
    override fun create() = StyleResolverNode(styleResolver, null)

    override fun update(node: StyleResolverNode) {
        if (
            styleResolver is ProxyStyleResolverImpl && node.styleResolverField is StyleResolverImpl
        ) {
            // Update the proxy resolve to the node instead of the node to the proxy resolver.
            styleResolver.bind(node)
        } else {
            node.styleResolver = styleResolver
        }
    }

    override fun hashCode(): Int = styleResolver.hashCode()

    override fun equals(other: Any?): Boolean =
        other is StyleResolverElement && other.styleResolver == styleResolver

    override fun InspectorInfo.inspectableProperties() {
        name = "styleResolver"
    }
}

internal object StyleResolverNodeKey {
    override fun toString(): String = "StyleResolverNodeKey"
}

@ExperimentalFoundationStyleApi
internal class AnimatableStylePropertyValue<T>(
    val property: StyleProperty<T>,
    val initialValue: T,
    val animations: StyleAnimations,
) {
    private val currentValue = mutableStateOf(initialValue)
    private val derivedValue = derivedStateOf {
        animations.animatedValueOrElse(property) { currentValue.value }
    }
    val value: T
        get() = derivedValue.value

    fun update(newValue: T) {
        currentValue.value = newValue
    }

    override fun toString(): String = "$derivedValue"
}

@OptIn(ExperimentalFoundationStyleApi::class)
internal typealias ResolvedPropertyMap<T> =
    SnapshotStateMap<StyleProperty<T>, AnimatableStylePropertyValue<T>>

@ExperimentalFoundationStyleApi
internal inline fun ResolvedPropertyMap<Any?>.removePropertyIf(
    crossinline predicate: (StyleProperty<*>) -> Boolean
) {
    keys.removeAll { predicate(it) }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@JvmInline
internal value class ResolvedProperties(
    val map: ResolvedPropertyMap<Any?> = ResolvedPropertyMap()
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(property: StyleProperty<T>): T {
        val propertyValue =
            (map as ResolvedPropertyMap<T>)[property] ?: return property.defaultValue()
        return propertyValue.value
    }

    operator fun contains(property: StyleProperty<*>) = property in map

    fun <T> getOrNull(property: StyleProperty<T>): T? =
        @Suppress("UNCHECKED_CAST") (map as ResolvedPropertyMap<T>)[property]?.value

    @Suppress("UNCHECKED_CAST")
    inline fun <T> getOrElse(property: StyleProperty<T>, defaultValue: () -> T): T {
        val propertyValue = (map as ResolvedPropertyMap<T>)[property] ?: return defaultValue()
        return propertyValue.value
    }

    fun <T> update(property: StyleProperty<T>, animations: StyleAnimations, value: T) {
        @Suppress("UNCHECKED_CAST") val map = map as ResolvedPropertyMap<T>
        val propertyValue = map[property]
        if (propertyValue == null) {
            val newPropertyValue = AnimatableStylePropertyValue(property, value, animations)
            map[property] = newPropertyValue
        } else propertyValue.update(value)
    }

    inline fun removeIf(crossinline predicate: (StyleProperty<*>) -> Boolean) {
        map.removePropertyIf(predicate)
    }

    inline fun forEach(
        crossinline predicate:
            (Map.Entry<StyleProperty<*>, AnimatableStylePropertyValue<*>>) -> Unit
    ) {
        map.forEach(predicate)
    }
}

@ExperimentalFoundationStyleApi
internal class StyleResolverNode(styleResolver: StyleResolver, val overrideScope: CoroutineScope?) :
    DelegatingNode(), TraversableNode, ObserverModifierNode, CompositionLocalConsumerModifierNode {
    internal var styleResolverField: StyleResolver = styleResolver
    private var resolved = false
    private var sourceJob: Job? = null
    private var currentInteractionSource: InteractionSource? = null
    internal var oldStyle: Style? = null
    internal var styleResolver: StyleResolver
        get() {
            ensureResolved()
            return styleResolverField
        }
        set(value) {
            val previous = styleResolverField
            if (previous != value) {
                previous.unbind()
                styleResolverField = value
                resolved = false
            }
        }

    internal val state: StyleState
        get() = styleResolverField.actual().styleState

    internal val style: CommonStyle
        get() = styleResolverField.actual().style

    internal fun ensureResolved() {
        if (!resolved) {
            resolved = true
            resolveStyle()
        }
    }

    internal fun markResolvedForTesting() {
        resolved = true
    }

    override val traverseKey: Any
        get() = StyleResolverNodeKey

    override fun onObservedReadsChanged() {
        resolveStyle()
    }

    private fun resolveStyle() {
        val resolver = styleResolverField
        observeReads { resolver.actual().collect() }
        val state = resolver.actual().styleState
        val interactionSource = state.interactionSource
        if (interactionSource != currentInteractionSource) {
            updateInteractionSources(state, interactionSource)
        }
    }

    internal val animationScope: CoroutineScope
        get() = overrideScope ?: coroutineScope

    override fun onAttach() {
        styleResolverField.bind(this)
    }

    override fun onDetach() {
        styleResolverField.dispose()
    }

    fun updateInteractionSources(state: StyleState, source: InteractionSource?) {
        sourceJob?.cancel()
        currentInteractionSource = source
        if (source != null) {
            sourceJob = coroutineScope.launch { state.processInteractions(source) }
        }
    }
}
