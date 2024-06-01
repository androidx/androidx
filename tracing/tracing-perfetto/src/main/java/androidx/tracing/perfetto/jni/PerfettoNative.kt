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
        const val version = "1.0.0"
        val checksums =
            mapOf(
                "arm64-v8a" to "a152fbd7ebaa109a9c3cf6bbb6d585aa0df08f97ae022b2090b1096a8f5e2665",
                "armeabi-v7a" to "b2821c9ddb77a3f070cce42be7cd3255d7ec92c868d7d518a99ed968d9018b9f",
                "x86" to "4cefdc75fe41deeeb2306891c25ce4db33599698cc6fcb2e82caad5aece9aa09",
                "x86_64" to "23daf0750238cf96bf9ea9fa1b13ae1d2eeb17644ea5439e18939ec6a8b9e5be",
            )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    @JvmStatic external fun nativeRegisterWithPerfetto()

    @FastNative @JvmStatic external fun nativeTraceEventBegin(key: Int, traceInfo: String)

    @CriticalNative @JvmStatic external fun nativeTraceEventEnd()

    @JvmStatic external fun nativeVersion(): String
}
