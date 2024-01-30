/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.semantics

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each property is intended to be set by the respective SemanticsPropertyReceiver extension instead
 * of used directly.
 */
object SemanticsProperties {
    /**
     * @see SemanticsPropertyReceiver.contentDescription
     */
    val ContentDescription = SemanticsPropertyKey<List<String>>(
        name = "ContentDescription",
        mergePolicy = { parentValue, childValue ->
            parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.testTag
     */
    val TestTag = SemanticsPropertyKey<String>(
        name = "TestTag",
        mergePolicy = { parentValue, _ ->
            // No merge
            parentValue
        }
    )
}

/**
 * SemanticsPropertyKey is the infrastructure for setting key/value pairs inside semantics block in
 * a type-safe way. Each key has one particular statically defined value type T.
 */
class SemanticsPropertyKey<T>(
    /**
     * The name of the property. Should be the same as the constant from shich it is accessed.
     */
    val name: String,
    internal val mergePolicy: (T?, T) -> T? = { parentValue, childValue ->
        parentValue ?: childValue
    }
) {
    fun merge(parentValue: T?, childValue: T): T? {
        return mergePolicy(parentValue, childValue)
    }
}

/**
 * SemanticsPropertyReceiver is the scope provided by semantics {} blocks, letting you set key/value
 * pairs primarily via extension functions.
 */
interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node, for use in testing, accessibility and
 * similar use cases.
 */
var SemanticsPropertyReceiver.contentDescription: String
    /**
     * Throws [UnsupportedOperationException]. Should not be called.
     */
    get() {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly"
        )
    }
    set(value) { set(SemanticsProperties.ContentDescription, listOf(value)) }

/**
 * Test tag attached to this Glance composable node.
 *
 * This is a free form String and can be used to find nodes in testing frameworks.
 */
var SemanticsPropertyReceiver.testTag: String
    /**
     * Throws [UnsupportedOperationException]. Should not be called.
     */
    get() {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly"
        )
    }
    set(value) {
        set(SemanticsProperties.TestTag, value)
    }

/**
 * Describes the semantics information associated with the owning component.
 */
class SemanticsConfiguration : SemanticsPropertyReceiver {
    private val props: MutableMap<SemanticsPropertyKey<*>, Any?> = mutableMapOf()
    override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        props[key] = value
    }

    /**
     * Retrieves the value for the given property, if one has been set,
     * If a value has not been set, throws [IllegalStateException]
     */
    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: SemanticsPropertyKey<T>): T {
        return props.getOrElse(key) {
            throw java.lang.IllegalStateException("Key not present: $key")
        } as T
    }

    /**
     * Retrieves the value for the given property, if one has been set,
     * If a value has not been set, returns the provided default value.
     */
    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrElseNullable(key: SemanticsPropertyKey<T>, defaultValue: () -> T?): T? {
        return props.getOrElse(key, defaultValue) as T?
    }

    /**
     * Retrieves the value for the given property, if one has been set,
     * If a value has not been set, returns null
     */
    fun <T> getOrNull(key: SemanticsPropertyKey<T>): T? {
        return getOrElseNullable(key) { null }
    }
}
