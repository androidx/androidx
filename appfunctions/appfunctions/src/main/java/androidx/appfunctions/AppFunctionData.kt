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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.WorkerThread
import androidx.appfunctions.internal.AppFunctionSerializableFactory
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.time.LocalDateTime

/**
 * A data class to contain information to be communicated between AppFunctions apps and agents.
 *
 * This class can be logically viewed as a mapping from [String] keys to properties of various
 * supported types, or arrays of supported types.
 *
 * When trying to retrieve an associated value, [AppFunctionData] would validate the request against
 * the predefined metadata specification provided from [Builder].
 *
 * For example,
 * ```
 * fun callCreateNoteFunction(
 *   metadata: AppFunctionMetadata,
 *   request: ExecuteAppFunctionRequest,
 * ) {
 *   val response = appFunctionManager.executeAppFunction(request)
 *
 *   if (metadata.response.valueType is AppFunctionObjectTypeMetadata) {
 *     val returnData = response.returnValue.getAppFunctionData(
 *       ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
 *     )
 *     val title = returnData.getString("title")
 *     // Throws an error if "owner" doesn't exist according to the metadata
 *     val owner = returnData.getString("owner")
 *     // Throws an error if "content" is String.
 *     val content = returnData.getInt("content")
 *   }
 * }
 * ```
 *
 * @see [AppFunctionData.Builder]
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class AppFunctionData
internal constructor(
    // TODO(b/447302747): Make it non-null once the constructor that takes qualifiedName has removed
    internal val spec: AppFunctionDataSpec?,
    internal val genericDocument: GenericDocument,
    internal val extras: Bundle,
) {
    // TODO(b/447302747): Remove this constructor
    internal constructor(
        genericDocument: GenericDocument,
        extras: Bundle,
    ) : this(null, genericDocument, extras)

    init {
        // This will set the Bundle's classLoader to be the classLoader from the App using this
        // AppFunctionData class.
        extras.classLoader = AppFunctionData::class.java.classLoader
    }

    /** Qualified name of the underlying object */
    public val qualifiedName: String
        get() = genericDocument.schemaType

    /**
     * Returns the ID of the underlying [GenericDocument]. Only use this for handling legacy schema.
     */
    @get:RestrictTo(LIBRARY_GROUP) public val id: String = genericDocument.id

    /**
     * Checks if [AppFunctionData] has an associated value with the specified [key].
     *
     * @param key The key to checks for.
     * @return True if there is an associated value. Otherwise, false.
     * @throws IllegalArgumentException If there is no metadata related to [key].
     */
    public fun containsKey(key: String): Boolean {
        if (spec != null && !spec.containsMetadata(key)) {
            throw IllegalArgumentException("There is no metadata associated with $key")
        }
        return key == LEGACY_ID_FIELD_KEY ||
            genericDocument.getProperty(key) != null ||
            extras.containsKey(key) || // Check direct key in extras
            extras.containsKey(extrasKey(key)) // Check prefixed key in extras
    }

    /**
     * Retrieves a [Boolean] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. It would return a default value false if the
     *   associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getBoolean(key: String): Boolean {
        return getBoolean(key, DEFAULT_BOOLEAN)
    }

    /**
     * Retrieves a [Boolean] value associated with the specified [key], or returns [defaultValue] if
     * the associated value is not found.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue The default value if the associated value is not found.
     * @return The value associated with the [key], or the [defaultValue] if the associated value is
     *   not required and it is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getBooleanOrNull(key) ?: defaultValue
    }

    /**
     * Retrieves a [Boolean] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getBooleanOrNull(key: String): Boolean? {
        val array = unsafeGetProperty(key, BooleanArray::class.java)
        val booleanValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                array[0]
            }
        spec?.validateReadRequest(
            key,
            Boolean::class.java,
            isCollection = false,
            targetValue = booleanValue,
        )
        return booleanValue
    }

    /**
     * Retrieves a [Float] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. It would return a default value 0.0 if the
     *   associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getFloat(key: String): Float {
        return getFloat(key, DEFAULT_FLOAT)
    }

    /**
     * Retrieves a [Float] value associated with the specified [key], or returns [defaultValue] if
     * the associated value is not found.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue The default value if the associated value is not found.
     * @return The value associated with the [key], or the [defaultValue] if the associated is not
     *   found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getFloat(key: String, defaultValue: Float): Float {
        return getFloatOrNull(key) ?: defaultValue
    }

    /**
     * Retrieves a [Float] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getFloatOrNull(key: String): Float? {
        val array = unsafeGetProperty(key, DoubleArray::class.java)
        val doubleValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                array[0]
            }
        spec?.validateReadRequest(
            key,
            Float::class.java,
            isCollection = false,
            targetValue = doubleValue,
        )
        if (doubleValue != null && !isDoubleWithinFloatRange(doubleValue)) {
            // This should never happen because the setters forbid such value to exist in the
            // first place.
            throw IllegalStateException(
                "The value associated with $key is not within the range of Float"
            )
        }
        return doubleValue?.toFloat()
    }

    /**
     * Retrieves a [Double] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. It would return a default value 0.0 if the
     *   associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getDouble(key: String): Double {
        return getDouble(key, DEFAULT_DOUBLE)
    }

    /**
     * Retrieves a [Double] value associated with the specified [key], or returns [defaultValue] if
     * the associated value is not found.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue The default value if the associated value is not found.
     * @return The value associated with the [key], or the [defaultValue] if the associated value is
     *   not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getDouble(key: String, defaultValue: Double): Double {
        return getDoubleOrNull(key) ?: defaultValue
    }

    /**
     * Retrieves a [Double] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getDoubleOrNull(key: String): Double? {
        val array = unsafeGetProperty(key, DoubleArray::class.java)
        val doubleValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                array[0]
            }
        spec?.validateReadRequest(
            key,
            Double::class.java,
            isCollection = false,
            targetValue = doubleValue,
        )
        return doubleValue
    }

    /**
     * Retrieves an [Int] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. It would return a default value 0L if the
     *   associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getInt(key: String): Int {
        return getInt(key, DEFAULT_INT)
    }

    /**
     * Retrieves an [Int] value associated with the specified [key], or returns [defaultValue] if
     * the associated value is not found.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue The default value if the associated value is not found.
     * @return The value associated with the [key], or the [defaultValue] if the associated value is
     *   not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getInt(key: String, defaultValue: Int): Int {
        return getIntOrNull(key) ?: defaultValue
    }

    /**
     * Retrieves an [Int] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getIntOrNull(key: String): Int? {
        val array = unsafeGetProperty(key, LongArray::class.java)
        val longValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                array[0]
            }
        if (longValue != null && !isLongWithinLongRange(longValue)) {
            // This should never happen because the setters forbid such value to exist in the
            // first place.
            throw IllegalStateException(
                "The value associated with $key is not within the range of Int"
            )
        }
        val intValue = longValue?.toInt()
        spec?.validateReadRequest(
            key,
            Int::class.java,
            isCollection = false,
            targetValue = intValue,
        )
        return intValue
    }

    /**
     * Retrieves a [Long] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. It would return a default value 0L if the
     *   associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getLong(key: String): Long {
        return getLong(key, DEFAULT_LONG)
    }

    /**
     * Retrieves a [Long] value associated with the specified [key], or returns [defaultValue] if
     * the associated value is not found.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue The default value if the associated value is not found.
     * @return The value associated with the [key], or the [defaultValue] if the associated value is
     *   not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getLong(key: String, defaultValue: Long): Long {
        return getLongOrNull(key) ?: defaultValue
    }

    /**
     * Retrieves a [Long] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getLongOrNull(key: String): Long? {
        val array = unsafeGetProperty(key, LongArray::class.java)
        val longValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                array[0]
            }
        spec?.validateReadRequest(
            key,
            Long::class.java,
            isCollection = false,
            targetValue = longValue,
        )
        return longValue
    }

    /**
     * Retrieves a [String] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getString(key: String): String? {
        return getStringOrNull(key)
    }

    /**
     * Retrieves a [String] value associated with the specified [key], or returns null if the
     * associated value is not found.
     *
     * This method is used internally by the [AppFunctionSerializableFactory] to retrieve the
     * underlying string value.
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun getStringOrNull(key: String): String? {
        val array = unsafeGetProperty(key, Array<String>::class.java)
        val stringValue =
            when {
                // For key == "id" we first check GD's id, if that's empty we fallback to checking
                // GD's properties.
                key == LEGACY_ID_FIELD_KEY -> id.ifEmpty { array?.ifEmpty { null }?.get(0) }
                array == null || array.isEmpty() -> null
                else -> array[0]
            }

        spec?.validateReadRequest(
            key,
            String::class.java,
            isCollection = false,
            targetValue = stringValue,
        )
        return stringValue
    }

    /**
     * Retrieves an [AppFunctionData] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getAppFunctionData(key: String): AppFunctionData? {
        val array = unsafeGetProperty(key, Array<GenericDocument>::class.java)
        val dataValue =
            if (array == null || array.isEmpty()) {
                null
            } else {
                AppFunctionData(
                    spec?.getPropertyObjectSpec(key, array[0].schemaType),
                    array[0],
                    extras.getBundle(extrasKey(key)) ?: Bundle.EMPTY,
                )
            }
        spec?.validateReadRequest(
            key,
            AppFunctionData::class.java,
            isCollection = false,
            targetValue = dataValue,
        )
        return dataValue
    }

    /**
     * Retrieves a [Parcelable] value of type [T] associated with the specified [key].
     *
     * Returns null or fails with an exception if:
     * - No value exists for the given [key].
     * - The object is not of type [clazz].
     * - The [Parcelable] of type [T] cannot be loaded by the app's `classLoader`.
     *
     * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
     * between agents and apps), forward and backward compatibility is **not guaranteed** by this
     * library. Implementers are responsible for managing any compatibility and versioning concerns.
     *
     * @param key The key to retrieve the value for.
     * @param clazz The [Class] of the [Parcelable] object to retrieve, of type [T].
     * @return The value associated with the [key], or null if the associated value is not found or
     *   is not of type [T].
     */
    public fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>): T? {
        return getParcelableOrNull(key, clazz)
    }

    /**
     * Retrieves a [Parcelable] value of type [T] associated with the specified [key].
     *
     * Returns null or fails with an exception if:
     * - No value exists for the given [key].
     * - The object is not of type [T].
     * - The [Parcelable] of type [T] cannot be loaded by the app's `classLoader`.
     *
     * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
     * between agents and apps), forward and backward compatibility is **not guaranteed** by this
     * library. Implementers are responsible for managing any compatibility and versioning concerns.
     *
     * @param T The type of the [Parcelable] object to retrieve.
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key], or null if the associated value is not found or
     *   is not of type [T].
     */
    public inline fun <reified T : Parcelable> getParcelable(key: String): T? {
        return getParcelable(key, T::class.java)
    }

    /**
     * Retrieves a [Parcelable] value of type [T] associated with the specified [key].
     *
     * Returns null or fails with an exception if:
     * - No value exists for the given [key].
     * - The object is not of type [clazz].
     * - The [Parcelable] of type [T] cannot be loaded by the app's `classLoader`.
     *
     * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
     * between agents and apps), forward and backward compatibility is **not guaranteed** by this
     * library. Implementers are responsible for managing any compatibility and versioning concerns.
     *
     * @param key The key to retrieve the value for.
     * @param clazz The [Class] of the [Parcelable] object to retrieve, of type [T].
     * @return The value associated with the [key], or null if the associated value is not found.
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun <T : Parcelable> getParcelableOrNull(key: String, clazz: Class<T>): T? {
        val parcelable = extras.getParcelable(extrasKey(key), clazz)
        spec?.validateReadRequest(key, clazz, isCollection = false, targetValue = parcelable)
        return parcelable
    }

    /**
     * Retrieves a [BooleanArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getBooleanArray(key: String): BooleanArray? {
        val booleanArrayValue = unsafeGetProperty(key, BooleanArray::class.java)
        spec?.validateReadRequest(
            key,
            Boolean::class.java,
            isCollection = true,
            targetValue = booleanArrayValue,
        )
        return booleanArrayValue
    }

    /**
     * Retrieves a [FloatArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getFloatArray(key: String): FloatArray? {
        val doubleArrayValue = unsafeGetProperty(key, DoubleArray::class.java)
        val floatArrayValue =
            doubleArrayValue
                ?.map { doubleValue ->
                    if (!isDoubleWithinFloatRange(doubleValue)) {
                        // This should never happen because the setters forbid such value to exist
                        // in
                        // the first place.
                        throw IllegalStateException(
                            "One of the value associated with $key is not within the range of Float"
                        )
                    }
                    doubleValue.toFloat()
                }
                ?.toFloatArray()
        spec?.validateReadRequest(
            key,
            Float::class.java,
            isCollection = true,
            targetValue = floatArrayValue,
        )
        return floatArrayValue
    }

    /**
     * Retrieves a [DoubleArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getDoubleArray(key: String): DoubleArray? {
        val doubleArrayValue = unsafeGetProperty(key, DoubleArray::class.java)
        spec?.validateReadRequest(
            key,
            Double::class.java,
            isCollection = true,
            targetValue = doubleArrayValue,
        )
        return doubleArrayValue
    }

    /**
     * Retrieves an [IntArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getIntArray(key: String): IntArray? {
        val longArrayValue = unsafeGetProperty(key, LongArray::class.java)
        val intArrayValue =
            longArrayValue
                ?.map { longValue ->
                    if (!isLongWithinLongRange(longValue)) {
                        // This should never happen because the setters forbid such value to exist
                        // in
                        // the first place.
                        throw IllegalStateException(
                            "One of the value associated with $key is not within the range of Int"
                        )
                    }
                    longValue.toInt()
                }
                ?.toIntArray()
        spec?.validateReadRequest(
            key,
            Int::class.java,
            isCollection = true,
            targetValue = intArrayValue,
        )
        return intArrayValue
    }

    /**
     * Retrieves a [LongArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getLongArray(key: String): LongArray? {
        val longArrayValue = unsafeGetProperty(key, LongArray::class.java)
        spec?.validateReadRequest(
            key,
            Long::class.java,
            isCollection = true,
            targetValue = longArrayValue,
        )
        return longArrayValue
    }

    /**
     * Retrieves a [ByteArray] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    public fun getByteArray(key: String): ByteArray? {
        return getByteArrayOrNull(key)
    }

    // Equivalent to getByteArray. Provided for generated AppFunctionSerializableFactory to use
    // without changing the access getter name based on type.
    @RestrictTo(LIBRARY_GROUP)
    public fun getByteArrayOrNull(key: String): ByteArray? {
        val byteArrayValue = unsafeGetProperty(key, Array<ByteArray>::class.java)
        val finalByteArrayValue =
            if (byteArrayValue == null || byteArrayValue.isEmpty()) {
                null
            } else {
                byteArrayValue[0]
            }
        spec?.validateReadRequest(
            key,
            Byte::class.java,
            isCollection = true,
            targetValue = finalByteArrayValue,
        )
        return finalByteArrayValue
    }

    /**
     * Retrieves a [List] of [String] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @Suppress("NullableCollection")
    public fun getStringList(key: String): List<String>? {
        val stringArrayValue = unsafeGetProperty(key, Array<String>::class.java)
        val stringListValue = stringArrayValue?.asList()
        spec?.validateReadRequest(
            key,
            String::class.java,
            isCollection = true,
            targetValue = stringListValue,
        )
        return stringListValue
    }

    /**
     * Retrieves a [List] of [AppFunctionData] value associated with the specified [key].
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the [key]. Or null if the associated value is not found.
     * @throws IllegalArgumentException if the [key] is not allowed or the value type is incorrect
     *   according to the metadata specification.
     */
    @Suppress("NullableCollection")
    public fun getAppFunctionDataList(key: String): List<AppFunctionData>? {
        val dataArrayValue =
            unsafeGetProperty(key, Array<GenericDocument>::class.java)?.mapIndexed { index, element
                ->
                AppFunctionData(
                    spec?.getPropertyObjectSpec(key, element.schemaType),
                    element,
                    extras.getBundle(extrasKey(key, index)) ?: Bundle.EMPTY,
                )
            }
        spec?.validateReadRequest(
            key,
            AppFunctionData::class.java,
            isCollection = true,
            targetValue = dataArrayValue,
        )
        return dataArrayValue
    }

    /**
     * Retrieves a [List] of [Parcelable] values of type [T] associated with the specified [key].
     *
     * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
     * between agents and apps), forward and backward compatibility is **not guaranteed** by this
     * library. Implementers are responsible for managing any compatibility and versioning concerns.
     *
     * @param key The key to retrieve the list for.
     * @param clazz The [Class] of the [Parcelable] object to retrieve, of type [T].
     * @return The [List] associated with the [key], or null if the value is not found or if any
     *   list item is not of type [T].
     */
    @Suppress("NullableCollection")
    public fun <T : Parcelable> getParcelableList(key: String, clazz: Class<T>): List<T>? {
        extras.classLoader = clazz.classLoader
        val parcelableList = extras.getParcelableArrayList(extrasKey(key), clazz)
        spec?.validateReadRequest(key, clazz, isCollection = true, targetValue = parcelableList)
        if (parcelableList?.all { clazz.isInstance(it) } != true) {
            // For some reason Bundle.getParcelableArrayList doesn't return null even when type is
            // wrong.
            return null
        }
        return parcelableList
    }

    /**
     * Retrieves a [List] of [Parcelable] values of type [T] associated with the specified [key].
     *
     * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
     * between agents and apps), forward and backward compatibility is **not guaranteed** by this
     * library. Implementers are responsible for managing any compatibility and versioning concerns.
     *
     * @param T The type of the [Parcelable] object to retrieve.
     * @param key The key to retrieve the list for.
     * @return The [List] associated with the [key], or null if the value is not found or if any
     *   list item is not of type [T].
     */
    @Suppress("NullableCollection")
    public inline fun <reified T : Parcelable> getParcelableList(key: String): List<T>? {
        return getParcelableList(key, T::class.java)
    }

    override fun toString(): String {
        // TODO(b/391419368): Improve output to avoid reference to underlying GenericDocument
        return "AppFunctionData(genericDocument=$genericDocument, extras=$extras)"
    }

    private fun isDoubleWithinFloatRange(doubleValue: Double): Boolean {
        if (doubleValue.isInfinite() || doubleValue.isNaN()) {
            // Float also has NaN and Infinity representation
            return true
        }
        if (doubleValue > Float.MAX_VALUE || doubleValue < -Float.MAX_VALUE) {
            // The double value is not within the range of a Float.
            return false
        }
        return true
    }

    private fun isLongWithinLongRange(longValue: Long): Boolean {
        return longValue >= Int.MIN_VALUE && longValue <= Int.MAX_VALUE
    }

    /**
     * Deserializes the [AppFunctionData] to an [AppFunctionSerializable] instance.
     *
     * @param serializableClass The AppFunctionSerializable class.
     * @return The instance of [serializableClass].
     * @throws IllegalArgumentException If unable to deserialize the [AppFunctionData] to an
     *   instance of [serializableClass].
     * @see [AppFunctionSerializable]
     */
    public fun <T : Any> deserialize(serializableClass: Class<T>): T {
        return try {
            val factory = getSerializableFactory(serializableClass)
            factory.fromAppFunctionData(this)
        } catch (e: Exception) {
            Log.d(
                APP_FUNCTIONS_TAG,
                "Something went wrong while deserialize $this to $serializableClass",
                e,
            )
            throw IllegalArgumentException(
                "Unable to deserialize $serializableClass. Is the class annotated with @AppFunctionSerializable?"
            )
        }
    }

    /**
     * Deserializes the [AppFunctionData] to an [AppFunctionSerializable] instance identified by
     * [qualifiedName].
     *
     * @param qualifiedName The qualifiedName of the AppFunctionSerializable class.
     * @return The instance of the class identified by [qualifiedName].
     * @throws IllegalArgumentException If unable to deserialize the [AppFunctionData] to an
     *   instance of the class identified by [qualifiedName].
     * @see [AppFunctionSerializable]
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun <T : Any> deserialize(qualifiedName: String): T {
        return deserialize<T>(getSerializableClass(qualifiedName))
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
     * Creates a new [AppFunctionData] instance with a new [AppFunctionDataSpec] based on the
     * provided [responseMetadata] and [componentMetadata].
     *
     * This should only used by the AppFunction infrastructure when recreating the [AppFunctionData]
     * from remote response. So that the [AppFunctionData] exposed in the public API surface will
     * have the same read validation.
     */
    internal fun replaceSpecWith(
        responseMetadata: AppFunctionResponseMetadata,
        componentMetadata: AppFunctionComponentsMetadata,
    ): AppFunctionData {
        return AppFunctionData(
            AppFunctionDataSpec.create(responseMetadata, componentMetadata),
            genericDocument,
            extras,
        )
    }

    /**
     * Creates a new [AppFunctionData] instance with a new [AppFunctionDataSpec] based on the
     * provided [objectTypeMetadata] and [componentMetadata].
     *
     * This should only used by the AppFunction infrastructure when recreating the [AppFunctionData]
     * from remote response. So that the [AppFunctionData] exposed in the public API surface will
     * have the same read validation.
     */
    internal fun replaceSpecWith(
        objectTypeMetadata: AppFunctionObjectTypeMetadata,
        componentMetadata: AppFunctionComponentsMetadata,
    ): AppFunctionData {
        return AppFunctionData(
            AppFunctionDataSpec.create(objectTypeMetadata, componentMetadata),
            genericDocument,
            extras,
        )
    }

    /**
     * Creates a new [AppFunctionData] instance with a new [AppFunctionDataSpec] based on the
     * provided [parameterMetadata] and [componentMetadata].
     *
     * This should only used by the AppFunction infrastructure when recreating the [AppFunctionData]
     * from remote response. So that the [AppFunctionData] exposed in the public API surface will
     * have the same read validation.
     */
    internal fun replaceSpecWith(
        parameterMetadata: List<AppFunctionParameterMetadata>,
        componentMetadata: AppFunctionComponentsMetadata,
    ): AppFunctionData =
        AppFunctionData(
            AppFunctionDataSpec.create(parameterMetadata, componentMetadata),
            genericDocument,
            extras,
        )

    /** Visits all [AppFunctionUriGrant] under the [AppFunctionData]. */
    @WorkerThread
    @RestrictTo(LIBRARY_GROUP)
    public fun visitAppFunctionUriGrants(visitor: (AppFunctionUriGrant) -> Unit) {
        if (qualifiedName == AppFunctionUriGrant::class.java.canonicalName) {
            val uriGrant =
                try {
                    deserialize(AppFunctionUriGrant::class.java)
                } catch (e: Exception) {
                    Log.d(
                        APP_FUNCTIONS_TAG,
                        "Unexpected error while visiting AppFunctionUriGrant",
                        e,
                    )
                    null
                }

            if (uriGrant != null) {
                visitor.invoke(uriGrant)
            }
        }

        for (key in genericDocument.propertyNames) {
            // AppFunctionUriGrant would only be stored as either singular AppFunctionData or list
            // of AppFunctionData.
            try {
                getAppFunctionData(key)?.visitAppFunctionUriGrants(visitor)
            } catch (_: Exception) {
                // Swallow the exception as it is an unsafe read in the first place
            }
            try {
                for (data in getAppFunctionDataList(key) ?: emptyList()) {
                    data.visitAppFunctionUriGrants(visitor)
                }
            } catch (_: Exception) {
                // Swallow the exception as it is an unsafe read in the first place
            }
        }
    }

    /**
     * Builder for constructing [AppFunctionData]
     *
     * For example, to write an [AppFunctionData] for calling an AppFunction
     *
     * ```
     * fun callCreateNoteFunction(metadata: AppFunctionMetadata) {
     *   val appFunctionData = AppFunctionData.Builder(
     *     metadata.parameters,
     *     metadata.components,
     *   ).apply {
     *     setString("title", "Note Title")
     *     // If the function doesn't accept "owner" as parameter, it would throw an error
     *     setString("owner", "Me")
     *     // If the function actually expects "content" as String, it would throw an error
     *     setInt("content", 100)
     *   }
     *    .build()
     * }
     * ```
     */
    public class Builder {

        // TODO(b/447302747): Remove this once the constructor that takes qualifiedName has removed
        private val qualifiedName: String
        // TODO(b/447302747): Make it non-null once the constructor that takes qualifiedName has
        // removed
        private val spec: AppFunctionDataSpec?
        private var genericDocumentBuilder: GenericDocument.Builder<*>
        private val extrasBuilder = Bundle()

        // TODO(b/447302747): Clean up the usage without providing metadata.
        /**
         * @param id: Only set this when creating a document for the legacy schema. In the legacy
         *   schema, ID is stored as [GenericDocument.id]. In Jetpack, ID is just a normal property.
         */
        internal constructor(qualifiedName: String, id: String = "") {
            this.qualifiedName = qualifiedName
            spec = null
            genericDocumentBuilder =
                GenericDocument.Builder<GenericDocument.Builder<*>>("", id, qualifiedName)
        }

        /**
         * Constructs a [Builder] to create input data for an AppFunction execution call.
         *
         * The caller can use this to construct the [AppFunctionData] for
         * [ExecuteAppFunctionRequest.functionParameters].
         *
         * @param parameterMetadataList List of [AppFunctionParameterMetadata] defining the input
         *   parameters.
         * @param componentMetadata [AppFunctionComponentsMetadata] that has the shared data types.
         * @see [AppFunctionParameterMetadata]
         * @see [AppFunctionComponentsMetadata]
         */
        public constructor(
            parameterMetadataList: List<AppFunctionParameterMetadata>,
            componentMetadata: AppFunctionComponentsMetadata,
        ) : this(AppFunctionDataSpec.create(parameterMetadataList, componentMetadata))

        /**
         * Constructs a [Builder] to create [AppFunctionData] representing an object.
         *
         * The caller can use this to construct the [AppFunctionData] that conforms with the
         * provided [objectTypeMetadata].
         *
         * @param objectTypeMetadata [AppFunctionObjectTypeMetadata] defining the object structure.
         * @param componentMetadata [AppFunctionComponentsMetadata] that has the shared data types.
         * @see [AppFunctionObjectTypeMetadata]
         * @see [AppFunctionComponentsMetadata]
         */
        public constructor(
            objectTypeMetadata: AppFunctionObjectTypeMetadata,
            componentMetadata: AppFunctionComponentsMetadata,
        ) : this(AppFunctionDataSpec.create(objectTypeMetadata, componentMetadata))

        /**
         * Constructs a [Builder] to create [AppFunctionData] representing an all-of object.
         *
         * The caller can use this to construct the [AppFunctionData] that conforms with the
         * provided [allOfTypeMetadata].
         *
         * @param allOfTypeMetadata [AppFunctionAllOfTypeMetadata] defining the object structure.
         * @param componentMetadata [AppFunctionComponentsMetadata] that has the shared data types.
         * @see [AppFunctionAllOfTypeMetadata]
         * @see [AppFunctionComponentsMetadata]
         */
        public constructor(
            allOfTypeMetadata: AppFunctionAllOfTypeMetadata,
            componentMetadata: AppFunctionComponentsMetadata,
        ) : this(
            AppFunctionDataSpec.create(
                allOfTypeMetadata.getPseudoObjectTypeMetadata(componentMetadata),
                componentMetadata,
            )
        )

        /**
         * Constructs a [Builder] to create [AppFunctionData] representing a response.
         *
         * The caller can use this to construct the [AppFunctionData] for
         * [ExecuteAppFunctionResponse.Success.returnValue].
         *
         * @param responseMetadata [AppFunctionResponseMetadata] defining the response structure.
         * @param componentMetadata [AppFunctionComponentsMetadata] that has the shared data types.
         * @see [AppFunctionResponseMetadata]
         * @see [AppFunctionComponentsMetadata]
         */
        public constructor(
            responseMetadata: AppFunctionResponseMetadata,
            componentMetadata: AppFunctionComponentsMetadata,
        ) : this(AppFunctionDataSpec.create(responseMetadata, componentMetadata))

        private constructor(spec: AppFunctionDataSpec) {
            this.spec = spec
            this.qualifiedName = spec.objectQualifiedName
            genericDocumentBuilder =
                GenericDocument.Builder<GenericDocument.Builder<*>>(
                    "",
                    "",
                    spec.objectQualifiedName,
                )
        }

        private fun setLegacyId(id: String) {
            // TODO(b/412573017): setId is only available in certain Android T extensions. Check if
            // we need to have a compat version of this API to set the ID.
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 13) {
                genericDocumentBuilder.setId(id)
            } else {
                Log.wtf(
                    APP_FUNCTIONS_TAG,
                    "setId method in GenericDocument isn't supported on the current device.",
                )
            }
        }

        /**
         * Sets a [Boolean] value for the given [key].
         *
         * @param key The key to set the [Boolean] value for.
         * @param value The [Boolean] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setBoolean(key: String, value: Boolean): Builder {
            spec?.validateWriteRequest(
                key,
                Boolean::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyBoolean(key, value)
            return this
        }

        /**
         * Sets a [Float] value for the given [key].
         *
         * @param key The key to set the [Float] value for.
         * @param value The [Float] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setFloat(key: String, value: Float): Builder {
            spec?.validateWriteRequest(
                key,
                Float::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyDouble(key, value.toDouble())
            return this
        }

        /**
         * Sets a [Double] value for the given [key].
         *
         * @param key The key to set the [Double] value for.
         * @param value The [Double] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setDouble(key: String, value: Double): Builder {
            spec?.validateWriteRequest(
                key,
                Double::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyDouble(key, value)
            return this
        }

        /**
         * Sets an [Int] value for the given [key].
         *
         * @param key The key to set the [Int] value for.
         * @param value The [Int] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setInt(key: String, value: Int): Builder {
            spec?.validateWriteRequest(
                key,
                Int::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyLong(key, value.toLong())
            return this
        }

        /**
         * Sets a [Long] value for the given [key].
         *
         * @param key The key to set the [Long] value for.
         * @param value The [Long] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setLong(key: String, value: Long): Builder {
            spec?.validateWriteRequest(
                key,
                Long::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyLong(key, value)
            return this
        }

        /**
         * Sets a [String] value for the given [key].
         *
         * @param key The key to set the [String] value for.
         * @param value The [String] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setString(key: String, value: String): Builder {
            spec?.validateWriteRequest(
                key,
                String::class.java,
                isCollection = false,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyString(key, value)
            if (key == LEGACY_ID_FIELD_KEY) {
                setLegacyId(value)
            }
            return this
        }

        /**
         * Sets an [AppFunctionData] value for the given [key].
         *
         * @param key The key to set the [AppFunctionData] value for.
         * @param value The [AppFunctionData] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setAppFunctionData(key: String, value: AppFunctionData): Builder {
            spec?.validateWriteRequest(
                key,
                AppFunctionData::class.java,
                isCollection = false,
                targetValue = value,
            )
            spec?.getPropertyObjectSpec(key, value.qualifiedName)?.validateDataSpecMatches(value)

            genericDocumentBuilder.setPropertyDocument(key, value.genericDocument)
            if (!value.extras.isEmpty()) {
                extrasBuilder.putBundle(extrasKey(key), value.extras)
            }
            return this
        }

        /**
         * Sets a [Parcelable] value of type [T] for the given [key].
         *
         * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
         * between agents and apps), forward and backward compatibility is **not guaranteed** by
         * this library. The sender and receiver of the `Parcelable` are responsible for managing
         * any compatibility and versioning concerns.
         *
         * @param key The key to set the value for.
         * @param value The [Parcelable] value of type [T] to set.
         */
        @CanIgnoreReturnValue
        public fun <T : Parcelable> setParcelable(key: String, value: T): Builder {
            spec?.validateWriteRequest(
                key,
                value.javaClass,
                isCollection = false,
                targetValue = value,
            )
            extrasBuilder.putParcelable(extrasKey(key), value)
            return this
        }

        /**
         * Sets a [BooleanArray] value for the given [key].
         *
         * @param key The key to set the [BooleanArray] value for.
         * @param value The [BooleanArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setBooleanArray(key: String, value: BooleanArray): Builder {
            spec?.validateWriteRequest(
                key,
                Boolean::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyBoolean(key, *value)
            return this
        }

        /**
         * Sets a [FloatArray] value for the given [key].
         *
         * @param key The key to set the [DoubleArray] value for.
         * @param value The [FloatArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setFloatArray(key: String, value: FloatArray): Builder {
            spec?.validateWriteRequest(
                key,
                Float::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyDouble(
                key,
                *(value.asList().map { it.toDouble() }.toDoubleArray()),
            )
            return this
        }

        /**
         * Sets a [DoubleArray] value for the given [key].
         *
         * @param key The key to set the [DoubleArray] value for.
         * @param value The [DoubleArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setDoubleArray(key: String, value: DoubleArray): Builder {
            spec?.validateWriteRequest(
                key,
                Double::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyDouble(key, *value)
            return this
        }

        /**
         * Sets an [IntArray] value for the given [key].
         *
         * @param key The key to set the [IntArray] value for.
         * @param value The [IntArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setIntArray(key: String, value: IntArray): Builder {
            spec?.validateWriteRequest(
                key,
                Int::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyLong(
                key,
                *(value.asList().map { it.toLong() }.toLongArray()),
            )
            return this
        }

        /**
         * Sets a [LongArray] value for the given [key].
         *
         * @param key The key to set the [LongArray] value for.
         * @param value The [LongArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setLongArray(key: String, value: LongArray): Builder {
            spec?.validateWriteRequest(
                key,
                Long::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyLong(key, *value)
            return this
        }

        /**
         * Sets a [ByteArray] value for the given [key].
         *
         * @param key The key to set the [ByteArray] value for.
         * @param value The [ByteArray] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setByteArray(key: String, value: ByteArray): Builder {
            spec?.validateWriteRequest(
                key,
                Byte::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyBytes(key, value)
            return this
        }

        /**
         * Sets a [List] of [String] value for the given [key].
         *
         * @param key The key to set the [List] of [String] value for.
         * @param value The [List] of [String] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setStringList(key: String, value: List<String>): Builder {
            spec?.validateWriteRequest(
                key,
                String()::class
                    .java, // Note: String()::class.java is equivalent to String::class.java
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyString(key, *value.toTypedArray())
            return this
        }

        /**
         * Sets a [List] of [AppFunctionData] value for the given [key].
         *
         * @param key The key to set the [List] of [AppFunctionData] value for.
         * @param value The [List] of [AppFunctionData] value to set.
         * @throws IllegalArgumentException if the [key] is not allowed or the [value] does not
         *   match the metadata specification associated with the [key].
         */
        @CanIgnoreReturnValue
        public fun setAppFunctionDataList(key: String, value: List<AppFunctionData>): Builder {
            spec?.validateWriteRequest(
                key,
                AppFunctionData::class.java,
                isCollection = true,
                targetValue = value,
            )
            genericDocumentBuilder.setPropertyDocument(
                key,
                *value.map { it.genericDocument }.toTypedArray(),
            )
            value.forEachIndexed { index, element ->
                spec
                    ?.getPropertyObjectSpec(key, element.qualifiedName)
                    ?.validateDataSpecMatches(element)
                if (!element.extras.isEmpty()) {
                    extrasBuilder.putBundle(extrasKey(key, index), element.extras)
                }
            }
            return this
        }

        /**
         * Sets a [List] of [Parcelable] values of type [T] for the given [key].
         *
         * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
         * between agents and apps), forward and backward compatibility is **not guaranteed** by
         * this framework. The sender and receiver of the `Parcelable` are responsible for managing
         * any compatibility and versioning concerns.
         *
         * @param key The key to set the list for.
         * @param value The [List] of [Parcelable] values of type [T] to set.
         */
        @CanIgnoreReturnValue
        public inline fun <reified T : Parcelable> setParcelableList(
            key: String,
            value: List<T>,
        ): Builder {
            return setParcelableList(key, value, T::class.java)
        }

        /**
         * Sets a [List] of [Parcelable] values of type [T] for the given [key].
         *
         * For `Parcelable` types not defined by the Android platform (e.g., custom classes shared
         * between agents and apps), forward and backward compatibility is **not guaranteed** by
         * this framework. The sender and receiver of the `Parcelable` are responsible for managing
         * any compatibility and versioning concerns.
         *
         * @param key The key to set the list for.
         * @param value The [List] of [Parcelable] values of type [T] to set.
         * @param clazz The [Class] of the [Parcelable] list to set, of type [T].
         */
        @CanIgnoreReturnValue
        public fun <T : Parcelable> setParcelableList(
            key: String,
            value: List<T>,
            clazz: Class<T>,
        ): Builder {
            spec?.validateWriteRequest(key, clazz, isCollection = true, targetValue = value)
            extrasBuilder.putParcelableArrayList(extrasKey(key), ArrayList(value))
            return this
        }

        /**
         * Builds [AppFunctionData]
         *
         * @throws IllegalArgumentException if any required property, as defined by the metadata
         *   specification, is missing.
         */
        public fun build(): AppFunctionData {
            val builtGenericDocument = genericDocumentBuilder.build()
            val appFunctionData = AppFunctionData(spec, builtGenericDocument, extrasBuilder)
            spec?.getAllPropertyKeys()?.forEach { propertyKey ->
                if (spec.isRequired(propertyKey)) {
                    if (!appFunctionData.containsKey(propertyKey)) {
                        throw IllegalArgumentException(
                            "Missing required property: '$propertyKey' for object '$qualifiedName'"
                        )
                    }
                }
            }
            return appFunctionData
        }
    }

    public companion object {
        private const val DEFAULT_BOOLEAN: Boolean = false
        private const val DEFAULT_FLOAT: Float = 0F
        private const val DEFAULT_DOUBLE: Double = 0.0
        private const val DEFAULT_INT: Int = 0
        private const val DEFAULT_LONG: Long = 0L

        private const val LEGACY_ID_FIELD_KEY = "id"

        private fun extrasKey(key: String) = "property/$key"

        private fun extrasKey(key: String, index: Int) = "property/$key[$index]"

        // TODO(b/399823985): Codegen the mapping table to prevent using reflection
        private fun <T : Any> getSerializableClass(qualifiedName: String): Class<T> {
            return try {
                @Suppress("UNCHECKED_CAST")
                Class.forName(qualifiedName) as Class<T>
            } catch (e: Exception) {
                Log.d(APP_FUNCTIONS_TAG, "Unable to find serializable class $qualifiedName", e)
                throw IllegalArgumentException("Unable to find serializable class $qualifiedName")
            }
        }

        // TODO(b/399823985): Codegen the mapping table to prevent using reflection
        private fun <T : Any> getSerializableFactory(
            serializableClass: Class<T>
        ): AppFunctionSerializableFactory<T> {
            val packageName = getPackageName(serializableClass)
            // Using `serializableClass.name` and not `serializableClass.simpleName` to be able to
            // reference nested classes (e.g. OuterClass$InnerClass), to avoid ambiguity between
            // inner classes of the same name.
            val serializableSimpleName = serializableClass.name.substringAfterLast('.')

            val factorySimpleName = "${'$'}${serializableSimpleName}Factory"
            val factoryClassName = "${packageName}.${factorySimpleName}"

            return try {
                val factoryClass = Class.forName(factoryClassName)
                @Suppress("UNCHECKED_CAST")
                factoryClass.getDeclaredConstructor().newInstance()
                    as AppFunctionSerializableFactory<T>
            } catch (e: Exception) {
                Log.d(
                    APP_FUNCTIONS_TAG,
                    "Unable to create AppFunctionSerializableFactory for $serializableClass",
                    e,
                )
                throw IllegalArgumentException(
                    "Unable to create AppFunctionSerializableFactory for $serializableClass"
                )
            }
        }

        private fun getPackageName(serializableClass: Class<*>): String {
            val setOfProxyTypes = setOf(LocalDateTime::class.simpleName, Uri::class.simpleName)
            val serializableProxyPackageName = "androidx.appfunctions.internal.serializableproxies"
            if (setOfProxyTypes.contains(serializableClass.simpleName)) {
                return serializableProxyPackageName
            }

            return serializableClass.packageName
        }

        /**
         * Serializes [serializable] to an [AppFunctionData].
         *
         * @param serializable The instance of [serializableClass].
         * @param serializableClass The class of [serializable].
         * @return [AppFunctionData] with properties from [serializable].
         * @throws IllegalArgumentException If unable to serialize [serializable] to an
         *   [AppFunctionData].
         * @see [AppFunctionSerializable]
         */
        @JvmStatic
        public fun <T : Any> serialize(
            serializable: T,
            serializableClass: Class<T>,
        ): AppFunctionData {
            return try {
                val factory = getSerializableFactory(serializableClass)
                factory.toAppFunctionData(serializable)
            } catch (e: Exception) {
                Log.d(
                    APP_FUNCTIONS_TAG,
                    "Something went wrong while serialize $serializable of class $serializableClass",
                    e,
                )
                throw IllegalArgumentException(
                    "Unable to serialize $serializableClass. Is the class annotated with @AppFunctionSerializable?"
                )
            }
        }

        /**
         * Serializes [serializable] to an [AppFunctionData].
         *
         * @param serializable The instance of [qualifiedName].
         * @param qualifiedName The qualified name of the class [serializable].
         * @return [AppFunctionData] with properties from [serializable].
         * @throws IllegalArgumentException If unable to serialize [serializable] to an
         *   [AppFunctionData].
         * @see [AppFunctionSerializable]
         */
        @RestrictTo(LIBRARY_GROUP)
        public fun <T : Any> serialize(serializable: T, qualifiedName: String): AppFunctionData {
            return serialize(serializable, getSerializableClass<T>(qualifiedName))
        }

        /** Represents an empty [AppFunctionData]. */
        @JvmField
        public val EMPTY: AppFunctionData =
            AppFunctionData(
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build(),
                Bundle.EMPTY,
            )
    }
}
