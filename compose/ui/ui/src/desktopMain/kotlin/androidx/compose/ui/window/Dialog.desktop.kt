/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.makeDisplayable
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setPositionSafely
import androidx.compose.ui.util.setSizeSafely
import androidx.compose.ui.util.setUndecoratedSafely
import java.awt.Dialog.ModalityType
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
 *  * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 * dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 * the platform default, which is smaller than the screen width.
 */
@Immutable
actual class DialogProperties actual constructor(
    actual val dismissOnBackPress: Boolean,
    actual val dismissOnClickOutside: Boolean,
    actual val usePlatformDefaultWidth: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        return result
    }
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember {
        AlignmentOffsetPositionProvider(
            alignment = Alignment.Center,
            offset = IntOffset(0, 0)
        )
    }
    PopupLayout(
        popupPositionProvider = popupPositioner,
        focusable = true,
        if (properties.dismissOnClickOutside) onDismissRequest else null,
        modifier = Modifier.drawBehind {
            drawRect(Color.Black.copy(alpha = 0.6f))
        },
        onKeyEvent = {
            if (properties.dismissOnBackPress &&
                it.type == KeyEventType.KeyDown && it.key == Key.Escape
            ) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
        content = content
    )
}

@Deprecated(
    message = "Replaced by DialogWindow",
    replaceWith = ReplaceWith("DialogWindow(" +
        "onCloseRequest, state, visible, title, icon, undecorated, transparent, resizable, " +
        "enabled, focusable, onPreviewKeyEvent, onKeyEvent, content" +
        ")")
)
@Composable
fun Dialog(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) = DialogWindow(
    onCloseRequest,
    state,
    visible,
    title,
    icon,
    undecorated,
    transparent,
    resizable,
    enabled,
    focusable,
    onPreviewKeyEvent,
    onKeyEvent,
    content
)

/**
 * Composes platform dialog in the current composition. When Dialog enters the composition,
 * a new platform dialog will be created and receives the focus. When Dialog leaves the
 * composition, dialog will be disposed and closed.
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [Dialog] in which composition
 * context it was created.
 *
 * Usage:
 * ```
 * @Composable
 * fun main() = application {
 *     val isDialogOpen by remember { mutableStateOf(true) }
 *     if (isDialogOpen) {
 *         Dialog(onCloseRequest = { isDialogOpen = false })
 *     }
 * }
 * ```
 * @param onCloseRequest Callback that will be called when the user closes the dialog.
 * Usually in this callback we need to manually tell Compose what to do:
 * - change `isOpen` state of the dialog (which is manually defined)
 * - close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 * - don't close the dialog on close request (`onCloseRequest = {}`)
 * @param state The state object to be used to control or observe the dialog's state
 * When size/position is changed by the user, state will be updated.
 * When size/position of the dialog is changed by the application (changing state),
 * the native dialog will update its corresponding properties.
 * If [DialogState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [DialogState.position] will be set to the absolute values.
 * @param visible Is [Dialog] visible to user.
 * If `false`:
 * - internal state of [Dialog] is preserved and will be restored next time the dialog
 * will be visible;
 * - native resources will not be released. They will be released only when [Dialog]
 * will leave the composition.
 * @param title Title in the titlebar of the dialog
 * @param icon Icon in the titlebar of the window (for platforms which support this).
 * On macOs individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency should be set
 * only if window is undecorated, otherwise an exception will be thrown.
 * @param resizable Can dialog be resized by the user (application still can resize the dialog
 * changing [state])
 * @param enabled Can dialog react to input events
 * @param focusable Can dialog receive focus
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content content of the dialog
 */
