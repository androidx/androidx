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

package androidx.datastore.core

/**
 * Storage provides a way to create StorageConnections that allow read and write a particular type
 * <T> of data.  Storage is used to construct DataStore objects, and encapsulates all the specifics
 * of the data format and persistence.
 *
 * Implementers provide the specifics of how and where the data is stored.
 */
interface Storage<T> {
    /**
     * Creates a storage connection which allows reading and writing to the underlying storage.
     *
     * Should be closed after usage.
     *
     * @throws IOException Unrecoverable IO exception when trying to access the underlying storage.
     */
    fun createConnection(): StorageConnection<T>
}
