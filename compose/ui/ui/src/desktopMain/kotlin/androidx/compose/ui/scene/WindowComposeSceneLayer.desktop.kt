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
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.awt.toAwtColor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.sizeInPx
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JDialog
import javax.swing.JLayeredPane
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class WindowComposeSceneLayer(
    private val composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
) : DesktopComposeSceneLayer() {
    private val window get() = requireNotNull(composeContainer.window)
    private val windowContainer get() = composeContainer.windowContainer
    private val windowContext = PlatformWindowContext().also {
        it.isWindowTransparent = true
        it.setContainerSize(windowContainer.sizeInPx)
    }

    private val dialog = JDialog(
        window,
    ).also {
        it.isAlwaysOnTop = true
        it.isUndecorated = true
        it.background = Color.Transparent.toAwtColor()
    }
    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            _mediator?.onComponentAttached()
        }
    }.also {
        it.layout = null
        it.isOpaque = false

        dialog.contentPane = it
    }

    private val windowPositionListener = object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            onChangeWindowPosition()
        }
    }

    private val dialogFocusListener = object : WindowFocusListener {
        override fun windowGainedFocus(e: WindowEvent?) = Unit
        override fun windowLostFocus(e: WindowEvent?) {
            // Use this as trigger of outside click
            outsidePointerCallback?.invoke(PointerEventType.Press)
            outsidePointerCallback?.invoke(PointerEventType.Release)
        }
    }

    private var _mediator: ComposeSceneMediator? = null
    private var outsidePointerCallback: ((eventType: PointerEventType) -> Unit)? = null

    override var density: Density = density
        set(value) {
            field = value
            _mediator?.onChangeDensity(value)
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            field = value
            _mediator?.onChangeLayoutDirection(value)
        }

    override var focusable: Boolean = focusable
        set(value) {
            field = value
            // TODO: Pass it to mediator/scene
        }

    override var boundsInWindow: IntRect = IntRect.Zero
        set(value) {
            field = value
            setDialogBounds(value)
        }

    override var compositionLocalContext: CompositionLocalContext?
        get() = _mediator?.compositionLocalContext
        set(value) { _mediator?.compositionLocalContext = value }

    override var scrimColor: Color? = null

    init {
        _mediator = ComposeSceneMediator(
            container = container,
            windowContext = windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            coroutineContext = compositionContext.effectCoroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.onChangeWindowTransparency(true)
            it.sceneBoundsInPx = windowContainer.sizeInPx.toRect()
            it.contentComponent.size = windowContainer.size
        }
        dialog.location = getDialogLocation(0, 0)
        dialog.size = windowContainer.size
        dialog.isVisible = true
        dialog.addWindowFocusListener(dialogFocusListener)

        // Track window position in addition to [onChangeWindowPosition] because [windowContainer]
        // might be not the same as real [window].
        window.addComponentListener(windowPositionListener)

        composeContainer.attachLayer(this)
    }

    override fun close() {
        composeContainer.detachLayer(this)
        _mediator?.dispose()
        _mediator = null

        window.removeComponentListener(windowPositionListener)

        dialog.removeWindowFocusListener(dialogFocusListener)
        dialog.dispose()
    }

    override fun setContent(content: @Composable () -> Unit) {
        _mediator?.setContent(content)
    }

    override fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        _mediator?.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent ?: { false },
            onKeyEvent = onKeyEvent ?: { false }
        )
    }

    override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)?
    ) {
        outsidePointerCallback = onOutsidePointerEvent
    }

    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset {
        return positionInWindow
    }

    override fun onChangeWindowPosition() {
        val scaledRectangle = boundsInWindow.toAwtRectangle(density)
        dialog.location = getDialogLocation(scaledRectangle.x, scaledRectangle.y)
    }

    override fun onChangeWindowSize() {
        windowContext.setContainerSize(windowContainer.sizeInPx)

        // Update compose constrains based on main window size
        _mediator?.sceneBoundsInPx = Rect(
            offset = -boundsInWindow.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }

    override fun onRenderOverlay(canvas: Canvas, width: Int, height: Int) {
        val scrimColor = scrimColor ?: return
        val paint = Paint().apply { color = scrimColor }.asFrameworkPaint()
        canvas.drawRect(org.jetbrains.skia.Rect.makeWH(width.toFloat(), height.toFloat()), paint)
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return WindowSkiaLayerComponent(
            mediator = mediator,
            windowContext = windowContext,
            skikoView = mediator,
            skiaLayerAnalytics = skiaLayerAnalytics
        )
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        val layoutDirection = layoutDirectionFor(container)
        return SingleLayerComposeScene(
            coroutineContext = mediator.coroutineContext,
            density = density,
            invalidate = mediator::onComposeInvalidation,
            layoutDirection = layoutDirection,
            composeSceneContext = composeContainer.createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
        )
    }

    private fun getDialogLocation(x: Int, y: Int): Point {
        val locationOnScreen = windowContainer.locationOnScreen
        return Point(
            locationOnScreen.x + x,
            locationOnScreen.y + y
        )
    }

    private fun setDialogBounds(bounds: IntRect) {
        val scaledRectangle = bounds.toAwtRectangle(density)
        dialog.location = getDialogLocation(scaledRectangle.x, scaledRectangle.y)
        dialog.setSize(scaledRectangle.width, scaledRectangle.height)
        _mediator?.contentComponent?.setSize(scaledRectangle.width, scaledRectangle.height)
        _mediator?.sceneBoundsInPx = Rect(
            offset = -bounds.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }
}

private fun IntRect.toAwtRectangle(density: Density): Rectangle {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    val width = right - left
    val height = bottom - top
    return Rectangle(
        left, top, width, height
    )
}
