/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.benchmark.traceprocessor.Insight
import androidx.benchmark.traceprocessor.PerfettoTrace

/**
 * Create a benchmark markdown trace deeplink
 *
 * LinkFormat isn't a TraceProcessor concept, which is why this functionality is here
 */
internal fun PerfettoTrace.Link.benchmarkMarkdownLink(linkFormat: LinkFormat): String =
    when (linkFormat) {
        LinkFormat.V2 -> Markdown.createFileLink(label = title, path = path)
        LinkFormat.V3 -> markdownUriLink
    }

/**
 * Create a insight category title markdown link
 *
 * LinkFormat isn't a TraceProcessor concept, which is why this functionality is here
 */
private fun Insight.Category.header(linkFormat: LinkFormat): String =
    if (linkFormat == LinkFormat.V3 && titleUrl != null) {
        Markdown.createLink(title, titleUrl!!)
    } else {
        title
    } + postTitleLabel

private fun List<Insight>.toObserved(linkFormat: LinkFormat): String {
    return this.joinToString(separator = " ", prefix = "seen in iterations: ") { insight ->
        insight.traceLink.benchmarkMarkdownLink(linkFormat) + "(${insight.observedLabel})"
    }
}

/**
 * Create a list of [InsightSummary]s, each summarizing multiple [Insight]s with the same
 * [Insight.Category]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun List<Insight>.createInsightSummaries(): List<InsightSummary> {
    return this.groupBy { it.category }
        .map { (category, insights) ->
            InsightSummary(
                category,
                insights.sortedBy {
                    // sort by iteration (embedded in title)
                    it.traceLink.title.toInt()
                }
            )
        }
}

/**
 * Represents an insight into performance issues detected during a benchmark.
 *
 * Provides details about the specific criterion that was violated, along with information about
 * where and how the violation was observed.
 *
 * @param category A description of the performance issue, including the expected behavior and any
 *   relevant thresholds.
 * @param observedV2 Specific details about when and how the violation occurred, such as the
 *   iterations where it was observed and any associated values. Uses [LinkFormat.V2].
 * @param observedV3 Specific details about when and how the violation occurred, such as the
 *   iterations where it was observed and any associated values. Uses [LinkFormat.V3] to link more
 *   precisely into traces.
 *
 * TODO(364598145): generalize
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class InsightSummary(
    val category: String,
    val observedV2: String,
    val observedV3: String,
) {
    constructor(
        category: Insight.Category,
        insights: List<Insight>
    ) : this(
        category = category.header(LinkFormat.V3), // TODO: avoid this format hard coding!
        observedV2 = insights.toObserved(LinkFormat.V2),
        observedV3 = insights.toObserved(LinkFormat.V3)
    ) {
        require(insights.isNotEmpty())
        require(insights.all { it.category == category })
    }
}
