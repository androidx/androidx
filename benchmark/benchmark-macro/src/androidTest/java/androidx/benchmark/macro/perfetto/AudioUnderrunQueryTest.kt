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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@SdkSuppress(minSdkVersion = 23)
@RunWith(AndroidJUnit4::class)
@SmallTest
class AudioUnderrunQueryTest {
    @Test
    fun validateFixedTrace() {
        assumeTrue(isAbiSupported())

        // the trace was generated during 2 seconds AudioUnderrunBenchmark scenario run
        val traceFile = createTempFileFromAsset("api23_audio_underrun", ".perfetto-trace")

        val subMetrics = AudioUnderrunQuery.getSubMetrics(traceFile.absolutePath)
        val expectedMetrics = AudioUnderrunQuery.SubMetrics(2212, 892)

        assertEquals(expectedMetrics, subMetrics)
    }
}