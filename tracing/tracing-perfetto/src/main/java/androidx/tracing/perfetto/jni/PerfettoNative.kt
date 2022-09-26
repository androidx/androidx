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
        const val version = "1.0.0-alpha05"
        val checksums = mapOf(
            "arm64-v8a" to "86fbcded1a071253e6b1ec8ac820b3f5f8c47a727beb9eb10f73b6ac0fbdfa7d",
            "armeabi-v7a" to "0ec22f0516b0c46a6edd2b7e3f1bbae25e28874780ab2d881a188c9f56e11f5a",
            "x86" to "f360e949c9b6659318ca010fda67bf35608f596d20430724941e444e25ba7097",
            "x86_64" to "219cc54c2fda8f777b71809910c1c0fce4aeb8e0ccd3dc8861fb7afa1dc5f9aa",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
