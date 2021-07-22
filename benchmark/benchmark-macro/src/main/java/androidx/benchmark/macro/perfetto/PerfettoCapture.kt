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

package androidx.benchmark.macro.perfetto

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import java.io.File

/**
 * Enables capturing a Perfetto trace from a test on Q+ devices.
 *
 * It's possible to support API 28, but there are a few issues to resolve:
 * - Use binary config protos
 * - May need to distribute perfetto binary, with atrace workaround
 * - App tags are not available, due to lack of `<profileable shell=true>`. Can potentially hack
 * around this for individual tags within test infra as needed.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class PerfettoCapture(private val unbundled: Boolean = Build.VERSION.SDK_INT in 21..28) {

    private val helper: PerfettoHelper = PerfettoHelper(unbundled)

    /**
     * Kill perfetto process, if it is running.
     */
    public fun cancel() {
        if (helper.isPerfettoRunning()) {
            helper.stopPerfetto()
        }
    }

    /**
     * Start collecting perfetto trace.
     *
     * TODO: provide configuration options
     */
    public fun start() {
        // Write binary proto to dir that shell can read
        // TODO: cache on disk
        val configProtoFile = File(Outputs.dirUsableByAppAndShell, "trace_config.pb")
        try {
            configProtoFile.writeBytes(PERFETTO_CONFIG.encode())
            helper.startCollecting(configProtoFile.absolutePath, false)
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
    public fun stop(destinationPath: String) {
        // Wait time determined empirically by running a trivial startup test (3 iterations) 200
        // times, and validating no metric capture failures.
        if (!helper.stopCollecting(500, destinationPath)) {
            // TODO: move internal failures to be exceptions
            throw IllegalStateException("Unable to store perfetto trace in $destinationPath")
        }
    }
}
