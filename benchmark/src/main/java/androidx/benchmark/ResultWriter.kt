/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.os.Environment
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal object ResultWriter {
    private fun List<Long>.toJsonWithMargin(): String {
        return joinToString(",\n") { "|            $it" }
    }

    private fun BenchmarkState.Report.toJson(): String {
        return "\n" + """
        |    {
        |        "name": "$testName",
        |        "className": "$className",
        |        "nanos": $nanos,
        |        "warmupIterations": $warmupIterations,
        |        "repeatIterations": $repeatIterations,
        |        "runs": [
        ${data.toJsonWithMargin()}
        |        ]
        |    }
    """.trimMargin()
    }

    data class FileManager(
        val extension: String,
        val initial: String,
        val tail: String,
        val separator: String? = null,
        val reportFormatter: (BenchmarkState.Report) -> String
    ) {
        private val packageName =
            InstrumentationRegistry.getInstrumentation().targetContext!!.packageName

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$packageName-benchmarkData.$extension"
        )
        var currentContent = initial
        var lastAddedEntry: BenchmarkState.Report? = null

        val fullFileContent: String
            get() = currentContent + tail

        fun append(report: BenchmarkState.Report) {
            if (currentContent != initial && separator != null) {
                currentContent += separator
            }
            lastAddedEntry = report
            currentContent += reportFormatter(report)
        }
    }

    @VisibleForTesting
    val fileManager = FileManager(
        extension = "json",
        initial = "{ \"results\": [",
        tail = "\n]}",
        separator = ",",
        reportFormatter = { report -> report.toJson() }
    )

    fun appendStats(report: BenchmarkState.Report) {
        fileManager.append(report)
        fileManager.file.run {
            if (!exists()) {
                parentFile.mkdirs()
                createNewFile()
            }

            // Currently, we just overwrite the whole file
            // Ideally, truncate off the 'tail', and append for efficiency
            writeText(fileManager.fullFileContent)
        }
    }
}
