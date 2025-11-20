/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getAllSemanticsNodes

/**
 * Provides necessary services to facilitate testing.
 *
 * This is typically implemented by entities like test rule.
 */
internal interface TestOwner {
    /** Clock that drives frames and recompositions in compose tests. */
    val mainClock: MainTestClock

    /**
     * Runs the given [action] on the ui thread.
     *
     * This is a blocking call.
     */
    // TODO: Does ui-test really need it? Can it use coroutine context on Owner?
    fun <T> runOnUiThread(action: () -> T): T

    /**
     * Collects all [RootForTest]s from all compose hierarchies.
     *
     * This method is the choke point where all assertions and interactions must go through when
     * testing composables and where we thus have the opportunity to automatically reach quiescence.
     * This is done by calling [ComposeUiTest.waitForIdle] before getting and returning the
     * registered roots.
     *
     * @param atLeastOneRootExpected Whether the caller expects that at least one compose root is
     *   present in the tested app. This affects synchronization efforts / timeouts of this API.
     */
    fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest>

    /**
     * Executes all tasks that are currently due for immediate execution on the virtual clock.
     *
     * Due tasks are operations scheduled to run on this clock without any delay. This method should
     * only be necessary when the test is running on a **confined**
     * [TestDispatcher][kotlinx.coroutines.test.TestDispatcher] (like
     * [StandardTestDispatcher][kotlinx.coroutines.test.StandardTestDispatcher]), where tasks are
     * queued but not executed automatically. On an
     * [UnconfinedTestDispatcher][kotlinx.coroutines.test.UnconfinedTestDispatcher], all tasks that
     * are scheduled without delay will be executed immediately, so they will never end up as a due
     * task.
     *
     * This function is a fundamental building block for more advanced synchronization APIs. For
     * example, it is used internally by APIs like `waitForIdle()`, `advanceTimeBy()`, etc. to
     * process pending work before or after manipulating the clock.
     *
     * In almost all testing scenarios, you should prefer using higher-level APIs to ensure proper
     * synchronization.
     *
     * Only call this function directly if you have a specific need to run only the immediately
     * available tasks without advancing time or waiting for a complete idle state.
     */
    fun runCurrent()
}

/**
 * Collects all [SemanticsNode]s from all compose hierarchies, and returns the [transform]ed
 * results.
 *
 * Set [useUnmergedTree] to `true` to search through the unmerged semantics tree.
 *
 * Set [skipDeactivatedNodes] to `false` to include
 * [deactivated][androidx.compose.ui.node.LayoutNode.isDeactivated] nodes in the search.
 *
 * Use [atLeastOneRootRequired] to treat not finding any compose hierarchies at all as an error. If
 * no hierarchies are found, we will wait 2 seconds to accommodate cases where composable content is
 * set asynchronously. On the other hand, if you expect or know that there is no composable content,
 * set [atLeastOneRootRequired] to `false` and no error will be thrown if there are no compose
 * roots, and the wait for compose roots will be reduced to .5 seconds.
 *
 * This method will wait for quiescence before collecting all SemanticsNodes. Collection happens on
 * the main thread and the [transform]ation of all SemanticsNodes to a result is done while on the
 * main thread. This allows us to transform the result using methods that must be called on the main
 * thread, without switching back and forth between the main thread and the test thread.
 */
internal fun <R> TestOwner.getAllSemanticsNodes(
    atLeastOneRootRequired: Boolean,
    useUnmergedTree: Boolean,
    skipDeactivatedNodes: Boolean = true,
    transform: (Iterable<SemanticsNode>) -> R,
): R {
    val roots =
        getRoots(atLeastOneRootRequired).also {
            check(!atLeastOneRootRequired || it.isNotEmpty()) {
                "No compose hierarchies found in the app. Possible reasons include: " +
                    "(1) the Activity that calls setContent did not launch; " +
                    "(2) setContent was not called; " +
                    "(3) setContent was called before the ComposeTestRule ran. " +
                    "If setContent is called by the Activity, make sure the Activity is " +
                    "launched after the ComposeTestRule runs"
            }
        }

    return runOnUiThread {
        transform.invoke(
            roots.flatMap {
                it.semanticsOwner.getAllSemanticsNodes(
                    mergingEnabled = !useUnmergedTree,
                    skipDeactivatedNodes = skipDeactivatedNodes,
                )
            }
        )
    }
}
