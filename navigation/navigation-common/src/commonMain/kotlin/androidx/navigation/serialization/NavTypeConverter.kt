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

import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.navigation.NavUriUtils
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlin.reflect.KType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.serializerOrNull

/** Marker for Native Kotlin types with either full or partial built-in NavType support */
private enum class InternalType {
    INT,
    INT_NULLABLE,
    BOOL,
    BOOL_NULLABLE,
    DOUBLE,
    DOUBLE_NULLABLE,
    FLOAT,
    FLOAT_NULLABLE,
    LONG,
    LONG_NULLABLE,
    STRING,
    STRING_NULLABLE,
    INT_ARRAY,
    BOOL_ARRAY,
    DOUBLE_ARRAY,
    FLOAT_ARRAY,
    LONG_ARRAY,
    ARRAY,
    LIST,
    ENUM,
    ENUM_NULLABLE,
    UNKNOWN,
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
            InternalType.DOUBLE -> InternalNavType.DoubleType
            InternalType.DOUBLE_NULLABLE -> InternalNavType.DoubleNullableType
            InternalType.FLOAT -> NavType.FloatType
            InternalType.FLOAT_NULLABLE -> InternalNavType.FloatNullableType
            InternalType.LONG -> NavType.LongType
            InternalType.LONG_NULLABLE -> InternalNavType.LongNullableType
            InternalType.STRING -> InternalNavType.StringNonNullableType
            InternalType.STRING_NULLABLE -> NavType.StringType
            InternalType.INT_ARRAY -> NavType.IntArrayType
            InternalType.BOOL_ARRAY -> NavType.BoolArrayType
            InternalType.DOUBLE_ARRAY -> InternalNavType.DoubleArrayType
            InternalType.FLOAT_ARRAY -> NavType.FloatArrayType
            InternalType.LONG_ARRAY -> NavType.LongArrayType
            InternalType.ARRAY -> {
                val typeParameter = getElementDescriptor(0).toInternalType()
                when (typeParameter) {
                    InternalType.STRING -> NavType.StringArrayType
                    InternalType.STRING_NULLABLE -> InternalNavType.StringNullableArrayType
                    else -> UNKNOWN
                }
            }
            InternalType.LIST -> {
                val typeParameter = getElementDescriptor(0).toInternalType()
                when (typeParameter) {
                    InternalType.INT -> NavType.IntListType
                    InternalType.BOOL -> NavType.BoolListType
                    InternalType.DOUBLE -> InternalNavType.DoubleListType
                    InternalType.FLOAT -> NavType.FloatListType
                    InternalType.LONG -> NavType.LongListType
                    InternalType.STRING -> NavType.StringListType
                    InternalType.STRING_NULLABLE -> InternalNavType.StringNullableListType
                    InternalType.ENUM -> parseEnumList()
                    else -> UNKNOWN
                }
            }
            InternalType.ENUM -> parseEnum()
            InternalType.ENUM_NULLABLE -> parseNullableEnum()
            else -> UNKNOWN
        }
    return type
}

internal expect fun SerialDescriptor.parseEnum(): NavType<*>

internal expect fun SerialDescriptor.parseNullableEnum(): NavType<*>

internal expect fun SerialDescriptor.parseEnumList(): NavType<*>

/**
 * Convert SerialDescriptor to an InternalCommonType.
 *
 * The descriptor's associated argument could be any of the native Kotlin types supported in
 * [InternalType], or it could be an unsupported type (custom class, object or enum).
 */
private fun SerialDescriptor.toInternalType(): InternalType {
    val serialName = serialName.replace("?", "")
    return when {
        kind == SerialKind.ENUM -> if (isNullable) InternalType.ENUM_NULLABLE else InternalType.ENUM
        serialName == "kotlin.Int" ->
            if (isNullable) InternalType.INT_NULLABLE else InternalType.INT
        serialName == "kotlin.Boolean" ->
            if (isNullable) InternalType.BOOL_NULLABLE else InternalType.BOOL
        serialName == "kotlin.Double" ->
            if (isNullable) InternalType.DOUBLE_NULLABLE else InternalType.DOUBLE
        serialName == "kotlin.Float" ->
            if (isNullable) InternalType.FLOAT_NULLABLE else InternalType.FLOAT
        serialName == "kotlin.Long" ->
            if (isNullable) InternalType.LONG_NULLABLE else InternalType.LONG
        serialName == "kotlin.String" ->
            if (isNullable) InternalType.STRING_NULLABLE else InternalType.STRING
        serialName == "kotlin.IntArray" -> InternalType.INT_ARRAY
        serialName == "kotlin.DoubleArray" -> InternalType.DOUBLE_ARRAY
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
        "Cannot find KSerializer for [${this.serialName}]. If applicable, custom KSerializers " +
            "for custom and third-party KType is currently not supported when declared " +
            "directly on a class field via @Serializable(with = ...). " +
            "Please use @Serializable or @Serializable(with = ...) on the " +
            "class or object declaration."
    }
    return this == kTypeSerializer.descriptor
}

