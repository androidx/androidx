/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.annotation.RestrictTo
import java.lang.Class

/**
 * A read-only, map-like interface for accessing unified camera metadata.
 *
 * This interface serves as the common base for metadata containers in the Camera common library,
 * such as [CameraCharacteristicsWrapper], [CaptureRequestWrapper], and [CaptureResultWrapper].
 *
 * It provides type-safe access to library-defined custom keys ([Metadata.Key]) in addition to the
 * platform-specific keys supported by its sub-interfaces.
 *
 * ### Key Concepts
 * - **Unified Access:** Allows combining native camera metadata (from the Android Camera2 API) with
 *   custom, internally-computed, or version-compatibility properties.
 * - **Read-Only:** The container's contents cannot be modified through this interface.
 * - **Type-Safety:** Values are retrieved using strongly-typed [Key] instances.
 */
public interface Metadata {
    /**
     * Retrieves the value associated with the specified [Metadata.Key].
     *
     * @param T The type of the value.
     * @param key The custom key to query.
     * @return The value associated with the key, or `null` if the key is not present in this
     *   container or if its value is not set.
     */
    public operator fun <T> get(key: Key<T>): T?

    /**
     * Retrieves the value associated with the specified [Metadata.Key], or returns [default] if the
     * key is not present.
     *
     * @param T The type of the value.
     * @param key The custom key to query.
     * @param default The value to return if the key is not present or if its value is `null`.
     * @return The value associated with the key, or [default] if the value is not present.
     */
    public fun <T> getOrDefault(key: Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * The set of all custom [Metadata.Key]s currently available in this container.
     *
     * Note that this set contains *only* custom library-defined keys. It does not include
     * platform-specific keys (such as [android.hardware.camera2.CameraCharacteristics.Key] or
     * [android.hardware.camera2.CaptureResult.Key]), which are managed separately by the
     * implementing wrappers.
     */
    public val metadataKeys: Set<Key<*>>

    /**
     * A type-safe key used to identify and retrieve values from a [Metadata] container.
     *
     * Keys are unique by their [name]. Only one [Key] instance can exist for a given name.
     * Attempting to create a key with an existing name but a different [type] will throw an
     * [IllegalStateException].
     * > [!IMPORTANT] Creating new [Key] instances is restricted to the AndroidX Camera library
     * > group. External clients can use predefined keys but cannot define their own.
     *
     * ### Examples (Internal Library Usage Only)
     *
     * Creating keys in Kotlin:
     * ```kotlin
     * // For primitive/standard types:
     * val widthKey = Metadata.Key<Int>("androidx.camera.width")
     * val nameKey = Metadata.Key<String>("androidx.camera.name")
     *
     * // For custom classes:
     * class MyConfig(val value: String)
     * val configKey = Metadata.Key<MyConfig>("androidx.camera.config")
     * ```
     *
     * @param T The type of the value associated with this key.
     * @property name The unique string name identifying this key.
     * @property type The [Class] representing the type of the value associated with this key.
     */
    public class Key<T> internal constructor(public val name: String, public val type: Class<T>) {
        public companion object {
            @JvmStatic private val keys: MutableMap<String, Key<*>> = HashMap()

            /**
             * Creates or retrieves a [Key] with the given [name] and inferred type [T].
             *
             * In Kotlin, this allows creating keys using constructor-like syntax:
             * ```kotlin
             * val myKey = Metadata.Key<String>("my.key.name")
             * ```
             *
             * @param T The type of the value associated with the key.
             * @param name The unique name for this key.
             * @return The existing or newly created [Key].
             * @throws IllegalStateException if a key with the same [name] but a different type
             *   already exists.
             */
            @JvmStatic
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public inline operator fun <reified T : Any> invoke(name: String): Key<T> =
                create(name, T::class.java)

            /**
             * Creates or retrieves a [Key] with the given [name] and [type].
             *
             * This method is primarily intended for Java compatibility or when the class type
             * cannot be reified.
             *
             * @param T The type of the value associated with the key.
             * @param name The unique name for this key.
             * @param type The [Class] representing the type of the value.
             * @return The existing or newly created [Key].
             * @throws IllegalStateException if a key with the same [name] but a different type
             *   already exists.
             */
            @JvmStatic
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Suppress("UNCHECKED_CAST")
            public fun <T : Any> create(name: String, type: Class<T>): Key<T> =
                synchronized(keys) {
                    val key = keys.getOrPut(name) { Key(name, type) }
                    check(key.type == type) {
                        "Key '$name' already exists with a different type: ${key.type.name} (requested: ${type.name})"
                    }
                    key as Key<T>
                }
        }

        override fun toString(): String = "$name: ${type.simpleName}"
    }
}

/**
 * An interface that provides compatibility-focused, read-only query access to
 * [CameraCharacteristics].
 *
 * Use this interface to query camera capabilities, physical properties, and supported
 * configurations. Implementing classes can abstract OS version differences and cache
 * expensive-to-retrieve properties to ensure efficient access across all API levels.
 *
 * `CameraCharacteristicsMetadata` extends [Metadata], supporting type-safe retrieval of custom
 * library-defined metadata keys via [Metadata.Key] in addition to native
 * [CameraCharacteristics.Key] keys.
 *
 * NOTE: This interface is not stable for inheritance. Do not implement this interface directly. For
 * testing, use the fakes in the `androidx.camera.common.testing` package.
 *
 * ### Example
 *
 * Querying native and custom keys:
 * ```kotlin
 * val characteristics: CameraCharacteristicsMetadata = ...
 *
 * // Query a native CameraCharacteristics key:
 * val lensFacing: Int? = characteristics[CameraCharacteristics.LENS_FACING]
 *
 * // Query a custom Metadata key:
 * val customValue: String? = characteristics[MY_CUSTOM_METADATA_KEY]
 * ```
 */
public interface CameraCharacteristicsMetadata : Metadata, UnsafeWrapper {
    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key], or returns [default] if the
     * value is `null` or the key is unsupported.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or unsupported.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return get(key) ?: default
    }
}

