/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components.text

import kotlin.js.Promise
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response


actual suspend fun loadResource(file: String): ByteArray? {
    return fetch(file).arrayBuffer().await().asByteArray()
}

private suspend fun fetch(url: String): Response =
    windowFetch(url).await()

@Suppress("CAST_NEVER_SUCCEEDS")
fun ArrayBuffer?.asByteArray(): ByteArray? = this?.run { Int8Array(this) as ByteArray }

private fun windowFetch(url: String) = js("window.fetch(url)").unsafeCast<Promise<Response>>()
