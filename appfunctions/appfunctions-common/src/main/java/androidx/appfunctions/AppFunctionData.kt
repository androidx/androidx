/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.app.appsearch.GenericDocument
import android.os.Bundle
import androidx.annotation.RequiresApi
import kotlin.collections.isEmpty

/**
 * A data class to contain information to be communicated between AppFunctions apps and agents.
 *
 * This class can be logically viewed as a mapping from [String] keys to properties of various
 * supported types, or arrays of supported types. For each supported type, two separate getters are
 * provided: one without a defaultValue, typically useful for mandatory parameter, and one with a
 * defaultValue, geared towards optional parameters. To check whether a key with a property of
 * expected type exists, the caller must call the appropriate version of the getter and handle
 * corresponding exceptions, see documentation for individual methods for details.
 *
 * **Examples:**
 *
 * ```
 * // Handle optional boolean parameter with default value of false.
 * val value = try {
 *     data.getBoolean("keyForOptional", false)
 * } catch (e: IllegalArgumentException) {
 *     // data is malformed: the value under "keyForOptional" is not of type Boolean.
 * }
 *
 * // Handle a mandatory boolean parameter
 * val value = try {
 *     data.getBoolean("keyForMandatory")
 *  } catch (e: IllegalArgumentException | NoSuchElementException) {
 *      // either there is no property under "keyForMandatory", or it is not of type Boolean.
 *  }
 * ```
 */
