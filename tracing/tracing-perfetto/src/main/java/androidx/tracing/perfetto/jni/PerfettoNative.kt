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
            "arm64-v8a" to "3e42560024c7311c55ad09ffbb9228806303c4e42be4bc6fa8f66ea77d48999a",
            "armeabi-v7a" to "46d84ff8765f87e5673ba1fc93ba84c79f9d01509770b99f5f68210498bdd8dc",
            "x86" to "18d4aeca911080983bf05fd69bef153ed707aee431d5ccf1f55e7d1ae62904e5",
            "x86_64" to "f54fb5fdeb75ef414116d6169d1fc1ba12c034056b0b98c58690661090312d10",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
