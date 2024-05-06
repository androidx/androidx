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

import androidx.annotation.RestrictTo
import androidx.core.bundle.Bundle
import kotlin.jvm.JvmField
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
 */
public expect abstract class NavType<T>(
    isNullableAllowed: Boolean
) {
    /**
     * Check if an argument with this type can hold a null value.
     * @return Returns true if this type allows null values, false otherwise.
     */
    public open val isNullableAllowed: Boolean

    /**
     * Put a value of this type in the `bundle`
     *
     * @param bundle bundle to put value in
     * @param key    bundle key
     * @param value  value of this type
     */
    public abstract fun put(bundle: Bundle, key: String, value: T)

    /**
     * Get a value of this type from the `bundle`
     *
     * @param bundle bundle to get value from
     * @param key    bundle key
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
     * Parse a value of this type from a String and then combine that
     * parsed value with the given previousValue of the same type to
     * provide a new value that contains both the new and previous value.
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
     * Parse a value of this type from a String and put it in a `bundle`
     *
     * @param bundle bundle to put value in
     * @param key    bundle key under which to put the value
     * @param value  string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: Bundle, key: String, value: String): T

    /**
     * Parse a value of this type from a String, combine that parsed value
     * with the given previousValue, and then put that combined parsed
     * value in a `bundle`.
     *
     * @param bundle bundle to put value in
     * @param key    bundle key under which to put the value
     * @param value  string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun parseAndPut(bundle: Bundle, key: String, value: String?, previousValue: T): T

    /**
     * Serialize a value of this NavType into a String.
     *
     * By default it returns value of [kotlin.toString] or null if value passed in is null.
     *
     * This method can be override for custom serialization implementation on types such
     * custom NavType classes.
     *
     * @param value a value representing this NavType to be serialized into a String
     * @return serialized String value of [value]
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
         * @param type        argType string, usually parsed from the Navigation XML file
         * @param packageName package name of the R file,
         * used for parsing relative class names starting with a dot.
         * @return a NavType representing the type indicated by the argType string.
         * Defaults to StringType for null.
         * @throws IllegalArgumentException if there is no valid argType
         * @throws RuntimeException if the type class name cannot be found
         */
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT") // this needs to be open to
        // maintain api compatibility and type cast are unchecked
        @JvmStatic
        public open fun fromArgType(type: String?, packageName: String?): NavType<*>

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValue(value: String): NavType<Any>

        /**
         * @param value nothing
         * @throws IllegalArgumentException not real
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun inferFromValueType(value: Any?): NavType<Any>

        /**
         * NavType for storing integer values,
         * corresponding with the "integer" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val IntType: NavType<Int>

        /**
         * NavType for storing integer arrays,
         * corresponding with the "integer[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val IntArrayType: NavType<IntArray?>

        /**
         * NavType for storing long values,
         * corresponding with the "long" type in a Navigation XML file.
         *
         * Null values are not supported.
         * Default values for this type in Navigation XML files must always end with an 'L' suffix, e.g.
         * `app:defaultValue="123L"`.
         */
        @JvmField
        public val LongType: NavType<Long>

        /**
         * NavType for storing long arrays,
         * corresponding with the "long[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val LongArrayType: NavType<LongArray?>

        /**
         * NavType for storing float values,
         * corresponding with the "float" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val FloatType: NavType<Float>

        /**
         * NavType for storing float arrays,
         * corresponding with the "float[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val FloatArrayType: NavType<FloatArray?>

        /**
         * NavType for storing boolean values,
         * corresponding with the "boolean" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        @JvmField
        public val BoolType: NavType<Boolean>

        /**
         * NavType for storing boolean arrays,
         * corresponding with the "boolean[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val BoolArrayType: NavType<BooleanArray?>

        /**
         * NavType for storing String values,
         * corresponding with the "string" type in a Navigation XML file.
         *
         * Null values are supported.
         */
        @JvmField
        public val StringType: NavType<String?>

        /**
         * NavType for storing String arrays,
         * corresponding with the "string[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        @JvmField
        public val StringArrayType: NavType<Array<String>?>
    }
}
