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

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.testing.TestNavigationEventCallback
import kotlin.test.Test

class NavigationEventDispatcherTest {

    @Test
    fun dispatch_onStarted_thenOnStartedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnStarted(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onProgressed_thenOnProgressedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnProgressed(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(1)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCompleted_theOnCompletedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnCompleted()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCancelled_theOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnCancelled()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(1)
    }

    @Test
    fun removeCallback_whenNavigationIsInProgress_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onEventCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventCancelled = {
                    // Capture the count of 'started' invocations when 'cancelled' is called.
                    startedInvocationsAtCancelTime = this.startedInvocations
                }
            )
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnStarted(NavigationEvent())
        // Sanity check that navigation has started.
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.remove()

        // Assert that onEventCancelled was called once, and it happened after onEventStarted.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun dispatch_whenCallbackDisablesItself_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(onEventStarted = { isEnabled = false })
        dispatcher.addCallback(callback)

        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnCompleted()

        // The callback was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun setEnabled_whenNavigationIsInProgress_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnStarted(NavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.isEnabled = false

        // Assert that disabling the callback does not trigger a cancellation.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenCallbackRemovesItselfOnStarted_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    // Capture the 'cancelled' count before removing to ensure it was 0.
                    cancelledInvocationsAtStartTime = this.cancelledInvocations
                    remove()
                }
            )
        dispatcher.addCallback(callback)

        DirectNavigationEventInputHandler(dispatcher).handleOnStarted(NavigationEvent())

        // Assert that 'onEventStarted' was called.
        assertThat(callback.startedInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' was called from within 'onEventStarted'.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_whenAnotherNavigationIsInProgress_thenPreviousIsCancelled() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)

        // Start the first navigation.
        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnStarted(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Start the second navigation, which should cancel the first.
        inputHandler.handleOnStarted(NavigationEvent())

        // Assert callback1 was cancelled and callback2 was started.
        assertThat(callback1.cancelledInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)

        // Complete the second navigation.
        inputHandler.handleOnCompleted()
        assertThat(callback2.completedInvocations).isEqualTo(1)

        // Ensure callback1 was not affected by the completion of the second navigation.
        assertThat(callback1.completedInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_whenNavigationIsInProgress_thenNewCallbackIsIgnoredForCurrentNavigation() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnStarted(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        // Add a second callback while the first navigation is in progress.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Complete the first navigation.
        inputHandler.handleOnCompleted()

        // Assert that only the first callback was affected.
        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(0)
        assertThat(callback2.completedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnCompleted()

        // Assert that the second navigation was handled by the new top callback (callback2).
        assertThat(callback1.startedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback1.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_whenNoEnabledCallbacksExist_thenFallbackIsInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only callback, the fallback should be called.
        callback.isEnabled = false
        inputHandler.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun dispatch_whenOverlayCallbackExists_thenOverlaySupersedesDefault() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        DirectNavigationEventInputHandler(dispatcher).handleOnCompleted()

        // The overlay callback should handle the event, and the normal one should not.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenDisabledOverlayCallbackExists_thenDefaultCallbackIsInvoked() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        // The highest priority callback is disabled.
        overlayCallback.isEnabled = false

        DirectNavigationEventInputHandler(dispatcher).handleOnCompleted()

        // The event should skip the disabled overlay and be handled by the default.
        assertThat(overlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenCallbackIsRegisteredWithAnotherDispatcher_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addCallback(callback)

        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_whenCallbackIsAlreadyRegistered_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addCallback(callback)

        assertThrows<IllegalArgumentException> { dispatcher.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_whenMultipleOverlayCallbacksExist_thenLastAddedIsInvoked() {
        val dispatcher = NavigationEventDispatcher()
        val firstOverlayCallback = TestNavigationEventCallback()
        val secondOverlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)
        dispatcher.addCallback(firstOverlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(secondOverlayCallback, NavigationEventPriority.Overlay)

        DirectNavigationEventInputHandler(dispatcher).handleOnCompleted()

        // Only the last-added overlay callback should handle the event.
        assertThat(secondOverlayCallback.completedInvocations).isEqualTo(1)
        assertThat(firstOverlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenNoCallbacksExist_thenFallbackIsInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })

        // With no callbacks registered at all, the fallback should still work.
        DirectNavigationEventInputHandler(dispatcher).handleOnCompleted()

        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun setEnabled_whenDisabledCallbackIsReEnabled_thenItReceivesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Disable the callback and confirm it doesn't receive an event.
        callback.isEnabled = false
        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(0)

        // Re-enable the callback.
        callback.isEnabled = true
        inputHandler.handleOnCompleted()

        // It should now receive the event.
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_whenNoNavigationIsInProgress_thenDispatchesToTopCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Dispatching progress or completed without a start should still notify the top callback.
        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnProgressed(NavigationEvent())
        assertThat(callback.progressedInvocations).isEqualTo(1)

        inputHandler.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Ensure no cancellation was ever triggered.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_whenCallbackIsRemovedAndReAdded_thenBehavesAsNew() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        val inputHandler = DirectNavigationEventInputHandler(dispatcher)
        inputHandler.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Remove the callback.
        callback.remove()
        inputHandler.handleOnCompleted()
        // Invocations should not increase.
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Re-add the same callback instance. It should be treated as a new callback.
        dispatcher.addCallback(callback)
        inputHandler.handleOnCompleted()
        // Invocations should increase again.
        assertThat(callback.completedInvocations).isEqualTo(2)
    }
}
