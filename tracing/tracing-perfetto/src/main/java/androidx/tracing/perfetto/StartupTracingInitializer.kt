/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tracing.perfetto

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.startup.Initializer
import androidx.tracing.perfetto.internal.handshake.protocol.Response
import java.io.File

/** Enables tracing at app startup if configured prior to app starting */
class StartupTracingInitializer : Initializer<Unit> {
    private companion object {
        private val TAG = StartupTracingInitializer::class.java.name
    }

    override fun create(context: Context) {
        // TODO(234351579): Support API < 30
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        suppressStrictModeDiskWrites {
            // read startup tracing config if present
            val config = StartupTracingConfigStore.load(context)
                ?: return // early exit if no config is found

            // delete config if not meant to be preserved between runs
            if (!config.isPersistent) StartupTracingConfigStore.clear(context)

            // enable tracing
            val libFilePath = config.libFilePath
            val enableTracingResponse =
                if (libFilePath == null) PerfettoSdkTrace.enable()
                else PerfettoSdkTrace.enable(File(libFilePath), context)

            // log the result for debuggability
            Log.d(TAG, "${Response::class.java.name}: { " +
                "resultCode: ${enableTracingResponse.resultCode}, " +
                "message: ${enableTracingResponse.message}, " +
                "requiredVersion: ${enableTracingResponse.requiredVersion} " +
                "}")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    // TODO(245426369): test in TrivialStartupTracingBenchmark
    private inline fun <R> suppressStrictModeDiskWrites(block: () -> R): R {
        val oldPolicy = StrictMode.allowThreadDiskWrites()
        try {
            return block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }
}
