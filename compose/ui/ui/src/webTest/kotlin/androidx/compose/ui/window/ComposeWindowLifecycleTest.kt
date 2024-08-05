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

import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.sendFromScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest


class ComposeWindowLifecycleTest : OnCanvasTests {

    @BeforeTest
    fun setup() {
        resetCanvas()
    }

    @Test
    @Ignore // ignored while investigating CI issues: this test opens a new browser window which can be the cause
    fun allEvents() = runTest {
        val canvas = getCanvas()
        canvas.focus()

        val lifecycleOwner = ComposeWindow(
            canvas = canvas,
            content = {},
            state = DefaultWindowState(document.documentElement!!)
        )

        val eventsChannel = Channel<Lifecycle.Event>(10)

        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                eventsChannel.sendFromScope(event)
            }
        })

        assertEquals(Lifecycle.State.CREATED, eventsChannel.receive().targetState)
        assertEquals(Lifecycle.State.STARTED, eventsChannel.receive().targetState)
        assertEquals(Lifecycle.State.RESUMED, eventsChannel.receive().targetState)

        // Browsers don't allow to blur the window from code:
        // https://developer.mozilla.org/en-US/docs/Web/API/Window/blur
        // So we simulate a new tab being open:
        val anotherWindow = window.open("about:config")
        assertTrue(anotherWindow != null)
        assertEquals(Lifecycle.State.STARTED, eventsChannel.receive().targetState)

        // Now go back to the original window
        anotherWindow.close()
        assertEquals(Lifecycle.State.RESUMED, eventsChannel.receive().targetState)
    }
}
