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
import androidx.navigation.NavType.Companion.parseSerializableOrParcelableType
import kotlin.reflect.KType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

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
internal fun SerialDescriptor.getNavType(): NavType<*> {
    return when (val internalType = this.toInternalType()) {
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
            if (typeParameter == Native.STRING) return NavType.StringArrayType
            if (typeParameter is Custom) {
                return convertCustomToNavType(typeParameter.className, true) ?: UNKNOWN
            }
            return UNKNOWN
        }
        is Custom -> {
            return convertCustomToNavType(internalType.className, false) ?: UNKNOWN
        }
        else -> UNKNOWN
    }
}

private fun convertCustomToNavType(className: String, isArray: Boolean): NavType<*>? {
    // To convert name to a Class<*>, subclasses need to be delimited with `$`. So we need to
    // replace the `.` delimiters in serial names to `$` for subclasses.
    val sequence = className.splitToSequence(".")
    var finalClassName = ""
    sequence.fold(false) { isSubclass, current ->
        if (isSubclass) {
            finalClassName += "$"
        } else {
            if (finalClassName.isNotEmpty()) finalClassName += "."
        }
        finalClassName += current
        if (isSubclass) true else current.toCharArray().first().isUpperCase()
    }
    // then try to parse it to a Serializable or Parcelable
    return try {
        parseSerializableOrParcelableType(finalClassName, isArray)
    } catch (e: ClassNotFoundException) {
        null
    }
}

/**
 * Convert KType to an [InternalCommonType].
 *
 * Conversion is based on KType name. The KType could be any of the native Kotlin types that
 * are supported in [Native], or it could be a custom KType (custom class, object or enum).
 */
private fun KType.toInternalType(): InternalCommonType {
    // first we need to parse KType name to the right format
    // extract base class without type parameters
    val typeParamRegex = Regex(pattern = "^[^<]*", options = setOf(RegexOption.IGNORE_CASE))
    val trimmedTypeParam = typeParamRegex.find(this.toString())
    // remove the warning that was appended to KType name due to missing kotlin reflect library
    val trimEndingRegex = Regex("(\\S+)")
    val trimmedEnding = trimEndingRegex.find(trimmedTypeParam!!.value)
    val finalName = trimmedEnding?.value
        // we assert the nullability directly with isNullable properties so its more reliable
        ?.replace("?", "")
        // we also replace the delimiter `$` with `.` for child classes to match the format of
        // serial names
        ?.replace("$", ".")

    return when (finalName) {
        null -> Native.UNKNOWN
        "int" -> Native.INT
        "java.lang.Integer" -> Native.INT
        "boolean" -> Native.BOOL
        "java.lang.Boolean" -> Native.BOOL
        "float" -> Native.FLOAT
        "java.lang.Float" -> Native.FLOAT
        "long" -> Native.LONG
        "java.lang.Long" -> Native.LONG
        "java.lang.String" -> Native.STRING
        "kotlin.IntArray" -> Native.INT_ARRAY
        "kotlin.BooleanArray" -> Native.BOOL_ARRAY
        "kotlin.FloatArray" -> Native.FLOAT_ARRAY
        "kotlin.LongArray" -> Native.LONG_ARRAY
        "kotlin.Array" -> Native.ARRAY
        "java.util.ArrayList" -> Native.ARRAY_LIST
        // KType List mapped to ArrayList because serialized name does not differentiate them
        "java.util.List" -> Native.ARRAY_LIST
        "java.util.Set" -> Native.SET
        "java.util.HashSet" -> Native.HASHSET
        "java.util.Map" -> Native.MAP
        "java.util.HashMap" -> Native.HASH_MAP
        else -> Custom(finalName)
    }
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
 * Returns true if a serialized argument type matches the given KType.
 *
 * Matching starts by matching the base class of the type and then matching type parameters
 * in their declared order. Nested TypeParameters are matched recursively in the same manner.
 *
 * Limitation: For custom Types, TypeParameters are erased in serialization by default.
 * The type is preserved only if there is a class field of type T, and even then we cannot know
 * that this type is in fact the TypeParameter. Therefore, matching custom types with
 * type parameters only works under two conditions:
 *
 * 1. A class field of that type must be declared for each generic type
 * 2. These class fields must be declared in primary constructor in same order
 *
 * For example, this declaration will work:
 * `class TestClass<T: Any, K: Any>(val arg: T, val arg2: K)`
 * and these variations will not work:
 * `class TestClass<T: Any, K: Any>(val arg: K, val arg2: T)`
 * `class TestClass<T: Any, K: Any>(val other: Int, val arg: K, val arg2: T)`
 *
 * If TypeParameters of custom classes cannot be matched, the custom class will be matched to
 * a KType purely based on the class's fully qualified name and will not be able to differentiate
 * between different TypeParameters. This can lead to indeterminate matching behavior.
 */
internal fun SerialDescriptor.matchKType(kType: KType): Boolean {
    if (this.isNullable != kType.isMarkedNullable) return false
    if (this.toInternalType() != kType.toInternalType()) return false
    var index = 0
    // recursive match nested TypeParameters
    while (index < elementsCount && index < kType.arguments.size) {
        val descriptor = getElementDescriptor(index)
        val childKType = kType.arguments.getOrNull(index)?.type ?: return false
        val result = descriptor.matchKType(childKType)
        if (!result) return false
        index++
    }
    return true
}

internal object UNKNOWN : NavType<String>(false) {
    override val name: String
        get() = "unknown"
    override fun put(bundle: Bundle, key: String, value: String) {}
    override fun get(bundle: Bundle, key: String): String? = null
    override fun parseValue(value: String): String = "null"
}
