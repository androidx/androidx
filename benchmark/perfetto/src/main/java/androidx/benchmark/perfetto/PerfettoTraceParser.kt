/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.perfetto

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Enables parsing perfetto traces on-device on Q+ devices.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
class PerfettoTraceParser {

    /**
     * The actual [File] path to the `trace_processor_shell`.
     */
    var shellFile: File? = null

    /**
     * Copies `trace_processor_shell` and enables parsing of the perfetto trace files.
     */
    fun copyTraceProcessorShell() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context: Context = instrumentation.context
        shellFile = File(context.cacheDir, "trace_processor_shell")
        // TODO: support other ABIs
        if (Build.SUPPORTED_64_BIT_ABIS.isEmpty()) {
            throw IllegalStateException("Unsupported ABI")
        }
        // Write the trace_processor_shell to the external directory so we can process
        // perfetto metrics on device.
        val shellFile = shellFile
        if (shellFile != null && !shellFile.exists()) {
            val created = shellFile.createNewFile()
            shellFile.setWritable(true)
            shellFile.setExecutable(true, false)
            if (!created) {
                throw IllegalStateException("Unable to create new file $shellFile")
            }
            shellFile.outputStream().use {
                // TODO: Copy the file based on the ABI
                context.assets.open("trace_processor_shell_aarch64").copyTo(it)
            }
        }
    }
}
