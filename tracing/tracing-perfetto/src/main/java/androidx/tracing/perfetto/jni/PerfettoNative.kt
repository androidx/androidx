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
        const val version = "1.0.0-alpha03"
        val checksums = mapOf(
            "arm64-v8a" to "388c9770a0a1690bac0e79e8d90c109b20ad46aa0bdb7ee6d8a7b87cf914474a",
            "armeabi-v7a" to "062f5b16ab1ae5ff1498d84ceeb6a6b8f1efa98b49350de2c1ff2abb53a7e954",
            "x86" to "0aeec233848db85805387d37f432db67520cb9f094997df66b0cb82f10d188b6",
            "x86_64" to "c416fab9db04d323d46fdcfc228fb74b26ab308d91afa3a7c1ee4ed2aa44b7df",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
