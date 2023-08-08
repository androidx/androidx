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

package androidx.benchmark.macro.perfetto

import android.os.Build
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.tracing.trace
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for Perfetto SDK tracing [androidx.tracing.perfetto.PerfettoSdkTrace] verifying that:
 * - it works in conjunction with android.os.Trace
 * - it can handle non-ASCII characters, including unicode surrogate pairs
 * - it can handle whitespace at the start/end of the traced string
 * - both fast and slow code-paths work (see [StringSource.fastPathLimit])
 *
 * Note: as per Benchmark convention (e.g. [PerfettoTraceRule]), the following terms are used:
 * - app tag tracing: referring to android.os.Trace
 * - user-space tracing: referring to Perfetto SDK (androidx.tracing.perfetto.Tracing)
 */
@LargeTest
@OptIn(ExperimentalPerfettoCaptureApi::class)
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
class PerfettoSdkTraceTest(enableAppTagTracing: Boolean, enableUserspaceTracing: Boolean) {
    @get:Rule
    val rule = PerfettoTraceRule(
        enableAppTagTracing = enableAppTagTracing,
        enableUserspaceTracing = enableUserspaceTracing
    ) { trace ->
        val expectedSlices = sequence {
            if (enableAppTagTracing) yield(StringSource.appTagTraceStrings)
            if (enableUserspaceTracing) yield(StringSource.userspaceTraceStrings)
        }.flatMap { it }.toList()
        val actualSlices = PerfettoTraceProcessor.runSingleSessionServer(trace.path) {
            StringSource.allTraceStrings.flatMap {
                querySlices(
                    it,
                    packageName = null
                ).map { s -> s.name }
            }
        }
        assertThat(actualSlices).containsExactlyElementsIn(expectedSlices)
    }

    @Ignore // b/260715950
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    fun test_endToEnd() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        assumeTrue(PerfettoHelper.isAbiSupported())
        StringSource.appTagTraceStrings.forEach { trace(it) { } }
        StringSource.userspaceTraceStrings.forEach { str ->
            androidx.tracing.perfetto.PerfettoSdkTrace.beginSection(str)
            androidx.tracing.perfetto.PerfettoSdkTrace.endSection()
        }
    }

    companion object {
        @Parameterized.Parameters(name = "appTagEnabled={0},userspaceEnabled={1}")
        @JvmStatic
        fun data() = listOf(
            arrayOf(false, false),
            arrayOf(true, false),
            arrayOf(true, true),
            arrayOf(false, true),
        )
    }
}

private object StringSource {
    /** Keep in sync with [androidx.tracing.perfetto.jni.PerfettoNative.nativeTraceEventBegin] */
    private const val fastPathLimit = 4096

    /** Keep in sync with [android.os.Trace] */
    private const val appTagLimit = 127

    private const val nonAscii = "  ośmiornica" + // non-ASCII string
        " \uD800\uDC00" + // known high surrogate + known low surrogate
        " \uD83D\uDC19" + // 🐙
        " 蛸" + // kanji character
        " {}()'-_\$  " // tricky characters and trailing spaces

    private val overFastLimit = String(CharArray(fastPathLimit + 1) { 'a' })
    private val atFastLimit = overFastLimit.take(fastPathLimit)
    private val underFastLimit = overFastLimit.take(fastPathLimit - 1)

    private val overFastLimitNonAscii = "$nonAscii $overFastLimit "

    private val atAppTagLimit = overFastLimitNonAscii.take(appTagLimit - 2) + "ę "

    val userspaceTraceStrings =
        listOf(nonAscii, underFastLimit, atFastLimit, overFastLimit, overFastLimitNonAscii)
    val appTagTraceStrings = listOf(atAppTagLimit)
    val allTraceStrings = userspaceTraceStrings + appTagTraceStrings
}
