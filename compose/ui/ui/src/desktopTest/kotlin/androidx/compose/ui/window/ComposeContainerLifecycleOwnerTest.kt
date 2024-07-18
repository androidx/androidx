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
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.scene.ComposeContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.time.Duration
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeContainerLifecycleOwnerTest {
    @Test
    fun allEvents() = runTest {
        val window = JFrame().apply {
            isVisible = false
        }
        val allEvents = ChannelEventObserver()
        val pane = TestComposePanel(window, allEvents)

        // initial state for a not-yet-shown window
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_CREATE)

        // show window
        SwingUtilities.invokeAndWait {
            window.isVisible = true
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_START)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)

        // show another window, the window under test looses focus
        val anotherWindow = JFrame().apply {
            isVisible = true
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_PAUSE)

        // another window is closed, the window under test regains focus
        anotherWindow.dispose()
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)

        // cannot check window minimization/restoration on CI as we are running in Xvfb without a window manager
        // so disabling this check for now
        /*
        // minimize window
        SwingUtilities.invokeAndWait {
            val toolkit = Toolkit.getDefaultToolkit()
            println("Toolkit ${toolkit::class.qualifiedName} is ICONIFIED supported ${toolkit.isFrameStateSupported(Frame.ICONIFIED)}")
            window.extendedState = Frame.ICONIFIED
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_PAUSE)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_STOP)

        // restore window
        SwingUtilities.invokeAndWait {
            window.extendedState = Frame.NORMAL
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_START)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)
        */

        // close window
        SwingUtilities.invokeAndWait {
            pane.container.dispose()
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_PAUSE)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_STOP)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_DESTROY)
        assertFailsWith<ClosedReceiveChannelException> {
            allEvents.receiveOrTimeout()
        }
    }

    @Test
    fun detachAndReattach() = runTest {
        val window = JFrame()
        val allEvents = ChannelEventObserver()
        val pane = TestComposePanel(window, allEvents)

        // initial state
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_CREATE)

        SwingUtilities.invokeAndWait {
            window.isVisible = true
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_START)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)

        SwingUtilities.invokeAndWait {
            window.remove(pane)
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_PAUSE)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_STOP)

        SwingUtilities.invokeAndWait {
            window.add(pane)
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_START)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)

        // no more events
        assertTrue(allEvents.tryReceive().isFailure)
    }

    @Test
    fun windowDeiconifiedWithoutAddNotify() = runTest {
        val window = JFrame()
        val pane = JLayeredPane()
        val allEvents = ChannelEventObserver()
        var container: ComposeContainer? = null
        SwingUtilities.invokeAndWait {
            container = ComposeContainer(
                container = pane,
                skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                window = window,
                renderSettings = RenderSettings.Default
            ).also {
                it.lifecycle.addObserver(allEvents)
            }
            window.add(pane)
        }

        // initial state
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_CREATE)

        SwingUtilities.invokeAndWait {
            window.state = JFrame.NORMAL
        }

        // no more events
        assertTrue(allEvents.tryReceive().isFailure)
    }

    @Test
    fun windowFocusedWithoutAddNotify() = runTest {
        val window = JFrame().apply {
            isVisible = false
        }
        val pane = JLayeredPane()
        val allEvents = ChannelEventObserver()
        var container: ComposeContainer? = null
        SwingUtilities.invokeAndWait {
            container = ComposeContainer(
                container = pane,
                skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                window = window,
                renderSettings = RenderSettings.Default
            ).also {
                it.lifecycle.addObserver(allEvents)
            }
            window.add(pane)
        }

        // initial state
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_CREATE)

        SwingUtilities.invokeAndWait {
            window.isVisible = true
        }

        // no more events
        assertTrue(allEvents.tryReceive().isFailure)
    }

    @Test
    fun lateAddNotify() = runTest {
        val window = JFrame().apply {
            isVisible = false
        }
        val pane = JLayeredPane()
        val allEvents = ChannelEventObserver()
        var container: ComposeContainer? = null
        SwingUtilities.invokeAndWait {
            container = ComposeContainer(
                container = pane,
                skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                window = window,
                renderSettings = RenderSettings.Default
            ).also {
                it.lifecycle.addObserver(allEvents)
            }
            window.add(pane)
        }

        // initial state
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_CREATE)

        SwingUtilities.invokeAndWait {
            window.isVisible = true
            window.state = JFrame.NORMAL
        }

        // no events yet
        assertTrue(allEvents.tryReceive().isFailure)

        // addNotify arrives after various window events
        SwingUtilities.invokeAndWait {
            container!!.addNotify()
        }
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_START)
        assertThat(allEvents.receiveOrTimeout()).isEqualTo(Lifecycle.Event.ON_RESUME)

        // no more events
        assertTrue(allEvents.tryReceive().isFailure)
    }

    private class ChannelEventObserver: LifecycleEventObserver, Channel<Lifecycle.Event> by Channel(capacity = 8) {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            runBlocking {
                send(event)
                if (event == Lifecycle.Event.ON_DESTROY) {
                    close()
                }
            }
        }
    }

    private class TestComposePanel(window: JFrame, observer: LifecycleEventObserver) : JLayeredPane() {
        lateinit var container: ComposeContainer

        init {
            SwingUtilities.invokeAndWait {
                container = ComposeContainer(
                    container = this,
                    skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
                    window = window,
                    renderSettings = RenderSettings.Default
                )
                container.lifecycle.addObserver(observer)
                window.add(this)
            }
        }

        override fun addNotify() {
            super.addNotify()
            container.addNotify()
        }

        override fun removeNotify() {
            super.removeNotify()
            container.removeNotify()
        }
    }

    private suspend fun <E> Channel<E>.receiveOrTimeout(timeout: Duration = Duration.ofSeconds(1)): E {
        return withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                receive()
            }
        }
    }
}