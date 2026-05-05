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

package androidx.compose.ui.window.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.toAwtModalityType
import androidx.compose.ui.awt.v2.SwingDialog
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowDecoration
import java.awt.Window

/**
 * Composes platform dialog in the current composition. When [DialogWindow] enters the composition,
 * a new platform dialog will be created and receive focus. When [DialogWindow] leaves the
 * composition, the dialog will be disposed and closed.
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [DialogWindow] in whose
 * composition context it was created.
 *
 * Usage:
 * ```
 * @Composable
 * fun main() = application {
 *     var isDialogOpen by remember { mutableStateOf(true) }
 *     if (isDialogOpen) {
 *         DialogWindow(onCloseRequest = { isDialogOpen = false }) {}
 *     }
 * }
 * ```
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param onCloseRequest Callback that will be called when the user closes the dialog.
 *   Usually in this callback we need to manually tell Compose what to do:
 *   - Change `isOpen` state of the dialog (which is manually defined)
 *   - Close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 *   - Don't close the dialog on close request (`onCloseRequest = {}`)
 * @param state The state object to control and observe the dialog's state.
 * @param visible Whether the dialog is visible to the user.
 *   When `false`:
 *   - The internal state of the [DialogWindow] is preserved and will be restored the next time the
 *     dialog will be made visible;
 *   - Native resources will not be released. They will be released only when [DialogWindow] leaves
 *     the composition.
 * @param title The title of the dialog.
 * @param icon The icon of the window (for platforms that support this).
 *   On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 *   set it via `iconFile` in build.gradle or via an `-Xdock:icon=...` parameter to the process
 *   (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param decoration Specifies the decoration for this dialog.
 * @param transparent Controls dialog transparency. Only an undecorated dialog may be transparent.
 *   Attempting to make a decorated dialog transparent will throw an exception.
 * @param resizable Whether the user can resize the dialog (application can resize the dialog by
 *   changing [state] regardless of this parameter).
 * @param enabled Whether the dialog reacts to input events.
 * @param focusable Whether the dialog can receive focus.
 * @param alwaysOnTop whether the dialog will always be on top of other windows and dialogs in the
 * application.
 * @param minSize The minimum dialog size. This will prevent the user from resizing the dialog
 *   to smaller than the specified value. A value of [DpSize.Unspecified] means no minimum.
 *   Note that some window managers may not respect this.
 * @param maxSize The maximum dialog size. This will prevent the user from resizing the dialog
 *   to larger than the specified value. A value of [DpSize.Unspecified] means no maximum.
 *   Note that some window managers may not respect this.
 * @param modalityType Modality type for the dialog. A top-level dialog cannot be
 *    [DialogModalityType.DocumentModal]
 * @param onPreviewKeyEvent Invoked when the dialog receives a key event, before it is sent to the
 *   [content]. The return value controls whether the key event will be sent to the [content]
 *   afterward. Return `true` to consume it, preventing further processing.
 * @param onKeyEvent Invoked when the dialog receives a key event, after it has been sent to
 *   [content], only if nothing there had consumed it. The return value controls whether the key
 *   event will be processed further (e.g., by the system). Return `true` to consume it, preventing
 *   further processing.
 * @param content Composable content of the dialog.
 */
@ExperimentalComposeUiApi
@Composable
@ComposableOpenTarget(-1)
fun DialogWindow(
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
    minSize: DpSize = DpSize.Unspecified,
    maxSize: DpSize = DpSize.Unspecified,
    modalityType: DialogModalityType = defaultDialogModality(),
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) {
    SwingDialog(
        onCloseRequest = onCloseRequest,
        parentWindow = LocalAwtWindow.current,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = decoration,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        minSize = minSize,
        maxSize = maxSize,
        modalityType = modalityType.toAwtModalityType(),
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        init = { },
        content = content,
    )
}

@Composable
private fun defaultDialogModality() =
    if (LocalAwtWindow.current == null) {
        DialogModalityType.ApplicationModal
    } else {
        DialogModalityType.DocumentModal
    }
