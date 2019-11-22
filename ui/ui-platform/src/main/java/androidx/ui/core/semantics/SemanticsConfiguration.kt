/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.semantics

import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.semantics.SemanticsPropertyReceiver

/**
 * Describes the semantic information associated with the owning component
 *
 * The information provided in the configuration is used to to generate the
 * semantics tree.
 */
class SemanticsConfiguration : SemanticsPropertyReceiver,
    Iterable<Map.Entry<SemanticsPropertyKey<*>, Any?>> {

    private val props: MutableMap<SemanticsPropertyKey<*>, Any?> = mutableMapOf()

    /**
     * Retrieves the value for the given property, if one has been set.
     * If a value has not been set, throws [IllegalStateException]
     */
    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: SemanticsPropertyKey<T>): T {
        return props.getOrElse(key) {
            throw IllegalStateException("Key not present: $key - consider getOrElse or getOrNull")
        } as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrElse(key: SemanticsPropertyKey<T>, defaultValue: () -> T): T {
        return props.getOrElse(key, defaultValue) as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrElseNullable(key: SemanticsPropertyKey<T>, defaultValue: () -> T?): T? {
        return props.getOrElse(key, defaultValue) as T?
    }

    override fun iterator(): Iterator<Map.Entry<SemanticsPropertyKey<*>, Any?>> {
        return props.iterator()
    }

    override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        props[key] = value
    }

    operator fun <T> contains(key: SemanticsPropertyKey<T>): Boolean {
        return props.containsKey(key)
    }

    // SEMANTIC BOUNDARY BEHAVIOR

    /**
     * Whether the owner of this configuration wants to own its
     * own [SemanticsNode].
     *
     * When set to true semantic information associated with the
     * owner of this configuration or any of its descendants will not leak into
     * parents. The [SemanticsNode] generated out of this configuration will
     * act as a boundary.
     *
     * Whether descendants of the owning component can add their semantic
     * information to the [SemanticsNode] introduced by this configuration
     * is controlled by [explicitChildNodes].
     *
     * This has to be true if [isMergingSemanticsOfDescendants] is also true.
     */
    var isSemanticBoundary: Boolean = false
        set(value) {
            assert(!isMergingSemanticsOfDescendants || value)
            field = value
        }

    /**
     * Whether the configuration forces all children of the owning component
     * that want to contribute semantic information to the semantics tree to do
     * so in the form of explicit [SemanticsNode]s.
     *
     * When set to false children of the owning component are allowed to
     * annotate [SemanticNode]s of their parent with the semantic information
     * they want to contribute to the semantic tree.
     * When set to true the only way for children of the owning component
     * to contribute semantic information to the semantic tree is to introduce
     * new explicit [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    var explicitChildNodes = false

    /**
     * Whether the semantic information provided by the owning component and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning component.
     *
     * Setting this to true requires that [isSemanticBoundary] is also true.
     */
    var isMergingSemanticsOfDescendants: Boolean = false
        set(value) {
            // TODO(ryanmentley): Changed this, confirm it's correct
            if (value) {
                assert(isSemanticBoundary)
            }

            field = value
        }

    // SEMANTIC ANNOTATIONS
    // These will end up on [SemanticNode]s generated from [SemanticsConfiguration]s.

    /**
     * Whether this configuration is empty.
     *
     * An empty configuration doesn't contain any semantic information that it
     * wants to contribute to the semantics tree.
     */
    val hasBeenAnnotated: Boolean
        get() = props.isEmpty()

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Absorb the semantic information from `other` into this configuration.
     *
     * This adds the semantic information of both configurations and saves the result in this
     * configuration.
     *
     * Only configurations that have [explicitChildNodes] set to false can absorb other
     * configurations.  The [other] configuration must not contain any properties that cannot be
     * merged into this configuration.
     */
    internal fun absorb(other: SemanticsConfiguration) {
        assert(!explicitChildNodes)

        if (!other.hasBeenAnnotated) {
            return
        }

        for (entry in other.props) {
            val key = entry.key
            if (props.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                key as SemanticsPropertyKey<Any?>
                props[key] = key.merge(props[key], entry.value)
            } else {
                props[key] = entry.value
            }
        }
    }

    /** Returns an exact copy of this configuration. */
    fun copy(): SemanticsConfiguration {
        val copy = SemanticsConfiguration()
        copy.isSemanticBoundary = isSemanticBoundary
        copy.explicitChildNodes = explicitChildNodes
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.props.putAll(props)
        return copy
    }

    // TODO(b/145977727): Remove this after we start using IDs.
    @Deprecated("This is only a temporary until IDs are introduced (b/145977727).")
    fun clear() {
        props.clear()
        isSemanticBoundary = false
        explicitChildNodes = false
        isMergingSemanticsOfDescendants = false
    }
}

fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
    return getOrElseNullable(key) { null }
}
