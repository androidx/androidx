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

package androidx.ui.test

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.semantics.enabled
import androidx.ui.semantics.testTag
import androidx.ui.test.util.BoundaryNode
import androidx.ui.test.util.expectErrorMessageStartsWith
import androidx.ui.test.util.obfuscateNodesInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class PrintToStringTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun printToString_nothingFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith(
            "Failed: assertExists.\n" +
            "Reason: Expected exactly '1' node but could not find any node that satisfies:"
        ) {
            onNodeWithText("Oops").printToString()
        }
    }

    @Test
    fun printToString_one() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        val result = onNodeWithText("Hello")
            .printToString(maxDepth = 0)

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "Node #X at (X, X, X, X)px\n" +
                "Text = 'Hello'\n" +
                "Has 1 sibling")
    }

    @Test
    fun printToString_many() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        val result = onRoot()
            .onChildren()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "1) Node #X at (X, X, X, X)px\n" +
                "Text = 'Hello'\n" +
                "Has 1 sibling\n" +
                "2) Node #X at (X, X, X, X)px\n" +
                "Text = 'World'\n" +
                "Has 1 sibling")
    }

    @Test
    fun printHierarchy() {
        composeTestRule.setContent {
            Column(Modifier.semantics { this.enabled = true; this.testTag = "column" }) {
                Box(Modifier.semantics { this.enabled = true; this.testTag = "box" }) {
                    Button(onClick = {}) {
                        Text("Button")
                    }
                }
                Text("Hello")
            }
        }

        val result = onRoot()
            .printToString()

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "Node #X at (X, X, X, X)px\n" +
                " |-Node #X at (X, X, X, X)px, Tag: 'column'\n" +
                "   Enabled = 'true'\n" +
                "    |-Node #X at (X, X, X, X)px, Tag: 'box'\n" +
                "    | Enabled = 'true'\n" +
                "    |  |-Node #X at (X, X, X, X)px\n" +
                "    |    Enabled = 'true'\n" +
                "    |    OnClick = 'AccessibilityAction(label=null, action=Function0" +
                "<java.lang.Boolean>)'\n" +
                "    |    Text = 'Button'\n" +
                "    |    MergeDescendants = 'true'\n" +
                "    |-Node #X at (X, X, X, X)px\n" +
                "      Text = 'Hello'")
    }

    @Test
    fun printMultiple_withDepth() {
        composeTestRule.setContent {
            BoundaryNode("tag1") {
                BoundaryNode("tag11") {
                    BoundaryNode("tag111")
                }
            }
            BoundaryNode("tag2") {
                BoundaryNode("tag22") {
                    BoundaryNode("tag222")
                }
            }
        }

        val result = onRoot()
            .onChildren()
            .printToString(maxDepth = 1)

        assertThat(obfuscateNodesInfo(result)).isEqualTo("" +
                "1) Node #X at (X, X, X, X)px, Tag: 'tag1'\n" +
                " |-Node #X at (X, X, X, X)px, Tag: 'tag11'\n" +
                "   Has 1 child\n" +
                "2) Node #X at (X, X, X, X)px, Tag: 'tag2'\n" +
                " |-Node #X at (X, X, X, X)px, Tag: 'tag22'\n" +
                "   Has 1 child")
    }

    @Composable
    fun ComposeSimpleCase() {
        MaterialTheme {
            Column {
                Text("Hello")
                Text("World")
            }
        }
    }
}