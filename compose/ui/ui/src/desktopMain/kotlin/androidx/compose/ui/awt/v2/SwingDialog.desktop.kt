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
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.componentListenerRef
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setUndecoratedSafely
import androidx.compose.ui.util.windowListenerRef
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.UndecoratedWindowDecoration
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.toDpRect
import androidx.compose.ui.window.resizerThickness
import androidx.compose.ui.window.roundToDimensionOrNull
import androidx.compose.ui.window.v2.DialogState
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowScreenProvider
import androidx.compose.ui.window.v2.rememberDialogState
import java.awt.Dialog.ModalityType
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// TODO(demin): fix mouse hover after opening a dialog.
//  When we open a modal dialog, ComposeLayer/mouseExited will
//  never be called for the parent window. See ./gradlew run3

/**
 * Similar to the corresponding [androidx.compose.ui.window.v2.DialogWindow] function, but
 * additionally allows configuring the underlying AWT dialog before it has been made displayable,
 * by providing an [init] block.
 *
 * This is useful to:
 * - Set dialog properties which cannot be changed after it has been made displayable, such as
 *   [java.awt.Window.setType].
 * - Adding listeners for events that can occur when the dialog becomes displayable/visible.
 *
 * IMPORTANT: this function should not be used to set properties which can be changed after the
 * window has been made displayable. Doing so can cause your code to stop working in the future if
 * a parameter that controls this property is added to this function.
 * For example, if you set the window's minimum size in [init] and later a `minimumSize` parameter
 * is added to this function, it will override your setting of the minimum size in [init].
 *
 * To set these kinds of properties, use this pattern instead:
 * ```
 * WindowDialog( ... ) {
 *     // Dialog content here
 *     LaunchedEffect(window) {
 *         // Configure dialog here
 *     }
 * }
 * ```
 *
 * Note: this function may be moved to `androidx.compose.ui.awt` before stabilization.
 *
 * @see androidx.compose.ui.window.v2.DialogWindow
 */
@ExperimentalComposeUiApi
@Composable
@ComposableOpenTarget(-1)
fun SwingDialog(
    onCloseRequest: () -> Unit,
    parentWindow: Window? = LocalAwtWindow.current,
    state: DialogState = rememberDialogState(),
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
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    modalityType: ModalityType =
        if (parentWindow == null) ModalityType.APPLICATION_MODAL else ModalityType.DOCUMENT_MODAL,
    init: (ComposeDialog) -> Unit,
    content: @Composable DialogWindowScope.() -> Unit
) {
    if ((parentWindow == null) && (modalityType == ModalityType.DOCUMENT_MODAL)) {
        throw IllegalArgumentException("SwingDialog with no parent window cannot be DOCUMENT_MODAL")
    }

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
    val currentModalityType by rememberUpdatedState(modalityType)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    val listeners = remember {
        object {
            var windowListenerRef = windowListenerRef()
            var componentListenerRef = componentListenerRef()

            fun removeFromAndClear(window: ComposeDialog) {
                windowListenerRef.unregisterFromAndClear(window)
                componentListenerRef.unregisterFromAndClear(window)
            }
        }
    }

    val coroutineContext = rememberCoroutineScope().coroutineContext

    var dialog: ComposeDialog? by remember { mutableStateOf(null) }
    SwingDialog(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            val graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            val currentDevice = currentState._screenId?.let { screenId ->
                graphicsDevices.firstOrNull { it.iDstring == screenId }
            }
            val parentDevice = parentWindow?.graphicsConfiguration?.device
            val initialDevice = currentDevice
                ?: state.screenRequests.tryReceive().getOrNull()?.getInitialScreenDevice(parentDevice)
                ?: WindowScreenProvider.Default.getInitialScreenDevice(parentDevice)
            val graphicsConfiguration = initialDevice.defaultConfiguration

            val dlg = if (parentWindow != null) {
                ComposeDialog(
                    owner = parentWindow,
                    modalityType = currentModalityType,
                    graphicsConfiguration = graphicsConfiguration,
                    coroutineContext = coroutineContext
                )
            } else {
                ComposeDialog(
                    graphicsConfiguration = graphicsConfiguration,
                    coroutineContext = coroutineContext
                )
            }

            // close state is controlled by DialogState.isOpen
            dlg.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
            listeners.windowListenerRef.registerWithAndSet(
                dlg,
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        currentOnCloseRequest()
                    }
                }
            )

            listeners.componentListenerRef.registerWithAndSet(
                dlg,
                object : ComponentAdapter() {
                    fun applyBoundsChanges() {
                        currentState._bounds = dlg.bounds.toDpRect()
                        if (currentState._screenId != dlg.graphicsConfiguration.device.iDstring) {
                            currentState._screenId = dlg.graphicsConfiguration.device.iDstring
                        }
                    }

                    override fun componentShown(e: ComponentEvent) {
                        // Initialize all state properties
                        applyBoundsChanges()
                        currentState.isInitialized = true
                    }

                    override fun componentResized(e: ComponentEvent) {
                        applyBoundsChanges()
                    }

                    override fun componentMoved(e: ComponentEvent) {
                        applyBoundsChanges()
                    }
                }
            )

            init(dlg)
            dialog = dlg

            dlg
        },
        dispose = {
            // We need to remove them because AWT can still call them after dispose()
            listeners.removeFromAndClear(it)
            it.dispose()
        },
        update = { dialog ->
            updater.update {
                set(currentTitle, dialog::setTitle)
                set(currentIcon, dialog::setIcon)
                set(currentDecoration is UndecoratedWindowDecoration, dialog::setUndecoratedSafely)
                set(currentTransparent, dialog::isTransparent::set)
                set(currentResizable, dialog::setResizable)
                set(currentEnabled, dialog::setEnabled)
                set(currentFocusable, dialog::setFocusableWindowState)
                set(currentAlwaysOnTop, dialog::setAlwaysOnTop)
                set(currentMinSize) { dialog.minimumSize = it.roundToDimensionOrNull() }
                set(currentMaxSize) { dialog.maximumSize = it.roundToDimensionOrNull() }
                set(currentModalityType, dialog::setModalityType)
                set(currentDecoration.resizerThickness, dialog::undecoratedResizerThickness::set)
            }

            if (!dialog.isDisplayable) {
                dialog.initializeBounds(currentState)

                // Need to make the dialog displayable, to make awt.SwingDialog render the first
                // frame before the dialog is visible.
                // Check isDisplayable again because initializeBounds could have already
                // called pack(), and we don't need to do it twice
                if (!dialog.isDisplayable) {
                    dialog.preferredSize = dialog.size
                    dialog.pack()  // Sizes to preferred size
                }
            }
        },
        content = content
    )

    LaunchedEffect(dialog, state) {
        val dialog = dialog ?: return@LaunchedEffect
        launch {
            while (isActive) {
                dialog.setScreenFrom(state.screenRequests.receive())
            }
        }
        launch {
            while (isActive) {
                dialog.setBoundsFrom(state.boundsRequests.receive())
            }
        }
    }
}

private fun ComposeDialog.initializeBounds(state: DialogState) {
    initializeBounds(state.boundsRequests, state._bounds, owner, ::measureContent)
}

private fun ComposeDialog.setBoundsFrom(boundsProvider: WindowBoundsProvider) {
    setBoundsFrom(boundsProvider, owner, ::measureContent)
}