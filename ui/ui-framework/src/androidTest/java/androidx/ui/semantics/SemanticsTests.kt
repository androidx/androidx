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
import androidx.test.filters.MediumTest
import androidx.ui.core.Layout
import androidx.ui.core.test.ValueModel
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.SemanticsPredicate
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findAllByText
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.verify
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.max
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
        composeTestRule.runOnUiThread {
            updateFunction()
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun removingMergedSubtree_updatesSemantics() {
        val label = "foo"
        val showSubtree = ValueModel(true)
        composeTestRule.setContent {
            Semantics(container = true, properties = {
                testTag = TestTag
            }) {
                SimpleTestLayout {
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
        }

        findByTag(TestTag).assertLabelEquals(label)

        composeTestRule.runOnIdleCompose { showSubtree.value = false }

        findByTag(TestTag).assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)

        findAllByText(label).assertCountEquals(0)
    }

    @Test
    fun addingNewMergedNode_updatesSemantics() {
        val label = "foo"
        val value = "bar"
        val showNewNode = ValueModel(false)
        composeTestRule.setContent {
            Semantics(container = true, properties = {
                testTag = TestTag
            }) {
                SimpleTestLayout {

                    Semantics(properties = {
                        accessibilityLabel = label
                    }) {
                        SimpleTestLayout {
                        }
                    }
                    if (showNewNode.value) {
                        Semantics(properties = {
                            accessibilityValue = value
                        }) {
                            SimpleTestLayout {
                            }
                        }
                    }
                }
            }
        }

        findByTag(TestTag)
            .assertLabelEquals(label)
            .assertDoesNotHaveProperty(SemanticsProperties.AccessibilityValue)

        composeTestRule.runOnIdleCompose { showNewNode.value = true }

        findByTag(TestTag)
            .assertLabelEquals(label)
            .assertValueEquals(value)
    }

    @Test
    fun removingSubtreeWithoutSemanticsAsTopNode_updatesSemantics() {
        val label = "foo"
        val showSubtree = ValueModel(true)
        composeTestRule.setContent {
            Semantics(container = true, properties = {
                testTag = TestTag
            }) {
                SimpleTestLayout {
                    if (showSubtree.value) {
                        Semantics(container = true, properties = {
                            accessibilityLabel = "foo"
                        }) {
                            SimpleTestLayout {
                            }
                        }
                    }
                }
            }
        }

        findAllByText(label).assertCountEquals(1)

        composeTestRule.runOnIdleCompose {
            showSubtree.value = false
        }

        findAllByText(label).assertCountEquals(0)
    }

    @Test
    fun changingStackedSemanticsComponent_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = ValueModel(false)
        composeTestRule.setContent {
            Semantics(container = true, properties = {
                testTag = TestTag
            }) {
                Semantics(properties = {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                }) {
                    SimpleTestLayout {
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        composeTestRule.runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingStackedSemanticsComponent_notTopMost_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = ValueModel(false)

        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = "don't care" }) {
                SimpleTestLayout {
                    Semantics(container = true, properties = {
                        testTag = TestTag
                    }) {
                        Semantics(properties = {
                            accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                        }) {
                            SimpleTestLayout {
                            }
                        }
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        composeTestRule.runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowStackedLayoutNodes_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = ValueModel(false)

        composeTestRule.setContent {
            SimpleTestLayout {
                SimpleTestLayout {
                    Semantics(container = true,
                        properties = {
                            testTag = TestTag
                            accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                        }) {
                        SimpleTestLayout {}
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        composeTestRule.runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowNodeMergedThroughBoundary_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = ValueModel(false)

        composeTestRule.setContent {
            Semantics(properties = { testTag = TestTag }) {
                Semantics(container = true, mergeAllDescendants = true) {
                    SimpleTestLayout {
                        Semantics(container = true, properties = {
                            accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                        }) {
                            SimpleTestLayout {}
                        }
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        composeTestRule.runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun mergeAllDescendants_doesNotCrossLayoutNodesUpward() {
        val label = "label"
        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = TestTag }) {
                SimpleTestLayout {
                    Semantics(container = true, mergeAllDescendants = true) {
                        SimpleTestLayout {
                            Semantics(container = true, properties = {
                                accessibilityLabel = label
                            }) {
                                SimpleTestLayout {}
                            }
                        }
                    }
                }
            }
        }

        findByTag(TestTag).assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)
        findByText(label) // assert exists
    }

    @Test(expected = IllegalArgumentException::class)
    fun mergeAllDescendants_withoutBoundary_throws() {
        composeTestRule.setContent {
            // This is more complicated than required, but makes sure we get the case
            // where it's merged down into a node that does have a boundary, as it's still
            // not allowed even in this case
            Semantics(mergeAllDescendants = true) {
                SimpleTestLayout {
                    Semantics(container = true) {
                        SimpleTestLayout { }
                    }
                }
            }
        }
    }

    @Test
    fun updateToNodeWithMultipleBoundaryChildren_updatesCorrectly() {
        // This test reproduced a bug that caused a ConcurrentModificationException when
        // detaching SemanticsNodes

        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = ValueModel(false)

        composeTestRule.setContent {
            Semantics(container = true, properties = {
                accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                testTag = TestTag
            }) {
                SimpleTestLayout {
                    Semantics(container = true) {
                        SimpleTestLayout { }
                    }
                    Semantics(container = true) {
                        SimpleTestLayout { }
                    }
                }
            }
        }

        findByTag(TestTag).assertLabelEquals(beforeLabel)

        composeTestRule.runOnIdleCompose { isAfter.value = true }

        findByTag(TestTag).assertLabelEquals(afterLabel)
    }
}

private fun SemanticsNodeInteraction.assertDoesNotHaveProperty(property: SemanticsPropertyKey<*>) {
    verify(SemanticsPredicate.keyNotDefined(property))
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably.  Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(children: @Composable() () -> Unit) {
    Layout(children = children) { measurables, constraints ->
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
