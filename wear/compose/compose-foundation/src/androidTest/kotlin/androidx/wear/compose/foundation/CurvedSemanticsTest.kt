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

package androidx.wear.compose.foundation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CurvedSemanticsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun semantics_set_correctly() {
        rule.setContent {
            CurvedLayout(modifier = Modifier.size(200.dp)) {
                basicCurvedText(
                    "base",
                    CurvedModifier.semantics {
                        contentDescription = "desc"
                        traversalIndex = 3.14f
                    },
                )
            }
        }

        rule.onNodeWithContentDescription("base").assertDoesNotExist()
        val traversalIndex =
            rule
                .onNodeWithContentDescription("desc")
                .fetchSemanticsNode()
                .config[SemanticsProperties.TraversalIndex]
        assertEquals(3.14f, traversalIndex)
    }

    @Test
    fun curved_content_description_defaults_to_text() {
        rule.setContent {
            CurvedLayout(modifier = Modifier.size(200.dp)) { basicCurvedText("text") }
        }

        rule.onNodeWithContentDescription("text").assertExists()
    }

    @Test
    fun curved_clearandsetsemantics_clears_default_content_description() {
        rule.setContent {
            CurvedLayout(modifier = Modifier.size(200.dp)) {
                basicCurvedText("text", CurvedModifier.clearAndSetSemantics {})
            }
        }

        // Content Description is cleared, does not default to the value of text.
        rule.onNodeWithContentDescription("text").assertDoesNotExist()
    }

    @Test
    fun noncurved_clearandsetsemantics_clears_default_content_description() {
        rule.setContent { BasicText("text", Modifier.clearAndSetSemantics {}) }

        rule.onNodeWithContentDescription("text").assertDoesNotExist()
    }

    @Test
    fun curved_semantics_then_clearandsetsemantics_first_wins() {
        rule.setContent {
            CurvedLayout(modifier = Modifier.size(200.dp)) {
                basicCurvedText(
                    "text",
                    CurvedModifier.semantics { contentDescription = "first" }
                        .clearAndSetSemantics { contentDescription = "second" },
                )
            }
        }

        rule.onNodeWithContentDescription("first").assertExists()
    }

    @Test
    fun noncurved_semantics_then_clearandsetsemantics_first_wins() {
        rule.setContent {
            BasicText(
                "text",
                Modifier.semantics { contentDescription = "first" }
                    .clearAndSetSemantics { contentDescription = "second" },
            )
        }

        rule.onNodeWithContentDescription("first").assertExists()
    }

    @Test
    fun curved_clearandsetsemantics_then_semantics_first_wins() {
        rule.setContent {
            CurvedLayout(modifier = Modifier.size(200.dp)) {
                basicCurvedText(
                    "text",
                    CurvedModifier.clearAndSetSemantics { contentDescription = "first" }
                        .semantics { contentDescription = "second" },
                )
            }
        }

        rule.onNodeWithContentDescription("first").assertExists()
    }

    @Test
    fun noncurved_clearandsetsemantics_then_semantics_first_wins() {
        rule.setContent {
            BasicText(
                "text",
                Modifier.clearAndSetSemantics { contentDescription = "first" }
                    .semantics { contentDescription = "second" },
            )
        }

        rule.onNodeWithContentDescription("first").assertExists()
    }
}
