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

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnRootWithViewInteractionTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val parentAId = View.generateViewId()
    private val parentBId = View.generateViewId()
    private val deepContainerId = View.generateViewId()
    private val emptyContainerId = View.generateViewId()
    private val findersContainerId = View.generateViewId()

    @Test
    fun findNode_insideDefaultActivityHost() {
        composeTestRule.setContent { Text("Default Content") }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(android.R.id.content)))
            .onNode(hasText("Default Content"))
            .assertIsDisplayed()
    }

    @Test
    fun siblingIsolation_distinguishesBetweenParents() {
        setupCustomHierarchy { root ->
            val parentA = FrameLayout(root.context).apply { id = parentAId }
            val parentB = FrameLayout(root.context).apply { id = parentBId }

            parentA.addView(
                ComposeView(root.context).apply { setContent { Text("Duplicate Item") } }
            )
            parentB.addView(
                ComposeView(root.context).apply { setContent { Text("Duplicate Item") } }
            )

            root.addView(parentA)
            root.addView(parentB)
        }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(parentAId)))
            .onNode(hasText("Duplicate Item"))
            .assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Duplicate Item").assertCountEquals(2)
    }

    @Test
    fun deepNesting_findsNodeRecursive() {
        setupCustomHierarchy { root ->
            val wrapper1 = LinearLayout(root.context)
            val wrapper2 = LinearLayout(root.context)
            val targetContainer = FrameLayout(root.context).apply { id = deepContainerId }

            val composeView = ComposeView(root.context).apply { setContent { Text("Deep Secret") } }

            targetContainer.addView(composeView)
            wrapper2.addView(targetContainer)
            wrapper1.addView(wrapper2)
            root.addView(wrapper1)
        }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(deepContainerId)))
            .onNode(hasText("Deep Secret"))
            .assertIsDisplayed()
    }

    @Test
    fun multipleComposeRoots_inSingleView() {
        setupCustomHierarchy { root ->
            val parent =
                LinearLayout(root.context).apply {
                    id = parentAId
                    orientation = LinearLayout.VERTICAL
                }

            parent.addView(ComposeView(root.context).apply { setContent { Text("Part 1") } })
            parent.addView(ComposeView(root.context).apply { setContent { Text("Part 2") } })

            root.addView(parent)
        }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(parentAId)))
            .onNode(hasText("Part 1"))
            .assertIsDisplayed()

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(parentAId)))
            .onNode(hasText("Part 2"))
            .assertIsDisplayed()
    }

    @Test(expected = IllegalStateException::class)
    fun fail_whenViewContainsNoCompose() {
        setupCustomHierarchy { root ->
            val emptyContainer = FrameLayout(root.context).apply { id = emptyContainerId }
            emptyContainer.addView(TextView(root.context).apply { text = "Not Compose" })
            root.addView(emptyContainer)
        }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(emptyContainerId)))
            .onNode(hasText("Not Compose"))
            .assertIsNotDisplayed()
    }

    @Test(expected = NoMatchingViewException::class)
    fun fail_whenEspressoViewNotFound() {
        composeTestRule.setContent { Text("Visible") }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(9999)))
            .onNode(hasText("Visible"))
            .assertIsNotDisplayed()
    }

    @Test
    fun onParent_resolvesCorrectly() {
        setupNestedHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(parentAId)))
            .onNode(hasText("Child 1"))
            .onParent()
            .assert(hasTestTag("parent"))
    }

    @Test
    fun onChildren_resolvesAllChildNodes() {
        setupNestedHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(parentAId)))
            .onNode(hasTestTag("parent"))
            .onChildren()
            .assertCountEquals(2)
    }

    @Test
    fun onChildAt_resolvesSpecificChild() {
        setupNestedHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(parentAId)))
            .onNode(hasTestTag("parent"))
            .onChildAt(1)
            .assert(hasText("Child 2"))
    }

    @Test
    fun onSiblings_findsNeighbors() {
        setupNestedHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(parentAId)))
            .onNode(hasText("Child 1"))
            .onSiblings()
            .assertCountEquals(1)
            .onFirst()
            .assert(hasText("Child 2"))
    }

    @Test
    fun onAncestors_canTraverseUpward() {
        setupNestedHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(parentAId)))
            .onNode(hasText("Child 1"))
            .onAncestors()
            .filter(hasTestTag("grandparent"))
            .assertCountEquals(1)
    }

    @Test
    fun finders_onNodeWithTag() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onNodeWithTag("UniqueTag")
            .assertExists()
    }

    @Test
    fun finders_onAllNodesWithTag() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onAllNodesWithTag("CommonTag")
            .assertCountEquals(2)
    }

    @Test
    fun finders_onNodeWithText() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onNodeWithText("Unique Text")
            .assertIsDisplayed()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onNodeWithText("unique", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun finders_onAllNodesWithText() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onAllNodesWithText("Common Text")
            .assertCountEquals(2)
    }

    @Test
    fun finders_onNodeWithContentDescription() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onNodeWithContentDescription("Unique Description")
            .assertExists()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onNodeWithContentDescription("Unique", substring = true)
            .assertExists()
    }

    @Test
    fun finders_onAllNodesWithContentDescription() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onAllNodesWithContentDescription("Common Description")
            .assertCountEquals(2)
    }

    @Test
    fun finders_onRoot_resolvesScopedRoot() {
        setupFindersHierarchy()

        composeTestRule
            .onRootWithViewInteraction(onView(withId(findersContainerId)))
            .onRoot()
            .assertExists()
            .onChildren()
            .assertCountEquals(1)
    }

    @Test
    fun goneParent_reportsComposeContentAsNotDisplayed() {
        setupCustomHierarchy { root ->
            val parent =
                FrameLayout(root.context).apply {
                    id = parentAId
                    visibility = View.GONE
                }

            parent.addView(
                ComposeView(root.context).apply { setContent { Text("Hidden Content") } }
            )
            root.addView(parent)
        }

        composeTestRule
            .onRootWithViewInteraction(interaction = onView(withId(parentAId)))
            .onNode(hasText("Hidden Content"))
            .assertIsNotDisplayed()
    }

    private fun setupNestedHierarchy() {
        setupCustomHierarchy { root ->
            val container = FrameLayout(root.context).apply { id = parentAId }
            container.addView(
                ComposeView(root.context).apply {
                    setContent {
                        Box(Modifier.testTag("grandparent")) {
                            Column(Modifier.testTag("parent")) {
                                Text("Child 1")
                                Text("Child 2")
                            }
                        }
                    }
                }
            )
            root.addView(container)
        }
    }

    private fun setupFindersHierarchy() {
        setupCustomHierarchy { root ->
            val container = FrameLayout(root.context).apply { id = findersContainerId }

            container.addView(
                ComposeView(root.context).apply {
                    setContent {
                        Column(Modifier.testTag("root_column")) {
                            Text("Unique Text")

                            Text("Common Text")
                            Text("Common Text")

                            Box(Modifier.testTag("UniqueTag"))

                            Box(Modifier.testTag("CommonTag"))
                            Box(Modifier.testTag("CommonTag"))

                            Box(Modifier.semantics { contentDescription = "Unique Description" })

                            Box(Modifier.semantics { contentDescription = "Common Description" })
                            Box(Modifier.semantics { contentDescription = "Common Description" })
                        }
                    }
                }
            )
            root.addView(container)
        }
    }

    private fun setupCustomHierarchy(builder: (LinearLayout) -> Unit) {
        composeTestRule.activity.runOnUiThread {
            val root =
                LinearLayout(composeTestRule.activity).apply { orientation = LinearLayout.VERTICAL }
            builder(root)
            composeTestRule.activity.setContentView(root)
        }
        composeTestRule.waitForIdle()
    }
}
