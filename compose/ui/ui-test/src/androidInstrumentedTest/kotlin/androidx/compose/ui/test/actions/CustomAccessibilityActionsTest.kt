/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabelMatching
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class CustomAccessibilityActionsTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "tag"

    @Test
    fun performCustomAccessibilityActionLabelled_failsWhenNoNodeMatches() {
        rule.setContent {
            Box(
                Modifier.semantics {
                    customActions = listOf(CustomAccessibilityAction("action") { true })
                }
            )
        }

        val interaction = rule.onNodeWithTag(tag)
        val error =
            assertFailsWith<AssertionError> {
                interaction.performCustomAccessibilityActionWithLabel("action")
            }
        assertThat(error).hasMessageThat().contains("could not find any node that satisfies")
    }

    @Test
    fun performCustomAccessibilityActionLabelled_failsWhenNoActionMatches() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions = listOf(CustomAccessibilityAction("action") { true })
                }
            )
        }

        val error =
            assertFailsWith<AssertionError> {
                rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabel("not action")
            }
        assertThat(error)
            .hasMessageThat()
            .startsWith("No custom accessibility actions matched [label is \"not action\"]")
    }

    @Test
    fun performCustomAccessibilityActionLabelled_failsWhenMultipleActionsMatch() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("action") { true },
                            CustomAccessibilityAction("action") { true },
                        )
                }
            )
        }

        val error =
            assertFailsWith<AssertionError> {
                rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabel("action")
            }
        assertThat(error)
            .hasMessageThat()
            .startsWith(
                "Expected exactly one custom accessibility action to match [label is \"action\"], " +
                    "but found 2."
            )
    }

    @Test
    fun performCustomAccessibilityActionLabelled_invokesActionWhenExactlyOneActionMatches() {
        var fooInvocationCount = 0
        var barInvocationCount = 0
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("foo") {
                                fooInvocationCount++
                                true
                            },
                            CustomAccessibilityAction("bar") {
                                barInvocationCount++
                                true
                            },
                        )
                }
            )
        }

        rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabel("foo")

        assertThat(fooInvocationCount).isEqualTo(1)
        assertThat(barInvocationCount).isEqualTo(0)
    }

    @Test
    fun performCustomAccessibilityActionLabelled_doesntFailWhenActionReturnsFalse() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("action") { false },
                        )
                }
            )
        }

        rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabel("action")
    }

    @Test
    fun performCustomAccessibilityActionWhere_failsWhenNoNodeMatches() {
        rule.setContent {
            Box(
                Modifier.semantics {
                    customActions = listOf(CustomAccessibilityAction("action") { true })
                }
            )
        }

        val interaction = rule.onNodeWithTag(tag)
        val error =
            assertFailsWith<AssertionError> {
                interaction.performCustomAccessibilityActionWithLabelMatching("description") {
                    true
                }
            }
        assertThat(error).hasMessageThat().contains("could not find any node that satisfies")
    }

    @Test
    fun performCustomAccessibilityActionWhere_failsWhenNoActionMatches() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions = listOf(CustomAccessibilityAction("action") { true })
                }
            )
        }

        val error =
            assertFailsWith<AssertionError> {
                rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabelMatching(
                    "description"
                ) {
                    false
                }
            }
        assertThat(error)
            .hasMessageThat()
            .startsWith("No custom accessibility actions matched [description]")
    }

    @Test
    fun performCustomAccessibilityActionWhere_failsWhenMultipleActionsMatch() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("action") { true },
                            CustomAccessibilityAction("action") { true },
                        )
                }
            )
        }

        val error =
            assertFailsWith<AssertionError> {
                rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabelMatching(
                    "description"
                ) {
                    true
                }
            }
        assertThat(error)
            .hasMessageThat()
            .startsWith(
                "Expected exactly one custom accessibility action to match [description], " +
                    "but found 2."
            )
    }

    @Test
    fun performCustomAccessibilityActionWhere_invokesActionWhenExactlyOneActionMatches() {
        var fooInvocationCount = 0
        var barInvocationCount = 0
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("foo") {
                                fooInvocationCount++
                                true
                            },
                            CustomAccessibilityAction("bar") {
                                barInvocationCount++
                                true
                            },
                        )
                }
            )
        }

        rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabelMatching("description") {
            it == "foo"
        }

        assertThat(fooInvocationCount).isEqualTo(1)
        assertThat(barInvocationCount).isEqualTo(0)
    }

    @Test
    fun performCustomAccessibilityActionWhere_doesntFailWhenActionReturnsFalse() {
        rule.setContent {
            Box(
                Modifier.testTag(tag).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("action") { false },
                        )
                }
            )
        }

        rule.onNodeWithTag(tag).performCustomAccessibilityActionWithLabelMatching("description") {
            true
        }
    }
}
