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

package androidx.ui.semantics

import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.test.filters.MediumTest
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findAllByText
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.assert
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch

@MediumTest
@RunWith(JUnit4::class)
class SemanticsTests {
    private val TestTag = "semantics-test-tag"

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private fun executeUpdateBlocking(updateFunction: () -> Unit) {
        val latch = CountDownLatch(1)
        runOnUiThread {
            updateFunction()
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun removingMergedSubtree_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        composeTestRule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true).testTag(TestTag)) {
                if (showSubtree.value) {
                    Semantics(properties = {
                        accessibilityLabel = label
                    }) {
                        SimpleTestLayout {
                        }
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(label)

        runOnIdleCompose { showSubtree.value = false }

        findByTag(TestTag).assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)

        findAllByText(label).assertCountEquals(0)
    }

    @Test
    fun addingNewMergedNode_updatesSemantics() {
        val label = "foo"
        val value = "bar"
        val showNewNode = mutableStateOf(false)
        composeTestRule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true).testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                if (showNewNode.value) {
                    SimpleTestLayout(Modifier.semantics { accessibilityValue = value }) { }
                }
            }
        }

        findByTag(TestTag)
            .assertLabelEquals(label)
            .assertDoesNotHaveProperty(SemanticsProperties.AccessibilityValue)

        runOnIdleCompose { showNewNode.value = true }

        findByTag(TestTag)
            .assertLabelEquals(label)
            .assertValueEquals(value)
    }

    @Test
    fun removingSubtreeWithoutSemanticsAsTopNode_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                if (showSubtree.value) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                }
            }
        }

        findAllByText(label).assertCountEquals(1)

        runOnIdleCompose {
            showSubtree.value = false
        }

        findAllByText(label).assertCountEquals(0)
    }

    @Test
    fun changingStackedSemanticsComponent_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)
        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics {
                accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel }
            ) {}
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingStackedSemanticsComponent_notTopMost_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag("don't care")) {
                SimpleTestLayout(Modifier.testTag(TestTag).semantics {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel }
                ) {}
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowStackedLayoutNodes_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        composeTestRule.setContent {
            SimpleTestLayout {
                SimpleTestLayout {
                    SimpleTestLayout(Modifier.testTag(TestTag).semantics {
                        accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel }
                    ) {}
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowNodeMergedThroughBoundary_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics(mergeAllDescendants = true)) {
                SimpleTestLayout(Modifier.semantics {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel }
                ) {}
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun mergeAllDescendants_doesNotCrossLayoutNodesUpward() {
        val label = "label"
        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true)) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                }
            }
        }

        findByTag(TestTag).assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)
        findByText(label) // assert exists
    }

    @Test
    fun updateToNodeWithMultipleBoundaryChildren_updatesCorrectly() {
        // This test reproduced a bug that caused a ConcurrentModificationException when
        // detaching SemanticsNodes

        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics {
                accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel }
            ) {
                SimpleTestLayout(Modifier.semantics { }) { }
                SimpleTestLayout(Modifier.semantics { }) { }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_doesNotReplaceNodesBelow() {
        // Regression test for b/148606417
        var nodeCount = 0
        val beforeLabel = "before"
        val afterLabel = "after"

        // Do different things in an attempt to defeat a sufficiently clever compiler
        val beforeAction = { println("this never gets called") }
        val afterAction = { println("neither does this") }

        val isAfter = mutableStateOf(false)

        composeTestRule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics {
                accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                onClick(action = {
                    if (isAfter.value) afterAction() else beforeAction()
                    return@onClick true
                })
            }) {
                SimpleTestLayout {
                    remember { nodeCount++ }
                }
            }
        }

        // This isn't the important part, just makes sure everything is behaving as expected
        findByTag(TestTag).assertLabelEquals(beforeLabel)
        assertThat(nodeCount).isEqualTo(1)

        runOnIdleCompose { isAfter.value = true }

        // Make sure everything is still behaving as expected
        findByTag(TestTag).assertLabelEquals(afterLabel)
        // This is the important part: make sure we didn't replace the identity due to unwanted
        // pivotal properties
        assertThat(nodeCount).isEqualTo(1)
    }
}

private fun SemanticsNodeInteraction.assertDoesNotHaveProperty(property: SemanticsPropertyKey<*>) {
    assert(SemanticsMatcher.keyNotDefined(property))
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably.  Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeables = measurables.map {
                it.measure(constraints)
            }
            val (width, height) = with(placeables.filterNotNull()) {
                Pair(
                    max(
                        maxBy { it.width.value }?.width ?: IntPx.Zero,
                        constraints.minWidth
                    ),
                    max(
                        maxBy { it.height.value }?.height ?: IntPx.Zero,
                        constraints.minHeight
                    )
                )
            }
            layout(width, height) {
                for (placeable in placeables) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
    }
}
