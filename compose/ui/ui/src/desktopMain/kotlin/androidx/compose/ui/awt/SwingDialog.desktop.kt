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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.scene.LocalComposeSceneContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.componentListenerRef
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setPositionSafely
import androidx.compose.ui.util.setSizeSafely
import androidx.compose.ui.util.setUndecoratedSafely
import androidx.compose.ui.util.windowListenerRef
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.UndecoratedWindowDecoration
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowLocationTracker
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.componentOrientation
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.resizerThickness
import java.awt.Dialog.ModalityType
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog

// TODO(demin): fix mouse hover after opening a dialog.
//  When we open a modal dialog, ComposeLayer/mouseExited will
//  never be called for the parent window. See ./gradlew run3
/**
 * Compose [ComposeDialog] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeDialog] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once [SwingDialog] leaves the composition, [dispose] will be called to free resources that were
 * obtained by the [ComposeDialog].
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [DialogWindow] in whose
 * composition context it was created.
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeDialog] properties depending on state.
 * When state changes, the block will be reexecuted to set the new properties.
 * Note the block will also be ran once right after the [create] block completes.
 *
 * [SwingDialog] is needed for creating dialogs that can't be created with the default Compose
 * function [DialogWindow]
 *
 * @param visible Whether the dialog is visible to the user.
 * If `false`:
 * - internal state of [ComposeDialog] is preserved and will be restored next time the dialog
 * will be visible;
 * - native resources will not be released. They will be released only when [SwingDialog] leaves
 * the composition.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param create The block creating the [ComposeDialog] to be composed.
 * @param dispose The block to dispose [ComposeDialog] and free native resources.
 * Usually it is simple `ComposeDialog::dispose`
 * @param update The callback to be invoked to update dialog properties.
 * @param content Composable content of the dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ComposableOpenTarget(-1)
fun SwingDialog(
    visible: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    create: () -> ComposeDialog,
    dispose: (ComposeDialog) -> Unit,
    update: (ComposeDialog) -> Unit = {},
    content: @Composable DialogWindowScope.() -> Unit
) {
    val compositionLocalContext by rememberUpdatedState(currentCompositionLocalContext)
    val windowExceptionHandlerFactory by rememberUpdatedState(
        LocalWindowExceptionHandlerFactory.current
    )
    val parentPlatformContext = LocalComposeSceneContext.current?.platformContext
    val layoutDirection = LocalLayoutDirection.current
    AwtWindow(
        visible = visible,
        create = {
            create().apply {
                this.rootForTestListener = parentPlatformContext?.rootForTestListener
                this.compositionLocalContext = compositionLocalContext
                this.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(this)
                setContent(onPreviewKeyEvent, onKeyEvent, content)
            }
        },
        dispose = {
            dispose(it)
        },
        update = {
            it.compositionLocalContext = compositionLocalContext
            it.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(it)
            it.componentOrientation = layoutDirection.componentOrientation

            val wasDisplayable = it.isDisplayable

            update(it)

            // If displaying for the first time, make sure we draw the first frame before making
            // the dialog visible to avoid showing the dialog background.
            // It's the responsibility of setSizeSafely to
            // - Make the dialog displayable
            // - Size the dialog and the ComposeLayer correctly, so that we can draw it here
            if (!wasDisplayable && it.isDisplayable) {
                Snapshot.withoutReadObservation {
                    if (!it.isValid) {
                        it.validate()
                    }
                    it.renderImmediately()
                }
            }
        },
    )
}

/**
 * Similar to the corresponding [DialogWindow] function, but additionally allows configuring the
 * underlying AWT dialog before it has been made displayable, by providing an [init] block.
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
 * @see DialogWindow
 */
@ExperimentalComposeUiApi
@Composable
@ComposableOpenTarget(-1)
fun SwingDialog(
    onCloseRequest: () -> Unit,
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
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    modalityType: ModalityType = ModalityType.DOCUMENT_MODAL,
    init: (ComposeDialog) -> Unit,
    content: @Composable DialogWindowScope.() -> Unit
) {
    val owner = LocalAwtWindow.current

    val currentState by rememberUpdatedState(state)
    val currentTitle by rememberUpdatedState(title)
    val currentIcon by rememberUpdatedState(icon)
    val currentDecoration by rememberUpdatedState(decoration)
    val currentTransparent by rememberUpdatedState(transparent)
    val currentResizable by rememberUpdatedState(resizable)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentFocusable by rememberUpdatedState(focusable)
    val currentAlwaysOnTop by rememberUpdatedState(alwaysOnTop)
    val currentModalityType by rememberUpdatedState(modalityType)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    // the state applied to the dialog. exist to avoid races between DialogState changes and the state stored inside the native dialog
    val appliedState = remember {
        object {
            var size: DpSize? = null
            var position: WindowPosition? = null
        }
    }

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

    SwingDialog(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            val graphicsConfiguration = WindowLocationTracker.lastActiveGraphicsConfiguration
            val dialog = if (owner != null) {
                ComposeDialog(
                    owner = owner,
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
            dialog.apply {
                // close state is controlled by DialogState.isOpen
                defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                listeners.windowListenerRef.registerWithAndSet(
                    this,
                    object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            currentOnCloseRequest()
                        }
                    }
                )
                listeners.componentListenerRef.registerWithAndSet(
                    this,
                    object : ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent) {
                            currentState.size = DpSize(width.dp, height.dp)
                            appliedState.size = currentState.size
                        }

                        override fun componentMoved(e: ComponentEvent) {
                            currentState.position = WindowPosition(x.dp, y.dp)
                            appliedState.position = currentState.position
                        }
                    }
                )
                WindowLocationTracker.onWindowCreated(this)

                init(dialog)
            }
        },
        dispose = {
            WindowLocationTracker.onWindowDisposed(it)
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
                set(currentModalityType, dialog::setModalityType)
                set(currentDecoration.resizerThickness, dialog::undecoratedResizerThickness::set)
            }
            if (state.size != appliedState.size) {
                dialog.setSizeSafely(state.size, WindowPlacement.Floating)
                appliedState.size = state.size
            }
            if (state.position != appliedState.position) {
                dialog.setPositionSafely(
                    state.position,
                    WindowPlacement.Floating,
                    platformDefaultPosition = { WindowLocationTracker.getCascadeLocationFor(dialog) }
                )
                appliedState.position = state.position
            }
        },
        content = content
    )
}

/**
 * Returns the AWT [java.awt.Dialog.ModalityType] corresponding to the given Compose
 * [DialogModalityType].
 */
internal fun DialogModalityType.toAwtModalityType(): ModalityType = when (this) {
    DialogModalityType.Modeless -> ModalityType.MODELESS
    DialogModalityType.DocumentModal -> ModalityType.DOCUMENT_MODAL
    DialogModalityType.ApplicationModal -> ModalityType.APPLICATION_MODAL
    else -> error("Unknown dialog modality type: $this")
}
