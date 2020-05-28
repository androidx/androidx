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

import androidx.ui.core.simpleIdentityToString
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

    /**
     * Whether the semantic information provided by the owning component and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning component.
     */
    var isMergingSemanticsOfDescendants: Boolean = false

    /**
     * Whether this configuration is empty.
     *
     * An empty configuration doesn't contain any semantic information that it
     * wants to contribute to the semantics tree.
     */
    val isEmpty: Boolean
        get() = props.isEmpty() && !isMergingSemanticsOfDescendants

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Absorb the semantic information from `other` into this configuration.
     *
     * This adds the semantic information of both configurations and saves the result in this
     * configuration.
     *
     * The [other] configuration must not contain any properties that cannot be
     * merged into this configuration.
     *
     * @param ignoreAlreadySet if true, ignore properties that are already set instead of merging
     */
    internal fun absorb(other: SemanticsConfiguration, ignoreAlreadySet: Boolean = false) {
        if (other.isMergingSemanticsOfDescendants) {
            isMergingSemanticsOfDescendants = true
        }

        for (entry in other.props) {
            val key = entry.key
            if (props.containsKey(key)) {
                if (!ignoreAlreadySet) {
                    @Suppress("UNCHECKED_CAST")
                    key as SemanticsPropertyKey<Any?>
                    props[key] = key.merge(props[key], entry.value)
                }
            } else {
                props[key] = entry.value
            }
        }
    }

    /** Returns an exact copy of this configuration. */
    fun copy(): SemanticsConfiguration {
        val copy = SemanticsConfiguration()
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.props.putAll(props)
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemanticsConfiguration

        if (isMergingSemanticsOfDescendants != other.isMergingSemanticsOfDescendants) return false
        if (props != other.props) return false

        return true
    }

    override fun hashCode(): Int {
        var result = props.hashCode()
        result = 31 * result + isMergingSemanticsOfDescendants.hashCode()
        return result
    }

    private val CommaSeparator = ", "
    override fun toString(): String {
        val propsString = StringBuilder()
        var nextSeparator = ""

        if (isMergingSemanticsOfDescendants) {
            propsString.append(nextSeparator)
            propsString.append("mergeDescendants=true")
            nextSeparator = CommaSeparator
        }

        for ((key, value) in props) {
            propsString.append(nextSeparator)
            propsString.append(key.name)
            propsString.append(" : ")
            propsString.append(value)
            nextSeparator = CommaSeparator
        }
        return "${simpleIdentityToString(this@SemanticsConfiguration)}{ $propsString }"
    }
}

fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
    return getOrElseNullable(key) { null }
}
