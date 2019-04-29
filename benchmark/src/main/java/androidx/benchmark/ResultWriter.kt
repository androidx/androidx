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
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal object ResultWriter {
    private fun List<Long>.toXmlWithMargin(): String {
        return joinToString("\n") { "|        <run nanos=\"$it\"/>" }
    }

    private fun BenchmarkState.Report.toXml(name: String, className: String): String {
        return "\n" + """
        |    <testcase
        |            name="$name"
        |            classname="$className"
        |            nanos="$nanos"
        |            warmupIterations="$warmupIterations"
        |            repeatIterations="$repeatIterations">
        ${data.toXmlWithMargin()}
        |    </testcase>
    """.trimMargin()
    }

    private fun List<Long>.toJsonWithMargin(): String {
        return joinToString(",\n") { "|            $it" }
    }

    private fun BenchmarkState.Report.toJson(name: String, className: String): String {
        return "\n" + """
        |    {
        |        "name": "$name",
        |        "classname": "$className",
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
        val reportFormatter: (BenchmarkState.Report, String, String) -> String
    ) {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext!!

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${context.packageName}-benchmarkData.$extension"
        )
        var currentContent = initial

        val fullFileContent: String
            get() = currentContent + tail

        fun append(report: BenchmarkState.Report, name: String, className: String) {
            if (currentContent != initial && separator != null) {
                currentContent += separator
            }
            currentContent += reportFormatter(report, name, className)
        }
    }

    val fileManagers = listOf(
        FileManager(
            extension = "xml",
            initial = "<benchmarksuite>",
            tail = "\n</benchmarksuite>",
            reportFormatter = { report, name, className ->
                report.toXml(name, className)
            }
        ),
        FileManager(
            extension = "json",
            initial = "{ \"results\": [",
            tail = "\n]}",
            separator = ",",
            reportFormatter = { report, name, className ->
                report.toJson(name, className)
            }
        )
    )

    fun appendStats(name: String, className: String, report: BenchmarkState.Report) {
        for (fileManager in fileManagers) {
            fileManager.append(report, WarningState.WARNING_PREFIX + name, className)
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
}
