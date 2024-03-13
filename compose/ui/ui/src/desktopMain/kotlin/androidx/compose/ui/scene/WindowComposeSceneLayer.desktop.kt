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

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.awt.getTransparentWindowBackground
import androidx.compose.ui.awt.setTransparent
import androidx.compose.ui.awt.toAwtRectangle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.skiko.OverlaySkikoViewDecorator
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.density
import androidx.compose.ui.window.getDialogScrimBlendMode
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.sizeInPx
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog
import javax.swing.JLayeredPane
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class WindowComposeSceneLayer(
    composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    private val transparent: Boolean,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
) : DesktopComposeSceneLayer(composeContainer, density, layoutDirection) {
    private val window get() = requireNotNull(composeContainer.window)
    private val windowContext = PlatformWindowContext().also {
        it.isWindowTransparent = true
        it.setContainerSize(windowContainer.sizeInPx)
    }

    private val dialog = JDialog(
        window,
    ).also {
        it.isAlwaysOnTop = true
        it.isFocusable = focusable
        it.isUndecorated = true
        it.background = getTransparentWindowBackground(
            isWindowTransparent = transparent,
            renderApi = composeContainer.renderApi
        )
    }
    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            mediator?.onComponentAttached()
        }
    }.also {
        it.layout = null
        it.setTransparent(transparent)

        dialog.contentPane = it
    }

    private val windowPositionListener = object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            onChangeWindowPosition()
        }
    }

    override var mediator: ComposeSceneMediator? = null

    override var focusable: Boolean = focusable
        set(value) {
            field = value
            dialog.isFocusable = value
        }

    override var boundsInWindow: IntRect = IntRect.Zero
        set(value) {
            field = value
            setDialogBounds(value)
        }

    override var scrimColor: Color? = null

    init {
        mediator = ComposeSceneMediator(
            container = container,
            windowContext = windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            eventFilter = eventFilter,
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

        // Track window position in addition to [onChangeWindowPosition] because [windowContainer]
        // might be not the same as real [window].
        window.addComponentListener(windowPositionListener)

        composeContainer.attachLayer(this)
    }

    override fun close() {
        super.close()
        composeContainer.detachLayer(this)
        mediator?.dispose()
        mediator = null

        window.removeComponentListener(windowPositionListener)

        dialog.dispose()
    }

    override fun onChangeWindowPosition() {
        val scaledRectangle = boundsInWindow.toAwtRectangle(density)
        dialog.location = getDialogLocation(scaledRectangle.x, scaledRectangle.y)
    }

    override fun onChangeWindowSize() {
        windowContext.setContainerSize(windowContainer.sizeInPx)

        // Update compose constrains based on main window size
        mediator?.sceneBoundsInPx = Rect(
            offset = -boundsInWindow.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }

    override fun onLayersChange() {
        // Force redraw because rendering depends on other layers
        // see [onRenderOverlay]
        dialog.repaint()
    }

    override fun onRenderOverlay(canvas: Canvas, width: Int, height: Int, transparent: Boolean) {
        val scrimColor = scrimColor ?: return
        val paint = Paint().apply {
            color = scrimColor
            blendMode = getDialogScrimBlendMode(transparent)
        }.asFrameworkPaint()
        canvas.drawRect(SkRect.makeWH(width.toFloat(), height.toFloat()), paint)
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val skikoView = OverlaySkikoViewDecorator(mediator) { canvas, width, height ->
            composeContainer.layersAbove(this).forEach {
                it.onRenderOverlay(canvas, width, height, transparent)
            }
        }
        return WindowSkiaLayerComponent(
            mediator = mediator,
            windowContext = windowContext,
            skikoView = skikoView,
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
        mediator?.contentComponent?.setSize(scaledRectangle.width, scaledRectangle.height)
        mediator?.sceneBoundsInPx = Rect(
            offset = -bounds.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }
}
