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
        const val version = "1.0.0-alpha07"
        val checksums = mapOf(
            "arm64-v8a" to "abb5fe78f243999cab6fc8ea05e6d0743df3582e7ea4782f5ef2a677d8a0bde7",
            "armeabi-v7a" to "d40c8487f7bca0917dcd6c0626d179394ef4b28d6a2fed809bc34e8ea15a100e",
            "x86" to "38e8e5872e2916b993ca5d37894d7973934730bc427af20de4a52f0656a9ce0f",
            "x86_64" to "ef545e11ceb45bf0eb611d12867bf9b9c5c9361d7e1865f8723a7505d3107ab8",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
