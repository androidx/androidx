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

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.navigationevent.testing.TestNavigationEventHandler
import kotlin.test.Test

class DirectNavigationEventInputTest {
    @Test
    fun backStarted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun backProgressed_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        input.backProgressed(NavigationEvent())
        assertThat(handler.onBackProgressedInvocations).isEqualTo(1)
    }

    @Test
    fun backCancelled_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        input.backCancelled()
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun backCompleted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardStarted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardProgressed_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardProgressedInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        input.forwardProgressed(NavigationEvent())
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardCancelled_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        input.forwardCancelled()
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun forwardCompleted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        input.forwardCompleted()
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun navigationMethod_ifNameChanged_shouldFail() {
        // Android Studio relies on the exact names of these methods for Predictive Back in
        // Interactive Mode to work properly. This test must fail if any method is renamed.
        // Hardcoded string assertions are used so that IDE rename refactoring does not
        // automatically update the assertion strings, guaranteeing a test failure upon rename.

        assertThat(DirectNavigationEventInput::backStarted.name).isEqualTo("backStarted")
        assertThat(DirectNavigationEventInput::backProgressed.name).isEqualTo("backProgressed")
        assertThat(DirectNavigationEventInput::backCancelled.name).isEqualTo("backCancelled")
        assertThat(DirectNavigationEventInput::backCompleted.name).isEqualTo("backCompleted")
        assertThat(DirectNavigationEventInput::forwardStarted.name).isEqualTo("forwardStarted")
        assertThat(DirectNavigationEventInput::forwardProgressed.name)
            .isEqualTo("forwardProgressed")
        assertThat(DirectNavigationEventInput::forwardCancelled.name).isEqualTo("forwardCancelled")
        assertThat(DirectNavigationEventInput::forwardCompleted.name).isEqualTo("forwardCompleted")

        // Compile-time checks to ensure parameter types and counts are preserved.
        // We perform compile-time signature verification here rather than using reflection
        // because reflection is expensive, and we want to avoid its runtime overhead.
        // If these methods are changed (different types, additional parameters), the test
        // will fail to compile. Subclasses of NavigationEvent are permitted.
        assertSignature(DirectNavigationEventInput::backStarted)
        assertSignature(DirectNavigationEventInput::backProgressed)
        assertSignatureZeroArgs(DirectNavigationEventInput::backCancelled)
        assertSignatureZeroArgs(DirectNavigationEventInput::backCompleted)
        assertSignature(DirectNavigationEventInput::forwardStarted)
        assertSignature(DirectNavigationEventInput::forwardProgressed)
        assertSignatureZeroArgs(DirectNavigationEventInput::forwardCancelled)
        assertSignatureZeroArgs(DirectNavigationEventInput::forwardCompleted)
    }

    @Suppress("FINAL_UPPER_BOUND", "UNUSED_PARAMETER")
    private fun <T : NavigationEvent> assertSignature(
        func: (DirectNavigationEventInput, T) -> Unit
    ) {
        // Compile-time check only.
    }

    @Suppress("UNUSED_PARAMETER")
    private fun assertSignatureZeroArgs(func: (DirectNavigationEventInput) -> Unit) {
        // Compile-time check only.
    }
}
