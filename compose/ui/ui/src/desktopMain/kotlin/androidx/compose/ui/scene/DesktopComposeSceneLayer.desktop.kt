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

package androidx.compose.ui.scene

import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.awt.AwtEventFilter
import androidx.compose.ui.awt.AwtEventFilters
import androidx.compose.ui.awt.OnlyValidPrimaryMouseButtonFilter
import androidx.compose.ui.awt.toAwtRectangle
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEachReversed
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.jetbrains.skia.Canvas

/**
 * Represents an abstract class for a desktop Compose scene layer.
 *
 * @see SwingComposeSceneLayer
 * @see WindowComposeSceneLayer
 */
internal abstract class DesktopComposeSceneLayer(
    protected val composeContainer: ComposeContainer,
    density: Density,
    layoutDirection: LayoutDirection,
) : ComposeSceneLayer {
    protected val windowContainer get() = composeContainer.windowContainer
    protected val layersAbove get() = composeContainer.layersAbove(this)
    protected val eventFilter get() = AwtEventFilters(
        OnlyValidPrimaryMouseButtonFilter,
        DetectEventOutsideLayer(),
        FocusableLayerEventFilter()
    )

    protected abstract val mediator: ComposeSceneMediator?

    private var outsidePointerCallback: ((eventType: PointerEventType) -> Unit)? = null
    private var isClosed = false

    final override var density: Density = density
        set(value) {
            field = value
            mediator?.onChangeDensity(value)
        }

    final override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            field = value
            mediator?.onChangeLayoutDirection(value)
        }

    final override var compositionLocalContext: CompositionLocalContext?
        get() = mediator?.compositionLocalContext
        set(value) { mediator?.compositionLocalContext = value }

    @CallSuper
    override fun close() {
        isClosed = true
    }

    final override fun setContent(content: @Composable () -> Unit) {
        mediator?.setContent(content)
    }

    final override fun setKeyEventListener(
        onPreviewKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)?,
        onKeyEvent: ((androidx.compose.ui.input.key.KeyEvent) -> Boolean)?
    ) {
        mediator?.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent ?: { false },
            onKeyEvent = onKeyEvent ?: { false }
        )
    }

    final override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)?
    ) {
        outsidePointerCallback = onOutsidePointerEvent
    }

    override fun calculateLocalPosition(positionInWindow: IntOffset) =
        positionInWindow // [ComposeScene] is equal to [windowContainer] for the layer.

    /**
     * Called when the focus of the window containing main Compose view has changed.
     */
    open fun onChangeWindowFocus() {
    }

    /**
     * Called when position of the window containing main Compose view has changed.
     */
    open fun onChangeWindowPosition() {
    }

    /**
     * Called when size of the window containing main Compose view has changed.
     */
    open fun onChangeWindowSize() {
    }

    /**
     * Called when the layers in [composeContainer] have changed.
     */
    open fun onLayersChange() {
    }

    /**
     * Renders an overlay on the canvas.
     *
     * @param canvas the canvas to render on
     * @param width the width of the overlay
     * @param height the height of the overlay
     * @param transparent a flag indicating whether [canvas] is transparent
     */
    open fun onRenderOverlay(canvas: Canvas, width: Int, height: Int, transparent: Boolean) {
    }

    /**
     * This method is called when a mouse event occurs outside of this layer.
     *
     * @param event the mouse event
     */
    fun onMouseEventOutside(event: MouseEvent) {
        if (isClosed || !event.isMainAction() || inBounds(event)) {
            return
        }
        val eventType = when (event.id) {
            MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
            MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
            else -> return
        }
        outsidePointerCallback?.invoke(eventType)
    }

    private fun inBounds(event: MouseEvent): Boolean {
        val point = if (event.component != windowContainer) {
            SwingUtilities.convertPoint(event.component, event.point, windowContainer)
        } else {
            event.point
        }
        return boundsInWindow.toAwtRectangle(density).contains(point)
    }

    /**
     * Detect and trigger [DesktopComposeSceneLayer.onMouseEventOutside] if event happened below
     * focused layer.
     */
    private inner class DetectEventOutsideLayer : AwtEventFilter {
        override fun shouldSendMouseEvent(event: MouseEvent): Boolean {
            layersAbove.toList().fastForEachReversed {
                it.onMouseEventOutside(event)
                if (it.focusable) {
                    return true
                }
            }
            return true
        }
    }

    private inner class FocusableLayerEventFilter : AwtEventFilter {
        private val noFocusableLayersAbove: Boolean
            get() = layersAbove.all { !it.focusable }

        override fun shouldSendMouseEvent(event: MouseEvent): Boolean = noFocusableLayersAbove
        override fun shouldSendKeyEvent(event: KeyEvent): Boolean = focusable && noFocusableLayersAbove
    }
}

private fun MouseEvent.isMainAction() =
    button == MouseEvent.BUTTON1