@Composable
fun DialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) {
    val owner = LocalWindow.current

    val currentState by rememberUpdatedState(state)
    val currentTitle by rememberUpdatedState(title)
    val currentIcon by rememberUpdatedState(icon)
    val currentUndecorated by rememberUpdatedState(undecorated)
    val currentTransparent by rememberUpdatedState(transparent)
    val currentResizable by rememberUpdatedState(resizable)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentFocusable by rememberUpdatedState(focusable)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    DialogWindow(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            ComposeDialog(owner, ModalityType.DOCUMENT_MODAL).apply {
                // close state is controlled by DialogState.isOpen
                defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        currentOnCloseRequest()
                    }
                })
                addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        currentState.size = DpSize(width.dp, height.dp)
                    }

                    override fun componentMoved(e: ComponentEvent) {
                        currentState.position = WindowPosition(x.dp, y.dp)
                    }
                })
            }
        },
        dispose = ComposeDialog::dispose,
        update = { dialog ->
            updater.update {
                set(currentTitle, dialog::setTitle)
                set(currentIcon, dialog::setIcon)
                set(currentUndecorated, dialog::setUndecoratedSafely)
                set(currentTransparent, dialog::isTransparent::set)
                set(currentResizable, dialog::setResizable)
                set(currentEnabled, dialog::setEnabled)
                set(currentFocusable, dialog::setFocusable)
                set(state.size, dialog::setSizeSafely)
                set(state.position, dialog::setPositionSafely)
            }
        },
        content = content
    )
}

@Deprecated(
    message = "Replaced by DialogWindow",
    replaceWith = ReplaceWith("DialogWindow(" +
        "visible, onPreviewKeyEvent, onKeyEvent, create, dispose, update, contents" +
        ")")
)
@Composable
fun Dialog(
    visible: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    create: () -> ComposeDialog,
    dispose: (ComposeDialog) -> Unit,
    update: (ComposeDialog) -> Unit = {},
    content: @Composable DialogWindowScope.() -> Unit
) = DialogWindow(
    visible,
    onPreviewKeyEvent,
    onKeyEvent,
    create,
    dispose,
    update,
    content
)

// TODO(demin): fix mouse hover after opening a dialog.
//  When we open a modal dialog, ComposeLayer/mouseExited will
//  never be called for the parent window. See ./gradlew run3
/**
 * Compose [ComposeDialog] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeDialog] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once Dialog leaves the composition, [dispose] will be called to free resources that
 * obtained by the [ComposeDialog].
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [Dialog] in which composition
 * context it was created.
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeDialog] properties depending on state.
 * When state changes, the block will be reexecuted to set the new properties.
 * Note the block will also be ran once right after the [create] block completes.
 *
 * Dialog is needed for creating dialog's that still can't be created with
 * the default Compose function [androidx.compose.ui.window.Dialog]
 *
 * @param visible Is [ComposeDialog] visible to user.
 * If `false`:
 * - internal state of [ComposeDialog] is preserved and will be restored next time the dialog
 * will be visible;
 * - native resources will not be released. They will be released only when [Dialog]
 * will leave the composition.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param create The block creating the [ComposeDialog] to be composed.
 * @param dispose The block to dispose [ComposeDialog] and free native resources.
 * Usually it is simple `ComposeDialog::dispose`
 * @param update The callback to be invoked after the layout is inflated.
 * @param content Composable content of the creating dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("unused")
@Composable
fun DialogWindow(
    visible: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    create: () -> ComposeDialog,
    dispose: (ComposeDialog) -> Unit,
    update: (ComposeDialog) -> Unit = {},
    content: @Composable DialogWindowScope.() -> Unit
) {
    val currentLocals by rememberUpdatedState(currentCompositionLocalContext)
    AwtWindow(
        visible = visible,
        create = {
            create().apply {
                setContent(onPreviewKeyEvent, onKeyEvent) {
                    CompositionLocalProvider(currentLocals) {
                        content()
                    }
                }
            }
        },
        dispose = dispose,
        update = {
            update(it)

            if (!it.isDisplayable) {
                it.makeDisplayable()
                it.contentPane.paint(it.graphics)
            }
        }
    )
}

/**
 * Receiver scope which is used by [androidx.compose.ui.window.Dialog].
 */
@Stable
interface DialogWindowScope : WindowScope {
    /**
     * [ComposeDialog] that was created inside [androidx.compose.ui.window.Dialog].
     */
    override val window: ComposeDialog
}
