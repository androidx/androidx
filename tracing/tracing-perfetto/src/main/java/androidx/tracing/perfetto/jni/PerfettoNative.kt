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
        const val version = "1.0.0-alpha01"
        val checksums = mapOf(
            "arm64-v8a" to "d4ca3ebe077dedee07e8875462fbd75e4cfde135975644ab6bf83a2de9135754",
            "armeabi-v7a" to "4912046f4055c4132efebd39ea429b4ebceeb191178dfa5337739a022167ecfa",
            "x86" to "10d6f0fe8f7cbe2b4308946df16d33a00dffbca4934509aa6b6fcb86cabde6d0",
            "x86_64" to "8774ff744b875db95cc8d10a6e8e68a822ca85e2d74b9f356ceddf5e4168c60a"
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeFlushEvents()
    external fun nativeVersion(): String
}