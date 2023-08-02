/*
 * Copyright 2022 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.datastore.multiprocess

import androidx.annotation.RestrictTo
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler

/**
 * CorruptionHandlers allow recovery from corruption that prevents reading data from the file (as
 * indicated by a CorruptionException).
 *
 * This is a duplicate of {@link androidx.datastore.core.CorruptionHandler}.
 */
internal interface CorruptionHandler<T> {
    /**
     * This function will be called by DataStore when it encounters corruption. If the
     * implementation of this function throws an exception, it will be propagated to the original
     * call to DataStore. Otherwise, the returned data will be written to disk.
     *
     * This function should not interact with any DataStore API - doing so can result in a deadlock.
     *
     * @param ex is the exception encountered when attempting to deserialize data from disk.
     * @return The value that DataStore should attempt to write to disk.
     **/
    public suspend fun handleCorruption(ex: CorruptionException): T
}

/**
 * Create a `CorruptionHandler` instance to adapt the `ReplaceFileCorruptionHandler` instance. As
 * MPDSFactory takes in `ReplaceFileCorruptionHandler` in public API, it needs to be adapted to the
 * local definition of `CorruptionHandler` as it is a duplicated class.
 */
// TODO(b/242906637): remove this method when interface definition is deduped
internal fun <T> adapterCorruptionHandlerOrNull(
    handler: ReplaceFileCorruptionHandler<T>?
): CorruptionHandler<T>? {
    return if (handler != null) object : CorruptionHandler<T> {
        override suspend fun handleCorruption(ex: CorruptionException): T {
            return handler.handleCorruption(ex)
        }
    } else null
}