internal object UNKNOWN : NavType<String>(false) {
    override val name: String
        get() = "unknown"

    override fun put(bundle: SavedState, key: String, value: String) {}

    override fun get(bundle: SavedState, key: String): String? = null

    override fun parseValue(value: String): String = "null"
}

internal object InternalNavType {
    val IntNullableType =
        object : NavType<Int?>(true) {
            override val name: String
                get() = "integer_nullable"

            override fun put(bundle: SavedState, key: String, value: Int?) {
                // store null as serializable inside bundle, so that decoder will use the null
                // instead of default value
                if (value == null) bundle.write { putNull(key) }
                else IntType.put(bundle, key, value)
            }

            override fun get(bundle: SavedState, key: String): Int? =
                bundle.read { if (contains(key) && !isNull(key)) getInt(key) else null }

            override fun parseValue(value: String): Int? {
                return if (value == "null") null else IntType.parseValue(value)
            }
        }

    val BoolNullableType =
        object : NavType<Boolean?>(true) {
            override val name: String
                get() = "boolean_nullable"

            override fun put(bundle: SavedState, key: String, value: Boolean?) {
                if (value == null) bundle.write { putNull(key) }
                else BoolType.put(bundle, key, value)
            }

            override fun get(bundle: SavedState, key: String): Boolean? =
                bundle.read { if (contains(key) && !isNull(key)) getBoolean(key) else null }

            override fun parseValue(value: String): Boolean? {
                return if (value == "null") null else BoolType.parseValue(value)
            }
        }

    val DoubleType: NavType<Double> =
        object : NavType<Double>(false) {
            override val name: String
                get() = "double"

            override fun put(bundle: SavedState, key: String, value: Double) {
                bundle.write { putDouble(key, value) }
            }

            override fun get(bundle: SavedState, key: String): Double =
                bundle.read { getDouble(key) }

            override fun parseValue(value: String): Double = value.toDouble()
        }

    val DoubleNullableType: NavType<Double?> =
        object : NavType<Double?>(true) {
            override val name: String
                get() = "double_nullable"

            override fun put(bundle: SavedState, key: String, value: Double?) {
                if (value == null) bundle.write { putNull(key) }
                else DoubleType.put(bundle, key, value)
            }

            override fun get(bundle: SavedState, key: String): Double? =
                bundle.read { if (contains(key) && !isNull(key)) getDouble(key) else null }

            override fun parseValue(value: String): Double? {
                return if (value == "null") null else DoubleType.parseValue(value)
            }
        }

    val FloatNullableType =
        object : NavType<Float?>(true) {
            override val name: String
                get() = "float_nullable"

            override fun put(bundle: SavedState, key: String, value: Float?) {
                if (value == null) bundle.write { putNull(key) }
                else FloatType.put(bundle, key, value)
            }

            override fun get(bundle: SavedState, key: String): Float? =
                bundle.read { if (contains(key) && !isNull(key)) getFloat(key) else null }

            override fun parseValue(value: String): Float? {
                return if (value == "null") null else FloatType.parseValue(value)
            }
        }

    val LongNullableType =
        object : NavType<Long?>(true) {
            override val name: String
                get() = "long_nullable"

            override fun put(bundle: SavedState, key: String, value: Long?) {
                if (value == null) bundle.write { putNull(key) }
                else LongType.put(bundle, key, value)
            }

            override fun get(bundle: SavedState, key: String): Long? =
                bundle.read { if (contains(key) && !isNull(key)) getLong(key) else null }

            override fun parseValue(value: String): Long? {
                return if (value == "null") null else LongType.parseValue(value)
            }
        }

    val StringNonNullableType =
        object : NavType<String>(false) {
            override val name: String
                get() = "string_non_nullable"

            override fun put(bundle: SavedState, key: String, value: String) {
                bundle.write { putString(key, value) }
            }

            override fun get(bundle: SavedState, key: String): String =
                bundle.read { if (contains(key) && !isNull(key)) getString(key) else "null" }

            // "null" is still parsed as "null"
            override fun parseValue(value: String): String = value

            // "null" is still serialized as "null"
            override fun serializeAsValue(value: String): String = NavUriUtils.encode(value)
        }

    val StringNullableArrayType: NavType<Array<String?>?> =
        object : CollectionNavType<Array<String?>?>(true) {
            override val name: String
                get() = "string_nullable[]"

