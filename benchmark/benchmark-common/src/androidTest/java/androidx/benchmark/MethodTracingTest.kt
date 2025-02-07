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

package androidx.benchmark

import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.runSingleSessionServer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 23)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MethodTracingTest {
    val methodTrace =
        createTempFileFromAsset("api35_runWithMeasurementDisabled-methodTracing", ".trace")
    val perfettoTrace =
        createTempFileFromAsset("api35_runWithMeasurementDisabled", ".perfetto-trace")

    @Test
    fun embed_and_methodCount() {
        MethodTracing.embedInPerfettoTrace(methodTrace, perfettoTrace)

        val metrics =
            TraceProcessor.runSingleSessionServer(perfettoTrace.absolutePath) {
                // Note that queryMetric relies on current classnames. This means if this *test apk*
                // is minified, these classnames will not match what's inside the fixed method trace
                // minifying this test isn't expected, but minifying benchmarks is, which is why the
                // current in-process class/method name are used
                MethodTracing.queryMetrics(this)
            }

        assertEquals(listOf(MetricResult("methodCount", listOf(38.0))), metrics)
    }
}
