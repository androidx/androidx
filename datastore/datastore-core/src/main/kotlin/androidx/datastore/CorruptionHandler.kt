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

package androidx.datastore

/**
 * CorruptionHandlers allow recovery from corruption that prevents reading data from the file (as
 * indicated by a CorruptionException).
 */
interface CorruptionHandler<T> {
    /**
     * This function should not interact with any DataStore API. This function will be called by
     * DataStore when it encounters corruption. If an exception is thrown, it will be propagated
     * to the original call to DataStore. Otherwise, the returned data will be written to disk.
     *
     * @param ex is the exception encountered when attempting to deserialize data from disk.
     * @return The value that DataStore should attempt to write to disk.
     **/
    suspend fun handleCorruption(ex: CorruptionException): T
}