/**
 * An interface that provides compatibility-focused, read-only query access to the settings applied
 * to a camera capture request.
 *
 * Use this interface to inspect the configuration parameters sent to the camera device during a
 * capture session. Implementing classes can abstract OS version differences and cache
 * expensive-to-retrieve properties to ensure efficient access across all API levels.
 *
 * `CaptureRequestMetadata` extends [Metadata], supporting type-safe retrieval of custom
 * library-defined metadata keys via [Metadata.Key] in addition to native [CaptureRequest.Key] keys
 * (using [get] and [getOrDefault]).
 *
 * `CaptureRequestMetadata` is a superinterface of [CaptureRequestWrapper].
 *
 * NOTE: This interface is not stable for inheritance. Do not implement this interface directly. For
 * testing, use the fakes in the `androidx.camera.common.testing` package.
 *
 * ### Example
 *
 * Querying native and custom keys:
 * ```kotlin
 * val request: CaptureRequestMetadata = ...
 *
 * // Query a native CaptureRequest key:
 * val exposureTime: Long? = request[CaptureRequest.SENSOR_EXPOSURE_TIME]
 *
 * // Query a custom Metadata key:
 * val customKey = Metadata.Key<Int>("androidx.camera.custom_key")
 * val customValue: Int? = request[customKey]
 * ```
 *
 * @see Metadata
 */
public interface CaptureRequestMetadata : Metadata, UnsafeWrapper {
    /**
     * Retrieves the value of the specified [CaptureRequest.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureRequest.Key], or returns [default] if the key is
     * not present or its value is `null`.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or is `null`.
     * @return The value of the key, or [default] if the key is not present or its value is `null`.
     */
    public fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T {
        return get(key) ?: default
    }
}

/**
 * An interface that provides compatibility-focused, read-only query access to the results of a
 * single image capture.
 *
 * Use this interface to query the results of a single image capture, including sensor exposure,
 * lens status, and 3A states. Implementing classes can abstract OS version differences and cache
 * expensive-to-retrieve properties to ensure efficient access across all API levels.
 *
 * `CaptureResultMetadata` extends [Metadata], supporting type-safe retrieval of custom
 * library-defined metadata keys via [Metadata.Key] in addition to native [CaptureResult.Key] keys
 * (using [get] and [getOrDefault]).
 *
 * `CaptureResultMetadata` is a superinterface of [CaptureResultWrapper].
 *
 * NOTE: This interface is not stable for inheritance. Do not implement this interface directly. For
 * testing, use the fakes in the `androidx.camera.common.testing` package.
 *
 * ### Example
 *
 * Querying native and custom keys:
 * ```kotlin
 * val result: CaptureResultMetadata = ...
 *
 * // Query a native CaptureResult key:
 * val lensState: Int? = result[CaptureResult.LENS_STATE]
 *
 * // Query a custom Metadata key:
 * val customKey = Metadata.Key<Int>("androidx.camera.custom_key")
 * val customValue: Int? = result[customKey]
 * ```
 *
 * @see Metadata
 */
public interface CaptureResultMetadata : Metadata, UnsafeWrapper {
    /**
     * Retrieves the value of the specified [CaptureResult.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CaptureResult.Key<T>): T?

    /**
     * Retrieves the value of the specified [CaptureResult.Key], or returns [default] if the key is
     * not present or its value is null.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or its value is null.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T {
        return get(key) ?: default
    }
}

/**
 * Helper extension to perform type-safe unchecked casts from generic maps.
 *
 * This is an internal helper to retrieve a value from a map of [Metadata.Key] to generic objects,
 * casting the result to the type specified by the key.
 *
 * @param T The expected return type.
 * @param key The key to look up.
 * @return The casted value, or `null` if the key is not present in the map.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<Metadata.Key<*>, *>.getUnchecked(key: Metadata.Key<T>): T? = this[key] as T?

/**
 * Helper extension to perform type-safe unchecked casts from generic maps.
 *
 * This is an internal helper to retrieve a value from a map of [CameraCharacteristics.Key] to
 * generic objects, casting the result to the type specified by the key.
 *
 * @param T The expected return type.
 * @param key The key to look up.
 * @return The casted value, or `null` if the key is not present in the map.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CameraCharacteristics.Key<*>, *>.getUnchecked(
    key: CameraCharacteristics.Key<T>
): T? = this[key] as T?

/**
 * Helper extension to perform type-safe unchecked casts from generic maps.
 *
 * This is an internal helper to retrieve a value from a map of [CaptureRequest.Key] to generic
 * objects, casting the result to the type specified by the key.
 *
 * @param T The expected return type.
 * @param key The key to look up.
 * @return The casted value, or `null` if the key is not present in the map.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CaptureRequest.Key<*>, *>.getUnchecked(key: CaptureRequest.Key<T>): T? =
    this[key] as T?

/**
 * Helper extension to perform type-safe unchecked casts from generic maps.
 *
 * This is an internal helper to retrieve a value from a map of [CaptureResult.Key] to generic
 * objects, casting the result to the type specified by the key.
 *
 * @param T The expected return type.
 * @param key The key to look up.
 * @return The casted value, or `null` if the key is not present in the map.
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CaptureResult.Key<*>, *>.getUnchecked(key: CaptureResult.Key<T>): T? =
    this[key] as T?
