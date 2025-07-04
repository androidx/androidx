/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.test.TestTextContextMenuDataInvoker
import androidx.compose.foundation.text.contextmenu.test.testTextContextMenuDataReader
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TextContextMenuModifierTraversalTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun whenNoModifiers_noInvocations() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent -> dataReadingContent() }

        assertThat(invocations).isEmpty()
    }

    @Test
    fun whenOneModifier_nodeTraversed() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Box(Modifier.appendTextContextMenuComponents { invocations += "node" }) {
                dataReadingContent()
            }
        }

        assertThat(invocations).containsExactly("node").inOrder()
    }

    @Test
    fun whenChainedModifier_nodesTraversedBottomToTop() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Box(
                Modifier.appendTextContextMenuComponents { invocations += "outer" }
                    .appendTextContextMenuComponents { invocations += "inner" }
            ) {
                dataReadingContent()
            }
        }

        assertThat(invocations).containsExactly("inner", "outer").inOrder()
    }

    @Test
    fun whenNestedModifier_nodesTraversedBottomToTop() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Box(Modifier.appendTextContextMenuComponents { invocations += "outer" }) {
                Box(Modifier.appendTextContextMenuComponents { invocations += "inner" }) {
                    dataReadingContent()
                }
            }
        }

        assertThat(invocations).containsExactly("inner", "outer").inOrder()
    }

    @Test
    fun whenMultipleNodes_doesNotTraverseNodesNotInAncestry() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Column(Modifier.appendTextContextMenuComponents { invocations += "outer" }) {
                Box(Modifier.appendTextContextMenuComponents { invocations += "inner1" }) {
                    dataReadingContent()
                }
                Box(Modifier.appendTextContextMenuComponents { invocations += "inner2" })
            }
        }

        assertThat(invocations).containsExactly("inner1", "outer").inOrder()
    }

    @Test
    fun whenDeeperChildNodes_doesNotTraverseNodesNotInAncestry() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Column(Modifier.appendTextContextMenuComponents { invocations += "outer" }) {
                dataReadingContent()
                Box(Modifier.appendTextContextMenuComponents { invocations += "inner1" })
                Box(Modifier.appendTextContextMenuComponents { invocations += "inner2" })
            }
        }

        assertThat(invocations).containsExactly("outer").inOrder()
    }

    @Test
    fun whenFilters_filtersAppliedAfterAllBuilders() {
        val invocations = mutableListOf<String>()
        setContentAndInvokeContextMenuData { dataReadingContent ->
            Box(
                Modifier.fakeFilterTextContextMenuComponents { invocations += "outer filter" }
                    .appendTextContextMenuComponents { invocations += "outer builder" }
                    .appendTextContextMenuComponents { invocations += "inner builder" }
                    .fakeFilterTextContextMenuComponents { invocations += "inner filter" }
                    .appendTextContextMenuComponents {
                        // Add an item otherwise the filters won't have anything to run on.
                        item("key", "label") { /* No action */ }
                    }
            ) {
                dataReadingContent()
            }
        }

        // We expect inner then outer builders, and then the filters in any order.
        assertThat(invocations)
            .containsExactly("inner builder", "outer builder", "inner filter", "outer filter")
        assertThat(invocations[0]).isEqualTo("inner builder")
        assertThat(invocations[1]).isEqualTo("outer builder")
        assertThat(invocations[2]).isAnyOf("inner filter", "outer filter")
        assertThat(invocations[3]).isAnyOf("inner filter", "outer filter")
    }

    private fun setContentAndInvokeContextMenuData(
        outerContent: @Composable (dataReadingContent: @Composable () -> Unit) -> Unit
    ) {
        val reader = TestTextContextMenuDataInvoker()
        rule.setContent { outerContent { Box(Modifier.testTextContextMenuDataReader(reader)) } }
        reader.invokeTraversal()
    }
}

/** Same as [filterTextContextMenuComponents] but puts defaults into the lambda args/returns. */
private fun Modifier.fakeFilterTextContextMenuComponents(block: () -> Unit): Modifier =
    filterTextContextMenuComponents {
        block()
        true
    }
