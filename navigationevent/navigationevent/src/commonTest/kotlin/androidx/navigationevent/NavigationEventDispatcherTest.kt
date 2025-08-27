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
import androidx.navigationevent.testing.TestNavigationEventCallback
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
    fun dispatch_onStarted_sendsEventToCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onProgressed_sendsEventToCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.progress(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(1)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCompleted_sendsEventToCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCancelled_sendsEventToCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.cancel()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(1)
    }

    @Test
    fun removeCallback_duringInProgressNavigation_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onEventCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventCancelled = { startedInvocationsAtCancelTime = this.startedInvocations }
            )
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        // Removing a callback that is handling an in-progress navigation
        // must trigger a cancellation event on that callback first.
        callback.remove()

        // Assert that onEventCancelled was called once, and it happened after onEventStarted.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun dispatch_callbackDisablesItself_doesNotSendCancellation() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(onEventStarted = { isEnabled = false })
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())
        input.complete()

        // The callback was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun setEnabled_duringInProgressNavigation_doesNotSendCancellation() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        // Disabling a callback should not automatically cancel an in-progress navigation.
        // This allows UI to be disabled without disrupting an ongoing user action.
        callback.isEnabled = false

        // Assert that disabling the callback does not trigger a cancellation.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_callbackRemovesItselfOnStarted_sendsCancellation() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    cancelledInvocationsAtStartTime = this.cancelledInvocations
                    remove()
                }
            )
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())

        // Assert that 'onEventStarted' was called.
        assertThat(callback.startedInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' was called from within 'onEventStarted'.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_newNavigationDuringExisting_cancelsPrevious() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Starting a new navigation must implicitly cancel any gesture already in progress
        // to ensure a predictable state.
        input.start(NavigationEvent())

        assertThat(callback1.cancelledInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)

        input.complete()
        assertThat(callback2.completedInvocations).isEqualTo(1)

        // Verify the cancelled callback receives no further events.
        assertThat(callback1.completedInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_duringInProgressNavigation_ignoresNewCallbackForCurrentEvent() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        // Add a new callback while a navigation is active.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // The dispatcher should be locked to the callback that started the navigation.
        // The new callback should not receive the completion event for the current navigation.
        input.complete()

        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(0)
        assertThat(callback2.completedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        input.start(NavigationEvent())
        input.complete()

        // The second navigation should be handled by the new top callback (callback2).
        assertThat(callback1.startedInvocations).isEqualTo(1)
        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_withNoEnabledCallbacks_invokesFallback() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only callback, the fallback should be triggered.
        callback.isEnabled = false
        input.complete()
        assertThat(callback.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun dispatch_withOverlayCallback_prioritizesOverlay() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()

        // The overlay callback should handle the event, and the normal one should not.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_withDisabledOverlay_invokesDefaultCallback() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        // The highest priority callback is disabled.
        overlayCallback.isEnabled = false

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()

        // The event should skip the disabled overlay and be handled by the default.
        assertThat(overlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_toSecondDispatcher_throwsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addCallback(callback)

        // A callback cannot be registered to more than one dispatcher at a time
        // to prevent ambiguous state and ownership issues.
        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_withAlreadyRegisteredCallback_throwsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addCallback(callback)

        // Adding the same callback instance twice is a developer error and should fail fast.
        assertThrows<IllegalArgumentException> { dispatcher.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_multipleOverlays_prioritizesLastAdded() {
        val dispatcher = NavigationEventDispatcher()
        val firstOverlayCallback = TestNavigationEventCallback()
        val secondOverlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)
        dispatcher.addCallback(firstOverlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(secondOverlayCallback, NavigationEventPriority.Overlay)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()

        // Only the last-added overlay callback should handle the event.
        assertThat(secondOverlayCallback.completedInvocations).isEqualTo(1)
        assertThat(firstOverlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_withNoCallbacks_invokesFallback() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })

        // With no callbacks registered at all, the fallback should still work.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()

        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun setEnabled_onDisabledCallback_reenablesEventReceiving() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Disable the callback and confirm it doesn't receive an event.
        callback.isEnabled = false
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()
        assertThat(callback.completedInvocations).isEqualTo(0)

        // Re-enable the callback.
        callback.isEnabled = true
        input.complete()

        // It should now receive the event.
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_withoutStart_sendsToTopCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Dispatching progress or completed without a start should still notify the top callback.
        // This handles simple, non-gesture back events.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.progress(NavigationEvent())
        assertThat(callback.progressedInvocations).isEqualTo(1)

        input.complete()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Ensure no cancellation was ever triggered.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_removedAndReadded_actsAsNew() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.complete()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Remove the callback.
        callback.remove()
        input.complete()
        // Invocations should not increase.
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Re-adding the same callback instance should treat it as a new registration.
        dispatcher.addCallback(callback)
        input.complete()
        // Invocations should increase again.
        assertThat(callback.completedInvocations).isEqualTo(2)
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
    fun onHasEnabledCallbacksChanged_onCallbackChange_notifiesInput() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(0)

        // Adding a callback should trigger the onHasEnabledCallbacksChanged listener.
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1)

        // Disabling the callback should trigger it again.
        callback.isEnabled = false
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(2)
    }

    @Test
    fun onHasEnabledCallbacksChanged_afterInputRemoved_doesNotNotify() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1)

        dispatcher.removeInput(input)

        // Add another callback; the removed input should not be notified.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1) // Unchanged
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
    fun state_onMultipleCallbacksAdded_reflectsLastAdded() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
        val detailsCallback =
            TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))

        assertThat(dispatcher.state.value).isEqualTo(Idle(NotProvided))

        dispatcher.addCallback(homeCallback)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("home")))

        // Callbacks are prioritized like a stack (LIFO), so adding a new one makes it active.
        dispatcher.addCallback(detailsCallback)
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_onSetInfoOnActiveCallback_updatesState() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("initial"))
        dispatcher.addCallback(callback)

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("initial")))

        // Calling setInfo on the active callback should immediately update the dispatcher's state.
        callback.setInfo(currentInfo = HomeScreenInfo("updated"), previousInfo = null)

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("updated")))
    }

    @Test
    fun state_onSetInfoOnInactiveCallback_doesNotUpdateState() = runTest {
        val dispatcher = NavigationEventDispatcher()
        val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
        val detailsCallback =
            TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
        dispatcher.addCallback(homeCallback)
        dispatcher.addCallback(detailsCallback)

        // The state should reflect the last-added (active) callback.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))

        // Calling setInfo on an inactive callback should NOT affect the global state.
        homeCallback.setInfo(currentInfo = HomeScreenInfo("home-updated"), previousInfo = null)

        // The state should remain unchanged because the update came from a non-active callback.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_onFullGestureLifecycle_transitionsToInProgressThenIdle() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        val startEvent = NavigationEvent(touchX = 0.1F)
        val progressEvent = NavigationEvent(touchX = 0.3f)

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture should move the state to InProgress with the start event.
        input.start(startEvent)
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(callbackInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Progressing the gesture should keep it InProgress but update to the latest event.
        input.progress(progressEvent)
        state = dispatcher.state.value as InProgress
        assertThat(state.latestEvent).isEqualTo(progressEvent)

        // Completing the gesture should return the state to Idle.
        input.complete()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun state_onGestureCancelled_returnsToIdle() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        val startEvent = NavigationEvent()

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture moves the state to InProgress.
        input.start(startEvent)
        assertThat(dispatcher.state.value).isEqualTo(InProgress(callbackInfo, null, startEvent))

        // Cancelling the gesture should also return the state to Idle.
        input.cancel()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun inProgressState_onInfoUpdateDuringGesture_reflectsNewInfo() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val firstInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = firstInfo)
        dispatcher.addCallback(callback)

        val startEvent = NavigationEvent(touchX = 0.1F)

        input.start(startEvent)

        // At the start, previousInfo is null.
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(firstInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Update the info mid-gesture.
        val secondInfo = HomeScreenInfo("updated")
        callback.setInfo(currentInfo = secondInfo, previousInfo = firstInfo)

        // The state should now reflect the updated info. The `previousInfo` is now captured.
        state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(secondInfo)
        assertThat(state.previousInfo).isEqualTo(firstInfo)
        assertThat(state.latestEvent).isEqualTo(startEvent) // Event hasn't changed yet.

        // Complete the gesture.
        input.complete()
        assertThat(dispatcher.state.value).isEqualTo(Idle(secondInfo))
    }

    @Test
    fun inProgressState_onNewGesture_clearsPreviousInfo() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val initialInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = initialInfo)
        dispatcher.addCallback(callback)

        // FIRST GESTURE: Create a complex state.
        input.start(NavigationEvent(touchX = 0.1f))
        callback.setInfo(currentInfo = HomeScreenInfo("updated"), previousInfo = null)
        input.complete()

        // After the first gesture, the final state is Idle with the updated info.
        val finalInfo = HomeScreenInfo("updated")
        assertThat(dispatcher.state.value).isEqualTo(Idle(finalInfo))

        // SECOND GESTURE: Start a new gesture.
        val event2 = NavigationEvent(touchX = 0.3f)
        input.start(event2)

        // When a new gesture starts, `previousInfo` must be null, not stale data
        // from a previous, completed gesture.
        val state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(finalInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(event2)
    }

    @Test
    fun state_onDispatcherDisabled_fallsBackToSibling() {
        val dispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher = dispatcher)

        val callbackA = TestNavigationEventCallback(currentInfo = HomeScreenInfo("A"))
        dispatcher.addCallback(callbackA)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        val callbackB = TestNavigationEventCallback(currentInfo = DetailsScreenInfo("B"))
        childDispatcher.addCallback(callbackB)
        // Assert that state reflects callbackB, which was added last and is now active.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))

        // Disable the dispatcher that hosts the currently active callback.
        childDispatcher.isEnabled = false

        // The state should now fall back to the next-highest priority callback (callbackA).
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        // Re-enable the dispatcher.
        childDispatcher.isEnabled = true

        // The state should once again reflect callbackB.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))
    }

    @Test
    fun getState_withTypeFilter_emitsOnlyMatchingStates() =
        runTest(UnconfinedTestDispatcher()) {
            val dispatcher = NavigationEventDispatcher()
            val initialHomeInfo = HomeScreenInfo("initial")
            val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
            val detailsCallback =
                TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
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
            dispatcher.addCallback(homeCallback)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))

            // A state with a non-matching type should be filtered out and not collected.
            dispatcher.addCallback(detailsCallback)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)

            // When the active callback is removed, the state falls back to a matching type,
            // but since the info is the same as before, no new state is emitted.
            detailsCallback.remove()
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))
        }

    @Test
    fun getState_withNonMatchingType_emitsOnlyInitialInfo() =
        runTest(UnconfinedTestDispatcher()) {
            val dispatcher = NavigationEventDispatcher()
            val initialHomeInfo = HomeScreenInfo("initial")
            val detailsCallback =
                TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
            val collectedStates = mutableListOf<NavigationEventState<HomeScreenInfo>>()

            dispatcher
                .getState(backgroundScope, initialHomeInfo)
                .onEach { collectedStates.add(it) }
                .launchIn(backgroundScope)
            advanceUntilIdle()

            // The flow must start with its initial value.
            assertThat(collectedStates).hasSize(1)
            assertThat(collectedStates.first()).isEqualTo(Idle(initialHomeInfo))

            // Add a callback with a non-matching type.
            dispatcher.addCallback(detailsCallback)
            advanceUntilIdle()

            // The collector should not have emitted a new value.
            assertThat(collectedStates).hasSize(1)

            // Update the non-matching callback's info.
            detailsCallback.setInfo(
                currentInfo = DetailsScreenInfo("details-updated"),
                previousInfo = null,
            )
            advanceUntilIdle()

            // The collector should still not have emitted a new value.
            assertThat(collectedStates).hasSize(1)
        }

    @Test
    fun progress_inIdleAndInProgress_returnsCorrectValue() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput().also { dispatcher.addInput(it) }
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        // Before any gesture, the state is Idle and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)

        // Start a gesture.
        input.start(NavigationEvent(progress = 0.1f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.1f)

        // InProgress state should reflect the event's progress.
        input.progress(NavigationEvent(progress = 0.5f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.5f)

        // Complete the gesture.
        input.complete()

        // After the gesture, the state is Idle again and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)
    }

    // endregion

    // region Hierarchy APIs

    @Test
    fun init_withParent_sharesCallbacks() {
        val parentDispatcher = NavigationEventDispatcher()
        val parentCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)

        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val childCallback = TestNavigationEventCallback()
        childDispatcher.addCallback(childCallback)

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)

        // Callbacks from child dispatchers are prioritized over their parents (LIFO).
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun init_withoutParent_hasIndependentCallbacks() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher()

        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // Dispatch an event through the parent.
        val event = NavigationEvent()
        val parentInput = TestNavigationEventInput()
        parentDispatcher.addInput(parentInput)
        parentInput.start(event)

        // Only the parent's callback should be invoked.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // Dispatch an event through the child.
        val childInput = TestNavigationEventInput()
        childDispatcher.addInput(childInput)
        childInput.start(event)

        // Only the child's callback should be invoked.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_toChild_isDispatchedViaParent() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val callback = TestNavigationEventCallback()

        childDispatcher.addCallback(callback)

        // Events dispatched from a parent should propagate to callbacks in child dispatchers.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_toParentThenChild_ordersLIFO() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // The last-added callback (child's) should be invoked first.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_multipleDispatchers_prioritizesLastAdded() {
        val parentDispatcher = NavigationEventDispatcher()
        val child1Dispatcher = NavigationEventDispatcher(parentDispatcher)
        val child2Dispatcher = NavigationEventDispatcher(parentDispatcher)

        val parentCallback = TestNavigationEventCallback()
        val childCallback1 = TestNavigationEventCallback()
        val childCallback2 = TestNavigationEventCallback()

        parentDispatcher.addCallback(parentCallback)
        child2Dispatcher.addCallback(childCallback2)
        child1Dispatcher.addCallback(childCallback1)

        // Callbacks are processed in a LIFO manner across the entire hierarchy.
        // The callback from child1 was added last, so it gets the event.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback2.startedInvocations).isEqualTo(0)
        assertThat(childCallback1.startedInvocations).isEqualTo(1)
    }

    @Test
    fun dispose_onChild_parentStillReceivesEvents() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // Disposing a child should not affect its parent.
        childDispatcher.dispose()

        // Dispatching an event from the parent should now trigger the parent's callback,
        // as the child's (previously higher priority) callback is gone.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
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
                input.start(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.start(event)
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
                input.start(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                parentDispatcher.addInput(input)
                input.start(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.start(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun isEnabled_whenTrue_dispatchesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        dispatcher.isEnabled = true

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenFalse_doesNotDispatchEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(isEnabled = true)
        dispatcher.addCallback(callback)

        dispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(event)
        assertThat(callback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_parentDisabled_disablesChildDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // Disabling the parent should effectively disable the entire sub-hierarchy.
        parentDispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.start(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_childDisabled_doesNotDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        childDispatcher.isEnabled = false

        // Events sent to the child dispatcher should not be processed by any callback.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.start(event)

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_childDisabled_parentStillDispatches() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        childDispatcher.isEnabled = false

        // Disabling a child should not affect the parent. Events sent directly to the
        // parent should be handled by the parent's callbacks.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(event)

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_parentReenabled_reenablesChildDispatch() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.start(initialEvent)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        parentDispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.start(reEnabledEvent)

        assertThat(childCallback.startedInvocations).isEqualTo(1)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_parentReenabled_childCallbackReceivesEvents() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.start(initialEvent)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        parentDispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.start(reEnabledEvent)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_grandparentDisabled_disablesGrandchildDispatch() {
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        grandparentDispatcher.isEnabled = false

        // Disabling the grandparent should disable all descendants.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.start(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_grandparentDisabled_grandchildCallbackDoesNotReceiveEvents() {
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        grandparentDispatcher.isEnabled = false

        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        grandparentDispatcher.addInput(input)
        input.start(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun callbackIsEnabled_whenDispatcherDisabled_doesNotReceiveEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        val preDisableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(preDisableEvent)
        assertThat(callback.startedInvocations).isEqualTo(1)

        dispatcher.isEnabled = false

        // An enabled callback on a disabled dispatcher should not receive events.
        val event = NavigationEvent()
        input.start(event)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun callbackIsEnabled_whenDispatcherReenabled_receivesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        dispatcher.isEnabled = false

        val preEnableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.start(preEnableEvent)
        assertThat(callback.startedInvocations).isEqualTo(0)

        dispatcher.isEnabled = true

        val reEnabledEvent = NavigationEvent()
        input.start(reEnabledEvent)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_onDisposedDispatcher_throws() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        assertThrows<IllegalStateException> {
                dispatcher.addCallback(TestNavigationEventCallback())
            }
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
                input.start(NavigationEvent())
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
                input.progress(NavigationEvent())
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
                input.complete()
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
                input.cancel()
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
}

/** A sealed interface for type-safe navigation information. */
sealed interface TestInfo : NavigationEventInfo

data class HomeScreenInfo(val id: String) : TestInfo

data class DetailsScreenInfo(val id: String) : TestInfo

/**
 * A test implementation of [NavigationEventInput] that records lifecycle events and invocation
 * counts.
 *
 * Use this class in tests to verify that `onAdded`, `onRemoved`, and `onHasEnabledCallbacksChanged`
 * are called correctly. It counts how many times each lifecycle method is invoked and stores a
 * reference to the most recently added dispatcher. It also provides helper methods to simulate
 * dispatching navigation events.
 *
 * @param onAdded An optional lambda to execute when [onAdded] is called.
 * @param onRemoved An optional lambda to execute when [onRemoved] is called.
 * @param onHasEnabledCallbacksChanged An optional lambda to execute when
 *   [onHasEnabledCallbacksChanged] is called.
 */
private class TestNavigationEventInput(
    private val onAdded: (dispatcher: NavigationEventDispatcher) -> Unit = {},
    private val onRemoved: () -> Unit = {},
    private val onHasEnabledCallbacksChanged: (hasEnabledCallbacks: Boolean) -> Unit = {},
) : NavigationEventInput() {

    /** The number of times [onAdded] has been invoked. */
    var addedInvocations: Int = 0
        private set

    /** The number of times [onRemoved] has been invoked. */
    var removedInvocations: Int = 0
        private set

    /** The number of times [onHasEnabledCallbacksChanged] has been invoked. */
    var onHasEnabledCallbacksChangedInvocations: Int = 0
        private set

    /**
     * The most recently added [NavigationEventDispatcher].
     *
     * This is set by [onAdded] and cleared to `null` by [onRemoved].
     */
    var currentDispatcher: NavigationEventDispatcher? = null
        private set

    /**
     * Test helper to simulate the start of a navigation event.
     *
     * This directly calls `dispatchOnStarted`, notifying any registered callbacks. Use this to
     * trigger the beginning of a navigation flow in your tests.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun start(event: NavigationEvent = NavigationEvent()) {
        dispatchOnStarted(event)
    }

    /**
     * Test helper to simulate the progress of a navigation event.
     *
     * This directly calls `dispatchOnProgressed`, notifying any registered callbacks.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun progress(event: NavigationEvent = NavigationEvent()) {
        dispatchOnProgressed(event)
    }

    /**
     * Test helper to simulate the completion of a navigation event.
     *
     * This directly calls `dispatchOnCompleted`, notifying any registered callbacks.
     */
    @MainThread
    fun complete() {
        dispatchOnCompleted()
    }

    /**
     * Test helper to simulate the cancellation of a navigation event.
     *
     * This directly calls `dispatchOnCancelled`, notifying any registered callbacks.
     */
    @MainThread
    fun cancel() {
        dispatchOnCancelled()
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

    override fun onHasEnabledCallbacksChanged(hasEnabledCallbacks: Boolean) {
        onHasEnabledCallbacksChangedInvocations++
        onHasEnabledCallbacksChanged.invoke(hasEnabledCallbacks)
    }
}
