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

@file:OptIn(
    ExperimentalCoroutinesApi::class
    // ExperimentalWasmJsInterop::class
)

package androidx.navigationevent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.w3c.dom.PopStateEvent
import org.w3c.dom.PopStateEventInit
import org.w3c.dom.events.Event

class BrowserInputTest {

    private object A : NavigationEventInfo()

    private object B : NavigationEventInfo()

    private object C : NavigationEventInfo()

    private object X : NavigationEventInfo()

    private object Y : NavigationEventInfo()

    private object Z : NavigationEventInfo()

    @Test
    fun initialStateWithSingleInfo() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        advanceUntilIdle()

        assertEquals(listOf(0).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun initialStateWithMultipleInfos() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        handler.setInfos(listOf(A, B, C), 1)
        dispatcher.addHandler(handler)

        advanceUntilIdle()

        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun changeDestination() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)

        advanceUntilIdle()

        assertEquals(listOf(0, 1).map { it.toJsNumber() }, window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun newInfosAreLonger() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 1)

        advanceUntilIdle()

        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun newInfosAreShorter() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        handler.setInfos(listOf(A, B, C), 1)
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeDestinationAndHierarchicalBack() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)
        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        assertEquals(listOf(0, 1).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeDestinationAndChronologicalBack() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object : NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)
        handler.setInfos(listOf(A, B), 0)

        advanceUntilIdle()

        assertEquals(listOf(0, 1).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun browserBackWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onBackCompleted() {
                    if (backInfo.isNotEmpty()) {
                        setInfo(
                            backInfo.last(),
                            backInfo.dropLast(1),
                            listOf(currentInfo) + forwardInfo,
                        )
                    }
                }
            }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)

        advanceUntilIdle()

        window.history.go(-1)

        advanceUntilIdle()

        assertEquals(listOf(0, 1).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)

        assertEquals(listOf(A, B), dispatcher.history.value.mergedHistory)
        assertEquals(0, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserForwardWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true, isForwardEnabled = true) {
                override fun onForwardCompleted() {
                    if (forwardInfo.isNotEmpty()) {
                        setInfo(
                            forwardInfo.first(),
                            backInfo + listOf(currentInfo),
                            forwardInfo.drop(1),
                        )
                    }
                }
            }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 0)

        advanceUntilIdle()

        window.history.go(1)

        advanceUntilIdle()

        assertEquals(listOf(0, 1).map { it.toJsNumber() }, window.history.states)
        assertEquals(1, window.history.index)

        assertEquals(listOf(A, B), dispatcher.history.value.mergedHistory)
        assertEquals(1, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserMultipleBackWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        var invokedCount = 0
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onBackCompleted() {
                    invokedCount++
                    if (backInfo.isNotEmpty()) {
                        setInfo(
                            backInfo.last(),
                            backInfo.dropLast(1),
                            listOf(currentInfo) + forwardInfo,
                        )
                    }
                }
            }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 2)

        advanceUntilIdle()

        window.history.go(-2)

        advanceUntilIdle()

        assertEquals(2, invokedCount)
        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)

        assertEquals(listOf(A, B, C), dispatcher.history.value.mergedHistory)
        assertEquals(0, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserMultipleForwardWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        var invokedCount = 0
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true, isForwardEnabled = true) {
                override fun onForwardCompleted() {
                    invokedCount++
                    if (forwardInfo.isNotEmpty()) {
                        setInfo(
                            forwardInfo.first(),
                            backInfo + listOf(currentInfo),
                            forwardInfo.drop(1),
                        )
                    }
                }
            }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 0)

        advanceUntilIdle()

        window.history.go(2)

        advanceUntilIdle()

        assertEquals(2, invokedCount)
        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(2, window.history.index)

        assertEquals(listOf(A, B, C), dispatcher.history.value.mergedHistory)
        assertEquals(2, dispatcher.history.value.currentIndex)
    }

    @Test
    fun goesToAnInvalidEntryGoesBackDirectly() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onForwardCompleted() {
                    require(false) { "Should not be called" }
                }
            }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 2)
        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        window.history.go(2)

        advanceUntilIdle()

        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeHandlersShouldWork() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onForwardCompleted() {
                    require(false) { "Should not be called" }
                }
            }
        handler.setInfos(listOf(A, B, C), 0)
        dispatcher.addHandler(handler)

        val handler2 =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onForwardCompleted() {
                    require(false) { "Should not be called" }
                }
            }
        handler2.setInfos(listOf(X, Y, Z), 2)
        dispatcher.addHandler(handler2)

        advanceUntilIdle()

        assertEquals(listOf(0, 1, 2).map { it.toJsNumber() }, window.history.states)
        assertEquals(2, window.history.index)
    }

    @Test
    fun combinedHandlersShouldWork() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onForwardCompleted() {
                    require(false) { "Should not be called" }
                }
            }
        handler.setInfos(listOf(A, B, C), 1)
        dispatcher.addHandler(handler)

        val handler2 =
            object : NavigationEventHandler<NavigationEventInfo>(A, true) {
                override fun onForwardCompleted() {
                    require(false) { "Should not be called" }
                }
            }
        handler2.setInfos(listOf(X, Y, Z), 1)

        dispatcher.addHandler(handler2)

        advanceUntilIdle()

        // [A, X, Y*, Z]
        assertEquals(listOf(0, 1, 2, 3).map { it.toJsNumber() }, window.history.states)
        assertEquals(2, window.history.index)
    }
}

private fun <T : NavigationEventInfo> NavigationEventHandler<T>.setInfos(
    entries: List<T>,
    currentIndex: Int,
) {
    setInfo(entries[currentIndex], entries.take(currentIndex), entries.drop(currentIndex + 1))
}

private class TestWindow : BrowserWindow {
    override val history: TestBrowserHistory = TestBrowserHistory(this)
    val eventListeners = mutableMapOf<String, MutableList<(Event) -> Unit>>()

    override fun addEventListener(type: String, callback: (Event) -> Unit) {
        val callbackList = eventListeners.getOrPut(type) { mutableListOf() }
        callbackList.add(callback)
    }

    override fun removeEventListener(type: String, callback: (Event) -> Unit) {
        eventListeners[type]?.remove(callback)
    }
}

private class TestBrowserHistory(private val window: TestWindow) : BrowserHistory {

    data class Entry(val state: JsAny?, val url: String?)

    val entries = mutableListOf<Entry>(Entry(null, null))

    val states
        get() = entries.map { it.state }

    var index = 0
        private set

    override val state: JsAny?
        get() = entries[index].state

    override fun push(data: JsAny?, url: String?) {
        // Removing
        for (i in entries.size - 1 downTo index + 1) {
            entries.removeAt(i)
        }

        // Adding
        entries.add(Entry(data, url))
        index++
    }

    override fun replace(data: JsAny?, url: String?) {
        entries[index] = Entry(data, url)
    }

    override suspend fun go(delta: Int) {
        index += delta
        window.eventListeners[BrowserInput.TYPE_POPSTATE]?.forEach {
            it.invoke(
                PopStateEvent(
                    BrowserInput.TYPE_POPSTATE,
                    PopStateEventInit(entries[index].state, false, false, false),
                )
            )
        }
    }
}
