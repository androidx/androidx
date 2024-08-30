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
import androidx.compose.ui.window.ComposeWindow
import androidx.compose.ui.window.DefaultWindowState
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.events.Event

/**
 * An interface with helper functions to initialise the tests
 */

private const val canvasId: String = "canvasApp"

internal interface OnCanvasTests {

    companion object {
        private var injected: Boolean = false
        private fun injectDefaultStyles() {
            if (injected) return
            injected = true
            document.head!!.appendChild(
                (document.createElement("style") as HTMLStyleElement).apply {
                    type = "text/css"
                    appendChild(
                        document.createTextNode(
                            "body { margin: 0;}}"
                        )
                    )
                }
            )
        }
    }

    fun getCanvas() = document.getElementById(canvasId) as HTMLCanvasElement

    private fun resetCanvas() {
        /** TODO: [kotlin.test.AfterTest] is fixed only in kotlin 2.0
        see https://youtrack.jetbrains.com/issue/KT-61888
         */
        document.getElementById(canvasId)?.remove()

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        canvas.setAttribute("tabindex", "0")

        document.body!!.appendChild(canvas)
    }

    fun composableContent(content: @Composable () -> Unit) {
        // We should use this method whenever we are relying on the fact that document coordinates start exactly at (0, 0)
        // TODO: strictly speaking, this way one test suit affects the other in that sense that styles can be injected by different tests suite
        injectDefaultStyles()
        resetCanvas()
        createComposeViewport(content)
    }

    fun createComposeViewport(content: @Composable () -> Unit) {
        ComposeWindow(canvas = getCanvas(), content = content, state = DefaultWindowState(document.documentElement!!))
    }

    fun dispatchEvents(vararg events: Any) {
        val canvas = getCanvas()
        for (event in events) {
            canvas.dispatchEvent(event as Event)
        }
    }
}

internal fun <T> Channel<T>.sendFromScope(value: T, scope: CoroutineScope = GlobalScope) {
    scope.launch(Dispatchers.Unconfined) { send(value) }
}