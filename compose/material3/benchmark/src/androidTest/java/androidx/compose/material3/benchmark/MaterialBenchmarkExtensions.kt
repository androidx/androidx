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

package androidx.compose.material3.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.recomposeUntilNoChangesPending
import androidx.compose.testutils.doFramesUntilNoChangesPending
import org.junit.Assert

/**
 * Measures the time of the first draw right after the given test case is added to an already
 * existing hierarchy.
 *
 * @param caseFactory a factory for [LayeredComposeTestCase]
 * @param allowPendingChanges in case `true`, the benchmark will allow pending changes after the
 * first draw. Otherwise, an [assertNoPendingChanges] will be called.
 */
internal fun ComposeBenchmarkRule.benchmarkFirstDraw(
    caseFactory: () -> LayeredComposeTestCase,
    allowPendingChanges: Boolean
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()

            runWithTimingDisabled {
                drawFinish()
                if (!allowPendingChanges) assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first measure right after the given test case is added to an already
 * existing hierarchy.
 *
 * @param caseFactory a factory for [LayeredComposeTestCase]
 * @param allowPendingChanges in case `true`, the benchmark will allow pending changes after the
 * first measure. Otherwise, an [assertNoPendingChanges] will be called.
 */
internal fun ComposeBenchmarkRule.benchmarkFirstMeasure(
    caseFactory: () -> LayeredComposeTestCase,
    allowPendingChanges: Boolean
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
            }

            measure()

            runWithTimingDisabled {
                if (!allowPendingChanges) assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first layout right after the given test case is added to an already
 * existing hierarchy.
 *
 * @param caseFactory a factory for [LayeredComposeTestCase]
 * @param allowPendingChanges in case `true`, the benchmark will allow pending changes after the
 * first layout. Otherwise, an [assertNoPendingChanges] will be called.
 */
internal fun ComposeBenchmarkRule.benchmarkFirstLayout(
    caseFactory: () -> LayeredComposeTestCase,
    allowPendingChanges: Boolean
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending()
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
                recomposeUntilNoChangesPending()
                requestLayout()
                measure()
            }

            layout()

            runWithTimingDisabled {
                if (!allowPendingChanges) assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

private class LayeredCaseAdapter(private val innerCase: LayeredComposeTestCase) : ComposeTestCase {

    companion object {
        fun of(caseFactory: () -> LayeredComposeTestCase): () -> LayeredCaseAdapter = {
            LayeredCaseAdapter(caseFactory())
        }
    }

    var isComposed by mutableStateOf(false)

    @Composable
    override fun Content() {
        innerCase.ContentWrappers {
            if (isComposed) {
                innerCase.MeasuredContent()
            }
        }
    }

    fun addMeasuredContent() {
        Assert.assertTrue(!isComposed)
        isComposed = true
    }
}

private const val MaxAmountOfRecompositions = 10
