/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.benchmark

import android.app.Activity
import android.view.View
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.TestCase
import androidx.ui.test.invalidateViews
import androidx.ui.test.recomposeSyncAssertHadChanges
import androidx.ui.test.recomposeSyncAssertNoChanges
import androidx.ui.test.runOnUiThreadSync

/**
 * Measures measure and layout performance of the given testCase by toggling measure constraints.
 */
fun BenchmarkRule.measureLayoutPerf(activity: Activity, testCase: TestCase) {
    activity.runOnUiThreadSync {
        testCase.runSetup()

        val width = testCase.view.measuredWidth
        val height = testCase.view.measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        testCase.measureWithSpec(widthSpec, heightSpec)
        testCase.layout()

        var lastWidth = testCase.view.measuredWidth
        var lastHeight: Int
        measureRepeated {
            runWithTimingDisabled {
                if (lastWidth == width) {
                    lastWidth = width - 10
                    lastHeight = height - 10
                } else {

                    lastWidth = width
                    lastHeight = height
                }
                widthSpec =
                    View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                heightSpec =
                    View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
            }
            testCase.measureWithSpec(widthSpec, heightSpec)
            testCase.layout()
        }
    }
}

/**
 * Measures draw performance of the given testCase by invalidating the view hierarchy.
 */
fun BenchmarkRule.measureDrawPerf(activity: Activity, testCase: TestCase) {
    activity.runOnUiThreadSync {
        testCase.runSetup()

        measureRepeated {
            runWithTimingDisabled {
                testCase.invalidateViews()
                testCase.prepareDraw()
            }
            testCase.draw()
            runWithTimingDisabled {
                testCase.finishDraw()
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 */
fun BenchmarkRule.toggleStateMeasureRecompose(
    activity: Activity,
    testCase: ComposeTestCase,
    toggleState: () -> Unit
) {
    activity.runOnUiThreadSync {
        testCase.runSetup()
        testCase.recomposeSyncAssertNoChanges()

        measureRepeated {
            runWithTimingDisabled {
                toggleState()
            }
            testCase.recomposeSyncAssertHadChanges()
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun BenchmarkRule.toggleStateMeasureMeasure(
    activity: Activity,
    testCase: ComposeTestCase,
    toggleState: () -> Unit
) {
    activity.runOnUiThreadSync {
        testCase.runSetup()
        testCase.recomposeSyncAssertNoChanges()

        measureRepeated {
            runWithTimingDisabled {
                toggleState()
                testCase.recomposeSyncAssertHadChanges()
            }
            testCase.measure()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun BenchmarkRule.toggleStateMeasureLayout(
    activity: Activity,
    testCase: ComposeTestCase,
    toggleState: () -> Unit
) {
    activity.runOnUiThreadSync {
        testCase.runSetup()
        testCase.recomposeSyncAssertNoChanges()

        measureRepeated {
            runWithTimingDisabled {
                toggleState()
                testCase.recomposeSyncAssertHadChanges()
                testCase.measure()
            }
            testCase.layout()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun BenchmarkRule.toggleStateMeasureDraw(
    activity: Activity,
    testCase: ComposeTestCase,
    toggleState: () -> Unit
) {
    activity.runOnUiThreadSync {
        testCase.runSetup()
        testCase.recomposeSyncAssertNoChanges()

        measureRepeated {
            runWithTimingDisabled {
                toggleState()
                testCase.recomposeSyncAssertHadChanges()
                testCase.measure()
                testCase.layout()
                testCase.prepareDraw()
            }
            testCase.draw()
            runWithTimingDisabled {
                testCase.finishDraw()
            }
        }
    }
}