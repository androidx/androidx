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
import android.util.Log
import androidx.startup.Initializer
import androidx.tracing.perfetto.internal.handshake.protocol.Response
import java.io.File

/** Enables tracing at app startup if a [StartupTracingConfig] is present. */
class StartupTracingInitializer : Initializer<Unit> {
    private companion object {
        private val TAG = StartupTracingInitializer::class.java.name
    }

    override fun create(context: Context) {
        // TODO(234351579): Support API < 30
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        // read startup tracing config file if present
        val packageName = context.applicationInfo.packageName
        val config = StartupTracingConfigStore.load(packageName)
            ?: return // early exit if no config is found

        // delete config file if not meant to be preserved between runs
        if (!config.isPersistent) StartupTracingConfigStore.clear(packageName)

        // enable tracing
        val libFilePath = config.libFilePath
        val enableTracingResponse =
            if (libFilePath == null) Trace.enable()
            else Trace.enable(File(libFilePath), context)

        // log the result for debuggability
        Log.d(TAG, "${Response::class.java.name}: { " +
            "resultCode: ${enableTracingResponse.resultCode}, " +
            "message: ${enableTracingResponse.message}, " +
            "requiredVersion: ${enableTracingResponse.requiredVersion} " +
            "}")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
