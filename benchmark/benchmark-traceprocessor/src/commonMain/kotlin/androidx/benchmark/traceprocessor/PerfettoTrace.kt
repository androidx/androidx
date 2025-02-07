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

package androidx.benchmark.traceprocessor

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

public class PerfettoTrace(
    /**
     * Absolute file path of the trace.
     *
     * Note that the trace is not guaranteed to be placed into an app-accessible directory, and may
     * require shell commands to access.
     */
    @get:RestrictTo(LIBRARY_GROUP) public val path: String
) {
    /**
     * Represents a link to a given PerfettoTrace, including a URL parameter string.
     *
     * As this link may be shared across environments (e.g. between an Android device and a host
     * desktop machine), the paths are not guaranteed to be absolute.
     */
    @ExperimentalInsightApi
    public class Link(
        public val title: String,
        /**
         * Path to the trace.
         *
         * When used with Android Benchmark, this is the relative path to the trace in the test
         * output directory.
         */
        public val path: String,

        /** Url params passed to ui.perfetto.dev, UTF-8 encoded */
        public val urlParamsEncoded: String
    ) {
        public constructor(
            title: String,
            /**
             * Path represented by the string
             *
             * When used with Android Benchmark, this is the relative path to the trace in the test
             * output directory.
             */
            path: String,
            /** Url params passed to ui.perfetto.dev, will be UTF-8 encoded */
            urlParamMap: Map<String, String>
        ) : this(
            title = title,
            path = path,
            urlParamsEncoded =
                buildString {
                    // insert ? or & as parameter delimiters
                    var firstDelimiter = true
                    fun appendDelimiter() {
                        if (firstDelimiter) {
                            append("?")
                            firstDelimiter = false
                        } else {
                            append("&")
                        }
                    }
                    // sort keys for stability (e.g. in tests)
                    urlParamMap.keys.sorted().forEach { key ->
                        appendDelimiter()
                        append("${urlEncode(key)}=${urlEncode(urlParamMap[key]!!)}")
                    }
                }
        )

        public val uri: String = "uri://$path$urlParamsEncoded"
        public val markdownUriLink: String = "[$title]($uri)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Link) return false

            if (title != other.title) return false
            if (path != other.path) return false
            if (urlParamsEncoded != other.urlParamsEncoded) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + path.hashCode()
            result = 31 * result + urlParamsEncoded.hashCode()
            return result
        }

        override fun toString(): String {
            return "Link(title='$title', uri='$uri')"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerfettoTrace) return false

        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String {
        return "PerfettoTrace(path='$path')"
    }

    // this companion object exists to enable PerfettoTrace.Companion.record to be declared
    public companion object
}
