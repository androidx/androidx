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

package androidx.navigationevent.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.DirectNavigationEventInputHandler
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventCallback
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
internal class NavigationEventDispatcherOwnerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun navigationEventDispatcherOwner_asChild_whenInComposition_thenCreatesChildDispatcher() {
        val callback = TestNavigationEventCallback()
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                // This component is expected to create its own dispatcher instance and provide
                // it to its children. This ensures that navigation events are scoped to the
                // correct part of the UI tree, preventing child events from accidentally
                // being handled by a parent.
                NavigationEventDispatcherOwner {
                    childOwner = LocalNavigationEventDispatcherOwner.current!!
                }
            }
        }

        childOwner.navigationEventDispatcher.addCallback(callback)
        val inputHandler = DirectNavigationEventInputHandler(childOwner.navigationEventDispatcher)
        inputHandler.handleOnCompleted()

        // Verify that the child created its own, separate owner and dispatcher.
        assertThat(childOwner).isNotEqualTo(parentOwner)

        // Verify that the child's dispatcher was invoked.
        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun navigationEventDispatcherOwner_asChild_whenRemovedFromComposition_thenIsDisposed() {
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        var showContent by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                if (showContent) {
                    NavigationEventDispatcherOwner {
                        childOwner = LocalNavigationEventDispatcherOwner.current!!
                    }
                }
            }
        }

        // Toggling this state variable removes the NavigationEventDispatcherOwner from composition.
        // This is the trigger for the component's disposal logic.
        @Suppress("AssignedValueIsNeverRead")
        showContent = false
        rule.waitForIdle()

        assertThat(childOwner).isNotEqualTo(parentOwner)

        // Verify that attempting to use the disposed dispatcher now throws an
        // IllegalStateException, preventing use-after-dispose bugs.
        val inputHandler = DirectNavigationEventInputHandler(childOwner.navigationEventDispatcher)
        assertThrows<IllegalStateException> { inputHandler.handleOnCompleted() }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun navigationEventDispatcherOwner_asChild_whenEnabledStateChanges_thenUpdatesDispatcher() {
        val callback = TestNavigationEventCallback()
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        var enabled by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                // The 'enabled' parameter is a lambda to allow for dynamic updates.
                // This is a common pattern for controlling behavior based on state,
                // such as disabling back navigation during a loading operation.
                NavigationEventDispatcherOwner(enabled = enabled) {
                    childOwner = LocalNavigationEventDispatcherOwner.current!!
                }
            }
        }

        // Trigger a recomposition to disable the dispatcher.
        @Suppress("AssignedValueIsNeverRead")
        enabled = false
        rule.waitForIdle()

        // Attempt to dispatch an event while the dispatcher is disabled.
        childOwner.navigationEventDispatcher.addCallback(callback)
        val inputHandler = DirectNavigationEventInputHandler(childOwner.navigationEventDispatcher)
        inputHandler.handleOnCompleted()

        assertThat(childOwner).isNotEqualTo(parentOwner)
        assertThat(childOwner.navigationEventDispatcher.isEnabled).isFalse()

        // Verify that the callback was never invoked because the dispatcher was disabled.
        assertThat(callback.isEnabled).isFalse()
        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun navigationEventDispatcherOwner_asRoot_whenNoParent_thenCreatesRootDispatcher() {
        val callback = TestNavigationEventCallback()
        lateinit var rootOwner: NavigationEventDispatcherOwner

        rule.setContent {
            // By placing the owner at the root of the composition without a parent provider,
            // it's expected to create a new "root" dispatcher. This is the top-most
            // parent in a potential navigation hierarchy.
            NavigationEventDispatcherOwner(parent = null) {
                rootOwner = LocalNavigationEventDispatcherOwner.current!!
            }
        }

        // Verify the root dispatcher can operate independently.
        rootOwner.navigationEventDispatcher.addCallback(callback)
        val inputHandler = DirectNavigationEventInputHandler(rootOwner.navigationEventDispatcher)
        inputHandler.handleOnCompleted()

        assertThat(rootOwner.navigationEventDispatcher.isEnabled).isTrue()

        // Verify the callback was invoked correctly.
        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun navigationEventDispatcherOwner_asRoot_whenRemovedFromComposition_thenIsDisposed() {
        lateinit var rootOwner: NavigationEventDispatcherOwner
        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                // This owner is a root since it has no parent in the composition.
                NavigationEventDispatcherOwner(parent = null) {
                    rootOwner = LocalNavigationEventDispatcherOwner.current!!
                }
            }
        }

        // Toggling the state removes the owner from composition, triggering its disposal.
        @Suppress("AssignedValueIsNeverRead")
        showContent = false
        rule.waitForIdle()

        // Verify that using the disposed dispatcher throws the expected exception.
        // This prevents use-after-dispose bugs.
        val inputHandler = DirectNavigationEventInputHandler(rootOwner.navigationEventDispatcher)
        assertThrows<IllegalStateException> { inputHandler.handleOnCompleted() }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun navigationEventDispatcherOwner_asRoot_whenEnabledStateChanges_thenUpdatesDispatcher() {
        val callback = TestNavigationEventCallback()
        lateinit var rootOwner: NavigationEventDispatcherOwner
        var enabled by mutableStateOf(true)

        rule.setContent {
            // The enabled state should work just as well for a root dispatcher
            // as it does for a child.
            NavigationEventDispatcherOwner(parent = null, enabled = enabled) {
                rootOwner = LocalNavigationEventDispatcherOwner.current!!
            }
        }

        // Recompose with the dispatcher disabled.
        @Suppress("AssignedValueIsNeverRead")
        enabled = false
        rule.waitForIdle()

        // Attempt to dispatch an event while disabled.
        rootOwner.navigationEventDispatcher.addCallback(callback)
        val inputHandler = DirectNavigationEventInputHandler(rootOwner.navigationEventDispatcher)
        inputHandler.handleOnCompleted()

        assertThat(rootOwner.navigationEventDispatcher.isEnabled).isFalse()

        // Verify no callbacks were invoked because the dispatcher was off.
        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun navigationEventDispatcherOwner_asRoot_whenNoExplicitlyNullParent_thenThrows() {
        assertThrows<IllegalStateException> {
                rule.setContent {
                    // Attempt to create a dispatcher owner without a parent in the composition.
                    // This should fail because the default parent is non-nullable.
                    NavigationEventDispatcherOwner {}
                }
            }
            .hasMessageThat()
            .contains(
                "No NavigationEventDispatcherOwner provided in LocalNavigationEventDispatcherOwner"
            )
    }
}
