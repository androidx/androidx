/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalSerializationApi::class)

package androidx.navigation.serialization

import androidx.core.bundle.Bundle
import androidx.navigation.NavType
import kotlin.reflect.KType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer

/**
 * Marker for Native Kotlin types with either full or partial built-in NavType support
 */
private enum class InternalType {
    INT,
    BOOL,
    FLOAT,
    LONG,
    STRING,
    INT_ARRAY,
    BOOL_ARRAY,
    FLOAT_ARRAY,
    LONG_ARRAY,
    ARRAY,
    LIST,
    UNKNOWN
}

/**
 * Converts an argument type to a built-in NavType.
 *
 * Built-in NavTypes include NavType objects declared within [NavType.Companion], such as
 * [NavType.IntType], [NavType.BoolArrayType] etc.
 *
 * Returns [UNKNOWN] type if the argument does not have built-in NavType support.
 */
internal fun SerialDescriptor.getNavType(): NavType<*> {
    val type = when (this.toInternalType()) {
        InternalType.INT -> NavType.IntType
        InternalType.BOOL -> NavType.BoolType
        InternalType.FLOAT -> NavType.FloatType
        InternalType.LONG -> NavType.LongType
        InternalType.STRING -> NavType.StringType
        InternalType.INT_ARRAY -> NavType.IntArrayType
        InternalType.BOOL_ARRAY -> NavType.BoolArrayType
        InternalType.FLOAT_ARRAY -> NavType.FloatArrayType
        InternalType.LONG_ARRAY -> NavType.LongArrayType
        InternalType.ARRAY -> {
            val typeParameter = getElementDescriptor(0).toInternalType()
            if (typeParameter == InternalType.STRING) NavType.StringArrayType else UNKNOWN
        }
        InternalType.LIST -> {
            val typeParameter = getElementDescriptor(0).toInternalType()
            when (typeParameter) {
                InternalType.INT -> NavType.IntListType
                InternalType.BOOL -> NavType.BoolListType
                InternalType.FLOAT -> NavType.FloatListType
                InternalType.LONG -> NavType.LongListType
                InternalType.STRING -> NavType.StringListType
                else -> UNKNOWN
            }
        }
        else -> UNKNOWN
    }
    return type
}

/**
 * Convert SerialDescriptor to an InternalCommonType.
 *
 * The descriptor's associated argument could be any of the native Kotlin types supported
 * in [InternalType], or it could be an unsupported type (custom class, object or enum).
 */
private fun SerialDescriptor.toInternalType(): InternalType {
    val serialName = serialName.replace("?", "")
    return when {
        serialName == "kotlin.Int" -> InternalType.INT
        serialName == "kotlin.Boolean" -> InternalType.BOOL
        serialName == "kotlin.Float" -> InternalType.FLOAT
        serialName == "kotlin.Long" -> InternalType.LONG
        serialName == "kotlin.String" -> InternalType.STRING
        serialName == "kotlin.IntArray" -> InternalType.INT_ARRAY
        serialName == "kotlin.BooleanArray" -> InternalType.BOOL_ARRAY
        serialName == "kotlin.FloatArray" -> InternalType.FLOAT_ARRAY
        serialName == "kotlin.LongArray" -> InternalType.LONG_ARRAY
        serialName == "kotlin.Array" -> InternalType.ARRAY
        // serial name for both List and ArrayList
        serialName.startsWith("kotlin.collections.ArrayList") -> InternalType.LIST
        // custom classes or other types without built-in NavTypes
        else -> InternalType.UNKNOWN
    }
}

/**
 * Match the [SerialDescriptor] of a type to a KType
 *
 * Returns true match, false otherwise.
 */
internal fun SerialDescriptor.matchKType(kType: KType): Boolean {
    if (this.isNullable != kType.isMarkedNullable) return false
    if (this.hashCode() != serializer(kType).descriptor.hashCode()) return false
    return true
}

internal object UNKNOWN : NavType<String>(false) {
    override val name: String
        get() = "unknown"
    override fun put(bundle: Bundle, key: String, value: String) {}
    override fun get(bundle: Bundle, key: String): String? = null
    override fun parseValue(value: String): String = "null"
}
