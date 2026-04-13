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

// ExperimentalWasmJsInterop is only available in Kotlin 2.2 and newer versions.
@file:Suppress("OPT_IN_USAGE")

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.navigationevent.testing.TestNavigationEventHandler
import kotlin.test.Test
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.w3c.dom.PopStateEvent
import org.w3c.dom.PopStateEventInit

internal class BrowserInputTest {

    private val testDispatcher = StandardTestDispatcher()
    private val window = TestWindow()
    private var input = BrowserInput(window, testDispatcher)
    private val dispatcher = NavigationEventDispatcher().apply { addInput(input) }

    @Test
    fun onHistoryChanged_initialStateWithSingleInfo_synchronizesBrowserHistory() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)

            advanceUntilIdle()

            // When a single destination is added, BrowserInput should replace the current
            // state with index 0 to mark the start of our tracked history.
            assertWithMessage("Browser history should be initialized with index 0")
                .that(window.states)
                .isEqualTo(listOf(0).map { it.toJsNumber() })
            assertWithMessage("Browser index should be at the start of our tracked history")
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage("BrowserInput should have initialized the state exactly once")
                .that(window.replaceStateCalls)
                .isEqualTo(1)
        }
    }

    @Test
    fun onHistoryChanged_initialStateWithMultipleInfos_synchronizesBrowserHistory() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B, TestInfo.C)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            // When starting with multiple destinations, BrowserInput must expand the browser
            // history to match the length of our history stack and move to the correct index.
            assertWithMessage(
                    "Browser history should match the length of our initial history stack"
                )
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should match the initial currentIndex")
                .that(window.index)
                .isEqualTo(1)
            assertWithMessage("BrowserInput should have expanded the history stack")
                .that(window.pushStateCalls)
                .isEqualTo(2)
        }
    }

    @Test
    fun onHistoryChanged_changeDestination_updatesBrowserHistory() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B)
            advanceUntilIdle()

            // Moving to a new destination expands the browser history stack.
            assertWithMessage("Browser history should contain both entries")
                .that(window.states)
                .isEqualTo(listOf(0, 1).map { it.toJsNumber() })
            assertWithMessage("Browser index should have moved to the new entry")
                .that(window.index)
                .isEqualTo(1)
        }
    }

    @Test
    fun onHistoryChanged_newInfosAreLonger_expandsBrowserHistory() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B, TestInfo.C)
            advanceUntilIdle()

            // If the new history stack is longer, BrowserInput must push new states to the browser
            // to accommodate the additional entries.
            assertWithMessage("Browser history should have been expanded with new entries")
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should reflect the new currentIndex")
                .that(window.index)
                .isEqualTo(1)
        }
    }

    @Test
    fun onHistoryChanged_newInfosAreShorter_updatesBrowserIndex() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B, TestInfo.C)
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 0, TestInfo.A)
            advanceUntilIdle()

            // If the new history stack is shorter, we can't physically shrink the browser stack,
            // so we move the browser's current index to match our logical stack.
            assertWithMessage(
                    "Browser states should remain the same length as we can't shrink the native stack"
                )
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should have moved back to match the logical stack")
                .that(window.index)
                .isEqualTo(0)
        }
    }

    @Test
    fun onHistoryChanged_changeDestinationAndHierarchicalBack_updatesBrowserIndex() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B)
            handler.setInfo(currentIndex = 0, TestInfo.A)
            advanceUntilIdle()

            // Hierarchical back (e.g., Up button) is treated as a jump to a previous entry
            // in the existing browser stack.
            assertWithMessage(
                    "Browser history should reflect the jump back for hierarchical navigation"
                )
                .that(window.index)
                .isEqualTo(0)
        }
    }

    @Test
    fun onHistoryChanged_changeDestinationAndChronologicalBack_updatesBrowserIndex() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B)
            handler.setInfo(currentIndex = 0, TestInfo.A, TestInfo.B)
            advanceUntilIdle()

            // Chronological back (e.g., Back button triggered via code) is also a jump back.
            assertWithMessage(
                    "Browser history should reflect the jump back for chronological navigation"
                )
                .that(window.index)
                .isEqualTo(0)
        }
    }

    @Test
    fun onPopState_back_dispatchesBackCompleted() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onBackCompleted = {
                        if (backInfo.isNotEmpty()) {
                            setInfo(
                                backInfo.last(),
                                backInfo.dropLast(1),
                                listOf(currentInfo) + forwardInfo,
                            )
                        }
                    },
                )
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B)
            advanceUntilIdle()

            window.go(-1)
            advanceUntilIdle()

            // A native back navigation should trigger a dispatch to our internal handlers.
            assertWithMessage(
                    "Browser history state should remain unchanged after a back navigation"
                )
                .that(window.states)
                .isEqualTo(listOf(0, 1).map { it.toJsNumber() })
            assertWithMessage("Browser index should have moved back")
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage("Dispatcher history should reflect the new merged state")
                .that(dispatcher.history.value.mergedHistory)
                .isEqualTo(listOf(TestInfo.A, TestInfo.B))
            assertWithMessage("Dispatcher index should match the new browser index")
                .that(dispatcher.history.value.currentIndex)
                .isEqualTo(0)
        }
    }

    @Test
    fun onPopState_forward_dispatchesForwardCompleted() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    isForwardEnabled = true,
                    onForwardCompleted = {
                        if (forwardInfo.isNotEmpty()) {
                            setInfo(
                                forwardInfo.first(),
                                backInfo + listOf(currentInfo),
                                forwardInfo.drop(1),
                            )
                        }
                    },
                )
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 0, TestInfo.A, TestInfo.B)
            advanceUntilIdle()

            window.go(1)
            advanceUntilIdle()

            // A native forward navigation should trigger a dispatch to our internal handlers.
            assertWithMessage(
                    "Browser history state should remain unchanged after a forward navigation"
                )
                .that(window.states)
                .isEqualTo(listOf(0, 1).map { it.toJsNumber() })
            assertWithMessage("Browser index should have moved forward")
                .that(window.index)
                .isEqualTo(1)
            assertWithMessage("Dispatcher history should reflect the new merged state")
                .that(dispatcher.history.value.mergedHistory)
                .isEqualTo(listOf(TestInfo.A, TestInfo.B))
            assertWithMessage("Dispatcher index should match the new browser index")
                .that(dispatcher.history.value.currentIndex)
                .isEqualTo(1)
        }
    }

    @Test
    fun onPopState_multipleBack_dispatchesMultipleBackCompleted() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onBackCompleted = {
                        if (backInfo.isNotEmpty()) {
                            setInfo(
                                backInfo.last(),
                                backInfo.dropLast(1),
                                listOf(currentInfo) + forwardInfo,
                            )
                        }
                    },
                )
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 2, TestInfo.A, TestInfo.B, TestInfo.C)
            advanceUntilIdle()

            window.go(-2)
            advanceUntilIdle()

            // Jumping multiple pages back via browser dropdown must sequentially unwind our state.
            assertWithMessage("Dispatcher should have received multiple back completed events")
                .that(handler.onBackCompletedInvocations)
                .isEqualTo(2)
            assertWithMessage("Browser history state should remain unchanged")
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should have jumped back two steps")
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage("Dispatcher history should reflect all unwind steps")
                .that(dispatcher.history.value.mergedHistory)
                .isEqualTo(listOf(TestInfo.A, TestInfo.B, TestInfo.C))
            assertWithMessage("Dispatcher index should reflect the final position")
                .that(dispatcher.history.value.currentIndex)
                .isEqualTo(0)
        }
    }

    @Test
    fun onPopState_multipleForward_dispatchesMultipleForwardCompleted() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    isForwardEnabled = true,
                    onForwardCompleted = {
                        if (forwardInfo.isNotEmpty()) {
                            setInfo(
                                forwardInfo.first(),
                                backInfo + listOf(currentInfo),
                                forwardInfo.drop(1),
                            )
                        }
                    },
                )
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 0, TestInfo.A, TestInfo.B, TestInfo.C)
            advanceUntilIdle()

            window.go(2)
            advanceUntilIdle()

            // Jumping multiple pages forward via browser dropdown must sequentially wind our state.
            assertWithMessage("Dispatcher should have received multiple forward completed events")
                .that(handler.onForwardCompletedInvocations)
                .isEqualTo(2)
            assertWithMessage("Browser history state should remain unchanged")
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should have moved forward two steps")
                .that(window.index)
                .isEqualTo(2)
            assertWithMessage("Dispatcher history should reflect all winding steps")
                .that(dispatcher.history.value.mergedHistory)
                .isEqualTo(listOf(TestInfo.A, TestInfo.B, TestInfo.C))
            assertWithMessage("Dispatcher index should reflect the final position")
                .that(dispatcher.history.value.currentIndex)
                .isEqualTo(2)
        }
    }

    @Test
    fun onPopState_toInvalidEntry_revertsNavigation() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onForwardCompleted = { error("Should not be called") },
                )
            dispatcher.addHandler(handler)

            handler.setInfo(currentIndex = 2, TestInfo.A, TestInfo.B, TestInfo.C)
            handler.setInfo(currentIndex = 0, TestInfo.A)
            advanceUntilIdle()

            window.go(2)
            advanceUntilIdle()

            // If the browser navigates to an entry that we no longer track as valid,
            // we must force the browser back to our current logical index.
            assertWithMessage(
                    "BrowserInput should have detected invalid entry and forced navigation back"
                )
                .that(window.index)
                .isEqualTo(0)
        }
    }

    @Test
    fun onHistoryChanged_changeHandlers_updatesBrowserHistory() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onForwardCompleted = { error("Should not be called") },
                )
            handler.setInfo(currentIndex = 0, TestInfo.A, TestInfo.B, TestInfo.C)
            dispatcher.addHandler(handler)

            val handler2 =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onForwardCompleted = { error("Should not be called") },
                )
            handler2.setInfo(currentIndex = 2, TestInfo.X, TestInfo.Y, TestInfo.Z)
            dispatcher.addHandler(handler2)

            advanceUntilIdle()

            // Replacing a handler might change the total size and index of the history stack.
            assertWithMessage("Browser history state should match the new handler's stack")
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2).map { it.toJsNumber() })
            assertWithMessage("Browser index should reflect the new merged handler index")
                .that(window.index)
                .isEqualTo(2)
        }
    }

    @Test
    fun onHistoryChanged_combinedHandlers_updatesBrowserHistory() {
        runTest(testDispatcher) {
            val handler =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onForwardCompleted = { error("Should not be called") },
                )
            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B, TestInfo.C)
            dispatcher.addHandler(handler)

            val handler2 =
                TestNavigationEventHandler<TestInfo>(
                    currentInfo = TestInfo.A,
                    onForwardCompleted = { error("Should not be called") },
                )
            handler2.setInfo(currentIndex = 1, TestInfo.X, TestInfo.Y, TestInfo.Z)

            dispatcher.addHandler(handler2)
            advanceUntilIdle()

            // When multiple handlers are active, their stacks are merged, and the browser
            // must reflect the combined size and index.
            // [A, X, Y*, Z] -> size 4, index 2
            assertThat(window.states).isEqualTo(listOf(0, 1, 2, 3).map { it.toJsNumber() })
            assertThat(window.index).isEqualTo(2)
        }
    }

    @Test
    fun go_timeout_completes() {
        runTest(testDispatcher) {
            window.emitPopState = false

            // This should not hang forever; it should complete via timeout if no 'popstate' event
            // fires.
            window.go(1)

            // The index should still be updated in our mock to reflect the attempt.
            assertWithMessage(
                    "Window mock index should be updated even if popstate event is missing"
                )
                .that(window.index)
                .isEqualTo(1)
        }
    }

    @Test
    fun onRemoved_resetsInternalState() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            handler.setInfo(currentIndex = 2, TestInfo.A, TestInfo.B, TestInfo.C)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            assertThat(window.index).isEqualTo(2)
            assertThat(window.entries).hasSize(3)

            dispatcher.removeInput(input)

            // When removed and re-added, BrowserInput must start from a clean state.
            dispatcher.addInput(input)
            advanceUntilIdle()

            // Verify it reset to 0 even though the window was at 2.
            assertWithMessage(
                    "BrowserInput should reset to index 0 after re-adding even if window was previously ahead"
                )
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage(
                    "BrowserInput should have called replaceState again during re-initialization"
                )
                .that(window.replaceStateCalls)
                .isEqualTo(2)
        }
    }

    @Test
    fun onPopState_nullState_isIgnored() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            assertThat(window.index).isEqualTo(0)

            // Browser navigation events without a state object (e.g., hash changes or initial load)
            // are ignored as we only track our integer indices.
            window.emitPopState(null)
            advanceUntilIdle()

            assertWithMessage("PopState with null state should be ignored")
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage("Should not have triggered any internal navigation fixes")
                .that(window.goCalls)
                .isEqualTo(0)
        }
    }

    @Test
    fun onPopState_invalidState_isIgnored() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            assertThat(window.index).isEqualTo(0)

            // Browser navigation events with invalid state types are ignored.
            window.emitPopState("invalid".toJsString())
            advanceUntilIdle()

            assertWithMessage("PopState with invalid state type should be ignored")
                .that(window.index)
                .isEqualTo(0)
            assertWithMessage("Should not have triggered any internal navigation fixes")
                .that(window.goCalls)
                .isEqualTo(0)
        }
    }

    @Test
    fun onPopState_externalNavigation_revertsNavigation() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            handler.setInfo(currentIndex = 0, TestInfo.A, TestInfo.B)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            assertThat(window.index).isEqualTo(0)
            assertThat(window.entries).hasSize(2) // [0, 1]

            // If an external script pushes an untracked state to the browser history,
            // BrowserInput should detect it's out of range and force the browser back
            // to our current tracked index.
            window.pushState(10.toJsNumber()) // window.index = 1, then window.index = 2
            assertWithMessage("Window mock should reflect the external pushState")
                .that(window.index)
                .isEqualTo(2)

            window.emitPopState(2.toJsNumber())
            advanceUntilIdle()

            assertWithMessage(
                    "BrowserInput should detect untracked external states and revert navigation back"
                )
                .that(window.index)
                .isEqualTo(0)
        }
    }

    @Test
    fun onHistoryChanged_concurrently_isSynchronized() {
        runTest(testDispatcher) {
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            dispatcher.addHandler(handler)
            advanceUntilIdle()

            // BrowserInput must ensure that rapid, concurrent updates to the history stack are
            // processed sequentially and the browser ends in the final expected state.
            handler.setInfo(currentIndex = 1, TestInfo.A, TestInfo.B)
            handler.setInfo(currentIndex = 2, TestInfo.A, TestInfo.B, TestInfo.C)
            handler.setInfo(currentIndex = 3, TestInfo.A, TestInfo.B, TestInfo.C, TestInfo.X)
            advanceUntilIdle()

            assertWithMessage("Browser index should match the final index of the history stack")
                .that(window.index)
                .isEqualTo(3)
            assertWithMessage("Browser history states should contain all pushed indices")
                .that(window.states)
                .isEqualTo(listOf(0, 1, 2, 3).map { it.toJsNumber() })
        }
    }

    @Test
    fun onAdded_recoversStateFromWindow() {
        runTest(testDispatcher) {
            // Pre-seed the window with a state
            window.replaceState(5.toJsNumber())

            // We need to create a new BrowserInput and add it to a new dispatcher
            // because the previous one was already initialized.
            val newDispatcher = NavigationEventDispatcher()
            val newInput = BrowserInput(window, testDispatcher)

            newDispatcher.addInput(newInput)
            advanceUntilIdle()

            // Verify that the input recovered the index 5
            // If we now add a handler with 1 item, it will call window.go(-5)
            val handler = TestNavigationEventHandler<TestInfo>(TestInfo.A)
            newDispatcher.addHandler(handler)
            advanceUntilIdle()

            // Index was 5, now it should be 0.
            assertThat(window.index).isEqualTo(0)
        }
    }

    /**
     * A sealed class representing type-safe navigation information used for testing.
     *
     * Instances of this class are used as the [NavigationEventInfo] in [BrowserInputTest] to
     * represent different screens or destinations in the navigation history.
     */
    private sealed class TestInfo : NavigationEventInfo() {
        data object A : TestInfo()

        data object B : TestInfo()

        data object C : TestInfo()

        data object X : TestInfo()

        data object Y : TestInfo()

        data object Z : TestInfo()
    }

    /**
     * A test implementation of [WindowCompat] used to simulate browser window behavior.
     *
     * This class maintains a fake history stack of [Entry] objects and tracks the number of calls
     * to [pushState], [replaceState], and [go]. It is used to verify that [BrowserInput] correctly
     * synchronizes the dispatcher's navigation state with the browser's history.
     */
    private class TestWindow : WindowCompat {
        /** The list of registered callbacks for 'popstate' events. */
        private val popStateListeners = mutableListOf<(PopStateEvent) -> Unit>()

        /** Represents a single entry in the browser history. */
        data class Entry(val state: JsAny?, val url: String?)

        /** The full stack of history entries. */
        val entries = mutableListOf<Entry>(Entry(null, null))

        /** A list of all state objects currently in the history stack. */
        val states
            get() = entries.map { it.state }

        /** The current position in the history stack. */
        var index = 0
            private set

        /** Whether [go] should automatically trigger a [PopStateEvent]. */
        var emitPopState = true

        /** Total number of [pushState] calls. */
        var pushStateCalls = 0

        /** Total number of [replaceState] calls. */
        var replaceStateCalls = 0

        /** Total number of [go] calls. */
        var goCalls = 0

        override val state: JsAny?
            get() = entries[index].state

        override val popStateEvents: Flow<PopStateEvent> = callbackFlow {
            val callback: (PopStateEvent) -> Unit = { event -> trySend(event) }
            popStateListeners.add(callback)
            awaitClose { popStateListeners.remove(callback) }
        }

        override fun pushState(data: JsAny?, url: String?) {
            pushStateCalls++

            // Standard browser behavior: pushing a new state clears any existing "forward" history.
            while (entries.size > index + 1) {
                entries.removeLast()
            }

            // Record the new state in the history stack and move the index forward.
            entries.add(Entry(data, url))
            index++
        }

        override fun replaceState(data: JsAny?, url: String?) {
            replaceStateCalls++
            entries[index] = Entry(data, url)
        }

        override suspend fun go(delta: Int) {
            if (delta == 0) return
            goCalls++
            index += delta

            // If enabled, automatically simulate the 'popstate' event that real browsers fire
            // after a navigation. Some tests disable this to trigger events manually.
            if (emitPopState) {
                emitPopState(entries[index].state)
            }
        }

        /** Manually triggers a [PopStateEvent] with the given [state]. */
        fun emitPopState(state: JsAny?) {
            val event =
                PopStateEvent(
                    type = WindowCompat.TYPE_POP_STATE,
                    eventInitDict = PopStateEventInit(state),
                )

            // This manually triggers the registration callbacks, simulating the browser's
            // asynchronous event dispatch.
            for (listener in popStateListeners) {
                listener.invoke(event)
            }
        }
    }

    /**
     * Sets the navigation information for this [NavigationEventHandler] by specifying a
     * [currentIndex] into a list of [entries].
     *
     * This helper simplifies setting up complex history states (back, current, and forward) in
     * tests.
     *
     * @param currentIndex the index of the [entries] that should be the current info
     * @param entries the list of all navigation entries to be split into back, current, and forward
     */
    private fun <T : TestInfo> NavigationEventHandler<T>.setInfo(
        currentIndex: Int,
        vararg entries: T,
    ) {
        // Partition the flat list of entries into back, current, and forward history.
        setInfo(
            currentInfo = entries[currentIndex],
            backInfo = entries.take(currentIndex),
            forwardInfo = entries.drop(currentIndex + 1),
        )
    }
}
