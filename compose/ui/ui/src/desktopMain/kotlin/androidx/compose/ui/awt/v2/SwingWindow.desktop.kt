/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.awt.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.awt.toAwtRectangleSizeRoundedUp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.requireReal
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.componentListenerRef
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setUndecoratedSafely
import androidx.compose.ui.util.windowListenerRef
import androidx.compose.ui.util.windowStateListenerRef
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.UndecoratedWindowDecoration
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowLocationTracker
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.toDpRect
import androidx.compose.ui.window.resizerThickness
import androidx.compose.ui.window.roundToDimensionOrNull
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowGeometryProviderScope
import androidx.compose.ui.window.v2.WindowScreenProvider
import androidx.compose.ui.window.v2.WindowScreenProviderScope
import androidx.compose.ui.window.v2.WindowState
import androidx.compose.ui.window.v2.rememberWindowState
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * Similar to the corresponding [androidx.compose.ui.window.v2.Window] function, but additionally
 * allows configuring the underlying AWT window before it has been made displayable by providing an
 * [init] block.
 *
 * This is useful to:
 * - Set window properties which cannot be changed after it has been made displayable, such as
 *   [java.awt.Window.setType].
 * - Adding listeners for events that can occur when the window becomes displayable/visible.
 *
 * IMPORTANT: this function should not be used to set properties which can be changed after the
 * window has been made displayable. Doing so can cause your code to stop working in the future if
 * a parameter that controls this property is added to this function.
 * For example, if you set the window's minimum size in [init] and later a `minimumSize` parameter
 * is added to this function, it will override your setting of the minimum size in [init].
 *
 * To set these kinds of properties, use this pattern instead:
 * ```
 * Window( ... ) {
 *     // Window content here
 *     LaunchedEffect(window) {
 *         // Configure window here
 *     }
 * }
 * ```
 *
 * Note: this function may be moved to `androidx.compose.ui.awt` before stabilization.
 *
 * @see androidx.compose.ui.window.v2.Window
 */
@ExperimentalComposeUiApi
@Composable
@ComposableOpenTarget(-1)
fun SwingWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration = WindowDecoration.SystemDefault,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    minSize: DpSize = DpSize.Unspecified,
    maxSize: DpSize = DpSize.Unspecified,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    init: (ComposeWindow) -> Unit,
    content: @Composable FrameWindowScope.() -> Unit
) {
    val currentState by rememberUpdatedState(state)
    val currentTitle by rememberUpdatedState(title)
    val currentIcon by rememberUpdatedState(icon)
    val currentDecoration by rememberUpdatedState(decoration)
    val currentTransparent by rememberUpdatedState(transparent)
    val currentResizable by rememberUpdatedState(resizable)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentFocusable by rememberUpdatedState(focusable)
    val currentAlwaysOnTop by rememberUpdatedState(alwaysOnTop)
    val currentMinSize by rememberUpdatedState(minSize)
    val currentMaxSize by rememberUpdatedState(maxSize)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    val listeners = remember {
        object {
            var windowListenerRef = windowListenerRef()
            var windowStateListenerRef = windowStateListenerRef()
            var componentListenerRef = componentListenerRef()

            fun removeFromAndClear(window: ComposeWindow) {
                windowListenerRef.unregisterFromAndClear(window)
                windowStateListenerRef.unregisterFromAndClear(window)
                componentListenerRef.unregisterFromAndClear(window)
            }
        }
    }

    val coroutineContext = rememberCoroutineScope().coroutineContext

    var window: ComposeWindow? by remember { mutableStateOf(null) }
    SwingWindow(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            val graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            val currentDevice = currentState._screenId?.let { screenId ->
                graphicsDevices.firstOrNull { it.iDstring == screenId }
            }
            val initialDevice = currentDevice
                ?: state.screenRequests.tryReceive().getOrNull()?.getInitialScreenDevice()
                ?: WindowScreenProvider.Default.getInitialScreenDevice()

            val wnd = ComposeWindow(
                graphicsConfiguration = initialDevice.defaultConfiguration,
                coroutineContext = coroutineContext
            )

            // close state is controlled by WindowState.isOpen
            wnd.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            listeners.windowListenerRef.registerWithAndSet(
                wnd,
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        currentOnCloseRequest()
                    }
                }
            )
            listeners.windowStateListenerRef.registerWithAndSet(wnd) {
                currentState._placement = wnd.placement
                currentState._isMinimized = wnd.isMinimized
            }
            listeners.componentListenerRef.registerWithAndSet(
                wnd,
                object : ComponentAdapter() {
                    fun applyBoundsChanges() {
                        currentState._bounds = wnd.bounds.toDpRect()
                        if (currentState._screenId != wnd.graphicsConfiguration.device.iDstring) {
                            currentState._screenId = wnd.graphicsConfiguration.device.iDstring
                        }
                    }

                    override fun componentShown(e: ComponentEvent) {
                        // Initialize all state properties
                        currentState._placement = wnd.placement
                        currentState._isMinimized = wnd.isMinimized
                        applyBoundsChanges()
                        currentState.isInitialized = true
                    }

                    override fun componentResized(e: ComponentEvent) {
                        // we check placement here and in windowStateChanged,
                        // because fullscreen changing doesn't
                        // fire windowStateChanged, only componentResized
                        currentState._placement = wnd.placement
                        applyBoundsChanges()
                    }

                    override fun componentMoved(e: ComponentEvent) {
                        applyBoundsChanges()
                    }
                }
            )
            WindowLocationTracker.onWindowCreated(wnd)

            init(wnd)
            window = wnd

            wnd
        },
        dispose = {
            WindowLocationTracker.onWindowDisposed(it)
            // We need to remove them because AWT can still call them after dispose()
            listeners.removeFromAndClear(it)
            it.dispose()
        },
        update = { window ->
            updater.update {
                set(currentTitle, window::setTitle)
                set(currentIcon, window::setIcon)
                set(currentDecoration is UndecoratedWindowDecoration, window::setUndecoratedSafely)
                set(currentTransparent, window::isTransparent::set)
                set(currentResizable, window::setResizable)
                set(currentEnabled, window::setEnabled)
                set(currentFocusable, window::setFocusableWindowState)
                set(currentAlwaysOnTop, window::setAlwaysOnTop)
                set(currentMinSize) { window.minimumSize = it.roundToDimensionOrNull() }
                set(currentMaxSize) { window.maximumSize = it.roundToDimensionOrNull() }
                set(currentDecoration.resizerThickness, window::undecoratedResizerThickness::set)
            }

            if (!window.isDisplayable) {
                window.initializePlacement(currentState)
                window.initializeBounds(currentState)

                // Need to make the window displayable, to make awt.SwingWindow render the first
                // frame before the window is visible.
                // Check isDisplayable again because initializeBounds could have already
                // called pack(), and we don't need to do it twice
                if (!window.isDisplayable) {
                    window.preferredSize = window.size
                    window.pack()  // Sizes to preferred size
                }
            }
        },
        content = content
    )

    LaunchedEffect(window, state) {
        val window = window ?: return@LaunchedEffect
        launch {
            while (isActive) {
                window.setScreenFrom(state.screenRequests.receive())
            }
        }
        launch {
            while (isActive) {
                window.placement = state.placementRequests.receive()
            }
        }
        launch {
            while (isActive) {
                window.isMinimized = state.isMinimizedRequests.receive()
            }
        }
        launch {
            while (isActive) {
                window.setBoundsFrom(state.boundsRequests.receive())
            }
        }
    }
}

