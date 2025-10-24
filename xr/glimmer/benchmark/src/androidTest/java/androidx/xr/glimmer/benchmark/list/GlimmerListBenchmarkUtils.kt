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

package androidx.xr.glimmer.benchmark.list

import androidx.compose.testutils.ComposeExecutionControl
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.platform.ViewRootForTest

// TODO: b/448365164 - It's a copy-paste of LazyBenchmarkTestCase, remove it once the bug is closed.
internal interface GlimmerListBenchmarkTestCase : ComposeTestCase {
    fun setUp()

    fun beforeToggleCheck()

    fun toggle()

    fun afterToggleCheck()

    fun tearDown()
}

/**
 * Measures scrolling, which internally includes force measure and layout phases.
 *
 * This is copied from LazyBenchmarkCommon for common lists. Once its method becomes public, this
 * method should be replaced and deleted (b/448365164).
 */
// TODO: b/448365164 - Migrate to use common LazyList benchmarks.
internal fun ComposeBenchmarkRule.toggleStateBenchmark(
    caseFactory: () -> GlimmerListBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread { doFramesUntilNoChangesPending() }

        measureRepeatedOnUiThread {
            runWithMeasurementDisabled {
                assertNoPendingRecompositionMeasureOrLayout()
                getTestCase().setUp()
                if (hasPendingChanges() || hasPendingMeasureOrLayout()) {
                    doFrame()
                }
                assertNoPendingRecompositionMeasureOrLayout()
                getTestCase().beforeToggleCheck()
            }

            performToggle(getTestCase()) // move

            runWithMeasurementDisabled {
                getTestCase().afterToggleCheck()
                getTestCase().tearDown()
                assertNoPendingRecompositionMeasureOrLayout()
            }
        }
    }
}

/**
 * This benchmark is specific to Glimmer's lazy lists. It measures draw phase after scrolling.
 *
 * It's forked from LazyBenchmarkCommon for common lists. We use a separate implementation because
 * Glimmer lists have a few extra changes, and the common version is still internal. Once both of
 * these issues are addressed, we might delete this method and use a shared version instead
 * (b/448365164).
 */
// TODO: b/448365164 - Migrate to use common LazyList benchmarks.
internal fun ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> GlimmerListBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread { doFrame() }

        measureRepeatedOnUiThread {
            runWithMeasurementDisabled {
                // reset the state and draw
                getTestCase().setUp()
                getTestCase().beforeToggleCheck()

                // TODO: b/448365164 - This line is specific for GlimmerList.
                //  This line is needed because we track isScrollable state in composition.
                //  See androidx.xr.glimmer.list.List#isScrollEnabled.
                recompose()

                measure()
                layout()
                drawPrepare()
                draw()
                drawFinish()
                getTestCase().toggle()

                // TODO: b/448365164 - The two following lines of code are specific for GlimmerList.
                //  The root cause why they are needed is PinnableItemContainer. After scrolling
                //  with toggle(), a new item gets focused, which makes FocusableNode to call
                //  PinnableItemContainer#pin. The list of pinned items is read inside List measure
                //  scope, which eventually triggers a remeasure.
                //  For some reason (that I haven't yet discovered), Compose doesn't know about the
                //  pending changes in recomposition, so calling measure() and layout() won't do
                //  anything unless View requests it via requestLayout(). If I don't do that, View
                //  will call measureAndLayout() either way right before draw() but it will happen
                //  inside the measuring block.
                //  This is why I put requestLayout() and recompose() here.
                requestLayout()
                recompose()

                measure()
                layout()
                drawPrepare()

                assertNoPendingRecompositionMeasureOrLayout()
            }
            draw()
            runWithMeasurementDisabled {
                getTestCase().afterToggleCheck()
                getTestCase().tearDown()
                drawFinish()
            }
        }
    }
}

private fun ComposeExecutionControl.performToggle(testCase: GlimmerListBenchmarkTestCase) {
    testCase.toggle()
    if (hasPendingChanges()) {
        recompose()
    }
    if (hasPendingMeasureOrLayout()) {
        getViewRoot().measureAndLayoutForTest()
    }
}

private fun ComposeExecutionControl.assertNoPendingRecompositionMeasureOrLayout() {
    if (hasPendingChanges() || hasPendingMeasureOrLayout()) {
        throw AssertionError(
            "Expected no pending changes but there were some (recomposition=${hasPendingChanges()}, measureOrLayout=${hasPendingMeasureOrLayout()})."
        )
    }
}

private fun ComposeExecutionControl.getViewRoot(): ViewRootForTest =
    getHostView() as ViewRootForTest
