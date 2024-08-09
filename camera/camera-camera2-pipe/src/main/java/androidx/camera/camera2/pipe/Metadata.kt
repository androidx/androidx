/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * A map-like interface used to describe or interact with metadata from CameraPipe and Camera2.
 *
 * These interfaces are designed to wrap native camera2 metadata objects in a way that allows
 * additional values to be passed back internally computed values, state, or control values.
 *
 * These interfaces are read-only.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Metadata {
    public operator fun <T> get(key: Key<T>): T?

    public fun <T> getOrDefault(key: Key<T>, default: T): T

    /** Metadata keys provide values or controls that are provided or computed by CameraPipe. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Key<T> private constructor(private val name: String) {
        public companion object {
            @JvmStatic internal val keys: MutableSet<String> = HashSet()

            /**
             * This will create a new Key instance, and will check to see that the key has not been
             * previously created somewhere else.
             */
            public fun <T> create(name: String): Key<T> {
                synchronized(keys) { check(keys.add(name)) { "$name is already defined!" } }
                return Key(name)
            }
        }

        override fun toString(): String {
            return "Metadata.Key($name)"
        }
    }
}
