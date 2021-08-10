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

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.macro.userspaceTrace
import org.jetbrains.annotations.TestOnly
import java.io.File

/**
 * Enables parsing perfetto traces on-device
 */
@RequiresApi(21)
internal object PerfettoTraceProcessor {
    private const val TAG = "PerfettoTraceProcessor"

    /**
     * The actual [File] path to the `trace_processor_shell`.
     *
     * Lazily copies the `trace_processor_shell` and enables parsing of the perfetto trace files.
     */
    @get:TestOnly
    val shellPath: String by lazy {
        // Checks for ABI support
        PerfettoHelper.createExecutable("trace_processor_shell")
    }

    private fun validateTracePath(absoluteTracePath: String) {
        require(!absoluteTracePath.contains(" ")) {
            "Trace path must not contain spaces: $absoluteTracePath"
        }
    }

    fun getJsonMetrics(absoluteTracePath: String, metric: String): String {
        validateTracePath(absoluteTracePath)
        require(!metric.contains(" ")) {
            "Metric must not contain spaces: $metric"
        }

        val command = "$shellPath --run-metric $metric $absoluteTracePath --metrics-output=json"
        Log.d(TAG, "Executing command $command")

        val json = userspaceTrace("trace_processor_shell") {
            Shell.executeCommand(command)
                .trim() // trim to enable empty check below
        }
        Log.d(TAG, "Trace Processor result: \n\n $json")
        if (json.isEmpty()) {
            throw IllegalStateException(
                "Empty json result from Trace Processor - " +
                    "possibly malformed command? Command: $command"
            )
        }
        return json
    }

    data class Slice(
        val name: String,
        val ts: Long,
        val dur: Long
    )

    private fun String.unquote(): String {
        require(this.first() == '"' && this.last() == '"')
        return this.substring(1, length - 1)
    }

    /**
     * Query a trace for a list of slices - name, timestamp, and duration.
     */
    fun querySlices(
        absoluteTracePath: String,
        vararg sliceNames: String
    ): List<Slice> {
        val whereClause = sliceNames
            .joinToString(separator = " OR ") {
                "slice.name = '$it'"
            }

        val queryResult = rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = """
                SELECT slice.name,ts,dur
                FROM slice
                JOIN thread_track ON thread_track.id = slice.track_id
                WHERE $whereClause
            """.trimMargin()
        )
        val resultLines = queryResult.split("\n")

        if (resultLines.first() != "\"name\",\"ts\",\"dur\"") {
            throw IllegalStateException("query failed!")
        }

        // results are in CSV with a header row, and strings wrapped with quotes
        return resultLines
            .filter { it.isNotBlank() } // drop blank lines
            .drop(1) // drop the header row
            .map {
                val columns = it.split(",")
                Slice(
                    name = columns[0].unquote(),
                    ts = columns[1].toLong(),
                    dur = columns[2].toLong()
                )
            }
    }

    private fun rawQuery(
        absoluteTracePath: String,
        query: String
    ): String {
        validateTracePath(absoluteTracePath)

        val queryFile = File(Outputs.dirUsableByAppAndShell, "trace_processor_query.sql")
        try {
            queryFile.writeText(query)

            val command = "$shellPath --query-file ${queryFile.absolutePath} $absoluteTracePath"
            return userspaceTrace("trace_processor_shell") {
                Shell.executeCommand(command)
            }
        } finally {
            queryFile.delete()
        }
    }
}
