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
import kotlinx.serialization.serializerOrNull

/** Marker for Native Kotlin types with either full or partial built-in NavType support */
private enum class InternalType {
    INT,
    INT_NULLABLE,
    BOOL,
    BOOL_NULLABLE,
    FLOAT,
    FLOAT_NULLABLE,
    LONG,
    LONG_NULLABLE,
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
    val type =
        when (this.toInternalType()) {
            InternalType.INT -> NavType.IntType
            InternalType.INT_NULLABLE -> InternalNavType.IntNullableType
            InternalType.BOOL -> NavType.BoolType
            InternalType.BOOL_NULLABLE -> InternalNavType.BoolNullableType
            InternalType.FLOAT -> NavType.FloatType
            InternalType.FLOAT_NULLABLE -> InternalNavType.FloatNullableType
            InternalType.LONG -> NavType.LongType
            InternalType.LONG_NULLABLE -> InternalNavType.LongNullableType
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
 * The descriptor's associated argument could be any of the native Kotlin types supported in
 * [InternalType], or it could be an unsupported type (custom class, object or enum).
 */
private fun SerialDescriptor.toInternalType(): InternalType {
    val serialName = serialName.replace("?", "")
    return when {
        serialName == "kotlin.Int" ->
            if (isNullable) InternalType.INT_NULLABLE else InternalType.INT
        serialName == "kotlin.Boolean" ->
            if (isNullable) InternalType.BOOL_NULLABLE else InternalType.BOOL
        serialName == "kotlin.Float" ->
            if (isNullable) InternalType.FLOAT_NULLABLE else InternalType.FLOAT
        serialName == "kotlin.Long" ->
            if (isNullable) InternalType.LONG_NULLABLE else InternalType.LONG
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
 * Returns true if match, false otherwise.
 */
internal fun SerialDescriptor.matchKType(kType: KType): Boolean {
    if (this.isNullable != kType.isMarkedNullable) return false
    val kTypeSerializer = serializerOrNull(kType)
    checkNotNull(kTypeSerializer) {
        "Custom serializers declared directly on a class field via @Serializable(with = ...) " +
            "is currently not supported by safe args for both custom types and third-party " +
            "types. Please use @Serializable or @Serializable(with = ...) on the " +
            "class or object declaration."
    }
    return this == kTypeSerializer.descriptor
}

internal object UNKNOWN : NavType<String>(false) {
    override val name: String
        get() = "unknown"

    override fun put(bundle: Bundle, key: String, value: String) {}

    override fun get(bundle: Bundle, key: String): String? = null

    override fun parseValue(value: String): String = "null"
}

internal object InternalNavType {
    val IntNullableType =
        object : NavType<Int?>(true) {
            override val name: String
                get() = "integer_nullable"

            override fun put(bundle: Bundle, key: String, value: Int?) {
                // store null as serializable inside bundle, so that decoder will use the null
                // instead of default value
                if (value == null) bundle.putBundle(key, null)
                else IntType.put(bundle, key, value)
            }

            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Int? {
                return bundle[key] as? Int
            }

            override fun parseValue(value: String): Int? {
                return if (value == "null") null else IntType.parseValue(value)
            }
        }

    val BoolNullableType =
        object : NavType<Boolean?>(true) {
            override val name: String
                get() = "boolean_nullable"

            override fun put(bundle: Bundle, key: String, value: Boolean?) {
                if (value == null) bundle.putBundle(key, null)
                else BoolType.put(bundle, key, value)
            }

            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Boolean? {
                return bundle[key] as? Boolean
            }

            override fun parseValue(value: String): Boolean? {
                return if (value == "null") null else BoolType.parseValue(value)
            }
        }

    val FloatNullableType =
        object : NavType<Float?>(true) {
            override val name: String
                get() = "float_nullable"

            override fun put(bundle: Bundle, key: String, value: Float?) {
                if (value == null) bundle.putBundle(key, null)
                else FloatType.put(bundle, key, value)
            }

            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Float? {
                return bundle[key] as? Float
            }

            override fun parseValue(value: String): Float? {
                return if (value == "null") null else FloatType.parseValue(value)
            }
        }

    val LongNullableType =
        object : NavType<Long?>(true) {
            override val name: String
                get() = "long_nullable"

            override fun put(bundle: Bundle, key: String, value: Long?) {
                if (value == null) bundle.putBundle(key, null)
                else LongType.put(bundle, key, value)
            }

            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Long? {
                return bundle[key] as? Long
            }

            override fun parseValue(value: String): Long? {
                return if (value == "null") null else LongType.parseValue(value)
            }
        }
}