            override fun put(bundle: SavedState, key: String, value: Array<String?>?) {
                bundle.write {
                    if (value == null) putNull(key)
                    else putStringArray(key, value.map { it ?: "null" }.toTypedArray())
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun get(bundle: SavedState, key: String): Array<String?>? =
                bundle.read {
                    if (contains(key) && !isNull(key)) {
                        getStringArray(key).map { StringType.parseValue(it) }.toTypedArray()
                    } else null
                }

            // match String? behavior where null -> null, and "null" -> null
            override fun parseValue(value: String): Array<String?> =
                arrayOf(StringType.parseValue(value))

            override fun parseValue(
                value: String,
                previousValue: Array<String?>?,
            ): Array<String?>? = previousValue?.plus(parseValue(value)) ?: parseValue(value)

            override fun valueEquals(value: Array<String?>?, other: Array<String?>?): Boolean =
                value.contentDeepEquals(other)

            override fun serializeAsValues(value: Array<String?>?): List<String> =
                value?.map { it?.let { NavUriUtils.encode(it) } ?: "null" } ?: emptyList()

            override fun emptyCollection(): Array<String?>? = arrayOf()
        }

    val StringNullableListType: NavType<List<String?>?> =
        object : CollectionNavType<List<String?>?>(true) {
            override val name: String
                get() = "List<String?>"

            override fun put(bundle: SavedState, key: String, value: List<String?>?) {
                bundle.write {
                    if (value == null) putNull(key)
                    else putStringArray(key, value.map { it ?: "null" }.toTypedArray())
                }
            }

            override fun get(bundle: SavedState, key: String): List<String?>? =
                bundle.read {
                    if (contains(key) && !isNull(key)) {
                        getStringArray(key).toList().map { StringType.parseValue(it) }
                    } else null
                }

            override fun parseValue(value: String): List<String?> {
                return listOf(StringType.parseValue(value))
            }

            override fun parseValue(value: String, previousValue: List<String?>?): List<String?>? {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }

            override fun valueEquals(value: List<String?>?, other: List<String?>?): Boolean {
                val valueArray = value?.toTypedArray()
                val otherArray = other?.toTypedArray()
                return valueArray.contentDeepEquals(otherArray)
            }

            override fun serializeAsValues(value: List<String?>?): List<String> =
                value?.map { it?.let { NavUriUtils.encode(it) } ?: "null" } ?: emptyList()

            override fun emptyCollection(): List<String?> = emptyList()
        }

    val DoubleArrayType: NavType<DoubleArray?> =
        object : CollectionNavType<DoubleArray?>(true) {
            override val name: String
                get() = "double[]"

            override fun put(bundle: SavedState, key: String, value: DoubleArray?) {
                bundle.write { if (value == null) putNull(key) else putDoubleArray(key, value) }
            }

            override fun get(bundle: SavedState, key: String): DoubleArray? =
                bundle.read { if (contains(key) && !isNull(key)) getDoubleArray(key) else null }

            override fun parseValue(value: String): DoubleArray =
                doubleArrayOf(DoubleType.parseValue(value))

            override fun parseValue(value: String, previousValue: DoubleArray?): DoubleArray =
                previousValue?.plus(parseValue(value)) ?: parseValue(value)

            override fun valueEquals(value: DoubleArray?, other: DoubleArray?): Boolean {
                val valueArray = value?.toTypedArray()
                val otherArray = other?.toTypedArray()
                return valueArray.contentDeepEquals(otherArray)
            }

            override fun serializeAsValues(value: DoubleArray?): List<String> =
                value?.toList()?.map { it.toString() } ?: emptyList()

            override fun emptyCollection(): DoubleArray = doubleArrayOf()
        }

    public val DoubleListType: NavType<List<Double>?> =
        object : CollectionNavType<List<Double>?>(true) {
            override val name: String
                get() = "List<Double>"

            override fun put(bundle: SavedState, key: String, value: List<Double>?) {
                bundle.write {
                    if (value == null) putNull(key) else putDoubleArray(key, value.toDoubleArray())
                }
            }

            override fun get(bundle: SavedState, key: String): List<Double>? =
                bundle.read {
                    if (contains(key) && !isNull(key)) getDoubleArray(key).toList() else null
                }

            override fun parseValue(value: String): List<Double> =
                listOf(DoubleType.parseValue(value))

            override fun parseValue(value: String, previousValue: List<Double>?): List<Double>? =
                previousValue?.plus(parseValue(value)) ?: parseValue(value)

            override fun valueEquals(value: List<Double>?, other: List<Double>?): Boolean {
                val valueArray = value?.toTypedArray()
                val otherArray = other?.toTypedArray()
                return valueArray.contentDeepEquals(otherArray)
            }

            override fun serializeAsValues(value: List<Double>?): List<String> =
                value?.map { it.toString() } ?: emptyList()

            override fun emptyCollection(): List<Double> = emptyList()
        }
}
