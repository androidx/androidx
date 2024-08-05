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
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

/**
 * An interface with helper functions to initialise the tests
 */

private const val canvasId: String = "canvasApp"

internal interface OnCanvasTests {
    fun getCanvas() = document.getElementById(canvasId) as HTMLCanvasElement

    fun resetCanvas(): HTMLCanvasElement {
        /** TODO: [kotlin.test.AfterTest] is fixed only in kotlin 2.0
        see https://youtrack.jetbrains.com/issue/KT-61888
         */
        document.getElementById(canvasId)?.remove()

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        canvas.setAttribute("tabindex", "0")

        document.body!!.appendChild(canvas)
        return canvas
    }

    fun createComposeWindow(content: @Composable () -> Unit) {
        CanvasBasedWindow(canvasElementId = canvasId, content = content)
    }
}

internal fun <T> Channel<T>.sendFromScope(value: T, scope: CoroutineScope = GlobalScope) {
    scope.launch(Dispatchers.Unconfined) { send(value) }
}