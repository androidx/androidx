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

import androidx.compose.ui.assertThat
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.scene.ComposeContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.awt.Frame
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import javax.swing.JFrame
import javax.swing.JLayeredPane
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeContainerLifecycleOwnerTest {
    @Test
    fun allEvents() = runApplicationTest {
        val window = JFrame()
        try {
            val allEvents = ChannelEventObserver()
            val pane = TestComposePanel(window, allEvents)
            window.contentPane.add(pane)

            // initial state for a not-yet-shown window
            allEvents.waitFor(Lifecycle.Event.ON_CREATE)

            // show window
            window.showAndMoveToFront()
            allEvents.waitFor(Lifecycle.Event.ON_START)
            allEvents.waitFor(Lifecycle.Event.ON_RESUME)

            // show another window, the window under test loses focus
            val anotherWindow = JFrame()
            try {
                anotherWindow.showAndMoveToFront()

                allEvents.waitFor(Lifecycle.Event.ON_PAUSE)

                // another window is closed, the window under test regains focus
                anotherWindow.isVisible = false
                window.toFront()
                allEvents.waitFor(Lifecycle.Event.ON_RESUME)
            } finally {
                anotherWindow.dispose()
            }

            if (window.toolkit.isFrameStateSupported(Frame.ICONIFIED)) {
                // minimize window
                if (window.setExtendedStateSucceeds(Frame.ICONIFIED)) {
                    allEvents.waitFor(Lifecycle.Event.ON_PAUSE)
                    allEvents.waitFor(Lifecycle.Event.ON_STOP)

                    // restore window
                    window.extendedState = Frame.NORMAL
                    allEvents.waitFor(Lifecycle.Event.ON_START)
                    allEvents.waitFor(Lifecycle.Event.ON_RESUME)
                } else {
                    println("Setting ICONIFIED window state failed")
                }
            } else {
                println("ICONIFIED window state not supported")
            }

            // close window
            window.contentPane.remove(pane)
            pane.dispose()
            allEvents.waitFor(Lifecycle.Event.ON_PAUSE)
            allEvents.waitFor(Lifecycle.Event.ON_STOP)
            allEvents.waitFor(Lifecycle.Event.ON_DESTROY)

            // no more events
            assertTrue(allEvents.tryReceive().isFailure)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun detachAndReattach() = runApplicationTest {
        val window = JFrame()
        try {
            val allEvents = ChannelEventObserver()
            val pane = TestComposePanel(window, allEvents)
            window.contentPane.add(pane)

            // initial state
            allEvents.waitFor(Lifecycle.Event.ON_CREATE)

            window.showAndMoveToFront()
            allEvents.waitFor(Lifecycle.Event.ON_START)
            allEvents.waitFor(Lifecycle.Event.ON_RESUME)

            window.contentPane.remove(pane)
            allEvents.waitFor(Lifecycle.Event.ON_PAUSE)
            allEvents.waitFor(Lifecycle.Event.ON_STOP)

            window.contentPane.add(pane)
            allEvents.waitFor(Lifecycle.Event.ON_START)
            allEvents.waitFor(Lifecycle.Event.ON_RESUME)

            // no more events
            assertTrue(allEvents.tryReceive().isFailure)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun windowDeiconifiedWithoutAddNotify() = runApplicationTest {
        val window = JFrame()
        try {
            val pane = JLayeredPane()
            val allEvents = ChannelEventObserver()
            val container = ComposeContainer(
                container = pane,
                skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                window = window,
            )
            container.architectureComponentsOwner.lifecycle.addObserver(allEvents)
            window.contentPane.add(pane)

            // initial state
            allEvents.waitFor(Lifecycle.Event.ON_CREATE)

            window.state = JFrame.NORMAL

            // no more events
            assertTrue(allEvents.tryReceive().isFailure)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun windowFocusedWithoutAddNotify() = runApplicationTest {
        val window = JFrame()
        try {
            val pane = JLayeredPane()
            val allEvents = ChannelEventObserver()
            val container = ComposeContainer(
                container = pane,
                skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                window = window,
            )
            container.architectureComponentsOwner.lifecycle.addObserver(allEvents)
            window.contentPane.add(pane)

            // initial state
            allEvents.waitFor(Lifecycle.Event.ON_CREATE)

            window.showAndMoveToFront()

            // no more events
            assertTrue(allEvents.tryReceive().isFailure)
        } finally {
            window.dispose()
        }
    }

    @Test
    fun lateAddNotify() = runApplicationTest {
        val window = JFrame()
        val pane = JLayeredPane()
        val allEvents = ChannelEventObserver()
        val container = ComposeContainer(
            container = pane,
            skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
            window = window,
        )
        container.architectureComponentsOwner.lifecycle.addObserver(allEvents)
        window.contentPane.add(pane)

        // initial state
        allEvents.waitFor(Lifecycle.Event.ON_CREATE)

        window.showAndMoveToFront()
        window.state = JFrame.NORMAL

        // no events yet
        assertTrue(allEvents.tryReceive().isFailure)

        // addNotify arrives after various window events
        container.addNotify()
        allEvents.waitFor(Lifecycle.Event.ON_START)
        allEvents.waitFor(Lifecycle.Event.ON_RESUME)

        // no more events
        assertTrue(allEvents.tryReceive().isFailure)

        window.dispose()
    }

    private class ChannelEventObserver: LifecycleEventObserver, Channel<Lifecycle.Event> by Channel(capacity = 8) {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            trySendBlocking(event)
            if (event == Lifecycle.Event.ON_DESTROY) {
                close()
            }
        }
    }

    private class TestComposePanel(window: JFrame, observer: LifecycleEventObserver) : JLayeredPane() {
        private val container: ComposeContainer = ComposeContainer(
            container = this,
            skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
            window = window,
        )

        init {
            container.architectureComponentsOwner.lifecycle.addObserver(observer)
            isFocusable = true
        }

        override fun addNotify() {
            super.addNotify()
            container.addNotify()
        }

        override fun removeNotify() {
            container.removeNotify()
            super.removeNotify()
        }

        fun dispose() {
            container.dispose()
        }
    }

    private fun Window.showAndMoveToFront() {
        isVisible = true
        toFront()
    }

    private suspend fun JFrame.setExtendedStateSucceeds(state: Int): Boolean =
        withTimeoutOrNull(1.seconds) {
            val actualNewState = suspendCancellableCoroutine { cont ->
                extendedState = state
                addWindowStateListener(object: WindowStateListener {
                    override fun windowStateChanged(e: WindowEvent) {
                        removeWindowStateListener(this)
                        cont.resume(
                            value = e.newState,
                            onCancellation = { _, _, _ -> }
                        )
                    }
                })
            }
            return@withTimeoutOrNull actualNewState == state
        } ?: false

    // Some window managers (on Linux) generate unexpected events, such as iconification when the
    // window is first made visible. This means it's not possible to check for the expected stream
    // of events exactly. Instead, we skip unexpected events, waiting for the next expected one.
    private suspend fun <E> Channel<E>.waitFor(value: E, timeout: Duration = 1.seconds) {
        withContext(Dispatchers.Default) {
            var lastReceived: E? = null
            withTimeoutOrNull(timeout) {
                while (true) {
                    lastReceived = receive()
                    if (lastReceived == value) break
                }
            }
            assertThat(lastReceived).isEqualTo(value)
        }
    }
}