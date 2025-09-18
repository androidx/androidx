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
@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.navigationevent

import androidx.annotation.MainThread
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.testing.TestNavigationEventHandler
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class NavigationEventDispatcherTest {

    // region Core API

    @Test
    fun dispatch_onBackStarted_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())

        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onForwardStarted_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardStarted(NavigationEvent())

        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onBackProgressed_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backProgressed(NavigationEvent())

        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(1)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onForwardProgressed_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardProgressed(NavigationEvent())

        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(1)
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onBackCompleted_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()

        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onForwardCompleted_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardCompleted()

        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(1)
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onBackCancelled_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCancelled()

        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_onForwardCancelled_sendsEventToHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardCancelled()

        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun removeHandler_duringInProgressBackNavigation_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onBackCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val handler =
            TestNavigationEventHandler(
                onBackCancelled = { startedInvocationsAtCancelTime = this.onBackStartedInvocations }
            )
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)

        // Removing a handler that is handling an in-progress navigation
        // must trigger a cancellation event on that handler first.
        handler.remove()

        // Assert that onBackCancelled was called once, and it happened after onBackStarted.
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun removeHandler_duringInProgressForwardNavigation_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onForwardCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val handler =
            TestNavigationEventHandler(
                onForwardCancelled = {
                    startedInvocationsAtCancelTime = this.onForwardStartedInvocations
                }
            )
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardStarted(NavigationEvent())
        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)

        // Removing a handler that is handling an in-progress navigation
        // must trigger a cancellation event on that handler first.
        handler.remove()

        // Assert that onForwardCancelled was called once, and it happened after onForwardStarted.
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun dispatch_handlerDisablesBack_doesNotSendCancellation() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler(onBackStarted = { isBackEnabled = false })
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())
        input.backCompleted()

        // The handler was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_handlerDisablesForward_doesNotSendCancellation() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler(onForwardStarted = { isForwardEnabled = false })
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardStarted(NavigationEvent())
        input.forwardCompleted()

        // The handler was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun setEnabled_duringInProgressNavigation_doesNotSendCancellation() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)

        // Disabling a handler should not automatically cancel an in-progress navigation.
        // This allows UI to be disabled without disrupting an ongoing user action.
        handler.isBackEnabled = false

        // Assert that disabling the handler does not trigger a cancellation.
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_handlerRemovesItselfOnBackStarted_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val handler =
            TestNavigationEventHandler(
                onBackStarted = {
                    cancelledInvocationsAtStartTime = this.onBackCancelledInvocations
                    remove()
                }
            )
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())

        // Assert that 'onBackStarted' was called.
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
        // Assert that 'onBackCancelled' was called from within 'onBackStarted'.
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)
        // Assert that 'onBackCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_handlerRemovesItselfOnForwardStarted_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val handler =
            TestNavigationEventHandler(
                onForwardStarted = {
                    cancelledInvocationsAtStartTime = this.onForwardCancelledInvocations
                    remove()
                }
            )
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardStarted(NavigationEvent())

        // Assert that 'onForwardStarted' was called.
        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)
        // Assert that 'onForwardCancelled' was called from within 'onForwardStarted'.
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(1)
        // Assert that 'onForwardCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_newBackNavigationDuringExisting_cancelsPrevious() {
        val dispatcher = NavigationEventDispatcher()
        val handler1 = TestNavigationEventHandler()
        dispatcher.addHandler(handler1)
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())
        assertThat(handler1.onBackStartedInvocations).isEqualTo(1)

        val handler2 = TestNavigationEventHandler()
        dispatcher.addHandler(handler2)

        // Starting a new navigation must implicitly cancel any gesture already in progress
        // to ensure a predictable state.
        input.backStarted(NavigationEvent())

        assertThat(handler1.onBackCancelledInvocations).isEqualTo(1)
        assertThat(handler2.onBackStartedInvocations).isEqualTo(1)

        input.backCompleted()
        assertThat(handler2.onBackCompletedInvocations).isEqualTo(1)

        // Verify the cancelled handler receives no further events.
        assertThat(handler1.onBackCompletedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_newForwardNavigationDuringExisting_cancelsPrevious() {
        val dispatcher = NavigationEventDispatcher()
        val handler1 = TestNavigationEventHandler()
        dispatcher.addHandler(handler1)
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.forwardStarted(NavigationEvent())
        assertThat(handler1.onForwardStartedInvocations).isEqualTo(1)

        val handler2 = TestNavigationEventHandler()
        dispatcher.addHandler(handler2)

        // Starting a new navigation must implicitly cancel any gesture already in progress
        // to ensure a predictable state.
        input.forwardStarted(NavigationEvent())

        assertThat(handler1.onForwardCancelledInvocations).isEqualTo(1)
        assertThat(handler2.onForwardStartedInvocations).isEqualTo(1)

        input.forwardCompleted()
        assertThat(handler2.onForwardCompletedInvocations).isEqualTo(1)

        // Verify the cancelled handler receives no further events.
        assertThat(handler1.onForwardCompletedInvocations).isEqualTo(0)
    }

    @Test
    fun addHandler_duringInProgressNavigation_ignoresNewHandlerForCurrentEvent() {
        val dispatcher = NavigationEventDispatcher()

        val handler1 = TestNavigationEventHandler()
        dispatcher.addHandler(handler1)
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(NavigationEvent())
        assertThat(handler1.onBackStartedInvocations).isEqualTo(1)

        // Add a new handler while a navigation is active.
        val handler2 = TestNavigationEventHandler()
        dispatcher.addHandler(handler2)

        // The dispatcher should be locked to the handler that started the navigation.
        // The new handler should not receive the completion event for the current navigation.
        input.backCompleted()

        assertThat(handler1.onBackCompletedInvocations).isEqualTo(1)
        assertThat(handler2.onBackStartedInvocations).isEqualTo(0)
        assertThat(handler2.onBackCompletedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        input.backStarted(NavigationEvent())
        input.backCompleted()

        // The second navigation should be handled by the new top handler (handler2).
        assertThat(handler1.onBackStartedInvocations).isEqualTo(1)
        assertThat(handler1.onBackCompletedInvocations).isEqualTo(1)
        assertThat(handler2.onBackStartedInvocations).isEqualTo(1)
        assertThat(handler2.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_withNoEnabledHandlers_invokesBackFallback() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only handler, the fallback should be triggered.
        handler.isBackEnabled = false
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun dispatch_withNoEnabledHandlers_doesNotInvokeBackFallbackForForward() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val handler = TestNavigationEventHandler()
        handler.isForwardEnabled = false
        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        // A forward navigation event should not trigger the back fallback.
        input.forwardCompleted()
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        assertThat(fallbackCalled).isFalse()
    }

    @Test
    fun dispatch_withOverlayHandler_prioritizesOverlay() {
        val dispatcher = NavigationEventDispatcher()
        val overlayHandler = TestNavigationEventHandler()
        val normalHandler = TestNavigationEventHandler()

        dispatcher.addHandler(overlayHandler, NavigationEventPriority.Overlay)
        dispatcher.addHandler(normalHandler, NavigationEventPriority.Default)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()

        // The overlay handler should handle the event, and the normal one should not.
        assertThat(overlayHandler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(normalHandler.onBackCompletedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_withDisabledOverlay_invokesDefaultHandler() {
        val dispatcher = NavigationEventDispatcher()
        val overlayHandler = TestNavigationEventHandler()
        val normalHandler = TestNavigationEventHandler()

        dispatcher.addHandler(overlayHandler, NavigationEventPriority.Overlay)
        dispatcher.addHandler(normalHandler, NavigationEventPriority.Default)

        // The highest priority handler is disabled.
        overlayHandler.isBackEnabled = false

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()

        // The event should skip the disabled overlay and be handled by the default.
        assertThat(overlayHandler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(normalHandler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun addHandler_toSecondDispatcher_throwsException() {
        val handler = TestNavigationEventHandler()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addHandler(handler)

        // A handler cannot be registered to more than one dispatcher at a time
        // to prevent ambiguous state and ownership issues.
        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addHandler(handler) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addHandler_withAlreadyRegisteredHandler_throwsException() {
        val handler = TestNavigationEventHandler()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addHandler(handler)

        // Adding the same handler instance twice is a developer error and should fail fast.
        assertThrows<IllegalArgumentException> { dispatcher.addHandler(handler) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addHandler_multipleOverlays_prioritizesLastAdded() {
        val dispatcher = NavigationEventDispatcher()
        val firstOverlayHandler = TestNavigationEventHandler()
        val secondOverlayHandler = TestNavigationEventHandler()
        val normalHandler = TestNavigationEventHandler()

        dispatcher.addHandler(normalHandler, NavigationEventPriority.Default)
        dispatcher.addHandler(firstOverlayHandler, NavigationEventPriority.Overlay)
        dispatcher.addHandler(secondOverlayHandler, NavigationEventPriority.Overlay)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()

        // Only the last-added overlay handler should handle the event.
        assertThat(secondOverlayHandler.onBackCompletedInvocations).isEqualTo(1)
        assertThat(firstOverlayHandler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(normalHandler.onBackCompletedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_withNoHandlers_invokesBackFallback() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })

        // With no handlers registered at all, the fallback should still work.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()

        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun setEnabled_onDisabledHandler_reenablesEventReceiving() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        // Disable the handler and confirm it doesn't receive an event.
        handler.isBackEnabled = false
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)

        // Re-enable the handler.
        handler.isBackEnabled = true
        input.backCompleted()

        // It should now receive the event.
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_withoutStart_sendsToTopHandler() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        // Dispatching progress or completed without a start should still notify the top handler.
        // This handles simple, non-gesture back events.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backProgressed(NavigationEvent())
        assertThat(handler.onBackProgressedInvocations).isEqualTo(1)

        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)

        // Ensure no cancellation was ever triggered.
        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
    }

    @Test
    fun addHandler_removedAndReadded_actsAsNew() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)

        // Remove the handler.
        handler.remove()
        input.backCompleted()
        // Invocations should not increase.
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)

        // Re-adding the same handler instance should treat it as a new registration.
        dispatcher.addHandler(handler)
        input.backCompleted()
        // Invocations should increase again.
        assertThat(handler.onBackCompletedInvocations).isEqualTo(2)
    }

    @Test
    fun addInput_onAdd_callsOnAttach() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        dispatcher.addInput(input)

        assertThat(input.addedInvocations).isEqualTo(1)
        assertThat(input.currentDispatcher).isEqualTo(dispatcher)
    }

    @Test
    fun addInput_twice_callsOnAttachOnce() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        dispatcher.addInput(input)
        // Adding the same input again should be idempotent and not re-trigger onAdded.
        dispatcher.addInput(input)

        assertThat(input.addedInvocations).isEqualTo(1)
    }

    @Test
    fun removeInput_onRemove_callsOnDetach() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        assertThat(input.addedInvocations).isEqualTo(1)

        dispatcher.removeInput(input)

        assertThat(input.removedInvocations).isEqualTo(1)
        assertThat(input.currentDispatcher).isNull()
    }

    @Test
    fun removeInput_whenNotRegistered_doesNotCallOnDetach() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        // Try to remove an input that was never added.
        dispatcher.removeInput(input)

        assertThat(input.removedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_onCall_detachesInputs() {
        val dispatcher = NavigationEventDispatcher()
        val input1 = TestNavigationEventInput()
        val input2 = TestNavigationEventInput()
        dispatcher.addInput(input1)
        dispatcher.addInput(input2)

        dispatcher.dispose()

        assertThat(input1.removedInvocations).isEqualTo(1)
        assertThat(input2.removedInvocations).isEqualTo(1)
        assertThat(input1.currentDispatcher).isNull()
        assertThat(input2.currentDispatcher).isNull()
    }

    @Test
    fun addInput_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        val input = TestNavigationEventInput()
        assertThrows<IllegalStateException> { dispatcher.addInput(input) }
            .hasMessageThat()
            .contains("This NavigationEventDispatcher has already been disposed")
    }

    @Test
    fun removeInput_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        val input = TestNavigationEventInput()
        assertThrows<IllegalStateException> { dispatcher.removeInput(input) }
            .hasMessageThat()
            .contains("This NavigationEventDispatcher has already been disposed")
    }

    @Test
    fun onHasEnabledHandlerChanged_onHandlerChange_notifiesInput() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        assertThat(input.onHasEnabledHandlersChangedInvocations).isEqualTo(0)

        // Adding a handler should trigger the onHasEnabledHandlersChanged listener.
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(input.onHasEnabledHandlersChangedInvocations).isEqualTo(1)

        // Disabling the handler should trigger it again.
        handler.isBackEnabled = false
        assertThat(input.onHasEnabledHandlersChangedInvocations).isEqualTo(2)
    }

    @Test
    fun onHasEnabledHandlerChanged_afterInputRemoved_doesNotNotify() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        val handler1 = TestNavigationEventHandler()
        dispatcher.addHandler(handler1)
        assertThat(input.onHasEnabledHandlersChangedInvocations).isEqualTo(1)

        dispatcher.removeInput(input)

        // Add another handler; the removed input should not be notified.
        val handler2 = TestNavigationEventHandler()
        dispatcher.addHandler(handler2)
        assertThat(input.onHasEnabledHandlersChangedInvocations).isEqualTo(1) // Unchanged
    }

    @Test
    fun dispose_onParent_detachesChildInputs() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        val parentInput = TestNavigationEventInput()
        val childInput = TestNavigationEventInput()
        parentDispatcher.addInput(parentInput)
        childDispatcher.addInput(childInput)

        // Disposing the parent should cascade to the child.
        parentDispatcher.dispose()

        assertThat(parentInput.removedInvocations).isEqualTo(1)
        assertThat(childInput.removedInvocations).isEqualTo(1)
        assertThat(parentInput.currentDispatcher).isNull()
        assertThat(childInput.currentDispatcher).isNull()
    }

    // endregion Core API

    // region Passive Listeners API

    @Test
    fun state_onMultipleHandlersAdded_reflectsLastAdded() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val homeHandler = TestNavigationEventHandler(currentInfo = HomeScreenInfo("home"))
        val detailsHandler = TestNavigationEventHandler(currentInfo = DetailsScreenInfo("details"))

        assertThat(dispatcher.state.value).isEqualTo(Idle(NotProvided))

        dispatcher.addHandler(homeHandler)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("home")))

        // Handlers are prioritized like a stack (LIFO), so adding a new one makes it active.
        dispatcher.addHandler(detailsHandler)
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_onSetInfoOnActiveHandler_updatesState() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler(currentInfo = HomeScreenInfo("initial"))
        dispatcher.addHandler(handler)

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("initial")))

        // Calling setInfo on the active handler should immediately update the dispatcher's state.
        handler.setInfo(currentInfo = HomeScreenInfo("updated"))

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("updated")))
    }

    @Test
    fun state_onSetInfoOnInactiveHandler_doesNotUpdateState() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val homeHandler = TestNavigationEventHandler(currentInfo = HomeScreenInfo("home"))
        val detailsHandler = TestNavigationEventHandler(currentInfo = DetailsScreenInfo("details"))
        dispatcher.addHandler(homeHandler)
        dispatcher.addHandler(detailsHandler)

        // The state should reflect the last-added (active) handler.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))

        // Calling setInfo on an inactive handler should NOT affect the global state.
        homeHandler.setInfo(currentInfo = HomeScreenInfo("home-updated"))

        // The state should remain unchanged because the update came from a non-active handler.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_onFullBackGestureLifecycle_transitionsToInProgressThenIdle() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val handlerInfo = HomeScreenInfo("home")
        val handler = TestNavigationEventHandler(currentInfo = handlerInfo)
        dispatcher.addHandler(handler)

        val startEvent = NavigationEvent(touchX = 0.1F)
        val progressEvent = NavigationEvent(touchX = 0.3f)

        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))

        // Starting a gesture should move the state to InProgress with the start event.
        input.backStarted(startEvent)
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(handlerInfo)
        assertThat(state.backInfo).isEmpty()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Progressing the gesture should keep it InProgress but update to the latest event.
        input.backProgressed(progressEvent)
        state = dispatcher.state.value as InProgress
        assertThat(state.latestEvent).isEqualTo(progressEvent)

        // Completing the gesture should return the state to Idle.
        input.backCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))
    }

    @Test
    fun state_onFullForwardGestureLifecycle_transitionsToInProgressThenIdle() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val handlerInfo = HomeScreenInfo("home")
        val handler = TestNavigationEventHandler(currentInfo = handlerInfo)
        dispatcher.addHandler(handler)

        val startEvent = NavigationEvent(touchX = 0.1F)
        val progressEvent = NavigationEvent(touchX = 0.3f)

        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))

        // Starting a gesture should move the state to InProgress with the start event.
        input.forwardStarted(startEvent)
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(handlerInfo)
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Progressing the gesture should keep it InProgress but update to the latest event.
        input.forwardProgressed(progressEvent)
        state = dispatcher.state.value as InProgress
        assertThat(state.latestEvent).isEqualTo(progressEvent)

        // Completing the gesture should return the state to Idle.
        input.forwardCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))
    }

    @Test
    fun state_onGestureCancelled_returnsToIdle() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val handlerInfo = HomeScreenInfo("home")
        val handler = TestNavigationEventHandler(currentInfo = handlerInfo)
        dispatcher.addHandler(handler)

        val startEvent = NavigationEvent()

        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))

        // Starting a gesture moves the state to InProgress.
        input.backStarted(startEvent)
        assertThat(dispatcher.state.value)
            .isEqualTo(InProgress(currentInfo = handlerInfo, latestEvent = startEvent))

        // Cancelling the gesture should also return the state to Idle.
        input.backCancelled()
        assertThat(dispatcher.state.value).isEqualTo(Idle(handlerInfo))
    }

    @Test
    fun inProgressState_onInfoUpdateDuringGesture_reflectsNewInfo() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val firstInfo = HomeScreenInfo("initial")
        val handler = TestNavigationEventHandler(currentInfo = firstInfo)
        dispatcher.addHandler(handler)

        val startEvent = NavigationEvent(touchX = 0.1F)

        input.backStarted(startEvent)

        // At the start, backInfo is empty.
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(firstInfo)
        assertThat(state.backInfo).isEmpty()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Update the info mid-gesture.
        val secondInfo = HomeScreenInfo("updated")
        handler.setInfo(currentInfo = secondInfo, backInfo = listOf(firstInfo))

        // The state should now reflect the updated info. The `backInfo` is now captured.
        state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(secondInfo)
        assertThat(state.backInfo).containsExactly(firstInfo)
        assertThat(state.latestEvent).isEqualTo(startEvent) // Event hasn't changed yet.

        // Complete the gesture.
        input.backCompleted()
        assertThat(dispatcher.state.value)
            .isEqualTo(Idle(currentInfo = secondInfo, backInfo = listOf(firstInfo)))
    }

    @Test
    fun inProgressState_onNewGesture_clearsPreviousInfo() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val initialInfo = HomeScreenInfo("initial")
        val handler = TestNavigationEventHandler(currentInfo = initialInfo)
        dispatcher.addHandler(handler)

        // FIRST GESTURE: Create a complex state.
        input.backStarted(NavigationEvent(touchX = 0.1f))
        handler.setInfo(currentInfo = HomeScreenInfo("updated"))
        input.backCompleted()

        // After the first gesture, the final state is Idle with the updated info.
        val finalInfo = HomeScreenInfo("updated")
        assertThat(dispatcher.state.value).isEqualTo(Idle(finalInfo))

        // SECOND GESTURE: Start a new gesture.
        val event2 = NavigationEvent(touchX = 0.3f)
        input.backStarted(event2)

        // When a new gesture starts, `backInfo` must be empty, not stale data
        // from a previous, completed gesture.
        val state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(finalInfo)
        assertThat(state.backInfo).isEmpty()
        assertThat(state.latestEvent).isEqualTo(event2)
    }

    @Test
    fun state_onDispatcherDisabled_fallsBackToSibling() {
        val dispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher = dispatcher)

        val handlerA = TestNavigationEventHandler(currentInfo = HomeScreenInfo("A"))
        dispatcher.addHandler(handlerA)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        val handlerB = TestNavigationEventHandler(currentInfo = DetailsScreenInfo("B"))
        childDispatcher.addHandler(handlerB)
        // Assert that state reflects handlerB, which was added last and is now active.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))

        // Disable the dispatcher that hosts the currently active handler.
        childDispatcher.isEnabled = false

        // The state should now fall back to the next-highest priority handler (handlerA).
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        // Re-enable the dispatcher.
        childDispatcher.isEnabled = true

        // The state should once again reflect handlerB.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))
    }

    @Test
    fun getState_withTypeFilter_emitsOnlyMatchingStates() =
        runTest(UnconfinedTestDispatcher()) {
            val dispatcher = NavigationEventDispatcher()
            val initialHomeInfo = HomeScreenInfo("initial")
            val homeHandler = TestNavigationEventHandler(currentInfo = HomeScreenInfo("home"))
            val detailsHandler =
                TestNavigationEventHandler(currentInfo = DetailsScreenInfo("details"))
            val collectedStates = mutableListOf<NavigationEventState<HomeScreenInfo>>()

            dispatcher
                .getState(backgroundScope, initialHomeInfo)
                .onEach { collectedStates.add(it) }
                .launchIn(backgroundScope)
            advanceUntilIdle()

            // The flow must start with the initial value provided.
            assertThat(collectedStates).hasSize(1)
            assertThat(collectedStates.last()).isEqualTo(Idle(initialHomeInfo))

            // A new state with a matching type should be collected.
            dispatcher.addHandler(homeHandler)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))

            // A state with a non-matching type should be filtered out and not collected.
            dispatcher.addHandler(detailsHandler)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)

            // When the active handler is removed, the state falls back to a matching type,
            // but since the info is the same as before, no new state is emitted.
            detailsHandler.remove()
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))
        }

    @Test
    fun getState_withNonMatchingType_emitsOnlyInitialInfo() =
        runTest(UnconfinedTestDispatcher()) {
            val dispatcher = NavigationEventDispatcher()
            val initialHomeInfo = HomeScreenInfo("initial")
            val detailsHandler =
                TestNavigationEventHandler(currentInfo = DetailsScreenInfo("details"))
            val collectedStates = mutableListOf<NavigationEventState<HomeScreenInfo>>()

            dispatcher
                .getState(backgroundScope, initialHomeInfo)
                .onEach { collectedStates.add(it) }
                .launchIn(backgroundScope)
            advanceUntilIdle()

            // The flow must start with its initial value.
            assertThat(collectedStates).hasSize(1)
            assertThat(collectedStates.first()).isEqualTo(Idle(initialHomeInfo))

            // Add a handler with a non-matching type.
            dispatcher.addHandler(detailsHandler)
            advanceUntilIdle()

            // The collector should not have emitted a new value.
            assertThat(collectedStates).hasSize(1)

            // Update the non-matching handler's info.
            detailsHandler.setInfo(currentInfo = DetailsScreenInfo("details-updated"))
            advanceUntilIdle()

            // The collector should still not have emitted a new value.
            assertThat(collectedStates).hasSize(1)
        }

    @Test
    fun progress_inIdleAndInProgress_returnsCorrectValue() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val handlerInfo = HomeScreenInfo("home")
        val handler = TestNavigationEventHandler(currentInfo = handlerInfo)
        dispatcher.addHandler(handler)

        // Before any gesture, the state is Idle and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)

        // Start a gesture.
        input.backStarted(NavigationEvent(progress = 0.1f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.1f)

        // InProgress state should reflect the event's progress.
        input.backProgressed(NavigationEvent(progress = 0.5f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.5f)

        // Complete the gesture.
        input.backCompleted()

        // After the gesture, the state is Idle again and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)
    }

    // endregion

    // region Hierarchy APIs

    @Test
    fun init_withParent_sharesHandlers() {
        val parentDispatcher = NavigationEventDispatcher()
        val parentHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)

        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val childHandler = TestNavigationEventHandler()
        childDispatcher.addHandler(childHandler)

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)

        // Handlers from child dispatchers are prioritized over their parents (LIFO).
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun init_withoutParent_hasIndependentHandlers() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher()

        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        // Dispatch an event through the parent.
        val event = NavigationEvent()
        val parentInput = TestNavigationEventInput()
        parentDispatcher.addInput(parentInput)
        parentInput.backStarted(event)

        // Only the parent's handler should be invoked.
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(1)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)

        // Dispatch an event through the child.
        val childInput = TestNavigationEventInput()
        childDispatcher.addInput(childInput)
        childInput.backStarted(event)

        // Only the child's handler should be invoked.
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(1)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun addHandler_toChild_isDispatchedViaParent() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val handler = TestNavigationEventHandler()

        childDispatcher.addHandler(handler)

        // Events dispatched from a parent should propagate to handlers in child dispatchers.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun addHandler_toParentThenChild_ordersLIFO() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()

        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        // The last-added handler (child's) should be invoked first.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun addHandler_multipleDispatchers_prioritizesLastAdded() {
        val parentDispatcher = NavigationEventDispatcher()
        val child1Dispatcher = NavigationEventDispatcher(parentDispatcher)
        val child2Dispatcher = NavigationEventDispatcher(parentDispatcher)

        val parentHandler = TestNavigationEventHandler()
        val childHandler1 = TestNavigationEventHandler()
        val childHandler2 = TestNavigationEventHandler()

        parentDispatcher.addHandler(parentHandler)
        child2Dispatcher.addHandler(childHandler2)
        child1Dispatcher.addHandler(childHandler1)

        // Handlers are processed in a LIFO manner across the entire hierarchy.
        // The handler from child1 was added last, so it gets the event.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler2.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler1.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun dispose_onChild_parentStillReceivesEvents() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        // Disposing a child should not affect its parent.
        childDispatcher.dispose()

        // Dispatching an event from the parent should now trigger the parent's handler,
        // as the child's (previously higher priority) handler is gone.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(1)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_onParent_cascadesAndDisablesChildren() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        parentDispatcher.dispose()

        // Attempting to use either dispatcher should now fail.
        val event = NavigationEvent()
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                parentDispatcher.addInput(input)
                input.backStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.backStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_onGrandparent_cascadesAndDisablesHierarchy() {
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        // Disposing the root dispatcher should disable the entire hierarchy.
        grandparentDispatcher.dispose()

        // Attempting to use any dispatcher in the hierarchy should fail.
        val event = NavigationEvent()
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                grandparentDispatcher.addInput(input)
                input.backStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                parentDispatcher.addInput(input)
                input.backStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.backStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun isEnabled_whenTrue_dispatchesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        dispatcher.isEnabled = true

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(event)
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenFalse_doesNotDispatchEvents() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler(isBackEnabled = true)
        dispatcher.addHandler(handler)

        dispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(event)
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_parentDisabled_disablesChildDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        // Disabling the parent should effectively disable the entire sub-hierarchy.
        parentDispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_childDisabled_doesNotDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        childDispatcher.isEnabled = false

        // Events sent to the child dispatcher should not be processed by any handler.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_childDisabled_parentStillDispatches() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        childDispatcher.isEnabled = false

        // Disabling a child should not affect the parent. Events sent directly to the
        // parent should be handled by the parent's handlers.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_parentReenabled_reenablesChildDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        parentDispatcher.isEnabled = false
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.backStarted(initialEvent)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)

        parentDispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.backStarted(reEnabledEvent)

        assertThat(childHandler.onBackStartedInvocations).isEqualTo(1)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_parentReenabled_childHandlerReceivesEvents() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        parentDispatcher.isEnabled = false
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.backStarted(initialEvent)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)

        parentDispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.backStarted(reEnabledEvent)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_grandparentDisabled_disablesGrandchildDispatch() {
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentHandler = TestNavigationEventHandler()
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()

        grandparentDispatcher.addHandler(grandparentHandler)
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        grandparentDispatcher.isEnabled = false

        // Disabling the grandparent should disable all descendants.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(grandparentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_grandparentDisabled_grandchildHandlerDoesNotReceiveEvents() {
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentHandler = TestNavigationEventHandler()
        val parentHandler = TestNavigationEventHandler()
        val childHandler = TestNavigationEventHandler()

        grandparentDispatcher.addHandler(grandparentHandler)
        parentDispatcher.addHandler(parentHandler)
        childDispatcher.addHandler(childHandler)

        grandparentDispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        grandparentDispatcher.addInput(input)
        input.backStarted(event)

        assertThat(grandparentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(parentHandler.onBackStartedInvocations).isEqualTo(0)
        assertThat(childHandler.onBackStartedInvocations).isEqualTo(0)
    }

    @Test
    fun handlerIsEnabled_whenDispatcherDisabled_doesNotReceiveEvents() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)
        val preDisableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(preDisableEvent)
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)

        dispatcher.isEnabled = false

        // An enabled handler on a disabled dispatcher should not receive events.
        val event = NavigationEvent()
        input.backStarted(event)

        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun handlerIsEnabled_whenDispatcherReenabled_receivesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)
        dispatcher.isEnabled = false

        val preEnableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.backStarted(preEnableEvent)
        assertThat(handler.onBackStartedInvocations).isEqualTo(0)

        dispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.backStarted(reEnabledEvent)

        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun addHandler_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> { dispatcher.addHandler(TestNavigationEventHandler()) }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnStarted_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.backStarted(NavigationEvent())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnProgressed_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.backProgressed(NavigationEvent())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnCompleted_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.backCompleted()
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnCancelled_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.backCancelled()
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Disposing an already-disposed dispatcher should fail.
        assertThrows<IllegalStateException> { dispatcher.dispose() }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun setEnabled_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> { dispatcher.isEnabled = false }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun setDisabled_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> { dispatcher.isEnabled = false }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    // endregion Hierarchy APIs

    // region Combined Info APIs
    @Test
    fun resolveBackInfo_withMultipleHandlers_combinesInfoInPriorityOrder() {
        val dispatcher = NavigationEventDispatcher()
        val homeInfo = HomeScreenInfo("home")
        val settingsInfo = DetailsScreenInfo("settings")
        val profileInfo = DetailsScreenInfo("profile")

        // A default handler with its own back stack.
        val defaultHandler =
            TestNavigationEventHandler(currentInfo = settingsInfo, backInfo = listOf(homeInfo))
        dispatcher.addHandler(defaultHandler, NavigationEventPriority.Default)

        // An overlay handler that should be prioritized.
        val overlayHandler =
            TestNavigationEventHandler(currentInfo = profileInfo, backInfo = listOf(settingsInfo))
        dispatcher.addHandler(overlayHandler, NavigationEventPriority.Overlay)

        // The combined back info should list the overlay's back info first,
        // followed by the default handler's back info.
        val combinedBackInfo = dispatcher.state.value.backInfo

        assertThat(combinedBackInfo).containsExactly(settingsInfo, homeInfo).inOrder()
    }

    @Test
    fun resolveBackInfo_withDisabledOverlay_ignoresItAndUsesDefault() {
        val dispatcher = NavigationEventDispatcher()
        val homeInfo = HomeScreenInfo("home")
        val settingsInfo = DetailsScreenInfo("settings")
        val profileInfo = DetailsScreenInfo("profile")

        // A default handler that is enabled.
        val defaultHandler =
            TestNavigationEventHandler(currentInfo = settingsInfo, backInfo = listOf(homeInfo))
        dispatcher.addHandler(defaultHandler, NavigationEventPriority.Default)

        // An overlay handler that is DISABLED.
        val overlayHandler =
            TestNavigationEventHandler(
                currentInfo = profileInfo,
                backInfo = listOf(settingsInfo),
                isBackEnabled = false, // This handler is disabled.
            )
        dispatcher.addHandler(overlayHandler, NavigationEventPriority.Overlay)

        // The combined back info should ignore the disabled overlay handler
        // and only contain the info from the enabled default handler.
        val combinedBackInfo = dispatcher.state.value.backInfo

        assertThat(combinedBackInfo).containsExactly(homeInfo)
    }

    @Test
    fun resolveBackInfo_withTwoDefaultHandlers_ordersLIFO() {
        val dispatcher = NavigationEventDispatcher()
        val infoA = HomeScreenInfo("A")
        val infoB = HomeScreenInfo("B")
        val infoC = DetailsScreenInfo("C")
        val infoD = DetailsScreenInfo("D")

        // First default handler added.
        val firstHandler = TestNavigationEventHandler(currentInfo = infoC, backInfo = listOf(infoA))
        dispatcher.addHandler(firstHandler, NavigationEventPriority.Default)

        // Second default handler added, which should have higher priority.
        val secondHandler =
            TestNavigationEventHandler(currentInfo = infoD, backInfo = listOf(infoB))
        dispatcher.addHandler(secondHandler, NavigationEventPriority.Default)

        // The combined back info should list the second handler's info first (LIFO),
        // followed by the first handler's info.
        val combinedBackInfo = dispatcher.state.value.backInfo

        assertThat(combinedBackInfo).containsExactly(infoB, infoA).inOrder()
    }

    @Test
    fun resolveBackInfo_whenOverlayAddedBeforeDefault_prioritizesOverlay() {
        val dispatcher = NavigationEventDispatcher()
        val defaultInfo = HomeScreenInfo("default")
        val overlayInfo = DetailsScreenInfo("overlay")

        // Add the overlay handler FIRST.
        val overlayHandler =
            TestNavigationEventHandler(currentInfo = overlayInfo, backInfo = listOf(overlayInfo))
        dispatcher.addHandler(overlayHandler, NavigationEventPriority.Overlay)

        // Add the default handler SECOND.
        val defaultHandler =
            TestNavigationEventHandler(currentInfo = defaultInfo, backInfo = listOf(defaultInfo))
        dispatcher.addHandler(defaultHandler, NavigationEventPriority.Default)

        // The combined back info must prioritize the overlay's info because its
        // priority level is checked before the default level.
        val combinedBackInfo = dispatcher.state.value.backInfo

        assertThat(combinedBackInfo).containsExactly(overlayInfo, defaultInfo).inOrder()
    }
    // endregion
}

