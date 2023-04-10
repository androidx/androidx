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
        const val version = "1.0.0-alpha15"
        val checksums = mapOf(
            "arm64-v8a" to "4cbe159889b2a9568f779cad114e4ecd1173bb34e503b41727d43d860d0b3313",
            "armeabi-v7a" to "747e162cf39be138e2335f8e6c8babbddededbdb2cc40370e5d287f1f091ab4a",
            "x86" to "d26b38905b6182a6d7b8583fcebacbe74374da5242a05bca80263a06d52718db",
            "x86_64" to "7804c3ab0a5bf42d28dfb7276b087b92d1b81ce2413cc51dda80571e63873554",
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
