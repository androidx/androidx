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
import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import java.io.File

internal object PerfettoNative {
    private const val libraryName = "tracing_perfetto"

    // TODO(224510255): load from a file produced at build time
    object Metadata {
        const val version = "1.0.0-alpha09"
        val checksums = mapOf(
            "arm64-v8a" to "130cea2d90e941acb9d2087c0bebe8229b461175454df20763bfe901c0d73155",
            "armeabi-v7a" to "cb0e60c5d8726d274bbc678cfa3a8724e66d2bfa6874c06c5f4347e545c0439d",
            "x86" to "c6b19ce773d17c74aa74aa813637bc1d2bd61f5bcc280e6a5c030a36948dd6dd",
            "x86_64" to "af62d109588db30c8ce11f2027227664520372f8c77259d4155dd92f7cc73a6e",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    @JvmStatic
    external fun nativeRegisterWithPerfetto()

    @FastNative
    @JvmStatic
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)

    @CriticalNative
    @JvmStatic
    external fun nativeTraceEventEnd()

    @JvmStatic
    external fun nativeVersion(): String
}
