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

package androidx.compose.ui.window

import androidx.lifecycle.Lifecycle
import isHeadlessBrowser
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.FocusEvent


class ComposeWindowLifecycleTest {
    private val canvasId = "canvas1"


    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
    }

    @Test
    fun allEvents() {
        if (isHeadlessBrowser()) return
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        document.body!!.appendChild(canvas)

        val lifecycleOwner = ComposeWindow(
            canvas = canvas,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        document.dispatchEvent(FocusEvent("blur"))
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        document.dispatchEvent(FocusEvent("focus"))
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)
    }
}