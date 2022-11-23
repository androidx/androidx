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
        const val version = "1.0.0-alpha08"
        val checksums = mapOf(
            "arm64-v8a" to "9714252b58c4bfbb99d59debead6160ea463df5632ab6d684c64359c16aea9ce",
            "armeabi-v7a" to "cc4bbd2428f59c33a57aa7c75666bd8cca4a9e3a5addda3a5c3ffac4c6442295",
            "x86" to "930082024f1490bfbff3480d85c729dd3dd2186c2f4f2dce01d84c10776a8bdc",
            "x86_64" to "4f5f20336c50360c34d09af871ab6248908b5b7a1af3e2b561a7731745e471b2",
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
