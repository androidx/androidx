/*
 * Copyright 2018 The Android Open Source Project
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

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.annotation.RestrictTo
import java.io.Serializable

/**
 * NavType denotes the type that can be used in a [NavArgument].
 *
 * There are built-in NavTypes for primitive types, such as int, long, boolean, float, and strings,
 * parcelable, and serializable classes (including Enums), as well as arrays of each supported type.
 *
 * You should only use one of the static NavType instances and subclasses defined in this class.
 *
 * @param T the type of the data that is supported by this NavType
 * @param isNullableAllowed whether an argument with this type can hold a null value.
 */
public abstract class NavType<T>(
    /**
     * Check if an argument with this type can hold a null value.
     *
     * @return Returns true if this type allows null values, false otherwise.
     */
    public open val isNullableAllowed: Boolean
) {

    /**
     * Put a value of this type in the `bundle`
     *
     * @param bundle bundle to put value in
     * @param key bundle key
     * @param value value of this type
     */
    public abstract fun put(bundle: Bundle, key: String, value: T)

    /**
     * Get a value of this type from the `bundle`
     *
     * @param bundle bundle to get value from
     * @param key bundle key
     * @return value of this type
     */
    public abstract operator fun get(bundle: Bundle, key: String): T?

    /**
     * Parse a value of this type from a String.
     *
     * @param value string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     * @throws IllegalArgumentException if value cannot be parsed into this type
     */
    public abstract fun parseValue(value: String): T

    /**
     * Parse a value of this type from a String and then combine that parsed value with the given
     * previousValue of the same type to provide a new value that contains both the new and previous
     * value.
     *
     * By default, the given value will replace the previousValue.
     *
     * @param value string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     * @throws IllegalArgumentException if value cannot be parsed into this type
     */
    public open fun parseValue(value: String, previousValue: T) = parseValue(value)

    /**
     * Parse a value of this type from a String and put it in a `bundle`
     *
     * @param bundle bundle to put value in
     * @param key bundle key under which to put the value
     * @param value string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: Bundle, key: String, value: String): T {
        val parsedValue = parseValue(value)
        put(bundle, key, parsedValue)
        return parsedValue
    }

    /**
     * Parse a value of this type from a String, combine that parsed value with the given
     * previousValue, and then put that combined parsed value in a `bundle`.
     *
     * @param bundle bundle to put value in
     * @param key bundle key under which to put the value
     * @param value string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: Bundle, key: String, value: String?, previousValue: T): T {
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

    /**
     * Serialize a value of this NavType into a String.
     *
     * By default it returns value of [kotlin.toString] or null if value passed in is null.
     *
     * This method can be override for custom serialization implementation on types such custom
     * NavType classes.
     *
     * Note: Final output should be encoded with [Uri.encode]
     *
     * @param value a value representing this NavType to be serialized into a String
     * @return encoded and serialized String value of [value]
     */
    public open fun serializeAsValue(value: T): String {
        return value.toString()
    }

    /**
     * The name of this type.
     *
     * This is the same value that is used in Navigation XML `argType` attribute.
     *
     * @return name of this type
     */
    public open val name: String = "nav_type"

    /**
     * Compares two values of type [T] and returns true if values are equal.
     *
     * @param value the first value for comparison
     * @param other the second value for comparison
     */
    public open fun valueEquals(value: T, other: T): Boolean = value == other

    override fun toString(): String {
        return name
    }

    public companion object {
        /**
         * Parse an argType string into a NavType.
         *
         * @param type argType string, usually parsed from the Navigation XML file
         * @param packageName package name of the R file, used for parsing relative class names
         *   starting with a dot.
         * @return a NavType representing the type indicated by the argType string. Defaults to
         *   StringType for null.
         * @throws IllegalArgumentException if there is no valid argType
         * @throws RuntimeException if the type class name cannot be found
         */
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT", "UNCHECKED_CAST") // this needs to be open to
        // maintain api compatibility and type cast are unchecked
        @JvmStatic
        public open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            when {
                IntType.name == type -> return IntType
                IntArrayType.name == type -> return IntArrayType
                IntListType.name == type -> return IntListType
                LongType.name == type -> return LongType
                LongArrayType.name == type -> return LongArrayType
                LongListType.name == type -> return LongListType
                BoolType.name == type -> return BoolType
                BoolArrayType.name == type -> return BoolArrayType
                BoolListType.name == type -> return BoolListType
                StringType.name == type -> return StringType
                StringArrayType.name == type -> return StringArrayType
                StringListType.name == type -> return StringListType
                FloatType.name == type -> return FloatType
                FloatArrayType.name == type -> return FloatArrayType
                FloatListType.name == type -> return FloatListType
                ReferenceType.name == type -> return ReferenceType
                !type.isNullOrEmpty() -> {
                    try {
                        var className: String
                        className =
                            if (type.startsWith(".") && packageName != null) {
                                packageName + type
                            } else {
                                type
                            }
                        val isArray = type.endsWith("[]")
                        if (isArray) className = className.substring(0, className.length - 2)
                        val clazz = Class.forName(className)
                        return requireNotNull(parseSerializableOrParcelableType(clazz, isArray)) {
                            "$className is not Serializable or Parcelable."
                        }
                    } catch (e: ClassNotFoundException) {
                        throw RuntimeException(e)
                    }
                }
            }
            return StringType
        }

        @Suppress("UNCHECKED_CAST")
        internal fun parseSerializableOrParcelableType(
            clazz: Class<*>,
            isArray: Boolean
        ): NavType<*>? =
            when {
                Parcelable::class.java.isAssignableFrom(clazz) -> {
                    if (isArray) {
                        ParcelableArrayType(clazz as Class<Parcelable>)
                    } else {
                        ParcelableType(clazz as Class<Any?>)
                    }
                }
                Enum::class.java.isAssignableFrom(clazz) && !isArray ->
                    EnumType(clazz as Class<Enum<*>>)
                Serializable::class.java.isAssignableFrom(clazz) -> {
                    if (isArray) {
                        SerializableArrayType(clazz as Class<Serializable>)
                    } else {
                        SerializableType(clazz as Class<Serializable>)
                    }
                }
                else -> null
            }

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValue(value: String): NavType<Any> {
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

        /**
         * @param value nothing
         * @throws IllegalArgumentException not real
         */
        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValueType(value: Any?): NavType<Any> {
            return when {
                value is Int -> IntType as NavType<Any>
                value is IntArray -> IntArrayType as NavType<Any>
                value is Long -> LongType as NavType<Any>
                value is LongArray -> LongArrayType as NavType<Any>
                value is Float -> FloatType as NavType<Any>
                value is FloatArray -> FloatArrayType as NavType<Any>
                value is Boolean -> BoolType as NavType<Any>
                value is BooleanArray -> BoolArrayType as NavType<Any>
                value is String || value == null -> StringType as NavType<Any>
                value is Array<*> && value.isArrayOf<String>() -> StringArrayType as NavType<Any>
                value.javaClass.isArray &&
                    Parcelable::class.java.isAssignableFrom(value.javaClass.componentType!!) -> {
                    ParcelableArrayType(value.javaClass.componentType as Class<Parcelable>)
                        as NavType<Any>
                }
                value.javaClass.isArray &&
                    Serializable::class.java.isAssignableFrom(value.javaClass.componentType!!) -> {
                    SerializableArrayType(value.javaClass.componentType as Class<Serializable>)
                        as NavType<Any>
                }
                value is Parcelable -> ParcelableType(value.javaClass) as NavType<Any>
                value is Enum<*> -> EnumType(value.javaClass) as NavType<Any>
                value is Serializable -> SerializableType(value.javaClass) as NavType<Any>
                else -> {
                    throw IllegalArgumentException(
                        "Object of type ${value.javaClass.name} is not supported for navigation " +
                            "arguments."
                    )
                }
            }
        }

        /**
         * NavType for storing integer values, corresponding with the "integer" type in a Navigation
         * XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val IntType: NavType<Int> =
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

        /**
         * NavType for storing integer values representing resource ids, corresponding with the
         * "reference" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val ReferenceType: NavType<Int> =
            object : NavType<Int>(false) {
                override val name: String
                    get() = "reference"

                override fun put(bundle: Bundle, key: String, @AnyRes value: Int) {
                    bundle.putInt(key, value)
                }

                @AnyRes
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

        /**
         * NavType for storing integer arrays, corresponding with the "integer[]" type in a
         * Navigation XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val IntArrayType: NavType<IntArray?> =
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

        /**
         * NavType for storing list of Ints.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        @JvmField
        public val IntListType: NavType<List<Int>?> =
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

        /**
         * NavType for storing long values, corresponding with the "long" type in a Navigation XML
         * file.
         *
         * Null values are not supported. Default values for this type in Navigation XML files must
         * always end with an 'L' suffix, e.g. `app:defaultValue="123L"`.
         */
        @JvmField
        public val LongType: NavType<Long> =
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

        /**
         * NavType for storing long arrays, corresponding with the "long[]" type in a Navigation XML
         * file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val LongArrayType: NavType<LongArray?> =
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

        /**
         * NavType for storing list of Longs.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        @JvmField
        public val LongListType: NavType<List<Long>?> =
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

        /**
         * NavType for storing float values, corresponding with the "float" type in a Navigation XML
         * file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val FloatType: NavType<Float> =
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

        /**
         * NavType for storing float arrays, corresponding with the "float[]" type in a Navigation
         * XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val FloatArrayType: NavType<FloatArray?> =
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

        /**
         * NavType for storing list of Floats.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        @JvmField
        public val FloatListType: NavType<List<Float>?> =
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

        /**
         * NavType for storing boolean values, corresponding with the "boolean" type in a Navigation
         * XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val BoolType: NavType<Boolean> =
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

        /**
         * NavType for storing boolean arrays, corresponding with the "boolean[]" type in a
         * Navigation XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val BoolArrayType: NavType<BooleanArray?> =
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

        /**
         * NavType for storing list of Booleans.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        @JvmField
        public val BoolListType: NavType<List<Boolean>?> =
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

        /**
         * NavType for storing String values, corresponding with the "string" type in a Navigation
         * XML file.
         *
         * Null values are supported.
         */
        @JvmField
        public val StringType: NavType<String?> =
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

        /**
         * NavType for storing String arrays, corresponding with the "string[]" type in a Navigation
         * XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val StringArrayType: NavType<Array<String>?> =
            object : CollectionNavType<Array<String>?>(true) {
                override val name: String
                    get() = "string[]"

                override fun put(bundle: Bundle, key: String, value: Array<String>?) {
                    bundle.putStringArray(key, value)
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

        /**
         * NavType for storing list of Strings.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        @JvmField
        public val StringListType: NavType<List<String>?> =
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

    /**
     * ParcelableType is used for passing Parcelables in [NavArgument]s.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the Parcelable class that is supported by this NavType
     */
    public class ParcelableType<D>(type: Class<D>) : NavType<D>(true) {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        public override fun put(bundle: Bundle, key: String, value: D) {
            type.cast(value)
            if (value == null || value is Parcelable) {
                bundle.putParcelable(key, value as Parcelable?)
            } else if (value is Serializable) {
                bundle.putSerializable(key, value as Serializable?)
            }
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: Bundle, key: String): D? {
            return bundle[key] as D?
        }

        /** @throws UnsupportedOperationException since Parcelables do not support default values */
        public override fun parseValue(value: String): D {
            throw UnsupportedOperationException("Parcelables don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ParcelableType<*>
            return type == that.type
        }

        public override fun hashCode(): Int {
            return type.hashCode()
        }

        /** Constructs a NavType that supports a given Parcelable type. */
        init {
            require(
                Parcelable::class.java.isAssignableFrom(type) ||
                    Serializable::class.java.isAssignableFrom(type)
            ) {
                "$type does not implement Parcelable or Serializable."
            }
            this.type = type
        }
    }

    /**
     * ParcelableArrayType is used for [NavArgument]s which hold arrays of Parcelables.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the type of Parcelable component class of the array
     */
    public class ParcelableArrayType<D : Parcelable>(type: Class<D>) : NavType<Array<D>?>(true) {
        private val arrayType: Class<Array<D>>

        public override val name: String
            get() = arrayType.name

        public override fun put(bundle: Bundle, key: String, value: Array<D>?) {
            arrayType.cast(value)
            bundle.putParcelableArray(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: Bundle, key: String): Array<D>? {
            return bundle[key] as Array<D>?
        }

        /** @throws UnsupportedOperationException since Arrays do not support default values */
        public override fun parseValue(value: String): Array<D> {
            throw UnsupportedOperationException("Arrays don't support default values.")
        }

        @Suppress("UNCHECKED_CAST")
        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ParcelableArrayType<Parcelable>
            return arrayType == that.arrayType
        }

        public override fun hashCode(): Int {
            return arrayType.hashCode()
        }

        override fun valueEquals(
            @Suppress("ArrayReturn") value: Array<D>?,
            @Suppress("ArrayReturn") other: Array<D>?
        ) = value.contentDeepEquals(other)

        /** Constructs a NavType that supports arrays of a given Parcelable type. */
        init {
            require(Parcelable::class.java.isAssignableFrom(type)) {
                "$type does not implement Parcelable."
            }
            val arrayType: Class<Array<D>> =
                try {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName("[L${type.name};") as Class<Array<D>>
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e) // should never happen
                }
            this.arrayType = arrayType
        }
    }

    /**
     * SerializableType is used for Serializable [NavArgument]s. For handling Enums you must use
     * [EnumType] instead.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @see EnumType
     */
    public open class SerializableType<D : Serializable> : NavType<D> {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        /**
         * Constructs a NavType that supports a given Serializable type.
         *
         * @param type class that is a subtype of Serializable
         */
        public constructor(type: Class<D>) : super(true) {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            require(!type.isEnum) { "$type is an Enum. You should use EnumType instead." }
            this.type = type
        }

        internal constructor(nullableAllowed: Boolean, type: Class<D>) : super(nullableAllowed) {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            this.type = type
        }

        public override fun put(bundle: Bundle, key: String, value: D) {
            type.cast(value)
            bundle.putSerializable(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: Bundle, key: String): D? {
            return bundle[key] as D?
        }

        /**
         * @throws UnsupportedOperationException since Serializables do not support default values
         */
        public override fun parseValue(value: String): D {
            throw UnsupportedOperationException("Serializables don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerializableType<*>) return false
            return type == other.type
        }

        public override fun hashCode(): Int {
            return type.hashCode()
        }
    }

    /**
     * EnumType is used for [NavArgument]s holding enum values.
     *
     * Null values are not supported. To specify a default value in a Navigation XML file, simply
     * use the enum constant without the class name, e.g. `app:defaultValue="MONDAY"`.
     *
     * @param type the Enum class that is supported by this NavType
     */
    public class EnumType<D : Enum<*>>(type: Class<D>) : SerializableType<D>(false, type) {
        private val type: Class<D>

        public override val name: String
            get() = type.name

        /**
         * Parse a value of this type from a String.
         *
         * @param value string representation of a value of this type
         * @return parsed value of the type represented by this NavType
         * @throws IllegalArgumentException if value cannot be parsed into this type
         */
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        public override fun parseValue(value: String): D {
            return type.enumConstants.firstOrNull { constant ->
                constant.name.equals(value, ignoreCase = true)
            }
                ?: throw IllegalArgumentException(
                    "Enum value $value not found for type ${type.name}."
                )
        }

        /** Constructs a NavType that supports a given Enum type. */
        init {
            require(type.isEnum) { "$type is not an Enum type." }
            this.type = type
        }
    }

    /**
     * SerializableArrayType is used for [NavArgument]s that hold arrays of Serializables. This type
     * also supports arrays of Enums.
     *
     * Null values are supported. Default values in Navigation XML files are not supported.
     *
     * @param type the Serializable component class of the array
     */
    public class SerializableArrayType<D : Serializable>(type: Class<D>) :
        NavType<Array<D>?>(true) {
        private val arrayType: Class<Array<D>>

        public override val name: String
            get() = arrayType.name

        public override fun put(bundle: Bundle, key: String, value: Array<D>?) {
            arrayType.cast(value)
            bundle.putSerializable(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        public override fun get(bundle: Bundle, key: String): Array<D>? {
            return bundle[key] as Array<D>?
        }

        /** @throws UnsupportedOperationException since Arrays do not support default values */
        public override fun parseValue(value: String): Array<D> {
            throw UnsupportedOperationException("Arrays don't support default values.")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as SerializableArrayType<*>
            return arrayType == that.arrayType
        }

        public override fun hashCode(): Int {
            return arrayType.hashCode()
        }

        override fun valueEquals(
            @Suppress("ArrayReturn") value: Array<D>?,
            @Suppress("ArrayReturn") other: Array<D>?
        ) = value.contentDeepEquals(other)

        /** Constructs a NavType that supports arrays of a given Serializable type. */
        init {
            require(Serializable::class.java.isAssignableFrom(type)) {
                "$type does not implement Serializable."
            }
            val arrayType: Class<Array<D>> =
                try {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName("[L${type.name};") as Class<Array<D>>
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e) // should never happen
                }
            this.arrayType = arrayType
        }
    }
}
