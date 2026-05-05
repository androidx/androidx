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
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import java.awt.Window
import javax.swing.JMenuBar

// TODO(demin): support focus management
//   https://youtrack.jetbrains.com/issue/CMP-10092/Window-API.-Support-focus-management
/**
 * Composes platform window in the current composition. When [Window] enters the composition,
 * a new platform window will be created and receive focus. When [Window] leaves the composition,
 * the window will be disposed and closed.
 *
 * Initial size of the window is controlled by [WindowState.size].
 * Initial position of the window is controlled by [WindowState.position].
 *
 * Usage in single-window application ([ApplicationScope.exitApplication] will close all the
 * windows and stop all effects defined in [application]):
 * ```
 * fun main() = application {
 *     Window(onCloseRequest = ::exitApplication) {}
 * }
 * ```
 *
 * or if it only needed to close the main window without closing all other opened windows:
 * ```
 * fun main() = application {
 *     var isOpen by remember { mutableStateOf(true) }
 *     if (isOpen) {
 *         Window(onCloseRequest = { isOpen = false }) {}
 *     }
 * }
 * ```
 *
 * @param onCloseRequest Callback that will be called when the user closes the window.
 * Usually in this callback we need to manually tell Compose what to do:
 * - change `isOpen` state of the window (which is manually defined)
 * - close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 * - don't close the window on close request (`onCloseRequest = {}`)
 * @param state The state object to be used to control or observe the window's state
 * When size/position/status is changed by the user, state will be updated.
 * When size/position/status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Whether the window is visible to the user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the title bar of the window
 * @param icon Icon in the title bar of the window (for platforms that support this).
 * On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param decoration Specifies the decoration for this window.
 * @param transparent Disables or enables window transparency. Transparency may be set only if the
 * window is undecorated, otherwise an exception will be thrown.
 * @param resizable Whether the window can be resized by the user (application still can resize the
 * window by changing [state]).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 * application.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content Composable content of the window.
 */
@ExperimentalComposeUiApi
@Composable
@ComposableOpenTarget(-1)
fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit
) {
    SwingWindow(
        onCloseRequest = onCloseRequest,
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
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        init = { },
        content = content,
    )
}

/**
 * Composes platform window in the current composition. When Window enters the composition,
 * a new platform window will be created and receives the focus. When Window leaves the
 * composition, window will be disposed and closed.
 *
 * Initial size of the window is controlled by [WindowState.size].
 * Initial position of the window is controlled by [WindowState.position].
 *
 * Usage in single-window application ([ApplicationScope.exitApplication] will close all the
 * windows and stop all effects defined in [application]):
 * ```
 * fun main() = application {
 *     Window(onCloseRequest = ::exitApplication) {}
 * }
 * ```
 *
 * or if it only needed to close the main window without closing all other opened windows:
 * ```
 * fun main() = application {
 *     var isOpen by remember { mutableStateOf(true) }
 *     if (isOpen) {
 *         Window(onCloseRequest = { isOpen = false }) {}
 *     }
 * }
 * ```
 *
 * @param onCloseRequest Callback that will be called when the user closes the window.
 * Usually in this callback we need to manually tell Compose what to do:
 * - change `isOpen` state of the window (which is manually defined)
 * - close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 * - don't close the window on close request (`onCloseRequest = {}`)
 * @param state The state object to be used to control or observe the window's state
 * When size/position/status is changed by the user, state will be updated.
 * When size/position/status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Whether the window is visible to the user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the title bar of the window
 * @param icon Icon in the title bar of the window (for platforms that support this).
 * On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency may be set only if the
 * window is undecorated, otherwise an exception will be thrown.
 * @param resizable Whether the window can be resized by the user (application still can resize the
 * window by changing [state]).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 * application.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content Composable content of the window.
 */
@Composable
@ComposableOpenTarget(-1)
fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = windowDecorationFromFlag(undecorated),
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content,
    )
}

/**
 * An entry point for Compose applications that only need a single top-level window.
 *
 * If you need to change attributes of the window in runtime, or need a custom closing logic, use
 * Composable [androidx.compose.ui.window.Window] in [application] entry point instead:
 * ```
 * application {
 *     Window(...) { }
 * }
 * ```
 *
 * Set [exitProcessOnExit] to `false` if you need to execute code after the
 * [singleWindowApplication] block, otherwise it won't be executed as [singleWindowApplication] will
 * exit the process.
 *
 * @param state The state object to be used to control or observe the window's state
 * When the size / position / status is changed by the user, the state will be updated.
 * When the size / position / status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Whether the window is visible to the user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the title bar of the window
 * @param icon Icon in the title bar of the window (for platforms that support this).
 * On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param decoration Specifies the decoration for this window.
 * @param transparent Disables or enables window transparency. Transparency may be set only if the
 * window is undecorated, otherwise an exception will be thrown.
 * @param resizable Whether the window can be resized by the user (application still can resize the
 * window by changing [state]).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 * application.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param exitProcessOnExit Whether `exitProcess(0)` will be called after the window is closed.
 * `exitProcess` speeds up process exit (instant instead of 1-4sec).
 * If `false`, the execution of the function will be unblocked after application is exited
 * (when the last window is closed, and all [LaunchedEffect]s are complete).
 * @param content Composable content of the window.
 */
@ExperimentalComposeUiApi
@JvmName("singleWindowApplicationWithAppScope")
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    exitProcessOnExit: Boolean = true,
    content: @Composable SingleWindowApplicationScope.() -> Unit
) = application(exitProcessOnExit = exitProcessOnExit) {
    Window(
        onCloseRequest = ::exitApplication,
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
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = {
            with(SingleWindowApplicationScope(this@application, this@Window)) {
                content()
            }
        }
    )
}

