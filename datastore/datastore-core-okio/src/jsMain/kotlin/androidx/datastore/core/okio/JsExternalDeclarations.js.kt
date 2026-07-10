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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.datastore.core.okio

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.definedExternally
import kotlin.js.js

/** W3C Declarations */

// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryHandle
internal external class JsFileSystemDirectoryHandle : JsAny {
    fun getFileHandle(
        name: String,
        options: FileSystemHandleOptions = definedExternally,
    ): Promise<JsFileSystemFileHandle>

    fun removeEntry(name: String): Promise<JsAny?>
}

// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemFileHandle
internal external class JsFileSystemFileHandle : JsAny {
    fun createWritable(): Promise<JsFileSystemWritableFileStream>

    fun getFile(): Promise<org.w3c.files.File>
}

// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemWritableFileStream
internal external class JsFileSystemWritableFileStream : JsAny {
    fun write(data: JsAny?): Promise<JsAny?>

    fun close(): Promise<JsAny?>
}

internal fun getJsOpfsRoot(): Promise<JsFileSystemDirectoryHandle> =
    js("window.navigator.storage.getDirectory()")

// https://developer.mozilla.org/en-US/docs/Web/API/Web_Locks_API
internal external interface JsLockManager : JsAny {
    fun request(name: String, callback: (Lock?) -> Promise<JsAny?>): Promise<JsAny?>

    fun request(
        name: String,
        options: LockManagerOptions,
        callback: (Lock?) -> Promise<JsAny?>,
    ): Promise<JsAny?>
}

internal fun getJsNavigator(): JsNavigator = js("navigator")

internal external interface JsNavigator : JsAny {
    val locks: JsLockManager
}
