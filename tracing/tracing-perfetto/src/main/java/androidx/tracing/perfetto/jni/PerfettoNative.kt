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

internal object PerfettoNative {
    private const val libraryName = "tracing_perfetto"

    // TODO: load from a file produced at build time
    object Metadata {
        const val version = "1.0.0-alpha01"
        // TODO: add SHA / signature to verify binaries before loading
    }

    fun loadLib(path: String? = null) {
        when (path) {
            null -> System.loadLibrary(libraryName)
            else -> System.load(path) // TODO: security
        }
    }

    external fun nativeRegisterWithPerfetto()
    external fun nativeTraceEventBegin(key: Int, traceInfo: String)
    external fun nativeTraceEventEnd()
    external fun nativeFlushEvents()
    external fun nativeVersion(): String
}
