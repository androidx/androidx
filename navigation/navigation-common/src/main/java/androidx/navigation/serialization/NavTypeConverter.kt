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

import android.os.Bundle
import androidx.navigation.NavType
import kotlin.reflect.KType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer

private interface InternalCommonType

/**
 * Marker for Native Kotlin Primitives and Collections
 */
private enum class Native : InternalCommonType {
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
    ARRAY_LIST,
    SET,
    HASHSET,
    MAP,
    HASH_MAP,
    UNKNOWN
}

/**
 * Marker for custom classes, objects, enums, and other Native Kotlin types that are not
 * included in [Native]
 */
private data class Custom(val className: String) : InternalCommonType

/**
 * Converts an argument type to a native NavType.
 *
 * Native NavTypes includes NavType objects declared within [NavType.Companion], or types that are
 * either java Serializable, Parcelable, or Enum.
 *
 * Returns [UNKNOWN] type if the argument does not belong to any of the above.
 */
@Suppress("UNCHECKED_CAST")
internal fun SerialDescriptor.getNavType(): NavType<Any?> {
    val type = when (this.toInternalType()) {
        Native.INT -> NavType.IntType
        Native.BOOL -> NavType.BoolType
        Native.FLOAT -> NavType.FloatType
        Native.LONG -> NavType.LongType
        Native.STRING -> NavType.StringType
        Native.INT_ARRAY -> NavType.IntArrayType
        Native.BOOL_ARRAY -> NavType.BoolArrayType
        Native.FLOAT_ARRAY -> NavType.FloatArrayType
        Native.LONG_ARRAY -> NavType.LongArrayType
        Native.ARRAY -> {
            val typeParameter = getElementDescriptor(0).toInternalType()
            if (typeParameter == Native.STRING) NavType.StringArrayType else UNKNOWN
        }
        else -> UNKNOWN
    }
    return type as NavType<Any?>
}

/**
 * Convert SerialDescriptor to an InternalCommonType.
 *
 * The descriptor's associated argument could be any of the native Kotlin types that
 * are supported in [Native], or it could be a custom type (custom class, object or enum).
 */
private fun SerialDescriptor.toInternalType(): InternalCommonType {
    val serialName = serialName.replace("?", "")
    return when {
        serialName == "kotlin.Int" -> Native.INT
        serialName == "kotlin.Boolean" -> Native.BOOL
        serialName == "kotlin.Float" -> Native.FLOAT
        serialName == "kotlin.Long" -> Native.LONG
        serialName == "kotlin.String" -> Native.STRING
        serialName == "kotlin.IntArray" -> Native.INT_ARRAY
        serialName == "kotlin.BooleanArray" -> Native.BOOL_ARRAY
        serialName == "kotlin.FloatArray" -> Native.FLOAT_ARRAY
        serialName == "kotlin.LongArray" -> Native.LONG_ARRAY
        serialName == "kotlin.Array" -> Native.ARRAY
        // serial name for both List and ArrayList
        serialName.startsWith("kotlin.collections.ArrayList") -> Native.ARRAY_LIST
        serialName.startsWith("kotlin.collections.LinkedHashSet") -> Native.SET
        serialName.startsWith("kotlin.collections.HashSet") -> Native.HASHSET
        serialName.startsWith("kotlin.collections.LinkedHashMap") -> Native.MAP
        serialName.startsWith("kotlin.collections.HashMap") -> Native.HASH_MAP
        else -> Custom(serialName)
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
