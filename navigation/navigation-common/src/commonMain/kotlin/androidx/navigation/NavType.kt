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

import androidx.annotation.RestrictTo
import androidx.navigation.NavType.Companion.BoolArrayType
import androidx.navigation.NavType.Companion.BoolListType
import androidx.navigation.NavType.Companion.BoolType
import androidx.navigation.NavType.Companion.FloatArrayType
import androidx.navigation.NavType.Companion.FloatListType
import androidx.navigation.NavType.Companion.FloatType
import androidx.navigation.NavType.Companion.IntArrayType
import androidx.navigation.NavType.Companion.IntListType
import androidx.navigation.NavType.Companion.IntType
import androidx.navigation.NavType.Companion.LongArrayType
import androidx.navigation.NavType.Companion.LongListType
import androidx.navigation.NavType.Companion.LongType
import androidx.navigation.NavType.Companion.StringArrayType
import androidx.navigation.NavType.Companion.StringListType
import androidx.navigation.NavType.Companion.StringType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlin.jvm.JvmStatic

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
public expect abstract class NavType<T>(isNullableAllowed: Boolean) {
    /**
     * Check if an argument with this type can hold a null value.
     *
     * @return Returns true if this type allows null values, false otherwise.
     */
    public open val isNullableAllowed: Boolean

    /**
     * Put a value of this type in the `savedState`
     *
     * @param bundle savedState to put value in
     * @param key savedState key
     * @param value value of this type
     */
    public abstract fun put(bundle: SavedState, key: String, value: T)

    /**
     * Get a value of this type from the `savedState`
     *
     * @param bundle savedState to get value from
     * @param key savedState key
     * @return value of this type
     */
    public abstract operator fun get(bundle: SavedState, key: String): T?

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
    public open fun parseValue(value: String, previousValue: T): T

    /**
     * Parse a value of this type from a String and put it in a `savedState`
     *
     * @param bundle savedState to put value in
     * @param key savedState key under which to put the value
     * @param value string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: SavedState, key: String, value: String): T

    /**
     * Parse a value of this type from a String, combine that parsed value with the given
     * previousValue, and then put that combined parsed value in a `savedState`.
     *
     * @param bundle savedState to put value in
     * @param key savedState key under which to put the value
     * @param value string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: SavedState, key: String, value: String?, previousValue: T): T

    /**
     * Serialize a value of this NavType into a String.
     *
     * By default it returns value of [kotlin.toString] or null if value passed in is null.
     *
     * This method can be override for custom serialization implementation on types such custom
     * NavType classes.
     *
     * Note: Final output should be encoded with [NavUriUtils.encode]
     *
     * @param value a value representing this NavType to be serialized into a String
     * @return encoded and serialized String value of [value]
     */
    public open fun serializeAsValue(value: T): String

    /**
     * The name of this type.
     *
     * This is the same value that is used in Navigation XML `argType` attribute.
     *
     * @return name of this type
     */
    public open val name: String

    /**
     * Compares two values of type [T] and returns true if values are equal.
     *
     * @param value the first value for comparison
     * @param other the second value for comparison
     */
    public open fun valueEquals(value: T, other: T): Boolean

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
        public open fun fromArgType(type: String?, packageName: String?): NavType<*>

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValue(value: String): NavType<Any>

        /**
         * @param value nothing
         * @throws IllegalArgumentException not real
         */
        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValueType(value: Any?): NavType<Any>

        /**
         * NavType for storing integer values, corresponding with the "integer" type in a Navigation
         * XML file.
         *
         * Null values are not supported.
         */
        public val IntType: NavType<Int>

        /**
         * NavType for storing integer arrays, corresponding with the "integer[]" type in a
         * Navigation XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        public val IntArrayType: NavType<IntArray?>

        /**
         * NavType for storing list of Ints.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        public val IntListType: NavType<List<Int>?>

        /**
         * NavType for storing long values, corresponding with the "long" type in a Navigation XML
         * file.
         *
         * Null values are not supported. Default values for this type in Navigation XML files must
         * always end with an 'L' suffix, e.g. `app:defaultValue="123L"`.
         */
        public val LongType: NavType<Long>

        /**
         * NavType for storing long arrays, corresponding with the "long[]" type in a Navigation XML
         * file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        public val LongArrayType: NavType<LongArray?>

        /**
         * NavType for storing list of Longs.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        public val LongListType: NavType<List<Long>?>

        /**
         * NavType for storing float values, corresponding with the "float" type in a Navigation XML
         * file.
         *
         * Null values are not supported.
         */
        public val FloatType: NavType<Float>

        /**
         * NavType for storing float arrays, corresponding with the "float[]" type in a Navigation
         * XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        public val FloatArrayType: NavType<FloatArray?>

        /**
         * NavType for storing list of Floats.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        public val FloatListType: NavType<List<Float>?>

        /**
         * NavType for storing boolean values, corresponding with the "boolean" type in a Navigation
         * XML file.
         *
         * Null values are not supported.
         */
        public val BoolType: NavType<Boolean>

        /**
         * NavType for storing boolean arrays, corresponding with the "boolean[]" type in a
         * Navigation XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        public val BoolArrayType: NavType<BooleanArray?>

        /**
         * NavType for storing list of Booleans.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        public val BoolListType: NavType<List<Boolean>?>

        /**
         * NavType for storing String values, corresponding with the "string" type in a Navigation
         * XML file.
         *
         * Null values are supported.
         */
        public val StringType: NavType<String?>

        /**
         * NavType for storing String arrays, corresponding with the "string[]" type in a Navigation
         * XML file.
         *
         * Null values are supported. Default values in Navigation XML files are not supported.
         */
        public val StringArrayType: NavType<Array<String>?>

        /**
         * NavType for storing list of Strings.
         *
         * Null values are supported. List NavTypes in Navigation XML files are not supported.
         */
        public val StringListType: NavType<List<String>?>
    }
}

