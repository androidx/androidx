/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * An extra layer for the [ComposeScene].
 * This is utilized to display content as a new [LayoutNode] tree.
 * It is designed to be implemented by platform as adapter for separate platform view/canvas.
 *
 * @see Popup
 * @see Dialog
 */
@InternalComposeUiApi
interface ComposeSceneLayer {
    /**
     * Density of the content which will be used to convert [Dp] units.
     */
    var density: Density

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    var layoutDirection: LayoutDirection

    /**
     * The real bounds of content in pixels relative to [WindowInfo.containerSize].
     * This property is used to set the position and size of [Popup]/[Dialog].
     * The implementation should be ready to react on the changes in size/position that can
     * happen during recompositions.
     */
    var boundsInWindow: IntRect

    /**
     * Composition locals context which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext?

    /**
     * The color of the background fill. It can be set to null if no background drawing is necessary.
     * The anticipated behavior from the implementation is to draw a full-window-sized rectangle
     * using this color. This rectangle should be layered above the main scene content/canvas
     * but below the content of this layer.
     *
     * @see DialogProperties.scrimColor
     */
    var scrimColor: Color?

    /**
     * Indicates if the layer is able to receive focus. When set to true, it can process IME events and key presses,
     * for example, the pressing of the back button.
     *
     * This flag also influences the expected behavior of [setOutsidePointerEventListener]:
     * when [focusable] is true, touch events outside of this layer's bounds are not propagated to
     * the content layered below this one.
     *
     * @see PopupProperties.focusable
     */
    var focusable: Boolean

    /**
     * Close all resources and subscriptions. It's anticipated that the platform implementation
     * will automatically close all layers along with the parent scene.
     * Once this method has been called, invoking any other method of this [ComposeSceneLayer]
     * is prohibited.
     */
    fun close()

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Sets the root key event listener.
     *
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be sent
     * to this [onPreviewKeyEvent]'s child. If none of the children consume the event, it will be
     * sent back up to the root using the [onKeyEvent] callback.
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware keyboard.
     * While implementing this callback, return true to stop propagation of this event.
     */
    fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
        onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    )

    /**
     * Establishes a callback function that is triggered when a pointer event occurs outside
     * of [boundsInWindow]. It's important to note that any gestures initiated within
     * the [boundsInWindow] should be entirely handled by this layer, without activating this event.
     *
     * @param onOutsidePointerEvent The callback function that is invoked when a pointer event
     * occurs outside. It's called only on the primary (left) mouse button or single pointer
     * gesture that started outside of [boundsInWindow].
     */
    fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)? = null,
    )

    /**
     * Returns the position relative to the [ComposeScene] of the [positionInWindow],
     * the position relative to the window in pixels.
     */
    fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset
}

/**
 * Creates and remembers a [ComposeSceneLayer] to be used as an extra layer for
 * the current [ComposeScene]. This layer can be utilized to display content
 * as a new [LayoutNode] tree.
 *
 * @param focusable Indicates whether the layer is focusable. Default value is false.
 * @return The created [ComposeSceneLayer].
 */
@Composable
internal fun rememberComposeSceneLayer(
    focusable: Boolean = false
): ComposeSceneLayer {
    val scene = LocalComposeScene.requireCurrent()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val compositionLocalContext = currentCompositionLocalContext
    val layer = remember {
        scene.createLayer(
            density = density,
            layoutDirection = layoutDirection,
            focusable = focusable,
            compositionContext = parentComposition,
        )
    }
    layer.focusable = focusable
    layer.compositionLocalContext = compositionLocalContext
    layer.density = density
    layer.layoutDirection = layoutDirection

    DisposableEffect(Unit) {
        onDispose {
            layer.close()
        }
    }
    return layer
}