@RequiresApi(35)
public class AppFunctionData
internal constructor(internal val genericDocument: GenericDocument, internal val extras: Bundle) {

    /** Qualified name of the underlying object */
    public val qualifiedName: String
        get() = genericDocument.schemaType

    /**
     * Retrieve the boolean property in [key].
     *
     * @return [Boolean] property if the value exists under [key].
     * @throws NoSuchElementException if the property under [key] does not exist.
     * @throws IllegalArgumentException if the property under [key] is not a boolean.
     */
    public fun getBoolean(key: String): Boolean {
        val array = getBooleanArray(key)
        if (array == null || array.isEmpty()) {
            throw NoSuchElementException("No elements found under [$key]")
        } else if (array.size > 1) { // TODO(b/390453916): properly handle single object vs. array
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the boolean property in [key] if available, or [defaultValue] otherwise.
     *
     * @return [Boolean] property if a single value exists under [key], otherwise return
     *   [defaultValue].
     * @throws IllegalArgumentException if the property under [key] is not a boolean.
     */
    public fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val array = getBooleanArray(key)
        if (array == null || array.isEmpty()) {
            return defaultValue
        } else if (array.size > 1) {
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the double property in [key].
     *
     * @return [Double] property if the value exists under [key].
     * @throws NoSuchElementException if the property under [key] does not exist.
     * @throws IllegalArgumentException if the property under [key] is not a double.
     */
    public fun getDouble(key: String): Double {
        val array = getDoubleArray(key)
        if (array == null || array.isEmpty()) {
            throw NoSuchElementException("No elements found under [$key]")
        } else if (array.size > 1) { // TODO(b/390453916): properly handle single object vs. array
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the double property in [key] if available, or [defaultValue] otherwise.
     *
     * @return [Double] property if a single value exists under [key], otherwise return
     *   [defaultValue].
     * @throws IllegalArgumentException if the property under [key] is not a double.
     */
    public fun getDouble(key: String, defaultValue: Double): Double {
        val array = getDoubleArray(key)
        if (array == null || array.isEmpty()) {
            return defaultValue
        } else if (array.size > 1) {
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the long property in [key].
     *
     * @return [Long] property if the value exist under [key].
     * @throws NoSuchElementException if the property under [key] does not exist.
     * @throws IllegalArgumentException if the property under [key] is not a long.
     */
    public fun getLong(key: String): Long {
        val array = getLongArray(key)
        if (array == null || array.isEmpty()) {
            throw NoSuchElementException("No elements found under [$key]")
        } else if (array.size > 1) { // TODO(b/390453916): properly handle single object vs. array
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the long property in [key] if available, or [defaultValue] otherwise.
     *
     * @return [Long] property if the value existS under [key], otherwise return [defaultValue].
     * @throws IllegalArgumentException if the property under [key] is not a long.
     */
    public fun getLong(key: String, defaultValue: Long): Long {
        val array = getLongArray(key)
        if (array == null || array.isEmpty()) {
            return defaultValue
        } else if (array.size > 1) {
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the string property in [key].
     *
     * @return [String] property if the value exist under [key].
     * @throws NoSuchElementException if the property under [key] does not exist.
     * @throws IllegalArgumentException if the property under [key] is not a string.
     */
    public fun getString(key: String): String {
        val array = getStringList(key)
        if (array == null || array.isEmpty()) {
            throw NoSuchElementException("No elements found under [$key]")
        } else if (array.size > 1) { // TODO(b/390453916): properly handle single object vs. array
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the string property in [key] if available, or [defaultValue] otherwise.
     *
     * @return [String] property if the value existS under [key], otherwise return [defaultValue].
     * @throws IllegalArgumentException if the property under [key] is not a string.
     */
    public fun getString(key: String, defaultValue: String): String {
        val array = getStringList(key)
        if (array == null || array.isEmpty()) {
            return defaultValue
        } else if (array.size > 1) {
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the appfunction data property in [key].
     *
     * @return [AppFunctionData] property if the value exists under [key].
     * @throws NoSuchElementException if the property under [key] does not exist.
     * @throws IllegalArgumentException if the property under [key] is not an [AppFunctionData].
     */
    public fun getAppFunctionData(key: String): AppFunctionData {
        val array = getAppFunctionDataList(key)
        if (array == null || array.isEmpty()) {
            throw NoSuchElementException("No elements found under [$key]")
        } else if (array.size > 1) { // TODO(b/390453916): properly handle single object vs. array
            throw IllegalArgumentException("Property under [$key] does not match request")
        }
        return array[0]
    }

    /**
     * Retrieve the boolean array property in [key].
     *
     * @return [BooleanArray] property if the value exist under [key].
     * @throws IllegalArgumentException if the property under [key] is not boolean array.
     */
    public fun getBooleanArray(key: String): BooleanArray? {
        return unsafeGetProperty(key, BooleanArray::class.java)
    }

    /**
     * Retrieve the double array property in [key].
     *
     * @return [DoubleArray] property if the value exist under [key].
     * @throws IllegalArgumentException if the property under [key] is not double array.
     */
    public fun getDoubleArray(key: String): DoubleArray? {
        return unsafeGetProperty(key, DoubleArray::class.java)
    }

    /**
     * Retrieve the long array property in [key].
     *
     * @return [LongArray] property if the value exist under [key].
     * @throws IllegalArgumentException if the property under [key] is not long array.
     */
    public fun getLongArray(key: String): LongArray? {
        return unsafeGetProperty(key, LongArray::class.java)
    }

    /**
     * Retrieve the string list property in [key].
     *
     * @return [List<String>] property if the value exist under [key].
     * @throws IllegalArgumentException if the property under [key] is not string array.
     */
    @Suppress("NullableCollection")
    public fun getStringList(key: String): List<String>? {
        return unsafeGetProperty(key, Array<String>::class.java)?.asList()
    }

    /**
     * Retrieve the appfunction data list property in [key].
     *
     * @return [List<AppFunctionData>] property if the value exist under [key].
     * @throws IllegalArgumentException if the property under [key] is not [AppFunctionData] array.
     */
    @Suppress("NullableCollection")
    public fun getAppFunctionDataList(key: String): List<AppFunctionData>? {
        return unsafeGetProperty(key, Array<GenericDocument>::class.java)?.mapIndexed {
            index,
            element ->
            AppFunctionData(element, extras.getBundle(extrasKey(key, index)) ?: Bundle.EMPTY)
        }
    }

    override fun toString(): String {
        // TODO(b/391419368): Improve output to avoid reference to underlying GenericDocument
        return "AppFunctionData(genericDocument=$genericDocument, extras=$extras)"
    }

    private fun <T : Any> unsafeGetProperty(key: String, arrayClass: Class<T>): T? {
        return try {
            val value = genericDocument.getProperty(key)
            if (value != null) {
                arrayClass.cast(value)
            } else {
                null
            }
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                "Found the property under [$key] but data type does not match with the request.",
                e,
            )
        }
    }

    /**
     * Builder for constructing [AppFunctionData]
     *
     * @param qualifiedName the data object qualified name
     */
    public class Builder(qualifiedName: String) {
        private val genericDocumentBuilder =
            GenericDocument.Builder<GenericDocument.Builder<*>>("", "", qualifiedName)
        private val extrasBuilder = Bundle()

        /** Sets a boolean property [value] under [key] */
        public fun setBoolean(key: String, value: Boolean): Builder {
            genericDocumentBuilder.setPropertyBoolean(key, value)
            return this
        }

        /** Sets a double property [value] under [key] */
        public fun setDouble(key: String, value: Double): Builder {
            genericDocumentBuilder.setPropertyDouble(key, value)
            return this
        }

        /** Sets a long property [value] under [key] */
        public fun setLong(key: String, value: Long): Builder {
            genericDocumentBuilder.setPropertyLong(key, value)
            return this
        }

        /** Sets a string property [value] under [key] */
        public fun setString(key: String, value: String): Builder {
            genericDocumentBuilder.setPropertyString(key, value)
            return this
        }

        /** Sets a document property [value] under [key] */
        public fun setAppFunctionData(key: String, value: AppFunctionData): Builder {
            genericDocumentBuilder.setPropertyDocument(key, value.genericDocument)
            if (!value.extras.isEmpty()) {
                extrasBuilder.putBundle(extrasKey(key), value.extras)
            }
            return this
        }

        /** Sets boolean properties [value] under [key] */
        public fun setBooleanArray(key: String, value: BooleanArray): Builder {
            genericDocumentBuilder.setPropertyBoolean(key, *value)
            return this
        }

        /** Sets double properties [value] under [key] */
        public fun setDoubleArray(key: String, value: DoubleArray): Builder {
            genericDocumentBuilder.setPropertyDouble(key, *value)
            return this
        }

        /** Sets long properties [value] under [key] */
        public fun setLongArray(key: String, value: LongArray): Builder {
            genericDocumentBuilder.setPropertyLong(key, *value)
            return this
        }

        /** Sets string properties [value] under [key] */
        public fun setStringList(key: String, value: List<String>): Builder {
            genericDocumentBuilder.setPropertyString(key, *value.toTypedArray())
            return this
        }

        /** Sets appfunction data properties [value] under [key] */
        public fun setAppFunctionDataList(key: String, value: List<AppFunctionData>): Builder {
            genericDocumentBuilder.setPropertyDocument(
                key,
                *value.map { it.genericDocument }.toTypedArray(),
            )
            value.forEachIndexed { index, element ->
                if (!element.extras.isEmpty()) {
                    extrasBuilder.putBundle(extrasKey(key, index), element.extras)
                }
            }
            return this
        }

        /** Builds [AppFunctionData] */
        public fun build(): AppFunctionData =
            AppFunctionData(genericDocumentBuilder.build(), extrasBuilder)
    }

    public companion object {
        private fun extrasKey(key: String) = "property/$key"

        private fun extrasKey(key: String, index: Int) = "property/$key[$index]"

        /** Represents an empty [AppFunctionData]. */
        @JvmField
        public val EMPTY: AppFunctionData =
            AppFunctionData(
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build(),
                Bundle.EMPTY,
            )
    }
}
