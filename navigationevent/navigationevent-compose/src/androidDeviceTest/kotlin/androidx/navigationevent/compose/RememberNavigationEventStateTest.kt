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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class RememberNavigationEventStateTest {

    @get:Rule val rule = createComposeRule()

    private val owner = TestNavigationEventDispatcherOwner()
    private val dispatcher = owner.navigationEventDispatcher
    private val input = DirectNavigationEventInput().also { dispatcher.addInput(it) }

    @Test
    fun rememberNavigationEventState_whenInfoChanges_updatesStateFields() {
        // Initial values
        var current by mutableStateOf(TestInfo(id = 1))
        val back = mutableStateListOf<TestInfo>()
        val forward = mutableStateListOf<TestInfo>()

        // Expose the state instance to the test thread
        lateinit var stateRef: NavigationEventState<TestInfo>

        rule.setContent {
            stateRef =
                rememberNavigationEventState(
                    currentInfo = current,
                    backInfo = back,
                    forwardInfo = forward,
                )
        }

        // Verify initial fields reflect initial params
        rule.runOnIdle {
            assertThat(stateRef.currentInfo).isEqualTo(TestInfo(id = 1))
            assertThat(stateRef.backInfo).isEmpty()
            assertThat(stateRef.forwardInfo).isEmpty()
        }

        // Trigger recomposition with new values
        current = TestInfo(id = 2)
        back += TestInfo(id = 1)
        forward += TestInfo(id = 3)

        // Wait for recomposition + SideEffect to apply
        rule.waitForIdle()

        // Verify the state object's public fields were kept in sync
        rule.runOnIdle {
            assertThat(stateRef.currentInfo).isEqualTo(TestInfo(id = 2))
            assertThat(stateRef.backInfo).isEqualTo(listOf(TestInfo(id = 1)))
            assertThat(stateRef.forwardInfo).isEqualTo(listOf(TestInfo(id = 3)))
        }
    }

    @Test
    fun rememberNavigationEventState_transitionState_reflectsGestureLifecycle() {
        // Create and register a state + handler pair
        lateinit var state: NavigationEventState<TestInfo>
        rule.setContent {
            val childOwner = rememberNavigationEventDispatcherOwner(parent = owner)
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
                state = rememberNavigationEventState(currentInfo = TestInfo(id = 1))
                NavigationEventHandler(state)
            }
        }

        // Initially idle
        rule.runOnIdle { assertThat(state.transitionState is Idle).isTrue() }

        // Start a back gesture → should become InProgress
        input.backStarted(NavigationEvent())
        rule.runOnIdle { assertThat(state.transitionState is InProgress).isTrue() }

        // Progressing keeps it InProgress
        input.backProgressed(NavigationEvent())
        rule.runOnIdle { assertThat(state.transitionState is InProgress).isTrue() }

        // Cancelling returns to Idle
        input.backCancelled()
        rule.runOnIdle { assertThat(state.transitionState is Idle).isTrue() }

        // Starting again goes InProgress, then completing returns to Idle
        input.backStarted(NavigationEvent())
        rule.runOnIdle { assertThat(state.transitionState is InProgress).isTrue() }
        input.backCompleted()
        rule.runOnIdle { assertThat(state.transitionState is Idle).isTrue() }
    }

    @Test
    fun rememberNavigationEventState_whenAddingToMultipleHandlers_throws() {
        // Expect the second registration to fail
        assertThrows<IllegalArgumentException> {
            rule.setContent {
                val state = rememberNavigationEventState(TestInfo(id = 1))
                val childOwner1 = rememberNavigationEventDispatcherOwner(parent = owner)
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner1) {
                    NavigationEventHandler(state = state) { error("no-op") }
                    val childOwner2 = rememberNavigationEventDispatcherOwner(parent = owner)
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides childOwner2
                    ) {
                        // Attempt to add the same handler again in the same composition tree
                        NavigationEventHandler(state = state) { error("no-op") }
                    }
                }
            }
        }
    }

    // A simple data class for testing the info-based handler.
    private data class TestInfo(val id: Int = -1) : NavigationEventInfo()
}
