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

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.unit.Density

/**
 * The type of the scope of [StyleResolver.resolve] that allows the `block` to read the fully
 * resolved values from a style.
 */
@ExperimentalFoundationStyleApi
public interface StyleResolverScope :
    StylePropertyAccessorScope, CompositionLocalAccessorScope, Density

/**
 * A resolver that will resolve the properties of a given style lambda. An instance of this should
 * be created and passed to the [styleResolver] modifier which will resolve the style lambda in
 * context.
 *
 * @param style a [CommonStyle] that will be invoked to resolve the style properties. [CustomStyle]
 *   types should provide a `toCommonStyle()` extension method that converts the custom style to
 *   [CommonStyle] to be passed as the value of the [style] parameter.
 * @param styleState a [StyleState] instance that is provided to the [StyleStateScope] as the
 *   [StyleStateScope.state] parameter. This allows the [style] to read the [styleState] during
 *   resolution to detect, for example, when the component being styled is focused, pressed,
 *   hovered, etc. in addition to custom states such as `isPlaying`.
 */
@ExperimentalFoundationStyleApi
public class StyleResolver(
    internal val style: CommonStyle,
    internal val styleState: StyleState = MutableStyleState(null),
) : RememberObserver {
    internal val resolvedProperties = ResolvedProperties()
    internal val resolvedLocals = ResolvedProperties()
    internal val animations = StyleAnimations()

    private var _node: StyleResolverNode? = null
    private var resolvedAtLeastOnce: Boolean = false
    private val collector = StylePropertyCollector()

    // Testing hooks. This hook allows testing local resolution without an attached node.
    private var _resolveParentLocal: ((local: StyleProperty<Any>) -> Any?)? = null

    private val node: StyleResolverNode
        get() =
            _node
                ?: error(
                    "Unbound style resolver. Cannot read an style resolver before it has been bound to " +
                        "a styleResolver modifier."
                )

    /**
     * Asks the [StyleResolver] to resolve the properties of a given style lambda. The resolved
     * properties can be read in [block].
     *
     * @param block called in a scope that allows style properties to be read.
     */
    public inline fun <R> resolve(block: StyleResolverScope.() -> R): R = accessorScope.block()

    /**
     * Implements [RememberObserver] API to allow composition to manage the lifetime of the
     * [StyleResolver]
     */
    override fun onRemembered() {
        // Nothing to do
    }

    /**
     * Implements [RememberObserver] API to allow composition to manage the lifetime of the
     * [StyleResolver]
     */
    override fun onForgotten() {
        dispose()
    }

    /**
     * Implements [RememberObserver] API to allow composition to manage the lifetime of the
     * [StyleResolver]
     */
    override fun onAbandoned() {
        dispose()
    }

    internal fun bind(node: StyleResolverNode) {
        if (_node != null)
            error("Cannot bind a style resolver to more than one styleResolver modifier")
        _node = node
    }

    internal fun unbind() {
        _node = null
    }

    private val _accessorScope: StyleResolverScope =
        object : StyleResolverScope {
            override val <T> StyleProperty<T>.value: T
                get() =
                    if (isLocal) {
                        resolvedLocals.getOrElse(this) { resolveLocal(this) ?: defaultValue() }
                    } else {
                        resolvedProperties.getOrElse(this, defaultValue)
                    }

            override val StyleProperty<*>.isSet: Boolean
                get() =
                    if (isLocal) {
                        if (this in resolvedLocals) {
                            true
                        } else {
                            var result = false
                            node.traverseAncestors(StyleResolverNodeKey) {
                                if (it !is StyleResolverNode) return@traverseAncestors true
                                result = this@isSet in it.styleResolver.resolvedLocals
                                !result
                            }
                            result
                        }
                    } else {
                        this in resolvedProperties
                    }

            override fun <T> getOrNull(property: StyleProperty<T>): T? =
                if (property.isLocal) {
                    resolvedLocals.getOrNull(property) ?: resolveLocal(property)
                } else {
                    resolvedProperties.getOrNull(property)
                }

            override fun anySet(properties: Set<StyleProperty<*>>): Boolean {
                var anyLocals = false
                var result = properties.any {
                    if (it.isLocal) {
                        anyLocals = true
                        it in resolvedLocals
                    } else {
                        it in resolvedProperties
                    }
                }
                if (!result && anyLocals) {
                    node.traverseAncestors(StyleResolverNodeKey) { ancestor ->
                        if (ancestor !is StyleResolverNode) return@traverseAncestors true
                        result = properties.any { it in ancestor.styleResolver.resolvedLocals }
                        !result
                    }
                }
                return result
            }

            override val <T> CompositionLocal<T>.currentValue: T
                get() = node.currentValueOf(this)

            override val density: Float
                get() = node.requireDensity().density

            override val fontScale: Float
                get() = node.requireDensity().fontScale
        }

    @PublishedApi
    internal val accessorScope: StyleResolverScope
        get() {
            node.ensureResolved()
            if (!resolvedAtLeastOnce)
                error(
                    "Unresolved style properties. A style resolved property cannot " +
                        "be used until it is both bound and resolved. For example, a style resolver is only " +
                        "bound and resolved after composition so the resolved values of a style cannot be " +
                        "used in composition"
                )
            return _accessorScope
        }

    internal fun dispose() {
        unbind()
        animations.close()
    }

    internal fun collect() {
        val density = node.requireDensity()
        collector.collect(style, node, density)
    }

    internal fun collectForTests(
        density: Density,
        resolveParentLocal: ((local: StyleProperty<Any>) -> Any?)? = null,
    ) {
        _resolveParentLocal = resolveParentLocal ?: { null }
        collector.collect(style, node, density)
        node.markResolvedForTesting()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> resolveLocal(local: StyleProperty<T>): T? {
        _resolveParentLocal?.let {
            return it(local as StyleProperty<Any>) as T?
        }
        var result: T? = null
        node.traverseAncestors(StyleResolverNodeKey) {
            if (it !is StyleResolverNode) return@traverseAncestors true
            result = it.styleResolver.resolvedLocals.getOrNull(local)
            result == null
        }
        return result
    }

    internal fun updateProperties(properties: StyleProperties) {

        // Update all the properties that are now resolved
        properties.forEach { property, value ->
            @Suppress("UNCHECKED_CAST")
            property as StyleProperty<Any?>
            if (property.isLocal) {
                resolvedLocals.update(property, animations, value)
            } else {
                resolvedProperties.update(property, animations, value)
            }
        }

        resolvedAtLeastOnce = true
    }

    internal fun discardUnneededProperties(current: StyleProperties, previous: StyleProperties) {
        // For values that will eventually be removed, return them to the parent value (or default
        // if no parent has the value). We cannot just remove them yet as they may be animating and
        // we need the placeholder for the animation until it finishes.
        resolvedProperties.forEach { (property, value) ->
            if (property !in current) {
                val newValue = property.defaultValue() ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                (value as AnimatableStylePropertyValue<Any>).update(newValue)
            }
        }

        // Remove all values that are no longer needed
        resolvedProperties.removeIf {
            it !in current && it !in previous && !animations.animating(it)
        }

        // For values that will eventually be removed, return them to the parent value (or default
        // if no parent has the value). We cannot just remove them yet as they may be animating and
        // we need the placeholder for the animation until it finishes.
        resolvedLocals.forEach { (property, value) ->
            if (property !in current) {
                val newValue = resolveLocal(property) ?: property.defaultValue() ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                (value as AnimatableStylePropertyValue<Any>).update(newValue)
            }
        }

        // Remove all values that are not needed.
        resolvedLocals.removeIf { it !in current && it !in previous && !animations.animating(it) }
    }
}
