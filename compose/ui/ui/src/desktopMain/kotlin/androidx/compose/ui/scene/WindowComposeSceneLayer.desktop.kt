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

import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.awt.JLayeredPaneWithTransparencyHack
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.awt.hasMacOsShadow
import androidx.compose.ui.awt.toAwtRectangle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.skiko.OverlayRenderDecorator
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.density
import androidx.compose.ui.window.getDialogScrimBlendMode
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.sizeInPx
import java.awt.Point
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog
import javax.swing.JWindow
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.DelicateSkikoApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.transparentWindowBackgroundHack

internal class WindowComposeSceneLayer(
    composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    private val renderSettings: RenderSettings,
    private val transparent: Boolean,
    compositionContext: CompositionContext,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
) : DesktopComposeSceneLayer(composeContainer, density, layoutDirection) {
    // WindowComposeSceneLayer is tied to the window it was created with
    private val parentWindow = requireNotNull(composeContainer.window)

    private val windowContext = PlatformWindowContext().also {
        it.isWindowTransparent = true
        it.setWindowContainer(windowContainer)
        it.setContainerSizeFromComponent(windowContainer)
    }

    private val layerWindow = JWindow(parentWindow).also {
        it.focusableWindowState = focusable
        it.type = Window.Type.POPUP

        @OptIn(DelicateSkikoApi::class)
        it.background =
            if (transparent) transparentWindowBackgroundHack(composeContainer.renderApi) else null
        if (transparent) {
            it.hasMacOsShadow = false
        }
    }
    private val container = object : JLayeredPaneWithTransparencyHack() {
        override fun addNotify() {
            super.addNotify()
            mediator?.onComponentAttached()
            if (focusable) {
                mediator?.contentComponent?.requestFocusInWindow()
            }
        }

        override fun removeNotify() {
            mediator?.onComponentDetached()
            super.removeNotify()
        }
    }.also {
        it.layout = null
        it.isOpaque = !transparent

        layerWindow.contentPane = it
    }

    private val windowPositionListener = object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            onWindowContainerPositionChanged()
        }
    }

    override var mediator: ComposeSceneMediator? = null

    override var focusable: Boolean = focusable
        set(value) {
            field = value
            layerWindow.focusableWindowState = value
            mediator?.contentComponent?.isFocusable = value
        }

    override var scrimColor: Color? = null

    // Blocking pointer input outside is not supported for window-based popup layers.
    override var consumePointerInputOutside: Boolean
        get() = false
        set(_) {}

    init {
        val boundsInPx = windowContainer.sizeInPx.toRect()
        drawBounds = boundsInPx.roundToIntRect()
        mediator = ComposeSceneMediator(
            container = container,
            isWindowLevel = true,
            windowContext = windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            eventListener = eventListener,
            measureDrawLayerBounds = true,
            architectureComponentsOwner = composeContainer.architectureComponentsOwner,
            coroutineContext = compositionContext.effectCoroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.onWindowTransparencyChanged(true)
            it.sceneBoundsInPx = boundsInPx
            it.contentComponent.size = windowContainer.size
            it.contentComponent.isFocusable = focusable
        }
        onDrawBoundsChanged()

        layerWindow.isVisible = true

        // Track window position in addition to [onChangeWindowPosition] because [windowContainer]
        // might be not the same as real [window].
        parentWindow.addComponentListener(windowPositionListener)

        composeContainer.attachLayer(this)
    }

    override fun close() {
        super.close()
        composeContainer.detachLayer(this)
        mediator?.dispose()
        mediator = null

        parentWindow.removeComponentListener(windowPositionListener)

        layerWindow.dispose()
    }

    override fun onWindowContainerPositionChanged() {
        val scaledRectangle = drawBounds.toAwtRectangle(density)
        setWindowContainerLocation(scaledRectangle.x, scaledRectangle.y)
    }

    override fun onWindowContainerSizeChanged() {
        windowContext.setContainerSizeFromComponent(windowContainer)

        // Update compose constrains based on main window size
        mediator?.sceneBoundsInPx = Rect(
            offset = -drawBounds.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }

    override fun onLayersChange() {
        // Force redraw because rendering depends on other layers
        // see [onRenderOverlay]
        layerWindow.repaint()
    }

    override fun onDrawBoundsChanged() {
        val scaledRectangle = drawBounds.toAwtRectangle(density)
        setWindowContainerLocation(scaledRectangle.x, scaledRectangle.y)
        layerWindow.setSize(scaledRectangle.width, scaledRectangle.height)
        mediator?.contentComponent?.setSize(scaledRectangle.width, scaledRectangle.height)
        mediator?.sceneBoundsInPx = Rect(
            offset = -drawBounds.topLeft.toOffset(),
            size = windowContainer.sizeInPx
        )
    }

    override fun onRenderOverlay(canvas: Canvas, width: Int, height: Int, transparent: Boolean) {
        val scrimColor = scrimColor ?: return
        val paint = Paint().apply {
            color = scrimColor
            blendMode = getDialogScrimBlendMode(transparent)
        }.skiaPaint
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val renderDelegate = OverlayRenderDecorator(
            recordDrawBounds(mediator)
        ) { canvas, width, height ->
            composeContainer.layersAbove(this).forEach {
                it.onRenderOverlay(canvas, width, height, transparent)
            }
        }
        return SkiaLayerComponent(
            mediator = mediator,
            windowContext = windowContext,
            renderDelegate = renderDelegate,
            skiaLayerAnalytics = skiaLayerAnalytics,
            renderSettings = renderSettings,
        )
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        val layoutDirection = layoutDirectionFor(container)
        return PlatformLayersComposeScene(
            frameRecomposer = mediator.frameRecomposer,
            density = density,
            // TODO: Split these into native layout vs repaint invalidation only for the
            //  Swing rendering mode (which has no V-Sync): there `invalidateLayout` should
            //  participate in AWT/Swing layout while `invalidateDraw` schedules a repaint.
            //  The default SkiaLayer pipeline drives V-Sync per-window and
            //  needs to be handled differently.
            invalidateLayout = mediator::onComposeInvalidation,
            invalidateDraw = mediator::onComposeInvalidation,
            layoutDirection = layoutDirection,
            composeSceneContext = composeContainer.createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
        )
    }

    private fun setWindowContainerLocation(x: Int, y: Int) {
        if (!windowContainer.isShowing) {
            return
        }
        val locationOnScreen = windowContainer.locationOnScreen
        layerWindow.location = Point(
            locationOnScreen.x + x,
            locationOnScreen.y + y
        )
    }
}
