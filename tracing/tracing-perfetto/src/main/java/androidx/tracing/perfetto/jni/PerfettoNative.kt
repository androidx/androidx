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
        const val version = "1.0.0-alpha13"
        val checksums = mapOf(
            "arm64-v8a" to "3513a43f87bb7b455a626fbbe3e0f2b65585ad929d81825ce7126fcfe250a6bb",
            "armeabi-v7a" to "de02d5fcc0f2cfacb2376f18836326886f23cd3df7359d9e0edae93bd6e3e7c8",
            "x86" to "4ae9b924dc06ac16b0cc591b7bd61197f51efcbfebc0909fdf78778fef1f715c",
            "x86_64" to "793b66df2d387626f005e15a4ab564acb6e32cd33dc17ec58e46e4fef05326c2",
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
