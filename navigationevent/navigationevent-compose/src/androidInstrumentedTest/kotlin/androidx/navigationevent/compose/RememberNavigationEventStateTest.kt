/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent.compose

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventState
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private data class CustomInfo1(val id: Int = 1) : NavigationEventInfo

private data class CustomInfo2(val id: Int = 2) : NavigationEventInfo

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class RememberNavigationEventStateTest {

    @get:Rule val rule = createComposeRule()

    private val owner = TestNavigationEventDispatcherOwner()
    private val dispatcher = owner.navigationEventDispatcher
    private val input = DirectNavigationEventInput().also { dispatcher.addInput(it) }

    @Test
    fun rememberState_whenStateIsIdle_returnsIdle() {
        lateinit var state: NavigationEventState<CustomInfo1>

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // Provide a handler with CustomInfo1 so the dispatcher has a current info.
                NavigationEventHandler(currentInfo = CustomInfo1(), onBackCompleted = {})
                state = rememberNavigationEventState(initialInfo = CustomInfo1())
            }
        }

        rule.runOnIdle { assertThat(state).isInstanceOf<Idle<CustomInfo1>>() }
    }

    @Test
    fun rememberState_whenStateIsInProgress_returnsInProgress() {
        lateinit var state: NavigationEventState<CustomInfo1>

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(currentInfo = CustomInfo1(), onBackCompleted = {})
                state = rememberNavigationEventState(initialInfo = CustomInfo1())
            }
        }

        // Start a gesture to move to InProgress<CustomInfo1>.
        input.backStarted(NavigationEvent())

        rule.runOnIdle { assertThat(state).isInstanceOf<InProgress<CustomInfo1>>() }
    }

    @Test
    fun rememberState_whenStateChanges_recomposesAndUpdates() {
        lateinit var state: NavigationEventState<CustomInfo1>

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(currentInfo = CustomInfo1(), onBackCompleted = {})
                state = rememberNavigationEventState(initialInfo = CustomInfo1())
            }
        }

        // 1. Initially Idle.
        rule.runOnIdle { assertThat(state).isInstanceOf<Idle<CustomInfo1>>() }

        // 2. Start gesture -> InProgress.
        input.backStarted(NavigationEvent())
        rule.waitForIdle()
        rule.runOnIdle { assertThat(state).isInstanceOf<InProgress<CustomInfo1>>() }

        // 3. Complete gesture -> Idle.
        input.backCompleted()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(state).isInstanceOf<Idle<CustomInfo1>>() }
    }

    @Test(expected = IllegalStateException::class)
    fun rememberState_whenNoDispatcherOwner_throwsIllegalStateException() {
        rule.setContent {
            // Missing NavigationEventDispatcherOwner -> should throw.
            rememberNavigationEventState(initialInfo = CustomInfo1())
        }
    }

    @Test
    fun rememberState_whenRequestingSpecificInfoType_matchesCorrectType() {
        lateinit var state: NavigationEventState<CustomInfo1>

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // Dispatcher uses CustomInfo1.
                NavigationEventHandler(currentInfo = CustomInfo1(), onBackCompleted = {})
                state = rememberNavigationEventState(initialInfo = CustomInfo1())
            }
        }

        input.backStarted(NavigationEvent())

        rule.runOnIdle {
            assertThat(state).isInstanceOf<InProgress<CustomInfo1>>()
            val inProgress = state as InProgress<CustomInfo1>
            assertThat(inProgress.currentInfo).isInstanceOf<CustomInfo1>()
        }
    }

    @Test
    fun rememberState_whenInfoTypeDoesNotMatch_remainsIdle() {
        lateinit var state: NavigationEventState<CustomInfo1>

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // Dispatcher uses CustomInfo2, but we observe CustomInfo1.
                NavigationEventHandler(currentInfo = CustomInfo2(), onBackCompleted = {})
                state = rememberNavigationEventState(initialInfo = CustomInfo1())
            }
        }

        // Emit events with CustomInfo2. Observer requests CustomInfo1, so it should ignore
        // these and remain Idle<CustomInfo1>.
        input.backStarted(NavigationEvent())

        rule.runOnIdle { assertThat(state).isInstanceOf<Idle<CustomInfo1>>() }
    }
}