internal fun <T> NavType<T>.navTypeParseAndPut(bundle: SavedState, key: String, value: String): T {
    val parsedValue = parseValue(value)
    put(bundle, key, parsedValue)
    return parsedValue
}

internal fun <T> NavType<T>.navTypeParseAndPut(
    bundle: SavedState,
    key: String,
    value: String?,
    previousValue: T,
): T {
    if (!bundle.read { contains(key) }) {
        throw IllegalArgumentException("There is no previous value in this savedState.")
    }
    if (value != null) {
        val parsedCombinedValue = parseValue(value, previousValue)
        put(bundle, key, parsedCombinedValue)
        return parsedCombinedValue
    }
    return previousValue
}

internal fun navTypeFromArgType(type: String?): NavType<*>? {
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
    }
    return null
}

@Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
internal fun navTypeInferFromValue(value: String): NavType<Any> {
    // because we allow Long literals without the L suffix at runtime,
    // the order of IntType and LongType parsing has to be reversed compared to Safe Args
    try {
        IntType.parseValue(value)
        return IntType as NavType<Any>
    } catch (_: IllegalArgumentException) {
        // ignored, proceed to check next type
    }
    try {
        LongType.parseValue(value)
        return LongType as NavType<Any>
    } catch (_: IllegalArgumentException) {
        // ignored, proceed to check next type
    }
    try {
        FloatType.parseValue(value)
        return FloatType as NavType<Any>
    } catch (_: IllegalArgumentException) {
        // ignored, proceed to check next type
    }
    try {
        BoolType.parseValue(value)
        return BoolType as NavType<Any>
    } catch (_: IllegalArgumentException) {
        // ignored, proceed to check next type
    }
    return StringType as NavType<Any>
}

@Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
internal fun navTypeInferFromValueType(value: Any?): NavType<Any>? {
    return when (value) {
        is Int -> IntType as NavType<Any>
        is IntArray -> IntArrayType as NavType<Any>
        is Long -> LongType as NavType<Any>
        is LongArray -> LongArrayType as NavType<Any>
        is Float -> FloatType as NavType<Any>
        is FloatArray -> FloatArrayType as NavType<Any>
        is Boolean -> BoolType as NavType<Any>
        is BooleanArray -> BoolArrayType as NavType<Any>
        is String,
        null -> StringType as NavType<Any>
        else -> null
    }
}

