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
        const val version = "1.0.1"
        val checksums =
            mapOf(
                "arm64-v8a" to "56f3ee5ac2acffb4da14a9656e2793fe38eed6d2a50c67954b09972572caa2b7",
                "armeabi-v7a" to "cd42550bfb36dfa24299a837e7ad6c15a6cf7535168ef8f3bdaf2ba1a25918c2",
                "x86" to "25c4555795c81f66e1868fe099525c3e45932db3de44fbda9237cd89b0921d10",
                "x86_64" to "eca114f8769316a288496646cf7ea4afbd1a61b38bf8fec5f3a4b27c6f13dfe7",
            )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    @JvmStatic external fun nativeRegisterWithPerfetto()

    @FastNative @JvmStatic external fun nativeTraceEventBegin(key: Int, traceInfo: String)

    @CriticalNative @JvmStatic external fun nativeTraceEventEnd()

    @JvmStatic external fun nativeVersion(): String
}
