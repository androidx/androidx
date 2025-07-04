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

package androidx.wear.compose.material3.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Benchmark for LocalReduceMotion.current. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ReduceMotionBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val reduceMotionCaseFactory = { ReduceMotionTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(reduceMotionCaseFactory)
    }
}

internal class ReduceMotionTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val motionReduced = LocalReduceMotion.current

        Box(
            modifier =
                Modifier.clearAndSetSemantics {
                    contentDescription =
                        if (motionReduced) {
                            "Animations are disabled"
                        } else {
                            "Animations are enabled"
                        }
                }
        ) {}
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}