internal class IntNavType : NavType<Int>(false) {
    override val name: String
        get() = "integer"

    override fun put(bundle: SavedState, key: String, value: Int) {
        bundle.write { putInt(key, value) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): Int {
        return bundle.read { getInt(key) }
    }

    override fun parseValue(value: String): Int {
        return if (value.startsWith("0x")) {
            value.substring(2).toInt(16)
        } else {
            value.toInt()
        }
    }
}

internal class IntArrayNavType : CollectionNavType<IntArray?>(true) {
    override val name: String
        get() = "integer[]"

    override fun put(bundle: SavedState, key: String, value: IntArray?) {
        bundle.write { if (value != null) putIntArray(key, value) else putNull(key) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): IntArray? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getIntArray(key) }
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

internal class IntListNavType : CollectionNavType<List<Int>?>(true) {
    override val name: String
        get() = "List<Int>"

    override fun put(bundle: SavedState, key: String, value: List<Int>?) {
        if (value != null) {
            bundle.write { putIntArray(key, value.toIntArray()) }
        }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): List<Int>? {
        return bundle.read {
            if (!contains(key) || isNull(key)) null else getIntArray(key).toList()
        }
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

internal class LongNavType : NavType<Long>(false) {
    override val name: String
        get() = "long"

    override fun put(bundle: SavedState, key: String, value: Long) {
        bundle.write { putLong(key, value) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): Long {
        return bundle.read { getLong(key) }
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

internal class LongArrayNavType : CollectionNavType<LongArray?>(true) {
    override val name: String
        get() = "long[]"

    override fun put(bundle: SavedState, key: String, value: LongArray?) {
        bundle.write { if (value != null) putLongArray(key, value) else putNull(key) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): LongArray? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getLongArray(key) }
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

internal class LongListNavType : CollectionNavType<List<Long>?>(true) {
    override val name: String
        get() = "List<Long>"

    override fun put(bundle: SavedState, key: String, value: List<Long>?) {
        bundle.write { if (value != null) putLongArray(key, value.toLongArray()) else putNull(key) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): List<Long>? {
        return bundle.read {
            if (!contains(key) || isNull(key)) null else getLongArray(key).toList()
        }
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

internal class FloatNavType : NavType<Float>(false) {
    override val name: String
        get() = "float"

    override fun put(bundle: SavedState, key: String, value: Float) {
        bundle.write { putFloat(key, value) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): Float {
        return bundle.read { getFloat(key) }
    }

    override fun parseValue(value: String): Float {
        return value.toFloat()
    }
}

internal class FloatArrayNavType : CollectionNavType<FloatArray?>(true) {
    override val name: String
        get() = "float[]"

    override fun put(bundle: SavedState, key: String, value: FloatArray?) {
        bundle.write { if (value != null) putFloatArray(key, value) else putNull(key) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): FloatArray? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getFloatArray(key) }
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

internal class FloatListNavType : CollectionNavType<List<Float>?>(true) {
    override val name: String
        get() = "List<Float>"

    override fun put(bundle: SavedState, key: String, value: List<Float>?) {
        bundle.write {
            if (value != null) putFloatArray(key, value.toFloatArray()) else putNull(key)
        }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): List<Float>? {
        return bundle.read {
            if (!contains(key) || isNull(key)) null else getFloatArray(key).toList()
        }
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

internal class BoolNavType : NavType<Boolean>(false) {
    override val name: String
        get() = "boolean"

    override fun put(bundle: SavedState, key: String, value: Boolean) {
        bundle.write { putBoolean(key, value) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): Boolean? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getBoolean(key) }
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

internal class BoolArrayNavType : CollectionNavType<BooleanArray?>(true) {
    override val name: String
        get() = "boolean[]"

    override fun put(bundle: SavedState, key: String, value: BooleanArray?) {
        bundle.write { if (value != null) putBooleanArray(key, value) else putNull(key) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): BooleanArray? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getBooleanArray(key) }
    }

    override fun parseValue(value: String): BooleanArray {
        return booleanArrayOf(BoolType.parseValue(value))
    }

    override fun parseValue(value: String, previousValue: BooleanArray?): BooleanArray? {
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

internal class BoolListNavType : CollectionNavType<List<Boolean>?>(true) {
    override val name: String
        get() = "List<Boolean>"

    override fun put(bundle: SavedState, key: String, value: List<Boolean>?) {
        bundle.write {
            if (value != null) putBooleanArray(key, value.toBooleanArray()) else putNull(key)
        }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): List<Boolean>? {
        return bundle.read {
            if (!contains(key) || isNull(key)) null else getBooleanArray(key).toList()
        }
    }

    override fun parseValue(value: String): List<Boolean> {
        return listOf(BoolType.parseValue(value))
    }

    override fun parseValue(value: String, previousValue: List<Boolean>?): List<Boolean>? {
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

internal class StringNavType : NavType<String?>(true) {
    override val name: String
        get() = "string"

    override fun put(bundle: SavedState, key: String, value: String?) {
        bundle.write {
            if (value != null) {
                putString(key, value)
            } else putNull(key)
        }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: SavedState, key: String): String? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getString(key) }
    }

    /**
     * Returns input value by default.
     *
     * If input value is "null", returns null as the reversion of Kotlin standard library
     * serializing null receivers of [kotlin.toString] into "null".
     */
    override fun parseValue(value: String): String? {
        return if (value == "null") null else value
    }

    /**
     * Returns default value of Uri.encode(value).
     *
     * If input value is null, returns "null" in compliance with Kotlin standard library parsing
     * null receivers of [kotlin.toString] into "null".
     */
    override fun serializeAsValue(value: String?): String {
        return value?.let { NavUriUtils.encode(value) } ?: "null"
    }
}

internal class StringArrayNavType : CollectionNavType<Array<String>?>(true) {
    override val name: String
        get() = "string[]"

    override fun put(bundle: SavedState, key: String, value: Array<String>?) {
        bundle.write { if (value != null) putStringArray(key, value) else putNull(key) }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun get(bundle: SavedState, key: String): Array<String>? {
        return bundle.read { if (!contains(key) || isNull(key)) null else getStringArray(key) }
    }

    override fun parseValue(value: String): Array<String> {
        return arrayOf(value)
    }

    override fun parseValue(value: String, previousValue: Array<String>?): Array<String>? {
        return previousValue?.plus(parseValue(value)) ?: parseValue(value)
    }

    override fun valueEquals(value: Array<String>?, other: Array<String>?) =
        value.contentDeepEquals(other)

    // "null" is still serialized as "null"
    override fun serializeAsValues(value: Array<String>?): List<String> =
        value?.map { NavUriUtils.encode(it) } ?: emptyList()

    override fun emptyCollection(): Array<String> = arrayOf()
}

internal class StringListNavType : CollectionNavType<List<String>?>(true) {
    override val name: String
        get() = "List<String>"

    override fun put(bundle: SavedState, key: String, value: List<String>?) {
        bundle.write {
            if (value != null) putStringArray(key, value.toTypedArray()) else putNull(key)
        }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun get(bundle: SavedState, key: String): List<String>? {
        return bundle.read {
            if (!contains(key) || isNull(key)) null else getStringArray(key).toList()
        }
    }

    override fun parseValue(value: String): List<String> {
        return listOf(value)
    }

    override fun parseValue(value: String, previousValue: List<String>?): List<String>? {
        return previousValue?.plus(parseValue(value)) ?: parseValue(value)
    }

    override fun valueEquals(value: List<String>?, other: List<String>?): Boolean {
        val valueArray = value?.toTypedArray()
        val otherArray = other?.toTypedArray()
        return valueArray.contentDeepEquals(otherArray)
    }

    override fun serializeAsValues(value: List<String>?): List<String> =
        value?.map { NavUriUtils.encode(it) } ?: emptyList()

    override fun emptyCollection(): List<String> = emptyList()
}

/**
 * Enables other navigation modules to internally access [NavUriUtils], which is package internal
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> NavType<T>.parseAndPutFromUri(bundle: SavedState, key: String, value: String): T {
    val decoded = NavUriUtils.decode(value)
    return parseAndPut(bundle, key, decoded)
}
