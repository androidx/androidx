/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.benchmark.InsightSummary
import androidx.benchmark.createInsightSummaries
import androidx.benchmark.traceprocessor.Insight
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.StartupInsights
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.runSingleSessionServer
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartupInsightsTest {
    private val api35ColdStart =
        createTempFileFromAsset(prefix = "api35_startup_cold_classinit", suffix = ".perfetto-trace")
            .absolutePath

    private val targetPackage = "androidx.compose.integration.hero.macrobenchmark.target"

    val insights = StartupInsights(helpUrlBase = "https://d.android.com/test#")

    // TODO (b/377581661) take time ranges from SlowStartReason
    private fun startupTraceLinkOf(title: String, reasonId: String) =
        PerfettoTrace.Link(
            title = title,
            path = "/fake/output/relative/path.perfetto-trace",
            urlParamMap =
                mapOf(
                    "AndroidStartup:packageName" to targetPackage,
                    "AndroidStartup:slowStartReasonId" to reasonId
                )
        )

    private val canonicalTraceInsights =
        listOf(
            Insight(
                observedLabel = "123305107ns",
                traceLink =
                    startupTraceLinkOf("6", "POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS"),
                category =
                    Insight.Category(
                        titleUrl =
                            "https://d.android.com/test#POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS",
                        title = "Potential CPU contention with another process",
                        postTitleLabel = " (expected: < 100000000ns)"
                    )
            ),
            Insight(
                observedLabel = "328462261ns",
                traceLink = startupTraceLinkOf("6", "JIT_ACTIVITY"),
                category =
                    Insight.Category(
                        titleUrl = "https://d.android.com/test#JIT_ACTIVITY",
                        title = "JIT Activity",
                        postTitleLabel = " (expected: < 100000000ns)"
                    )
            ),
            Insight(
                observedLabel = "150 count",
                traceLink = startupTraceLinkOf("6", "JIT_COMPILED_METHODS"),
                category =
                    Insight.Category(
                        titleUrl = "https://d.android.com/test#JIT_COMPILED_METHODS",
                        title = "JIT compiled methods",
                        postTitleLabel = " (expected: < 65 count)"
                    )
            )
        )

    private val canonicalTraceInsightSummary =
        listOf(
            InsightSummary(
                category =
                    "[Potential CPU contention with another process](https://d.android.com/test#POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS) (expected: < 100000000ns)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(123305107ns)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?AndroidStartup%3ApackageName=androidx.compose.integration.hero.macrobenchmark.target&AndroidStartup%3AslowStartReasonId=POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS)(123305107ns)"
            ),
            InsightSummary(
                category =
                    "[JIT Activity](https://d.android.com/test#JIT_ACTIVITY) (expected: < 100000000ns)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(328462261ns)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?AndroidStartup%3ApackageName=androidx.compose.integration.hero.macrobenchmark.target&AndroidStartup%3AslowStartReasonId=JIT_ACTIVITY)(328462261ns)"
            ),
            InsightSummary(
                category =
                    "[JIT compiled methods](https://d.android.com/test#JIT_COMPILED_METHODS) (expected: < 65 count)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(150 count)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?AndroidStartup%3ApackageName=androidx.compose.integration.hero.macrobenchmark.target&AndroidStartup%3AslowStartReasonId=JIT_COMPILED_METHODS)(150 count)"
            ),
        )

    @MediumTest
    @Test
    fun queryInsights() {
        TraceProcessor.runSingleSessionServer(api35ColdStart) {
            assertThat(
                insights
                    .queryInsights(
                        session = this,
                        packageName = Packages.MISSING,
                        traceLinkTitle = "6",
                        traceLinkPath = "/fake/output/relative/path.perfetto-trace"
                    )
                    .isEmpty()
            )

            assertThat(
                    insights.queryInsights(
                        session = this,
                        packageName = targetPackage,
                        traceLinkTitle = "6",
                        traceLinkPath = "/fake/output/relative/path.perfetto-trace"
                    )
                )
                .isEqualTo(canonicalTraceInsights)
        }
    }

    @MediumTest
    @Test
    fun createInsightSummaries_v2() {
        assertThat(canonicalTraceInsights.createInsightSummaries().map { it.observedV2 })
            .isEqualTo(canonicalTraceInsightSummary.map { it.observedV2 })
    }

    @MediumTest
    @Test
    fun createInsightSummaries_v3() {
        assertThat(canonicalTraceInsights.createInsightSummaries().map { it.observedV3 })
            .isEqualTo(canonicalTraceInsightSummary.map { it.observedV3 })
    }
}
