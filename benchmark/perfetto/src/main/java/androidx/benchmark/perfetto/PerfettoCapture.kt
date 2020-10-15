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
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RequiresApi(29)
internal class PerfettoCapture {
    private val helper = PerfettoHelper()

    fun cancel() {
        if (helper.isPerfettoRunning) {
            helper.stopPerfetto()
        }
    }

    fun start() {
        val context: Context = InstrumentationRegistry.getInstrumentation().context

        // Write textproto asset to external files dir, so it can be read by shell
        // TODO: use binary proto (which will also give us rooted 28 support)
        val configBytes = context.resources.openRawResource(R.raw.trace_config).readBytes()
        val textProtoFile = File(context.getExternalFilesDir(null), "trace_config.textproto")

        try {
            textProtoFile.writeBytes(configBytes)

            // Start tracing
            if (!helper.startCollecting(textProtoFile.absolutePath, true)) {
                // TODO: move internal failures to be exceptions
                throw IllegalStateException("Unable to read start collecting")
            }
        } finally {
            textProtoFile.delete()
        }
    }

    fun stop(destinationPath: String) {
        if (!helper.stopCollecting(50, destinationPath)) {
            // TODO: move internal failures to be exceptions
            throw IllegalStateException("Unable to store perfetto trace")
        }
    }
}