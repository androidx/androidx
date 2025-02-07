/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.benchmark.traceprocessor

/**
 * Individual case of a problem identified in a given trace (from a specific iteration).
 *
 * Benchmark output will summarize multiple insights, for example:
 * ```Markdown
 * [JIT Activity](https://d.android.com/test#JIT_ACTIVITY) (expected: < 100000000ns)
 * seen in iterations: [6](file:///path/to/trace.perfetto-trace)(328462261ns),
 * ```
 */
@ExperimentalInsightApi
public class Insight(
    /**
     * Category of the Insight, representing the type of problem.
     *
     * Every similar insight must identify equal [Category]s to enable macrobenchmark to group them
     * across iterations.
     */
    public val category: Category,

    /** suffix after the deeplink to convey problem severity, e.g. "(100000000ns)" */
    public val observedLabel: String,

    /**
     * Link to content within a trace to highlight for the given problem.
     *
     * Insights should specify [PerfettoTrace.Link.urlParamsEncoded] to pass context to
     * [ui.perfetto.dev](https://ui.perfetto.dev).
     */
    public val traceLink: PerfettoTrace.Link,
) {
    /**
     * Category represents general expectations that have been violated by an insight - e.g. JIT
     * shouldn't take longer than XX ms, or trampoline activities shouldn't be used.
     *
     * Every Insight that shares the same conceptual category must share an equal [Category] so that
     * macrobenchmark can group them when summarizing observed patterns across multiple iterations.
     *
     * In Studio versions supporting web links, will produce an output like the following:
     * ```Markdown
     * [JIT compiled methods](https://d.android.com/test#JIT_COMPILED_METHODS) (expected: < 65 count)
     * ```
     *
     * In the above example:
     * - [title] is `"JIT compiled methods"`
     * - [titleUrl] is `"https://d.android.com/test#JIT_COMPILED_METHODS"`
     * - [postTitleLabel] is " (expected: < 65 count)"
     *
     * In Studio versions not supporting web links, [titleUrl] is ignored.
     */
    @ExperimentalInsightApi
    public class Category(
        /** Title of the Insight category, for example "JIT compiled methods" */
        public val title: String,

        /**
         * Optional web URL to explain the category of problem.
         *
         * If non-null, generally [title] should be linked to this URL.
         */
        public val titleUrl: String?,

        /**
         * Suffix after the title, generally specifying expected values.
         *
         * For example, " (expected: < 65 count)"
         */
        public val postTitleLabel: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is Category) return false

            if (title != other.title) return false
            if (titleUrl != other.titleUrl) return false
            if (postTitleLabel != other.postTitleLabel) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + (titleUrl?.hashCode() ?: 0)
            result = 31 * result + postTitleLabel.hashCode()
            return result
        }

        override fun toString(): String {
            return "Category(title='$title', titleUrl=$titleUrl, postTitleLabel='$postTitleLabel')"
        }
    }

    @ExperimentalInsightApi
    public interface Provider {
        public fun queryInsights(
            /** TraceProcessor.Session used to query problems and observations as Insights. */
            session: TraceProcessor.Session,

            /**
             * Package name of the target application, Insights should only be provided for this
             * app.
             */
            packageName: String,

            /**
             * Value to be passed to [PerfettoTrace.Link.title] when constructing a
             * [PerfettoTrace.Link] for an insight.
             *
             * In Macrobenchmark, this is the test iteration in which the individual insight was
             * observed.
             */
            traceLinkTitle: String,

            /**
             * Value to be passed to [PerfettoTrace.Link.path] when constructing a
             * [PerfettoTrace.Link] for an insight.
             *
             * In Macrobenchmark, this is the output relative path to the trace, to enable links to
             * be used on the host, after the output directory is copied out.
             */
            traceLinkPath: String,
        ): List<Insight>
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Insight) return false

        if (category != other.category) return false
        if (observedLabel != other.observedLabel) return false
        if (traceLink != other.traceLink) return false

        return true
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + observedLabel.hashCode()
        result = 31 * result + traceLink.hashCode()
        return result
    }

    override fun toString(): String {
        return "Insight(category=$category, observedLabel='$observedLabel', traceLink=$traceLink)"
    }
}