@ExperimentalComposeUiApi
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced by override that takes a `SingleWindowApplicationScope`"
)
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    exitProcessOnExit: Boolean = true,
    content: @Composable FrameWindowScope.() -> Unit
) = application(exitProcessOnExit = exitProcessOnExit) {
    Window(
        onCloseRequest = ::exitApplication,
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
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content
    )
}

/**
 * An entry point for Compose applications that only need a single top-level window.
 *
 * If you need to change attributes of the window in runtime, or need a custom closing logic, use
 * Composable `Window` in [application] entry point instead:
 * ```
 * application {
 *     Window(...) { }
 * }
 * ```
 *
 * Set [exitProcessOnExit] to `false` if you need to execute code after the
 * [singleWindowApplication] block, otherwise it won't be executed as [singleWindowApplication] will
 * exit the process.
 *
 * @param state The state object to be used to control or observe the window's state
 * When the size / position / status is changed by the user, the state will be updated.
 * When the size / position / status of the window is changed by the application (changing state),
 * the native window will update its corresponding properties.
 * If application changes, for example [WindowState.placement], then after the next
 * recomposition, [WindowState.size] will be changed to correspond the real size of the window.
 * If [WindowState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [WindowState.position] will be set to the absolute values.
 * @param visible Whether the window is visible to the user.
 * If `false`:
 * - internal state of [Window] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param title Title in the title bar of the window
 * @param icon Icon in the title bar of the window (for platforms that support this).
 * On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency may be set only if the
 * window is undecorated, otherwise an exception will be thrown.
 * @param resizable Whether the window can be resized by the user (application still can resize the
 * window by changing [state]).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 * application.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param exitProcessOnExit Whether `exitProcess(0)` will be called after the window is closed.
 * `exitProcess` speeds up process exit (instant instead of 1-4sec).
 * If `false`, the execution of the function will be unblocked after application is exited
 * (when the last window is closed, and all [LaunchedEffect]s are complete).
 * @param content Composable content of the window.
 */
@JvmName("singleWindowApplicationWithAppScope")
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    exitProcessOnExit: Boolean = true,
    content: @Composable SingleWindowApplicationScope.() -> Unit
) {
    singleWindowApplication(
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = windowDecorationFromFlag(undecorated),
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        exitProcessOnExit = exitProcessOnExit,
        content = content,
    )
}

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced by override that takes a `SingleWindowApplicationScope`"
)
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    exitProcessOnExit: Boolean = true,
    content: @Composable FrameWindowScope.() -> Unit
) = application(exitProcessOnExit = exitProcessOnExit) {
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = windowDecorationFromFlag(undecorated),
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content
    )
}

/**
 * Compose [ComposeWindow] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeWindow] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once Window leaves the composition, [dispose] will be called to free resources that
 * obtained by the [ComposeWindow].
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeWindow] properties depending on state.
 * When state changes, the block will be re-executed to set the new properties.
 * Note the block will also be run once right after the [create] block completes.
 *
 * Window is needed for creating window's that still can't be created with
 * the default Compose function [androidx.compose.ui.window.Window]
 *
 * @param visible Whether the window is visible to the user.
 * If `false`:
 * - internal state of [ComposeWindow] is preserved and will be restored next time the window
 * will be visible;
 * - native resources will not be released. They will be released only when [Window]
 * will leave the composition.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the [onKeyEvent] callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param create The block creating the [ComposeWindow] to be composed.
 * @param dispose The block to dispose [ComposeWindow] and free native resources.
 * Usually it is simple `ComposeWindow::dispose`
 * @param update The callback to be invoked to update dialog properties.
 * @param content Composable content of the window.
 */
@Suppress("unused")
@Deprecated(
    message = "Renamed to SwingWindow",
    replaceWith = ReplaceWith(
        "SwingWindow(visible, onPreviewKeyEvent, onKeyEvent, create, dispose, update, content)",
        "androidx.compose.ui.awt.SwingWindow"
    )
)
@Composable
@ComposableOpenTarget(-1)
fun Window(
    visible: Boolean = true,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    create: () -> ComposeWindow,
    dispose: (ComposeWindow) -> Unit,
    update: (ComposeWindow) -> Unit = {},
    content: @Composable FrameWindowScope.() -> Unit
) {
    SwingWindow(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = create,
        dispose = dispose,
        update = update,
        content = content
    )
}

/**
 * Receiver scope which is used by [androidx.compose.ui.window.Window].
 */
@Stable
interface FrameWindowScope : WindowScope {
    /**
     * [ComposeWindow] that was created inside [androidx.compose.ui.window.Window].
     */
    override val window: ComposeWindow
}

/**
 * Receiver scope for [singleWindowApplication].
 */
@Stable
interface SingleWindowApplicationScope: ApplicationScope, FrameWindowScope

@Composable
internal fun SingleWindowApplicationScope(
    applicationScope: ApplicationScope,
    windowScope: FrameWindowScope
): SingleWindowApplicationScope {
    return remember(applicationScope, windowScope) {
        object : SingleWindowApplicationScope,
            ApplicationScope by applicationScope,
            FrameWindowScope by windowScope {}
    }
}

/**
 * Composes menu bar on the top of the window
 *
 * @param content content of the menu bar (list of menus)
 */
@Composable
@ComposableOpenTarget(-1)
fun FrameWindowScope.MenuBar(content: @Composable @MenuComposable MenuBarScope.() -> Unit) {
    val parentComposition = rememberCompositionContext()

    DisposableEffect(Unit) {
        val menu = JMenuBar()
        val composition = menu.setContent(parentComposition, content)
        window.jMenuBar = menu

        onDispose {
            window.jMenuBar = null
            composition.dispose()
        }
    }
}
