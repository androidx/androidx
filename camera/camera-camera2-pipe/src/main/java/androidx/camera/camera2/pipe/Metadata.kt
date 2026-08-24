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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import androidx.camera.common.Metadata as CommonMetadata
import kotlin.reflect.KClass

/** A compatibility interface for accessing unified camera metadata. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Metadata : CommonMetadata {

    /** A type-safe key used to identify and retrieve values from a [Metadata] container. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Key<T> private constructor(public val name: String, public val type: KClass<*>) {
        @Suppress("UNCHECKED_CAST")
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val commonKey: CommonMetadata.Key<*> by lazy {
            CommonMetadata.Key.create(name, type.java as Class<Any>)
        }

        override fun toString(): String = "$name: ${type.simpleName}"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key<*>) return false
            return name == other.name && type == other.type
        }

        override fun hashCode(): Int {
            return name.hashCode() * 31 + type.hashCode()
        }

        public companion object {
            private val keys = HashMap<String, Key<*>>()

            /** Creates a compatibility [Key] instance. */
            @JvmStatic
            public inline fun <reified T : Any> create(name: String): Key<T> =
                create(name, T::class)

            /**
             * Creates a compatibility [Key] instance, ensuring instance uniqueness for the same
             * name and type.
             */
            @JvmStatic
            @Suppress("UNCHECKED_CAST")
            public fun <T : Any> create(name: String, type: KClass<T>): Key<T> =
                synchronized(keys) {
                    val key = keys.getOrPut(name) { Key<T>(name, type) }
                    check(key.type == type) {
                        "Key '$name' already exists with a different type: ${key.type.simpleName} (requested: ${type.simpleName})"
                    }
                    key as Key<T>
                }
        }
    }

    /** Retrieves the value associated with the specified compatibility [Metadata.Key]. */
    public operator fun <T> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return get(key.commonKey as CommonMetadata.Key<Any>) as T?
    }

    /**
     * Retrieves the value associated with the specified compatibility [Metadata.Key], or returns
     * [default] if the key is not present.
     */
    public fun <T> getOrDefault(key: Key<T>, default: T): T {
        return get(key) ?: default
    }

    // Default implementations for CommonMetadata to ensure classes implementing
    // this interface in external codebases (like Google3) continue to compile
    // without needing to immediately implement these new methods.

    override fun <T : Any> get(key: CommonMetadata.Key<T>): T? {
        return null
    }

    override fun <T : Any> getOrDefault(key: CommonMetadata.Key<T>, default: T): T {
        return get(key) ?: default
    }

    override val metadataKeys: Set<CommonMetadata.Key<*>>
        get() = emptySet()
}
