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

package androidx.datastore.core.okio

import androidx.datastore.core.ReadScope
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope

internal expect class OpfsWebStorageConnection<T>(
    name: String,
    serializer: OkioSerializer<T>,
    coordinator: WebInterProcessCoordinator,
) : StorageConnection<T> {
    override val coordinator: WebInterProcessCoordinator

    override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R

    override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit)

    override fun close()
}

internal expect open class OpfsWebReadScope<T> : ReadScope<T> {
    override suspend fun readData(): T

    override fun close()
}

internal expect class OpfsWebWriteScope<T> : OpfsWebReadScope<T>, WriteScope<T> {
    override suspend fun writeData(value: T)
}

internal expect fun org.khronos.webgl.ArrayBuffer.asByteArray(): ByteArray

internal expect fun okio.Buffer.toInt8Array(): org.khronos.webgl.Int8Array

internal fun Throwable.isFileNotFound(): Boolean {
    return message?.contains("NotFoundError") == true ||
        message?.contains("A requested file or directory could not be found") == true
}
