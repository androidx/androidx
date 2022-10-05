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
        const val version = "1.0.0-alpha06"
        val checksums = mapOf(
            "arm64-v8a" to "c8a60a491ef1381c2c95e6281401251dbc9c128a402506bb93681d11cb01e2f7",
            "armeabi-v7a" to "1bdd59a655c574d561087389863d110b4193458f6baf7d539847d37516caf477",
            "x86" to "889c130b1a028cabf288af1a8f94bc430edf06bb740854ac43073b3db7e59927",
            "x86_64" to "2757be9d9b44b59b2c69c0afad68b0e6871e11a53f453bb777911585e0e84086",
        )
    }

    fun loadLib() = System.loadLibrary(libraryName)

    fun loadLib(file: File, loader: SafeLibLoader) = loader.loadLib(file, Metadata.checksums)

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeVersion(): String
}
