/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.testapp.twoWayIpc

//noinspection BanConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * A data container that provides a place to stash values with keys and get them back.
 *
 * @see IpcAction
 * @see TwoWayIpcSubject
 */
internal class CompositeServiceSubjectModel {
    private val data = ConcurrentHashMap<Key<*>, Any?>()

    open class Key<T>

    @Suppress("UNCHECKED_CAST") operator fun <T> get(key: Key<T>) = data[key] as T

    fun <T> contains(key: Key<T>) = data.containsKey(key)

    operator fun <T> set(key: Key<T>, value: T?) {
        data[key] = value
    }

    /** Gets the value with [key] and atomically creates it if [key] is not set. */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrPut(key: Key<T>, create: () -> T): T {
        data[key]?.let {
            return it as T
        }
        synchronized(this) {
            data[key]?.let {
                return it as T
            }
            return create().also { data[key] = it }
        }
    }
}
