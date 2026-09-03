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

import androidx.collection.MutableObjectList
import androidx.collection.ScatterSet
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.snapshots.SnapshotStateSet
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
    private var nestedStyleKeys = SnapshotStateSet<NestedStyleKey>()

    // Testing hooks. This hook allows testing local resolution without an attached node.
    private var parentOverride: StyleResolver? = null

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

    internal fun bindParent(parent: StyleResolver) {
        parentOverride = parent
    }

    internal fun unbind() {
        _node = null
        parentOverride = null
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
                            traverseAncestors {
                                result = this@isSet in it.resolvedLocals
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
                    traverseAncestors { ancestor ->
                        result = properties.any { it in ancestor.resolvedLocals }
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

    internal fun collectForTests(density: Density) {
        collector.collect(style, node, density)
        node.markResolvedForTesting()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> resolveLocal(local: StyleProperty<T>): T? {
        var result: T? = null
        traverseAncestors {
            result = it.resolvedLocals.getOrNull(local)
            result == null
        }
        return result
    }

    internal fun traverseAncestors(block: (StyleResolver) -> Boolean) {
        var parentOverride = parentOverride
        if (parentOverride != null) {
            while (parentOverride != null) {
                if (!block(parentOverride)) return
                parentOverride = parentOverride.parentOverride
            }
            return
        }
        node.traverseAncestors(StyleResolverNodeKey) {
            if (it !is StyleResolverNode) return@traverseAncestors true
            block(it.styleResolver)
        }
    }

    internal fun updateProperties(
        properties: StyleProperties,
        newNestedStyleKeys: ScatterSet<NestedStyleKey>?,
    ) {
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

        if (newNestedStyleKeys == null || newNestedStyleKeys.isEmpty()) {
            nestedStyleKeys.clear()
        } else {
            if (
                nestedStyleKeys.size == 1 &&
                    newNestedStyleKeys.size == 1 &&
                    newNestedStyleKeys.first() !in nestedStyleKeys
            ) {
                // This avoids an `asSet()` allocation if both sets only have one element.
                nestedStyleKeys.clear()
                nestedStyleKeys.add(newNestedStyleKeys.first())
            } else {
                // This pattern ensures that nestedStyleKeys is not modified when
                // newNestedStyleKeys has the same keys. addAll() does not modify the set if no
                // new elements are added and retainAll() doesn't modify the set if no elements are
                // removed. This pattern requires two allocations for the `asSet()` and the
                // enumerator, but it is worth it to avoid changing nestedStyleKeys as changing
                // it will invalidate all styles that read it. There are special cases for sizes 0
                // and 1 to avoid the additional allocations in common cases.
                @Suppress("AsCollectionCall") nestedStyleKeys.addAll(newNestedStyleKeys.asSet())
                nestedStyleKeys.retainAll { it in newNestedStyleKeys }
            }
        }

        resolvedAtLeastOnce = true
    }

    internal fun resolveNestedStyle(
        key: NestedStyleKey,
        block: (style: CommonStyle, state: StyleState) -> Unit,
    ) {
        // Mostly, there will only be one parent that contributes to the list of styles to
        // evaluate, so this avoids allocating a list until we really need one. This value is
        // null if no parents have this key, StyleResolver if only one parent contains this
        // key, or a MutableObjectList<StyleResolver> if more than one contains this key.
        var resolvers: Any? = null
        traverseAncestors { resolver ->
            // This serves two purposes. First, it ensures we are not running any style that does
            // not have a local style defined that we are interested in. Second, if the local style
            // is conditionally provided by the parent, then the child will invalidate when the
            // set of nested styles changes.
            if (key !in resolver.nestedStyleKeys) return@traverseAncestors true
            resolvers =
                // This avoids allocating a list if we only have one parent (the expected case)
                // that provides the style key.
                when (val r = resolvers) {
                    is StyleResolver -> mutableObjectListOf(r, resolver)
                    is MutableObjectList<*> -> {
                        // This cast is safe as the list is created in the previous case with this
                        // type.
                        @Suppress("UNCHECKED_CAST")
                        r as MutableObjectList<StyleResolver>
                        r.add(resolver)
                        r
                    }
                    else -> resolver
                }
            true
        }

        when (val r = resolvers) {
            is StyleResolver -> {
                block(r.style, r.styleState)
            }
            is MutableObjectList<*> -> {
                // Iterate the nodes backwards. The nodes are added from nearest parent to
                // furthest, but should be executed from the furthest to the nearest.
                @Suppress("UNCHECKED_CAST")
                r as MutableObjectList<StyleResolver>
                for (index in r.indices.reversed()) {
                    val resolver = r[index]
                    block(resolver.style, resolver.styleState)
                }
            }
        }
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
