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

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.use
import kotlinx.browser.sessionStorage
import okio.Buffer
import org.w3c.dom.Storage as DomStorage

// TODO(b/441511612): Support LocalStorage and OPFS.
public enum class WebStorageType {
    SESSION
}

public class WebStorage<T>(
    private val serializer: OkioSerializer<T>,
    private val name: String,
    private val storageType: WebStorageType,
    private val coordinatorProducer: (String, WebStorageType) -> InterProcessCoordinator =
        { name, storageType ->
            createWebProcessCoordinator(name, storageType)
        },
) : Storage<T> {
    override fun createConnection(): StorageConnection<T> {
        val domStorage =
            when (storageType) {
                WebStorageType.SESSION -> sessionStorage
            }
        val coordinator = coordinatorProducer(name, storageType)
        return WebStorageConnection(domStorage, name, serializer, coordinator)
    }
}

internal class WebStorageConnection<T>(
    private val domStorage: DomStorage,
    private val name: String,
    private val serializer: OkioSerializer<T>,
    override val coordinator: InterProcessCoordinator,
) : StorageConnection<T> {

    private val closed = AtomicBoolean(false)

    override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R {
        checkNotClosed()
        return coordinator.tryLock { locked ->
            WebReadScope(domStorage, name, serializer).use { block(it, locked) }
        }
    }

    override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
        checkNotClosed()
        coordinator.lock { WebWriteScope(domStorage, name, serializer).use { block(it) } }
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "StorageConnection has already been disposed." }
    }

    override fun close() {
        closed.set(true)
    }
}

internal open class WebReadScope<T>(
    private val domStorage: DomStorage,
    private val name: String,
    private val serializer: OkioSerializer<T>,
) : ReadScope<T> {
    private val closed = AtomicBoolean(false)

    protected fun checkClose() {
        check(!closed.get()) { "This scope has already been closed." }
    }

    override suspend fun readData(): T {
        checkClose()
        val stringData = domStorage.getItem(name)
        if (stringData.isNullOrEmpty()) {
            return serializer.defaultValue
        }

        val buffer = Buffer().writeUtf8(stringData)
        try {
            return serializer.readFrom(buffer)
        } catch (ex: Exception) {
            if (ex is CorruptionException) throw ex
            throw CorruptionException("Unable to deserialize stored data.", ex)
        }
    }

    override fun close() {
        closed.set(true)
    }
}

internal class WebWriteScope<T>(
    private val domStorage: DomStorage,
    private val name: String,
    private val serializer: OkioSerializer<T>,
) : WebReadScope<T>(domStorage, name, serializer), WriteScope<T> {

    override suspend fun writeData(value: T) {
        checkClose()
        val buffer = Buffer()
        serializer.writeTo(value, buffer)
        val stringData = buffer.readUtf8()
        domStorage.setItem(name, stringData)
    }
}
