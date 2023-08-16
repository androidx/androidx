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

internal fun ComposeBenchmarkRule.benchmarkDrawUntilStable(
    caseFactory: () -> LayeredComposeTestCase,
    maxSteps: Int = 10,
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending(maxSteps)
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
            }

            var loopCount = 0
            while (hasPendingChanges()) {
                loopCount++
                runWithTimingDisabled {
                    recomposeUntilNoChangesPending(maxSteps)
                    requestLayout()
                    measure()
                    layout()
                    drawPrepare()
                }

                draw()

                runWithTimingDisabled {
                    drawFinish()
                }
            }

            if (loopCount == 1) {
                throw AssertionError("Use benchmarkFirstDraw instead")
            }

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

internal fun ComposeBenchmarkRule.benchmarkMeasureUntilStable(
    caseFactory: () -> LayeredComposeTestCase,
    maxSteps: Int = MaxSteps,
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending(maxSteps)
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
            }

            var loopCount = 0
            while (hasPendingChanges()) {
                loopCount++
                runWithTimingDisabled {
                    recomposeUntilNoChangesPending(maxSteps)
                    requestLayout()
                }

                measure()

                runWithTimingDisabled {
                    layout()
                    drawPrepare()
                    draw()
                    drawFinish()
                }
            }

            if (loopCount == 1) {
                throw AssertionError("Use benchmarkFirstMeasure instead")
            }

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

internal fun ComposeBenchmarkRule.benchmarkLayoutUntilStable(
    caseFactory: () -> LayeredComposeTestCase,
    maxSteps: Int = MaxSteps,
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending(maxSteps)
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
            }

            var loopCount = 0
            while (hasPendingChanges()) {
                loopCount++
                runWithTimingDisabled {
                    recomposeUntilNoChangesPending(maxSteps)
                    requestLayout()
                    measure()
                }

                layout()

                runWithTimingDisabled {
                    drawPrepare()
                    draw()
                    drawFinish()
                }
            }

            if (loopCount == 1) {
                throw AssertionError("Use benchmarkFirstLayout instead")
            }

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

internal fun ComposeBenchmarkRule.benchmarkFirstRenderUntilStable(
    caseFactory: () -> LayeredComposeTestCase,
    maxSteps: Int = MaxSteps,
) {
    runBenchmarkFor(LayeredCaseAdapter.of(caseFactory)) {
        measureRepeated {
            runWithTimingDisabled {
                doFramesUntilNoChangesPending(maxSteps)
                // Add the content to benchmark
                getTestCase().addMeasuredContent()
            }

            var loopCount = 0
            while (hasPendingChanges()) {
                loopCount++
                recomposeUntilNoChangesPending()
                requestLayout()
                measure()
                layout()
                drawPrepare()
                draw()

                runWithTimingDisabled {
                    drawFinish()
                }
            }

            if (loopCount == 1) {
                throw AssertionError("Use benchmarkToFirstPixel instead")
            }

            runWithTimingDisabled {
                assertNoPendingChanges()
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

private const val MaxSteps = 10
