/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunWithoutImplicitWaitTest {
    @Test
    fun triggeredAnimationAndCaptureMotionValues() = runComposeUiTest {
        var size by mutableStateOf(64.dp)

        var animationIsDone = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.testTag("foo")
                            .animateContentSize { _, _ -> animationIsDone = true }
                            .size(size)
                            .background(Color.Red)
                )
            }
        }

        mainClock.autoAdvance = false

        val timeSeries = mutableListOf<IntSize>()
        size = 32.dp

        while (!animationIsDone) {
            mainClock.advanceTimeByFrame()
            waitForIdle()
            runOnUiThread { runWithoutImplicitWait { captureMotionTestValues(timeSeries) } }
        }
        assertThat(timeSeries)
            .containsExactly(
                IntSize(64, 64),
                IntSize(64, 64),
                IntSize(63, 63),
                IntSize(60, 60),
                IntSize(56, 56),
                IntSize(52, 52),
                IntSize(49, 49),
                IntSize(46, 46),
                IntSize(43, 43),
                IntSize(41, 41),
                IntSize(39, 39),
                IntSize(37, 37),
                IntSize(36, 36),
                IntSize(35, 35),
                IntSize(35, 35),
                IntSize(34, 34),
                IntSize(34, 34),
                IntSize(33, 33),
                IntSize(32, 32),
            )
            .inOrder()
    }

    @Test
    fun runWithoutImplicitWait_doesNotTriggerWaitForIdle() = runComposeUiTest {
        var isNodeVisible by mutableStateOf(true)

        setContent {
            if (isNodeVisible) {
                Box(modifier = Modifier.testTag("dummy_node"))
            }
        }

        isNodeVisible = false

        runOnUiThread {
            runWithoutImplicitWait {
                // In a normal context, onNodeWithTag() forces a waitForIdle(),
                // which would execute the pending recomposition.
                onNodeWithTag("dummy_node")
                    .assertExists(
                        "waitForIdle() was called internally, breaking the suppression contract!"
                    )
            }
        }
        // Calling onNodeWithTag outside runWithoutImplicitWait will now force idle.
        // This executes the pending recomposition, removing the node, and hence it won't exist
        // anymore.
        onNodeWithTag("dummy_node").assertDoesNotExist()
    }

    @Test
    fun runWithoutImplicitWait_reusesSemanticsNodesWithinBlock() = runComposeUiTest {
        setContent { Box(Modifier.testTag("node").size(10.dp)) }

        val (first, second) =
            runOnUiThread {
                runWithoutImplicitWait {
                    onNodeWithTag("node").fetchSemanticsNode() to
                        onNodeWithTag("node").fetchSemanticsNode()
                }
            }
        val nextBlock = runOnUiThread {
            runWithoutImplicitWait { onNodeWithTag("node").fetchSemanticsNode() }
        }
        val outside = onNodeWithTag("node").fetchSemanticsNode()

        // Queries in one block share the collected nodes, but every block collects them again
        assertThat(second).isSameInstanceAs(first)
        assertThat(nextBlock).isNotSameInstanceAs(first)
        // Outside the block, the nodes are collected again for every query
        assertThat(onNodeWithTag("node").fetchSemanticsNode()).isNotSameInstanceAs(outside)
    }

    @Test
    fun runWithoutImplicitWait_seesSemanticsChangedBetweenBlocks() = runComposeUiTest {
        var description by mutableStateOf("first")
        setContent {
            // The description is only read when the semantics are computed, so changing it doesn't
            // recompose, and thus doesn't need the clock to advance
            Box(Modifier.testTag("node").size(10.dp).semantics { contentDescription = description })
        }
        mainClock.autoAdvance = false
        waitForIdle()
        val startTime = mainClock.currentTime

        runOnUiThread {
            runWithoutImplicitWait { onNodeWithTag("node").assertContentDescriptionEquals("first") }
        }

        description = "second"
        onNodeWithTag("node").assertContentDescriptionEquals("second")
        assertThat(mainClock.currentTime).isEqualTo(startTime)

        // The clock didn't advance, so the nodes collected in the previous block must not be reused
        runOnUiThread {
            runWithoutImplicitWait {
                onNodeWithTag("node").assertContentDescriptionEquals("second")
            }
        }
    }

    @Test
    fun runWithoutImplicitWait_cachesMergedAndUnmergedTreeSeparately() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("parent").semantics(mergeDescendants = true) {}) {
                Box(Modifier.testTag("child").size(10.dp).semantics { contentDescription = "desc" })
            }
        }

        runOnUiThread {
            runWithoutImplicitWait {
                // In the merged tree, the description of the child is merged into its parent
                onNodeWithContentDescription("desc").assert(hasTestTag("parent"))
                onNodeWithContentDescription("desc", useUnmergedTree = true)
                    .assert(hasTestTag("child"))
                onNodeWithContentDescription("desc").assert(hasTestTag("parent"))
            }
        }
    }

    @Test
    fun runWithoutImplicitWait_cachesActiveAndDeactivatedNodesSeparately() = runComposeUiTest {
        var active by mutableStateOf(true)
        setContent { ReusableContentHost(active) { Box(Modifier.testTag("node").size(10.dp)) } }
        // A deactivated node has no semantics properties, so find it by its id
        val id = onNodeWithTag("node").fetchSemanticsNode().id
        val node = onNode(SemanticsMatcher("SemanticsId = $id") { it.id == id })

        active = false
        waitForIdle()

        runOnUiThread {
            runWithoutImplicitWait {
                // Regular queries skip deactivated nodes, assertIsDeactivated() includes them
                node.assertDoesNotExist()
                node.assertIsDeactivated()
            }
        }
    }

    @Test
    fun runWithoutImplicitWait_capturesLastSeenSemanticsOfNewInteraction() = runComposeUiTest {
        var showNode by mutableStateOf(true)
        setContent {
            if (showNode) {
                Box(Modifier.testTag("node").size(10.dp).semantics { contentDescription = "desc" })
            }
        }

        val node = onNodeWithTag("node")
        runOnUiThread { runWithoutImplicitWait { node.assertExists() } }
        showNode = false

        val error = assertFailsWith<AssertionError> { node.assertExists() }
        assertThat(error.message)
            .contains("The node is no longer in the tree, last known semantics:")
        assertThat(error.message).contains("ContentDescription = '[desc]'")
    }

    @Test
    fun runWithoutImplicitWait_refreshesLastSeenSemanticsCapturedBefore() = runComposeUiTest {
        var showNode by mutableStateOf(true)
        var description by mutableStateOf("outside")
        setContent {
            if (showNode) {
                val currentDescription = description
                Box(
                    Modifier.testTag("node").size(10.dp).semantics {
                        contentDescription = currentDescription
                    }
                )
            }
        }

        // Captures the semantics outside of the block
        val node = onNodeWithTag("node")
        node.assertExists()

        description = "inside"
        waitForIdle()
        // The semantics were captured before, so they are captured again inside the block
        runOnUiThread { runWithoutImplicitWait { node.assertExists() } }
        showNode = false

        val error = assertFailsWith<AssertionError> { node.assertExists() }
        assertThat(error.message)
            .contains("The node is no longer in the tree, last known semantics:")
        assertThat(error.message).contains("ContentDescription = '[inside]'")
    }

    /**
     * Illustrative implementation of a "sample the property values of the current frame" method.
     *
     * Motion tests do exactly that, just with more syntactic sugar 🍬.
     */
    private fun ComposeUiTest.captureMotionTestValues(fooSizeTimeSeries: MutableList<IntSize>) {
        // Capture a value and add to the time series.
        fooSizeTimeSeries.add(onNodeWithTag("foo").fetchSemanticsNode().size)

        repeat(100) {
            // simulation of capturing multiple properties.
            // For making the point, this just repeatedly captures the same property.
            onNodeWithTag("foo").fetchSemanticsNode().size
        }
    }
}
