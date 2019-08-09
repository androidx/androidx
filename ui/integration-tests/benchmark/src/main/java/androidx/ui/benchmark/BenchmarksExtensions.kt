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
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.FrameManager
import androidx.compose.disposeComposition
import androidx.ui.test.AndroidTestCase
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.TestCase
import androidx.ui.test.ToggleableTestCase
import androidx.ui.test.invalidateViews
import androidx.ui.test.recomposeSyncAssert
import androidx.ui.test.recomposeSyncAssertHadChanges
import androidx.ui.test.recomposeSyncAssertNoChanges
import androidx.ui.test.requestLayout
import androidx.ui.test.runOnUiThreadSync

/**
 * Measures measure and layout performance of the given test case by toggling measure constraints.
 */
fun BenchmarkRule.measureLayoutPerf(activity: Activity, testCase: TestCase) {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()

        val width = testCase.view.measuredWidth
        val height = testCase.view.measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        testCase.requestLayout()
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
                testCase.requestLayout()
            }
            testCase.measureWithSpec(widthSpec, heightSpec)
            testCase.layout()
        }

        if (testCase is ComposeTestCase) {
            activity.disposeComposition()
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun BenchmarkRule.measureDrawPerf(activity: Activity, testCase: TestCase) {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()

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

        if (testCase is ComposeTestCase) {
            activity.disposeComposition()
        }
    }
}

/**
 * Measures the time of the first composition of the given compose test case.
 */
fun BenchmarkRule.measureFirstCompose(
    activity: Activity,
    testCase: ComposeTestCase
) {
    activity.runOnUiThreadSync {
        measureRepeated {
            testCase.setupContent(activity)
            runWithTimingDisabled {
                testCase.recomposeSyncAssertNoChanges()
                activity.disposeComposition()
            }
        }
    }
}

/**
 * Measures the time of the first set content of the given Android test case.
 */
fun BenchmarkRule.measureFirstSetContent(
    activity: Activity,
    testCase: AndroidTestCase
) {
    activity.runOnUiThreadSync {
        measureRepeated {
            testCase.setupContent(activity)
        }
    }
}

/**
 * Measures the time of the first measure of the given test case.
 */
fun BenchmarkRule.measureFirstMeasure(
    activity: Activity,
    testCase: TestCase
) {
    activity.runOnUiThreadSync {
        measureRepeated {
            runWithTimingDisabled {
                testCase.setupContent(activity)
                testCase.requestLayout()
            }

            testCase.measure()

            runWithTimingDisabled {
                if (testCase is ComposeTestCase) {
                    testCase.recomposeSyncAssertNoChanges()
                    activity.disposeComposition()
                }
            }
        }
    }
}

/**
 * Measures the time of the first layout of the given test case.
 */
fun BenchmarkRule.measureFirstLayout(
    activity: Activity,
    testCase: TestCase
) {
    activity.runOnUiThreadSync {
        measureRepeated {
            runWithTimingDisabled {
                testCase.setupContent(activity)
                testCase.requestLayout()
                testCase.measure()
            }

            testCase.layout()

            runWithTimingDisabled {
                if (testCase is ComposeTestCase) {
                    testCase.recomposeSyncAssertNoChanges()
                    activity.disposeComposition()
                }
            }
        }
    }
}

/**
 * Measures the time of the first draw of the given test case.
 */
fun BenchmarkRule.measureFirstDraw(
    activity: Activity,
    testCase: TestCase
) {
    activity.runOnUiThreadSync {
        measureRepeated {
            runWithTimingDisabled {
                testCase.setupContent(activity)
                testCase.requestLayout()
                testCase.measure()
                testCase.layout()
                testCase.prepareDraw()
            }

            testCase.draw()

            runWithTimingDisabled {
                testCase.finishDraw()
                if (testCase is ComposeTestCase) {
                    testCase.recomposeSyncAssertNoChanges()
                    activity.disposeComposition()
                }
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureRecompose(
    activity: Activity,
    testCase: T
) where T : ComposeTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()
        testCase.recomposeSyncAssertNoChanges()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
            }
            testCase.recomposeSyncAssertHadChanges()
        }
        activity.disposeComposition()
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureMeasure(
    activity: Activity,
    testCase: T,
    toggleCausesRecompose: Boolean = true,
    firstDrawCausesRecompose: Boolean = false
) where T : ComposeTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        runToFirstDraw(testCase, firstDrawCausesRecompose)

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                testCase.recomposeSyncAssert(toggleCausesRecompose)
                testCase.requestLayout()
            }
            testCase.measure()
        }
        activity.disposeComposition()
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureLayout(
    activity: Activity,
    testCase: T,
    toggleCausesRecompose: Boolean = true,
    firstDrawCausesRecompose: Boolean = false
) where T : ComposeTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        runToFirstDraw(testCase, firstDrawCausesRecompose)

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                testCase.recomposeSyncAssert(toggleCausesRecompose)
                testCase.requestLayout()
                testCase.measure()
            }
            testCase.layout()
        }
        activity.disposeComposition()
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureDraw(
    activity: Activity,
    testCase: T,
    toggleCausesRecompose: Boolean = true,
    firstDrawCausesRecompose: Boolean = false
) where T : ComposeTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        runToFirstDraw(testCase, firstDrawCausesRecompose)

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                testCase.recomposeSyncAssert(toggleCausesRecompose)
                testCase.requestLayout()
                testCase.measure()
                testCase.layout()
                testCase.prepareDraw()
            }
            testCase.draw()
            runWithTimingDisabled {
                testCase.finishDraw()
            }
        }
        activity.disposeComposition()
    }
}

/**
 * Runs first draw on the test case and runs recomposition. Some layout/draw cycles
 * cause recomposition changes ([firstDrawCausesRecompose]). Changes are expected only
 * when [firstDrawCausesRecompose] is `true`.
 */
private fun <T> runToFirstDraw(
    testCase: T,
    firstDrawCausesRecompose: Boolean
) where T : ComposeTestCase {
    testCase.runToFirstDraw()
    FrameManager.nextFrame()
    testCase.recomposeSyncAssert(firstDrawCausesRecompose)
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureMeasure(
    activity: Activity,
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
            }
            testCase.measure()
        }
        activity.disposeComposition()
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureLayout(
    activity: Activity,
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                testCase.measure()
            }
            testCase.layout()
        }
        activity.disposeComposition()
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> BenchmarkRule.toggleStateMeasureDraw(
    activity: Activity,
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    activity.runOnUiThreadSync {
        testCase.runToFirstDraw()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                testCase.measure()
                testCase.layout()
                testCase.prepareDraw()
            }
            testCase.draw()
            runWithTimingDisabled {
                testCase.finishDraw()
            }
        }
        activity.disposeComposition()
    }
}
