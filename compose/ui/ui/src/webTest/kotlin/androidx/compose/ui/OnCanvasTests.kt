/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.window.ComposeViewportConfiguration
import kotlin.coroutines.suspendCoroutine
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.ShadowRoot
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.get

/**
 * An interface with helper functions to initialise the tests
 */

private const val containerId: String = "canvasApp"

private external interface CanReplaceChildren {
    // this is a standard method for (among other things) emptying DOM element content
    // https://developer.mozilla.org/en-US/docs/Web/API/Element/replaceChildren
    // TODO: add this to our kotlin web external definitions
    fun replaceChildren()
}

internal interface OnCanvasTests {

    @BeforeTest
    fun beforeTest() {
        /** TODO: [kotlin.test.AfterTest] is fixed only in kotlin 2.0
        see https://youtrack.jetbrains.com/issue/KT-61888
         */
        resetCanvas()
    }

    private fun resetCanvas() {
        (getContainer() as CanReplaceChildren).replaceChildren()
    }

    private fun getContainer() = document.getElementById(containerId) ?: error("failed to get canvas with id ${containerId}")

    private fun getAppRoot() = getShadowRoot().children[1] as HTMLElement

    fun getA11YContainer(): HTMLElement? {
        return if (getAppRoot().children.length < 3) {
            null
        } else {
            // The expected order is: canvas, interop container <div>, a11y container <div>
            getAppRoot().children[2] as HTMLElement
        }
    }

    fun getShadowRoot(): ExtendedShadowRoot =
        (getContainer().shadowRoot as? ExtendedShadowRoot) ?: error("failed to get shadowRoot")

    fun getCanvas(): HTMLCanvasElement {
        val canvas = (getShadowRoot().querySelector("canvas") as? HTMLCanvasElement) ?: error("failed to get canvas")
        return canvas
    }

    suspend fun createComposeWindow(
        configure: ComposeViewportConfiguration.() -> Unit = {},
        content: @Composable () -> Unit
    ) {
        ComposeViewport(viewportContainerId = containerId, configure = configure, content = content)

        suspendCoroutine { continuation ->
            // This helps reduce the flakiness.
            // A potential cause of flakiness: the default Coroutine Dispatcher regularly postpones
            // the resumption of the tasks in its queue to the next frame.
            // (it does so to let the event loop run / release the single thread)
            // I don't expect any issue from doing this, since a test will suspend and won't do anything.
            window.requestAnimationFrame { continuation.resumeWith(Result.success(it)) }
        }
    }

    suspend fun awaitA11YChanges(timeout: Duration = 2.seconds) {
        val a11yContainer = getA11YContainer() ?: return
        var prevTime = currentTimeMillis()

        fun skipFramesUntil(condition: () -> Boolean, onTrue: () -> Unit) {
            val currentTime = currentTimeMillis()
            assertTrue(currentTime - prevTime < timeout.inWholeMilliseconds, "awaitA11YChanges timed out after $timeout")
            prevTime = currentTime
            window.requestAnimationFrame {
                if (!condition()) {
                    skipFramesUntil(condition, onTrue)
                } else {
                    onTrue()
                }
            }
        }

        suspendCoroutine { continuation ->
            val initialContent = a11yContainer.innerHTML
            skipFramesUntil(
                condition = { a11yContainer.innerHTML != initialContent },
                onTrue = { continuation.resumeWith(Result.success(Unit)) }
            )
        }
    }

    fun dispatchEvents(vararg events: Event) {
        dispatchEvents(getCanvas(), *events)
    }

    fun dispatchEvents(element: EventTarget = getCanvas(), vararg events: Event) {
        for (event in events) {
            element.dispatchEvent(event)
        }
    }

    fun requestFocus() {
        getCanvas().focus()
    }

    fun runApplicationTest(body: suspend WebApplicationScope.() -> Unit): TestResult {
        return runTest {
            WebApplicationScope(this).body()
        }
    }
}

internal fun <T> Channel<T>.sendFromScope(value: T, scope: CoroutineScope = MainScope()) {
    scope.launch(Dispatchers.Unconfined) { send(value) }
}

internal class WebApplicationScope(
    private val scope: CoroutineScope,
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    private val initialRecomposers = Recomposer.runningRecomposers.value

    suspend fun awaitIdle() {
        awaitWithYield()

        Snapshot.sendApplyNotifications()

        for (recomposerInfo in Recomposer.runningRecomposers.value - initialRecomposers) {
            recomposerInfo.state.takeWhile { it > Recomposer.State.Idle }.collect()
        }

        awaitWithYield()
    }

    /**
     * awaitAnimationFrame is needed for text input tests,
     * due to DomInputStrategy implementation relying on animation frame events.
     */
    internal suspend fun awaitAnimationFrame() {
        suspendCoroutine { continuation ->
            window.requestAnimationFrame { continuation.resumeWith(Result.success(Unit)) }
        }
    }

    suspend fun TestInputState.awaitAndAssertTextEquals(expected: String, message: String? = null) {
        awaitAnimationFrame()
        awaitIdle()
        assertEquals(expected = expected, actual = text, message = message)
    }

    suspend fun TestInputState.awaitAndAssertTextMatches(expected: Regex, message: String? = null) {
        awaitAnimationFrame()
        awaitIdle()
        assertTrue(expected.matches(text), message)
    }
}

internal interface TestInputState {
    val text: CharSequence

    @Composable
    fun createBasicTextField(focusRequester: FocusRequester)
}

/**
 * This is heavily inspired by (if not to say borrowed from) the desktop awaitEDT helper function
 */
private suspend fun awaitWithYield() {
    repeat(100) {
        yield()
    }
}

@JsName("ShadowRoot")
internal external class ExtendedShadowRoot : ShadowRoot {

    fun elementFromPoint(x: Double, y: Double): Element
}