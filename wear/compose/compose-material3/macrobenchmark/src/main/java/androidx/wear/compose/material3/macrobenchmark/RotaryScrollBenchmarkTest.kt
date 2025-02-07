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

package androidx.wear.compose.material3.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.test.filters.LargeTest
import androidx.wear.compose.material3.macrobenchmark.common.RotaryScrollBenchmark
import androidx.wear.compose.material3.macrobenchmark.common.hasRotaryInputDevice
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class RotaryScrollBenchmarkTest(compilationMode: CompilationMode) :
    BenchmarkTestBase(
        compilationMode = compilationMode,
        macrobenchmarkScreen = RotaryScrollBenchmark,
        actionSuffix = "ROTARY_SCROLL_ACTIVITY"
    ) {

    @Test
    override fun start() {
        Assume.assumeTrue(hasRotaryInputDevice())
        super.start()
    }
}
