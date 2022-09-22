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
        const val version = "1.0.0-alpha04"
        val checksums = mapOf(
            "arm64-v8a" to "3dbf1db7dfa7e4099f671255983ee7bd8fc9dfd95f4772179b791f6dd88d783a",
            "armeabi-v7a" to "4806a3e5b2cf23ea03b4a87fbcff03dfc76c4ef8eb8e7358b8ce2e20a7c0f86f",
            "x86" to "5ddb57d45bcd16325259330f7ea3f9b5803b394d10c9f57757f8ed1441507e10",
            "x86_64" to "3c50eac377e7285c5f729e674d2140f296067835ca57eb43c67813a57f681a48",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
