/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle.xcode

import java.io.File

/**
 * Parses benchmark results from the xcresult file.
 *
 * @param xcResultFile The XCResult output file to parse.
 * @param commandExecutor An executor that can invoke the `xcrun` and get the results from `stdout`.
 */
class XcResultParser(
    private val xcResultFile: File,
    private val commandExecutor: (args: List<String>) -> String
) {
    fun parseResults(): Pair<ActionsInvocationRecord, List<ActionTestSummary>> {
        val json = commandExecutor(xcRunCommand())
        val gson = GsonHelpers.gson()
        val record = gson.fromJson(json, ActionsInvocationRecord::class.java)
        val summaries =
            record.actions.testReferences().flatMap { testRef ->
                val summary = commandExecutor(xcRunCommand(testRef))
                val testPlanSummaries =
                    gson.fromJson(summary, ActionTestPlanRunSummaries::class.java)
                testPlanSummaries.testSummaries().map { summaryMeta ->
                    val output = commandExecutor(xcRunCommand(summaryMeta.summaryRefId()))
                    val testSummary = gson.fromJson(output, ActionTestSummary::class.java)
                    testSummary
                }
            }
        return record to summaries
    }

    /**
     * Builds an `xcrun` command that can parse both the xcresult scaffold, and additionally
     * traverse nested `plist`s to pull additional benchmark result metadata.
     */
    private fun xcRunCommand(id: String? = null): List<String> {
        val args =
            mutableListOf(
                "xcrun",
                "xcresulttool",
                "get",
                "--path",
                xcResultFile.absolutePath,
                "--format",
                "json"
            )

        if (!id.isNullOrEmpty()) {
            args += listOf("--id", id)
        }

        return args
    }
}
