/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.userspaceTrace
import java.io.File

/**
 * Enables capturing a Perfetto trace
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class PerfettoCapture(
    /**
     * Bundled is available above API 28, but we default to using unbundled as well on API 29, as
     * ProcessStatsConfig.scan_all_processes_on_start isn't supported on the bundled version.
     */
    unbundled: Boolean = Build.VERSION.SDK_INT in 21..29
) {

    private val helper: PerfettoHelper = PerfettoHelper(unbundled)

    public fun isRunning() = helper.isRunning()

    /**
     * Start collecting perfetto trace.
     *
     * TODO: provide configuration options
     */
    public fun start(packages: List<String>) = userspaceTrace("start perfetto") {
        // Write binary proto to dir that shell can read
        // TODO: cache on disk
        val configProtoFile = File(Outputs.dirUsableByAppAndShell, "trace_config.pb")
        try {
            userspaceTrace("write config") {
                val atraceApps = if (Build.VERSION.SDK_INT <= 28 || packages.isEmpty()) {
                    packages
                } else {
                    listOf("*")
                }
                configProtoFile.writeBytes(perfettoConfig(atraceApps).validateAndEncode())
            }
            userspaceTrace("start perfetto process") {
                helper.startCollecting(configProtoFile.absolutePath, false)
            }
        } finally {
            configProtoFile.delete()
        }
    }

    /**
     * Stop collection, and record trace to the specified file path.
     *
     * @param destinationPath Absolute path to write perfetto trace to. Must be shell-writable,
     * such as result of `context.getExternalFilesDir(null)` or other similar `external` paths.
     */
    public fun stop(destinationPath: String) = userspaceTrace("stop perfetto") {
        helper.stopCollecting(destinationPath)
    }
}
