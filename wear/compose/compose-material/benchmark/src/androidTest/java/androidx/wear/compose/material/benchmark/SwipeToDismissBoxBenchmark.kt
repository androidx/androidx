/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material.benchmark

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.ProfilerConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.SwipeToDismissBox
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.MaterialTheme
import org.junit.Rule
import org.junit.Test

class SwipeToDismissBoxBenchmark {
    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule(
        MicrobenchmarkConfig(
            profiler = ProfilerConfig.MethodTracing()
        )
    )

    @Test
    fun s2dbox_benchmarkToFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel { S2DBoxTestCase() }
    }
}

private class S2DBoxTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        Screen()
    }

    @Composable
    private fun Screen() {
        val state = rememberSwipeToDismissBoxState()
        SwipeToDismissBox(
            state = state,
            onDismissed = { },
        ) { isBackground ->
            if (isBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.onSurface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                )
            }
        }
    }
}
