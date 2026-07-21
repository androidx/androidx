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
 * Read-only map-like interface for accessing camera metadata.
 *
 * Use this interface to wrap native camera metadata and provide unified access to both native
 * fields and custom, internally computed, or version-compatibility properties.
 */
public interface Metadata {
    /**
     * Retrieves the value associated with the specified [Metadata.Key].
     *
     * @param key The key to query.
     * @return The value associated with the key, or `null` if not found.
     */
    public operator fun <T> get(key: Key<T>): T?

    /**
     * Retrieves the value associated with the specified [Metadata.Key], or returns [default] if not
     * found.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present.
     * @return The value associated with the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: Key<T>, default: T): T {
        return get(key) ?: default
    }

    /** Set of all custom [Metadata.Key]s available in this container. */
    public val metadataKeys: Set<Key<*>>

    /**
     * Type-safe key for identifying and retrieving values in a [Metadata] container.
     *
     * Keys are uniquely identified by their [name]. To prevent type ambiguity, only one [Key]
     * instance can exist for a given name. Attempting to create a key with an existing name but a
     * different [type] will result in an [IllegalStateException].
     *
     * ### Examples
     *
     * Creating keys for different types in Kotlin:
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
     * @param name The unique string name identifying this key.
     * @param type The [Class] representing the type of the value associated with this key.
     */
    public class Key<T> internal constructor(public val name: String, public val type: Class<T>) {
        public companion object {
            @JvmStatic private val keys: MutableMap<String, Key<*>> = HashMap()

            /**
             * Creates or retrieves a [Key] with the given [name] and inferred type [T].
             *
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
             * @param name The unique name for this key.
             * @param type The [Class] representing the type of the value associated with this key.
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

/** Helper extension to perform type-safe unchecked casts from maps. */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<Metadata.Key<*>, *>.getUnchecked(key: Metadata.Key<T>): T? = this[key] as T?

/** Helper extension to perform type-safe unchecked casts from maps. */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CameraCharacteristics.Key<*>, *>.getUnchecked(
    key: CameraCharacteristics.Key<T>
): T? = this[key] as T?

/** Helper extension to perform type-safe unchecked casts from maps. */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CaptureRequest.Key<*>, *>.getUnchecked(key: CaptureRequest.Key<T>): T? =
    this[key] as T?

/** Helper extension to perform type-safe unchecked casts from maps. */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> Map<CaptureResult.Key<*>, *>.getUnchecked(key: CaptureResult.Key<T>): T? =
    this[key] as T?
