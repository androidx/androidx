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

package androidx.benchmark

import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PerfettoTraceLinkTest {
    @Test
    fun benchmarkMarkdownLink_emptyString() {
        val link =
            PerfettoTrace.Link(title = "bar", path = "foo.perfetto-trace", urlParamsEncoded = "")
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V2))
            .isEqualTo("[bar](file://foo.perfetto-trace)")
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V3))
            .isEqualTo("[bar](uri://foo.perfetto-trace)")
    }

    @Test
    fun benchmarkMarkdownLink_simpleString() {
        val link =
            PerfettoTrace.Link(
                title = "bar",
                path = "foo.perfetto-trace",
                urlParamsEncoded = "?key=value"
            )
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V2))
            .isEqualTo("[bar](file://foo.perfetto-trace)")
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V3))
            .isEqualTo("[bar](uri://foo.perfetto-trace?key=value)")
    }

    @Test
    fun benchmarkMarkdownLink_map() {
        val link =
            PerfettoTrace.Link(
                title = "bar",
                path = "foo.perfetto-trace",
                urlParamMap = mapOf("key1" to "value1", "key2" to "value2")
            )
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V2))
            .isEqualTo("[bar](file://foo.perfetto-trace)")
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V3))
            .isEqualTo("[bar](uri://foo.perfetto-trace?key1=value1&key2=value2)")
    }

    @Test
    fun benchmarkMarkdownLink_mapEncode() {
        val link =
            PerfettoTrace.Link(
                title = "bar",
                path = "foo.perfetto-trace",
                urlParamMap = mapOf("foo:bar" to "baz")
            )
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V2))
            .isEqualTo("[bar](file://foo.perfetto-trace)")
        assertThat(link.benchmarkMarkdownLink(LinkFormat.V3))
            .isEqualTo("[bar](uri://foo.perfetto-trace?foo%3Abar=baz)")
    }
}
