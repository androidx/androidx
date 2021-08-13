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

package androidx.benchmark.macro

import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * Rule to enable linking files and traces to Studio UI for macrobench correctness tests.
 *
 * Filepaths are registered, and reported, but files are not created by this class, that should
 * be handled by the test. Ensure you don't clean up the file - it needs to persist to be copied
 * over by Studio.
 */
class FileLinkingRule : TestRule {
    private lateinit var currentDescription: Description
    private var summaryString = ""

    private fun createReportedFilePath(
        label: String,
        @Suppress("SameParameterValue") extension: String,
    ): String {
        // remove parens / brackets, as it confuses linking
        val methodLabel = currentDescription.toUniqueName()
            .replace("(", "_")
            .replace(")", "_")
            .replace("[", "_")
            .replace("]", "_")

        val file = File(
            Outputs.dirUsableByAppAndShell,
            "${label}_${Outputs.dateToFileName()}.$extension"
        )
        val absolutePath: String = file.absolutePath
        val relativePath = Outputs.relativePathFor(absolutePath)

        summaryString += "$methodLabel [$label](file://$relativePath)\n"
        return absolutePath
    }

    /**
     * Map of trace abs path -> process to highlight.
     *
     * After trace is complete (at end of test), we write a UI state packet to it, so trace UI
     * can highlight/select the relevant process.
     */
    private val traceToPackageMap = mutableMapOf<String, String>()

    fun createReportedTracePath(
        packageName: String,
        label: String = "trace"
    ): String {
        val absolutePath = createReportedFilePath(label, "perfetto-trace")
        traceToPackageMap[absolutePath] = packageName
        return absolutePath
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(::applyInternal)
            .apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            if (Outputs.outputDirectory != Outputs.dirUsableByAppAndShell) {
                summaryString = "Warning: FileLinkingRule won't work when outputDirectory != " +
                    "dirUsableByAppAndShell" + summaryString
            }

            currentDescription = description
            try {
                base.evaluate()
            } finally {
                flush()
            }
        }
    }

    private fun flush() {
        traceToPackageMap.forEach { entry ->
            File(entry.key).apply {
                if (exists()) {
                    appendUiState(
                        UiState(null, null, entry.value)
                    )
                }
            }
        }

        InstrumentationResults.instrumentationReport {
            ideSummaryRecord(
                summaryV1 = "", // not supported
                summaryV2 = summaryString.trim()
            )
        }
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