internal fun WindowScreenProvider.getInitialScreenDevice(
    defaultDevice: GraphicsDevice? = null
): GraphicsDevice {
    val lastActiveConfig = WindowLocationTracker.lastActiveGraphicsConfiguration
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val devices = env.screenDevices
    val actualDefaultDevice = defaultDevice
        ?: devices.firstOrNull { it.iDstring == lastActiveConfig?.device?.iDstring }
        ?: env.defaultScreenDevice
    val scope = WindowScreenProviderScope(devices.toList(), actualDefaultDevice)
    return with(scope) {
        getScreen().device
    }
}

private fun ComposeWindow.initializePlacement(state: WindowState) {
    val placementRequest = state.placementRequests.tryReceive().getOrNull()
    val currentPlacement = state._placement

    placement = placementRequest ?: currentPlacement ?: WindowPlacement.Floating
}

internal fun Window.initializeBounds(
    boundsRequests: ReceiveChannel<WindowBoundsProvider>,
    currentBounds: DpRect?,
    parentWindow: Window?,
    measureContent: (Constraints) -> IntSize
) {
    var boundsRequest = boundsRequests.tryReceive().getOrNull()

    // Prioritize requests, then currentBounds, then default
    if (boundsRequest != null) {
        // Apply all pending requests
        while (boundsRequest != null) {
            setBoundsFrom(boundsRequest, parentWindow, measureContent)
            boundsRequest = boundsRequests.tryReceive().getOrNull()
        }
    } else if (currentBounds != null) {
        bounds = currentBounds.toAwtRectangleSizeRoundedUp()
    } else {
        setBoundsFrom(WindowBoundsProvider.Default, parentWindow, measureContent)
    }
}

internal fun Window.setBoundsFrom(
    boundsProvider: WindowBoundsProvider,
    parentWindow: Window?,
    measureContent: (Constraints) -> IntSize
) {
    if (!isDisplayable) {
        // Give it a preferred size to avoid measuring via ComposeSceneMediator.preferredSize
        // when pack() is called
        preferredSize = java.awt.Dimension(0, 0)
        pack()
    }

    val scope = WindowGeometryProviderScope(
        parentWindow = parentWindow,
        window = this,
        measureContent = measureContent
    )
    with(scope) {
        bounds = boundsProvider.getBounds().requireReal().toAwtRectangleSizeRoundedUp()
    }
}

private fun ComposeWindow.initializeBounds(state: WindowState) {
    initializeBounds(state.boundsRequests, state._bounds, null, ::measureContent)
}

private fun ComposeWindow.setBoundsFrom(boundsProvider: WindowBoundsProvider) {
    setBoundsFrom(boundsProvider, null, ::measureContent)
}

internal fun Window.setScreenFrom(screenProvider: WindowScreenProvider) {
    val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    val defaultDevice = graphicsConfiguration.device

    val scope = WindowScreenProviderScope(
        devices = devices.toList(),
        defaultDevice = defaultDevice
    )
    val device = with(scope) { screenProvider.getScreen().device }
    setScreenFrom(device)
}

/** Moves the window to the given screen, preserving relative position within the screen. */
private fun Window.setScreenFrom(device: GraphicsDevice) {
    if (device == graphicsConfiguration.device) return

    val toolkit = Toolkit.getDefaultToolkit()

    val configuration = device.defaultConfiguration
    val screenBounds = configuration.bounds
    val screenInsets = toolkit.getScreenInsets(configuration)

    val currentConfiguration = graphicsConfiguration
    val currentScreenBounds = currentConfiguration.bounds
    val currentScreenInsets = toolkit.getScreenInsets(currentConfiguration)
    val currentRelativeX = x - currentScreenBounds.x - currentScreenInsets.left
    val currentRelativeY = y - currentScreenBounds.y - currentScreenInsets.top

    setLocation(
        screenBounds.x + screenInsets.left + currentRelativeX,
        screenBounds.y + screenInsets.top + currentRelativeY,
    )
}