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

package androidx.datastore.core.okio

import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage

public class WebSessionStorage<T>(serializer: OkioSerializer<T>, name: String) : Storage<T> {
    private val delegateStorage = WebStorage(serializer, name, WebStorageType.SESSION)

    override fun createConnection(): StorageConnection<T> = delegateStorage.createConnection()
}

public class WebLocalStorage<T>(serializer: OkioSerializer<T>, name: String) : Storage<T> {
    private val delegateStorage = WebStorage(serializer, name, WebStorageType.LOCAL)

    override fun createConnection(): StorageConnection<T> = delegateStorage.createConnection()
}

public class WebOpfsStorage<T>(serializer: OkioSerializer<T>, private val name: String) :
    Storage<T> {
    private val delegateStorage = WebStorage(serializer, name, WebStorageType.OPFS)

    override fun createConnection(): StorageConnection<T> = delegateStorage.createConnection()
}

private class WebStorage<T>(
    private val serializer: OkioSerializer<T>,
    private val name: String,
    private val storageType: WebStorageType,
) : Storage<T> {
    private val coordinator by lazy { createWebProcessCoordinator(name, storageType) }

    override fun createConnection(): StorageConnection<T> {
        return when (storageType) {
            WebStorageType.SESSION,
            WebStorageType.LOCAL -> {
                val domStorage =
                    if (storageType == WebStorageType.SESSION) sessionStorage else localStorage
                SessionAndLocalWebStorageConnection(domStorage, name, serializer, coordinator)
            }
            WebStorageType.OPFS -> {
                OpfsWebStorageConnection(name, serializer, coordinator)
            }
        }
    }
}