/** A sealed interface for type-safe navigation information. */
sealed interface TestInfo : NavigationEventInfo

data class HomeScreenInfo(val id: String) : TestInfo

data class DetailsScreenInfo(val id: String) : TestInfo

/**
 * A test implementation of [NavigationEventInput] that records lifecycle events and invocation
 * counts.
 *
 * Use this class in tests to verify that `onAdded`, `onRemoved`, and `onHasEnabledHandlersChanged`
 * are called correctly. It provides helper methods to simulate dispatching both back and forward
 * navigation events.
 *
 * @param onAdded An optional lambda to execute when [onAdded] is called.
 * @param onRemoved An optional lambda to execute when [onRemoved] is called.
 * @param onHasEnabledHandlersChanged An optional lambda to execute when
 *   [onHasEnabledHandlersChanged] is called.
 */
private class TestNavigationEventInput(
    private val onAdded: (dispatcher: NavigationEventDispatcher) -> Unit = {},
    private val onRemoved: () -> Unit = {},
    private val onHasEnabledHandlersChanged: (hasEnabledHandlers: Boolean) -> Unit = {},
) : NavigationEventInput() {

    /** The number of times [onAdded] has been invoked. */
    var addedInvocations: Int = 0
        private set

    /** The number of times [onRemoved] has been invoked. */
    var removedInvocations: Int = 0
        private set

    /** The number of times [onHasEnabledHandlersChanged] has been invoked. */
    var onHasEnabledHandlersChangedInvocations: Int = 0
        private set

    /**
     * The most recently added [NavigationEventDispatcher].
     *
     * This is set by [onAdded] and cleared to `null` by [onRemoved].
     */
    var currentDispatcher: NavigationEventDispatcher? = null
        private set

    /**
     * Test helper to simulate the start of a back navigation event.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun backStarted(event: NavigationEvent = NavigationEvent()) {
        dispatchOnBackStarted(event)
    }

    /**
     * Test helper to simulate the progress of a back navigation event.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun backProgressed(event: NavigationEvent = NavigationEvent()) {
        dispatchOnBackProgressed(event)
    }

    /** Test helper to simulate the completion of a back navigation event. */
    @MainThread
    fun backCompleted() {
        dispatchOnBackCompleted()
    }

    /** Test helper to simulate the cancellation of a back navigation event. */
    @MainThread
    fun backCancelled() {
        dispatchOnBackCancelled()
    }

    /**
     * Test helper to simulate the start of a forward navigation event.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun forwardStarted(event: NavigationEvent = NavigationEvent()) {
        dispatchOnForwardStarted(event)
    }

    /**
     * Test helper to simulate the progress of a forward navigation event.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun forwardProgressed(event: NavigationEvent = NavigationEvent()) {
        dispatchOnForwardProgressed(event)
    }

    /** Test helper to simulate the completion of a forward navigation event. */
    @MainThread
    fun forwardCompleted() {
        dispatchOnForwardCompleted()
    }

    /** Test helper to simulate the cancellation of a forward navigation event. */
    @MainThread
    fun forwardCancelled() {
        dispatchOnForwardCancelled()
    }

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        addedInvocations++
        currentDispatcher = dispatcher
        onAdded.invoke(dispatcher)
    }

    override fun onRemoved() {
        currentDispatcher = null
        removedInvocations++
        onRemoved.invoke()
    }

    override fun onHasEnabledHandlerChanged(hasEnabledHandler: Boolean) {
        onHasEnabledHandlersChangedInvocations++
        onHasEnabledHandlersChanged.invoke(hasEnabledHandler)
    }
}
