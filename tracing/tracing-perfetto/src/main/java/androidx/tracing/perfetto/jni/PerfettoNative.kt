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
        const val version = "1.0.0-beta03"
        val checksums = mapOf(
            "arm64-v8a" to "e11502d6fa0c949774a792c2406744a1fff112ba26e6af19a6722dd55a6061ca",
            "armeabi-v7a" to "cd286085893cc7760b658f48b436fd317493159dcbab680667c0bf01d25ffb04",
            "x86" to "2679c351d40e405e46dec58c8f63570eb29196cd9e67404cf66abe74bc933a88",
            "x86_64" to "2971a9135cd69feb2a4e3c7b42f6b52469421713331cdb7a841c1c8cc707b637",
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
