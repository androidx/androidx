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

package androidx.navigation

import androidx.core.bundle.Bundle

import androidx.annotation.RestrictTo
import androidx.navigation.internal.Uri
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

public actual abstract class NavType<T> actual constructor(
    public actual open val isNullableAllowed: Boolean
) {
    public actual abstract fun put(bundle: Bundle, key: String, value: T)
    public actual abstract operator fun get(bundle: Bundle, key: String): T?
    public actual abstract fun parseValue(value: String): T

    public actual open fun parseValue(value: String, previousValue: T): T = parseValue(value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: Bundle, key: String, value: String): T {
        val parsedValue = parseValue(value)
        put(bundle, key, parsedValue)
        return parsedValue
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: Bundle, key: String, value: String?, previousValue: T): T {
        if (!bundle.containsKey(key)) {
            throw IllegalArgumentException("There is no previous value in this bundle.")
        }
        if (value != null) {
            val parsedCombinedValue = parseValue(value, previousValue)
            put(bundle, key, parsedCombinedValue)
            return parsedCombinedValue
        }
        return previousValue
    }

    public actual open fun serializeAsValue(value: T): String {
        return value.toString()
    }

    public actual open val name: String = "nav_type"

    public actual open fun valueEquals(value: T, other: T): Boolean = value == other

    override fun toString(): String {
        return name
    }

    public actual companion object {
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        @JvmStatic
        public actual open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            return when (type) {
                IntType.name -> IntType
                IntArrayType.name -> IntArrayType
                IntListType.name -> IntListType
                LongType.name -> LongType
                LongArrayType.name -> LongArrayType
                LongListType.name -> LongListType
                BoolType.name -> BoolType
                BoolArrayType.name -> BoolArrayType
                BoolListType.name -> BoolListType
                StringType.name -> StringType
                StringArrayType.name -> StringArrayType
                StringListType.name -> StringListType
                FloatType.name -> FloatType
                FloatArrayType.name -> FloatArrayType
                FloatListType.name -> FloatListType
                else -> {
                    if (!type.isNullOrEmpty()) {
                        throw IllegalArgumentException(
                            "Object of type $type is not supported for navigation arguments."
                        )
                    }
                    StringType
                }
            }
        }

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValue(value: String): NavType<Any> {
            // because we allow Long literals without the L suffix at runtime,
            // the order of IntType and LongType parsing has to be reversed compared to Safe Args
            try {
                IntType.parseValue(value)
                return IntType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                LongType.parseValue(value)
                return LongType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                FloatType.parseValue(value)
                return FloatType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                BoolType.parseValue(value)
                return BoolType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            return StringType as NavType<Any>
        }

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValueType(value: Any?): NavType<Any> {
            return when (value) {
                is Int -> IntType as NavType<Any>
                is IntArray -> IntArrayType as NavType<Any>
                is Long -> LongType as NavType<Any>
                is LongArray -> LongArrayType as NavType<Any>
                is Float -> FloatType as NavType<Any>
                is FloatArray -> FloatArrayType as NavType<Any>
                is Boolean -> BoolType as NavType<Any>
                is BooleanArray -> BoolArrayType as NavType<Any>
                is String, null -> StringType as NavType<Any>
                is Array<*> -> StringArrayType as NavType<Any>
                else -> throw IllegalArgumentException(
                    "$value is not supported for navigation arguments."
                )
            }
        }

        @JvmField
        public actual val IntType: NavType<Int> =
            object : NavType<Int>(false) {
                override val name: String
                    get() = "integer"

                override fun put(bundle: Bundle, key: String, value: Int) {
                    bundle.putInt(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): Int {
                    return bundle[key] as Int
                }

                override fun parseValue(value: String): Int {
                    return if (value.startsWith("0x")) {
                        value.substring(2).toInt(16)
                    } else {
                        value.toInt()
                    }
                }
            }

        @JvmField
        public actual val IntArrayType: NavType<IntArray?> =
            object : CollectionNavType<IntArray?>(true) {
                override val name: String
                    get() = "integer[]"

                override fun put(bundle: Bundle, key: String, value: IntArray?) {
                    bundle.putIntArray(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): IntArray? {
                    return bundle[key] as IntArray?
                }

                override fun parseValue(value: String): IntArray {
                    return intArrayOf(IntType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: IntArray?): IntArray {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: IntArray?, other: IntArray?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: IntArray?): List<String> =
                    value?.toList()?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): IntArray = intArrayOf()
            }

        @JvmField
        public actual val IntListType: NavType<List<Int>?> =
            object : CollectionNavType<List<Int>?>(true) {
                override val name: String
                    get() = "List<Int>"

                override fun put(bundle: Bundle, key: String, value: List<Int>?) {
                    bundle.putIntArray(key, value?.toIntArray())
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): List<Int>? {
                    return (bundle[key] as IntArray?)?.toList()
                }

                override fun parseValue(value: String): List<Int> {
                    return listOf(IntType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: List<Int>?): List<Int>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: List<Int>?, other: List<Int>?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: List<Int>?): List<String> =
                    value?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): List<Int> = emptyList()
            }

        @JvmField
        public actual val LongType: NavType<Long> =
            object : NavType<Long>(false) {
                override val name: String
                    get() = "long"

                override fun put(bundle: Bundle, key: String, value: Long) {
                    bundle.putLong(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): Long {
                    return bundle[key] as Long
                }

                override fun parseValue(value: String): Long {
                    // At runtime the L suffix is optional, contrary to the Safe Args plugin.
                    // This is in order to be able to parse long numbers passed as deep link URL
                    // parameters
                    var localValue = value
                    if (value.endsWith("L")) {
                        localValue = localValue.substring(0, value.length - 1)
                    }
                    return if (value.startsWith("0x")) {
                        localValue.substring(2).toLong(16)
                    } else {
                        localValue.toLong()
                    }
                }
            }

        @JvmField
        public actual val LongArrayType: NavType<LongArray?> =
            object : CollectionNavType<LongArray?>(true) {
                override val name: String
                    get() = "long[]"

                override fun put(bundle: Bundle, key: String, value: LongArray?) {
                    bundle.putLongArray(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): LongArray? {
                    return bundle[key] as LongArray?
                }

                override fun parseValue(value: String): LongArray {
                    return longArrayOf(LongType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: LongArray?): LongArray? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: LongArray?, other: LongArray?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: LongArray?): List<String> =
                    value?.toList()?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): LongArray = longArrayOf()
            }

        @JvmField
        public actual val LongListType: NavType<List<Long>?> =
            object : CollectionNavType<List<Long>?>(true) {
                override val name: String
                    get() = "List<Long>"

                override fun put(bundle: Bundle, key: String, value: List<Long>?) {
                    bundle.putLongArray(key, value?.toLongArray())
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): List<Long>? {
                    return (bundle[key] as LongArray?)?.toList()
                }

                override fun parseValue(value: String): List<Long> {
                    return listOf(LongType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: List<Long>?): List<Long>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: List<Long>?, other: List<Long>?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: List<Long>?): List<String> =
                    value?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): List<Long> = emptyList()
            }

        @JvmField
        public actual val FloatType: NavType<Float> =
            object : NavType<Float>(false) {
                override val name: String
                    get() = "float"

                override fun put(bundle: Bundle, key: String, value: Float) {
                    bundle.putFloat(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): Float {
                    return bundle[key] as Float
                }

                override fun parseValue(value: String): Float {
                    return value.toFloat()
                }
            }

        @JvmField
        public actual val FloatArrayType: NavType<FloatArray?> =
            object : CollectionNavType<FloatArray?>(true) {
                override val name: String
                    get() = "float[]"

                override fun put(bundle: Bundle, key: String, value: FloatArray?) {
                    bundle.putFloatArray(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): FloatArray? {
                    return bundle[key] as FloatArray?
                }

                override fun parseValue(value: String): FloatArray {
                    return floatArrayOf(FloatType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: FloatArray?): FloatArray? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: FloatArray?, other: FloatArray?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: FloatArray?): List<String> =
                    value?.toList()?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): FloatArray = floatArrayOf()
            }

        @JvmField
        public actual val FloatListType: NavType<List<Float>?> =
            object : CollectionNavType<List<Float>?>(true) {
                override val name: String
                    get() = "List<Float>"

                override fun put(bundle: Bundle, key: String, value: List<Float>?) {
                    bundle.putFloatArray(key, value?.toFloatArray())
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): List<Float>? {
                    return (bundle[key] as FloatArray?)?.toList()
                }

                override fun parseValue(value: String): List<Float> {
                    return listOf(FloatType.parseValue(value))
                }

                override fun parseValue(value: String, previousValue: List<Float>?): List<Float>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: List<Float>?, other: List<Float>?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: List<Float>?): List<String> =
                    value?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): List<Float> = emptyList()
            }

        @JvmField
        public actual val BoolType: NavType<Boolean> =
            object : NavType<Boolean>(false) {
                override val name: String
                    get() = "boolean"

                override fun put(bundle: Bundle, key: String, value: Boolean) {
                    bundle.putBoolean(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): Boolean? {
                    return bundle[key] as Boolean?
                }

                override fun parseValue(value: String): Boolean {
                    return when (value) {
                        "true" -> true
                        "false" -> false
                        else -> {
                            throw IllegalArgumentException(
                                "A boolean NavType only accepts \"true\" or \"false\" values."
                            )
                        }
                    }
                }
            }

        @JvmField
        public actual val BoolArrayType: NavType<BooleanArray?> =
            object : CollectionNavType<BooleanArray?>(true) {
                override val name: String
                    get() = "boolean[]"

                override fun put(bundle: Bundle, key: String, value: BooleanArray?) {
                    bundle.putBooleanArray(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): BooleanArray? {
                    return bundle[key] as BooleanArray?
                }

                override fun parseValue(value: String): BooleanArray {
                    return booleanArrayOf(BoolType.parseValue(value))
                }

                override fun parseValue(
                    value: String,
                    previousValue: BooleanArray?
                ): BooleanArray? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: BooleanArray?, other: BooleanArray?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: BooleanArray?): List<String> =
                    value?.toList()?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): BooleanArray = booleanArrayOf()
            }

        @JvmField
        public actual val BoolListType: NavType<List<Boolean>?> =
            object : CollectionNavType<List<Boolean>?>(true) {
                override val name: String
                    get() = "List<Boolean>"

                override fun put(bundle: Bundle, key: String, value: List<Boolean>?) {
                    bundle.putBooleanArray(key, value?.toBooleanArray())
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): List<Boolean>? {
                    return (bundle[key] as BooleanArray?)?.toList()
                }

                override fun parseValue(value: String): List<Boolean> {
                    return listOf(BoolType.parseValue(value))
                }

                override fun parseValue(
                    value: String,
                    previousValue: List<Boolean>?
                ): List<Boolean>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: List<Boolean>?, other: List<Boolean>?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: List<Boolean>?): List<String> =
                    value?.map { it.toString() } ?: emptyList()

                override fun emptyCollection(): List<Boolean> = emptyList()
            }

        @JvmField
        public actual val StringType: NavType<String?> =
            object : NavType<String?>(true) {
                override val name: String
                    get() = "string"

                override fun put(bundle: Bundle, key: String, value: String?) {
                    bundle.putString(key, value)
                }

                @Suppress("DEPRECATION")
                override fun get(bundle: Bundle, key: String): String? {
                    return bundle[key] as String?
                }

                /**
                 * Returns input value by default.
                 *
                 * If input value is "null", returns null as the reversion of Kotlin standard
                 * library serializing null receivers of [kotlin.toString] into "null".
                 */
                override fun parseValue(value: String): String? {
                    return if (value == "null") null else value
                }

                /**
                 * Returns default value of Uri.encode(value).
                 *
                 * If input value is null, returns "null" in compliance with Kotlin standard library
                 * parsing null receivers of [kotlin.toString] into "null".
                 */
                override fun serializeAsValue(value: String?): String {
                    return value?.let { Uri.encode(value) } ?: "null"
                }
            }

        @JvmField
        public actual val StringArrayType: NavType<Array<String>?> =
            object : CollectionNavType<Array<String>?>(true) {
                override val name: String
                    get() = "string[]"

                override fun put(bundle: Bundle, key: String, value: Array<String>?) {
                    @Suppress("UNCHECKED_CAST")
                    bundle.putStringArray(key, value as Array<String?>?)
                }

                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                override fun get(bundle: Bundle, key: String): Array<String>? {
                    return bundle[key] as Array<String>?
                }

                override fun parseValue(value: String): Array<String> {
                    return arrayOf(value)
                }

                override fun parseValue(
                    value: String,
                    previousValue: Array<String>?
                ): Array<String>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: Array<String>?, other: Array<String>?) =
                    value.contentDeepEquals(other)

                override fun serializeAsValues(value: Array<String>?): List<String> =
                    value?.map { Uri.encode(it) } ?: emptyList()

                override fun emptyCollection(): Array<String> = arrayOf()
            }

        @JvmField
        public actual val StringListType: NavType<List<String>?> =
            object : CollectionNavType<List<String>?>(true) {
                override val name: String
                    get() = "List<String>"

                override fun put(bundle: Bundle, key: String, value: List<String>?) {
                    bundle.putStringArray(key, value?.toTypedArray())
                }

                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                override fun get(bundle: Bundle, key: String): List<String>? {
                    return (bundle[key] as Array<String>?)?.toList()
                }

                override fun parseValue(value: String): List<String> {
                    return listOf(value)
                }

                override fun parseValue(
                    value: String,
                    previousValue: List<String>?
                ): List<String>? {
                    return previousValue?.plus(parseValue(value)) ?: parseValue(value)
                }

                override fun valueEquals(value: List<String>?, other: List<String>?): Boolean {
                    val valueArray = value?.toTypedArray()
                    val otherArray = other?.toTypedArray()
                    return valueArray.contentDeepEquals(otherArray)
                }

                override fun serializeAsValues(value: List<String>?): List<String> =
                    value?.map { Uri.encode(it) } ?: emptyList()

                override fun emptyCollection(): List<String> = emptyList()
            }
    }
}
