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

package androidx.wear.compose.material3.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.testutils.createCompilationParams
import androidx.wear.compose.material3.macrobenchmark.common.TransformingLazyColumnBenchmark
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TransformingLazyColumnBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        disableChargingExperience()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = TRANSFORMING_LAZY_COLUMN_ACTIVITY
                startActivityAndWait(intent)
            }
        ) {
            TransformingLazyColumnBenchmark.exercise.invoke(this)
        }
    }

    companion object {

        private const val TRANSFORMING_LAZY_COLUMN_ACTIVITY =
            "$PACKAGE_NAME.TRANSFORMING_LAZY_COLUMN_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
