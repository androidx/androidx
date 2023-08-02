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
package androidx.tracing.perfetto.jni

import androidx.tracing.perfetto.security.SafeLibLoader
import java.io.File

internal object PerfettoNative {
    private const val libraryName = "tracing_perfetto"

    // TODO(224510255): load from a file produced at build time
    object Metadata {
        const val version = "1.0.0-alpha02"
        val checksums = mapOf(
            "arm64-v8a" to "d6187ec5bd682c48b50416c0486893cb721a816e3f28cee63bdf5e8af4e2cb36",
            "armeabi-v7a" to "ccae81fba02dcb139bd567ffd804de444d02b2435eddbd2ecfe597ce824cf3af",
            "x86" to "a796d80749ce7defc11ad9c0ad89ec783fd13fa52ce42b75ea562d0bfbb51714",
            "x86_64" to "e9064001c7267bb6a77c5f521a00aef396bb8f64e8fd78b62df23a1bba7b18ce",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
