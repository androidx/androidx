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

import android.util.Log
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import java.io.File
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to enable linking files and traces to Studio UI for macrobench correctness tests.
 *
 * File paths are registered, and reported, but files are not created by this class, that should be
 * handled by the test. Ensure you don't clean up the file - it needs to persist to be copied over
 * by Studio.
 */
class FileLinkingRule : TestRule {
    private lateinit var currentDescription: Description
    private var summaryString = ""

    private fun createReportedFilePath(
        label: String,
        @Suppress("SameParameterValue") extension: String,
    ): String {
        // remove parens / brackets, as it confuses linking
        val methodLabel =
            currentDescription
                .toUniqueName()
                .replace("(", "_")
                .replace(")", "_")
                .replace("[", "_")
                .replace("]", "_")

        val file =
            File(Outputs.dirUsableByAppAndShell, "${label}_${Outputs.dateToFileName()}.$extension")
        val absolutePath: String = file.absolutePath
        val relativePath = Outputs.relativePathFor(absolutePath)

        summaryString += "$methodLabel [$label](file://$relativePath)\n"
        return absolutePath
    }

    fun createReportedTracePath(label: String = "trace"): String {
        return createReportedFilePath(label, "perfetto-trace")
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(::applyInternal).apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) =
        object : Statement() {
            override fun evaluate() {
                currentDescription = description
                try {
                    base.evaluate()
                } finally {
                    flush()
                }
            }
        }

    private fun flush() {
        if (Outputs.outputDirectory == Outputs.dirUsableByAppAndShell) {
            InstrumentationResults.instrumentationReport {
                reportSummaryToIde(message = summaryString.trim())
            }
        } else {
            Log.d(TAG, "FileLinkingRule doesn't support outputDirectory != dirUsableByAppAndShell")
        }
    }

    private fun Description.toUniqueName() = testClass.simpleName + "_" + methodName
}
