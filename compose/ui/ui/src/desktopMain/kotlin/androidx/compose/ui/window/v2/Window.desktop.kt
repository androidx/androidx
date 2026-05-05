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

package androidx.compose.ui.window.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.v2.SwingWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.SingleWindowApplicationScope
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.application

// TODO(demin): support focus management
//   https://youtrack.jetbrains.com/issue/CMP-10092/Window-API.-Support-focus-management
/**
 * Composes a platform window in the current composition. When [Window] enters the composition,
 * a new platform window will be created and receive focus. When [Window] leaves the composition,
 * the window will be disposed and closed.
 *
 * The placement and positioning of the window is controlled via [WindowState].
 *
 * [onCloseRequest] is called when the user asks to close the window. To close all windows and shut
 * down the application, use ([ApplicationScope.exitApplication]:
 * ```
 * fun main() = application {
 *     Window(onCloseRequest = ::exitApplication) { ... }
 * }
 * ```
 *
 * To merely close the window, use:
 * ```
 * fun main() = application {
 *     var isOpen by remember { mutableStateOf(true) }
 *     if (isOpen) {
 *         Window(onCloseRequest = { isOpen = false }) { ... }
 *     }
 * }
 * ```
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param onCloseRequest Callback that will be called when the user tries to close the window.
 * @param state The state object to control and observe the window's state.
 * @param visible Whether the window is visible to the user.
 *   When `false`:
 *   - The internal state of the [Window] is preserved and will be restored the next time the window
 *     will be made visible;
 *   - Native resources will not be released. They will be released only when [Window] leaves the
 *     composition.
 * @param title The title of the window.
 * @param icon The icon of the window (for platforms that support this).
 *   On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 *   set it via `iconFile` in build.gradle or via an `-Xdock:icon=...` parameter to the process
 *   (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param decoration Specifies the decoration for this window.
 * @param transparent Controls window transparency. Only an undecorated window may be transparent.
 *   Attempting to make a decorated window transparent will throw an exception.
 * @param resizable Whether the user can resize the window (application can resize the window by
 *   changing [state] regardless of this parameter).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 *   application.
 * @param minSize The minimum window size. This will prevent the user from resizing the window
 *   to smaller than the specified value. A value of [DpSize.Unspecified] means no minimum.
 *   Note that some window managers may not respect this.
 * @param maxSize The maximum window size. This will prevent the user from resizing the window
 *   to larger than the specified value. A value of [DpSize.Unspecified] means no maximum.
 *   Note that some window managers may not respect this.
 * @param onPreviewKeyEvent Invoked when the window receives a key event, before it is sent to the
 *   [content]. The return value controls whether the key event will be sent to the [content]
 *   afterward. Return `true` to consume it, preventing further processing.
 * @param onKeyEvent Invoked when the window receives a key event, after it has been sent to
 *   [content], only if nothing there had consumed it. The return value controls whether the key
 *   event will be processed further (e.g., by the system). Return `true` to consume it, preventing
 *   further processing.
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
        minSize = minSize,
        maxSize = maxSize,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        init = { },
        content = content,
    )
}

/**
 * An entry point for Compose applications with a single top-level window.
 *
 * To show more than one top-level window, or to implement custom closing logic, use
 * Composable [androidx.compose.ui.window.v2.Window] in [application] entry point instead:
 * ```
 * application {
 *     Window(...) { }
 *     Window(onCloseRequest = { ... } ) { }
 * }
 * ```
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * Set [exitProcessOnExit] to `false` to execute code after the [singleWindowApplication] block,
 * otherwise it won't be executed as [singleWindowApplication] will exit the process.
 *
 * @param state The state object to be used to control or observe the window's state
 * @param visible Whether the window is visible to the user.
 * When `false`:
 * - The internal state of the [Window] is preserved and will be restored the next time the window
 *   will be made visible;
 * - Native resources will not be released. They will be released only when [Window] leaves the
 *   composition.
 * @param title The title of the window.
 * @param icon The icon of the window (for platforms that support this).
 *   On macOS individual windows can't have a separate icon. To change the icon in the Dock,
 *   set it via `iconFile` in build.gradle or via an `-Xdock:icon=...` parameter to the process
 *   (https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html#platform-specific-options)
 * @param decoration Specifies the decoration for this window.
 * @param transparent Controls window transparency. Only an undecorated window may be transparent.
 *   Attempting to make a decorated window transparent will throw an exception.
 * @param resizable Whether the user can resize the window (application can resize the window by
 *   changing [state] regardless of this parameter).
 * @param enabled Whether the window reacts to input events.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop whether the window will always be on top of other windows and dialogs in the
 *   application.
 * @param minSize The minimum window size. This will prevent the user from resizing the window
 *   to smaller than the specified value. A value of [DpSize.Unspecified] means no minimum.
 *   Note that some window managers may not respect this.
 * @param maxSize The maximum window size. This will prevent the user from resizing the window
 *   to larger than the specified value. A value of [DpSize.Unspecified] means no maximum.
 *   Note that some window managers may not respect this.
 * @param onPreviewKeyEvent Invoked when the window receives a key event, before it is sent to the
 *   [content]. The return value controls whether the key event will be sent to the [content]
 *   afterward. Return `true` to consume it, preventing further processing.
 * @param onKeyEvent Invoked when the window receives a key event, after it has been sent to
 *   [content], only if nothing there had consumed it. The return value controls whether the key
 *   event will be processed further (e.g., by the system). Return `true` to consume it, preventing
 *   further processing.
 * @param exitProcessOnExit Whether `exitProcess(0)` will be called after the window is closed.
 *   `exitProcess` speeds up process exit (instant instead of 1-4sec).
 *   If `false`, the execution of the function will be unblocked after application is exited
 *   (when the last window is closed, and all [LaunchedEffect]s are complete).
 * @param content Composable content of the window.
 */
@ExperimentalComposeUiApi
fun singleWindowApplication(
    state: WindowState = WindowState(),
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
        minSize = minSize,
        maxSize = maxSize,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = {
            with(SingleWindowApplicationScope(this@application, this@Window)) {
                content()
            }
        }
    )
}