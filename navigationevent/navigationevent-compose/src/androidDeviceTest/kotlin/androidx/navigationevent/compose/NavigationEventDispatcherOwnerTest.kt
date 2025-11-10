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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventHandler
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
    fun rememberNavigationEventDispatcherOwner_asChild_whenInComposition_thenCreatesChildDispatcher() {
        val handler = TestNavigationEventHandler()
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                // This component is expected to create its own dispatcher instance.
                // It picks up the parentOwner from the LocalNavigationEventDispatcherOwner.
                childOwner = rememberNavigationEventDispatcherOwner()
            }
        }

        childOwner.navigationEventDispatcher.addHandler(handler)
        val input = DirectNavigationEventInput()
        childOwner.navigationEventDispatcher.addInput(input)
        input.backCompleted()

        // Verify that the child created its own, separate owner and dispatcher.
        assertThat(childOwner).isNotEqualTo(parentOwner)

        // Verify that the child's dispatcher was invoked.
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asChild_whenRemovedFromComposition_thenIsDisposed() {
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        var showContent by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                if (showContent) {
                    childOwner = rememberNavigationEventDispatcherOwner()
                }
            }
        }

        // Toggling this state variable removes the owner from composition.
        // This is the trigger for the component's disposal logic.
        @Suppress("AssignedValueIsNeverRead")
        showContent = false
        rule.waitForIdle()

        assertThat(childOwner).isNotEqualTo(parentOwner)

        // Verify that attempting to use the disposed dispatcher now throws an
        // IllegalStateException, preventing use-after-dispose bugs.
        val input = DirectNavigationEventInput()
        assertThrows<IllegalStateException> { childOwner.navigationEventDispatcher.addInput(input) }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asChild_whenEnabledStateChanges_thenUpdatesDispatcher() {
        val handler = TestNavigationEventHandler()
        val parentOwner = TestNavigationEventDispatcherOwner()
        lateinit var childOwner: NavigationEventDispatcherOwner

        var enabled by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                // The 'enabled' parameter is a state value to allow for dynamic updates.
                childOwner = rememberNavigationEventDispatcherOwner(enabled = enabled)
            }
        }

        // Trigger a recomposition to disable the dispatcher.
        @Suppress("AssignedValueIsNeverRead")
        enabled = false
        rule.waitForIdle()

        // Attempt to dispatch an event while the dispatcher is disabled.
        childOwner.navigationEventDispatcher.addHandler(handler)
        val input = DirectNavigationEventInput()
        childOwner.navigationEventDispatcher.addInput(input)
        input.backCompleted()

        assertThat(childOwner).isNotEqualTo(parentOwner)
        assertThat(childOwner.navigationEventDispatcher.isEnabled).isFalse()

        // Verify that the handler was never invoked because the dispatcher was disabled.
        assertThat(handler.isBackEnabled).isFalse()
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_whenChildRecomposes_thenParentIsNotDisposed() {
        // This test simulates a configuration change or navigation event where a child
        // owner is disposed and replaced. It verifies that the parent's dispatcher
        // remains functional and is not affected by its child's lifecycle.
        lateinit var parentOwner: NavigationEventDispatcherOwner
        lateinit var childOwner1: NavigationEventDispatcherOwner
        var configuration by mutableStateOf(1)

        rule.setContent {
            // Create the parent owner
            parentOwner = rememberNavigationEventDispatcherOwner(parent = null)
            // Manually provide it to its children
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides parentOwner) {
                // Use a state variable to switch between children, simulating recomposition.
                if (configuration == 1) {
                    childOwner1 = rememberNavigationEventDispatcherOwner()
                } else {
                    // Composing a different child causes the first one to be disposed.
                    rememberNavigationEventDispatcherOwner()
                }
            }
        }
        rule.waitForIdle() // Let the initial composition complete.

        // Trigger a recomposition. This removes the first child from the composition,
        // which calls its `onDispose` block.
        configuration = 2
        rule.waitForIdle()

        // Verify the parent is still functional by using its dispatcher.
        val parentHandler = TestNavigationEventHandler()
        parentOwner.navigationEventDispatcher.addHandler(parentHandler)
        val input = DirectNavigationEventInput()
        parentOwner.navigationEventDispatcher.addInput(input)
        input.backCompleted()

        // The parent's handler should be invoked, proving it was not disposed.
        assertThat(parentHandler.onBackCompletedInvocations).isEqualTo(1)

        // Additionally, verify the original child owner was correctly disposed.
        assertThrows<IllegalStateException> {
                childOwner1.navigationEventDispatcher.addInput(DirectNavigationEventInput())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_whenParentChanges_thenOldDispatcherIsDisposed() {
        val parentOwner1 = TestNavigationEventDispatcherOwner()
        val parentOwner2 = TestNavigationEventDispatcherOwner()
        lateinit var childOwner1: NavigationEventDispatcherOwner

        var currentParent by mutableStateOf(parentOwner1)

        rule.setContent {
            // Provide the parent via a mutable state to simulate it changing.
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides currentParent) {
                // The child owner will use the currentParent from the local as its parent.
                val childOwner = rememberNavigationEventDispatcherOwner()
                // Capture the first instance of the child owner.
                if (currentParent == parentOwner1) {
                    childOwner1 = childOwner
                }
            }
        }
        rule.waitForIdle() // Let initial composition complete.

        // Trigger a recomposition with a new parent. This should cause the
        // original localDispatcher (inside childOwner1) to be disposed.
        currentParent = parentOwner2
        rule.waitForIdle()

        // Verify the original child owner was correctly disposed because its
        // parent changed, triggering the DisposableEffect's cleanup.
        assertThrows<IllegalStateException> {
                childOwner1.navigationEventDispatcher.addInput(DirectNavigationEventInput())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asRoot_whenNoParent_thenCreatesRootDispatcher() {
        val handler = TestNavigationEventHandler()
        lateinit var rootOwner: NavigationEventDispatcherOwner

        rule.setContent {
            // By passing parent = null, it's expected to create a new "root" dispatcher.
            rootOwner = rememberNavigationEventDispatcherOwner(parent = null)
        }

        // Verify the root dispatcher can operate independently.
        rootOwner.navigationEventDispatcher.addHandler(handler)
        val input = DirectNavigationEventInput()
        rootOwner.navigationEventDispatcher.addInput(input)
        input.backCompleted()

        assertThat(rootOwner.navigationEventDispatcher.isEnabled).isTrue()

        // Verify the handler was invoked correctly.
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asRoot_whenRemovedFromComposition_thenIsDisposed() {
        lateinit var rootOwner: NavigationEventDispatcherOwner
        var showContent by mutableStateOf(true)

        rule.setContent {
            if (showContent) {
                // This owner is a root since it has parent = null.
                rootOwner = rememberNavigationEventDispatcherOwner(parent = null)
            }
        }

        // Toggling the state removes the owner from composition, triggering its disposal.
        @Suppress("AssignedValueIsNeverRead")
        showContent = false
        rule.waitForIdle()

        // Verify that using the disposed dispatcher throws the expected exception.
        // This prevents use-after-dispose bugs.
        val input = DirectNavigationEventInput()
        assertThrows<IllegalStateException> { rootOwner.navigationEventDispatcher.addInput(input) }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asRoot_whenEnabledStateChanges_thenUpdatesDispatcher() {
        val handler = TestNavigationEventHandler()
        lateinit var rootOwner: NavigationEventDispatcherOwner
        var enabled by mutableStateOf(true)

        rule.setContent {
            // The enabled state should work just as well for a root dispatcher
            // as it does for a child.
            rootOwner = rememberNavigationEventDispatcherOwner(parent = null, enabled = enabled)
        }

        // Recompose with the dispatcher disabled.
        @Suppress("AssignedValueIsNeverRead")
        enabled = false
        rule.waitForIdle()

        // Attempt to dispatch an event while disabled.
        rootOwner.navigationEventDispatcher.addHandler(handler)
        val input = DirectNavigationEventInput()
        rootOwner.navigationEventDispatcher.addInput(input)
        input.backCompleted()

        assertThat(rootOwner.navigationEventDispatcher.isEnabled).isFalse()

        // Verify no handlers were invoked because the dispatcher was off.
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun rememberNavigationEventDispatcherOwner_asRoot_whenNoExplicitlyNullParent_thenThrows() {
        assertThrows<IllegalStateException> {
                rule.setContent {
                    // Attempt to create a dispatcher owner without a parent in the composition.
                    // This should fail because the default parent is non-nullable
                    // and LocalNavigationEventDispatcherOwner is not provided.
                    rememberNavigationEventDispatcherOwner()
                }
            }
            .hasMessageThat()
            .contains(
                "No NavigationEventDispatcherOwner provided in LocalNavigationEventDispatcherOwner"
            )
    }
}
