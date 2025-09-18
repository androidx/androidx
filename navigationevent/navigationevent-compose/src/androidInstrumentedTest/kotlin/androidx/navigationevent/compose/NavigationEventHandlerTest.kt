/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent.compose

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavigationEventHandlerTest {

    @get:Rule val rule = createComposeRule()

    private val owner = TestNavigationEventDispatcherOwner()
    private val dispatcher = owner.navigationEventDispatcher
    private val input = DirectNavigationEventInput().also { dispatcher.addInput(it) }

    @Test
    fun infoHandler_whenInfoChanges_providesUpdatedInfoToLambda() {
        // This test verifies that when `currentInfo` and `previousInfo` change,
        // the handler correctly uses the new values.
        var currentInfo by mutableStateOf(TestInfo(id = 1))
        val backInfo = mutableStateListOf<TestInfo>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = currentInfo,
                    backInfo = backInfo,
                    onBackCompleted = {},
                )
            }
        }

        // 1. Trigger with initial state.
        input.backStarted(NavigationEvent())
        rule.runOnIdle {
            @Suppress("UNCHECKED_CAST")
            val state = owner.navigationEventDispatcher.state.value as InProgress<TestInfo>

            assertThat(state.currentInfo).isEqualTo(TestInfo(id = 1))
            assertThat(state.backInfo).isEmpty()
        }

        // 2. Update the state, which triggers a recomposition.
        currentInfo = TestInfo(id = 2)
        backInfo += TestInfo(id = 1)
        rule.waitForIdle() // Wait for recomposition to apply the SideEffect.

        // 3. Verify the new, updated state is received.
        rule.runOnIdle {
            @Suppress("UNCHECKED_CAST")
            val state = owner.navigationEventDispatcher.state.value as InProgress<TestInfo>

            assertThat(state.currentInfo).isEqualTo(currentInfo)
            assertThat(state.backInfo).containsExactly(TestInfo(id = 1))
        }
    }

    @Test
    fun lambdaHandler_whenEventsAreDispatched_invokesCorrectLambdas() {
        val events = mutableListOf<String>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    onBackCancelled = { events += "cancelled" },
                    onBackCompleted = { events += "completed" },
                )
            }
        }

        // Test completed
        input.backStarted(NavigationEvent())
        input.backProgressed(NavigationEvent())
        input.backCompleted()
        rule.runOnIdle { assertThat(events).isEqualTo(listOf("completed")) }
        events.clear()

        // Test cancelled
        input.backStarted(NavigationEvent())
        input.backCancelled()
        rule.runOnIdle { assertThat(events).isEqualTo(listOf("cancelled")) }
    }

    @Test
    fun lambdaHandler_whenDisabled_invokesFallbackInsteadOfHandler() {
        var handlerCalled = false
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = false,
                    onBackCompleted = { handlerCalled = true },
                )
            }
        }

        input.backStarted(NavigationEvent())
        input.backCompleted()
        rule.runOnIdle {
            assertThat(handlerCalled).isFalse()
            assertThat(owner.fallbackOnBackPressedInvocations).isEqualTo(1)
        }
    }

    @Test
    fun lambdaHandler_whenEnabledStateChanges_togglesHandlerCorrectly() {
        val results = mutableListOf<String>()
        var isBackEnabled by mutableStateOf(true)

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = isBackEnabled,
                    onBackCompleted = { results += "handler" },
                )
            }
        }

        // Phase 1: Enabled, should call handler
        input.backStarted(NavigationEvent())
        input.backCompleted()
        rule.runOnIdle {
            assertThat(results).containsExactly("handler")
            assertThat(owner.fallbackOnBackPressedInvocations).isEqualTo(0)
        }

        // Phase 2: Disabled, should call fallback
        isBackEnabled = false
        results.clear()
        rule.runOnIdle {
            input.backStarted(NavigationEvent())
            input.backCompleted()
        }
        rule.runOnIdle {
            assertThat(results).isEmpty()
            assertThat(owner.fallbackOnBackPressedInvocations).isEqualTo(1)
        }

        // Phase 3: Re-enabled, should call handler again
        isBackEnabled = true
        rule.runOnIdle {
            input.backStarted(NavigationEvent())
            input.backCompleted()
        }
        rule.runOnIdle { assertThat(results).containsExactly("handler") }
    }

    @Test
    fun lambdaHandler_whenLambdaChanges_invokesNewLambda() {
        val results = mutableListOf<String>()
        var onBackCompleted by mutableStateOf({ results += "first" })

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = onBackCompleted,
                )
            }
        }

        // Call with the first lambda
        input.backCompleted()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(results).containsExactly("first") }

        // Change the lambda and call again
        onBackCompleted = { results += "second" }
        rule.waitForIdle()
        input.backCompleted()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(results).containsExactly("first", "second") }
    }

    @Test
    fun lambdaHandler_whenNested_invokesOnlyInnermost() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = { result += "parent" },
                )
                Button(onClick = { input.backStarted(NavigationEvent()) }) {
                    NavigationEventHandler(
                        currentInfo = TestInfo(),
                        isBackEnabled = true,
                        onBackCompleted = { result += "child" },
                    )
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        input.backCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("child")) }
    }

    @Test
    fun lambdaHandler_whenNestedChildIsDisabled_invokesParent() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = { result += "parent" },
                )
                Button(onClick = { input.backStarted(NavigationEvent()) }) {
                    NavigationEventHandler(
                        currentInfo = TestInfo(),
                        isBackEnabled = false,
                        onBackCompleted = { result += "child" },
                    )
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        input.backCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("parent")) }
    }

    @Test
    fun lambdaHandler_whenSiblingsExist_invokesLastComposed() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = { result += "first" },
                )
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = { result += "second" },
                )
            }
        }

        input.backStarted(NavigationEvent())
        input.backCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("second")) }
    }

    @Test
    fun lambdaHandler_whenLastSiblingIsDisabled_invokesPrevious() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = true,
                    onBackCompleted = { result += "first" },
                )
                NavigationEventHandler(
                    currentInfo = TestInfo(),
                    isBackEnabled = false,
                    onBackCompleted = { result += "second" },
                )
            }
        }

        input.backStarted(NavigationEvent())
        input.backCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("first")) }
    }

    // A simple data class for testing the info-based handler.
    private data class TestInfo(val id: Int = -1) : NavigationEventInfo
}
