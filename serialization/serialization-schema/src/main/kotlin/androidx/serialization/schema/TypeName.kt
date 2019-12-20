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

package androidx.serialization.schema

import kotlin.math.min

/**
 * The qualified name of a declared, wrapper, or well-known type.
 *
 * For example, the nested enum class `Bar` below would be represented as
 * `TypeName("com.example", "Foo", "Bar")`
 *
 * ```kotlin
 * package com.example
 *
 * data class Foo(@Field(1) val bar: Bar) {
 *     enum class Bar {
 *         @EnumValue(0)
 *         ZERO,
 *         @EnumValue(1)
 *         ONE
 *     }
 * }
 * ```
 *
 * @property packageName The package containing the type, or null for the default package.
 * @property names A list of nested type names.
 */
class TypeName(
    packageName: String? = null,
    val names: List<String>
) : Comparable<TypeName> {
    init {
        require(names.isNotEmpty()) { "names must contain at least one name" }
        require(names.none { it.isEmpty() }) {
            "names must not contain empty names. names: $names"
        }
    }

    constructor(
        packageName: String?,
        vararg names: String
    ) : this(packageName, names.asList())

    val packageName: String? = packageName?.ifEmpty { null }

    /**
     * The simple name of the type.
     *
     * For `com.example.Foo.Bar`, this is `Bar`.
     */
    val simpleName: String = names.last()

    /**
     * The canonical name of the type, separated with dots.
     */
    val canonicalName: String by lazy {
        buildString {
            if (packageName != null) {
                append(packageName)
                append(".")
            }
            names.joinTo(this, ".")
        }
    }

    override fun toString(): String = canonicalName

    override fun hashCode(): Int {
        return (packageName.hashCode() * 31) + names.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is TypeName -> this.packageName == other.packageName && this.names == other.names
            else -> false
        }
    }

    override fun compareTo(other: TypeName): Int {
        compareValues(this.packageName, other.packageName).let { if (it != 0) return it }
        if (this.names === other.names) return 0

        for (i in 0 until min(this.names.size, other.names.size)) {
            this.names[i].compareTo(other.names[i]).let { if (it != 0) return it }
        }

        return (this.names.size - other.names.size).coerceIn(-1, 1)
    }
}
