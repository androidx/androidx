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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigationevent.DirectNavigationEventInputHandler
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavigationEventHandlerTest {

    @get:Rule val rule = createComposeRule()

    private val owner = TestNavigationEventDispatcherOwner()
    private val dispatcher = owner.navigationEventDispatcher
    private val inputHandler = DirectNavigationEventInputHandler(dispatcher)

    @Test
    fun navigationEventHandler_whenOnStartDispatched_invokesHandler() {
        var onStart = false

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    onStart = true
                    progress.collect()
                }

                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(onStart).isTrue() }
    }

    @Test
    fun navigationEventHandler_whenOnCompleteDispatched_executesPostCollectCode() {
        var counter = 0

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    progress.collect()
                    counter++
                }
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        inputHandler.handleOnCompleted()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun navigationEventHandler_whenNewGestureIsHandled_invokesHandlerForEachGesture() {
        var counter = 0

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    progress.collect()
                    counter++
                }
                Button(
                    onClick = {
                        inputHandler.handleOnStarted(NavigationEvent())
                        inputHandler.handleOnCompleted()
                    }
                ) {
                    Text(text = "backPress")
                }
            }
        }

        // Simulate the first complete gesture
        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        // Simulate a second complete gesture
        // This ensures the handler can be invoked multiple times.
        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    fun navigationEventHandler_whenEnabledChanges_invokesFallbackWhenDisabled() {
        val result = mutableListOf<String>()
        var enabled by mutableStateOf(true)

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(enabled = enabled) { progress ->
                    progress.collect()
                    result += "onBack"
                }
            }
        }

        // Phase 1: Test when enabled
        // The handler should be called.
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnCompleted()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("onBack"))
            assertThat(owner.fallbackOnBackPressedInvocations).isEqualTo(0)
        }

        // Phase 2: Test when disabled
        // The fallback should be invoked instead of the handler.
        enabled = false
        rule.runOnIdle {
            inputHandler.handleOnStarted(NavigationEvent())
            inputHandler.handleOnCompleted()
            assertThat(result).isEqualTo(listOf("onBack")) // Unchanged
            assertThat(owner.fallbackOnBackPressedInvocations).isEqualTo(1)
        }

        // Phase 3: Test when re-enabled
        // The handler should work again.
        enabled = true
        rule.runOnIdle {
            inputHandler.handleOnStarted(NavigationEvent())
            inputHandler.handleOnCompleted()
            assertThat(result).isEqualTo(listOf("onBack", "onBack"))
        }
    }

    @Test
    fun navigationEventHandler_whenDisabledJustBeforeGestureStart_completesAndInvokesHandler() {
        // This test verifies a specific edge case: a gesture starts and is dispatched
        // to the handler, but the `enabled` state changes to `false` *just before* the
        // handler's coroutine actually runs its `enabled` check. The handler should
        // still complete successfully because the gesture was already committed to it.
        // This prevents the system from dropping a gesture that was valid at the moment
        // it began.
        val result = mutableListOf<String>()
        var count by mutableStateOf(2)
        var wasStartedWhenDisabled = false
        var wasCancelled = false

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(enabled = count > 1) { progress ->
                    // This flag captures if the handler's coroutine was launched
                    // even if the `enabled` check will fail.
                    if (count <= 1) {
                        wasStartedWhenDisabled = true
                    }
                    try {
                        progress.collect()
                        result += "onBack"
                    } catch (e: CancellationException) {
                        wasCancelled = true
                    }
                }
            }
        }

        // The 'enabled' check happens inside the callback. Disabling right before the
        // dispatch means the handler will start but see that it's disabled.
        count = 1
        inputHandler.handleOnStarted(NavigationEvent())

        // The launched effect for the handler might still run, but it should not prevent
        // the gesture from completing normally.
        rule.runOnIdle { assertThat(wasStartedWhenDisabled).isTrue() }
        inputHandler.handleOnCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("onBack")) }

        // It should not be cancelled because the gesture completes successfully.
        rule.runOnIdle { assertThat(wasCancelled).isFalse() }
    }

    @Test
    fun navigationEventHandler_whenDisabledJustAfterGestureStart_completesSuccessfully() {
        val result = mutableListOf<String>()
        var count by mutableStateOf(2)
        var wasStartedWhenDisabled = false

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler(enabled = count > 1) { progress ->
                    if (count <= 1) {
                        wasStartedWhenDisabled = true
                    }
                    progress.collect()
                    result += "onBack"
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        // Disable after the gesture has already started. The `enabled` check has already passed,
        // so the gesture should continue to be handled.
        count = 1

        rule.runOnIdle { assertThat(wasStartedWhenDisabled).isFalse() }
        inputHandler.handleOnCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("onBack")) }
    }

    @Test(expected = IllegalStateException::class)
    fun navigationEventHandler_whenProgressFlowNotCollected_throwsIllegalStateException() {
        val result = mutableListOf<String>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { _ ->
                    // This handler is invalid because it never collects the progress Flow.
                    // A handler MUST call `progress.collect()` to suspend execution until the
                    // gesture is completed, cancelled, or handed off.
                    result += "start"
                    delay(300)
                    result += "async"
                    result += "complete"
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        // The exception is thrown on completion because the handler's coroutine finishes
        // prematurely without having suspended for the gesture's result.
        inputHandler.handleOnCompleted()

        rule.waitUntil(1000) { result.size >= 3 }
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("start", "async", "complete")) }
    }

    @Test
    fun navigationEventHandler_whenNewGestureStarts_cancelsPreviousAsyncWork() {
        val result = mutableListOf<String>()
        var asyncStarted = false

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    result += "start"
                    progress.collect()
                    asyncStarted = true
                    delay(300) // simulate some work
                    result += "async"
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnCompleted()
        rule.waitUntil { asyncStarted } // failing

        // Start a new gesture. This should cancel the scope of the previous handler,
        // including the async job it launched.
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnCompleted()
        rule.waitUntil(1000) { result.size >= 3 }

        rule.runOnIdle {
            // The first "async" should have been cancelled and never added to the list.
            assertThat(result).isEqualTo(listOf("start", "start", "async"))
        }
    }

    @Test
    fun navigationEventHandler_whenNested_invokesOnlyInnermostHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    result += "parent"
                    progress.collect()
                }
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    // When handlers are nested, only the deepest, last-composed handler is active.
                    NavigationEventHandler { progress ->
                        result += "child"
                        progress.collect()
                    }
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("child")) }
    }

    @Test
    fun navigationEventHandler_whenNestedChildIsDisabled_invokesParentHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // This parent handler should be invoked because its child is disabled.
                NavigationEventHandler { progress ->
                    result += "parent"
                    progress.collect()
                }
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    NavigationEventHandler(enabled = false) { progress ->
                        result += "child"
                        progress.collect()
                    }
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("parent")) }
    }

    @Test
    fun navigationEventHandler_whenSiblingsExist_invokesLastDeclaredHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    result += "first"
                    progress.collect()
                }
                // In composition, the last sibling handler to be executed becomes the active one.
                NavigationEventHandler { progress ->
                    result += "second"
                    progress.collect()
                }
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("second")) }
    }

    @Test
    fun navigationEventHandler_whenLastSiblingIsDisabled_invokesFirstSiblingHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // This handler should be invoked because the one after it is disabled.
                NavigationEventHandler { progress ->
                    result += "first"
                    progress.collect()
                }
                NavigationEventHandler(enabled = false) { progress ->
                    result += "second"
                    progress.collect()
                }
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    Text(text = "backPress")
                }
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("first")) }
    }

    @Test
    fun navigationEventHandler_whenOnEventLambdaChanges_usesNewLambdaForSubsequentEvents() {
        val results = mutableListOf<String>()
        var handler by
            mutableStateOf<suspend (Flow<NavigationEvent>) -> Unit>({ progress ->
                results += "first"
                progress.collect()
            })

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // The key of the handler is its `onEvent` lambda. Changing it should
                // correctly replace the old handler with the new one.
                NavigationEventHandler(onEvent = handler)
                Button(onClick = { inputHandler.handleOnStarted(NavigationEvent()) }) {
                    Text(text = "backPress")
                }
            }
        }
        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            // Trigger a recomposition that provides a new handler lambda.
            handler = { progress ->
                results += "second"
                progress.collect()
            }
        }
        // This second click should trigger the new handler.
        rule.onNodeWithText("backPress").performClick()

        rule.runOnIdle { assertThat(results).isEqualTo(listOf("first", "second")) }
    }

    @Test
    fun navigationEventHandler_whenOnProgressDispatched_collectsEachEvent() {
        val result = mutableListOf<Int>()
        var counter = 0

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress -> progress.collect { result += counter++ } }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())

        rule.waitForIdle()
        assertThat(result).isEqualTo(listOf(0, 1, 2))
    }

    @Test
    fun navigationEventHandler_whenGestureIsCancelled_throwsCancellationException() {
        val result = mutableListOf<String>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    try {
                        result += "start"
                        progress.collect { result += "progress" }
                        result += "completed"
                    } catch (e: CancellationException) {
                        // It's good practice to catch CancellationException to handle cleanup
                        // when a back gesture is cancelled by the user.
                        result += e.message!!
                    }
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnCancelled()

        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("start", "progress", "navEvent cancelled"))
        }
    }

    @Test
    fun navigationEventHandler_whenGestureIsCancelledAndNotCaught_stopsExecution() {
        val result = mutableListOf<String>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    result += "start"
                    progress.collect { result += "progress" }
                    // This line should not be reached because cancellation will be thrown
                    // from `collect` and, if not caught, will terminate the coroutine.
                    result += "completed"
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnCancelled()

        rule.runOnIdle { assertThat(result).isEqualTo(listOf("start", "progress")) }
    }

    @Test
    fun navigationEventHandler_whenNewGestureStartsAfterCancellation_handlesNewGestureCorrectly() {
        val result = mutableListOf<String>()

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    try {
                        progress.collect { result += "progress" }
                        result += "complete"
                    } catch (e: CancellationException) {
                        result += e.message!!
                    }
                }
            }
        }

        // Simulate a cancelled gesture
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnCancelled()

        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("progress", "navEvent cancelled"))
            result.clear()
        }

        // The handler should reset and be ready for a new gesture.
        inputHandler.handleOnStarted(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnProgressed(NavigationEvent())
        inputHandler.handleOnCompleted()
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("progress", "progress", "complete")) }
    }

    @Test
    fun navigationEventHandler_whenGestureIsCancelled_cancelsAssociatedAsyncWork() {
        val result = mutableListOf<String>()
        var asyncStarted = false

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                NavigationEventHandler { progress ->
                    result += "start"
                    asyncStarted = true
                    delay(300) // simulate some work
                    result += "async"
                    progress.collect()
                }
            }
        }

        inputHandler.handleOnStarted(NavigationEvent())
        rule.waitUntil { asyncStarted }
        // Cancelling the gesture should cancel the handler's scope, which in turn
        // cancels the async job.
        inputHandler.handleOnCancelled()

        rule.runOnIdle {
            runBlocking { delay(700) } // allow time for the cancelled async job to not complete
            assertThat(result).isEqualTo(listOf("start"))
        }
    }

    @Test
    fun navigationEventHandler_whenRemovedFromCompositionMidGesture_disposes() {
        val result = mutableListOf<String>()
        var showHandler by mutableStateOf(true)

        rule.setContent {
            NavigationEventDispatcherOwner(parent = owner) {
                // This handler will be removed from the composition mid-gesture.
                if (showHandler) {
                    NavigationEventHandler { progress ->
                        try {
                            result += "start"
                            progress.collect()
                            result += "complete"
                        } catch (e: CancellationException) {
                            result += "cancelled"
                        }
                    }
                }
            }
        }

        // 1. Start the back gesture. The handler's coroutine is now running.
        inputHandler.handleOnStarted(NavigationEvent())
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("start")) }

        // 2. Remove the handler from the composition.
        // This simulates navigating away or a state change that hides the component.
        // The underlying callback should be disposed, cancelling the handler's coroutine.
        showHandler = false
        rule.runOnIdle {} // Allow recomposition to take effect.

        // 3. Attempt to complete the original gesture.
        // Since the handler was removed, it should not receive this event.
        inputHandler.handleOnCompleted()

        // The handler should have been cancelled when it was disposed.
        // It should not have completed.
        rule.runOnIdle { assertThat(result).isEqualTo(listOf("start", "cancelled")) }
    }
}
